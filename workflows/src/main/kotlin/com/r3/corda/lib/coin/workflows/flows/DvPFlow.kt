package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*

@InitiatingFlow
@StartableByService
@StartableByRPC
class DvPFlow(
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        return Unit
    }
}

@Suppress("unused")
@InitiatedBy(DvPFlow::class)
class DvPFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
    }
}
