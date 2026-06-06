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

    override fun addValidator(validatorWallet: String): BlockchainSubmission = simulated("addValidator")

    override fun addEventValidator(contractEventId: BigInteger, validatorWallet: String): BlockchainSubmission = simulated("addEventValidator")

    override fun createEvent(command: ContractEventCommand): BlockchainSubmission =
        simulated("createEvent", BigInteger.valueOf(nextEventId.getAndIncrement()))

    override fun setEventStatus(contractEventId: BigInteger, active: Boolean): BlockchainSubmission = simulated("setEventStatus")

    override fun mintTicket(contractEventId: BigInteger, seatInfo: String): BlockchainSubmission =
        simulated("mintTicket", BigInteger.valueOf(nextTokenId.getAndIncrement()))

    override fun purchaseTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission = simulated("purchaseTicket")

    override fun listTicket(contractTokenId: BigInteger, resalePriceWei: BigInteger): BlockchainSubmission = simulated("listTicket")

    override fun purchaseResaleTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission = simulated("purchaseResaleTicket")

    override fun cancelListing(contractTokenId: BigInteger): BlockchainSubmission = simulated("cancelListing")

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

    private fun simulated(action: String, resultId: BigInteger? = null): BlockchainSubmission =
        BlockchainSubmission(
            action = action,
            transactionHash = "simulated-${UUID.randomUUID()}",
            status = BlockchainTransactionStatus.SIMULATED,
            resultId = resultId,
        )
}
