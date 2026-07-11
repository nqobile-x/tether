package com.example.tether.alerting

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

/** Direct SMS send, no backend. Returns true when the send was handed to the radio. */
class SmsSender(private val context: Context) {

    fun send(phoneNumber: String, message: String): Boolean {
        return try {
            val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            } ?: return false
            val parts = manager.divideMessage(message)
            manager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SMS blocked, missing SEND_SMS permission", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "SMS send failed for $phoneNumber", e)
            false
        }
    }

    private companion object {
        const val TAG = "SmsSender"
    }
}
