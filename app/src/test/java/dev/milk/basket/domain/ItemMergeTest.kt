package dev.milk.basket.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `when updatedAt is equal and only the name differs the one that sorts first is kept`() {
        val apple = ShoppingItem("milk", "apple", false, false, 1_000)
        val milk = apple.copy(name = "milk")

        assertEquals(apple, merge(apple, milk))
        assertEquals(apple, merge(milk, apple))
    }

    @Test
    fun `merge gives the same answer whichever item comes first`() {
        val items = listOf(
            ShoppingItem("milk", "apple", false, false, 1_000),   // 基準
            ShoppingItem("milk", "milk", false, false, 1_000),   // name 不同
            ShoppingItem("milk", "apple", true, false, 1_000),   // checked 不同
            ShoppingItem("milk", "apple", false, true, 1_000),   // deleted 不同
        )

        for (a in items) {
            for (b in items) {
                assertEquals(merge(a, b), merge(b, a))
            }
        }
    }

    @Test
    fun `merging an item with itself gives the same item`() {
        val items = listOf(
            ShoppingItem("milk", "apple", false, false, 1_000),   // 基準
            ShoppingItem("milk", "milk", false, false, 1_000),   // name 不同
            ShoppingItem("milk", "apple", true, false, 1_000),   // checked 不同
            ShoppingItem("milk", "apple", false, true, 1_000),   // deleted 不同
        )

        for (a in items) {
            assertEquals(a, merge(a, a))
        }
    }

    @Test
    fun `when both are deleted the result stays deleted whichever is later`() {
        val earlier = ShoppingItem("milk", "milk", false, true, 1_000)
        val later = earlier.copy(name = "milk 2L", updatedAt = 2_000)

        assertTrue(merge(earlier, later).deleted)
        assertTrue(merge(later, earlier).deleted)
    }

    @Test
    fun `the later edit wins as a whole so an earlier change to another field is lost`() {
        val original = ShoppingItem("milk", "apple", false, false, 1_000)

        val nameEdit = original.copy(name = "milk 2L", updatedAt = 1_500)
        val checkedEdit = original.copy(checked = true, updatedAt = 2_000)

        assertEquals(checkedEdit, merge(nameEdit, checkedEdit))
        assertEquals(checkedEdit, merge(checkedEdit, nameEdit))
    }

    @Test
    fun `a later delete wins over an earlier edit`() {
        val original = ShoppingItem("milk", "milk", false, false, 1_000)
        val edited   = original.copy(name = "milk 2L", updatedAt = 2_000)
        val deleted  = original.copy(deleted = true, updatedAt = 3_000)
        assertEquals(deleted, merge(edited, deleted))
        assertEquals(deleted, merge(deleted, edited))
    }

    @Test
    fun `a later edit wins over an earlier delete`() {
        val original = ShoppingItem("milk", "milk", false, false, 1_000)
        val edited   = original.copy(name = "milk 2L", updatedAt = 2_000)
        val deleted  = original.copy(deleted = true, updatedAt = 1_500)
        assertEquals(edited, merge(edited, deleted))
        assertEquals(edited, merge(deleted, edited))
    }

    @Test
    fun `an item replaced by a newer remote copy is reported`() {
        val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
        val tea = ShoppingItem(id = "tea", name = "tea", checked = false, deleted = false, updatedAt = 1_000)
        val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)
        val milk2l = milk.copy(name = "milk_2L", updatedAt = 2_000)
        val local =listOf(milk, tea, egg)
        val remote =listOf(tea, egg, milk2l)

        assertEquals(listOf(milk2l), changedByRemote(local, remote))
    }

    @Test
    fun `an item only the remote side has is not reported`() {
        val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
        val tea = ShoppingItem(id = "tea", name = "tea", checked = false, deleted = false, updatedAt = 1_000)
        val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)
        val local = emptyList<ShoppingItem>()
        val remote = listOf(milk, tea, egg)
        assertEquals(emptyList<ShoppingItem>(), changedByRemote(local, remote))
    }

    @Test
    fun `an item deleted on the remote side is reported`() {
        val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
        val tea = ShoppingItem(id = "tea", name = "tea", checked = false, deleted = false, updatedAt = 1_000)
        val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)
        val eggDeleted = egg.copy(deleted = true, updatedAt = 2_000)
        val local = listOf(milk, tea, egg)
        val remote = listOf(milk, tea, eggDeleted)
        assertEquals(listOf(eggDeleted), changedByRemote(local, remote))
    }

    @Test
    fun `an item the local side wins is not reported`() {
        val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
        val tea = ShoppingItem(id = "tea", name = "tea", checked = false, deleted = false, updatedAt = 1_000)
        val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)
        val eggDeleted = egg.copy(deleted = true, updatedAt = 2_000)
        val local = listOf(milk, tea, eggDeleted)
        val remote = listOf(milk, tea, egg)
        assertEquals(emptyList<ShoppingItem>(), changedByRemote(local, remote))
    }

    @Test
    fun `identical lists report nothing`() {
        val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
        val tea = ShoppingItem(id = "tea", name = "tea", checked = false, deleted = false, updatedAt = 1_000)
        val egg = ShoppingItem(id = "egg", name = "egg", checked = false, deleted = false, updatedAt = 1_000)
        val local = listOf(milk, tea, egg)
        val remote = listOf(milk, tea, egg)
        assertEquals(emptyList<ShoppingItem>(), changedByRemote(local, remote))
    }

    @Test
    fun `an item only the local side has is not reported`() {
        val milk = ShoppingItem(id = "milk", name = "milk", checked = false, deleted = false, updatedAt = 1_000)
        val local = listOf(milk)
        val remote = emptyList<ShoppingItem>()
        assertEquals(emptyList<ShoppingItem>(), changedByRemote(local, remote))
    }
}