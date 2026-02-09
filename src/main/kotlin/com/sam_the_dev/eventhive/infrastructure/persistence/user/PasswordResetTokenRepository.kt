package com.sam_the_dev.eventhive.infrastructure.persistence.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, Long> {
    fun findByToken(token: String): Optional<PasswordResetTokenEntity>
    fun deleteByUser_Id(userId: Long) 
}