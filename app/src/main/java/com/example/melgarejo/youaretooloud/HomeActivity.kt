package com.example.melgarejo.youaretooloud

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeActivity : Activity() {

    private var mListener: ValueEventListener
    private val TAG = "HomeActivity"

    init {
        mListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.child("sound")
                Log.d(TAG, value.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val database = FirebaseDatabase.getInstance()
        val databaseReference = database.getReference("android_things")
        databaseReference.addValueEventListener(mListener)

    }
}
