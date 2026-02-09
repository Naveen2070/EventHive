package com.sam_the_dev.eventhive.infrastructure.persistence.event

import com.sam_the_dev.eventhive.infrastructure.persistence.base.AuditableEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "ticket_tiers", uniqueConstraints = [
        UniqueConstraint(columnNames = ["event_id", "name"])
    ]
)
class TicketTierEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(name = "price", precision = 10, scale = 2)
    var price: BigDecimal,

    @Column(nullable = false)
    var totalAllocation: Int,

    @Column(nullable = false)
    var availableAllocation: Int,

    @Column(nullable = false)
    var validFrom: LocalDateTime,

    @Column(nullable = false)
    var validUntil: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    val event: EventEntity,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @Column(name = "updated_by", nullable = false)
    var updatedBy: Long,
) : AuditableEntity()