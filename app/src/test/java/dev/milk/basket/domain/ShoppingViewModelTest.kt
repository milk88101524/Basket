package dev.milk.basket.domain

import dev.milk.basket.ShoppingEvent
import dev.milk.basket.ShoppingState
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
}