package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal

@InitiatingFlow
@StartableByService
@StartableByRPC
class MoveCordaCoinFlow(
        private val amount: BigDecimal
 ) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val coinTypeStateRef = vaultServiceUtils.getVaultState<CordaCoinType>()
        val coinType = coinTypeStateRef.state.data

        val coinTypePointer = coinType.toPointer<CordaCoinType>()
        val amountAndType = amount of coinTypePointer

        return subFlow(MoveFungibleTokens(
                amount = amountAndType,
                holder = ourIdentity)
        )
    }
}

@Suppress("unused")
@InitiatedBy(MoveCordaCoinFlow::class)
class MoveCordaCoinFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(MoveFungibleTokensHandler(flowSession))
    }
}
