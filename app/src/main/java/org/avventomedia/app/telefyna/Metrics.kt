package org.avventomedia.app.telefyna

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import android.os.SystemClock
import java.io.File

object Metrics {

    private fun getFreeDiskSpace(volume: String): String {
        val path = File(volume)
        return if (path.exists()) {
            val stats = StatFs(path.absolutePath)
            String.format("<b>%s</b> FreeSpace: %d MB<br>", path.absoluteFile, (stats.availableBlocksLong * stats.blockSizeLong) / (1024 * 1024))
        } else {
            ""
        }
    }

    private fun getFreeMemory(): String {
        val mi = ActivityManager.MemoryInfo()
        val activityManager = Monitor.instance?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        return String.format("FreeMemory: %d MB<br>", mi.availMem / (1024 * 1024))
    }

    private fun getUptime(): String {
        return String.format("UpTime: %s", Utils.formatDuration(SystemClock.uptimeMillis()))
    }

    fun retrieve(): String {
        val metrics = StringBuilder()
        metrics.append("Network: ${Utils.logLocalIpAddresses().joinToString(",")}<br>")
        metrics.append(Utils.readUrl("https://ipinfo.io/json"))
        metrics.append(Monitor.instance?.let { getFreeDiskSpace(it.getAuditFilePath("")) })
        metrics.append(Monitor.instance?.let { getFreeDiskSpace(it.getProgramsFolderPath(false)) })
        metrics.append(Monitor.instance?.let { getFreeDiskSpace(it.getProgramsFolderPath(true)) })
        metrics.append(getFreeMemory())
        metrics.append(getUptime())
        return metrics.toString()
    }
}