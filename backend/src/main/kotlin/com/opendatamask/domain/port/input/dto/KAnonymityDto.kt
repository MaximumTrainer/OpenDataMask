package com.opendatamask.domain.port.input.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KAnonymityReport(
    val workspaceId: Long,
    @get:JsonProperty("kValue")
    val kValue: Int,
    val atRisk: Boolean,
    val quasiIdentifiers: List<String>,
    val recommendation: String
)
