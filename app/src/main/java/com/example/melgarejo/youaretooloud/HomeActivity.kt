package com.example.melgarejo.youaretooloud

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.firebase.database.*


class HomeActivity : Activity(), MediaPlayer.OnPreparedListener {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var mListener: ValueEventListener

    private val TAG = "HomeActivity"
    private val DELAY_IN_MILISECONDS: Long = 5000

    init {
        mListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.child("sound").getValue(Boolean::class.java) ?: false
                if (value && !mediaPlayer.isPlaying) {
                    mediaPlayer.prepareAsync()

                } else {
                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                }
                Log.d(TAG, value.toString())
            }
        }
    }

    private fun startTimer(mp: MediaPlayer?) {
        val handler = Handler()
        handler.postDelayed({
            mp?.stop()
            databaseReference.child("sound").setValue(false)
        }, DELAY_IN_MILISECONDS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Log.e(TAG, "ANTES DE ABRIR O MEDIA PLAYER")
        mediaPlayer = MediaPlayer.create(this, R.raw.siren)
        mediaPlayer.isLooping = true
        mediaPlayer.setOnPreparedListener(this)
        while(!isConnectedToWifi(this)) {
            Log.e(TAG, "nao está conectado")
        }
        Log.e(TAG, "ANTES DE ABRIR O FIREBASE")
        database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference("android_things")
        databaseReference.addValueEventListener(mListener)

    }

    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
        startTimer(mp)
    }

    fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT > 23) {
            isConnectedToWifiAfterMarshmallow(connectivityManager)
        } else {
            isConnectedToWifiBeforeMarshmallow(connectivityManager)
        }
    }

    private fun isConnectedToWifiBeforeMarshmallow(connectivityManager: ConnectivityManager): Boolean {
        val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return networkInfo != null && networkInfo.isConnected
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