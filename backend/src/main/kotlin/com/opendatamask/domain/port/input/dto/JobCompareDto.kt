package com.opendatamask.domain.port.input.dto

data class JobCompareResponse(
    val jobAId: Long,
    val jobBId: Long,
    val rowsDelta: Long,
    val tablesAddedInB: List<String>,
    val tablesRemovedInB: List<String>,
    val tablesInCommon: List<String>
)
