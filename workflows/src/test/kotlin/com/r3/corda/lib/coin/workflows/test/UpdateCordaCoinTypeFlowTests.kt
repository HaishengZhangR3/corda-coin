package com.r3.corda.lib.coin.workflows.test

import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.coin.workflows.flows.CreateCordaCoinTypeFlow
import com.r3.corda.lib.coin.workflows.flows.UpdateCordaCoinTypeFlow
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class UpdateCordaCoinTypeFlowTests {

    lateinit var network: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode
    lateinit var nodeC: StartedMockNode

    @Before
    fun setup() {
        network = Utils.createMockNetwork()
        nodeA = network.createPartyNode()
        nodeB = network.createPartyNode()
        nodeC = network.createPartyNode()

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `should be possible to create a chat`() {

        val createFlow = nodeA.startFlow(CreateCordaCoinTypeFlow(
                name = "subject",
                nav = BigDecimal(8)
        ))
        network.runNetwork()
        val coinType = createFlow.getOrThrow()

        val updateFlow = nodeA.startFlow(UpdateCordaCoinTypeFlow(
                coinTypeId = coinType.linearId,
                name = "new suject",
                nav = BigDecimal(10),
                newObservers = listOf(nodeB.info.legalIdentities.first())
        ))
        network.runNetwork()
        updateFlow.getOrThrow()


        //check whether the created one in node B is same as that in the DB of host node A
        val typeA = nodeA.services.vaultService.queryBy(CordaCoinType::class.java).states.single().state.data

        // same chat session in two nodes should have same participants
        assert(BigDecimal(10).equals(typeA.nav))

        //check whether the created one in node B is same as that in the DB of host node A
        val typeB = nodeB.services.vaultService.queryBy(CordaCoinType::class.java).states.single().state.data

        // same chat session in two nodes should have same participants
        assert(BigDecimal(10).equals(typeB.nav))
    }
}
