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
package org.hyperledger.besu.tests.web3j.privacy.contracts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hyperledger.besu.privacy.contracts.generated.DefaultOnChainPrivacyGroupManagementContract;
import org.hyperledger.besu.privacy.contracts.generated.OnChainPrivacyGroupManagementProxy;
import org.hyperledger.besu.tests.acceptance.dsl.AcceptanceTestBase;
import org.hyperledger.besu.tests.acceptance.dsl.account.Accounts;
import org.hyperledger.besu.tests.acceptance.dsl.node.BesuNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Base64String;

@SuppressWarnings("unchecked")
public class PrivacyProxyTest extends AcceptanceTestBase {

  private final Base64String firstParticipant =
      Base64String.wrap("93Ky7lXwFkMc7+ckoFgUMku5bpr9tz4zhmWmk9RlNng=");
  private final Base64String secondParticipant =
      Base64String.wrap("9iaJ6OObl6TUWYjXAOyZsL0VaDPwF+tRFkMwwYSeqqw=");
  private final Base64String thirdParticipant =
      Base64String.wrap("Jo2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=");
  private OnChainPrivacyGroupManagementProxy onChainPrivacyGroupManagementProxy;

  private static final String RAW_GET_PARTICIPANTS = "0x5aa68ac0";
  private static final String RAW_ADD_PARTICIPANT =
      "0xb4926e2500000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000000";

  private BesuNode minerNode;
  private DefaultOnChainPrivacyGroupManagementContract defaultOnChainPrivacyGroupManagementContract;

  @Before
  public void setUp() throws Exception {
    minerNode = besu.createMinerNode("node");
    cluster.start(minerNode);
    defaultOnChainPrivacyGroupManagementContract =
        minerNode.execute(
            contractTransactions.createSmartContract(
                DefaultOnChainPrivacyGroupManagementContract.class));
    onChainPrivacyGroupManagementProxy =
        minerNode.execute(
            contractTransactions.createSmartContract(
                OnChainPrivacyGroupManagementProxy.class,
                defaultOnChainPrivacyGroupManagementContract.getContractAddress()));
  }

  @Test
  public void rlp() throws Exception {
    assertThat(onChainPrivacyGroupManagementProxy.isValid()).isEqualTo(true);
    contractVerifier
        .validTransactionReceipt(onChainPrivacyGroupManagementProxy.getContractAddress())
        .verify(onChainPrivacyGroupManagementProxy);
    assertThat(RAW_GET_PARTICIPANTS)
        .isEqualTo(onChainPrivacyGroupManagementProxy.getParticipants().encodeFunctionCall());
    assertThat(RAW_ADD_PARTICIPANT)
        .isEqualTo(
            onChainPrivacyGroupManagementProxy
                .addParticipants(Collections.emptyList())
                .encodeFunctionCall());
  }

  @Ignore("return 0x which causes web3j to throw exception instead of return empty list")
  @Test
  public void deploysWithNoParticipant() throws Exception {
    final List<byte[]> participants = onChainPrivacyGroupManagementProxy.getParticipants().send();
    assertThat(participants.size()).isEqualTo(0);
  }

  @Test
  public void canAddParticipants() throws Exception {
    onChainPrivacyGroupManagementProxy
        .addParticipants(Arrays.asList(firstParticipant.raw(), secondParticipant.raw()))
        .send();
    final List<byte[]> participants = onChainPrivacyGroupManagementProxy.getParticipants().send();
    assertThat(participants.size()).isEqualTo(2);
    assertThat(firstParticipant.raw()).isEqualTo(participants.get(0));
    assertThat(secondParticipant.raw()).isEqualTo(participants.get(1));
  }

  @Test
  public void nonOwnerCannotUpgrade() throws Exception {
    onChainPrivacyGroupManagementProxy
        .addParticipants(Arrays.asList(firstParticipant.raw(), secondParticipant.raw()))
        .send();
    final List<byte[]> participants = onChainPrivacyGroupManagementProxy.getParticipants().send();
    assertThat(participants.size()).isEqualTo(2);
    assertThat(firstParticipant.raw()).isEqualTo(participants.get(0));
    assertThat(secondParticipant.raw()).isEqualTo(participants.get(1));

    final DefaultOnChainPrivacyGroupManagementContract upgradedContract =
        minerNode.execute(
            contractTransactions.createSmartContract(
                DefaultOnChainPrivacyGroupManagementContract.class));

    final HttpService httpService =
        new HttpService(
            "http://" + minerNode.getHostName() + ":" + minerNode.getJsonRpcSocketPort().get());
    final Web3j web3j = Web3j.build(httpService);

    // load the proxy contract, use it with another signer
    final OnChainPrivacyGroupManagementProxy proxyContractAccount2 =
        OnChainPrivacyGroupManagementProxy.load(
            onChainPrivacyGroupManagementProxy.getContractAddress(),
            web3j,
            Credentials.create(Accounts.GENESIS_ACCOUNT_TWO_PRIVATE_KEY),
            new DefaultGasProvider());
    // contract is the proxy contract and uses genesis account 2. It should not be able to upgrade
    // the contract, because it is not the owner of "upgradedContract"
    assertThatThrownBy(
            () -> proxyContractAccount2.upgradeTo(upgradedContract.getContractAddress()).send())
        .isInstanceOf(TransactionException.class);
  }

  @Test
  public void ownerCanUpgrade() throws Exception {
    onChainPrivacyGroupManagementProxy
        .addParticipants(Arrays.asList(firstParticipant.raw(), secondParticipant.raw()))
        .send();
    final List<byte[]> participants = onChainPrivacyGroupManagementProxy.getParticipants().send();
    assertThat(participants.size()).isEqualTo(2);
    assertThat(firstParticipant.raw()).isEqualTo(participants.get(0));
    assertThat(secondParticipant.raw()).isEqualTo(participants.get(1));

    final DefaultOnChainPrivacyGroupManagementContract upgradedContract =
        minerNode.execute(
            contractTransactions.createSmartContract(
                DefaultOnChainPrivacyGroupManagementContract.class));

    onChainPrivacyGroupManagementProxy.upgradeTo(upgradedContract.getContractAddress()).send();
    final List<byte[]> participantsAfterUpgrade =
        onChainPrivacyGroupManagementProxy.getParticipants().send();
    assertThat(participantsAfterUpgrade.size()).isEqualTo(2);
    assertThat(firstParticipant.raw()).isEqualTo(participantsAfterUpgrade.get(0));
    assertThat(secondParticipant.raw()).isEqualTo(participantsAfterUpgrade.get(1));
  }

  @Test
  public void canAddTwiceToContractWhenCallLock() throws Exception {
    onChainPrivacyGroupManagementProxy
        .addParticipants(Arrays.asList(firstParticipant.raw(), thirdParticipant.raw()))
        .send();
    onChainPrivacyGroupManagementProxy.lock().send();
    onChainPrivacyGroupManagementProxy
        .addParticipants(Collections.singletonList(secondParticipant.raw()))
        .send();
    final List<byte[]> participants = onChainPrivacyGroupManagementProxy.getParticipants().send();
    assertThat(participants.size()).isEqualTo(3);
    assertThat(firstParticipant.raw()).isEqualTo(participants.get(0));
    assertThat(thirdParticipant.raw()).isEqualTo(participants.get(1));
    assertThat(secondParticipant.raw()).isEqualTo(participants.get(2));
  }
}
