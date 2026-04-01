import { describe, it, expect } from 'vitest'
import { ConnectionType, GeneratorType } from '../../types/index'

describe('ConnectionType enum', () => {
  it('includes all expected connection types', () => {
    expect(ConnectionType.POSTGRESQL).toBe('POSTGRESQL')
    expect(ConnectionType.MONGODB).toBe('MONGODB')
    expect(ConnectionType.AZURE_SQL).toBe('AZURE_SQL')
    expect(ConnectionType.MONGODB_COSMOS).toBe('MONGODB_COSMOS')
    expect(ConnectionType.FILE).toBe('FILE')
  })

  it('has exactly 5 connection types', () => {
    expect(Object.keys(ConnectionType).length).toBe(5)
  })
})

describe('GeneratorType enum', () => {
  it('includes all 11 generator types', () => {
    expect(Object.keys(GeneratorType).length).toBe(11)
    expect(GeneratorType.NAME).toBe('NAME')
    expect(GeneratorType.EMAIL).toBe('EMAIL')
    expect(GeneratorType.NULL).toBe('NULL')
  })
})
