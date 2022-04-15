package com.shong.phonecontentresolver.receiver

import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

class CallReceiver : PhonecallReceiver() {
    val TAG = this::class.java.simpleName + "_sHong"

    override fun onIncomingCallStarted(context: Context, number: String?, start: Date?) {
        Log.d(TAG, "onIncomingCallStarted")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive")
    }

    override fun onOutgoingCallStarted(context: Context, number: String?, start: Date?) {
        Log.d(TAG, "onOutgoingcomingCallStarted")
    }

    override fun onIncomingCallEnded(context: Context, number: String?, start: Date?, end: Date?) {
        Log.d(TAG, "onIncomingCallEnded")
    }

    override fun onOutgoingCallEnded(context: Context, number: String?, start: Date?, end: Date?) {
        Log.d(TAG, "onOutgoingCallEnded")
    }

    override fun onMissedCall(context: Context, number: String?, start: Date?) {
        Log.d(TAG, "onMissedCall")
    }

}