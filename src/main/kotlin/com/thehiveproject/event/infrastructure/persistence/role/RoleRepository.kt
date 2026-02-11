package com.thehiveproject.event.infrastructure.persistence.role

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<RoleEntity, Long> {
    fun findByName(name: String): RoleEntity?
}