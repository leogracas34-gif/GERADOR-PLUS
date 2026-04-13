package com.geradorplus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GeradorPlusApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
