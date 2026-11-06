package dev.milk.basket.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class FakeLocalDataSource(
    var items: List<ShoppingItem>,
    var failOnSave: Boolean = false,
    var failOnRead: Boolean = false,
    val readDelay: Long = 0
): LocalDataSource {
    override suspend fun read(): List<ShoppingItem> {
        delay(readDelay.milliseconds)
        return if (failOnRead) {
            throw IllegalStateException("disk fail")
        } else {
            items
        }
    }

    override suspend fun save(items: List<ShoppingItem>) {
        if (failOnSave) {
            throw IllegalStateException("disk full")
        }
        this.items = items
    }
}

class FakeRemoteDataSource(
    var items: List<ShoppingItem>,
    var failOnWrite: Boolean = false,
    var failOnRead: Boolean = false,
    val readDelay: Long = 0
): RemoteDataSource {
    val receipts = mutableListOf<List<ShoppingItem>>()

    override suspend fun read(): List<ShoppingItem> {
        delay(readDelay.milliseconds)
        return if (failOnRead) {
            throw IOException("offline")
        } else {
            items
        }
    }

    override suspend fun write(items: List<ShoppingItem>) {
        if (failOnWrite) {
            throw IOException("offline")
        }
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
            assertEquals(emptyList<List<ShoppingItem>>(), fakeRemote.receipts)
            assertEquals(listOf(milkNew), changed)
        }
    }

    @Test
    fun `local has a bread added offline, bread is uploaded and nothing is reported`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val bread = ShoppingItem(id = "bread", name = "bread", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, bread)
            val remote = listOf(milk)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)
            val changed = repo.sync()

            assertEquals(listOf(milk, bread), fakeLocal.items)
            assertEquals(listOf(listOf(bread)), fakeRemote.receipts)
            assertEquals(emptyList<ShoppingItem>(), changed)
        }
    }

    @Test
    fun `upload fails, the merged list is still saved locally and the exception reaches the caller`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val bread = ShoppingItem(id = "bread", name = "bread", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, bread)
            val remote = listOf(milkNew)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote, failOnWrite = true)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)
            val result = runCatching { repo.sync() }

            assertEquals(listOf(milkNew, bread), fakeLocal.items)
            assertTrue(result.exceptionOrNull()?.cause is IOException)
        }
    }

    @Test
    fun `upload fails, the exception carries the items the remote changed`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val bread = ShoppingItem(id = "bread", name = "bread", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, bread)
            val remote = listOf(milkNew)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote, failOnWrite = true)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)
            val result = runCatching { repo.sync() }
            val error = result.exceptionOrNull() as? UploadFailed

            assertNotNull(error)
            assertEquals(listOf(milkNew), error?.changed)
        }
    }

    @Test
    fun `upload fails once, the next sync uploads the bread again`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val bread = ShoppingItem(id = "bread", name = "bread", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, bread)
            val remote = listOf(milkNew)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote, failOnWrite = true)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)
            val result = runCatching { repo.sync() }
            val error = result.exceptionOrNull() as? UploadFailed

            assertNotNull(error)
            fakeRemote.failOnWrite = false

            repo.sync()
            assertEquals(listOf(listOf(bread)), fakeRemote.receipts)
        }
    }

    @Test
    fun `local has a new bread and remote has a newer milk, bread is uploaded and milk is reported`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val bread = ShoppingItem(id = "bread", name = "bread", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, bread)
            val remote = listOf(milkNew)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)
            val result = repo.sync()

            assertEquals(listOf(milkNew), result)
            assertEquals(listOf(listOf(bread)), fakeRemote.receipts)
            assertEquals(listOf(milkNew, bread), fakeLocal.items)
        }
    }

    @Test
    fun `remote cannot be read, nothing is uploaded, the local list is left as it is and the IOException reaches the caller`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val bread = ShoppingItem(id = "bread", name = "bread", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, bread)
            val remote = listOf(milkNew)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote, failOnRead = true)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)
            val result = runCatching { repo.sync() }

            assertTrue(result.exceptionOrNull() is IOException)
            assertEquals(emptyList<List<ShoppingItem>>(), fakeRemote.receipts)
            assertEquals(listOf(milk, bread), fakeLocal.items)
        }
    }

    @Test
    fun `local list cannot be saved, nothing is uploaded and the exception reaches the caller`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val bread = ShoppingItem(id = "bread", name = "bread", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, bread)
            val remote = listOf(milkNew)

            val fakeLocal = FakeLocalDataSource(local, failOnSave = true)
            val fakeRemote = FakeRemoteDataSource(remote)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)
            val result = runCatching { repo.sync() }

            assertTrue(result.exceptionOrNull() is IllegalStateException)
            assertEquals(emptyList<List<ShoppingItem>>(), fakeRemote.receipts)
        }
    }

    @Test
    fun `getItems returns the local list and ignores the remote`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk)
            val remote = listOf(egg)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote)

            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            assertEquals(local, repo.getItems())
        }
    }
}