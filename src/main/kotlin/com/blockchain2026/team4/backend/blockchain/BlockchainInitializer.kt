package com.blockchain2026.team4.backend.blockchain

import com.blockchain2026.team4.backend.blockchain.gateway.TrustTicketGateway
import com.blockchain2026.team4.backend.blockchain.service.BlockchainTransactionService
import com.blockchain2026.team4.backend.common.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials

@Component
@ConditionalOnProperty(prefix = "app.blockchain", name = ["enabled"], havingValue = "true")
class BlockchainInitializer(
    private val trustTicketGateway: TrustTicketGateway,
    private val blockchainTransactionService: BlockchainTransactionService,
    private val appProperties: AppProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        try {
            val operatorAddress = Credentials.create(appProperties.blockchain.operatorPrivateKey).address
            log.info("[BlockchainInit] operator 주소 확인: $operatorAddress")
            val submission = trustTicketGateway.addValidator(operatorAddress)
            blockchainTransactionService.record(submission)
            log.info("[BlockchainInit] operator VALIDATOR_ROLE 부여 완료: ${submission.transactionHash}")
        } catch (e: Exception) {
            log.warn("[BlockchainInit] operator VALIDATOR_ROLE 부여 실패 (이미 있거나 네트워크 오류): ${e.message}")
        }
    }
}
