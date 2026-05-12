package com.blockchain2026.team4.backend.checkin.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.Base64

@Service
class QrCodeService {
    fun createPngBase64(payload: String): String {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 512, 512)
        val output = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(matrix, "PNG", output)
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }
}
