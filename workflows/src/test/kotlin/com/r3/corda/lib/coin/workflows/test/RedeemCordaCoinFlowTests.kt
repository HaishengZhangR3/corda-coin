package com.r3.corda.lib.coin.workflows.test

import com.r3.corda.lib.coin.workflows.flows.CreateCordaCoinTypeFlow
import com.r3.corda.lib.coin.workflows.flows.IssueCordaCoinFlow
import com.r3.corda.lib.coin.workflows.flows.RedeemCordaCoinFlow
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertTrue

class RedeemCordaCoinFlowTests {

    lateinit var network: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode
    lateinit var nodeC: StartedMockNode

    lateinit var partyA: Party
    lateinit var partyB: Party
    lateinit var partyC: Party

    @Before
    fun setup() {
        network = Utils.createMockNetwork()
        nodeA = network.createPartyNode()
        nodeB = network.createPartyNode()
        nodeC = network.createPartyNode()

        partyA = nodeA.info.legalIdentities.first()
        partyB = nodeB.info.legalIdentities.first()
        partyC = nodeC.info.legalIdentities.first()

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    fun assertBigDecimalEqual(a: BigDecimal, b: BigDecimal) {
        assert(0 == a.compareTo(b), {"${a}, ${b}"})
    }
    fun singleCoin(node: StartedMockNode) =
        node.services.vaultService.queryBy(FungibleToken::class.java).states.single().state.data

    fun coins(node: StartedMockNode) =
        node.services.vaultService.queryBy(FungibleToken::class.java).states

    @Test
    fun `normal test`() {
        val createFlow = nodeA.startFlow(CreateCordaCoinTypeFlow(
                name = "subject",
                nav = BigDecimal(8)
        ))
        network.runNetwork()
        val typeId = createFlow.getOrThrow()
        val amount = BigDecimal(100.00)

        val issueFlow = nodeA.startFlow(IssueCordaCoinFlow(
                coinTypeId = typeId.linearId,
                amount = amount,
                holder = partyB
        ))
        network.runNetwork()
        issueFlow.getOrThrow()

        val redeemFlow = nodeB.startFlow(RedeemCordaCoinFlow(
                amount = BigDecimal(30),
                issuer = partyA
        ))
        network.runNetwork()
        redeemFlow.getOrThrow()

        val coinsa = coins(nodeA)
        val coinsb = coins(nodeB)

        assertTrue { coinsa.size == 0 }
        assertTrue { coinsb.size == 1 }

        val coinb = coinsb.single().state.data
        assertBigDecimalEqual(BigDecimal(70), coinb.amount.toDecimal())
        assert(coinb.holder is Party && coinb.holder !is AnonymousParty)


        val redeemFlow2 = nodeB.startFlow(RedeemCordaCoinFlow(
                amount = BigDecimal(70),
                issuer = partyA
        ))
        network.runNetwork()
        redeemFlow2.getOrThrow()

        val coinsa2 = coins(nodeA)
        val coinsb2 = coins(nodeB)

        assertTrue { coinsa2.size == 0 }
        assertTrue { coinsb2.size == 0 }

    }
}
