package com.shong.phonecontentresolver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.annotation.CallSuper
import java.util.*

abstract class PhonecallReceiver : BroadcastReceiver() {
/*
    -- 상태 정리 --
    TelephonyManager.EXTRA_STATE_IDLE: 통화종료 혹은 통화벨 종료
    TelephonyManager.EXTRA_STATE_RINGING: 통화벨 울리는중
    TelephonyManager.EXTRA_STATE_OFFHOOK: 통화중*/

    @CallSuper
    override fun onReceive(context: Context, intent: Intent) {
        // firebase 이벤트 로그 발생
        val bundle = intent.getExtras()
        val state = bundle?.getString(TelephonyManager.EXTRA_STATE)

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        if (intent.action == "android.intent.action.NEW_OUTGOING_CALL") {
            savedNumber = intent.extras?.getString("android.intent.extra.PHONE_NUMBER")
        } else if (intent.action == "android.intent.action.PHONE_STATE") {
            val stateStr = intent.extras?.getString(TelephonyManager.EXTRA_STATE)
            val number = intent.extras?.getString(TelephonyManager.EXTRA_INCOMING_NUMBER)
            var callState = 0
            when (stateStr) {
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    callState = TelephonyManager.CALL_STATE_IDLE
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    callState = TelephonyManager.CALL_STATE_OFFHOOK
                }
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    callState = TelephonyManager.CALL_STATE_RINGING
                }
            }
            onCallStateChanged(context, callState, number)
        }
    }

    //Derived classes should override these to respond to specific events of interest
    protected open fun onIncomingCallStarted(context: Context, number: String?, start: Date?) {}

    protected open fun onOutgoingCallStarted(context: Context, number: String?, start: Date?) {}
    protected open fun onIncomingCallEnded(
        context: Context,
        number: String?,
        start: Date?,
        end: Date?
    ) {
    }

    protected open fun onOutgoingCallEnded(
        context: Context,
        number: String?,
        start: Date?,
        end: Date?
    ) {
    }

    protected open fun onMissedCall(context: Context, number: String?, start: Date?) {}

    //Deals with actual events
    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    fun onCallStateChanged(context: Context, state: Int, number: String?) {
        if (TextUtils.isEmpty(number)) { //number 정보가 없으면..
            return
        }
        if (lastState == state) { //No change, debounce extras
            return
        }
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                callStartTime = Date()
                savedNumber = number
                onIncomingCallStarted(context, number, callStartTime)
            }
            TelephonyManager.CALL_STATE_OFFHOOK ->  //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false
                    callStartTime = Date()
                    onOutgoingCallStarted(context, savedNumber, callStartTime)
                }
            TelephonyManager.CALL_STATE_IDLE ->  //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) { //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber, callStartTime)
                } else if (isIncoming) {
                    onIncomingCallEnded(context, savedNumber, callStartTime, Date())
                } else {
                    onOutgoingCallEnded(context, savedNumber, callStartTime, Date())
                }
        }
        lastState = state
    }

    companion object {
        //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var callStartTime: Date? = null
        private var isIncoming = false
        private var savedNumber //because the passed incoming is only valid in ringing
                : String? = null
    }
}
