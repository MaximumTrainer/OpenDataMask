package com.opendatamask.domain.port.output

interface EncryptionPort {
    fun encrypt(plain: String): String
    fun decrypt(encrypted: String): String
}
