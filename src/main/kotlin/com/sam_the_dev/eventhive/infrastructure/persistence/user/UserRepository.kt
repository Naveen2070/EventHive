package com.sam_the_dev.eventhive.infrastructure.persistence.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Long>{
    fun findByUsernameOrEmail(username: String, email: String): UserEntity?
}