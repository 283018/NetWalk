package edu.pwr.zpi.netwalk.network

import android.content.Context
import java.io.File
import java.util.UUID

class PendingBatchStore(
    context: Context,
) {
    private val dir = File(context.cacheDir, "pending_measurement_batches").apply {
        mkdirs()
    }

    fun save(bytes: ByteArray): File {
        val name = "${System.currentTimeMillis()}_${UUID.randomUUID()}.json.gz"
        val file = File(dir, name)
        file.writeBytes(bytes)
        return file
    }

    fun listFilesSorted(): List<File> =
        dir
            .listFiles()
            ?.asList()
            ?.filter { it.isFile && it.name.endsWith(".json.gz") }
            ?.sortedBy { it.name }
            ?: emptyList()

    fun delete(file: File) {
        file.delete()
    }
}
