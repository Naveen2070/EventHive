package com.thehiveproject.event.infrastructure.persistence.role

import com.thehiveproject.event.infrastructure.persistence.base.AuditableEntity
import jakarta.persistence.*

@Entity
@Table(name = "roles")
class RoleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String,

    @Column(name = "created_by" , nullable = false)
    val createdBy: Long,

    @Column(name = "updated_by" , nullable = false)
    var updatedBy: Long,
) : AuditableEntity()