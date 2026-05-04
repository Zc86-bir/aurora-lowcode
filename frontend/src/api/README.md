// frontend/src/api/README.md
# API SDK — 自动生成

> **规则**: 禁止手写 Axios/fetch 请求后端。所有 API 调用必须使用 `@hey-api/openapi-ts` 生成的强类型 SDK。

## 生成方式

```bash
# 从本地 OpenAPI 文件生成
pnpm generate:api

# 从运行中的后端生成
pnpm generate:api:remote
```

## 使用示例

```typescript
import { getForms, createForm, updateForm, deleteForm } from '@/api/generated'
import { apiConfig } from '@/api/client'

// 查询所有表单
const { data, error } = await getForms({
  client: apiConfig,
  headers: {
    'X-Tenant-Id': getTenantId()!,
    'Authorization': `Bearer ${getAuthToken()!}`,
  },
})

// 创建表单
const { data: form } = await createForm({
  client: apiConfig,
  headers: {
    'X-Tenant-Id': getTenantId()!,
    'Authorization': `Bearer ${getAuthToken()!}`,
  },
  body: {
    entityName: 'Customer',
    tableName: 'customer',
    fields: [
      { name: 'fullName', type: 'string', required: true },
      { name: 'email', type: 'string', required: true },
    ],
  },
})

// 更新表单
await updateForm({
  client: apiConfig,
  path: { formName: 'customer_form' },
  body: { /* ... */ },
})

// 删除表单
await deleteForm({
  client: apiConfig,
  path: { formName: 'customer_form' },
})
```

## 生成的文件结构

```
src/api/generated/
├── client.ts           # fetch 客户端配置
├── types.gen.ts        # OpenAPI 类型定义
├── schemas.gen.ts      # JSON Schema 类型
├── services.gen.ts     # 所有 API 服务方法
└── index.ts            # 统一导出
```

## 禁止行为

❌ **禁止** 手写 fetch/Axios 请求后端 API
❌ **禁止** 手动定义 API 响应类型
❌ **禁止** 硬编码 API URL

## 正确做法

✅ **必须** 使用 `pnpm generate:api` 生成 SDK
✅ **必须** 从 `@/api/generated` 导入类型和方法
✅ **必须** 使用 `apiConfig` 配置客户端
