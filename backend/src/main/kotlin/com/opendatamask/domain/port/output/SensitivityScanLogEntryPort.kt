package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.SensitivityScanLogEntry

interface SensitivityScanLogEntryPort {
    fun findByScanLogId(scanLogId: Long): List<SensitivityScanLogEntry>
    fun save(entry: SensitivityScanLogEntry): SensitivityScanLogEntry
    fun saveAll(entries: List<SensitivityScanLogEntry>): List<SensitivityScanLogEntry>
}
