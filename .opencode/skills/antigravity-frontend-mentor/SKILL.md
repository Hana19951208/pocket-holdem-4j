---
name: antigravity-frontend-mentor
description: 前端学习专项技能 - 使用"类比后端"方法解释 React/TypeScript/Antd，附中文注释和通俗易懂的示例
---

# 前端学习指南 (Antigravity Style)

## 使用场景
- 学习前端框架（React、TypeScript）
- 使用 Ant Design 组件库
- 理解前端概念（类比后端）
- 编写符合规范的前端代码

## 学习方法：类比后端

前端概念 | 后端类比 | 说明
---------|----------|------|
React 组件 | Controller + Service | 处理用户交互和业务逻辑
Props | 方法参数 | 组件接收外部数据
State | 数据库状态 | 组件内部的可变数据
useState | @State 字段 | 组件级别的状态管理
useEffect | @PostConstruct / @PreDestroy | 副作用生命周期
useContext | Spring 上下文 | 全局状态共享
useReducer | Command Pattern | 复杂状态逻辑
useCallback | 方法缓存 | 性能优化
useMemo | 计算属性缓存 | 缓存计算结果
Hook | AOP 切面 | 抽取横切关注点
Context | Bean 容器 | 依赖注入容器

## TypeScript 基础

### 1. 类型定义（类比 Java POJO）
```typescript
// TypeScript 接口（类比 Java POJO / DTO）
interface User {
  id: number;
  username: string;
  email: string;
  age?: number;          // 可选属性（类比 @Nullable）
  roles: string[];       // 数组类型
  status: 'active' | 'inactive';  // 联合类型（类比枚举）
}

// 实现接口
const user: User = {
  id: 1,
  username: 'zhangsan',
  email: 'zhangsan@example.com',
  roles: ['admin', 'user'],
  status: 'active',
};
```

### 2. 泛型（类比 Java 泛型）
```typescript
// 泛型接口（类比 Java 的 List<T>）
interface Response<T> {
  code: number;
  message: string;
  data: T;               // 泛型数据
  timestamp: number;
}

// 使用泛型
interface User {
  id: number;
  name: string;
}

const response: Response<User> = {
  code: 200,
  message: 'success',
  data: { id: 1, name: '张三' },
  timestamp: Date.now(),
};

// 泛型函数（类比 Java 的 <T> T findById(Long id)）
function getData<T>(url: string): Promise<Response<T>> {
  return fetch(url).then(res => res.json());
}

// 调用泛型函数
getData<User>('/api/user/1').then(res => {
  console.log(res.data.name);
});
```

### 3. 类型守卫（类比 Java instanceof）
```typescript
// 类型守卫函数
function isUser(obj: unknown): obj is User {
  return (obj as User).id !== undefined 
      && (obj as User).name !== undefined;
}

// 使用类型守卫
function processData(data: unknown) {
  if (isUser(data)) {
    // TypeScript 知道 data 是 User 类型
    console.log(data.name);
  }
}

// 可区分联合类型
type Shape = 
  | { kind: 'circle'; radius: number }
  | { kind: 'square'; size: number }
  | { kind: 'rectangle'; width: number; height: number };

// 使用 switch 进行类型收窄
function getArea(shape: Shape): number {
  switch (shape.kind) {
    case 'circle':
      return Math.PI * shape.radius * shape.radius;
    case 'square':
      return shape.size * shape.size;
    case 'rectangle':
      return shape.width * shape.height;
    default:
      // 确保所有情况都被处理（类比 Java 的 default 分支）
      const _exhaustive: never = shape;
      throw new Error('未知形状');
  }
}
```

## React 核心概念

