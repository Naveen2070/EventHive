package com.sam_the_dev.eventhive.infrastructure.persistence.event

import com.sam_the_dev.eventhive.domain.event.EventStatus
import com.sam_the_dev.eventhive.infrastructure.persistence.base.AuditableEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    var organizer: UserEntity,

    @Column(name = "created_by" , nullable = false)
    val createdBy: Long,

    @Column(name = "updated_by" , nullable = false)
    var updatedBy: Long,
) : AuditableEntity()