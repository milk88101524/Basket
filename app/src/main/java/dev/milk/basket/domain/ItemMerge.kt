package dev.milk.basket.domain

fun merge(a: ShoppingItem, b: ShoppingItem): ShoppingItem = when {
    a.updatedAt > b.updatedAt -> a
    a.updatedAt < b.updatedAt -> b
    a.deleted -> b
    a.checked -> b
    else -> a
}

fun mergeList(local: List<ShoppingItem>, remote: List<ShoppingItem>): List<ShoppingItem> = (local + remote).groupBy { it.id }.values.map { it.reduce(::merge) }