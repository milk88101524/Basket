package dev.milk.basket.domain

import dev.milk.basket.ShoppingEvent
import dev.milk.basket.ShoppingState
import dev.milk.basket.ShoppingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoppingViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `on start the list is the local one`() {
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

            assertEquals(ShoppingState.Loaded(listOf(milk)), vm.state.value)
        }
    }
}