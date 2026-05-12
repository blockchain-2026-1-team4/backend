package com.blockchain2026.team4.backend.blockchain.repository

import com.blockchain2026.team4.backend.blockchain.entity.BlockchainTransactionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BlockchainTransactionRepository : JpaRepository<BlockchainTransactionEntity, UUID>
