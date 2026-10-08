package dev.milk.basket.domain

fun merge(a: ShoppingItem, b: ShoppingItem): ShoppingItem = when {
    a.updatedAt > b.updatedAt -> a
    a.updatedAt < b.updatedAt -> b
    a.deleted != b.deleted -> if (a.deleted) b else a
    a.checked != b.checked -> if (a.checked) b else a
    else -> a
}

fun mergeList(local: List<ShoppingItem>, remote: List<ShoppingItem>): List<ShoppingItem> = (local + remote).groupBy { it.id }.values.map { it.reduce(::merge) }