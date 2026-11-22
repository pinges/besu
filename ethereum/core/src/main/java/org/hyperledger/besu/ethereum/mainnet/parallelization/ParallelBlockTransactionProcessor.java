/*
 * Copyright contributors to Hyperledger Besu.
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
package org.hyperledger.besu.ethereum.mainnet.parallelization;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.mainnet.block.access.list.BlockAccessList.BlockAccessListBuilder;
import org.hyperledger.besu.ethereum.processing.TransactionProcessingResult;
import org.hyperledger.besu.ethereum.trie.pathbased.bonsai.worldview.BonsaiWorldState;
import org.hyperledger.besu.ethereum.worldstate.WorldStateQueryParams;
import org.hyperledger.besu.evm.blockhash.BlockHashLookup;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.worldstate.MutableWorldState;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicIntegerArray;

public abstract class ParallelBlockTransactionProcessor {

  protected CompletableFuture<ParallelizedTransactionContext>[] futures;

  // 1 once a worker began the transaction at that index
  private AtomicIntegerArray started;

  protected CompletableFuture<ParallelizedTransactionContext> removeFuture(final int txIndex) {
    final CompletableFuture<ParallelizedTransactionContext> future = futures[txIndex];
    futures[txIndex] = null;
    return future;
  }

  /**
   * Takes the future of a transaction for the block processor. A worker that already began the
   * transaction is awaited for up to {@code maxWait}: what it has left to do is less than running
   * the transaction again from the start. A transaction no worker has begun is cancelled, so no
   * worker runs it. Returns null when no future exists for the index.
   */
  protected CompletableFuture<ParallelizedTransactionContext> takeFuture(
      final int txIndex, final Duration maxWait) {
    final CompletableFuture<ParallelizedTransactionContext> future = removeFuture(txIndex);
    if (future == null || future.isDone()) {
      return future;
    }
    if (started.get(txIndex) == 0) {
      future.cancel(true);
      return future;
    }
    try {
      future.get(maxWait.toNanos(), TimeUnit.NANOSECONDS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      future.cancel(true);
    } catch (final TimeoutException e) {
      future.cancel(true);
    } catch (final ExecutionException | CancellationException e) {
      // the caller reads the outcome from the future's state
    }
    return future;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public void runAsyncBlock(
      final ProtocolContext protocolContext,
      final BlockHeader blockHeader,
      final List<Transaction> transactions,
      final Address miningBeneficiary,
      final BlockHashLookup blockHashLookup,
      final Wei blobGasPrice,
      final Executor executor,
      final Optional<BlockAccessListBuilder> blockAccessListBuilder,
      final Optional<BlockHeader> maybeParentHeader) {

    futures = new CompletableFuture[transactions.size()];
    final AtomicIntegerArray startedForBlock = new AtomicIntegerArray(transactions.size());
    started = startedForBlock;

    for (int i = 0; i < transactions.size(); i++) {
      final int txIndex = i;
      final Transaction transaction = transactions.get(i);

      futures[i] =
          CompletableFuture.supplyAsync(
              () -> {
                startedForBlock.set(txIndex, 1);
                return precomputeLogsBloom(
                    runTransaction(
                        protocolContext,
                        blockHeader,
                        txIndex,
                        transaction,
                        miningBeneficiary,
                        blockHashLookup,
                        blobGasPrice,
                        blockAccessListBuilder,
                        maybeParentHeader));
              },
              executor);
    }
  }

  // the receipt's bloom depends on this transaction alone, so it is hashed on the worker
  private static ParallelizedTransactionContext precomputeLogsBloom(
      final ParallelizedTransactionContext context) {
    if (context != null && context.transactionProcessingResult() != null) {
      context.transactionProcessingResult().getLogsBloom();
    }
    return context;
  }

  /**
   * Cancels every still-pending speculative future and nulls its slot. Already-consumed slots are
   * null, so on the success path this is a no-op.
   */
  public void abort() {
    if (futures == null) {
      return;
    }
    for (int i = 0; i < futures.length; i++) {
      final CompletableFuture<ParallelizedTransactionContext> future = futures[i];
      if (future != null) {
        future.cancel(true);
        futures[i] = null;
      }
    }
  }

  /** World state at the parent block. Call only when the parent header is known to be present. */
  protected Optional<BonsaiWorldState> getWorldState(
      final ProtocolContext protocolContext, final BlockHeader parentHeader) {
    return protocolContext
        .getWorldStateArchive()
        .getWorldState(WorldStateQueryParams.withBlockHeaderAndNoUpdateNodeHead(parentHeader))
        .map(BonsaiWorldState.class::cast);
  }

  protected abstract ParallelizedTransactionContext runTransaction(
      ProtocolContext protocolContext,
      BlockHeader blockHeader,
      int transactionLocation,
      Transaction transaction,
      Address miningBeneficiary,
      BlockHashLookup blockHashLookup,
      Wei blobGasPrice,
      Optional<BlockAccessListBuilder> blockAccessListBuilder,
      Optional<BlockHeader> maybeParentHeader);

  public abstract Optional<TransactionProcessingResult> getProcessingResult(
      MutableWorldState worldState,
      Address miningBeneficiary,
      Transaction transaction,
      int location,
      Optional<Counter> confirmedParallelizedTransactionCounter,
      Optional<Counter> conflictingButCachedTransactionCounter);
}
