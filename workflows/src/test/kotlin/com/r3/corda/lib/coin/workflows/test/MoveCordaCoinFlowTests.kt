package com.r3.corda.lib.coin.workflows.test

import com.r3.corda.lib.coin.workflows.flows.*
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
import kotlin.test.assertEquals

class MoveCordaCoinFlowTests {

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
                holder = partyA
        ))
        network.runNetwork()
        issueFlow.getOrThrow()

        val moveFlow = nodeA.startFlow(MoveCordaCoinFlow(
                amount = BigDecimal(30),
                moveHolder = partyB
        ))
        network.runNetwork()
        moveFlow.getOrThrow()

        val coina = singleCoin(nodeA)
        val coinb = singleCoin(nodeB)

        assertBigDecimalEqual(BigDecimal(70), coina.amount.toDecimal())
        assertBigDecimalEqual(BigDecimal(30), coinb.amount.toDecimal())

        assert(coina.holder is Party && coina.holder !is AnonymousParty)
        assert(coinb.holder is Party && coinb.holder !is AnonymousParty)
    }
}
