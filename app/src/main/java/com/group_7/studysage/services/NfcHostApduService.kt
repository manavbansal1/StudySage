// NfcHostApduService.kt
package com.group_7.studysage.services

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log


/**
 * NFC Host-based Card Emulation Service for sending and receiving data via NFC.
 * This service handles APDU commands from NFC readers.
 * - Listens for SELECT AID commands to initiate communication.
 * - Sends data to the NFC reader when requested.
 * - Receives data from the NFC reader and notifies the activity.
 *
 * while solving we faced some isses with nfc data transfer so we used this guide
 * https://developer.android.com/develop/connectivity/nfc/hce
 * https://developer.android.com/reference/android/nfc/package-summary
 */
class NfcHostApduService : HostApduService() {

    companion object {
        private const val TAG = "NFC_HCE"
        private const val SELECT_APDU_HEADER = "00A40400"
        private const val AID = "F0010203040506" // Your app's unique AID

        // Shared data between service and activity
        var dataToSend: ByteArray? = null
        var onDataReceived: ((ByteArray) -> Unit)? = null
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val hexCommand = commandApdu.toHex()
        Log.d(TAG, "Received APDU: $hexCommand")

        // Check if it's a SELECT command for our AID
        if (hexCommand.startsWith(SELECT_APDU_HEADER)) {
            Log.d(TAG, "SELECT AID received")

            // Send data if available
            dataToSend?.let { data ->
                Log.d(TAG, "Sending data: ${data.size} bytes")
                return data + byteArrayOf(0x90.toByte(), 0x00.toByte()) // Success status
            }

            // Return success even if no data
            return byteArrayOf(0x90.toByte(), 0x00.toByte())
        }

        // Check if it's data from the reader
        if (commandApdu.size > 2) {
            Log.d(TAG, "Received data from reader: ${commandApdu.size} bytes")

            // Remove status bytes if present
            val data = if (commandApdu.size > 2 &&
                commandApdu[commandApdu.size - 2] == 0x90.toByte() &&
                commandApdu[commandApdu.size - 1] == 0x00.toByte()) {
                commandApdu.copyOfRange(0, commandApdu.size - 2)
            } else {
                commandApdu
            }

            onDataReceived?.invoke(data)
            return byteArrayOf(0x90.toByte(), 0x00.toByte())
        }

        // Unknown command
        Log.w(TAG, "Unknown APDU command")
        return byteArrayOf(0x6F.toByte(), 0x00.toByte()) // Unknown error
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02X".format(it) }
    }
}