package edu.pwr.zpi.netwalk.network

import android.content.Context
import edu.pwr.zpi.netwalk.logD
import edu.pwr.zpi.netwalk.logI
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
        logI("[PendingBatchStore: save] Saved batch to ${file.name}, size=${bytes.size} bytes")
        return file
    }

    fun listFilesSorted(): List<File> {
        val files = dir
            .listFiles()
            ?.asList()
            ?.filter { it.isFile && it.name.endsWith(".json.gz") }
            ?.sortedBy { it.name }
            ?: emptyList()
        logD("[PendingBatchStore: listFilesSorted] Found ${files.size} pending files")
        return files
    }

    fun delete(file: File) {
        val ok = file.delete()
        logI("[PendingBatchStore: delete] Deleted ${file.name}: $ok")
    }
}
