<h1>VlogApp - V2 - Android端 影视流媒体应用</h1>

## 项目概述 (Project Overview)

VlogApp 是一个基于 Jetpack Compose 开发的视频流媒体应用，提供视频浏览、播放、搜索和个人中心等功能。应用采用现代 Android 开发技术栈，包括 Jetpack Compose、MVVM 架构、Room 数据库和 Retrofit 网络请求等。

### 技术栈 (Tech Stack)

- **UI框架**: Jetpack Compose (BOM 2025.05.00)
- **架构模式**: MVVM + Repository Pattern
- **依赖注入**: Hilt (2.55)
- **网络请求**: Retrofit (2.9.0) + OkHttp (4.12.0)
- **JSON解析**: Moshi (1.15.0)
- **本地数据库**: Room (2.7.1)
- **图片加载**: Coil (2.7.0)
- **视频播放**: Media3 (1.7.1)
- **导航**: Navigation Compose (2.9.0)
- **编译工具**: KSP (2.1.10-1.0.30)
- **最低SDK**: 29 (Android 10)
- **目标SDK**: 36 (Android 14)

## 架构设计 (Architecture)

### MVVM 架构

本项目采用 MVVM (Model-View-ViewModel) 架构模式:

- **Model**: 数据层，包括 Repository 和数据源
- **View**: UI 层，使用 Jetpack Compose 构建
- **ViewModel**: 业务逻辑层，连接 View 和 Model

### 数据流 - 要点

#### 架构模式
```
UI (Composables) <-> ViewModel <-> Repository <-> LocalDataSource (Room)
                                      ↓
                                 RemoteDataSource (API)
```

#### 核心原则
**Single Source of Truth (单一数据源)**
- 页面数据的唯一真相来源为本地 Room 数据库
- Room 数据库作为应用内所有数据的权威来源
- UI 层只从 ViewModel 获取数据，ViewModel 只从 Repository 获取数据

#### 数据同步策略
**数据更新机制**
- Room 数据的真相来源于 API 请求
- 用户可通过下拉刷新或页面刷新按钮触发数据更新
- Repository 负责协调本地和远程数据源的同步

**版本控制与冲突解决**
- 使用时间戳或版本号进行数据版本控制
- 远程数据优先原则：API 数据覆盖本地数据
- 实现乐观锁机制处理并发更新

#### 状态管理
**数据状态封装**
```kotlin
sealed class DataState<T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Error(val exception: Throwable) : DataState<Nothing>()
}
```

**错误处理策略**
- 网络错误：显示缓存数据 + 错误提示
- 数据解析错误：记录日志 + 降级处理
- 数据库错误：重试机制 + 用户友好提示

#### 性能优化
**缓存策略**
- 内存缓存：ViewModel 层面的数据缓存
- 磁盘缓存：Room 数据库持久化存储
- 图片缓存：Coil 库自动处理

**分页加载**
- 推荐使用 Paging 3 库实现高效分页
- Repository 层统一处理分页逻辑
- 支持网络和本地数据源的无缝切换

#### Repository 实现模式
**数据获取流程**
1. 首先从 Room 获取缓存数据立即展示
2. 并行发起 API 请求获取最新数据
3. 更新 Room 数据库
4. 通过 Flow 自动通知 UI 更新

**事务处理**
- 使用 Room 事务确保数据一致性
- 批量操作使用 @Transaction 注解
- 关联数据的原子性更新

### 项目结构 (Project Structure)

