package edu.uw.ischool.c1ndyy.arewethereyet

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest


const val ALARM_ACTION = "edu.uw.ischool.newart.ALARM"

class MainActivity : AppCompatActivity() {

    lateinit var textMessage : EditText
    lateinit var phoneNumber : EditText
    lateinit var minuteIncr : EditText
    lateinit var startButton : Button
    var receiver : BroadcastReceiver? = null
    private val smsRequestCode = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textMessage = findViewById(R.id.messageInput)
        phoneNumber = findViewById(R.id.phoneInput)
        minuteIncr = findViewById(R.id.incrInput)
        startButton = findViewById(R.id.startButton)

        startButton.isEnabled = false

        // textwatcher for each edittext
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // make sure values are not empty
                val allFieldsFilled = !textMessage.text.isNullOrBlank() &&
                        !phoneNumber.text.isNullOrBlank() &&
                        !minuteIncr.text.isNullOrBlank() &&
                        isValidInput(minuteIncr.text.toString()) &&
                        isValidPhoneNumber(phoneNumber.text.toString())

                if (allFieldsFilled) {
                    minuteIncr.error = null
                    startButton.isEnabled = true
                } else {
                    if (!isValidPhoneNumber(phoneNumber.text.toString()) && phoneNumber.text.isNotEmpty()) {
                        phoneNumber.error = "Please enter valid phone number format."
                    }
                    if (!isValidInput(minuteIncr.text.toString()) && minuteIncr.text.isNotEmpty()) {
                        minuteIncr.error = "Please enter a positive integer."
                    }
                    startButton.isEnabled = false
                }

            }
        }

        textMessage.addTextChangedListener(textWatcher)
        phoneNumber.addTextChangedListener(textWatcher)
        minuteIncr.addTextChangedListener(textWatcher)

        startButton.setOnClickListener (View.OnClickListener {
            if (startButton.text == "Start") {
                start()
                textMessage.isEnabled = false;
                phoneNumber.isEnabled = false;
                minuteIncr.isEnabled = false;
                startButton.setText("Stop")
                Toast.makeText(this, "Messages will begin to start.", Toast.LENGTH_LONG).show()
            } else {
                stop()
                textMessage.isEnabled = true;
                phoneNumber.isEnabled = true;
                minuteIncr.isEnabled = true;
                receiver = null
                startButton.setText("Start")
                Toast.makeText(this, "Messages have stopped.", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun start() {
        val activityThis = this
        val smsManager = SmsManager.getDefaultSmsSubscriptionId()
        val msg = textMessage.text.toString()
        val phoneInput = phoneNumber.text.toString()
        val minuteTime = minuteIncr.text.toString().toLong() * 1000 * 60

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), smsRequestCode)
        } else {
            // Permission is already granted, proceed with sending SMS
            sendSms(phoneInput, msg)
        }

        if (receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    //val phoneFormat = "(${phoneInput.substring(0, 3)}) ${phoneInput.substring(3, 6)}-${phoneInput.substring(6, 10)}"
                    //val toastMsg = "${phoneFormat}: $msg"
                    //Toast.makeText(activityThis, toastMsg, Toast.LENGTH_LONG).show()
                    Log.d("ScheduledTask", "Scheduled task executed.")
                    sendSms(phoneInput, msg)
                }
            }
            val filter = IntentFilter(ALARM_ACTION)
            registerReceiver(receiver, filter)
            val intent = Intent(ALARM_ACTION)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val alarmManager : AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                minuteTime,
                pendingIntent
            )
        }
    }

    fun stop() {
        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
        }

        val intent = Intent(ALARM_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val alarmManager : AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun isValidInput(input: String): Boolean {
        try {
            val intValue = input.toInt()
            return intValue > 0
        } catch (e: NumberFormatException) {
            return false
        }
    }

    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val sentIntent = Intent("SMS_SENT")
            val piSent = PendingIntent.getBroadcast(this, 0, sentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            smsManager.sendTextMessage(phoneNumber, null, message, piSent, null)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SendSMS", "Failed to send message to $phoneNumber: $message")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            smsRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with sending SMS
                    start()
                } else {
                    // Permission denied, handle accordingly (e.g., show a message to the user)
                }
            }
        }
    }
}