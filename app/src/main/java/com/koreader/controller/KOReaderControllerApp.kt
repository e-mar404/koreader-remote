package com.koreader.controller

import android.app.Application
import com.koreader.controller.data.KOReaderClient
import com.koreader.controller.data.SettingsRepository

class KOReaderControllerApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    
    lateinit var koreaderClient: KOReaderClient
        private set
    
    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        koreaderClient = KOReaderClient()
    }
}