```
app/src/main/java/com/vlog/app/
├── MainActivity.kt                 # 应用入口Activity
├── VlogApp.kt                     # Application类，Hilt入口点
├── data/                          # 数据层
│   ├── ApiResponse.kt             # API响应封装
│   ├── ApiResponseCode.kt         # API响应码定义
│   ├── model/                     # 数据模型
│   ├── users/                     # 用户相关数据
│   │   ├── Users.kt               # 用户数据模型
│   │   ├── UserService.kt         # 用户API服务
│   │   ├── UserRepository.kt      # 用户数据仓库
│   │   ├── UserDataRepository.kt  # 用户本地数据仓库
│   │   └── UserSessionManager.kt  # 用户会话管理
│   └── videos/                    # 视频相关数据
│       ├── Videos.kt           # 视频列表模型
│       ├── VideoDetail.kt         # 视频详情模型
│       ├── Categories.kt          # 分类模型
│       ├── GatherList.kt          # 集数列表模型
│       └── PlayList.kt            # 播放列表模型
├── di/                            # 依赖注入
│   ├── Constants.kt               # 常量定义
│   └── NetworkModule.kt           # 网络模块配置
├── navigation/                    # 导航
│   ├── BottomNavItem.kt           # 底部导航项
│   └── VlogNavigation.kt          # 导航配置
├── screens/                       # 页面
│   ├── home/                      # 首页
│   │   ├── HomeScreen.kt          # 首页UI
│   │   └── HomeViewModel.kt       # 首页ViewModel
│   ├── users/                     # 用户相关页面
│   │   ├── LoginScreen.kt         # 登录页面
│   │   ├── RegisterScreen.kt      # 注册页面
│   │   └── UserViewModel.kt       # 用户ViewModel
│   ├── videos/                    # 视频页面
│   │   └── VideosScreen.kt        # 视频列表页面
│   ├── profile/                   # 个人中心
│   │   └── ProfileScreen.kt       # 个人中心页面
│   ├── publish/                   # 发布页面
│   │   └── PublishScreen.kt       # 发布页面
│   └── subscribes/                # 订阅页面
│       └── SubscribesScreen.kt    # 订阅页面
└── ui/                            # UI主题
    └── theme/                     # 主题配置
        ├── Color.kt               # 颜色定义
        ├── Shape.kt               # 形状定义
        ├── Theme.kt               # 主题配置
        ├── Type.kt                # 字体配置
        ├── Typography.kt          # 排版配置
        └── VlogLine.kt            # 自定义组件
```

### 关键组件

- **Repository**: 数据仓库，负责协调本地和远程数据源
- **ViewModel**: 管理 UI 状态和处理用户交互
- **Composables**: 声明式 UI 组件
- **Room Database**: 本地数据存储
- **Retrofit**: 网络请求
- **Okhttp3**: 网络请求
- **Moshi**: Json解析
- **media3**: 播放器
  
### API 说明

#### API 基础配置

- **基础URL**: `https://api.66log.com/api/json/v2/`
- **应用版本**: 1.0.0
- **认证方式**: Token-based authentication
- **请求拦截器**: 自动添加 `app_info` 参数

#### 当前 API 状态

项目已配置以下 API 端点：

**用户相关 API**:
- GET `users/stated-name` - 检查用户名可用性
- GET `users/stated-nickname` - 检查昵称可用性
- POST `users/login` - 用户登录
- POST `users/register` - 用户注册
- GET `users/stated-me/{name}/{token}` - 获取用户信息
- POST `users/updated/{name}/{token}` - 更新用户信息

**视频相关 API**:
- GET `videos/categories/{type}` - 获取视频分类
- GET `videos/list` - 获取视频列表
- GET `videos/detail/{id}` - 获取视频详情
- GET `videos/comments/{videoId}` - 获取视频评论
- POST `videos/comments-created/{videoId}` - 发布视频评论
- GET `videos/search?key={searchKey}` - 搜索视频


**新增 订阅 API**:
- GET `videos/favorites/{username}?token={token}` - 获取订阅列表
- POST `videos/favorites-created/{videoId}?name={username}&token={token}` - 订阅视频
- POST `videos/favorites-removed/{videoId}?name={username}&token={token}` - 移除订阅视频


#### 主要 API 端点

##### Videos List API
```
GET videos/list?typed={typed}&page={page}&size={size}&year={year}&order_by={orderBy}&cate={cate}&token={token}
```
参数说明：
- `typed`: 视频类型 (1=电影, 2=连续剧, 3=动漫, 4=综艺)
- `year`: 年份 (0=全部, 1...2022，或具体年份如 2025)
- `order_by`: 排序方式 (0="ORDER BY PUBLISHED AT: 按上映时间排序", 1="ORDER BY SCORE: 按评分排序", 2="VIDEO ORDER BY HOT: 按热度排序", 3="VIDEO ORDER BY RECOMMEND: 按推荐排序")
- `page`: 页码
- `size`: 页面数据大小，最大24，最小12
- `cate`: 分类 ID (可选)
- `token`: 用户令牌 (可选)

返回数据模型：
data class Videos

##### Videos Detail API
```
GET videos/detail/{id}?gather={gatherId}&token={token}
```
参数说明：
- `id`: 视频 ID
- `token`: 用户令牌 (可选)

返回数据模型：
data class VideoDetail

##### 分类 API
```
GET videos/categories/{typed}
```
参数说明：
- `typed`: 视频类型 (1=电影, 2=连续剧, 3=动漫, 4=综艺)
返回数据模型：
data class Categories


##### 评论相关 API
```
GET videos/comments/{videoId}?token={token}
POST videos/comments-post/{videoId}?token={token}
```

##### 搜索 API
```
GET videos/search?key={searchKey}&token={token}
```


