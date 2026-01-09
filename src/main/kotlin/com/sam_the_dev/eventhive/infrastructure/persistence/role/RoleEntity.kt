package com.sam_the_dev.eventhive.infrastructure.persistence.role

import com.sam_the_dev.eventhive.infrastructure.persistence.base.AuditableEntity
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