package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.contracts.states.CordaCoin
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal

@InitiatingFlow
@StartableByService
@StartableByRPC
class IssueCordaCoinFlow(
        private val coinTypeId: UniqueIdentifier,
        private val holder: Party,
        private val amount: BigDecimal,
        private val note: String
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val coinTypeStateRef = vaultServiceUtils.getVaultStates<CordaCoinType>(coinTypeId).single()
        val coinType = coinTypeStateRef.state.data

        val coinTypePointer = coinType.toPointer<CordaCoinType>()
        val issuedAmount = amount of coinTypePointer issuedBy ourIdentity

        val coin = CordaCoin(
                amount = issuedAmount,
                holder = holder,
                note = note
        )
        val initFlow = initiateFlow(holder)
        return subFlow(IssueTokensFlow(coin, listOf(initFlow), emptyList()))
    }
}

@Suppress("unused")
@InitiatedBy(IssueCordaCoinFlow::class)
class IssueCordaCoinFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(IssueTokensFlowHandler(flowSession))
    }
}
