package dev.milk.basket

import dev.milk.basket.domain.ShoppingItem
import dev.milk.basket.domain.ShoppingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShoppingViewModel(
    repo: ShoppingRepository,
    scope: CoroutineScope
) {
    private val _state = MutableStateFlow<ShoppingState>(ShoppingState.Initial)
    val state: StateFlow<ShoppingState> = _state.asStateFlow()

    fun add(event: ShoppingEvent) {

    }
}

sealed interface ShoppingState {
    data object Initial: ShoppingState
    data object Loading: ShoppingState
    data class Loaded(val item: List<ShoppingItem>): ShoppingState
    data class Error(val error: Exception): ShoppingState
}

sealed interface ShoppingEvent {
    data object Started: ShoppingEvent
}