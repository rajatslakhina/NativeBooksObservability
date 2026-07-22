package com.example.nativebooks

import com.example.nativebooks.auth.LoginValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginValidatorTest {
    private val validator = LoginValidator()

    @Test
    fun acceptsAReasonableLogin() {
        assertTrue(validator.validate("reader@example.com", "books123").isValid)
    }

    @Test
    fun rejectsMalformedEmailAndShortPassword() {
        assertFalse(validator.validate("reader", "123").isValid)
        assertFalse(validator.validate("reader@example.com", "123").isValid)
    }
}
