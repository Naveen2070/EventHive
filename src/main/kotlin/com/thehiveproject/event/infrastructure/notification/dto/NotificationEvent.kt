package com.thehiveproject.event.infrastructure.notification.dto

import java.io.Serializable

data class NotificationEvent(
    val recipientEmail: String,
    val subject: String,
    val templateCode: String,
    val variables: Map<String, String>
) : Serializable