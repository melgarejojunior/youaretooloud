package com.example.melgarejo.youaretooloud

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Created by gus
 */
class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}