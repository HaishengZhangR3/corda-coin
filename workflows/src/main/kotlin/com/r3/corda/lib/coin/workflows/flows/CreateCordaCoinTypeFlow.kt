package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal

@InitiatingFlow
@StartableByService
@StartableByRPC
class CreateCordaCoinTypeFlow(
        private val name: String,
        private val nav: BigDecimal
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = vaultServiceUtils.notary()

        val coinType = CordaCoinType (
                name = name,
                admin = ourIdentity,
                nav = nav
        )

        val transactionState = coinType withNotary notary
        return subFlow(CreateEvolvableTokens(
                transactionState = transactionState,
                observers = emptyList()))
    }
}


@InitiatedBy(CreateCordaCoinTypeFlow::class)
class CreateCordaCoinTypeFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
    }
}
