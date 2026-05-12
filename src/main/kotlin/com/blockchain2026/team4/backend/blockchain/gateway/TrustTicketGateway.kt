package com.blockchain2026.team4.backend.blockchain.gateway

import com.blockchain2026.team4.backend.blockchain.dto.BlockchainSubmission
import com.blockchain2026.team4.backend.blockchain.dto.ContractEventCommand
import java.math.BigInteger

interface TrustTicketGateway {
    fun addOrganizer(organizerWallet: String): BlockchainSubmission

    fun addEventValidator(contractEventId: BigInteger, validatorWallet: String): BlockchainSubmission

    fun createEvent(command: ContractEventCommand): BlockchainSubmission

    fun setEventStatus(contractEventId: BigInteger, active: Boolean): BlockchainSubmission

    fun mintTicket(contractEventId: BigInteger, seatInfo: String): BlockchainSubmission

    fun purchaseTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission

    fun listTicket(contractTokenId: BigInteger, resalePriceWei: BigInteger): BlockchainSubmission

    fun purchaseResaleTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission

    fun cancelListing(contractTokenId: BigInteger): BlockchainSubmission

    fun useTicket(contractTokenId: BigInteger): BlockchainSubmission

    fun verifySignedTicket(
        contractTokenId: BigInteger,
        claimedOwner: String,
        expiresAtEpochSeconds: BigInteger,
        signature: String,
    ): Boolean
}
