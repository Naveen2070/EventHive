package com.thehiveproject.event.infrastructure.persistence.event

import com.thehiveproject.event.domain.event.EventStatus
import com.thehiveproject.event.infrastructure.persistence.base.AuditableEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "events")
class EventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDateTime,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDateTime,

    @Column(nullable = false)
    var location: String,

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    var ticketTiers: MutableList<TicketTierEntity> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus,

    @Column(name = "organizer_id", nullable = false)
    var organizerId: Long,

    @Column(name = "created_by" , nullable = false)
    val createdBy: Long,

    @Column(name = "updated_by" , nullable = false)
    var updatedBy: Long,
) : AuditableEntity()