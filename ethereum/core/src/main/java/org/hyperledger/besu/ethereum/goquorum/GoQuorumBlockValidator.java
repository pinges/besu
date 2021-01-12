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
package org.hyperledger.besu.ethereum.goquorum;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.hyperledger.besu.ethereum.MainnetBlockValidator;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.chain.BadBlockManager;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.ethereum.mainnet.BlockBodyValidator;
import org.hyperledger.besu.ethereum.mainnet.BlockHeaderValidator;
import org.hyperledger.besu.ethereum.mainnet.BlockProcessor;
import org.hyperledger.besu.ethereum.mainnet.BlockProcessor.Result;

import java.util.Optional;

import org.apache.logging.log4j.Logger;

public class GoQuorumBlockValidator extends MainnetBlockValidator {

  private static final Logger LOG = getLogger();

  // TODO-goquorum proper wiring of GoQuorumPrivateStorage instead of static references?
  private final GoQuorumPrivateStorage goQuorumPrivateStorage;

  public GoQuorumBlockValidator(
      final BlockHeaderValidator blockHeaderValidator,
      final BlockBodyValidator blockBodyValidator,
      final BlockProcessor blockProcessor,
      final BadBlockManager badBlockManager) {
    super(blockHeaderValidator, blockBodyValidator, blockProcessor, badBlockManager);

    if (!(blockProcessor instanceof GoQuorumBlockProcessor)) {
      throw new IllegalStateException(
          "GoQuorumBlockValidator requires an instance of GoQuorumBlockProcessor");
    }

    goQuorumPrivateStorage = GoQuorumKeyValueStorage.INSTANCE;
  }

  @Override
  protected Result processBlock(
      final ProtocolContext context, final MutableWorldState worldState, final Block block) {

    System.out.println("Using the GoQuormBlockValidator");
    final MutableWorldState privateWorldState =
        getPrivateWorldState(context, worldState.rootHash(), block.getHash());

    return ((GoQuorumBlockProcessor) blockProcessor)
        .processBlock(context.getBlockchain(), worldState, privateWorldState, block);
  }

  private MutableWorldState getPrivateWorldState(
      final ProtocolContext context, final Hash worldStateRootHash, final Hash publicBlockHash) {
    final Hash privateStateRootHash =
        goQuorumPrivateStorage
            .getPrivateStateRootHash(worldStateRootHash)
            .orElse(Hash.EMPTY_TRIE_HASH);

    final Optional<MutableWorldState> maybePrivateWorldState =
        context
            .getPrivateWorldStateArchive()
            .flatMap(p -> p.getMutable(privateStateRootHash, publicBlockHash));
    if (maybePrivateWorldState.isEmpty()) {
      LOG.debug(
          "Private world state not available for public world state root hash {}, public block hash {}",
          worldStateRootHash,
          publicBlockHash);

      /*
       This should never happen because privateStateRootResolver will either return a matching
       private world state root hash, or the hash for an empty world state (first private tx ever).
      */
      throw new IllegalStateException(
          "Private world state not available for public world state root hash " + publicBlockHash);
    }
    return maybePrivateWorldState.get();
  }
}
