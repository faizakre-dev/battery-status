package com.faizakram.batterystatus.domain.estimates

import com.faizakram.batterystatus.core.model.BatteryEstimates
import com.faizakram.batterystatus.core.model.ChargeSession
import com.faizakram.batterystatus.core.sysfs.SysfsBatteryData
import org.junit.Assert.assertEquals
import org.junit.Test

class EstimatesReducerTest {

    @Test
    fun `a closed session adds its fraction of a cycle`() {
        val current = BatteryEstimates.EMPTY
        val session = session(mahAdded = 1_000)

        val next = EstimatesReducer.reduce(
            current,
            EstimateUpdate(
                session = session,
                sysfs = SysfsBatteryData(cycleCount = null, chargeFullUah = null, chargeFullDesignUah = 4_000_000),
                nowMillis = 5_000,
            ),
        )

        assertEquals(0.25, next.accumulatedCycleFraction, 1e-9)
        assertEquals(4_000_000, next.designCapacityUah)
        assertEquals(5_000L, next.lastUpdatedMillis)
    }

    @Test
    fun `accumulates across sessions`() {
        val current = BatteryEstimates(
            designCapacityUah = 4_000_000,
            lifetimePeakUah = 4_000_000,
            accumulatedCycleFraction = 0.5,
            sysfsCycleCount = null,
            lastUpdatedMillis = 0,
        )

        val next = EstimatesReducer.reduce(
            current,
            EstimateUpdate(session = session(mahAdded = 2_000), nowMillis = 1),
        )

        assertEquals(1.0, next.accumulatedCycleFraction, 1e-9)
    }

    @Test
    fun `lifetime peak only grows`() {
        val current = BatteryEstimates.EMPTY

        val higher = EstimatesReducer.reduce(
            current,
            EstimateUpdate(observedChargeCounterUah = 3_500_000, nowMillis = 1),
        )
        val lower = EstimatesReducer.reduce(
            higher,
            EstimateUpdate(observedChargeCounterUah = 2_000_000, nowMillis = 2),
        )

        assertEquals(3_500_000, higher.lifetimePeakUah)
        assertEquals(3_500_000, lower.lifetimePeakUah)
    }

    @Test
    fun `adopts sysfs design capacity and cycle count when readable`() {
        val next = EstimatesReducer.reduce(
            BatteryEstimates.EMPTY,
            EstimateUpdate(
                sysfs = SysfsBatteryData(cycleCount = 318, chargeFullUah = 3_800_000, chargeFullDesignUah = 4_000_000),
                nowMillis = 1,
            ),
        )

        assertEquals(4_000_000, next.designCapacityUah)
        assertEquals(318, next.sysfsCycleCount)
    }

    @Test
    fun `falls back to 1_1x peak when no design capacity is known`() {
        val next = EstimatesReducer.reduce(
            BatteryEstimates.EMPTY,
            EstimateUpdate(observedChargeCounterUah = 2_000_000, nowMillis = 1),
        )

        assertEquals(2_200_000, next.designCapacityUah)
    }

    @Test
    fun `without a session the accumulated fraction is unchanged`() {
        val current = BatteryEstimates(
            designCapacityUah = 4_000_000,
            lifetimePeakUah = 4_000_000,
            accumulatedCycleFraction = 2.5,
            sysfsCycleCount = null,
            lastUpdatedMillis = 0,
        )

        val next = EstimatesReducer.reduce(
            current,
            EstimateUpdate(observedChargeCounterUah = 3_000_000, nowMillis = 1),
        )

        assertEquals(2.5, next.accumulatedCycleFraction, 1e-9)
    }

    private fun session(mahAdded: Int) = ChargeSession(
        startMillis = 0,
        endMillis = 1,
        percentFrom = 50,
        percentTo = 75,
        mahAdded = mahAdded,
        maxTemperatureCelsius = null,
        avgCurrentUa = null,
        isFull = false,
    )
}
