package dev.milk.basket.domain

data class ShoppingItem(
    val id: String,
    val name: String,
    val checked: Boolean,
    val deleted: Boolean,
    val updateAt: Long
)
