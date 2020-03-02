package com.r3.corda.lib.coin.contracts

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.TokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.*

class CordaCoinContract : FungibleTokenContract(), Contract {
    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }

    // here you'd choose to override the verifying method over FungibleTokenContract
    override fun verifyIssue(
            issueCommand: CommandWithParties<TokenCommand>,
            inputs: List<IndexedState<FungibleToken>>,
            outputs: List<IndexedState<FungibleToken>>,
            attachments: List<Attachment>,
            references: List<StateAndRef<ContractState>>
    ) = super.verifyIssue(issueCommand, inputs, outputs, attachments, references)

    override fun verifyMove(
            moveCommands: List<CommandWithParties<TokenCommand>>,
            inputs: List<IndexedState<FungibleToken>>,
            outputs: List<IndexedState<FungibleToken>>,
            attachments: List<Attachment>,
            references: List<StateAndRef<ContractState>>
    ) = super.verifyMove(moveCommands, inputs, outputs, attachments, references)

    override fun verifyRedeem(
            redeemCommand: CommandWithParties<TokenCommand>,
            inputs: List<IndexedState<FungibleToken>>,
            outputs: List<IndexedState<FungibleToken>>,
            attachments: List<Attachment>,
            references: List<StateAndRef<ContractState>>
    ) = super.verifyRedeem(redeemCommand, inputs, outputs, attachments, references)
}