##### 订阅 API
```
GET `videos/favorites/{username}?token={token}` //获取订阅列表
POST `videos/favorites-created/{videoId}?name={username}&token={token}` //订阅视频
POST `videos/favorites-removed/{videoId}?name={username}&token={token}` //移除订阅视频
POST `videos/favorites-videos/{username}?token={token}` //更新订阅视频数据，此数据由于涉及多表查询完整数据，需要记录刷新时间，限制5分钟才能执行一次
```

## 开发规范 (Development Guidelines)

### 代码风格

1. **命名规范**
   - 类名: 使用 PascalCase (如 `VideoRepository`)
   - 函数名: 使用 camelCase (如 `loadHomeData()`)
   - 常量: 使用 UPPER_SNAKE_CASE (如 `API_BASE_URL`)
   - 变量: 使用 camelCase (如 `Videos`)

2. **注释规范**
   - 为所有公共 API 添加 KDoc 注释
   - 复杂逻辑需要添加详细注释
   - 使用中文或英文注释，但保持一致性

3. **包结构**
   - 按功能模块组织代码
   - 相关功能放在同一个包中

### UI 规范

1. **组件化**
   - 将 UI 拆分为可复用的组件
   - 组件应该是独立的，可测试的
   - 遵循单一职责原则

2. **主题一致性**
   - 使用 `VlogAppTheme` 中定义的颜色、形状和排版
   - 不要硬编码颜色值，使用 `MaterialTheme.colorScheme`


### 数据模型规范

1. **实体设计**
   - 数据库实体类使用 `Entity` 后缀
   - 网络响应模型与本地实体分离
   - 提供转换方法在不同模型之间转换

2. **Repository 实现**
   - 遵循单一职责原则
   - 提供清晰的 API 接口
   - 处理数据缓存和同步逻辑

## 当前实现状态 (Current Implementation Status)

### ✅ 已完成功能

#### 1. 基础架构
- ✅ **项目架构**: MVVM + Repository Pattern
- ✅ **依赖注入**: Hilt 配置完成
- ✅ **网络层**: Retrofit + OkHttp + Moshi 配置
- ✅ **导航系统**: Navigation Compose 配置
- ✅ **主题系统**: Material3 主题配置

#### 2. 用户系统
- ✅ **用户数据模型**: Users 数据类
- ✅ **用户API服务**: UserService 接口定义
- ✅ **用户仓库**: UserRepository 和 UserDataRepository
- ✅ **用户ViewModel**: 完整的用户状态管理
- ✅ **登录页面**: LoginScreen UI 实现
- ✅ **注册页面**: RegisterScreen UI 实现
- ✅ **用户会话管理**: UserSessionManager

#### 3. 视频系统
- ✅ **视频数据模型**: Videos, VideoDetail, Categories, GatherList, PlayList 等完整数据模型
- ✅ **视频API服务**: VideoService 接口定义，符合 API 规范，包含详情页面 API
- ✅ **视频仓库**: VideoRepository 和 VideoDataRepository 实现，支持详情数据获取
- ✅ **视频ViewModel**: 完整的视频状态管理和筛选逻辑
- ✅ **视频筛选列表页面**: VideosScreen 完整实现
  - ✅ 类型、分类、年份、排序筛选功能
  - ✅ 网格布局视频列表展示
  - ✅ 下拉刷新和分页加载
  - ✅ Room 数据库集成和缓存
  - ✅ 点击跳转视频详情功能
- ✅ **视频详情页面**: VideoDetailScreen 完整实现
  - ✅ VideoDetailViewModel 状态管理
  - ✅ 完整的视频信息展示界面
  - ✅ 集数选择对话框
  - ✅ 播放器区域和导航功能
- ✅ **基础页面结构**: HomeScreen 等页面框架

#### 4. 导航系统
- ✅ **底部导航**: 5个主要页面的导航配置
- ✅ **页面路由**: 登录、注册、主要功能页面路由
- ✅ **认证流程**: 登录状态检查和页面跳转
- ✅ **搜索导航**: 支持搜索参数传递的路由配置

#### 5. 搜索系统
- ✅ **搜索数据模型**: 搜索API响应模型
- ✅ **搜索API服务**: SearchService 接口定义
- ✅ **搜索仓库**: SearchRepository 实现
- ✅ **搜索ViewModel**: 完整的搜索状态管理
- ✅ **搜索页面**: SearchScreen 完整实现
  - ✅ 搜索输入和结果展示
  - ✅ 热门搜索和历史记录
  - ✅ 搜索结果订阅功能集成
- ✅ **搜索集成**: VideosScreen 搜索按钮和导航

