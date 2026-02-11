package com.thehiveproject.event.infrastructure.persistence.user

import com.thehiveproject.event.infrastructure.persistence.base.AuditableEntity
import com.thehiveproject.event.infrastructure.persistence.role.UserRoleEntity
import jakarta.persistence.*

@Entity
@Table(name = "app_users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? =null,

    @Column(nullable = false, unique = true)
    var username: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(name = "created_by" , nullable = false)
    val createdBy: Long,

    @Column(name = "updated_by" , nullable = false)
    var updatedBy: Long,

    @OneToMany(
        mappedBy = "user",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    var userRoles: MutableSet<UserRoleEntity> = mutableSetOf()
): AuditableEntity()