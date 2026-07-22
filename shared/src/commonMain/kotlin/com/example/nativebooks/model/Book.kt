package com.example.nativebooks.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val category: String,
    val year: Int,
    val readingMinutes: Int,
)
