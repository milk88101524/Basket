package dev.milk.basket.domain

fun merge(a: ShoppingItem, b: ShoppingItem): ShoppingItem {
    return if (a.updatedAt < b.updatedAt) {
        b // a < b
    } else {
        a // a >= b
    }
}