package com.sam_the_dev.eventhive.infrastructure.persistence.user

import com.sam_the_dev.eventhive.infrastructure.persistence.base.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "app_users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(name = "created_by" , nullable = false)
    var createdBy: Long,

    @Column(name = "updated_by" , nullable = false)
    var updatedBy: Long,
): AuditableEntity()