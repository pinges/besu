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
package org.hyperledger.orion.testutil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OrionConfiguration {

  private final Path[] publicKeys;
  private final Path[] privateKeys;
  private final Path tempDir;
  private final List<String> otherNodes = new ArrayList<>();
  private final boolean clearKnownNodes;

  public OrionConfiguration(
          final Path[] publicKeys,
          final Path[] privateKeys,
          final Path tempDir,
          final List<String> otherNodes,
          final boolean clearKnownNodes) {

    this.publicKeys = publicKeys;
    this.privateKeys = privateKeys;
    this.tempDir = tempDir;
    this.otherNodes.addAll(otherNodes);
    this.clearKnownNodes = clearKnownNodes;
  }

  public Path[] getPublicKeys() {
    return publicKeys;
  }

  public Path[] getPrivateKeys() {
    return privateKeys;
  }

  public Path getTempDir() {
    return tempDir;
  }

  public List<String> getOtherNodes() {
    return otherNodes;
  }

  public void addOtherNode(final String otherNode) {
    otherNodes.add(otherNode);
  }

  public boolean isClearKnownNodes() {
    return clearKnownNodes;
  }
}
