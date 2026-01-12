package com.sam_the_dev.eventhive.infrastructure.persistence.base

import jakarta.persistence.*
import java.time.Instant

@MappedSuperclass
abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant
        protected set

    @Column(name = "deleted_by")
    var deletedBy: Long ?= null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
        protected set

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
        protected set

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    @PrePersist
    fun prePersist() {
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }

    fun markDeleted(deletedBy: Long) {
        isDeleted = true
        isActive = false
        this.deletedBy = deletedBy
        deletedAt = Instant.now()
    }
}