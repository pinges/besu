/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.eth.manager;

import org.hyperledger.besu.ethereum.eth.manager.EthPeer.DisconnectCallback;
import org.hyperledger.besu.ethereum.eth.peervalidation.PeerValidator;
import org.hyperledger.besu.ethereum.p2p.peers.Peer;
import org.hyperledger.besu.ethereum.p2p.rlpx.RlpxAgent;
import org.hyperledger.besu.ethereum.p2p.rlpx.connections.PeerConnection;
import org.hyperledger.besu.ethereum.p2p.rlpx.wire.messages.DisconnectMessage;
import org.hyperledger.besu.metrics.BesuMetricCategory;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.permissioning.NodeMessagePermissioningProvider;
import org.hyperledger.besu.util.Subscribers;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import org.apache.tuweni.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthPeers {
  private static final Logger LOG = LoggerFactory.getLogger(EthPeers.class);
  public static final Comparator<EthPeer> TOTAL_DIFFICULTY =
      Comparator.comparing(((final EthPeer p) -> p.chainState().getEstimatedTotalDifficulty()));

  public static final Comparator<EthPeer> CHAIN_HEIGHT =
      Comparator.comparing(((final EthPeer p) -> p.chainState().getEstimatedHeight()));

  public static final Comparator<EthPeer> HEAVIEST_CHAIN =
      TOTAL_DIFFICULTY.thenComparing(CHAIN_HEIGHT);

  public static final Comparator<EthPeer> LEAST_TO_MOST_BUSY =
      Comparator.comparing(EthPeer::outstandingRequests)
          .thenComparing(EthPeer::getLastRequestTimestamp);

  private final Map<Bytes, EthPeer> connections = new ConcurrentHashMap<>();
  // If entries in the notReadyConnections are not removed or
  private final Cache<PeerConnection, EthPeer> notReadyConnections =
      CacheBuilder.newBuilder()
          .expireAfterWrite(Duration.ofSeconds(20L))
          .concurrencyLevel(1)
          .removalListener(this::onCacheRemoval)
          .build();
  private final String protocolName;
  private final Clock clock;
  private final List<NodeMessagePermissioningProvider> permissioningProviders;
  private final int maxMessageSize;
  private final Subscribers<ConnectCallback> connectCallbacks = Subscribers.create();
  private final Subscribers<DisconnectCallback> disconnectCallbacks = Subscribers.create();
  private final Collection<PendingPeerRequest> pendingRequests = new CopyOnWriteArrayList<>();
  private final int peerLowerBound;
  private final int peerUpperBound;
  private final int maxRemotelyInitiatedConnections;
  private final Boolean randomPeerPriority;
  private final Bytes nodeIdMask = Bytes.random(64);

  private Comparator<EthPeer> bestPeerComparator;
  private final Bytes localNodeId;
  private RlpxAgent rlpxAgent;

  public EthPeers(
      final String protocolName,
      final Clock clock,
      final MetricsSystem metricsSystem,
      final int maxMessageSize,
      final List<NodeMessagePermissioningProvider> permissioningProviders,
      final Bytes encodedBytes,
      final int peerLowerBound,
      final int peerUpperBound,
      final int maxRemotelyInitiatedConnections,
      final Boolean randomPeerPriority) {
    this.protocolName = protocolName;
    this.clock = clock;
    this.permissioningProviders = permissioningProviders;
    this.maxMessageSize = maxMessageSize;
    this.bestPeerComparator = HEAVIEST_CHAIN;
    this.localNodeId = encodedBytes;
    this.peerLowerBound = peerLowerBound;
    this.peerUpperBound = peerUpperBound;
    this.maxRemotelyInitiatedConnections = maxRemotelyInitiatedConnections;
    this.randomPeerPriority = randomPeerPriority;
    metricsSystem.createIntegerGauge(
        BesuMetricCategory.ETHEREUM,
        "peer_count",
        "The current number of peers connected",
        () -> (int) streamAvailablePeers().count());
    metricsSystem.createIntegerGauge(
        BesuMetricCategory.PEERS,
        "pending_peer_requests_current",
        "Number of peer requests currently pending because peers are busy",
        pendingRequests::size);
  }

  public void registerNewConnection(
      final PeerConnection newConnection, final List<PeerValidator> peerValidators) {
    final AtomicBoolean addToNotReadyConnections = new AtomicBoolean(false);
    final AtomicReference<EthPeer> ethPeer = new AtomicReference<>();
    final Bytes id = newConnection.getPeer().getId();
    connections.compute(
        id,
        (c, p) -> {
          if (p != null && !p.isDisconnected()) {
            return p; // Keep existing peer with the existing connection.
          }
          final List<PeerConnection> nrcWithPeer = getNotReadyConnections(id);
          if (!nrcWithPeer.isEmpty()) {
            ethPeer.set(notReadyConnections.getIfPresent(nrcWithPeer.get(0)));
            if (nrcWithPeer.stream()
                .anyMatch(
                    conn ->
                        (conn.inboundInitiated() == newConnection.inboundInitiated())
                            && !conn.isDisconnected())) {
              LOG.info(
                  "Found connection from same peer and initiated by the same side in not ready connections. Peer {}",
                  id);
              return null;
            }
          }
          addToNotReadyConnections.set(true);
          return null;
        });
    if (addToNotReadyConnections.get()) {
      EthPeer peer = ethPeer.get();
      if (peer == null) {
        peer =
            new EthPeer(
                newConnection,
                protocolName,
                this::ethPeerStatusExchanged,
                peerValidators,
                maxMessageSize,
                clock,
                permissioningProviders,
                localNodeId);
      }

      synchronized (notReadyConnections) {
        LOG.info(
            "Adding connection {} with peer {} into notReadyConnections map", newConnection, id);
        final Optional<EthPeer> peerInList =
            notReadyConnections.asMap().values().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
        if (peerInList.isEmpty()) {
          notReadyConnections.put(newConnection, peer);
        } else {
          notReadyConnections.put(newConnection, peerInList.get());
        }
      }

    } else {
      newConnection.disconnect(DisconnectMessage.DisconnectReason.ALREADY_CONNECTED);
      LOG.info("disconnecting connection {} with peer {}", newConnection, id);
    }
  }

  @NotNull
  private List<PeerConnection> getNotReadyConnections(final Bytes id) {
    return notReadyConnections.asMap().keySet().stream()
        .filter(nrc -> nrc.getPeer().getId().equals(id))
        .collect(Collectors.toList());
  }

  public void registerDisconnect(final PeerConnection connection) {
    final EthPeer peer = connections.remove(connection.getPeer().getId());
    if (peer != null) {
      disconnectCallbacks.forEach(callback -> callback.onDisconnect(peer));
      peer.handleDisconnect();
      abortPendingRequestsAssignedToDisconnectedPeers();
      LOG.debug("Disconnected EthPeer {}", peer);
    }
    reattemptPendingPeerRequests();
  }

  private void abortPendingRequestsAssignedToDisconnectedPeers() {
    synchronized (this) {
      final Iterator<PendingPeerRequest> iterator = pendingRequests.iterator();
      while (iterator.hasNext()) {
        final PendingPeerRequest request = iterator.next();
        if (request.getAssignedPeer().map(EthPeer::isDisconnected).orElse(false)) {
          request.abort();
        }
      }
    }
  }

  public EthPeer peer(final PeerConnection connection) {
    final EthPeer peer = notReadyConnections.getIfPresent(connection);
    if (peer != null) {
      return peer;
    } else {
      return connections.get(connection.getPeer().getId());
    }
  }

  public EthPeer peer(final Bytes peerId) {
    return connections.get(peerId);
  }

  public PendingPeerRequest executePeerRequest(
      final PeerRequest request, final long minimumBlockNumber, final Optional<EthPeer> peer) {
    final PendingPeerRequest pendingPeerRequest =
        new PendingPeerRequest(this, request, minimumBlockNumber, peer);
    synchronized (this) {
      if (!pendingPeerRequest.attemptExecution()) {
        pendingRequests.add(pendingPeerRequest);
      }
    }
    return pendingPeerRequest;
  }

  public void dispatchMessage(
      final EthPeer peer, final EthMessage ethMessage, final String protocolName) {
    peer.dispatch(ethMessage, protocolName);
    if (peer.hasAvailableRequestCapacity()) {
      reattemptPendingPeerRequests();
    }
  }

  public void dispatchMessage(final EthPeer peer, final EthMessage ethMessage) {
    dispatchMessage(peer, ethMessage, protocolName);
  }

  @VisibleForTesting
  void reattemptPendingPeerRequests() {
    synchronized (this) {
      final Iterator<PendingPeerRequest> iterator = pendingRequests.iterator();
      while (iterator.hasNext()) {
        final PendingPeerRequest request = iterator.next();
        if (request.attemptExecution()) {
          pendingRequests.remove(request);
        }
      }
    }
  }

  public long subscribeConnect(final ConnectCallback callback) {
    return connectCallbacks.subscribe(callback);
  }

  public void unsubscribeConnect(final long id) {
    connectCallbacks.unsubscribe(id);
  }

  public void subscribeDisconnect(final DisconnectCallback callback) {
    disconnectCallbacks.subscribe(callback);
  }

  public int peerCount() {
    removeDisconnectedPeers();
    return connections.size();
  }

  public int getMaxPeers() {
    return peerUpperBound;
  }

  public Stream<EthPeer> streamAllPeers() {
    return connections.values().stream();
  }

  private void removeDisconnectedPeers() {
    connections.values().stream()
        .forEach(
            ep -> {
              if (ep.isDisconnected()) {
                connections.remove(ep.getId(), ep);
              }
            });
  }

  public Stream<EthPeer> streamAvailablePeers() {
    removeDisconnectedPeers();
    return streamAllPeers()
        .filter(EthPeer::readyForRequests)
        .filter(peer -> !peer.isDisconnected());
  }

  public Stream<EthPeer> streamBestPeers() {
    return streamAvailablePeers().sorted(getBestChainComparator().reversed());
  }

  public Optional<EthPeer> bestPeer() {
    return streamAvailablePeers().max(getBestChainComparator());
  }

  public Optional<EthPeer> bestPeerWithHeightEstimate() {
    return bestPeerMatchingCriteria(
        p -> p.isFullyValidated() && p.chainState().hasEstimatedHeight());
  }

  public Optional<EthPeer> bestPeerMatchingCriteria(final Predicate<EthPeer> matchesCriteria) {
    return streamAvailablePeers().filter(matchesCriteria).max(getBestChainComparator());
  }

  public void setBestChainComparator(final Comparator<EthPeer> comparator) {
    LOG.info("Updating the default best peer comparator");
    bestPeerComparator = comparator;
  }

  public Comparator<EthPeer> getBestChainComparator() {
    return bestPeerComparator;
  }

  public void setRlpxAgent(final RlpxAgent rlpxAgent) {
    this.rlpxAgent = rlpxAgent;
    rlpxAgent.setGetAllActiveConnectionsCallback(this::getAllActiveConnections);
    rlpxAgent.setGetAllConnectionsCallback(this::getAllConnections);
  }

  private Stream<PeerConnection> getAllActiveConnections() {
    return connections.values().stream()
        .map(p -> p.getConnection())
        .filter(c -> !c.isDisconnected());
  }

  private Stream<PeerConnection> getAllConnections() {
    return Stream.concat(
            connections.values().stream().map(p -> p.getConnection()),
            notReadyConnections.asMap().keySet().stream())
        .distinct()
        .filter(c -> !c.isDisconnected());
  }

  public boolean shouldConnectOutbound(final Peer peer) {
    final Bytes id = peer.getId();
    if (connections.containsKey(id)) {
      return false;
    }
    if (peerCount() < peerLowerBound) {
      final List<PeerConnection> notReadyConnections = getNotReadyConnections(id);
      if (!notReadyConnections.isEmpty()) {
        // we already have a connection that we have initiated that is getting ready
        if (!notReadyConnections.stream().anyMatch(c -> !c.inboundInitiated())) {
          return false;
        }
      }
    }
    return true;
  }

  @FunctionalInterface
  public interface ConnectCallback {
    void onPeerConnected(EthPeer newPeer);
  }

  @Override
  public String toString() {
    if (connections.isEmpty()) {
      return "0 EthPeers {}";
    }
    final String connectionsList =
        connections.values().stream()
            .sorted()
            .map(EthPeer::toString)
            .collect(Collectors.joining(", \n"));
    return connections.size() + " EthPeers {\n" + connectionsList + '}';
  }

  private void ethPeerStatusExchanged(final EthPeer peer) {
    if (addPeerToEthPeers(peer)) {
      connectCallbacks.forEach(cb -> cb.onPeerConnected(peer));
    }
  }

  private int comparePeerPriorities(final EthPeer p1, final EthPeer p2) {
    final PeerConnection a = p1.getConnection();
    final PeerConnection b = p2.getConnection();
    final boolean aIgnoresPeerLimits = rlpxAgent.canExceedConnectionLimits(a.getPeer());
    final boolean bIgnoresPeerLimits = rlpxAgent.canExceedConnectionLimits(b.getPeer());
    if (aIgnoresPeerLimits && !bIgnoresPeerLimits) {
      return -1;
    } else if (bIgnoresPeerLimits && !aIgnoresPeerLimits) {
      return 1;
    } else {
      return randomPeerPriority
          ? compareByMaskedNodeId(a, b)
          : compareConnectionInitiationTimes(a, b);
    }
  }

  private int compareConnectionInitiationTimes(final PeerConnection a, final PeerConnection b) {
    return Math.toIntExact(a.getInitiatedAt() - b.getInitiatedAt());
  }

  private int compareByMaskedNodeId(final PeerConnection a, final PeerConnection b) {
    return a.getPeer().getId().xor(nodeIdMask).compareTo(b.getPeer().getId().xor(nodeIdMask));
  }

  private void enforceRemoteConnectionLimits() {
    if (!shouldLimitRemoteConnections() || peerCount() < maxRemotelyInitiatedConnections) {
      // Nothing to do
      return;
    }

    getActivePrioritizedPeers()
        .filter(p -> p.getConnection().inboundInitiated())
        .filter(p -> !rlpxAgent.canExceedConnectionLimits(p.getConnection().getPeer()))
        .skip(maxRemotelyInitiatedConnections)
        .forEach(
            conn -> {
              LOG.debug(
                  "Too many remotely initiated connections. Disconnect low-priority connection: {}, maxRemote={}",
                  conn,
                  maxRemotelyInitiatedConnections);
              conn.disconnect(DisconnectMessage.DisconnectReason.TOO_MANY_PEERS);
            });
  }

  private Stream<EthPeer> getActivePrioritizedPeers() {
    return connections.values().stream()
        .filter(p -> !p.getConnection().isDisconnected())
        .sorted(this::comparePeerPriorities);
  }

  private void enforceConnectionLimits() {
    if (peerCount() < peerUpperBound) {
      // Nothing to do - we're under our limits
      return;
    }
    getActivePrioritizedPeers()
        .skip(peerUpperBound)
        .map(EthPeer::getConnection)
        .filter(c -> !rlpxAgent.canExceedConnectionLimits(c.getPeer()))
        .forEach(
            conn -> {
              LOG.debug(
                  "Too many connections. Disconnect low-priority connection: {}, maxConnections={}",
                  conn,
                  peerUpperBound);
              conn.disconnect(DisconnectMessage.DisconnectReason.TOO_MANY_PEERS);
            });
  }

  private boolean remoteConnectionLimitReached() {
    return shouldLimitRemoteConnections()
        && countUntrustedRemotelyInitiatedConnections() >= maxRemotelyInitiatedConnections;
  }

  private boolean shouldLimitRemoteConnections() {
    return maxRemotelyInitiatedConnections < peerUpperBound;
  }

  private long countUntrustedRemotelyInitiatedConnections() {
    return connections.values().stream()
        .map(ep -> ep.getConnection())
        .filter(c -> c.inboundInitiated())
        .filter(c -> !c.isDisconnected())
        .filter(conn -> !rlpxAgent.canExceedConnectionLimits(conn.getPeer()))
        .count();
  }

  private void onCacheRemoval(
      final RemovalNotification<PeerConnection, EthPeer> removalNotification) {
    if (removalNotification.wasEvicted()) {
      final PeerConnection peerConnectionRemoved = removalNotification.getKey();
      final PeerConnection peerConnectionOfEthPeer = removalNotification.getValue().getConnection();
      if (!peerConnectionRemoved.equals(peerConnectionOfEthPeer)) {
        // this means that there is another connection to the same peer that was selected or
        peerConnectionRemoved.disconnect(DisconnectMessage.DisconnectReason.ALREADY_CONNECTED);
      }
    }
  }

  private boolean addPeerToEthPeers(final EthPeer peer) {
    // We have a connection to a peer that is on the right chain and is willing to connect to us.
    // Figure out whether we want to keep this peer and add it to the EthPeers connections.
    final Bytes id = peer.getId();
    if (!randomPeerPriority) {
      // Disconnect if too many peers
      if (!rlpxAgent.canExceedConnectionLimits(peer.getConnection().getPeer())
          && peerCount() >= peerUpperBound) {
        LOG.debug(
            "Too many peers. Disconnect incoming connection: {} currentCount {}, max {}",
            peer.getConnection(),
            peerCount(),
            peerUpperBound);
        peer.getConnection().disconnect(DisconnectMessage.DisconnectReason.TOO_MANY_PEERS);
        return false;
      }
      // Disconnect if too many remotely-initiated connections
      if (!rlpxAgent.canExceedConnectionLimits(peer.getConnection().getPeer())
          && remoteConnectionLimitReached()) {
        LOG.debug(
            "Too many remotely-initiated connections. Disconnect incoming connection: {}, maxRemote={}",
            peer.getConnection(),
            maxRemotelyInitiatedConnections);
        peer.getConnection().disconnect(DisconnectMessage.DisconnectReason.TOO_MANY_PEERS);
        return false;
      }
      final boolean added = connections.putIfAbsent(id, peer) == null;
      if (added)
        LOG.info(
            "Added peer {} with connection {} to connections", peer.getId(), peer.getConnection());
      return added;
    } else {
      // randomPeerPriority! Add the peer and if there are too many connections fix it
      connections.putIfAbsent(id, peer);
      enforceRemoteConnectionLimits();
      enforceConnectionLimits();
      return connections.containsKey(id);
    }
  }
}
