package com.sam_the_dev.eventhive.infrastructure.persistence.event

import com.sam_the_dev.eventhive.api.dto.EventSearchCriteria
import com.sam_the_dev.eventhive.domain.event.EventStatus
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object EventSpecification {

    fun withCriteria(criteria: EventSearchCriteria): Specification<EventEntity> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            // 1. Title (Partial Match, Case Insensitive)
            criteria.title?.let {
                if (it.isNotBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("title")), "%${it.lowercase()}%"))
                }
            }

            // 2. Location (Partial Match)
            criteria.location?.let {
                if (it.isNotBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("location")), "%${it.lowercase()}%"))
                }
            }

            // 3. Price Range (Requires JOIN with TicketTierEntity)
            if (criteria.minPrice != null || criteria.maxPrice != null) {
                // Join Event -> TicketTiers
                // Use JoinType.LEFT if you wanted events without tiers, but for price filtering INNER is safer
                val tiersJoin = root.join<EventEntity, TicketTierEntity>("ticketTiers", JoinType.INNER)

                criteria.minPrice?.let {
                    predicates.add(cb.greaterThanOrEqualTo(tiersJoin.get("price"), it))
                }

                criteria.maxPrice?.let {
                    predicates.add(cb.lessThanOrEqualTo(tiersJoin.get("price"), it))
                }

                // Important: Since one event has multiple tiers, a join can produce duplicate event rows.
                // We ensure the result list contains unique events.
                query?.distinct(true)
            }

            // 4. Date Range
            criteria.startDate?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), it))
            }
            criteria.endDate?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), it))
            }

            // 5. Status
            // Default to PUBLISHED if status is invalid or null (common for public search)
            val statusString = criteria.status ?: EventStatus.PUBLISHED.name
            try {
                val statusEnum = EventStatus.valueOf(statusString.uppercase())
                predicates.add(cb.equal(root.get<EventStatus>("status"), statusEnum))
            } catch (_: IllegalArgumentException) {
                // Fallback if user sends garbage status
                predicates.add(cb.equal(root.get<EventStatus>("status"), EventStatus.PUBLISHED))
            }

            // Combine all with AND
            cb.and(*predicates.toTypedArray())
        }
    }
}