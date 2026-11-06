package dev.milk.basket

import dev.milk.basket.domain.ShoppingItem
import dev.milk.basket.domain.ShoppingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShoppingViewModel(
    private val repo: ShoppingRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(ShoppingState())
    val state: StateFlow<ShoppingState> = _state.asStateFlow()

    fun add(event: ShoppingEvent) {
        when(event) {
            ShoppingEvent.Started -> loadData()
        }
    }

    private fun loadData() {
        scope.launch {
            try {
                val local = repo.getItems()
                _state.update { ShoppingState(items = local, syncState = SyncStatus.Syncing) }
                val changed = repo.sync()
                val merged = repo.getItems()
                _state.update { ShoppingState(items = merged, syncState = SyncStatus.Done, changed = changed) }
            } catch (e: Exception) {
                val latest = try { repo.getItems() } catch (_: Exception) { null }
                _state.update { it.copy(items = latest ?: it.items, syncState = SyncStatus.Failed(e)) }
            }
        }
    }
}

data class ShoppingState(
    val items: List<ShoppingItem> = emptyList(),
    val syncState: SyncStatus = SyncStatus.Idle,
    val changed: List<ShoppingItem> = emptyList()
)

sealed interface SyncStatus {
    data object Idle: SyncStatus
    data object Syncing: SyncStatus
    data object Done: SyncStatus
    data class Failed(val error: Exception): SyncStatus
}

sealed interface ShoppingEvent {
    data object Started: ShoppingEvent
}