### 1. 组件（类比后端 Controller）
```typescript
// React 组件（类比 Java @Controller）
import React, { useState } from 'react';

// 函数组件（推荐，类比 Spring 的 @RestController 方法）
interface UserCardProps {
  user: User;
  onEdit?: (user: User) => void;  // 可选回调（类比 @Nullable Consumer）
}

/**
 * 用户卡片组件
 * 
 * 类比说明：
 * - Props 类比 Controller 方法参数
 * - 返回值 JSX 类比 @ResponseBody
 * - onEdit 回调类比 @RequestCallback 或事件监听
 */
function UserCard({ user, onEdit }: UserCardProps) {
  // useState 类比 @Value 或成员变量，提供响应式能力
  const [isExpanded, setIsExpanded] = useState(false);
  
  // 处理函数（类比 @PostMapping 方法）
  const handleToggle = () => {
    setIsExpanded(!isExpanded);
  };
  
  const handleEdit = () => {
    // 调用回调（类比调用其他 Service）
    onEdit?.(user);
  };
  
  // 渲染（类比 return @ResponseBody）
  return (
    <div className="user-card">
      {/* 条件渲染（类比 Thymeleaf th:if） */}
      {isExpanded ? (
        <div className="user-detail">
          <h3>{user.name}</h3>
          <p>邮箱: {user.email}</p>
          <p>角色: {user.roles.join(', ')}</p>
          <button onClick={handleEdit}>编辑</button>
        </div>
      ) : (
        <div className="user-summary" onClick={handleToggle}>
          <h3>{user.name}</h3>
          <span>点击查看详情</span>
        </div>
      )}
    </div>
  );
}

export default UserCard;
```

### 2. useEffect（类比生命周期注解）
```typescript
import { useEffect, useState } from 'react';

/**
 * useEffect 详解（类比 Spring 生命周期）
 * 
 * 类比说明：
 * - useEffect(() => {}, []) 类比 @PostConstruct
 * - useEffect(() => {}, [deps]) 类比 @EventListener
 * - useEffect(() => () => {}, []) 类比 @PreDestroy
 */
function UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  
  /**
   * 组件挂载时执行（类比 @PostConstruct）
   * 空依赖数组 [] 表示只执行一次
   */
  useEffect(() => {
    console.log('组件已挂载，开始加载数据');
    fetchUsers();
    
    // 可选：返回清理函数（类比 @PreDestroy）
    return () => {
      console.log('组件即将卸载，清理资源');
    };
  }, []);  // 空依赖数组：仅挂载时执行
  
  /**
   * 依赖变化时执行（类比 @EventListener）
   * 依赖 users 变化时重新执行
   */
  useEffect(() => {
    if (users.length > 0) {
      console.log(`用户列表已更新，共 ${users.length} 条数据`);
    }
  }, [users]);  // 依赖 users：当 users 变化时执行
  
  /**
   * 无依赖数组：每次渲染都执行（谨慎使用）
   * 类比每次请求都执行的逻辑
   */
  useEffect(() => {
    console.log('组件已渲染');
    // 注意：这可能导致无限循环，谨慎使用！
  });
  
  const fetchUsers = async () => {
    try {
      const res = await fetch('/api/users');
      const data = await res.json();
      setUsers(data);
    } catch (error) {
      console.error('获取用户失败', error);
    } finally {
      setLoading(false);
    }
  };
  
  if (loading) {
    return <div>加载中...</div>;
  }
  
  return (
    <ul>
      {users.map(user => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}
```

### 3. useContext（类比 Spring 上下文）
```typescript
import React, { createContext, useContext, useState } from 'react';

/**
 * Context 模式（类比 Spring @ApplicationContext）
 * 用于跨组件层级共享数据，避免 props 逐层传递
 */

// 1. 创建 Context（类比定义 Bean）
interface UserContextType {
  user: User | null;
  login: (user: User) => void;
  logout: () => void;
}

const UserContext = createContext<UserContextType | null>(null);

// 2. Provider 组件（类比 @Configuration + @Bean）
interface UserProviderProps {
  children: React.ReactNode;
}

/**
 * 用户上下文提供者
 * 
 * 类比说明：
 * - value 类比 Spring 容器中的 Bean 实例
 * - Provider 包裹的子组件都可以访问上下文
 */
export function UserProvider({ children }: UserProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  
  const login = (newUser: User) => {
    setUser(newUser);
  };
  
  const logout = () => {
    setUser(null);
  };
  
  return (
    <UserContext.Provider value={{ user, login, logout }}>
      {children}
    </UserContext.Provider>
  );
}

// 3. 使用 Context（类比 @Autowired）
function useUser() {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error('useUser 必须在 UserProvider 内部使用');
  }
  return context;
}

// 4. 子组件中使用
function UserInfo() {
  // 使用 hook 获取上下文（类比注入 Bean）
  const { user, logout } = useUser();
  
  if (!user) {
    return <div>请登录</div>;
  }
  
  return (
    <div>
      <span>欢迎, {user.name}</span>
      <button onClick={logout}>退出</button>
    </div>
  );
}
```

