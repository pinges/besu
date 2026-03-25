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
package org.hyperledger.besu.ethereum.eth.sync.snapsync;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderTestFixture;
import org.hyperledger.besu.ethereum.eth.sync.common.PivotSyncState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SnapSyncProcessStateTest {

  private SnapSyncProcessState processState;
  private BlockHeader pivotHeader;

  @BeforeEach
  public void setUp() {
    pivotHeader = new BlockHeaderTestFixture().number(1000).buildHeader();
    final PivotSyncState baseSyncState = new PivotSyncState(pivotHeader, false);
    processState = new SnapSyncProcessState(baseSyncState);
  }

  @Test
  public void shouldNotBeFrozenInitially() {
    assertThat(processState.isPivotFrozen()).isFalse();
    assertThat(processState.getFrozenPivotBlockHeader()).isEmpty();
  }

  @Test
  public void shouldFreezeAndReturnHeader() {
    final BlockHeader result = processState.freezePivot(pivotHeader);

    assertThat(result).isEqualTo(pivotHeader);
    assertThat(processState.isPivotFrozen()).isTrue();
    assertThat(processState.getFrozenPivotBlockHeader()).contains(pivotHeader);
  }

  @Test
  public void shouldBeIdempotentAndReturnOriginalFrozenHeader() {
    final BlockHeader laterHeader = new BlockHeaderTestFixture().number(2000).buildHeader();

    processState.freezePivot(pivotHeader);
    final BlockHeader result = processState.freezePivot(laterHeader);

    // Should return the original frozen header, not the later one
    assertThat(result).isEqualTo(pivotHeader);
    assertThat(processState.getFrozenPivotBlockHeader()).contains(pivotHeader);
  }
}
