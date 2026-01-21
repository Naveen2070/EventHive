package com.sam_the_dev.eventhive.infrastructure.persistence.event

import com.sam_the_dev.eventhive.api.dto.EventSearchCriteria
import com.sam_the_dev.eventhive.domain.event.EventStatus
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object EventSpecification {

    fun withCriteria(criteria: EventSearchCriteria): Specification<EventEntity> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            // 1. Title (Partial Match, Case Insensitive) -> LIKE %title%
            criteria.title?.let {
                predicates.add(cb.like(cb.lower(root.get("title")), "%${it.lowercase()}%"))
            }

            // 2. Location (Partial Match)
            criteria.location?.let {
                predicates.add(cb.like(cb.lower(root.get("location")), "%${it.lowercase()}%"))
            }

            // 3. Price Range
            criteria.minPrice?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), it))
            }
            criteria.maxPrice?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), it))
            }

            // 4. Date Range
            criteria.startDate?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), it))
            }
            criteria.endDate?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), it))
            }

            // 5. Status (Exact Match)
            criteria.status?.let {
                try {
                    val statusEnum = EventStatus.valueOf(it.uppercase())
                    predicates.add(cb.equal(root.get<EventStatus>("status"), statusEnum))
                } catch (_: IllegalArgumentException) {
                    predicates.add(cb.equal(root.get<EventStatus>("status"), EventStatus.PUBLISHED.name))
                }
            }

            // Combine all with AND
            cb.and(*predicates.toTypedArray())
        }
    }
}