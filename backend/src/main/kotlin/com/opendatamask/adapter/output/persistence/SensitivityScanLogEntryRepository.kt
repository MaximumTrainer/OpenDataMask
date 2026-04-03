package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.SensitivityScanLogEntry
import com.opendatamask.domain.port.output.SensitivityScanLogEntryPort
import org.springframework.data.jpa.repository.JpaRepository

interface SensitivityScanLogEntryRepository : JpaRepository<SensitivityScanLogEntry, Long>, SensitivityScanLogEntryPort {
    override fun findByScanLogId(scanLogId: Long): List<SensitivityScanLogEntry>
    override fun save(entry: SensitivityScanLogEntry): SensitivityScanLogEntry
}