### 4. useReducer（类比 Redux 或 CQRS）
```typescript
import React, { useReducer } from 'react';

/**
 * useReducer 状态管理（类比 Redux 或 CQRS）
 * 适用于复杂状态逻辑，比 useState 更强大
 */

// 1. 定义状态和操作类型
interface State {
  count: number;
  user: User | null;
  loading: boolean;
}

type Action = 
  | { type: 'INCREMENT' }
  | { type: 'DECREMENT' }
  | { type: 'SET_USER'; payload: User }
  | { type: 'CLEAR_USER' }
  | { type: 'SET_LOADING'; payload: boolean };

// 2. Reducer 函数（类比事件处理器或命令处理）
/**
 * 状态更新函数
 * 
 * 类比说明：
 * - 类比 CQRS 中的 Command Handler
 * - 接收当前状态和操作，返回新状态
 * - 必须是纯函数，无副作用
 */
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'INCREMENT':
      return { ...state, count: state.count + 1 };
    
    case 'DECREMENT':
      return { ...state, count: state.count - 1 };
    
    case 'SET_USER':
      return { ...state, user: action.payload, loading: false };
    
    case 'CLEAR_USER':
      return { ...state, user: null };
    
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    
    default:
      return state;
  }
}

// 3. 初始化状态（类比工厂方法）
function init(initialCount: number): State {
  return {
    count: initialCount,
    user: null,
    loading: false,
  };
}

// 4. 使用 useReducer
function Counter() {
  const [state, dispatch] = useReducer(reducer, 0, init);
  
  return (
    <div>
      <p>计数: {state.count}</p>
      <button onClick={() => dispatch({ type: 'INCREMENT' })}>
        +1
      </button>
      <button onClick={() => dispatch({ type: 'DECREMENT' })}>
        -1
      </button>
    </div>
  );
}
```

### 5. 自定义 Hook（类比 AOP 切面）
```typescript
import { useState, useEffect, useCallback } from 'react';

/**
 * 自定义 Hook（类比 Spring AOP 切面）
 * 抽取横切关注点，封装可复用的逻辑
 */

/**
 * 请求数据的 Hook（类比 Feign Client 或 RestTemplate）
 */
function useFetch<T>(url: string) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  
  useEffect(() => {
    // 请求取消标记
    let cancelled = false;
    
    const fetchData = async () => {
      try {
        setLoading(true);
        const res = await fetch(url);
        const result = await res.json();
        
        if (!cancelled) {
          setData(result);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err as Error);
          setData(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    
    fetchData();
    
    // 清理函数（类比 @PreDestroy）
    return () => {
      cancelled = true;
    };
  }, [url]);
  
  return { data, loading, error };
}

/**
 * 防抖 Hook（类比 Redis 缓存 + 过期策略）
 */
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);
  
  useEffect(() => {
    // 设置定时器（类比缓存过期）
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);
    
    // 清理定时器
    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);
  
  return debouncedValue;
}

/**
 * 本地存储 Hook（类比 @Cacheable 或 Session 存储）
 */
function useLocalStorage<T>(key: string, initialValue: T) {
  // 从本地存储读取
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      console.error(`读取本地存储 ${key} 失败`, error);
      return initialValue;
    }
  });
  
  // 更新存储
  const setValue = (value: T | ((val: T) => T)) => {
    try {
      const valueToStore = value instanceof Function 
        ? value(storedValue) 
        : value;
      
      setStoredValue(valueToStore);
      window.localStorage.setItem(key, JSON.stringify(valueToStore));
    } catch (error) {
      console.error(`写入本地存储 ${key} 失败`, error);
    }
  };
  
  return [storedValue, setValue] as const;
}
```

## Ant Design 组件使用

