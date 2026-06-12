package edu.pwr.zpi.netwalk

import android.util.Log

// Simple, consistent logging helpers using a single tag.
const val LOG_TAG = "NetWalk"

fun logD(message: String) = Log.d(LOG_TAG, message)

fun logI(message: String) = Log.i(LOG_TAG, message)

fun logW(message: String) = Log.w(LOG_TAG, message)

fun logE(
    message: String,
    t: Throwable? = null,
) {
    if (t != null) Log.e(LOG_TAG, message, t) else Log.e(LOG_TAG, message)
}
