package com.thehiveproject.event.api.dto

import org.springframework.data.domain.Page

data class PaginatedResponse<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)

fun <T> Page<T>.toPaginatedResponse(): PaginatedResponse<T> =
    PaginatedResponse(
        content = this.content,
        pageNumber = this.number,
        pageSize = this.size,
        totalElements = this.totalElements,
        totalPages = this.totalPages,
        isLast = this.isLast
    )
