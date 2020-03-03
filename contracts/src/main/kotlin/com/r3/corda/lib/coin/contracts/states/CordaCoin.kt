package com.r3.corda.lib.coin.contracts.states

import com.r3.corda.lib.coin.contracts.CordaCoinContract
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
@BelongsToContract(CordaCoinContract::class)
class CordaCoin(
        override val amount: Amount<IssuedTokenType>,
        override val holder: AbstractParty,
        val note: String = ""
) : FungibleToken(
        amount = amount,
        holder = holder
) {
    override fun toString(): String {
        return "CordaCoin(amount=$amount, holder=$holder, note='$note')"
    }

    // ConfidentialIssueTokensFlow will call ConfidentialTokensFlow, which
    // will duplicate a new FungibleToken instance using:
    //          token.withNewHolder(anonymousParty)
    // so, I have to override withNewHolder to generate a CordaCoin instance
    // instead of FungibleToken instance
    override fun withNewHolder(newHolder: AbstractParty): FungibleToken {
        return CordaCoin(amount = amount, holder = newHolder, note = note)
    }

}
