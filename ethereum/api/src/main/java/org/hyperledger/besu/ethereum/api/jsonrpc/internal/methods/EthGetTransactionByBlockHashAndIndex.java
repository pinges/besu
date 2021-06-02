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
package org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods;

import org.hyperledger.besu.config.GoQuorumOptions;
import org.hyperledger.besu.enclave.GoQuorumEnclave;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.parameters.UnsignedIntParameter;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.TransactionCompleteResult;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.TransactionResult;
import org.hyperledger.besu.ethereum.api.query.BlockchainQueries;
import org.hyperledger.besu.ethereum.api.query.TransactionWithMetadata;
import org.hyperledger.besu.ethereum.core.Hash;
import org.hyperledger.besu.ethereum.core.PrivacyParameters;

import java.util.Optional;

public class EthGetTransactionByBlockHashAndIndex
    implements JsonRpcMethod, GoQuorumPrivatePayloadRetriever {

  private final BlockchainQueries blockchain;
  private GoQuorumEnclave enclave = null;

  public EthGetTransactionByBlockHashAndIndex(
      final BlockchainQueries blockchain, final PrivacyParameters privacyParameters) {
    this.blockchain = blockchain;
    if (GoQuorumOptions.goQuorumCompatibilityMode) {
      this.enclave = privacyParameters.getGoQuorumPrivacyParameters().orElseThrow().enclave();
    }
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GET_TRANSACTION_BY_BLOCK_HASH_AND_INDEX.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final Hash hash = requestContext.getRequiredParameter(0, Hash.class);
    final int index = requestContext.getRequiredParameter(1, UnsignedIntParameter.class).getValue();
    final Optional<TransactionWithMetadata> transactionWithMetadata =
        blockchain.transactionByBlockHashAndIndex(hash, index);
    final TransactionResult result =
        retrievePrivatePayloadIfNecessary(
            transactionWithMetadata.map(TransactionCompleteResult::new).orElse(null), enclave);
    return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), result);
  }
}
