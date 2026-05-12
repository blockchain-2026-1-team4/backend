package com.blockchain2026.team4.backend.auth.service

import com.blockchain2026.team4.backend.common.error.BusinessException
import com.blockchain2026.team4.backend.common.error.ErrorCode
import org.springframework.stereotype.Component
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.nio.charset.StandardCharsets

@Component
class WalletSignatureVerifier {
    fun verify(message: String, signature: String, expectedWalletAddress: String): Boolean {
        val recoveredAddress = recoverAddress(message, signature)
        return recoveredAddress.equals(expectedWalletAddress.normalizeWallet(), ignoreCase = true)
    }

    private fun recoverAddress(message: String, signature: String): String {
        val signatureBytes = Numeric.hexStringToByteArray(signature)
        if (signatureBytes.size != 65) {
            throw BusinessException(ErrorCode.WALLET_SIGNATURE_INVALID)
        }

        val v = signatureBytes[64].toInt().let { if (it < 27) it + 27 else it }.toByte()
        val r = signatureBytes.copyOfRange(0, 32)
        val s = signatureBytes.copyOfRange(32, 64)
        val signatureData = Sign.SignatureData(v, r, s)
        val publicKey = Sign.signedPrefixedMessageToKey(message.toByteArray(StandardCharsets.UTF_8), signatureData)

        return "0x${Keys.getAddress(publicKey)}"
    }

    private fun String.normalizeWallet(): String = trim().lowercase()
}
