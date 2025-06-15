package model

import java.io.Serializable

data class Transaction(
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val note: String = ""
) : Serializable

