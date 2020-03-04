package com.r3.corda.lib.coin.workflows.test

import com.r3.corda.lib.coin.workflows.flows.ConfidentialIssueCordaCoinFlow
import com.r3.corda.lib.coin.workflows.flows.CreateCordaCoinTypeFlow
import com.r3.corda.lib.coin.workflows.flows.IssueCordaCoinFlow
import com.r3.corda.lib.coin.workflows.flows.IssueCordaCoinsFlow
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

class IssueCordaCoinFlowTests {

    lateinit var network: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode
    lateinit var nodeC: StartedMockNode
    lateinit var nodeD: StartedMockNode

    @Before
    fun setup() {
        network = Utils.createMockNetwork()
        nodeA = network.createPartyNode()
        nodeB = network.createPartyNode()
        nodeC = network.createPartyNode()
        nodeD = network.createPartyNode()

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

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
                holder = nodeA.info.legalIdentities.first()
        ))
        network.runNetwork()
        issueFlow.getOrThrow()

        //check whether the created one is right
        val coin = nodeA.services.vaultService.queryBy(FungibleToken::class.java).states.single().state.data

        assert(0 == amount.compareTo(coin.amount.toDecimal()),
                {"${amount}, ${coin.amount.toDecimal()}"})
        assert(coin.amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()),
                {"${coin.amount.token.tokenType.tokenIdentifier}, ${typeId.linearId}"})

        assert(coin.holder is Party)
        assert(coin.holder !is AnonymousParty)
    }

    @Test
    fun `normal test 2`() {

        val createFlow = nodeA.startFlow(CreateCordaCoinTypeFlow(
                name = "subject",
                nav = BigDecimal(8)
        ))
        network.runNetwork()
        val typeId = createFlow.getOrThrow()
        val amount = BigDecimal(1200)

        val issueFlow = nodeA.startFlow(IssueCordaCoinFlow(
                coinTypeId = typeId.linearId,
                amount = amount,
                holder = nodeB.info.legalIdentities.first(),
                participants = listOf(nodeB.info.legalIdentities.first(),
                        nodeC.info.legalIdentities.first()),
                observers = listOf(nodeD.info.legalIdentities.first())
        ))
        network.runNetwork()
        issueFlow.getOrThrow()

        // in B
        val coinb = nodeB.services.vaultService.queryBy(FungibleToken::class.java).states.single().state.data
        assert(0 == amount.compareTo(coinb.amount.toDecimal()),
                {"${amount}, ${coinb.amount.toDecimal()}"})
        assert(coinb.amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()))

        // in C
        val coinc = nodeC.services.vaultService.queryBy(FungibleToken::class.java).states.single().state.data
        assert(0 == amount.compareTo(coinb.amount.toDecimal()),
                {"${amount}, ${coinb.amount.toDecimal()}"})
        assert(coinc.amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()))

        // in D
        val coind = nodeD.services.vaultService.queryBy(FungibleToken::class.java).states.single().state.data
        assert(0 == amount.compareTo(coind.amount.toDecimal()),
                {"${amount}, ${coind.amount.toDecimal()}"})
        assert(coind.amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()))

    }

    @Test
    fun confidential_test() {
        val createFlow = nodeA.startFlow(CreateCordaCoinTypeFlow(
                name = "subject",
                nav = BigDecimal(8)
        ))
        network.runNetwork()
        val typeId = createFlow.getOrThrow()
        val amount = BigDecimal(100.00)

        val issueFlow = nodeA.startFlow(ConfidentialIssueCordaCoinFlow(
                coinTypeId = typeId.linearId,
                amount = amount,
                holder = nodeA.info.legalIdentities.first()
        ))
        network.runNetwork()
        issueFlow.getOrThrow()

        val coin = nodeA.services.vaultService.queryBy(FungibleToken::class.java).states.single().state.data

        assert(0 == amount.compareTo(coin.amount.toDecimal()),
                {"${amount}, ${coin.amount.toDecimal()}"})
        assert(coin.amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()),
                {"${coin.amount.token.tokenType.tokenIdentifier}, ${typeId.linearId}"})

        assert(coin.holder !is Party)
        assert(coin.holder is AnonymousParty)

    }

    @Test
    fun `list test 1`() {

        val createFlow = nodeA.startFlow(CreateCordaCoinTypeFlow(
                name = "subject",
                nav = BigDecimal(8)
        ))
        network.runNetwork()
        val typeId = createFlow.getOrThrow()
        val amounts = listOf(BigDecimal(100), BigDecimal(200)).sorted()
        val holders = listOf(nodeA.info.legalIdentities.first(), nodeB.info.legalIdentities.first())

        val issueFlow = nodeA.startFlow(IssueCordaCoinsFlow(
             coinTypeId = typeId.linearId,
             amounts = amounts,
             holders = holders,
             participants = listOf(nodeC.info.legalIdentities.first()),
             observers = listOf(nodeD.info.legalIdentities.first())
        ))
        network.runNetwork()
        issueFlow.getOrThrow()

        // in A
        val coina = getStates(nodeA)
        val amountsa = coina.map { it.amount.toDecimal() }.first()
        assert(0 == amounts[0].compareTo(amountsa), {"${amounts[0]}, ${amountsa}"})
        assert(coina[0].amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()))

        // in B
        val coinb = getStates(nodeB)
        val amountsb = coinb.map { it.amount.toDecimal() }.first()
        assert(0 == amounts[1].compareTo(amountsb), {"${amounts[1]}, ${amountsb}"})
        assert(coinb[0].amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()))

        // in C
        val coinc = getStates(nodeC)
        val amountsc = coinc.map { it.amount.toDecimal() }.sorted()
        assertEquals(amountsc.size, amounts.size)

        for (i in amounts.indices){
            assert(0 == amounts[i].compareTo(amountsc[i]), {"${amounts[i]}, ${amountsc[i]}"})
            assert(coinc[i].amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()))
        }

        // in D
        val coind = getStates(nodeD)
        val amountsd = coind.map { it.amount.toDecimal() }.sorted()
        assertEquals(amountsd.size, amounts.size)

        for (i in amounts.indices){
            assert(0 == amounts[i].compareTo(amountsd[i]), {"${amounts[i]}, ${amountsd[i]}"})
            assert(coind[i].amount.token.tokenType.tokenIdentifier.equals(typeId.linearId.toString()))
        }
    }

    private fun getStates(node: StartedMockNode): List<FungibleToken> {
        val statesAndRef = node.services.vaultService.queryBy(FungibleToken::class.java).states
        return statesAndRef.map { it.state.data }
    }

}
