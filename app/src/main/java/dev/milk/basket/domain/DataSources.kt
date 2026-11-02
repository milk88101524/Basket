package dev.milk.basket.domain

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
        return changed
    }
}