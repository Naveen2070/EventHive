package com.sam_the_dev.eventhive.api.controller

import com.sam_the_dev.eventhive.api.dto.RegisterUserDto
import com.sam_the_dev.eventhive.api.dto.UserDTO
import com.sam_the_dev.eventhive.domain.user.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class UserAuthController(
    private val userService: UserService
) {

    @PostMapping("/register")
    fun register(@RequestBody userDto: RegisterUserDto): ResponseEntity<UserDTO> {
        val createdUser = userService.registerUser(userDto)
        return ResponseEntity(createdUser, HttpStatus.CREATED)
    }

    @GetMapping("/users/{id}")
    fun getUser(@PathVariable id: Long): ResponseEntity<UserDTO> {
        val user = userService.getUserById(id)
        return ResponseEntity.ok(user)
    }
}