#### 6. 订阅系统
- ✅ **订阅数据模型**: FavoritesWithVideo 关联查询模型
- ✅ **订阅API服务**: FavoriteService 接口定义
- ✅ **订阅仓库**: FavoriteRepository Room集成
- ✅ **订阅ViewModel**: 响应式订阅状态管理
- ✅ **订阅页面**: FavoritesScreen 完整实现
- ✅ **订阅组件**: VideoItem 订阅功能集成

#### 7. 观看历史系统
- ✅ **历史数据模型**: WatchHistory 数据模型
- ✅ **历史数据库**: Room 数据库集成
- ✅ **历史仓库**: WatchHistoryRepository 实现
- ✅ **历史ViewModel**: 观看历史状态管理
- ✅ **历史记录**: 自动记录视频观看历史
- ✅ **历史展示**: 观看历史列表页面
- ✅ **历史管理**: 清除历史记录功能

### 🚧 开发计划

#### 1. 视频筛选列表页面 (优先级：高)
- ✅ **视频筛选列表页面** ，如图：design\210731.png
  - ✅ **视频类型筛选**: 支持电影、连续剧、动漫、综艺类型筛选 (0=全部, 1=电影, 2=连续剧, 3=动漫, 4=综艺)
  - ✅ **分类列表**: 根据类型动态获取分类列表，支持分类筛选
  - ✅ **年份筛选**: 支持年份筛选 (0=全部, 2025，2024，2023，2022以前)
  - ✅ **排序方式**: 支持推荐、最新、最热、评分排序 (0=推荐, 1=最新, 2=最热, 3=评分)
  - ✅ **视频列表展示**: 网格布局展示视频，包含标题、评分、更新信息、封面图、标签
  - ✅ **下拉刷新功能**: SwipeRefresh 实现下拉刷新
  - ✅ **分页加载功能**: LazyVerticalGrid 实现无限滚动分页加载
  - ✅ **Room 数据库集成**: 本地数据缓存和离线支持
  - ✅ **API 参数规范**: 所有筛选参数与 API 规范完全一致
  - ✅ 点击跳转视频详情

#### 2. 视频详情页面 (优先级：高)
- ✅ **视频详情页面 UI**: 完整的详情页面界面实现
- ✅ **视频信息展示**: 评分、别名、导演、演员、地区、语言、简介、标签、编剧、更新状态等信息展示
- ✅ **集数列表**: 以对话框形式展现集数选择，支持集数切换
- ✅ **顶部导航**: 返回按钮和刷新功能
- ✅ **播放器区域**: 封面展示和播放按钮
- ✅ **API集成**: 视频详情数据获取和刷新
- ✅ **导航集成**: 从视频列表页面跳转到详情页面
- [ ] 评论系统

#### 3. 视频播放功能 (优先级：中)
- ✅ Media3 播放器集成
- ✅ 播放控制界面
- ✅ 全屏播放支持

#### 4. 订阅功能增强 (优先级：中)
- ✅ 视频详情页面订阅功能
  - ✅ 详情页面订阅按钮集成
  - ✅ 播放器页面订阅功能
  - ✅ 订阅状态同步到服务器
- ✅ 订阅数据优化
  - ✅ 实现订阅视频数据更新API
  - ✅ 5分钟刷新限制机制
  - ✅ 订阅数据批量同步

#### 5. 其他功能 (优先级：低)
- ✅ 个人中心完善
- ✅ 版本更新功能

## 开发环境配置 (Development Setup)

### 环境要求

- **Android Studio**: Arctic Fox 或更高版本
- **JDK**: 11 或更高版本
- **Gradle**: 8.10.1
- **Kotlin**: 2.1.10
- **最低Android版本**: API 29 (Android 10)
- **目标Android版本**: API 36 (Android 14)

### 构建配置

1. **克隆项目**
   ```bash
   git clone [repository-url]
   cd VlogAppV2
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目
   - 等待 Gradle 同步完成

3. **配置API**
   - API基础URL已配置为: `https://api.66log.com/api/json/v2/`
   - 如需修改，请编辑 `Constants.kt` 文件

4. **运行项目**
   - 连接Android设备或启动模拟器
   - 点击运行按钮或使用 `./gradlew assembleDebug`

### 主要依赖版本

- Compose BOM: 2025.05.00
- Hilt: 2.55
- Retrofit: 2.9.0
- Room: 2.7.1
- Media3: 1.7.1
- Coil: 2.7.0

### 注意事项

1. **网络权限**: 应用需要网络权限访问API
2. **存储权限**: 视频缓存和用户数据存储
3. **API认证**: 部分功能需要用户登录和Token认证
4. **版本兼容**: 确保使用兼容的Android SDK版本

## 贡献指南 (Contributing)

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 许可证 (License)

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。
