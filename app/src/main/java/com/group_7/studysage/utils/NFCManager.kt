package com.group_7.studysage.utils

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.group_7.studysage.data.nfc.NFCPayload
import com.group_7.studysage.services.NfcHostApduService
import java.nio.charset.Charset

class NFCManager(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "NFC_MANAGER"
        private const val AID = "F0010203040506" // Must match HCE service
    }

    fun isNfcAvailable(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    /**
     * Prepare data for sending via HCE
     */
    fun prepareDataForSending(payload: NFCPayload) {
        val jsonString = json.encodeToString(payload)
        val dataBytes = jsonString.toByteArray(Charset.forName("UTF-8"))

        Log.d(TAG, "Preparing data for HCE: ${dataBytes.size} bytes")
        Log.d(TAG, "JSON: $jsonString")

        NfcHostApduService.dataToSend = dataBytes
    }

    /**
     * Clear sending data
     */
    fun clearSendingData() {
        NfcHostApduService.dataToSend = null
    }

    /**
     * Read data from another phone acting as HCE
     */
    fun readDataFromTag(tag: Tag): NFCPayload? {
        Log.d(TAG, "Attempting to read from tag")

        try {
            val isoDep = IsoDep.get(tag) ?: run {
                Log.e(TAG, "IsoDep not supported")
                return null
            }

            isoDep.connect()
            isoDep.timeout = 5000 // 5 seconds

            // Build SELECT APDU command
            val selectCommand = buildSelectCommand(AID)
            Log.d(TAG, "Sending SELECT command")

            val response = isoDep.transceive(selectCommand)

            isoDep.close()

            if (response.size < 2) {
                Log.e(TAG, "Invalid response size: ${response.size}")
                return null
            }

            // Check status bytes (last 2 bytes should be 90 00 for success)
            val sw1 = response[response.size - 2]
            val sw2 = response[response.size - 1]

            Log.d(TAG, "Status: ${sw1.toHex()}${sw2.toHex()}")

            if (sw1 != 0x90.toByte() || sw2 != 0x00.toByte()) {
                Log.e(TAG, "Error status from card")
                return null
            }

            // Extract data (remove status bytes)
            val data = response.copyOfRange(0, response.size - 2)

            if (data.isEmpty()) {
                Log.w(TAG, "No data received")
                return null
            }

            Log.d(TAG, "Received ${data.size} bytes")

            val jsonString = String(data, Charset.forName("UTF-8"))
            Log.d(TAG, "JSON: $jsonString")

            return json.decodeFromString<NFCPayload>(jsonString)

        } catch (e: Exception) {
            Log.e(TAG, "Error reading from tag", e)
            return null
        }
    }

    /**
     * Parse received data from HCE service callback
     */
    fun parseReceivedData(data: ByteArray): NFCPayload? {
        return try {
            val jsonString = String(data, Charset.forName("UTF-8"))
            Log.d(TAG, "Parsing received data: $jsonString")
            json.decodeFromString<NFCPayload>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing received data", e)
            null
        }
    }

    private fun buildSelectCommand(aid: String): ByteArray {
        val aidBytes = aid.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        return byteArrayOf(
            0x00.toByte(), // CLA
            0xA4.toByte(), // INS (SELECT)
            0x04.toByte(), // P1
            0x00.toByte(), // P2
            aidBytes.size.toByte() // Lc
        ) + aidBytes + byteArrayOf(0x00.toByte()) // Le
    }

    private fun Byte.toHex(): String = "%02X".format(this)
}