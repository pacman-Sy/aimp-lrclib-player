package com.aimp.player

import android.app.Application

class AIMPApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: AIMPApplication
            private set
    }
}
