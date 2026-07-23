/*
 * Copyright (C) 2021 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.avventomedia.app.telefyna.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.extractor.ts.TsExtractor.TS_PACKET_SIZE
import io.github.thibaultbee.srtdroid.core.enums.Transtype
import io.github.thibaultbee.srtdroid.core.extensions.connect
import io.github.thibaultbee.srtdroid.core.models.SrtSocket
import io.github.thibaultbee.srtdroid.core.models.SrtUrl
import org.avventomedia.app.telefyna.audit.AuditLog
import org.avventomedia.app.telefyna.audit.Logger
import java.io.IOException
import java.util.LinkedList
import java.util.Queue

@OptIn(UnstableApi::class)
class SrtDataSource : BaseDataSource(/*isNetwork*/ true) {

    companion object {
        private const val DEFAULT_PAYLOAD_SIZE = 1316
    }

    private var socket: SrtSocket? = null
    private var srtUrl: SrtUrl? = null

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val uri = dataSpec.uri
        val srtUrl = try {
            SrtUrl(uri)
        } catch (e: Exception) {
            Logger.log(AuditLog.Event.ERROR, "Invalid SRT URI: $uri")
            throw IOException("Invalid SRT URI: $uri", e)
        }

        try {
            socket = SrtSocket().apply {
                Logger.log(AuditLog.Event.CONNECTING, "Connecting to SRT ${srtUrl.hostname}:${srtUrl.port}...")
                connect(srtUrl)
            }
            this.srtUrl = srtUrl
            transferStarted(dataSpec)
            return C.LENGTH_UNSET.toLong()
        } catch (e: Exception) {
            Logger.log(AuditLog.Event.ERROR, "Failed to connect to SRT stream: ${e.message}")
            close()
            throw IOException("SRT connection failed: ${e.message}", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        }

        val currentSocket = socket ?: throw IOException("SRT Socket is closed")

        return try {
            val rcvBuffer = currentSocket.recv(length.coerceAtMost(DEFAULT_PAYLOAD_SIZE))
            if (rcvBuffer.isEmpty()) {
                return C.RESULT_END_OF_INPUT
            }
            val bytesToCopy = rcvBuffer.size.coerceAtMost(length)
            System.arraycopy(rcvBuffer, 0, buffer, offset, bytesToCopy)
            bytesToCopy
        } catch (e: Exception) {
            // Stream EOF or socket disconnection
            C.RESULT_END_OF_INPUT
        }
    }

    override fun getUri(): Uri? {
        return srtUrl?.let { Uri.parse(it.srtUri.toString()) }
    }

    override fun close() {
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore close exceptions
        } finally {
            socket = null
            srtUrl = null
            transferEnded()
        }
    }
}