package com.darshan.server.ambientlight

import android.content.Context
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.SparseIntArray
import com.darshan.ambientlight.IAmbientLightListener
import com.darshan.ambientlight.IAmbientLightService

class AmbientLightService(private val context: Context) : IAmbientLightService.Stub() {

    private val mListeners = RemoteCallbackList<IAmbientLightListener>()

    private val mClientListenerCounts = SparseIntArray()
    private val MAX_LISTENERS_PER_UID = 5

    private val mWorkerThread = HandlerThread("AmbientLightWorker").apply { start() }
    private val mHandler = Handler(mWorkerThread.looper)

    private fun enforceAccessPermission() {
        context.enforceCallingOrSelfPermission(
            "com.darshan.permission.ACCESS_AMBIENT_LIGHT",
            "AmbientLightService access denied"
        )
    }

    override fun registerListener(listener: IAmbientLightListener?) {
        enforceAccessPermission()
        if (listener == null) return

        val uid = Binder.getCallingUid()
        synchronized(mClientListenerCounts) {
            val count = mClientListenerCounts.get(uid, 0)
            if (count >= MAX_LISTENERS_PER_UID) {
                throw SecurityException("Exceeded maximum listener limit ($MAX_LISTENERS_PER_UID) for UID $uid")
            }
            if (mListeners.register(listener, uid)) {
                mClientListenerCounts.put(uid, count + 1)
            }
        }
    }

    override fun unregisterListener(listener: IAmbientLightListener?) {
        enforceAccessPermission()
        if (listener == null) return

        val uid = Binder.getCallingUid()
        synchronized(mClientListenerCounts) {
            if (mListeners.unregister(listener)) {
                val count = mClientListenerCounts.get(uid, 0)
                if (count > 1) {
                    mClientListenerCounts.put(uid, count - 1)
                } else {
                    mClientListenerCounts.delete(uid)
                }
            }
        }
    }

    fun notifyLuxChanged(newLux: Float) {
        mHandler.post {
            val broadcastCount = mListeners.beginBroadcast()
            for (i in 0 until broadcastCount) {
                try {
                    mListeners.getBroadcastItem(i).onLuxChanged(newLux)
                } catch (e: RemoteException) {
                    // Handled automatically by RemoteCallbackList
                }
            }
            mListeners.finishBroadcast()
        }
    }
}