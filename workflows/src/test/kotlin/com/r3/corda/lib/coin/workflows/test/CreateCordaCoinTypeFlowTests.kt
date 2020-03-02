package com.r3.corda.lib.coin.workflows.test

import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.coin.workflows.flows.CreateCordaCoinTypeFlow
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CreateCordaCoinTypeFlowTests {

    lateinit var network: MockNetwork
    lateinit var nodeA: StartedMockNode
    lateinit var nodeB: StartedMockNode

    @Before
    fun setup() {
        network = Utils.createMockNetwork()
        nodeA = network.createPartyNode()
        nodeB = network.createPartyNode()

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `test 1`() {

        val flow = nodeA.startFlow(CreateCordaCoinTypeFlow(
                name = "subject",
                nav = BigDecimal(8)
        ))
        network.runNetwork()
        flow.getOrThrow()

        //check whether the created one in node B is same as that in the DB of host node A
        val type = nodeA.services.vaultService.queryBy(CordaCoinType::class.java).states.single().state.data

        // same chat session in two nodes should have same participants
        assert(BigDecimal(8).equals(type.nav))
    }
}
