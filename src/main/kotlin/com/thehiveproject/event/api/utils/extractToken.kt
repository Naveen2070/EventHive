package com.thehiveproject.event.api.utils

fun extractToken(header: String): String {
    return if (header.startsWith("Bearer ")) header.substring(7) else header
}