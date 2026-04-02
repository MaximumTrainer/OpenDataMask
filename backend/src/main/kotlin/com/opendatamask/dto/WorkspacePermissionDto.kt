package com.opendatamask.dto

data class UpdatePermissionsRequest(
    val grants: List<String> = emptyList(),
    val revocations: List<String> = emptyList()
)
