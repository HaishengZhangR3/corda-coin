package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.contracts.states.CordaCoin
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.issue.ConfidentialIssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.issue.ConfidentialIssueTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlowHandler
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal

// If you only issue one token one time, refer to this flow: IssueCordaCoinFlow
// If you want to issue multiple tokens at a time, refer to: IssueCordaCoinsFlow
// If you want to issue token confidentially, refer to this flow: ConfidentialIssueCordaCoinFlow

// Those flows are very similar, but I purposely did not re-use the code for an easier reference

@InitiatingFlow
@StartableByService
@StartableByRPC
class IssueCordaCoinFlow(
        private val coinTypeId: UniqueIdentifier,
        private val amount: BigDecimal,
        private val holder: Party,
        private val note: String,
        private val participants: List<Party>,
        private val observers: List<Party>
) : FlowLogic<SignedTransaction>() {

    constructor(coinTypeId: UniqueIdentifier, amount: BigDecimal, holder: Party)
            : this (coinTypeId = coinTypeId, amount = amount, holder = holder, note = "",
                participants = emptyList(), observers = emptyList())

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

        // If you are issuing to self, there is no need to pass in a flow session.
        return when{
            holder.equals(ourIdentity) -> subFlow(IssueTokensFlow(coin))
            else -> {
                val participantsSession = (participants + holder).toSet().map { initiateFlow(it) }
                val observersSession = observers.map { initiateFlow(it) }
                subFlow(IssueTokensFlow(coin, participantsSession, observersSession))
            }
        }
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


@InitiatingFlow
@StartableByService
@StartableByRPC
class IssueCordaCoinsFlow(
        private val coinTypeId: UniqueIdentifier,
        private val amounts: List<BigDecimal>,
        private val holders: List<Party>,
        private val note: String,
        private val participants: List<Party>,
        private val observers: List<Party>
) : FlowLogic<SignedTransaction>() {

    constructor(coinTypeId: UniqueIdentifier, amounts: List<BigDecimal>, holders: List<Party>)
            : this (coinTypeId = coinTypeId, amounts = amounts, holders = holders,
            note = "", participants = emptyList(), observers = emptyList())

    @Suspendable
    override fun call(): SignedTransaction {
        val coinTypeStateRef = vaultServiceUtils.getVaultStates<CordaCoinType>(coinTypeId).single()
        val coinType = coinTypeStateRef.state.data
        val coinTypePointer = coinType.toPointer<CordaCoinType>()

        require(amounts.size == holders.size) {"Amounts should match holders size."}

        val amountAndHolder: MutableList<CordaCoin> = mutableListOf()
        for (i in amounts.indices) {
            val issuedAmount = amounts[i] of coinTypePointer issuedBy ourIdentity
            val coin = CordaCoin(
                    amount = issuedAmount,
                    holder = holders[i],
                    note = note
            )
            amountAndHolder.add(coin)
        }

        // If you are issuing to self, there is no need to pass in a flow session.
        val uniqueHolders = holders.toSet()
        return when{
            // only issue to myself
            (uniqueHolders.size == 1) && holders.first().equals(ourIdentity) ->
                subFlow(IssueTokensFlow(amountAndHolder, emptyList(), emptyList()))

            // issue to others
            else -> {
                val participantsSession = (participants + uniqueHolders).map { initiateFlow(it) }
                val observersSession = observers.map { initiateFlow(it) }
                subFlow(IssueTokensFlow(amountAndHolder, participantsSession, observersSession))
            }
        }
    }
}

@Suppress("unused")
@InitiatedBy(IssueCordaCoinsFlow::class)
class IssueCordaCoinsFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(IssueTokensFlowHandler(flowSession))
    }
}

@InitiatingFlow
@StartableByService
@StartableByRPC
class ConfidentialIssueCordaCoinFlow(
        private val coinTypeId: UniqueIdentifier,
        private val amount: BigDecimal,
        private val holder: Party,
        private val note: String,
        private val participants: List<Party>,
        private val observers: List<Party>
) : FlowLogic<SignedTransaction>() {

    constructor(coinTypeId: UniqueIdentifier, amount: BigDecimal, holder: Party)
            : this (coinTypeId = coinTypeId, amount = amount, holder = holder, note = "",
            participants = emptyList(), observers = emptyList())

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

        // You'd see that both ConfidentialIssueTokensFlow and IssueTokensFlow are quite simila,
        // the two differences you'd see are:
        //    - No matter whether you are issuing to self or others, you must pass the participants session in Confidential version
        //    - After issuance, the coin holder is AnonymousParty (in Confidential version) instead of Party (in non-Confidential version)
        val participantsSession = (participants + holder).toSet().map { initiateFlow(it) }
        val observersSession = observers.map { initiateFlow(it) }
        return subFlow(ConfidentialIssueTokensFlow(coin, participantsSession, observersSession))
    }
}

@Suppress("unused")
@InitiatedBy(ConfidentialIssueCordaCoinFlow::class)
class ConfidentialIssueCordaCoinFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ConfidentialIssueTokensFlowHandler(flowSession))
    }
}
