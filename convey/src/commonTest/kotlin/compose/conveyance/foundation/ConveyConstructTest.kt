package compose.conveyance.foundation

import compose.conveyance.ConveyWeight
import kotlin.test.Test
import kotlin.test.assertTrue

class ConveyConstructTest {

    @Test
    fun auditFlagsAMissingHero() {
        val registry = ConveyConstructRegistry()
        registry.register(ConstructEntry("Do a thing", ConveyWeight.Secondary, ConveyOutcome.Unspecified))
        assertTrue("NO HERO ELEMENT DECLARED" in registry.audit())
    }

    @Test
    fun auditListsTheDeclaredHero() {
        val registry = ConveyConstructRegistry()
        registry.register(
            ConstructEntry(
                "Advance to payment",
                ConveyWeight.Hero,
                ConveyOutcome.Navigate("checkout/payment"),
            ),
        )
        val report = registry.audit()
        assertTrue("Advance to payment" in report)
        assertTrue("navigate → checkout/payment" in report)
    }

    @Test
    fun auditFlagsAPrimaryElementWithNoDeclaredOutcome() {
        val registry = ConveyConstructRegistry()
        val entry = ConstructEntry("Filter results", ConveyWeight.Primary, ConveyOutcome.Unspecified)
        registry.register(entry)
        assertTrue("no declared outcome" in registry.audit())
    }

    @Test
    fun unregisterRemovesTheEntryFromTheAudit() {
        val registry = ConveyConstructRegistry()
        val entry = ConstructEntry("Temporary", ConveyWeight.Secondary, ConveyOutcome.Unspecified)
        registry.register(entry)
        registry.unregister(entry)
        assertTrue("Temporary" !in registry.audit())
    }
}
