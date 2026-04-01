package com.opendatamask.dto

import com.opendatamask.model.ActionType

data class PostJobActionRequest(
    val actionType: ActionType,
    val config: String = "{}",
    val enabled: Boolean = true
)
