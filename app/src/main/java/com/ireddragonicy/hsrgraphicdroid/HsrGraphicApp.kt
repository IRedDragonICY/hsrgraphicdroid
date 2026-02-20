package com.ireddragonicy.hsrgraphicdroid

import android.app.Application
import android.util.Log
import com.topjohnwu.superuser.Shell

class HsrGraphicApp : Application() {
    
    companion object {
        private const val TAG = "HsrGraphicApp"
        
        init {
            // Initialize libsu with support for KernelSU, APatch, and Magisk
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    // FLAG_MOUNT_MASTER is required for KernelSU and APatch
                    // to properly access app data directories
                    .setFlags(Shell.FLAG_REDIRECT_STDERR or Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        // Dynamic colors are handled by Compose's dynamicColorScheme() in HsrGraphicTheme
        
        // Pre-initialize shell in background to avoid UI delay
        Shell.getShell { shell ->
            Log.d(TAG, "Shell initialized - isRoot: ${shell.isRoot}")
            if (!shell.isRoot) {
                Log.w(TAG, "Root access not available. Please grant root permission.")
            }
        }
    }
}
