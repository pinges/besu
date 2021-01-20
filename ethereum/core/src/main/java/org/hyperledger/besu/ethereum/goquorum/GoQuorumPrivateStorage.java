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

import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.TransactionReceipt;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;

public interface GoQuorumPrivateStorage {

  Optional<Hash> getPrivateStateRootHash(final Hash publicStateRootHash);

  Optional<TransactionReceipt> getTransactionReceipt(Bytes32 blockHash, Bytes32 txHash);

  // TODO-goquorum private bloom filters

  Updater updater();

  interface Updater {

    // TODO-goquorum Do we need this? I think in quorum they do not have both the public and the
    // private receipt
    Updater putPrivateStateRootHashMapping(
        final Hash publicStateRootHash, final Hash privateStateRootHash);

    // TODO-goquorum Do we need this? I think we are storing the private Tx receipts instead of the
    // public one in the public DB
    Updater putTransactionReceipt(
        final Hash blockHash,
        final Hash transactionHash,
        final TransactionReceipt transactionReceipt);

    void commit();

    void rollback();
  }
}