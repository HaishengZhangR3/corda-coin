package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemTokensFlowHandler
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal

@InitiatingFlow
@StartableByService
@StartableByRPC
class RedeemCordaCoinFlow(
        private val amount: BigDecimal,
        private val issuer: Party
 ) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val coinTypeStateRef = vaultServiceUtils.getVaultState<CordaCoinType>()
        val coinType = coinTypeStateRef.state.data
        val coinTypePointer = coinType.toPointer<CordaCoinType>()
        val amountAndType = amount of coinTypePointer

        val issuerSession = initiateFlow(issuer)
        return subFlow(RedeemFungibleTokensFlow(
                amount = amountAndType,
                issuerSession = issuerSession)
        )
    }
}

@Suppress("unused")
@InitiatedBy(RedeemCordaCoinFlow::class)
class RedeemCordaCoinFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(RedeemTokensFlowHandler(flowSession))
    }
}
