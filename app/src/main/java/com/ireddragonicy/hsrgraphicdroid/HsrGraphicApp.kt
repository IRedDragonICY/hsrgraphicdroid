package com.ireddragonicy.hsrgraphicdroid

import android.app.Application
import com.topjohnwu.superuser.Shell

class HsrGraphicApp : Application() {
    
    companion object {
        init {
            // Initialize libsu
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }
    
    override fun onCreate() {
        super.onCreate()
    }
}
