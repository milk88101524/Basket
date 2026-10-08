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

    @Test
    fun `an item only the local has is kept`() {
        val milk = ShoppingItem("milk", "milk", false, false, 1_000)
        val result = mergeList(local = listOf(milk), remote = emptyList())
        assertEquals(listOf(milk), result)
    }

    @Test
    fun `the same item in both lists is merged by the rule of the later one`() {
        val remoteMilk = ShoppingItem("milk", "milk", false, false, 1_000)
        val localMilk = remoteMilk.copy(name = "milk 2L", updatedAt = 2_000)
        val result = mergeList(local = listOf(localMilk), remote = listOf(remoteMilk))
        assertEquals(listOf(localMilk), result)
    }

    @Test
    fun `an older local item does not replace a newer remote item`() {
        val localMilk = ShoppingItem("milk", "milk", false, false, 1_000)
        val remoteMilk = localMilk.copy(name = "milk 2L", updatedAt = 2_000)

        val result = mergeList(local = listOf(localMilk), remote = listOf(remoteMilk))

        assertEquals(listOf(remoteMilk), result)
    }

    @Test
    fun `two local items with the same id are merged into one`() {
        val older = ShoppingItem("milk", "milk", false, false, 1_000)
        val newer = older.copy(name = "milk 2L", updatedAt = 2_000)
        val result = mergeList(local = listOf(older, newer), remote = emptyList())
        assertEquals(listOf(newer), result)
    }

    @Test
    fun `when updatedAt is equal the item that is not deleted is kept`() {
        val kept = ShoppingItem("milk", "milk", false, false, 1_000)
        val gone = kept.copy(deleted = true)

        assertEquals(kept, merge(kept, gone))
        assertEquals(kept, merge(gone, kept))
    }

    @Test
    fun `when updatedAt is equal the item that is not checked is kept`() {
        val kept = ShoppingItem("milk", "milk", false, false, 1_000)
        val ticked = kept.copy(checked = true)

        assertEquals(kept, merge(kept, ticked))
        assertEquals(kept, merge(ticked, kept))
    }

    @Test
    fun `when updatedAt is equal deleted decides before checked`() {
        val notDeleted = ShoppingItem("milk", "milk", true, false, 1_000)
        val deletedOnly = ShoppingItem("milk", "milk", false, true, 1_000)

        assertEquals(notDeleted, merge(notDeleted, deletedOnly))
        assertEquals(notDeleted, merge(deletedOnly, notDeleted))
    }

    @Test
    fun `when updatedAt is equal and both are deleted the one that is not checked is kept`() {
        val deletedNotChecked = ShoppingItem("milk", "milk", false, true, 1_000)
        val deletedChecked = deletedNotChecked.copy(checked = true)

        assertEquals(deletedNotChecked, merge(deletedNotChecked, deletedChecked))
        assertEquals(deletedNotChecked, merge(deletedChecked, deletedNotChecked))
    }
}