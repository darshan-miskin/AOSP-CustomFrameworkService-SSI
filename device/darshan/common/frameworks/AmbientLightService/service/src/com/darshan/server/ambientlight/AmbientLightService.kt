package com.darshan.server.ambientlight

import android.content.Context
import android.os.Binder
import android.os.Handler
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.SparseIntArray
import android.util.Slog
import com.android.server.FgThread
import com.darshan.ambientlight.IAmbientLightListener
import com.darshan.ambientlight.IAmbientLightService
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintWriter

class AmbientLightService(private val context: Context) : IAmbientLightService.Stub() {

    private val mListeners = RemoteCallbackList<IAmbientLightListener>()
    private val mClientListenerCounts = SparseIntArray()
    private val MAX_LISTENERS_PER_UID = 5

    private val mHandler = Handler(FgThread.get().looper)
    private var mCurrentLux: Int = 400

    init {
        Slog.d(TAG, "Initializing AmbientLightService binder architecture...")
    }

    private fun enforceAccessPermission() {
        context.enforceCallingOrSelfPermission(
            "com.darshan.permission.ACCESS_AMBIENT_LIGHT",
            "AmbientLightService access denied"
        )
    }

    override fun getLux(): Int {
        enforceAccessPermission()
        synchronized(this) {
            return mCurrentLux
        }
    }

    override fun setLux(lux: Int) {
        enforceAccessPermission()
        synchronized(this) {
            mCurrentLux = lux
        }
        notifyLuxChanged(lux)
    }

    override fun registerCallback(listener: IAmbientLightListener?) {
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

    override fun unregisterCallback(listener: IAmbientLightListener?) {
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

    fun notifyLuxChanged(newLux: Int) {
        mHandler.post {
            val broadcastCount = mListeners.beginBroadcast()
            for (i in 0 until broadcastCount) {
                try {
                    mListeners.getBroadcastItem(i).onLuxChanged(newLux)
                } catch (e: RemoteException) {
                    // Handled automatically by RemoteCallbackList context tracking
                }
            }
            mListeners.finishBroadcast()
        }
    }
    
    override fun getInterfaceVersion() = IAmbientLightService.VERSION
    override fun getInterfaceHash() = IAmbientLightService.HASH

    override fun dump(fd: FileDescriptor, args: Array<out String>?) {
        if (context.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            PrintWriter(FileOutputStream(fd)).use { it.println("Permission Denial: can't dump AmbientLightService") }
            return
        }

        PrintWriter(FileOutputStream(fd)).use { pw ->
            pw.println("--- AmbientLightService Status ---")
            synchronized(this) {
                pw.println("  Current Lux Metric: $mCurrentLux")
            }
            pw.println("  Active Listeners: ${mListeners.registeredCallbackCount}")
            pw.println("----------------------------------------")
        }
    }

    override fun onShellCommand(
        inF: FileDescriptor?, out: FileDescriptor?, err: FileDescriptor?,
        args: Array<out String>, callback: android.os.ShellCallback?,
        resultReceiver: android.os.ResultReceiver
    ) {
        val callingUid = Binder.getCallingUid()
        if (callingUid != 0 && callingUid != 2000) {
            resultReceiver.send(-1, null)
            return
        }

        var returnCode = 0
        PrintWriter(FileOutputStream(out ?: FileDescriptor.out)).use { writer ->
            try {
                if (args.isNotEmpty()) {
                    when (args[0]) {
                        "set" -> {
                            val value = args.getOrNull(1)?.toIntOrNull() ?: 0
                            synchronized(this) { mCurrentLux = value }
                            notifyLuxChanged(value)
                            writer.println("Successfully set ambient lux to $value")
                        }
                        "get" -> {
                            val value = synchronized(this) { mCurrentLux }
                            writer.println("Current ambient lux: $value")
                        }
                        else -> {
                            writer.println("Unknown command. Usage: set [value] or get")
                            returnCode = -1
                        }
                    }
                } else {
                    writer.println("AmbientLightService commands: set, get")
                    returnCode = -1
                }
            } catch (e: Exception) {
                writer.println("Exception error on loop: ${e.message}")
                returnCode = -1
            }
        }
        resultReceiver.send(returnCode, null)
    }

    companion object {
        private const val TAG = "AmbientLightService"
    }
}
