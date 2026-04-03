package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.ConnectionType

interface ConnectorFactoryPort {
    fun createConnector(
        type: ConnectionType,
        connectionString: String,
        username: String? = null,
        password: String? = null,
        database: String? = null
    ): DatabaseConnector
}
