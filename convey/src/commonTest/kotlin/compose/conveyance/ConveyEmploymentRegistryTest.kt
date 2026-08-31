package compose.conveyance

import kotlin.test.Test
import kotlin.test.assertEquals

class ConveyEmploymentRegistryTest {

    @Test
    fun anElementWithFourOrMoreJobsIsNotUnderEmployed() {
        val registry = ConveyEmploymentRegistry(enforceInDebug = false)

        registry.register(
            id = "button",
            jobs = setOf(ConveyJob.Invite, ConveyJob.Progress, ConveyJob.Confirm, ConveyJob.Interrupt),
            ambient = false,
        )

        assertEquals(0, registry.underEmployedCount)
        assertEquals(0, registry.ambientCount)
    }

    @Test
    fun anElementWithFewerThanFourJobsIsUnderEmployed() {
        val registry = ConveyEmploymentRegistry(enforceInDebug = false)

        registry.register(id = "label", jobs = setOf(ConveyJob.Report), ambient = false)

        assertEquals(1, registry.underEmployedCount)
    }

    @Test
    fun anAmbientElementIsNeverCountedAsUnderEmployedRegardlessOfJobCount() {
        val registry = ConveyEmploymentRegistry(enforceInDebug = false)

        registry.register(id = "spacer", jobs = emptySet(), ambient = true)

        assertEquals(0, registry.underEmployedCount)
        assertEquals(1, registry.ambientCount)
    }

    @Test
    fun unregisterRemovesAnElementFromBothCounts() {
        val registry = ConveyEmploymentRegistry(enforceInDebug = false)
        registry.register(id = "label", jobs = setOf(ConveyJob.Report), ambient = false)

        registry.unregister("label")

        assertEquals(0, registry.underEmployedCount)
    }

    @Test
    fun snapshotReportsBothCountsAndConfiguredLimits() {
        val registry = ConveyEmploymentRegistry(enforceInDebug = false, ambientBudget = 2)
        registry.register(id = "label", jobs = setOf(ConveyJob.Report), ambient = false)
        registry.register(id = "spacer", jobs = emptySet(), ambient = true)

        val snapshot = registry.snapshot()

        assertEquals(
            "ConveyEmployment Snapshot:\n" +
                "  Under-employed: 1  (min 4 jobs)\n" +
                "  Ambient:        1  (budget 2)\n",
            snapshot,
        )
    }
}
