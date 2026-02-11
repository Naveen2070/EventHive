package com.thehiveproject.event.infrastructure.notification

import com.thehiveproject.event.domain.user.event.PasswordChangedEvent
import com.thehiveproject.event.domain.user.event.PasswordResetInitiatedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UserEventListener(
    private val emailService: EmailService
) {

    @Async
    @EventListener
    fun handlePasswordReset(event: PasswordResetInitiatedEvent) {
        emailService.sendPasswordResetLink(
            to = event.email,
            token = event.token,
            username = event.username
        )
    }

    @Async
    @EventListener
    fun handlePasswordChanged(event: PasswordChangedEvent) {
        emailService.sendPasswordChangedAlert(
            to = event.email,
            username = event.username
        )
    }
}