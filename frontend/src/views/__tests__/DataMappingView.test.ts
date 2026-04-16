import { describe, it, expect } from 'vitest'
import { MappingAction, MaskingStrategy, GeneratorType } from '../../types/index'

describe('MappingAction enum', () => {
  it('has MIGRATE_AS_IS and MASK values', () => {
    expect(MappingAction.MIGRATE_AS_IS).toBe('MIGRATE_AS_IS')
    expect(MappingAction.MASK).toBe('MASK')
  })

  it('has exactly 2 values', () => {
    expect(Object.keys(MappingAction).length).toBe(2)
  })
})

describe('MaskingStrategy enum', () => {
  it('has FAKE, HASH and NULL values', () => {
    expect(MaskingStrategy.FAKE).toBe('FAKE')
    expect(MaskingStrategy.HASH).toBe('HASH')
    expect(MaskingStrategy.NULL).toBe('NULL')
  })

  it('has exactly 3 values', () => {
    expect(Object.keys(MaskingStrategy).length).toBe(3)
  })
})

describe('Custom data mapping types integration', () => {
  it('MIGRATE_AS_IS mapping has no masking strategy', () => {
    const mapping = {
      id: 1,
      workspaceId: 1,
      connectionId: 2,
      tableName: 'users',
      columnName: 'id',
      action: MappingAction.MIGRATE_AS_IS,
      maskingStrategy: null,
      fakeGeneratorType: null,
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00'
    }
    expect(mapping.action).toBe('MIGRATE_AS_IS')
    expect(mapping.maskingStrategy).toBeNull()
    expect(mapping.fakeGeneratorType).toBeNull()
  })

  it('MASK mapping with FAKE strategy has a generator type', () => {
    const mapping = {
      id: 2,
      workspaceId: 1,
      connectionId: 2,
      tableName: 'users',
      columnName: 'email',
      action: MappingAction.MASK,
      maskingStrategy: MaskingStrategy.FAKE,
      fakeGeneratorType: GeneratorType.EMAIL,
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00'
    }
    expect(mapping.action).toBe('MASK')
    expect(mapping.maskingStrategy).toBe('FAKE')
    expect(mapping.fakeGeneratorType).toBe('EMAIL')
  })

  it('MASK mapping with HASH strategy has no generator type', () => {
    const mapping = {
      id: 3,
      workspaceId: 1,
      connectionId: 2,
      tableName: 'users',
      columnName: 'user_ref',
      action: MappingAction.MASK,
      maskingStrategy: MaskingStrategy.HASH,
      fakeGeneratorType: null,
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00'
    }
    expect(mapping.maskingStrategy).toBe('HASH')
    expect(mapping.fakeGeneratorType).toBeNull()
  })

  it('MASK mapping with NULL strategy nullifies the value', () => {
    const mapping = {
      id: 4,
      workspaceId: 1,
      connectionId: 2,
      tableName: 'users',
      columnName: 'ssn',
      action: MappingAction.MASK,
      maskingStrategy: MaskingStrategy.NULL,
      fakeGeneratorType: null,
      createdAt: '2024-01-01T00:00:00',
      updatedAt: '2024-01-01T00:00:00'
    }
    expect(mapping.maskingStrategy).toBe('NULL')
    expect(mapping.fakeGeneratorType).toBeNull()
  })

  it('BulkCustomDataMappingRequest can contain mixed actions', () => {
    const request = {
      connectionId: 2,
      tableName: 'users',
      columnMappings: [
        { columnName: 'id', action: MappingAction.MIGRATE_AS_IS },
        { columnName: 'email', action: MappingAction.MASK, maskingStrategy: MaskingStrategy.FAKE, fakeGeneratorType: GeneratorType.EMAIL },
        { columnName: 'ssn', action: MappingAction.MASK, maskingStrategy: MaskingStrategy.NULL }
      ]
    }
    expect(request.columnMappings).toHaveLength(3)
    expect(request.columnMappings[0].action).toBe('MIGRATE_AS_IS')
    expect(request.columnMappings[1].maskingStrategy).toBe('FAKE')
    expect(request.columnMappings[2].maskingStrategy).toBe('NULL')
  })

  it('ConnectionSchemaResponse contains tables with columns', () => {
    const schema = {
      connectionId: 2,
      tables: [
        {
          tableName: 'users',
          columns: [
            { name: 'id', type: 'bigint', nullable: false },
            { name: 'email', type: 'varchar', nullable: true }
          ]
        }
      ]
    }
    expect(schema.tables).toHaveLength(1)
    expect(schema.tables[0].tableName).toBe('users')
    expect(schema.tables[0].columns).toHaveLength(2)
    expect(schema.tables[0].columns[1].nullable).toBe(true)
  })
})
