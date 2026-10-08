package dev.milk.basket.domain

fun merge(a: ShoppingItem, b: ShoppingItem): ShoppingItem {
    return if (a.updateAt < b.updateAt) {
        b // a < b
    } else {
        a // a >= b
    }
}