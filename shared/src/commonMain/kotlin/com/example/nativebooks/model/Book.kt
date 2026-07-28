package com.example.nativebooks.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val category: String,
    val year: Int,
    val readingMinutes: Int,
)
