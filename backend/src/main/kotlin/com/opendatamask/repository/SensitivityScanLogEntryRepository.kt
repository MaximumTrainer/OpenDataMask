package com.opendatamask.repository

import com.opendatamask.domain.model.SensitivityScanLogEntry
import org.springframework.data.jpa.repository.JpaRepository

interface SensitivityScanLogEntryRepository : JpaRepository<SensitivityScanLogEntry, Long> {
    fun findByScanLogId(scanLogId: Long): List<SensitivityScanLogEntry>
}
