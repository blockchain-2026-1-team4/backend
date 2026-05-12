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
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import java.math.BigInteger

@Component
@ConditionalOnProperty(prefix = "app.blockchain", name = ["enabled"], havingValue = "true")
class Web3jTrustTicketGateway(
    private val appProperties: AppProperties,
) : TrustTicketGateway {
    private val web3j: Web3j = Web3j.build(HttpService(appProperties.blockchain.rpcUrl))
    private val credentials: Credentials = Credentials.create(appProperties.blockchain.operatorPrivateKey)
    private val transactionManager = RawTransactionManager(web3j, credentials, appProperties.blockchain.chainId)

    override fun addOrganizer(organizerWallet: String): BlockchainSubmission =
        send("addOrganizer", listOf(Address(organizerWallet)))

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

    override fun mintTicket(contractEventId: BigInteger, seatInfo: String): BlockchainSubmission =
        send("mintTicket", listOf(Uint256(contractEventId), Utf8String(seatInfo)))

    override fun purchaseTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission =
        send("purchaseTicket", listOf(Uint256(contractTokenId)), valueWei)

    override fun listTicket(contractTokenId: BigInteger, resalePriceWei: BigInteger): BlockchainSubmission =
        send("listTicket", listOf(Uint256(contractTokenId), Uint256(resalePriceWei)))

    override fun purchaseResaleTicket(contractTokenId: BigInteger, valueWei: BigInteger): BlockchainSubmission =
        send("purchaseResaleTicket", listOf(Uint256(contractTokenId)), valueWei)

    override fun cancelListing(contractTokenId: BigInteger): BlockchainSubmission =
        send("cancelListing", listOf(Uint256(contractTokenId)))

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
            Transaction.createEthCallTransaction(credentials.address, appProperties.blockchain.contractAddress, FunctionEncoder.encode(function)),
            DefaultBlockParameterName.LATEST,
        ).send()
        val decoded = FunctionReturnDecoder.decode(response.value, function.outputParameters)
        return decoded.firstOrNull()?.value as? Boolean ?: false
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

        return BlockchainSubmission(
            action = action,
            transactionHash = result.transactionHash,
            status = BlockchainTransactionStatus.SUBMITTED,
        )
    }

    private fun hexToBytes(value: String): ByteArray {
        val clean = value.removePrefix("0x")
        return clean.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
