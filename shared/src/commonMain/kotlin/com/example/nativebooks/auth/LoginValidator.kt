package com.example.nativebooks.auth

data class LoginValidation(
    val isValid: Boolean,
    val message: String,
)

class LoginValidator {
    fun validate(email: String, password: String): LoginValidation {
        val normalizedEmail = email.trim()

        return when {
            normalizedEmail.isEmpty() -> LoginValidation(false, "Enter your email address.")
            '@' !in normalizedEmail || '.' !in normalizedEmail.substringAfter('@', "") ->
                LoginValidation(false, "Enter a valid email address.")
            password.length < 6 -> LoginValidation(false, "Password must have at least 6 characters.")
            else -> LoginValidation(true, "Welcome back.")
        }
    }
}
