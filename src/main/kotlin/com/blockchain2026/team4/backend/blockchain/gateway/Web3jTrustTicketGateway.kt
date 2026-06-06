package com.blockchain2026.team4.backend.blockchain.gateway

import com.blockchain2026.team4.backend.blockchain.dto.BlockchainSubmission
import com.blockchain2026.team4.backend.blockchain.dto.ContractEventCommand
import com.blockchain2026.team4.backend.blockchain.entity.BlockchainTransactionStatus
import com.blockchain2026.team4.backend.common.config.AppProperties
import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.Log
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

@Component
@ConditionalOnProperty(prefix = "app.blockchain", name = ["enabled"], havingValue = "true")
class Web3jTrustTicketGateway(
    private val appProperties: AppProperties,
) : TrustTicketGateway {
    private val eventCreatedTopic = Hash.sha3String("EventCreated(uint256,address,string)")
    private val organizerAddedTopic = Hash.sha3String("OrganizerAdded(address)")
    private val ticketMintedTopic = Hash.sha3String("TicketMinted(uint256,uint256,string)")
    private val ticketPurchasedTopic = Hash.sha3String("TicketPurchased(uint256,uint256,address,uint256)")
    private val ticketListedTopic = Hash.sha3String("TicketListed(uint256,address,uint256)")
    private val ticketListingCanceledTopic = Hash.sha3String("TicketListingCanceled(uint256,address)")
    private val ticketResoldTopic = Hash.sha3String("TicketResold(uint256,address,address,uint256)")
    private val web3j: Web3j = Web3j.build(HttpService(appProperties.blockchain.rpcUrl))
    private val callFromAddress = "0x0000000000000000000000000000000000000000"
    private val credentials: Credentials by lazy {
        Credentials.create(appProperties.blockchain.operatorPrivateKey)
    }
    private val transactionManager: RawTransactionManager by lazy {
        RawTransactionManager(web3j, credentials, appProperties.blockchain.chainId)
    }

    override fun addOrganizer(organizerWallet: String): BlockchainSubmission =
        send("addOrganizer", listOf(Address(organizerWallet)))

    override fun confirmOrganizerAdded(organizerWallet: String, transactionHash: String): BlockchainSubmission =
        confirmEvent(
            action = "addOrganizer",
            transactionHash = transactionHash,
            topic = organizerAddedTopic,
        ) { log ->
            sameAddress(topicAddress(log, 1), organizerWallet)
        }

    override fun addValidator(validatorWallet: String): BlockchainSubmission =
        send("addValidator", listOf(Address(validatorWallet)))

    override fun addEventValidator(contractEventId: BigInteger, validatorWallet: String): BlockchainSubmission =
        send("addEventValidator", listOf(Uint256(contractEventId), Address(validatorWallet)))

    override fun createEvent(command: ContractEventCommand): BlockchainSubmission =
        send(
            "createEvent",
            listOf(
                Utf8String(command.eventName),
                Uint256(command.eventTimestamp),
                Uint256(command.ticketPriceWei),
                Uint256(command.totalTicketCount),
                Uint256(command.primarySaleStart),
                Uint256(command.primarySaleEnd),
                Bool(command.resaleAllowed),
                Uint256(command.maxResalePriceRate),
                Uint256(command.resaleStart),
                Uint256(command.resaleEnd),
            ),
        )

    override fun setEventStatus(contractEventId: BigInteger, active: Boolean): BlockchainSubmission =
        send("setEventStatus", listOf(Uint256(contractEventId), Bool(active)))

    override fun cancelEvent(contractEventId: BigInteger): BlockchainSubmission =
        send("cancelEvent", listOf(Uint256(contractEventId)))

    override fun mintTicket(contractEventId: BigInteger, seatInfo: String): BlockchainSubmission =
        send("mintTicket", listOf(Uint256(contractEventId), Utf8String(seatInfo)))

    override fun purchaseTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission =
        send("purchaseTicket", listOf(Uint256(contractTokenId)), valueWei)

    override fun confirmPrimaryPurchase(
        contractTokenId: BigInteger,
        buyerWallet: String,
        transactionHash: String,
    ): BlockchainSubmission =
        confirmEvent(
            action = "purchaseTicket",
            transactionHash = transactionHash,
            topic = ticketPurchasedTopic,
        ) { log ->
            topicUint256(log, 2) == contractTokenId && sameAddress(topicAddress(log, 3), buyerWallet)
        }

    override fun listTicket(contractTokenId: BigInteger, resalePriceWei: BigInteger): BlockchainSubmission =
        send("listTicket", listOf(Uint256(contractTokenId), Uint256(resalePriceWei)))

    override fun confirmTicketListed(
        contractTokenId: BigInteger,
        sellerWallet: String,
        resalePriceWei: BigInteger,
        transactionHash: String,
    ): BlockchainSubmission =
        confirmEvent(
            action = "listTicket",
            transactionHash = transactionHash,
            topic = ticketListedTopic,
        ) { log ->
            topicUint256(log, 1) == contractTokenId &&
                sameAddress(topicAddress(log, 2), sellerWallet) &&
                dataUint256(log) == resalePriceWei
        }

    override fun purchaseResaleTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission =
        send("purchaseResaleTicket", listOf(Uint256(contractTokenId)), valueWei)

    override fun confirmResalePurchase(
        contractTokenId: BigInteger,
        sellerWallet: String,
        buyerWallet: String,
        valueWei: BigInteger,
        transactionHash: String,
    ): BlockchainSubmission =
        confirmEvent(
            action = "purchaseResaleTicket",
            transactionHash = transactionHash,
            topic = ticketResoldTopic,
        ) { log ->
            topicUint256(log, 1) == contractTokenId &&
                sameAddress(topicAddress(log, 2), sellerWallet) &&
                sameAddress(topicAddress(log, 3), buyerWallet) &&
                dataUint256(log) == valueWei
        }

    override fun cancelListing(contractTokenId: BigInteger): BlockchainSubmission =
        send("cancelListing", listOf(Uint256(contractTokenId)))

    override fun confirmListingCanceled(
        contractTokenId: BigInteger,
        sellerWallet: String,
        transactionHash: String,
    ): BlockchainSubmission =
        confirmEvent(
            action = "cancelListing",
            transactionHash = transactionHash,
            topic = ticketListingCanceledTopic,
        ) { log ->
            topicUint256(log, 1) == contractTokenId && sameAddress(topicAddress(log, 2), sellerWallet)
        }

    override fun useTicket(contractTokenId: BigInteger): BlockchainSubmission =
        send("useTicket", listOf(Uint256(contractTokenId)))

    override fun verifySignedTicket(
        contractTokenId: BigInteger,
        claimedOwner: String,
        expiresAtEpochSeconds: BigInteger,
        signature: String,
    ): Boolean {
        val function = Function(
            "verifySignedTicket",
            listOf(Uint256(contractTokenId), Address(claimedOwner), Uint256(expiresAtEpochSeconds), org.web3j.abi.datatypes.DynamicBytes(hexToBytes(signature))),
            listOf(object : TypeReference<Bool>() {}),
        )
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(callFromAddress, appProperties.blockchain.contractAddress, FunctionEncoder.encode(function)),
            DefaultBlockParameterName.LATEST,
        ).send()
        val decoded = FunctionReturnDecoder.decode(response.value, function.outputParameters)
        return decoded.firstOrNull()?.value as? Boolean ?: false
    }

    override fun getTicketCheckInMessageHash(
        contractTokenId: BigInteger,
        claimedOwner: String,
        expiresAtEpochSeconds: BigInteger,
    ): String {
        val function = Function(
            "getTicketCheckInMessageHash",
            listOf(Uint256(contractTokenId), Address(claimedOwner), Uint256(expiresAtEpochSeconds)),
            listOf(object : TypeReference<org.web3j.abi.datatypes.generated.Bytes32>() {}),
        )
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(callFromAddress, appProperties.blockchain.contractAddress, FunctionEncoder.encode(function)),
            DefaultBlockParameterName.LATEST,
        ).send()
        val decoded = FunctionReturnDecoder.decode(response.value, function.outputParameters)
        val bytes = decoded.firstOrNull()?.value as? ByteArray ?: return "0x"
        return "0x${bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }}"
    }

    private fun send(action: String, inputs: List<Type<*>>, valueWei: BigInteger = BigInteger.ZERO): BlockchainSubmission {
        val contractAddress = appProperties.blockchain.contractAddress
        if (contractAddress.isBlank() || appProperties.blockchain.operatorPrivateKey.isBlank()) {
            throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, "블록체인 계약 주소 또는 운영자 키가 설정되지 않았습니다.")
        }

        val function = Function(action, inputs, emptyList())
        val encoded = FunctionEncoder.encode(function)
        val result = transactionManager.sendTransaction(
            appProperties.blockchain.gasPriceWei,
            appProperties.blockchain.gasLimit,
            contractAddress,
            encoded,
            valueWei,
        )

        if (result.hasError()) {
            throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, result.error.message)
        }

        val receipt = waitForReceipt(result.transactionHash, Duration.ofSeconds(45))
        if (receipt != null && receipt.status == "0x0") {
            throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, "컨트랙트 트랜잭션이 실패했습니다: ${result.transactionHash}")
        }

        return BlockchainSubmission(
            action = action,
            transactionHash = result.transactionHash,
            status = if (receipt == null) BlockchainTransactionStatus.SUBMITTED else BlockchainTransactionStatus.CONFIRMED,
            contractEventId = when (action) {
                "createEvent" -> findIndexedUint(receipt, eventCreatedTopic, 1)
                else -> null
            },
            contractTokenId = when (action) {
                "mintTicket" -> findIndexedUint(receipt, ticketMintedTopic, 2)
                else -> null
            },
        )
    }

    private fun confirmEvent(
        action: String,
        transactionHash: String,
        topic: String,
        matches: (Log) -> Boolean,
    ): BlockchainSubmission {
        if (transactionHash.isBlank()) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "트랜잭션 해시가 필요합니다.")
        }

        val receipt = waitForReceipt(transactionHash, Duration.ofSeconds(60))
            ?: throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, "트랜잭션 영수증을 아직 찾을 수 없습니다: $transactionHash")

        if (!receipt.to.isNullOrBlank() && !sameAddress(receipt.to, appProperties.blockchain.contractAddress)) {
            throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, "TrustTicket 컨트랙트로 보낸 트랜잭션이 아닙니다.")
        }
        if (receipt.status == "0x0") {
            throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, "컨트랙트 트랜잭션이 실패했습니다: $transactionHash")
        }

        val matchedLog = receipt.logs.firstOrNull { log ->
            sameAddress(log.address, appProperties.blockchain.contractAddress) &&
                log.topics.firstOrNull()?.equals(topic, ignoreCase = true) == true &&
                matches(log)
        } ?: throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, "트랜잭션 영수증에서 예상한 $action 이벤트를 찾지 못했습니다.")

        return BlockchainSubmission(
            action = action,
            transactionHash = transactionHash,
            status = BlockchainTransactionStatus.CONFIRMED,
            contractEventId = findIndexedUint(receipt, eventCreatedTopic, 1),
            contractTokenId = when (action) {
                "purchaseTicket", "listTicket", "purchaseResaleTicket", "cancelListing" -> topicUint256(matchedLog, if (action == "purchaseTicket") 2 else 1)
                else -> null
            },
        )
    }

    private fun waitForReceipt(transactionHash: String, timeout: Duration): TransactionReceipt? {
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline)) {
            val response = web3j.ethGetTransactionReceipt(transactionHash).send()
            if (response.hasError()) {
                throw BusinessException(ErrorCode.BLOCKCHAIN_TRANSACTION_FAILED, response.error.message)
            }
            if (response.transactionReceipt.isPresent) {
                return response.transactionReceipt.get()
            }
            Thread.sleep(1_000L)
        }
        return null
    }

    private fun findIndexedUint(receipt: TransactionReceipt?, topic: String, topicIndex: Int): BigInteger? =
        receipt?.logs
            ?.firstOrNull {
                sameAddress(it.address, appProperties.blockchain.contractAddress) &&
                    it.topics.firstOrNull()?.equals(topic, ignoreCase = true) == true
            }
            ?.let { topicUint256(it, topicIndex) }

    private fun topicUint256(log: Log, topicIndex: Int): BigInteger? =
        log.topics.getOrNull(topicIndex)?.removePrefix("0x")?.takeIf { it.isNotBlank() }?.let { BigInteger(it, 16) }

    private fun topicAddress(log: Log, topicIndex: Int): String? =
        log.topics.getOrNull(topicIndex)?.removePrefix("0x")?.takeLast(40)?.let { "0x$it" }

    private fun dataUint256(log: Log): BigInteger? =
        log.data?.removePrefix("0x")?.takeIf { it.isNotBlank() }?.take(64)?.let { BigInteger(it, 16) }

    private fun sameAddress(left: String?, right: String?): Boolean =
        !left.isNullOrBlank() && !right.isNullOrBlank() && left.equals(right, ignoreCase = true)

    private fun hexToBytes(value: String): ByteArray {
        val clean = value.removePrefix("0x")
        return clean.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
