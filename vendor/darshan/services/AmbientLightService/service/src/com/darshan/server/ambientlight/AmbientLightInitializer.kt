package com.darshan.server.ambientlight

import android.content.Context
import com.android.server.SystemService

class AmbientLightInitializer(context: Context) : SystemService(context) {
    private val mService = AmbientLightService(context)
    override fun onStart() {
        publishBinderService("ambient_light_service", mService)
    }
}