### 1. 基础表单（类比 Spring Validation）
```typescript
import React, { useState } from 'react';
import { Form, Input, Button, Select, message, Card } from 'antd';

/**
 * 登录表单组件
 * 
 * 类比说明：
 * - Form 组件类比 Spring @Valid + BindingResult
 * - rules 类比 Hibernate Validation 注解
 * - onFinish 类比 @PostMapping 处理方法
 */
interface LoginFormValues {
  username: string;
  password: string;
  remember: boolean;
}

function LoginForm() {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  
  // 提交处理（类比 @PostMapping 方法）
  const onFinish = async (values: LoginFormValues) => {
    setLoading(true);
    
    try {
      console.log('登录参数:', values);
      
      // 模拟登录请求（类比调用远程服务）
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      message.success('登录成功！');
    } catch (error) {
      message.error('登录失败，请重试');
    } finally {
      setLoading(false);
    }
  };
  
  // 提交失败处理
  const onFinishFailed = (errorInfo: any) => {
    console.log('表单验证失败:', errorInfo);
    message.warning('请完善表单信息');
  };
  
  return (
    <Card title="用户登录" style={{ maxWidth: 400, margin: '0 auto' }}>
      {/* Form 组件（类比 @Valid 表单验证） */}
      <Form
        form={form}
        name="login"
        initialValues={{ remember: true }}
        onFinish={onFinish}
        onFinishFailed={onFinishFailed}
        autoComplete="off"
        layout="vertical"
      >
        {/* 用户名输入框 */}
        <Form.Item
          label="用户名"
          name="username"
          rules={[
            { required: true, message: '请输入用户名' },
            { min: 3, message: '用户名至少3个字符' },
            { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线' },
          ]}
        >
          <Input 
            prefix={<span>👤</span>} 
            placeholder="请输入用户名" 
          />
        </Form.Item>
        
        {/* 密码输入框 */}
        <Form.Item
          label="密码"
          name="password"
          rules={[
            { required: true, message: '请输入密码' },
            { min: 6, message: '密码至少6个字符' },
          ]}
        >
          <Input.Password 
            prefix={<span>🔒</span>}
            placeholder="请输入密码" 
          />
        </Form.Item>
        
        {/* 记住我 */}
        <Form.Item name="remember" valuePropName="checked">
          <label>记住我</label>
        </Form.Item>
        
        {/* 提交按钮 */}
        <Form.Item>
          <Button 
            type="primary" 
            htmlType="submit" 
            loading={loading}
            block
          >
            登录
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
}

export default LoginForm;
```

### 2. 表格组件（类比 JPA Repository + Pageable）
```typescript
import React, { useState, useEffect } from 'react';
import { Table, Tag, Space, Button, Input, message } from 'antd';
import { SearchOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';

/**
 * 用户表格组件
 * 
 * 类比说明：
 * - Table 组件类比 Spring Data JPA 的 Page<T>
 * - columns 配置类比 @Query 方法的返回映射
 * - pagination 类比 Pageable 分页参数
 */
interface User {
  id: number;
  name: string;
  email: string;
  status: 'active' | 'inactive';
  roles: string[];
}

function UserTable() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [searchText, setSearchText] = useState('');
  
  // 表格列配置（类比 JPA 的投影接口）
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户名',
      dataIndex: 'name',
      key: 'name',
      // 搜索过滤（类比 @Query 方法的动态查询）
      filteredValue: searchText ? [searchText] : null,
      onFilter: (value: string, record: User) => 
        record.name.toLowerCase().includes(value.toLowerCase()),
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      // 自定义渲染（类比 @JsonFormat）
      render: (status: string) => (
        <Tag color={status === 'active' ? 'green' : 'red'}>
          {status === 'active' ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '角色',
      dataIndex: 'roles',
      key: 'roles',
      // 数组渲染
      render: (roles: string[]) => (
        <>
          {roles.map(role => (
            <Tag key={role} color="blue">{role}</Tag>
          ))}
        </>
      ),
    },
    {
      title: '操作',
      key: 'action',
      // 操作列（类比 @RestController 的 CRUD 端点）
      render: (_: any, record: User) => (
        <Space size="middle">
          <Button 
            type="link" 
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button 
            type="link" 
            danger 
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];
  
  // 加载数据（类比 @Query 方法）
  const fetchUsers = async (page: number = 1) => {
    setLoading(true);
    
    try {
      const res = await fetch(`/api/users?page=${page}&size=${pagination.pageSize}`);
      const data = await res.json();
      
      setUsers(data.content);
      setPagination({
        ...pagination,
        current: page,
        total: data.totalElements,
      });
    } catch (error) {
      message.error('获取用户列表失败');
    } finally {
      setLoading(false);
    }
  };
  
  // 分页变化处理
  const handleTableChange = (newPagination: any) => {
    fetchUsers(newPagination.current);
  };
  
  // 编辑处理
  const handleEdit = (user: User) => {
    console.log('编辑用户:', user);
    message.info(`编辑用户: ${user.name}`);
  };
  
  // 删除处理
  const handleDelete = (user: User) => {
    console.log('删除用户:', user);
    message.success(`已删除用户: ${user.name}`);
  };
  
  // 搜索处理
  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchText(e.target.value);
  };
  
  // 初始加载（类比 @PostConstruct）
  useEffect(() => {
    fetchUsers(1);
  }, []);
  
  return (
    <div>
      {/* 搜索框 */}
      <Input
        placeholder="搜索用户名"
        prefix={<SearchOutlined />}
        style={{ width: 300, marginBottom: 16 }}
        onChange={handleSearch}
      />
      
      {/* 表格组件 */}
      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"  // 行唯一标识（类比 @Id）
        loading={loading}
        pagination={pagination}
        onChange={handleTableChange}
      />
    </div>
  );
}

export default UserTable;
```

