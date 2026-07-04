package com.erp.pizzeria.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Genera codigos QR en PNG usando ZXing. Para la boleta se entrega como data-URI
 * base64 de modo que se incruste directamente en la vista sin un endpoint extra.
 */
@Service
public class GeneradorQrService {

    private static final int TAMANIO_DEFECTO = 220;

    /** Devuelve el QR como data-URI base64 listo para el src de un img. */
    public String generarDataUri(String contenido, int tamanio) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(generarPng(contenido, tamanio));
    }

    public String generarDataUri(String contenido) {
        return generarDataUri(contenido, TAMANIO_DEFECTO);
    }

    /** Genera los bytes PNG del QR. Lanza IllegalStateException si la codificacion falla. */
    public byte[] generarPng(String contenido, int tamanio) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1);
            BitMatrix matriz = new QRCodeWriter()
                    .encode(contenido, BarcodeFormat.QR_CODE, tamanio, tamanio, hints);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el codigo QR de la boleta", e);
        }
    }
}
