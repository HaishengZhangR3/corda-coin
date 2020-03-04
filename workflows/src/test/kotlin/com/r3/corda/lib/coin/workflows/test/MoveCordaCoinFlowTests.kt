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

    @Test
    fun `normal test 2`() {
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
                moveHolder = partyB,
                changeHolder = partyA
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

    @Test
    fun `confidential 1`() {
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

        val moveFlow = nodeA.startFlow(ConfidentialMoveCordaCoinFlow(
                amount = BigDecimal(30),
                moveHolder = partyB
        ))
        network.runNetwork()
        moveFlow.getOrThrow()

        val coina = singleCoin(nodeA)
        val coinb = singleCoin(nodeB)

        assertBigDecimalEqual(BigDecimal(70), coina.amount.toDecimal())
        assertBigDecimalEqual(BigDecimal(30), coinb.amount.toDecimal())

        // key difference between Confidential and non-Confidential version:
        assert(coina.holder !is Party && coina.holder is AnonymousParty)
        assert(coinb.holder !is Party && coinb.holder is AnonymousParty)
    }

    @Test
    fun `queryCriteria test`() {
        val createFlow = nodeA.startFlow(CreateCordaCoinTypeFlow(
                name = "subject",
                nav = BigDecimal(8)
        ))
        network.runNetwork()
        val typeId = createFlow.getOrThrow()

        val howManyCoins = 10
        var amounts: MutableList<BigDecimal> = mutableListOf()
        var parties: MutableList<Party> = mutableListOf()
        for (i in 1..howManyCoins) {
            amounts.add(BigDecimal(i))
            parties.add(partyA)
        }

        val issueFlow = nodeA.startFlow(IssueCordaCoinsFlow(
                coinTypeId = typeId.linearId,
                amounts = amounts.toList(),
                holders = parties.toList()
        ))
        network.runNetwork()
        issueFlow.getOrThrow()

        val coinsa = coins(nodeA)
        assert(coinsa.size == howManyCoins)

        val moveFlow = nodeA.startFlow(CriteriaMoveCordaCoinFlow(
                amount = BigDecimal(30),
                moveHolder = partyB,
                selectAmountRange = Pair(BigDecimal(3), BigDecimal(9))
        ))
        network.runNetwork()
        moveFlow.getOrThrow()

        val newCoinsa = coins(nodeA)
        val newCoinsb = coins(nodeB)


        // We've issued 55 in total to A: (1+2+...+10), plan to select [3,8],
        // After moving, A should still have: 1, 2, 9, 10; and 3 (new created or existing)
        // After moving, B should have: 30

        assert(newCoinsa.size == 5)
        assert(newCoinsb.size == 1)

        assertBigDecimalEqual(newCoinsb.single().state.data.amount.toDecimal(), BigDecimal(30))

        val holderCheckA = newCoinsa.map { it.state.data.holder is Party && it.state.data.holder !is AnonymousParty }.toSet().single()
        assert(holderCheckA)

        val holderCheckB = newCoinsb.map { it.state.data.holder is Party && it.state.data.holder !is AnonymousParty }.toSet().single()
        assert(holderCheckB == true)
    }

}
