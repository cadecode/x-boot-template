# Vue 3 代码风格指南

> 目录结构、文件命名、组件编写、状态管理、测试规范的完整参考模板。

---

## 目录结构

```text
my-vue3-project/
├── src/
│   ├── assets/                      # 静态资源（图片、字体等）
│   ├── components/
│   │   ├── common/                  # 通用UI组件（跨项目复用，无业务逻辑）
│   │   │   └── BaseButton.vue
│   │   └── biz/                     # 通用业务组件（跨页面复用，含业务逻辑）
│   │       └── BizUserCard.vue
│   ├── composables/                 # 组合式函数（可复用的状态逻辑）
│   │   └── useUser.ts
│   ├── layouts/                     # 布局组件（页面整体结构）
│   │   ├── default.vue
│   │   └── components/              # 布局专用组件（仅布局层使用）
│   │       └── AppHeader.vue
│   ├── pages/                       # 页面组件（路由对应，kebab-case命名）
│   │   ├── user-profile.vue
│   │   └── user/                    # 页面模块
│   │       ├── list.vue
│   │       └── components/          # 页面专用组件（仅当前模块使用）
│   │           └── UserFilter.vue
│   ├── router/                      # 路由配置（管理页面路由）
│   │   ├── index.ts
│   │   └── user-routes.ts
│   ├── stores/                      # Pinia状态管理（全局共享状态）
│   │   └── userStore.ts
│   ├── services/                    # API服务层（封装后端接口调用）
│   │   └── userService.ts
│   ├── utils/                       # 工具函数（纯函数，无副作用）
│   │   ├── formatDate.ts
│   │   └── validateEmail.ts
│   ├── types/                       # TypeScript类型定义（接口、枚举等）
│   │   ├── User.ts
│   │   └── api.ts
│   ├── styles/                      # 全局样式（变量、混入、重置样式）
│   │   └── global.scss
│   ├── App.vue
│   └── main.ts
├── tests/
│   └── unit/                        # 单元测试（与src目录结构对应）
│       ├── components/
│       └── utils/
└── package.json
```

---

## 文件命名规则

| 类型         | 规则               | 示例                | 说明               |
| ------------ | ------------------ | ------------------- | ------------------ |
| 通用UI组件   | `Base` + PascalCase | `BaseButton.vue`    | 无业务逻辑，纯UI   |
| 通用业务组件 | `Biz` + PascalCase  | `BizUserCard.vue`   | 跨页面复用，含业务 |
| 布局组件     | `App` + PascalCase  | `AppHeader.vue`     | 仅布局层使用       |
| 页面文件     | kebab-case          | `user-profile.vue`  | 与路由路径一致     |
| 页面专用组件 | PascalCase          | `UserFilter.vue`    | 仅当前模块使用     |
| 组合式函数   | `use` + camelCase   | `useUser.ts`        | 逻辑复用           |
| Store        | camelCase + `Store` | `userStore.ts`      | 全局状态           |
| Service      | camelCase + `Service`| `userService.ts`   | API封装            |
| 工具函数     | camelCase           | `formatDate.ts`     | 纯函数             |
| 类型定义     | PascalCase          | `User.ts`           | 接口/枚举          |
| 测试文件     | 源文件名 + `.spec.ts`| `BaseButton.spec.ts`| 单元测试           |

---

## 组件编写

### 组件分类与命名

| 组件类型     | 目录                     | 命名规则           | 示例               |
| ------------ | ------------------------ | ------------------ | ------------------ |
| 通用UI组件   | `components/common/`     | `Base` + PascalCase | `BaseButton.vue`   |
| 通用业务组件 | `components/biz/`        | `Biz` + PascalCase  | `BizUserCard.vue`  |
| 布局组件     | `layouts/components/`    | `App` + PascalCase  | `AppHeader.vue`    |
| 页面专用组件 | `pages/xxx/components/`  | PascalCase          | `UserFilter.vue`   |

### 单文件组件结构

```html
<script setup lang="ts">
// 1. 类型导入
import type { UserInfo } from '@/types/User'

// 2. 第三方库
import { ElMessage } from 'element-plus'

// 3. 组件导入
import BaseButton from '@/components/common/BaseButton.vue'

// 4. 工具/组合式函数
import { useUser } from '@/composables/useUser'

// 5. Props
interface Props {
  userId: string
  title?: string
}
const props = withDefaults(defineProps<Props>(), {
  title: '默认'
})

// 6. Emits
const emit = defineEmits<{
  (e: 'update', user: UserInfo): void
}>()

// 7. 响应式数据
const loading = ref(false)

// 8. 计算属性
const displayName = computed(() => user.value?.name)

// 9. 方法
async function fetchUser() {}

// 10. 生命周期
onMounted(() => {})

// 11. 暴露
defineExpose({ refresh: fetchUser })
</script>

<template>
  <!-- 自定义组件：PascalCase；第三方：kebab-case -->
  <BaseButton @click="handleClick" />
  <el-button type="primary" />
</template>

<style scoped lang="scss">
// BEM命名
.user-card {
  &__header {}
  &--active {}
}
</style>
```

