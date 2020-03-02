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
}
