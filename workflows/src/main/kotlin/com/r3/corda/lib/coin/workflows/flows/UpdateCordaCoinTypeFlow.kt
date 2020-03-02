package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal

@InitiatingFlow
@StartableByService
@StartableByRPC
class UpdateCordaCoinTypeFlow(
        private val coinTypeId: UniqueIdentifier,
        private val name: String = "",
        private val nav: BigDecimal = BigDecimal.ZERO,
        private val newObservers: List<Party> = emptyList()
) : FlowLogic<SignedTransaction>() {

    constructor(coinTypeId: UniqueIdentifier, newCoinType: CordaCoinType, newObservers: List<Party> = emptyList())
            : this(coinTypeId = coinTypeId,
                    name = newCoinType.name,
                    nav = newCoinType.nav,
                    newObservers = newObservers)

    @Suspendable
    override fun call(): SignedTransaction {

        require(nav > BigDecimal.ZERO, {"Nav must be positive"})
        require(name.isNotEmpty() || nav > BigDecimal.ZERO || newObservers.isNotEmpty(), {"Nothing to update"})

        val stateRef = vaultServiceUtils.getVaultStates<CordaCoinType>(coinTypeId).single()
        val state = stateRef.state.data

        val newCoinType = state.copy(
                name = if (name.isEmpty()) state.name else name,
                nav = if (nav == BigDecimal.ZERO) state.nav else nav
        )
        val observerSessions = newObservers.map { initiateFlow(it) }
        return subFlow(UpdateEvolvableTokenFlow(
                oldStateAndRef = stateRef,
                newState = newCoinType,
                participantSessions = emptyList(),

                // mind: you cannot remove observers from existing list, you can only add or keep
                observerSessions = observerSessions)
        )
    }
}

@InitiatedBy(UpdateCordaCoinTypeFlow::class)
class UpdateCordaCoinTypeFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(UpdateEvolvableTokenFlowHandler(otherSession))
    }
}
