package com.blockchain2026.team4.backend.blockchain.gateway

import com.blockchain2026.team4.backend.blockchain.dto.BlockchainSubmission
import com.blockchain2026.team4.backend.blockchain.dto.ContractEventCommand
import com.blockchain2026.team4.backend.blockchain.entity.BlockchainTransactionStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.web3j.crypto.Hash
import java.math.BigInteger
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@Component
@ConditionalOnProperty(prefix = "app.blockchain", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class NoopTrustTicketGateway : TrustTicketGateway {
    private val nextEventId = AtomicLong(1)
    private val nextTokenId = AtomicLong(1)

    override fun addOrganizer(organizerWallet: String): BlockchainSubmission = simulated("addOrganizer")

    override fun confirmOrganizerAdded(organizerWallet: String, transactionHash: String): BlockchainSubmission =
        simulated("addOrganizer", transactionHash = transactionHash)

    override fun addValidator(validatorWallet: String): BlockchainSubmission = simulated("addValidator")

    override fun addEventValidator(contractEventId: BigInteger, validatorWallet: String): BlockchainSubmission = simulated("addEventValidator")

    override fun createEvent(command: ContractEventCommand): BlockchainSubmission =
        simulated("createEvent", contractEventId = BigInteger.valueOf(nextEventId.getAndIncrement()))

    override fun setEventStatus(contractEventId: BigInteger, active: Boolean): BlockchainSubmission = simulated("setEventStatus")

    override fun cancelEvent(contractEventId: BigInteger): BlockchainSubmission = simulated("cancelEvent")

    override fun mintTicket(contractEventId: BigInteger, seatInfo: String): BlockchainSubmission =
        simulated("mintTicket", contractTokenId = BigInteger.valueOf(nextTokenId.getAndIncrement()))

    override fun purchaseTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission = simulated("purchaseTicket")

    override fun confirmPrimaryPurchase(
        contractTokenId: BigInteger,
        buyerWallet: String,
        transactionHash: String,
    ): BlockchainSubmission = simulated("purchaseTicket", transactionHash = transactionHash, contractTokenId = contractTokenId)

    override fun listTicket(contractTokenId: BigInteger, resalePriceWei: BigInteger): BlockchainSubmission = simulated("listTicket")

    override fun confirmTicketListed(
        contractTokenId: BigInteger,
        sellerWallet: String,
        resalePriceWei: BigInteger,
        transactionHash: String,
    ): BlockchainSubmission = simulated("listTicket", transactionHash = transactionHash, contractTokenId = contractTokenId)

    override fun purchaseResaleTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission = simulated("purchaseResaleTicket")

    override fun confirmResalePurchase(
        contractTokenId: BigInteger,
        sellerWallet: String,
        buyerWallet: String,
        valueWei: BigInteger,
        transactionHash: String,
    ): BlockchainSubmission = simulated("purchaseResaleTicket", transactionHash = transactionHash, contractTokenId = contractTokenId)

    override fun cancelListing(contractTokenId: BigInteger): BlockchainSubmission = simulated("cancelListing")

    override fun confirmListingCanceled(
        contractTokenId: BigInteger,
        sellerWallet: String,
        transactionHash: String,
    ): BlockchainSubmission = simulated("cancelListing", transactionHash = transactionHash, contractTokenId = contractTokenId)

    override fun useTicket(contractTokenId: BigInteger): BlockchainSubmission = simulated("useTicket")

    override fun verifySignedTicket(
        contractTokenId: BigInteger,
        claimedOwner: String,
        expiresAtEpochSeconds: BigInteger,
        signature: String,
    ): Boolean = true

    override fun getTicketCheckInMessageHash(
        contractTokenId: BigInteger,
        claimedOwner: String,
        expiresAtEpochSeconds: BigInteger,
    ): String = Hash.sha3String("${contractTokenId}:${claimedOwner.lowercase()}:$expiresAtEpochSeconds")

    private fun simulated(
        action: String,
        transactionHash: String = "simulated-${UUID.randomUUID()}",
        contractEventId: BigInteger? = null,
        contractTokenId: BigInteger? = null,
    ): BlockchainSubmission =
        BlockchainSubmission(
            action = action,
            transactionHash = transactionHash,
            status = BlockchainTransactionStatus.SIMULATED,
            contractEventId = contractEventId,
            contractTokenId = contractTokenId,
        )
}
