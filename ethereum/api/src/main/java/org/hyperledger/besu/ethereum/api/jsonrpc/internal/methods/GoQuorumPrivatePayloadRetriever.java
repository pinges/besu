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
import org.hyperledger.besu.enclave.EnclaveClientException;
import org.hyperledger.besu.enclave.GoQuorumEnclave;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.AbstractTransactionsPendingOrCompleteResult;

import org.apache.tuweni.bytes.Bytes;

public interface GoQuorumPrivatePayloadRetriever {

  default AbstractTransactionsPendingOrCompleteResult retrievePrivatePayloadIfNecessary(
      final AbstractTransactionsPendingOrCompleteResult transactionResult,
      final GoQuorumEnclave enclave) {
    // when we are in quorum mode we have to check whether this is a private transaction
    if (GoQuorumOptions.goQuorumCompatibilityMode && transactionResult != null) {
      final String v = transactionResult.getV();
      if (v.equals("0x25") || v.equals("0x26")) {
        // Try to get the private payload form the private transaction manager
        // Multi-tenancy mode is not supported with goQuorum mode, so we do not have to check
        // whether the user is supposed to have access to this payload.
        final String enclaveKeyHex = transactionResult.getInput();
        final String enclaveKeyBase64 = Bytes.fromHexString(enclaveKeyHex).toBase64String();
        try {
          final byte[] payload = enclave.receive(enclaveKeyBase64).getPayload();
          transactionResult.setInput(payload);
        } catch (final EnclaveClientException e) {
          // We have not been party to the private tx, so just return the public one
        }
      }
    }
    return transactionResult;
  }
}
