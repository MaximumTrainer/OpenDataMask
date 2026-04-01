import { describe, it, expect } from 'vitest'
import { ActionType } from '../../types/index'

describe('ActionType enum', () => {
  it('includes all action types', () => {
    expect(ActionType.WEBHOOK).toBe('WEBHOOK')
    expect(ActionType.EMAIL).toBe('EMAIL')
    expect(ActionType.SCRIPT).toBe('SCRIPT')
  })

  it('has exactly 3 action types', () => {
    expect(Object.keys(ActionType).length).toBe(3)
  })
})
