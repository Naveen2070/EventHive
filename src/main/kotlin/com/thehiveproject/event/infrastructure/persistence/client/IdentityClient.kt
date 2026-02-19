package com.thehiveproject.event.infrastructure.persistence.client

import com.thehiveproject.event.api.dto.UserSummaryDTO
import com.thehiveproject.event.configuration.FeignConfig
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(
    name = "identity-service",
    url = "\${identity-service.url}",
    configuration = [FeignConfig::class]
)
interface IdentityClient {
    /**
     * Retrieves a summary of a single user by their ID.
     *
     * Sends a POST request to the Identity Service endpoint `/api/internal/users/{id}`
     * with the user ID in the path variable and returns the corresponding user details.
     *
     * @param id the ID of the user to fetch
     * @return a [UserSummaryDTO] object corresponding to the requested user
     */
    @GetMapping("/api/internal/users/{id}")
    fun getUsersById(@PathVariable id: Long): UserSummaryDTO

    /**
     * Retrieves a list of user summaries for the given user IDs.
     *
     * Sends a POST request to the Identity Service endpoint `/api/internal/users/batch`
     * with a list of user IDs in the request body and returns the corresponding user details.
     *
     * @param ids a list of user IDs to fetch
     * @return a list of [UserSummaryDTO] objects corresponding to the requested IDs
     */
    @PostMapping("/api/internal/users/batch")
    fun getUsersByIds(@RequestBody ids: List<Long>): List<UserSummaryDTO>
}
