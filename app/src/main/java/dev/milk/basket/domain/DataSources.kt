package dev.milk.basket.domain

import java.io.IOException

interface LocalDataSource {
    suspend fun read(): List<ShoppingItem>
    suspend fun save(items: List<ShoppingItem>)
}

interface RemoteDataSource {
    suspend fun read(): List<ShoppingItem>
    suspend fun write(items: List<ShoppingItem>)
}

class ShoppingRepository(
    private val local: LocalDataSource,
    private val remote: RemoteDataSource,
) {
    suspend fun sync(): List<ShoppingItem> {
        // get local & remote data
        val localItems = local.read()
        val remoteItems = remote.read()

        // mergeList
        val merged = mergeList(localItems, remoteItems)

        // save local data
        local.save(merged)

        // changedByRemote
        val changed = changedByRemote(localItems, remoteItems)

        // toUpload
        val upload = toUpload(localItems, remoteItems)

        if (upload.isNotEmpty()) {
            try {
                remote.write(upload)
            } catch (e: IOException) {
                throw UploadFailed(changed = changed, cause = e)
            }
        }
        return changed
    }

    suspend fun getItems(): List<ShoppingItem> {
        return emptyList()
    }
}

class UploadFailed(
    val changed: List<ShoppingItem>,
    cause: Throwable
): Exception("upload failed", cause)