package edu.uw.ischool.c1ndyy.arewethereyet

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


const val ALARM_ACTION = "edu.uw.ischool.newart.ALARM"

class MainActivity : AppCompatActivity() {

    lateinit var textMessage : EditText
    lateinit var phoneNumber : EditText
    lateinit var minuteIncr : EditText
    lateinit var startButton : Button
    var receiver : BroadcastReceiver? = null

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
            } else {
                stop()
                textMessage.isEnabled = true;
                phoneNumber.isEnabled = true;
                minuteIncr.isEnabled = true;
                receiver = null
                startButton.setText("Start")
            }
        })
    }

    fun start() {
        val activityThis = this

        val msg = textMessage.text.toString()
        val phoneInput = phoneNumber.text.toString()
        val minuteTime = minuteIncr.text.toString().toLong() * 1000 * 60

        if (receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val phoneFormat = "(${phoneInput.substring(0, 3)}) ${phoneInput.substring(3, 6)}-${phoneInput.substring(6, 10)}"
                    val toastMsg = "${phoneFormat}: $msg"
                    Toast.makeText(activityThis, toastMsg, Toast.LENGTH_LONG).show()
                }
            }
            val filter = IntentFilter(ALARM_ACTION)
            registerReceiver(receiver, filter)
            val intent = Intent(ALARM_ACTION)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val alarmManager : AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                minuteTime,
                pendingIntent
            )
        }
    }

    fun stop() {
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
}