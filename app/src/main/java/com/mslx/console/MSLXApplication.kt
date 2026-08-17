package com.mslx.console

import android.app.Application
import com.mslx.console.data.AppContainer

class MSLXApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
