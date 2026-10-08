package dev.milk.basket.domain

fun merge(a: ShoppingItem, b: ShoppingItem): ShoppingItem {
    return if (a.updatedAt < b.updatedAt) {
        b // a < b
    } else {
        a // a >= b
    }
}

fun mergeList(local: List<ShoppingItem>, remote: List<ShoppingItem>): List<ShoppingItem> {
    val result = remote.toMutableList()  // 轉成可編輯的 list (list 是唯讀)
    for (itemLocal in local) {
        if (result.none { it.id == itemLocal.id}) {
            result.add(itemLocal)
        }
    }
    return result
}