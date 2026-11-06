package dev.milk.basket.domain

import dev.milk.basket.ShoppingEvent
import dev.milk.basket.ShoppingViewModel
import dev.milk.basket.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on start the list is the local one`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
//            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk)
            val remote = listOf(milk)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote)
            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            val vm = ShoppingViewModel(repo, backgroundScope)
            vm.add(ShoppingEvent.Started)
            runCurrent()

            assertEquals(listOf(milk), vm.state.value.items)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `after Started the sync runs and the list shows the remote item too`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk)
            val remote = listOf(egg)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote)
            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            val vm = ShoppingViewModel(repo, backgroundScope)
            vm.add(ShoppingEvent.Started)
            runCurrent()

            assertEquals(listOf(milk, egg), vm.state.value.items)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `sync fails, the list stays the local one and the status is Failed`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk)
            val remote = listOf(egg)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote, failOnRead = true)
            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            val vm = ShoppingViewModel(repo, backgroundScope)
            vm.add(ShoppingEvent.Started)
            runCurrent()

            assertTrue(vm.state.value.syncState is SyncStatus.Failed)
            assertEquals(listOf(milk), vm.state.value.items)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `after a successful sync the status is Done`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk)
            val remote = listOf(egg)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote)
            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            val vm = ShoppingViewModel(repo, backgroundScope)
            vm.add(ShoppingEvent.Started)
            runCurrent()

            assertEquals(SyncStatus.Done, vm.state.value.syncState)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `after Started the state's changed holds the items the remote changed`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk)
            val remote = listOf(milkNew, egg)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote)
            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            val vm = ShoppingViewModel(repo, backgroundScope)
            vm.add(ShoppingEvent.Started)
            runCurrent()

            assertEquals(listOf(milkNew), vm.state.value.changed)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `while the sync runs the status is Syncing`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk)
            val remote = listOf(milkNew, egg)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote, readDelay = 2_000)
            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            val vm = ShoppingViewModel(repo, backgroundScope)
            vm.add(ShoppingEvent.Started)
            runCurrent()
            assertEquals(SyncStatus.Syncing, vm.state.value.syncState)

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when the upload fails the list is the merged one and the status is Failed`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, egg)
            val remote = listOf(milkNew)

            val fakeLocal = FakeLocalDataSource(local)
            val fakeRemote = FakeRemoteDataSource(remote, failOnWrite = true)
            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            val vm = ShoppingViewModel(repo, backgroundScope)
            vm.add(ShoppingEvent.Started)
            runCurrent()
            assertTrue(vm.state.value.syncState is SyncStatus.Failed)
            assertEquals(listOf(milkNew, egg), vm.state.value.items)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when the first read of the local list fails the list is empty and the status is Failed`() {
        runTest {
            val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
            val milkNew = milk.copy(name = "milk 2L", updatedAt = 2_000)
            val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)

            val local = listOf(milk, egg)
            val remote = listOf(milkNew)

            val fakeLocal = FakeLocalDataSource(local, failOnRead = true)
            val fakeRemote = FakeRemoteDataSource(remote)
            val repo = ShoppingRepository(fakeLocal, fakeRemote)

            val vm = ShoppingViewModel(repo, backgroundScope)
            vm.add(ShoppingEvent.Started)
            runCurrent()
            assertTrue(vm.state.value.syncState is SyncStatus.Failed)
            assertEquals(emptyList<ShoppingItem>(), vm.state.value.items)
        }
    }
}