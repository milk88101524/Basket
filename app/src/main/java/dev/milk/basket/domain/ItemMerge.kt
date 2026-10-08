package dev.milk.basket.domain

fun merge(a: ShoppingItem, b: ShoppingItem): ShoppingItem {
    return if (a.updatedAt < b.updatedAt) {
        b // a < b
    } else {
        a // a >= b
    }
}

fun mergeList(local: List<ShoppingItem>, remote: List<ShoppingItem>): List<ShoppingItem> = remote