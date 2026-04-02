package com.opendatamask.domain.port.output

interface TokenPort {
    fun generateToken(username: String): String
    fun getUsernameFromToken(token: String): String
    fun validateToken(token: String): Boolean
}
