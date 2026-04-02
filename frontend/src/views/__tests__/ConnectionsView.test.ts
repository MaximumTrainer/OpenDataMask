import { describe, it, expect } from 'vitest'
import { ConnectionType, GeneratorType } from '../../types/index'

describe('ConnectionType enum', () => {
  it('includes all expected connection types', () => {
    expect(ConnectionType.POSTGRESQL).toBe('POSTGRESQL')
    expect(ConnectionType.MONGODB).toBe('MONGODB')
    expect(ConnectionType.AZURE_SQL).toBe('AZURE_SQL')
    expect(ConnectionType.MONGODB_COSMOS).toBe('MONGODB_COSMOS')
    expect(ConnectionType.FILE).toBe('FILE')
    expect(ConnectionType.MYSQL).toBe('MYSQL')
  })

  it('has exactly 6 connection types', () => {
    expect(Object.keys(ConnectionType).length).toBe(6)
  })
})

describe('GeneratorType enum', () => {
  it('includes all 47 generator types', () => {
    expect(Object.keys(GeneratorType).length).toBe(47)
    expect(GeneratorType.NAME).toBe('NAME')
    expect(GeneratorType.EMAIL).toBe('EMAIL')
    expect(GeneratorType.NULL).toBe('NULL')
    // Name variants
    expect(GeneratorType.FIRST_NAME).toBe('FIRST_NAME')
    expect(GeneratorType.LAST_NAME).toBe('LAST_NAME')
    // Financial
    expect(GeneratorType.IBAN).toBe('IBAN')
    expect(GeneratorType.BTC_ADDRESS).toBe('BTC_ADDRESS')
    // Medical
    expect(GeneratorType.ICD_CODE).toBe('ICD_CODE')
    // Composite
    expect(GeneratorType.PARTIAL_MASK).toBe('PARTIAL_MASK')
    expect(GeneratorType.FORMAT_PRESERVING).toBe('FORMAT_PRESERVING')
  })
})
