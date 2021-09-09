package ui.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import com.services.it.feel.R
import constants.Constants
import utils.Preference
import utils.Utils
import java.util.*

class IncomingCallActivity : AppCompatActivity(R.layout.dialog_incoming_call) {
    companion object {
        const val actionCloseActivity = "actionCloseActivity"
        const val keyIncomingNumber = "keyIncomingNumber"
    }

    private var time: Long = 0

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == actionCloseActivity) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingNumber = intent.getStringExtra(keyIncomingNumber)
        if (incomingNumber.isNullOrEmpty()) {
            finish()
            return
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(actionCloseActivity)
        registerReceiver(broadcastReceiver, intentFilter)

        window?.let {
            it.attributes.gravity = Gravity.TOP
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }

        time = Calendar.getInstance().timeInMillis

        updateUI(incomingNumber)
    }

    override fun onResume() {
        val milliseconds = Calendar.getInstance().timeInMillis
        if (milliseconds - time > Constants.incomingCallActivityCloseDelay) {
            finish()
        }

        super.onResume()
    }

    override fun onPause() {
        val milliseconds = Calendar.getInstance().timeInMillis
        val result = milliseconds - time
        if (result > Constants.incomingCallActivityCloseDelay) {
            finish()
        }

        super.onPause()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: IllegalArgumentException) { }

        super.onDestroy()
    }

    private fun updateUI(incomingNumber: String) {
        val phoneNumber = if (incomingNumber.startsWith("0")) {
            incomingNumber.removeRange(0, 1)
        } else {
            incomingNumber
        }

        val employees = Preference.getEmployees(this)
        val employee = employees.firstOrNull { it.phoneNumber?.endsWith(phoneNumber) ?: false }
        if (employee == null) {
            finish()
            return
        }

        val logo = findViewById<ShapeableImageView>(R.id.logo)
        val initials = findViewById<MaterialTextView>(R.id.initials)
        val primary = findViewById<MaterialTextView>(R.id.primary)
        val secondary = findViewById<MaterialTextView>(R.id.secondary)

        primary.text = getString(R.string.combine_two_words, employee.firstName.toString().trim(), employee.lastName.toString().trim())

        val secondaryText = employee.specialization

        if (secondaryText.isNullOrEmpty()) {
            secondary.visibility = View.GONE
        } else {
            secondary.text = secondaryText
            secondary.visibility = View.VISIBLE
        }

        val initialsText = if (employee.firstName.isNullOrEmpty()) {
            if (employee.lastName.isNullOrEmpty()) {
                "XY"
            } else {
                employee.lastName[0].toString()
            }
        } else {
            var initialsText = employee.firstName[0].toString()
            if (!employee.lastName.isNullOrEmpty()) {
                initialsText += employee.lastName[0].toString()
            }
            initialsText
        }

        initials.text = initialsText

        val listener = object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                if (!isDestroyed) {
                    initials.visibility = View.VISIBLE
                }
                return false
            }

            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                if (!isDestroyed) {
                    initials.visibility = View.INVISIBLE
                }
                return false
            }
        }

        val requestOptions = RequestOptions.circleCropTransform()
            .placeholder(Utils.generateOvalDrawable(primary.text.toString()))
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(this)
            .load(Constants.url + "Uploads/Users/${employee.documentId}/ProfilePhoto.png")
            .listener(listener)
            .apply(requestOptions)
            .into(logo)
    }
}