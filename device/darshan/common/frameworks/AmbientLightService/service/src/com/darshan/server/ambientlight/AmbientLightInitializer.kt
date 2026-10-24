package com.darshan.server.ambientlight

import android.content.Context
import android.util.Slog
import com.android.server.SystemService

class AmbientLightInitializer(context: Context) : SystemService(context) {

    companion object {
        private const val TAG = "AmbientLight Initializer"
    }

    override fun onStart() {
        Slog.i(TAG, "Initializing Ambient Light System Service...")
        try {
            val mService = AmbientLightService(context)
            publishBinderService("ambient_light_service", mService)
            Slog.i(TAG, "Ambient Light Service successfully published.")
        } catch (e: Throwable) {
            Slog.e(TAG, "Failed to publish ambient_light_service", e)
            throw e
        }
    }
}