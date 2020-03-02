package com.r3.corda.lib.coin.workflows.utils

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.*
import net.corda.core.serialization.SingletonSerializeAsToken

val FlowLogic<*>.vaultServiceUtils get() = serviceHub.cordaService(VaultServiceUtils::class.java)

@CordaService
class VaultServiceUtils(val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    fun notary() = serviceHub.networkMapCache.notaryIdentities.first()

    inline fun <reified T : LinearState> getVaultStates(id: UniqueIdentifier, status: StateStatus = StateStatus.UNCONSUMED): List<StateAndRef<T>> {
        val stateAndRefs = serviceHub.vaultService.queryBy<T>(
                criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id), status = status),
                paging = PageSpecification(DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE))
        return stateAndRefs.states
    }

    inline fun <reified T : LinearState> getVaultState(): StateAndRef<T> {
        val stateAndRefs = serviceHub.vaultService.queryBy<T>()
        return stateAndRefs.states.first()
    }
}
