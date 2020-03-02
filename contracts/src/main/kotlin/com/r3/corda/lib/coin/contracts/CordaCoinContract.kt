package com.r3.corda.lib.coin.contracts

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract

class CordaCoinContract : FungibleTokenContract() {
    companion object {
        @JvmStatic
        val ID = CordaCoinTypeContract::class.qualifiedName!!
    }

    // here you'd choose to override the verifying method over FungibleTokenContract
    // override fun verifyIssue
    // override fun verifyMove
    // override fun verifyRedeem

}
