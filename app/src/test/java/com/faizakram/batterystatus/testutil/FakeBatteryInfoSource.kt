package com.faizakram.batterystatus.testutil

import com.faizakram.batterystatus.core.battery.BatteryInfoSource
import com.faizakram.batterystatus.core.model.BatterySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** [BatteryInfoSource] that returns a fixed, mutable snapshot. */
class FakeBatteryInfoSource(var snapshot: BatterySnapshot) : BatteryInfoSource {

    override val snapshots: Flow<BatterySnapshot> = flowOf(snapshot)

    override fun current(): BatterySnapshot = snapshot
}
