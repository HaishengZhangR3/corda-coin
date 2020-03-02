package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
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
        val notary = vaultServiceUtils.notary()

        val coinType = CordaCoinType (
                name = name,
                admin = ourIdentity,
                nav = nav
        )

        val transactionState = coinType withNotary notary
        val signedTxn = subFlow(CreateEvolvableTokens(
                transactionState = transactionState,
                observers = observers))

        return signedTxn.coreTransaction.outRefsOfType<CordaCoinType>().single().state.data
    }
}


@InitiatedBy(CreateCordaCoinTypeFlow::class)
class CreateCordaCoinTypeFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
    }
}
