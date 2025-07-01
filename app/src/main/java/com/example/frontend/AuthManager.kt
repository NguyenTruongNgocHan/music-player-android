package com.example.frontend

object AuthManager {
    private var guest = true

    fun isGuest(): Boolean = guest

    fun setGuestMode(value: Boolean) {
        guest = value
    }
}
