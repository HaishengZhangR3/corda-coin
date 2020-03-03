package com.r3.corda.lib.coin.contracts

import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.math.BigDecimal

// Here, you must implement Contract interface, otherwise there'll be runtime error
class CordaCoinTypeContract : EvolvableTokenContract(), Contract {
    companion object {
        @JvmStatic
        val ID = CordaCoinTypeContract::class.qualifiedName!!
    }
    override fun verify(tx: LedgerTransaction) {
        super.verify(tx)
        require(true)
    }

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        requireThat {
            "Must be no input in creating coin type." using (tx.inputStates.size == 0)
            "Must be only one output in creating coin type." using (tx.outputStates.size == 1)
            "Coin type must match." using (tx.outputStates.single() is CordaCoinType)

            val coinType = tx.outputStates.single() as CordaCoinType
            "NAV must be positive value." using (coinType.nav > BigDecimal.ZERO)
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        requireThat {
            "Must be only one input in updating coin type." using (tx.inputStates.size == 1)
            "Must be only one output in updating coin type." using (tx.outputStates.size == 1)
            "Input coin type must match." using (tx.inputStates.single() is CordaCoinType)
            "Output coin type must match." using (tx.outputStates.single() is CordaCoinType)

            val coinType = tx.outputStates.single() as CordaCoinType
            "NAV must be positive value." using (coinType.nav > BigDecimal.ZERO)
        }
    }
}
