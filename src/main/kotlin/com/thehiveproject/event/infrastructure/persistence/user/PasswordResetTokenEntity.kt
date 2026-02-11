package com.thehiveproject.event.infrastructure.persistence.user

import com.thehiveproject.event.infrastructure.persistence.base.AuditableEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetTokenEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var token: String,

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    var user: UserEntity,

    @Column(nullable = false)
    var expiryDate: LocalDateTime
): AuditableEntity()