### 3. 弹窗组件（类比 @Modal 或 Dialog）
```typescript
import React, { useState } from 'react';
import { Modal, Button, Form, Input, message } from 'antd';

/**
 * 用户编辑弹窗
 * 
 * 类比说明：
 * - Modal 组件类比 Java Swing 的 JDialog
 * - visible 控制显示隐藏（类比 show() / hide()）
 * - onCancel / onOk 类比对话框的确定/取消回调
 */
interface UserEditModalProps {
  visible: boolean;
  user: User | null;
  onCancel: () => void;
  onSuccess: (user: User) => void;
}

function UserEditModal({ 
  visible, 
  user, 
  onCancel, 
  onSuccess 
}: UserEditModalProps) {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  
  // 打开弹窗时填充表单（类比 @ModelAttribute）
  React.useEffect(() => {
    if (visible && user) {
      form.setFieldsValue(user);
    } else if (!visible) {
      form.resetFields();
    }
  }, [visible, user, form]);
  
  // 确认提交（类比 @PostMapping）
  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      
      // 模拟保存请求
      await new Promise(resolve => setTimeout(resolve, 500));
      
      message.success('保存成功！');
      onSuccess({ ...user, ...values });
    } catch (error) {
      console.error('表单验证失败:', error);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <Modal
      title={user ? `编辑用户: ${user.name}` : '新增用户'}
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      okText="确定"
      cancelText="取消"
      destroyOnClose  // 关闭时销毁子组件（类比 @PreDestroy）
    >
      <Form
        form={form}
        layout="vertical"
        name="userForm"
      >
        <Form.Item
          name="name"
          label="用户名"
          rules={[
            { required: true, message: '请输入用户名' },
            { min: 3, message: '用户名至少3个字符' },
          ]}
        >
          <Input placeholder="请输入用户名" />
        </Form.Item>
        
        <Form.Item
          name="email"
          label="邮箱"
          rules={[
            { required: true, message: '请输入邮箱' },
            { type: 'email', message: '请输入有效的邮箱地址' },
          ]}
        >
          <Input placeholder="请输入邮箱" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

// 父组件中使用
function UserManagement() {
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  
  const handleAdd = () => {
    setEditingUser(null);
    setModalVisible(true);
  };
  
  const handleEdit = (user: User) => {
    setEditingUser(user);
    setModalVisible(true);
  };
  
  const handleModalSuccess = (user: User) => {
    setModalVisible(false);
    console.log('保存的用户:', user);
    // 刷新列表
  };
  
  return (
    <div>
      <Button type="primary" onClick={handleAdd}>
        新增用户
      </Button>
      
      <UserEditModal
        visible={modalVisible}
        user={editingUser}
        onCancel={() => setModalVisible(false)}
        onSuccess={handleModalSuccess}
      />
    </div>
  );
}
```

## 最佳实践

### 1. 组件设计原则
```typescript
// ✅ 好的实践：单一职责
// 每个组件只做一件事
function UserAvatar({ userId }: { userId: number }) {
  const { data: user, loading } = useFetch<User>(`/api/users/${userId}`);
  
  if (loading) return <Skeleton.Avatar />;
  if (!user) return null;
  
  return <img src={user.avatarUrl} alt={user.name} />;
}

// ❌ 不好的实践：职责过多
// 一个组件做了太多事情
function UserProfileWithEditAndList() {
  // 显示用户信息 + 编辑表单 + 用户列表
  // 应该拆分为多个组件
}
```

