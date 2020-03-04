package com.r3.corda.lib.coin.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.coin.contracts.states.CordaCoinType
import com.r3.corda.lib.coin.workflows.utils.vaultServiceUtils
import com.r3.corda.lib.tokens.contracts.internal.schemas.PersistentFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.move.ConfidentialMoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.move.ConfidentialMoveTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.flows.move.MoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.move.MoveTokensFlowHandler
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import java.math.BigDecimal


// If you move token, refer to this flow: MoveCordaCoinFlow
// If you want move token confidentially, refer to this flow: ConfidentialMoveCordaCoinFlow
// If you specify query criteria when moving, refer to CriteriaMoveCordaCoinFlow

// As alwsya, those flows are very similar, but I purposely did not re-use the code for an easier reference

@InitiatingFlow
@StartableByService
@StartableByRPC
class MoveCordaCoinFlow(
        private val amount: BigDecimal,
        private val moveHolder: Party,
        private val changeHolder: Party?
) : FlowLogic<SignedTransaction>() {

    constructor(amount: BigDecimal, moveHolder: Party)
        : this(amount = amount, moveHolder = moveHolder, changeHolder = null)
    @Suspendable
    override fun call(): SignedTransaction {

        val coinTypeStateRef = vaultServiceUtils.getVaultState<CordaCoinType>()
        val coinType = coinTypeStateRef.state.data
        val coinTypePointer = coinType.toPointer<CordaCoinType>()
        val amountAndType = amount of coinTypePointer

        val partyAndAmount = PartyAndAmount(moveHolder, amountAndType)
        val participantSessions = listOf(moveHolder).toSet().map { initiateFlow(it) }

        // changeHolder must reside in the same node as the caller
        return subFlow(MoveFungibleTokensFlow(
                partyAndAmount = partyAndAmount,
                participantSessions = participantSessions,
                changeHolder = changeHolder as AbstractParty?)
        )
    }
}

@Suppress("unused")
@InitiatedBy(MoveCordaCoinFlow::class)
class MoveCordaCoinFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(MoveTokensFlowHandler(flowSession))
    }
}


@InitiatingFlow
@StartableByService
@StartableByRPC
class ConfidentialMoveCordaCoinFlow(
        private val amount: BigDecimal,
        private val moveHolder: Party,
        private val changeHolder: Party?
) : FlowLogic<SignedTransaction>() {

    constructor(amount: BigDecimal, moveHolder: Party)
            : this(amount = amount, moveHolder = moveHolder, changeHolder = null)
    @Suspendable
    override fun call(): SignedTransaction {

        val coinTypeStateRef = vaultServiceUtils.getVaultState<CordaCoinType>()
        val coinType = coinTypeStateRef.state.data
        val coinTypePointer = coinType.toPointer<CordaCoinType>()
        val amountAndType = amount of coinTypePointer

        val finalChangeHolder = changeHolder ?: ourIdentity
        val partyAndAmount = PartyAndAmount(moveHolder, amountAndType)
        val participantSessions = listOf(moveHolder, finalChangeHolder).toSet().map { initiateFlow(it) }

        // To call Confidential version, you must specify a changeHolder,
        // which IMO should not design this way

        // changeHolder must reside in the same node as the caller
        return subFlow(ConfidentialMoveFungibleTokensFlow(
                partyAndAmount = partyAndAmount,
                participantSessions = participantSessions,
                changeHolder = finalChangeHolder)
        )
    }
}

@Suppress("unused")
@InitiatedBy(ConfidentialMoveCordaCoinFlow::class)
class ConfidentialMoveCordaCoinFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ConfidentialMoveTokensFlowHandler(flowSession))
    }
}


@InitiatingFlow
@StartableByService
@StartableByRPC
class CriteriaMoveCordaCoinFlow(
        private val amount: BigDecimal,
        private val moveHolder: Party,
        private val selectAmountRange: Pair<BigDecimal, BigDecimal> = Pair(BigDecimal.ZERO, BigDecimal.ZERO)
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {


        val coinTypeStateRef = vaultServiceUtils.getVaultState<CordaCoinType>()
        val coinType = coinTypeStateRef.state.data
        val coinTypePointer = coinType.toPointer<CordaCoinType>()
        val amountAndType = amount of coinTypePointer

        val partyAndAmount = PartyAndAmount(moveHolder, amountAndType)
        val participantSessions = listOf(moveHolder).toSet().map { initiateFlow(it) }

        // Specify sortDesc = true (descending) or false (ascending) in order to demonstrate queryCriteria
        val queryCriteria = criteria(
                token = coinTypePointer,
                holder = ourIdentity,
                selectAmountRange = selectAmountRange)

        // changeHolder must reside in the same node as the caller
        return subFlow(MoveFungibleTokensFlow(
                partyAndAmount = partyAndAmount,
                participantSessions = participantSessions,
                queryCriteria = queryCriteria
                )
        )
    }

    // Here is a simple demonstration to compose query criteria in order to select your favourite token.
    private fun criteria(token: TokenType, holder: AbstractParty, selectAmountRange: Pair<BigDecimal, BigDecimal>): QueryCriteria {


        val holderCriteria = QueryCriteria.VaultCustomQueryCriteria(
                builder {
                    PersistentFungibleToken::holder.equal(holder)
                }
        )
        val tokenClassCriteria = QueryCriteria.VaultCustomQueryCriteria(
                builder {
                    PersistentFungibleToken::tokenClass.equal(token.tokenClass)
                }
        )
        val tokenIdentifierCriteria = QueryCriteria.VaultCustomQueryCriteria(
                builder {
                    PersistentFungibleToken::tokenIdentifier.equal(token.tokenIdentifier)
                }
        )

        return when
            (selectAmountRange.first.compareTo(selectAmountRange.second) == 0
                    && selectAmountRange.first.compareTo(BigDecimal.ZERO) == 0) {

            true -> holderCriteria.and(tokenClassCriteria).and(tokenIdentifierCriteria)
            false -> {
                // Amount in DB is the [real amount] * [10^fractionDigits] (which is 2), therefore,
                // the amount in DB represents the amount passed in * 100
                // Also, the validation of range is quite simple:
                //      ignore the check if: start == end == 0
                //      pass in simply without other check
                val rangeCriteria = QueryCriteria.VaultCustomQueryCriteria(
                        builder {
                            PersistentFungibleToken::amount.between(
                                    selectAmountRange.first.multiply(BigDecimal(100)).longValueExact(),
                                    selectAmountRange.second.multiply(BigDecimal(100)).longValueExact()
                                    )
                        }
                )
                holderCriteria.and(tokenClassCriteria).and(tokenIdentifierCriteria).and(rangeCriteria)
            }
        }
    }
}

@Suppress("unused")
@InitiatedBy(CriteriaMoveCordaCoinFlow::class)
class CriteriaMoveCordaCoinFlowHandler(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(MoveTokensFlowHandler(flowSession))
    }
}

