package com.cvc953.localplayer

import android.app.Application
import com.cvc953.localplayer.model.SongRepository

class LocalPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Locale will be applied in MainActivity.onCreate()
        SongRepository.getInstance(this).startObserving()
    }
}
