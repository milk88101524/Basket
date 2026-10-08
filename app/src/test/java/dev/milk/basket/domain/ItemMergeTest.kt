package dev.milk.basket.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemMergeTest {
    @Test
    fun `the item updated later wins`() {
        val older = ShoppingItem("milk", "milk", false, false, 1_000)
        val newer = older.copy(name = "milk 2L", updatedAt = 2_000)

        val result = merge(older, newer)
        assertEquals(newer, result)
    }

    @Test
    fun `an item only the remote has is added`() {
        val milk = ShoppingItem("milk", "milk", false, false, 1_000)
        val result = mergeList(local = emptyList(), remote = listOf(milk))
        assertEquals(listOf(milk), result)
    }
}