package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.User
import com.opendatamask.domain.port.output.UserPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long>, UserPort {
    override fun findById(id: Long): Optional<User>
    override fun findByUsername(username: String): Optional<User>
    override fun findByEmail(email: String): Optional<User>
    override fun existsByUsername(username: String): Boolean
    override fun existsByEmail(email: String): Boolean
    override fun save(user: User): User
}
