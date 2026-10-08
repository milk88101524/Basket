package dev.milk.basket.domain

fun merge(a: ShoppingItem, b: ShoppingItem): ShoppingItem {
    // name 先不考慮
    return if (a.updatedAt == b.updatedAt) {
        if (!a.deleted) { a } else { b }
    } else {
        if (a.updatedAt < b.updatedAt) {
            b // a < b
        } else {
            a // a >= b
        }
    }
}

fun mergeList(local: List<ShoppingItem>, remote: List<ShoppingItem>): List<ShoppingItem> = (local + remote).groupBy { it.id }.values.map { it.reduce(::merge) }