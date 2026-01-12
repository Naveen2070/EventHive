package com.sam_the_dev.eventhive.infrastructure.persistence.booking

import com.sam_the_dev.eventhive.domain.booking.BookingStatus
import com.sam_the_dev.eventhive.infrastructure.persistence.base.AuditableEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.event.EventEntity
import com.sam_the_dev.eventhive.infrastructure.persistence.user.UserEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "bookings")
class BookingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "booking_reference", nullable = false, unique = true)
    var bookingReference: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: EventEntity,

    @Column(name = "tickets_count", nullable = false)
    var ticketsCount: Int,

    @Column(name = "total_price", nullable = false)
    var totalPrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BookingStatus,

    @Column(name = "created_by" , nullable = false)
    val createdBy: Long,

    @Column(name = "updated_by" , nullable = false)
    var updatedBy: Long,

) : AuditableEntity()