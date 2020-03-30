package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.evolvable.CreateEvolvableTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.CreateEvolvableTokensFlowHandler
import net.corda.core.flows.*
import net.corda.core.identity.Party
import java.math.BigDecimal

@InitiatingFlow
@StartableByService
@StartableByRPC
class CreateCordaCoinTypeFlow(
        private val name: String,
        private val nav: BigDecimal,
        private val observers: List<Party> = emptyList()
) : FlowLogic<CordaCoinType>() {

    constructor() : this(name = "Corda Coin", nav = BigDecimal.ONE)
    constructor(nav: BigDecimal) : this(name = "Corda Coin", nav = nav)
    constructor(name: String, nav: BigDecimal) : this(name = name, nav = nav, observers = emptyList())

    @Suspendable
    override fun call(): CordaCoinType {
        logger.info("Select the notary in your network.")
        val notary = vaultServiceUtils.notary()

        logger.info("Construct coin type to be created.")
        val coinType = CordaCoinType (
                name = name,
                admin = ourIdentity,
                nav = nav
        )

        logger.info("Call subFlow to do it.")
        val transactionState = coinType withNotary notary
        val observerSessions = observers.map { initiateFlow(it) }
        val signedTxn = subFlow(CreateEvolvableTokensFlow(
                transactionState = transactionState,
                participantSessions = emptyList(),
                observerSessions = observerSessions))

        logger.info("All done! Just return the coin type we created.")
        return signedTxn.coreTransaction.outRefsOfType<CordaCoinType>().single().state.data
    }
}


@InitiatedBy(CreateCordaCoinTypeFlow::class)
class CreateCordaCoinTypeFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(CreateEvolvableTokensFlowHandler(otherSession))
    }
}