### 模板使用规则

| 组件类型   | 模板中写法 | 示例              |
| ---------- | ---------- | ----------------- |
| 自定义组件 | PascalCase | `<BaseButton />`  |
| 第三方UI库 | kebab-case | `<el-button />`   |
| 原生HTML   | 小写       | `<div />`         |

### 组合式函数模板

```typescript
// composables/useCounter.ts
export function useCounter(initialValue = 0) {
  const count = ref(initialValue)
  const doubled = computed(() => count.value * 2)

  function increment() { count.value++ }
  function reset() { count.value = initialValue }

  return { count, doubled, increment, reset }
}
```

> 要点：以 `use` 开头，返回响应式状态和方法

---

## 状态管理与 API 服务

### Store 规范（Pinia）

**文件树示例**

```text
stores/
├── userStore.ts
├── cartStore.ts
└── productStore.ts
```

**模板**

```typescript
// stores/userStore.ts
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    name: '',
    isLoggedIn: false
  }),

  getters: {
    displayName: (state) => state.name || '游客'
  },

  actions: {
    async login(email: string, password: string) {
      // 登录逻辑
    },
    logout() {
      this.$reset()
    }
  }
})
```

**组件中使用**

```html
<script setup>
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()
// 状态用 storeToRefs 解构保持响应性
const { name, displayName } = storeToRefs(userStore)
// actions 直接解构
const { login, logout } = userStore
</script>
```

> 要点：状态使用 `storeToRefs` 解构，actions 可直接解构

---

### Service 规范

**文件树示例**

```text
services/
├── userService.ts
├── productService.ts
└── api/
    ├── client.ts       # axios实例配置
    └── interceptors.ts # 拦截器
```

**模板**

```typescript
// services/userService.ts
import request from './api/client'
import type { UserInfo, LoginParams } from '@/types/User'

export const userService = {
  async getUser(id: string): Promise<UserInfo> {
    const { data } = await request.get(`/api/users/${id}`)
    return data
  },

  async login(params: LoginParams): Promise<{ token: string }> {
    const { data } = await request.post('/api/users/login', params)
    return data
  }
}
```

> 要点：只负责 API 调用，不包含 UI 逻辑，明确返回类型

---

## 工具函数与类型定义

### 工具函数规范

**文件树示例**

```text
utils/
├── formatDate.ts       # 日期格式化
├── formatCurrency.ts   # 货币格式化
├── validateEmail.ts    # 邮箱验证
└── storage.ts          # 本地存储封装
```

**模板**

```typescript
// utils/formatDate.ts
export function formatDate(date: Date, format = 'YYYY-MM-DD'): string {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
}

// utils/validateEmail.ts
export function validateEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@([^\s@.,]+\.)+[^\s@.,]{2,}$/
  return emailRegex.test(email)
}
```

> 要点：纯函数无副作用，一个文件一个功能，避免创建 `utils.ts` 万能文件

---

### 类型定义规范

**文件树示例**

```text
types/
├── User.ts             # 用户相关类型
├── Product.ts          # 商品相关类型
├── api.ts              # API通用类型
└── global.d.ts         # 全局类型声明
```

**模板**

```typescript
// types/User.ts
export interface UserInfo {
  id: string
  name: string
  email: string
  role: UserRole
}

export enum UserRole {
  Admin = 'admin',
  User = 'user'
}

// types/api.ts
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}
```

**使用方式**

```typescript
// ✅ 正确：使用 type 关键字导入纯类型
import type { UserInfo } from '@/types/User'

// ✅ 正确：枚举需要值导入
import { UserRole } from '@/types/User'
```

> 要点：使用 `type` 关键字导入纯类型，枚举需要值导入

---

## 测试规范

### 文件树示例

```text
tests/unit/
├── components/
│   ├── common/
│   │   └── BaseButton.spec.ts
│   └── biz/
│       └── BizUserCard.spec.ts
├── composables/
│   └── useCounter.spec.ts
├── stores/
│   └── userStore.spec.ts
├── services/
│   └── userService.spec.ts
└── utils/
    └── formatDate.spec.ts
```

### 组件测试模板

