package com.opendatamask.domain.port.output

import com.opendatamask.model.User
import java.util.Optional

interface UserPort {
    fun findById(id: Long): Optional<User>
    fun findByUsername(username: String): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun save(user: User): User
}
