package com.example.melgarejo.youaretooloud

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.firebase.database.*


class HomeActivity : Activity(), MediaPlayer.OnPreparedListener {

    companion object {
        private const val TAG = "HomeActivity"
        private const val DB_ROOT_NAME = "android_things"
        private const val DB_DELAY = "delay"
        private const val DB_PLAY_IT = "sound"
    }


    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var ledRedGpio: Gpio
    private lateinit var ledGreenGpio: Gpio
    private lateinit var ledBlueGpio: Gpio
    private lateinit var buttonInputDriver: ButtonInputDriver

    private var mListener: ValueEventListener
    private var delayInMilliSeconds = 1300L
    private var initialized = false

    init {
        mListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                delayInMilliSeconds = snapshot.child(DB_DELAY).getValue(Long::class.java) ?: delayInMilliSeconds
                val shouldPlayIt = snapshot.child(DB_PLAY_IT).getValue(Boolean::class.java) ?: false
                if (shouldPlayIt && !mediaPlayer.isPlaying) {
                    try {
                        mediaPlayer.prepareAsync()
                    } catch (t: Throwable) {
                        playSong(mediaPlayer)
                    }
                } else {
                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                }
                Log.d(TAG, shouldPlayIt.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setupInOuts()
        createMediaPlayerInstance()
        waitUntilConnected()
        setupFirebaseDatabase()
    }

    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        if (initialized) {
            playSong(mp)
        } else {
            initialized = true
        }
    }

    private fun setupInOuts() {
        val pioService = PeripheralManager.getInstance()
        ledRedGpio = pioService.openGpio(BoardDefaults.gpioForRedLED)
        ledRedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        ledGreenGpio = pioService.openGpio(BoardDefaults.gpioForGreenLED)
        ledGreenGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        ledBlueGpio = pioService.openGpio(BoardDefaults.gpioForBlueLED)
        ledBlueGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
    }

    private fun setupFirebaseDatabase() {
        setLedValue(ledBlueGpio, false)
        database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference(DB_ROOT_NAME)
        databaseReference.addValueEventListener(mListener)
        databaseReference.child(DB_PLAY_IT).setValue(false)
        databaseReference.child(DB_DELAY).setValue(delayInMilliSeconds)
        setLedValue(ledBlueGpio, true)
    }

    private fun waitUntilConnected() {
        setLedValue(ledGreenGpio, false)
        while (!isConnectedToWifi(this)) {
            Log.e(TAG, "You have to connect to a WiFi network or Ethernet cable")
        }
        setLedValue(ledGreenGpio, true)
    }

    /**
     * Update the value of the LED output.
     */
    private fun setLedValue(led: Gpio, value: Boolean) {
        Log.d(TAG, "Setting LED ${led.name} value to $value")
        led.value = value
    }

    private fun createMediaPlayerInstance() {
        setLedValue(ledRedGpio, false)
        mediaPlayer = MediaPlayer.create(this, R.raw.siren2)
        maximizeVolume()
        mediaPlayer.isLooping = true
        mediaPlayer.setOnPreparedListener(this)
        setLedValue(ledRedGpio, true)
    }

    private fun maximizeVolume() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val percent = 1f
        val maximized = (maxVolume * percent).toInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, maximized, 0)
    }

    private fun playSong(mp: MediaPlayer?) {
        mp?.start()
        startTimer(mp)
    }

    private fun startTimer(mp: MediaPlayer?) {
        val handler = Handler()
        handler.postDelayed({
            try {
                mp?.stop()
                databaseReference.child(DB_PLAY_IT).setValue(false)
            } catch (t: Throwable) {
                Log.e(TAG, t.cause?.message)
            }
        }, delayInMilliSeconds)
    }

    private fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return isConnectedToWifiAfterMarshmallow(connectivityManager)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun isConnectedToWifiAfterMarshmallow(connectivityManager: ConnectivityManager): Boolean {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && isWifiNetwork(networkInfo) && networkInfo.isConnected
    }

    private fun isWifiNetwork(networkInfo: NetworkInfo): Boolean {
        return networkInfo.type == ConnectivityManager.TYPE_WIFI || networkInfo.type == ConnectivityManager.TYPE_WIMAX
    }
}