```typescript
// tests/unit/components/common/BaseButton.spec.ts
import { mount } from '@vue/test-utils'
import BaseButton from '@/components/common/BaseButton.vue'

describe('BaseButton', () => {
  it('renders correctly', () => {
    const wrapper = mount(BaseButton, {
      slots: { default: 'Click' }
    })
    expect(wrapper.text()).toBe('Click')
  })

  it('emits click event when clicked', async () => {
    const wrapper = mount(BaseButton)
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
```

### 组合式函数测试模板

```typescript
// tests/unit/composables/useCounter.spec.ts
import { useCounter } from '@/composables/useCounter'

describe('useCounter', () => {
  it('initializes with default value', () => {
    const { count } = useCounter()
    expect(count.value).toBe(0)
  })

  it('increments count correctly', () => {
    const { count, increment } = useCounter(5)
    increment()
    expect(count.value).toBe(6)
  })
})
```

### 工具函数测试模板

```typescript
// tests/unit/utils/formatDate.spec.ts
import { formatDate } from '@/utils/formatDate'

describe('formatDate', () => {
  it('formats date with default format', () => {
    const date = new Date(2024, 0, 15)
    expect(formatDate(date)).toBe('2024-01-15')
  })

  it('formats date with custom format', () => {
    const date = new Date(2024, 0, 15)
    expect(formatDate(date, 'YYYY/MM/DD')).toBe('2024/01/15')
  })
})
```

> 要点：测试文件与源文件同名 + `.spec.ts`，目录结构与 `src/` 对应

---

## 组件库封装范式

### 基础版

```html
<script lang="ts" setup>
import type { InputInstance, InputProps } from 'element-plus'
import { getCurrentInstance } from 'vue'
import type { ExtractPropTypes } from 'vue'

// 1. 定义 Props 类型（继承 Element Plus Input 的所有属性）
export interface CustomInputProps extends ExtractPropTypes<InputProps> {
  title?: string  // 自定义标题
}

// 2. 定义实例类型（继承原始实例 + 自定义方法）
export interface CustomInputInstance extends InputInstance {
  someClick: () => void
}

defineOptions({
  inheritAttrs: false  // 手动控制属性透传
})

// 3. Props 默认值
const props = withDefaults(defineProps<CustomInputProps>(), {
  title: '自定义封装的Input',
  clearable: true  // 覆盖默认值为 true
})

// 4. 事件定义
const emit = defineEmits<{
  (e: 'titleClick'): void
}>()

const vm = getCurrentInstance()

// 5. ref 回调：合并原始实例和自定义方法
function changeRef(inputInstance: Record<string, any> | null) {
  if (vm) {
    vm.exposeProxy = vm.exposed = Object.assign(inputInstance || {}, {
      someClick
    }) as CustomInputInstance
  }
}

function someClick() {
  console.log('someClick')
}

function handleTitleClick() {
  emit('titleClick')
}

// 6. 暴露合并后的实例
defineExpose((vm?.exposeProxy || {}) as CustomInputInstance)
</script>

<template>
  <div class="custom-input">
    <div @click="handleTitleClick">{{ title }}</div>
    <!-- 透传所有属性和插槽 -->
    <el-input :ref="changeRef" v-bind="{ ...$attrs, ...props }">
      <template v-for="(_, name) in $slots" :key="name" #[name]="slotProps">
        <slot :name="name" v-bind="slotProps" />
      </template>
    </el-input>
  </div>
</template>
```

---

### h 函数版

```html
<script lang="ts" setup>
import { getCurrentInstance, h } from 'vue'
import { ElInput } from 'element-plus'
import type { InputInstance, InputProps } from 'element-plus'
import type { ExtractPropTypes } from 'vue'

export interface CustomInputProps extends ExtractPropTypes<InputProps> {
  title?: string
}

export interface CustomInputInstance extends InputInstance {
  someClick: () => void
}

defineOptions({
  inheritAttrs: false
})

const props = withDefaults(defineProps<CustomInputProps>(), {
  title: '自定义封装的Input',
  clearable: true
})

const emit = defineEmits<{
  (e: 'titleClick'): void
}>()

const vm = getCurrentInstance()

function changeRef(inputInstance: Record<string, any> | null) {
  if (vm) {
    vm.exposeProxy = vm.exposed = Object.assign(inputInstance || {}, {
      someClick
    })
  }
}

function someClick() {
  console.log('someClick')
}

function handleTitleClick() {
  emit('titleClick')
}

defineExpose((vm?.exposeProxy || {}) as CustomInputInstance)
</script>

<template>
  <div class="custom-input">
    <div @click="handleTitleClick">{{ title }}</div>
    <!-- h 函数渲染，自动处理插槽透传 -->
    <component :is="h(ElInput, { ...$attrs, ...props, ref: changeRef }, $slots)" />
  </div>
</template>
```
