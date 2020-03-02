package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.tokens.workflows.flows.rpc.UpdateEvolvableToken
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByService
@StartableByRPC
class UpdateCordaCoinTypeFlow(
        private val coinTypeId: UniqueIdentifier,
        private val newCoinType: CordaCoinType
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val stateRef = vaultServiceUtils.getVaultStates<CordaCoinType>(coinTypeId).single()
        return subFlow(UpdateEvolvableToken(stateRef, newCoinType))
    }
}

@InitiatedBy(UpdateCordaCoinTypeFlow::class)
class UpdateCordaCoinTypeFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
    }
}
