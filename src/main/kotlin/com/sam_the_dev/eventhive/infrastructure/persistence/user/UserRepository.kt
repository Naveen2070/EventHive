package com.sam_the_dev.eventhive.infrastructure.persistence.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Long>{
    fun findByEmail(email: String): UserEntity?
    fun findByUsernameOrEmail(username: String, email: String): UserEntity?
    fun existsByUsername(username: String): Boolean
}