import { describe, it, expect } from 'vitest'
import { JobStatus, TableMode, LogLevel } from '../../types/index'
import type { JobTableStats } from '../../types/index'

describe('JobStatus', () => {
  it('includes expected status values', () => {
    expect(JobStatus.PENDING).toBe('PENDING')
    expect(JobStatus.RUNNING).toBe('RUNNING')
    expect(JobStatus.COMPLETED).toBe('COMPLETED')
    expect(JobStatus.FAILED).toBe('FAILED')
    expect(JobStatus.CANCELLED).toBe('CANCELLED')
  })
})

describe('TableMode enum', () => {
  it('includes all table modes', () => {
    expect(TableMode.PASSTHROUGH).toBe('PASSTHROUGH')
    expect(TableMode.MASK).toBe('MASK')
    expect(TableMode.GENERATE).toBe('GENERATE')
    expect(TableMode.SUBSET).toBe('SUBSET')
    expect(TableMode.SKIP).toBe('SKIP')
  })
})

describe('LogLevel enum', () => {
  it('includes all log levels', () => {
    expect(LogLevel.DEBUG).toBe('DEBUG')
    expect(LogLevel.INFO).toBe('INFO')
    expect(LogLevel.WARN).toBe('WARN')
    expect(LogLevel.ERROR).toBe('ERROR')
  })
})

describe('JobTableStats interface', () => {
  it('can hold per-table stats values', () => {
    const stats: JobTableStats = {
      id: 1,
      jobId: 2,
      tableName: 'users',
      rowsRead: 500,
      rowsWritten: 490,
      rowsSkipped: 10,
      startedAt: '2024-01-01T00:00:00',
      completedAt: '2024-01-01T00:00:01',
      elapsedMs: 1000,
      rowsPerSecond: 490,
      errorMessage: undefined
    }
    expect(stats.tableName).toBe('users')
    expect(stats.rowsRead).toBe(500)
    expect(stats.elapsedMs).toBe(1000)
  })
})


describe('JobStatus', () => {
  it('includes expected status values', () => {
    expect(JobStatus.PENDING).toBe('PENDING')
    expect(JobStatus.RUNNING).toBe('RUNNING')
    expect(JobStatus.COMPLETED).toBe('COMPLETED')
    expect(JobStatus.FAILED).toBe('FAILED')
    expect(JobStatus.CANCELLED).toBe('CANCELLED')
  })
})

describe('TableMode enum', () => {
  it('includes all table modes', () => {
    expect(TableMode.PASSTHROUGH).toBe('PASSTHROUGH')
    expect(TableMode.MASK).toBe('MASK')
    expect(TableMode.GENERATE).toBe('GENERATE')
    expect(TableMode.SUBSET).toBe('SUBSET')
    expect(TableMode.SKIP).toBe('SKIP')
  })
})

describe('LogLevel enum', () => {
  it('includes all log levels', () => {
    expect(LogLevel.DEBUG).toBe('DEBUG')
    expect(LogLevel.INFO).toBe('INFO')
    expect(LogLevel.WARN).toBe('WARN')
    expect(LogLevel.ERROR).toBe('ERROR')
  })
})
