package org.avventomedia.app.telefyna.listen

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import org.avventomedia.app.telefyna.Monitor

class AutoStart : BroadcastReceiver() {
    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val i = Intent(context, Monitor::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10000, pendingIntent) // wait for 10 seconds
        }
        Log.d("AutoStart", "Boot completed received")
    }
}