package com.theapache64.mlkit.sample

import android.app.Application
import com.fitpolo.support.MokoSupport

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        MokoSupport.getInstance().init(this)
    }
}