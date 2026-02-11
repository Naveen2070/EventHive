package com.thehiveproject.event.infrastructure.persistence.role

import com.thehiveproject.event.infrastructure.persistence.base.AuditableEntity
import com.thehiveproject.event.infrastructure.persistence.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "user_roles")
class UserRoleEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    var role: RoleEntity,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "updated_by", nullable = false)
    var updatedBy: Long,
) : AuditableEntity()