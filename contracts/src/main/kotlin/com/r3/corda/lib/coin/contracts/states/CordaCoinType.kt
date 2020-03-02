package com.r3.corda.lib.coin.contracts.states

import com.r3.corda.lib.coin.contracts.CordaCoinTypeContract
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@CordaSerializable
@BelongsToContract(CordaCoinTypeContract::class)
data class CordaCoinType(
        val name: String,
        val admin: Party,
        val nav: BigDecimal,
        val created: Instant = Instant.now()
) : EvolvableTokenType(), LinearState {

    override val linearId: UniqueIdentifier = UniqueIdentifier.fromString(UUID.randomUUID().toString())
    override val maintainers: List<Party> get() = listOf(admin)
    override val participants: List<AbstractParty> get() = listOf(admin)
    override val fractionDigits = 2

    override fun toString(): String {
        return "CordaCoinType(linearId=$linearId, name='$name', created=$created, admin=$admin, nav=$nav, fractionDigits=$fractionDigits)"
    }

}
