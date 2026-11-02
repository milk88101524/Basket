package dev.milk.basket.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeLocalDataSource(var items: List<ShoppingItem>): LocalDataSource {
    override suspend fun read(): List<ShoppingItem> = items

    override suspend fun save(items: List<ShoppingItem>) {
        this.items = items
    }
}

class FakeRemoteDataSource(var items: List<ShoppingItem>): RemoteDataSource {
    val receipts = mutableListOf<List<ShoppingItem>>()

    override suspend fun read(): List<ShoppingItem> = items

    override suspend fun write(items: List<ShoppingItem>) {
        receipts.add(items)
    }
}

class ShoppingRepositoryTest {
    @Test
    fun `remote has a newer milk and a new egg, local is updated, nothing is uploaded and milk is reported`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk)
            val remote = listOf(milkNew, egg)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)
            val changed = repo.sync()

            assertEquals(setOf(milkNew, egg), fakeLocal.items.toSet())
            assertEquals(emptyList<ShoppingItem>(), fakeRemote.receipts)
            assertEquals(listOf(milkNew), changed)
        }
    }
}