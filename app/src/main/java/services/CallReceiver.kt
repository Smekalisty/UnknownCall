package services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import constants.Constants
import ui.call.IncomingCallActivity

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        if (intent.hasExtra(TelephonyManager.EXTRA_STATE)) {
            @Suppress("MoveVariableDeclarationIntoWhen")
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    @Suppress("DEPRECATION")
                    val hasIncomingNumber = intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    if (hasIncomingNumber) {
                        @Suppress("DEPRECATION")
                        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                        Handler(Looper.getMainLooper()).postDelayed({
                            val newIntent = Intent(context, IncomingCallActivity::class.java)
                            newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            newIntent.putExtra(IncomingCallActivity.keyIncomingNumber, incomingNumber)
                            context.startActivity(newIntent)
                        }, Constants.approximatelyDelayCallerAppAppear)
                    }
                }

                TelephonyManager.EXTRA_STATE_IDLE -> {
                    context.sendBroadcast(Intent(IncomingCallActivity.actionCloseActivity))
                }
            }
        }
    }
}