### 2. Props 传递原则
```typescript
// ✅ 好的实践：最小化 Props
// 只传递子组件真正需要的
function UserCard({ user }: { user: User }) {
  // 只需要 name 和 avatar，直接传递
  return (
    <div>
      <UserAvatar userId={user.id} />
      <span>{user.name}</span>
    </div>
  );
}

// ❌ 不好的实践：传递过多 Props
// 传递了子组件不需要的数据
function BadUserCard({ user, onUpdate, onDelete, fetchData }: UserCardProps) {
  // 子组件不需要 onUpdate、onDelete、fetchData
}
```

### 3. 状态管理
```typescript
// ✅ 好的实践：正确区分状态
function GoodComponent() {
  const [count, setCount] = useState(0);          // 组件状态
  const [user, setUser] = useState<User | null>(null);  // 服务器状态
  
  const { data: products } = useFetch<Product[]>('/api/products');  // 服务器状态
  
  // 避免：不要把服务器状态同步到本地状态
  // const [productsCopy, setProductsCopy] = useState(products);  // 错误！
}
```

### 4. 性能优化
```typescript
// ✅ 好的实践：使用 useMemo 缓存计算结果
function ExpensiveComponent({ data }: { data: User[] }) {
  // 缓存过滤结果，避免每次渲染都重新计算
  const activeUsers = useMemo(() => 
    data.filter(user => user.status === 'active'),
    [data]
  );
  
  // 缓存排序结果
  const sortedUsers = useMemo(() => 
    [...activeUsers].sort((a, b) => a.name.localeCompare(b.name)),
    [activeUsers]
  );
  
  return <UserList users={sortedUsers} />;
}

// ✅ 好的实践：使用 useCallback 缓存回调
function Parent() {
  const [count, setCount] = useState(0);
  
  // 缓存回调，避免子组件不必要的重新渲染
  const handleClick = useCallback(() => {
    setCount(c => c + 1);
  }, []);  // 空依赖：回调不需要依赖任何变量
  
  return <Child onClick={handleClick} count={count} />;
}
```

### 5. 中文注释规范
```typescript
// ✅ 好的实践：中文注释解释业务逻辑
/**
 * 计算用户折扣价格
 * 
 * 业务规则：
 * - VIP 用户享受 15% 折扣
 * - 普通用户享受 5% 折扣
 * - 折扣金额四舍五入到分
 */
function calculateDiscountPrice(price: number, user: User): number {
  const discountRate = user.roles.includes('vip') ? 0.15 : 0.05;
  const discountAmount = Math.round(price * discountRate * 100) / 100;
  return price - discountAmount;
}

// ❌ 不好的实践：注释说显而易见的事情
// const name = '张三';  // 把 name 设为张三（这有什么意义？）
```

## 常见问题解答

### Q1: useState 和 useReducer 怎么选？
**答案**:
- **useState**: 简单状态（数字、字符串、简单对象）
- **useReducer**: 复杂状态、多个相关状态、状态更新逻辑复杂

### Q2: 为什么子组件会不必要的重新渲染？
**答案**:
- 父组件传递的对象/数组是新的引用
- 解决方案：使用 `useMemo` 缓存计算结果，使用 `useCallback` 缓存回调

### Q3: useEffect 的依赖数组怎么填？
**答案**:
- 填写组件中使用的所有变量
- 不要遗漏，否则可能读取到旧值
- 使用 ESLint 的 `exhaustive-deps` 规则检查

### Q4: Context 太深怎么办？
**答案**:
- 使用 Context 组合（分解为多个小 Context）
- 使用 `use-context-selector` 只订阅需要的状态
- 考虑使用 Zustand 或 Jotai 等状态管理库

## 学习资源推荐

### 1. 官方文档
- [React 官方文档](https://react.dev)
- [TypeScript 官方文档](https://www.typescriptlang.org/docs/)
- [Ant Design 官方文档](https://ant.design/components/overview)

### 2. 视频教程
- [尚硅谷 React 教程](https://www.bilibili.com/video/BV1wy4y1D7JT)
- [TypeScript 入门教程](https://wangdoc.com/typescript/)

### 3. 项目实战
- 从简单的 CRUD 应用开始
- 学习 Ant Design 组件库的使用
- 尝试实现一个完整的用户管理系统
