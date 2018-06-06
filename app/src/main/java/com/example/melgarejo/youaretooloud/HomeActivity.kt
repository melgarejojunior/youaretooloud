package com.example.melgarejo.youaretooloud

import android.app.Activity
import android.media.MediaPlayer
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
        mediaPlayer = MediaPlayer.create(this, R.raw.siren)
        mediaPlayer.isLooping = true
        mediaPlayer.setOnPreparedListener(this)
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

}