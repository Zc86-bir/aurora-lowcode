import { describe, expect, it, expectTypeOf } from 'vitest'
import {
  listDepartments,
  listDictionaryItems,
  listNotices,
  listTenants,
  type SysDepartment,
  type SysDictionaryItem,
  type SysNotice,
  type SysTenant,
} from '@/api/admin-contract'

describe('admin contract additions', () => {
  it('exports list functions for remaining P1 system objects', () => {
    expectTypeOf(listDepartments).toBeFunction()
    expectTypeOf(listDictionaryItems).toBeFunction()
    expectTypeOf(listNotices).toBeFunction()
    expectTypeOf(listTenants).toBeFunction()
  })

  it('defines typed entities for remaining P1 system objects', () => {
    expectTypeOf<SysDepartment['id']>().toEqualTypeOf<string>()
    expectTypeOf<SysDictionaryItem['id']>().toEqualTypeOf<string>()
    expectTypeOf<SysNotice['id']>().toEqualTypeOf<string>()
    expectTypeOf<SysTenant['id']>().toEqualTypeOf<string>()
    expect(true).toBe(true)
  })
})
