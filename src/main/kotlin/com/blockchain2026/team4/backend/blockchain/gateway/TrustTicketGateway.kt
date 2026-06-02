package com.blockchain2026.team4.backend.blockchain.gateway

import com.blockchain2026.team4.backend.blockchain.dto.BlockchainSubmission
import com.blockchain2026.team4.backend.blockchain.dto.ContractEventCommand
import java.math.BigInteger

interface TrustTicketGateway {
    fun addOrganizer(organizerWallet: String): BlockchainSubmission

    fun confirmOrganizerAdded(organizerWallet: String, transactionHash: String): BlockchainSubmission

    fun addValidator(validatorWallet: String): BlockchainSubmission

    fun addEventValidator(contractEventId: BigInteger, validatorWallet: String): BlockchainSubmission

    fun createEvent(command: ContractEventCommand): BlockchainSubmission

    fun setEventStatus(contractEventId: BigInteger, active: Boolean): BlockchainSubmission

    fun mintTicket(contractEventId: BigInteger, seatInfo: String): BlockchainSubmission

    fun purchaseTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission

    fun confirmPrimaryPurchase(
        contractTokenId: BigInteger,
        buyerWallet: String,
        transactionHash: String,
    ): BlockchainSubmission

    fun listTicket(contractTokenId: BigInteger, resalePriceWei: BigInteger): BlockchainSubmission

    fun confirmTicketListed(
        contractTokenId: BigInteger,
        sellerWallet: String,
        resalePriceWei: BigInteger,
        transactionHash: String,
    ): BlockchainSubmission

    fun purchaseResaleTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission

    fun confirmResalePurchase(
        contractTokenId: BigInteger,
        sellerWallet: String,
        buyerWallet: String,
        valueWei: BigInteger,
        transactionHash: String,
    ): BlockchainSubmission

    fun cancelListing(contractTokenId: BigInteger): BlockchainSubmission

    fun confirmListingCanceled(
        contractTokenId: BigInteger,
        sellerWallet: String,
        transactionHash: String,
    ): BlockchainSubmission

    fun useTicket(contractTokenId: BigInteger): BlockchainSubmission

    fun verifySignedTicket(
        contractTokenId: BigInteger,
        claimedOwner: String,
        expiresAtEpochSeconds: BigInteger,
        signature: String,
    ): Boolean

    fun getTicketCheckInMessageHash(
        contractTokenId: BigInteger,
        claimedOwner: String,
        expiresAtEpochSeconds: BigInteger,
    ): String
}
