package com.darshan.ambientlight.sdk

import android.content.Context
import android.os.IBinder
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import com.darshan.ambientlight.IAmbientLightListener
import com.darshan.ambientlight.IAmbientLightService
import java.util.concurrent.Executor

class AmbientLightManager(context: Context) {

    private val mContext: Context = context.applicationContext
    private var mService: IAmbientLightService? = null
    private val mListeners = HashMap<AmbientLightListener, ListenerTransport>()

    interface AmbientLightListener {
        fun onLuxChanged(lux: Int)
    }

    private val mDeathRecipient = IBinder.DeathRecipient {
        synchronized(mListeners) {
            mService = null
            mListeners.clear()
        }
        Log.w(TAG, "AmbientLightService died. Stale listener bindings cleared.")
    }

    @Synchronized
    private fun getService(): IAmbientLightService? {
        if (mService != null) return mService

        val binder = ServiceManager.getService(TAG_SERVICE)
        if (binder != null) {
            try {
                binder.linkToDeath(mDeathRecipient, 0)
                mService = IAmbientLightService.Stub.asInterface(binder)
            } catch(e: RemoteException) {
                Log.e(TAG, "Failed to link DeathRecipient to binder", e)
            }
        } else {
            Log.e(TAG, "Failed to retrieve service binder for $TAG_SERVICE")
        }
        return mService
    }

    fun getLux(): Int {
        val service = getService() ?: return 0
        return try {
            service.getLux()
        } catch(e: RemoteException) {
            Log.e(TAG, "Failed to get lux", e)
            0
        }
    }

    fun registerListener(executor: Executor, listener: AmbientLightListener) {
        val service = getService() ?: return

        synchronized(mListeners) {
            if (mListeners.containsKey(listener)) return

            val transport = ListenerTransport(executor, listener)
            try {
                service.registerCallback(transport)
                mListeners[listener] = transport
            } catch(e: RemoteException) {
                Log.e(TAG, "Failed to register asynchronous listener", e)
            }
        }
    }

    fun unregisterListener(listener: AmbientLightListener) {
        val service = getService() ?: return

        synchronized(mListeners) {
            val transport = mListeners.remove(listener) ?: return
            try {
                service.unregisterCallback(transport)
            } catch(e: RemoteException) {
                Log.e(TAG, "Failed to unregister asynchronous listener", e)
            }
        }
    }

    private class ListenerTransport(
        private val executor: Executor,
        private val listener: AmbientLightListener
    ) : IAmbientLightListener.Stub() {

        override fun onLuxChanged(lux: Int) {
            executor.execute {
                listener.onLuxChanged(lux)
            }
        }

//        override fun getInterfaceVersion(): Int = IAmbientLightListener.VERSION
//        override fun getInterfaceHash(): String = IAmbientLightListener.HASH
    }

    companion object {
        private const val TAG = "AmbientLight Manager"
        private const val TAG_SERVICE = "ambient_light_service"
    }
}