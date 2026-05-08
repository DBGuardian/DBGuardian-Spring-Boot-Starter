# 危险废物处理企业ERP管理系统

## 项目简介

危险废物处理企业ERP管理系统是一体化业务管理平台，以合同全流程管理为核心，融合客户关系管理、运输调度管理、仓储管理、财务管理、人员管理等功能模块。

## 技术栈

- **框架**: Spring Boot 2.7.18
- **安全框架**: Spring Security
- **ORM框架**: MyBatis-Plus 3.5.3.1
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **消息队列**: RabbitMQ（可选，用于异步消息推送）
- **工具库**: 
  - Lombok
  - Hutool 5.8.20
  - JWT 0.11.5
  - Apache POI（Excel处理）
  - iText（PDF处理）
  - Knife4j（API文档）

## 项目结构

```
src/main/java/com/erp/
├── config/                    # 配置类
│   ├── SecurityConfig.java    # Spring Security配置
│   ├── MybatisPlusConfig.java # MyBatis-Plus配置
│   ├── RedisConfig.java       # Redis配置
│   ├── CorsConfig.java        # 跨域配置
│   └── WebMvcConfig.java      # Web MVC配置
├── common/                   # 公共模块
│   ├── annotation/           # 自定义注解
│   ├── constant/             # 常量定义
│   ├── enums/                # 枚举类
│   ├── exception/            # 异常处理
│   ├── result/               # 统一响应结果
│   └── util/                 # 工具类
├── security/                 # 安全模块
│   ├── filter/               # 过滤器
│   ├── token/                # Token管理
│   └── user/                 # 用户信息
├── controller/               # 控制器层
│   ├── ai/                   # AI助手控制器
│   ├── auth/                 # 认证控制器
│   ├── common/               # 公共控制器
│   ├── contract/             # 合同管理控制器
│   ├── customer/             # 客户管理控制器
│   ├── dashboard/            # 工作台控制器
│   ├── finance/              # 财务管理控制器
│   ├── oa/                   # OA审核控制器
│   ├── production/           # 生产执行控制器
│   ├── report/               # 报表控制器
│   ├── settlement/           # 结算控制器
│   ├── system/               # 系统管理控制器
│   └── transport/            # 运输管理控制器
├── service/                  # 服务层
│   ├── auth/                 # 认证服务
│   ├── common/               # 公共业务服务
│   ├── contract/             # 合同管理服务
│   ├── customer/             # 客户管理服务
│   ├── dashboard/            # 工作台服务
│   ├── finance/              # 财务管理服务
│   ├── oa/                   # OA审核服务
│   ├── production/           # 生产执行服务
│   ├── report/               # 报表服务
│   ├── settlement/           # 结算服务
│   ├── system/               # 系统管理服务
│   └── transport/            # 运输管理服务
├── mapper/                   # 数据访问层
│   ├── common/               # 公共Mapper
│   ├── contract/             # 合同Mapper
│   ├── customer/             # 客户Mapper
│   ├── dashboard/            # 工作台Mapper
│   ├── finance/              # 财务Mapper
│   ├── oa/                   # OA审核Mapper
│   ├── production/           # 生产执行Mapper
│   ├── report/               # 报表Mapper
│   ├── settlement/           # 结算Mapper
│   ├── system/               # 系统管理Mapper
│   └── transport/            # 运输Mapper
├── entity/                   # 实体类
│   ├── common/               # 公共实体
│   ├── contract/             # 合同实体
│   ├── customer/             # 客户实体
│   ├── finance/              # 财务实体
│   ├── oa/                   # OA审核实体
│   ├── production/           # 生产执行实体
│   ├── settlement/           # 结算实体
│   ├── system/               # 系统实体
│   └── transport/            # 运输实体
├── aspect/                   # 切面（AOP）
└── util/                     # 工具类
```

## 模块清单

### Service层 (src/main/java/com/erp/service/customer)
- **CustomerService** - 客户管理服务（客户新增、更新、删除、分页查询、详情查询、批量Excel导入/导出、获取客户报价记录、获取客户合同记录、获取客户跟进记录）
- **CustomerFollowUpService** - 客户跟进服务（查询当前用户跟进记录、新增跟进记录、根据客户ID查询跟进记录）
- **SupplierService** - 供应商管理服务（供应商分页查询、详情查询、单个/批量创建/更新/删除、Excel导入/导出、供应商编码唯一性校验）

### Service层 (src/main/java/com/erp/service/auth)
- **AuthService** - 登录/登出、Token刷新、忘记密码账号校验与邮箱验证码发送/验证

### Service层 (src/main/java/com/erp/service/system)
- **SystemService** - 系统管理服务（员工注册与审核、注册信息分页查询、正式员工分页查询、管理员直接新增/编辑员工、重置员工密码、审核通过/驳回员工注册）
- **LogService** - 日志管理服务（全部日志/操作日志/数据变更日志/登录日志分页查询、日志详情、日志导出），与 `LogMapper` 配合实现
- **LogRecordService** - 日志记录服务（统一记录操作日志、数据变更日志、登录日志，自动裁剪内容长度并捕获异常，避免影响主业务流程）
- **HazardousWasteItemService** - 危险废物名录CRUD、分页查询、引用统计、Excel导入/导出，新增/导入时自动维护废物类别编码与名称映射并关联类别表，删除条目时自动检测并清理无引用的废物类别
- **HazardousWasteCategoryService** - 废物类别限额配置单条查询、分页查询与修改（限额、开始时间、结束时间），支持按废物类别编码/名称筛选，并在分页响应中附带对应危废条目数量
- **MessageService** - 消息通知中心业务服务，提供分页查询、详情、未读数量、统计分析、标记已读/批量已读、批量软删除、全部已读、清空消息等接口能力，所有操作都会校验当前登录人，防止越权
- **MessageNotificationService** - 消息通知发送服务，封装预警、业务通知、系统消息的构建与发送，支持合同到期/应收款逾期预警、审核流转通知、批量预警等场景，故障时自动降级为直写数据库
- **MessageConsumerService** - RabbitMQ消费者服务，监听 `erp.alert.queue`、`erp.business.queue`、`erp.notification.queue`、`erp.system.queue`，调用 `MessageService.processMessage` 将 `MessageDTO` 持久化到 MESSAGE 表，并对紧急/高优先级预警预留扩展处理钩子
- **EmailChannelService** - 邮件通道配置服务，提供SMTP配置读取/保存、测试邮件发送、通用邮件发送能力，支持AES-256加密授权码存储与发送限流
- **AiAgentConfigService / AiChatService** - 大模型智能体配置与 AI 助手对话服务：前者负责管理 `AI_AGENT_CONFIG` 与 `AI_AGENT_GLOBAL_CONFIG`（智能体列表、默认智能体、接口地址、模型名称、API Key 密文及全局限流配置），后者基于已启用的默认智能体通过 OpenAI 兼容接口调用 DeepSeek 等大模型并返回 AI 回答
- **PermissionService** - 权限管理服务，提供权限树构建、页面字段权限映射、分页查询、权限CRUD操作、模块页面关联管理等功能，支持字段级别的权限控制
- **RoleService** - 角色管理服务，提供角色CRUD操作、角色权限关联管理、角色成员查询等功能，支持角色的权限分配与成员管理
- **ReportService** - 报表服务接口，占位预留，后续用于各类统计报表能力
- **FinanceService** - 财务管理服务接口，占位预留，后续用于应收应付、结算等财务能力
- **ProductionService** - 生产执行服务接口，占位预留，后续用于生产执行综合统计能力

### Service层 (src/main/java/com/erp/service/contract)
- **ContractService** - 危废合同服务（合同新增含报价单与合同PDF上传、合同更新、合同详情、合同分页查询、合同统计信息），内置 `customer_snapshot` JSON 快照能力（选择客户即刻固化关键信息，未建档客户走临时抬头）
- **ContractApprovalFlowService** - 合同审批流服务（创建审批流记录、更新审批流记录为合同审核）
- **QuotationService** - 报价单服务（新增/更新/详情/分页查询/导出，支持报价单号、内部编号、客户名称、计价方式、有效期区间、PDF生成状态等组合筛选），自动维护甲/乙方抬头及联系人信息，审核接口可将待审批单据流转为已通过/已拒绝并记录审核意见，统一通过 `FILE` 表追踪报价单PDF
- **BusinessContractService** - 业务合作合同服务（分页查询、新增/更新/删除、支持上传合同文件、状态更新、逻辑删除仅限待审核状态），提供汇款用合同选项搜索、按危废合同ID查询关联业务合同、合同状态数量统计、业务费结算专用合同列表查询等功能
- **OutOfScopeServiceService** - 价外服务管理服务，提供价外服务的CRUD操作，支持按业务类型（报价单/合同）和业务ID查询价外服务列表
- **SalespersonService** - 业务员管理服务，提供业务员的分页查询、详情查询、新增、更新、删除等功能

### Service层 (src/main/java/com/erp/service/production)
- **PickupNoticeService** - 收运通知单服务（创建/更新/删除收运通知单，支持按单号或编号查询详情、分页查询列表与导出、提交/撤回审核、审核通过/拒绝、审核阶段补充合同号、按废物代码查询危废类别限额与已入库量）
- **WeighingSlipService** - 总磅单管理服务，提供总磅单创建、更新、查询、分页查询功能，支持关联多个运输单号，自动计算净重（总重-空重），状态机遵循"待细分/已细分"，已细分状态不能修改，获取总磅单信息时自动加载关联的收运通知单列表和危废明细
- **WarehousingService** - 入库单管理服务，提供批量创建入库单功能（`batchCreateWarehousing`），支持根据总磅单为每个关联的收运通知单自动生成入库单，入库单危废明细自动从收运通知单明细带出；入库单号自动生成（格式：RKD-YYYYMMDD-4位序号）；状态机遵循"待审核/已审核/已结算"，修改需先过状态与锁定校验；提供分页查询、详情、更新、审核、取消审核、删除等完整功能
- **TransferManifestService** - 转移联单服务（分页查询含废物子项、导出Excel数据、批量导出PDF附件ZIP、Excel批量导入、PDF导入、生成联单编号、提交审核、撤回审核、审核通过/拒绝、批量PDF预览）
- **OutboundService** - 出库单服务（创建/更新/审核/详情/分页查询出库单，支持审核流程管理）
- **ProductionService** - 生产执行综合服务接口，占位预留

### Service层 (src/main/java/com/erp/service/transport)
- **VehicleService** - 车辆管理服务（车辆档案分页查询、详情查询、创建、更新、删除、导出、批量导入）
- **DispatchOrderService** - 运输单管理服务（创建/更新运输单，按单号或通知单号查询详情，校验合同缺失与超限风险，分页查询运输单列表，生成单个或批量运输单PDF用于打印，提供导出用运输单列表查询）

### Service层 (src/main/java/com/erp/service/common)
- **FileService** - 文件服务（上传并保存文件、根据业务类型和业务ID查询文件列表、删除文件，内部通过 `FileStorageService` 选择本地或OSS等不同存储实现）
- **FileStorageService / LocalFileStorageServiceImpl** - 文件存储抽象与本地实现（基于 `D:/erp`，按业务类型与日期分目录存储文件，统一生成下载URL并防御路径遍历攻击）

### Service层 (src/main/java/com/erp/service/finance)
- **FinanceService** - 财务管理服务（获取合同关联的可结算入库单、获取入库单对应的危废明细）
- **InvoiceNoticeService** - 开票通知单服务（分页查询、详情查询、创建、更新、提交审批），支持与结算单关联
- **InvoiceService** - 发票管理服务（ZIP/Excel批量导入发票、发票CRUD、分页查询、发票号码查重、开票/作废/红冲操作）
- **FundSubjectService** - 会计科目服务（科目创建/更新/删除/查询，支持带上级科目创建，辅助核算/数量核算/外币核算配置）
- **FundAccountService** - 资金账户服务（账户创建/更新/删除/启用停用/分页查询，支持按组织隔离）
- **FundTransactionService** - 资金流水服务（流水创建/更新/删除/关联回单文件，支持自动生成凭证号）
- **FundSummaryService** - 汇总表服务（获取资金汇总表数据）
- **FundDiaryService** - 日记账服务（日记账明细查询/生成PDF/批量PDF/Excel模板下载/Excel导入/导出）
- **FundSettlementService** - 资金结算服务（结算单创建/更新/提交审核/审核通过/拒绝/关联发票/关联资金流水）
- **FundPeriodService** - 账期服务（账期CRUD/开启/关闭/结账/反结账，支持结账校验）
- **FundOrganizationService** - 组织架构服务（组织CRUD/树形结构查询/启用停用）
- **FundAccountGroupService** - 账户组服务（账户组CRUD/账户组与账户关联管理）
- **BusinessClosureValidationService** - 月末结账校验服务（校验所有科目期初余额、凭证数量、发票状态等）
- **BusinessClosureValidationScheduledService** - 月末结账校验定时任务服务

### Service层 (src/main/java/com/erp/service/settlement)
- **SettlementService** - 结算单服务（获取累积已结算量和合同计划总量、获取入库单危废明细、创建/更新/提交审核/审核通过/拒绝/关联发票/关联资金流水/分页查询/详情/删除结算单）
- **BusinessFeeService** - 业务费服务（业务费分页查询/统计/详情/创建/更新/删除/审核/驳回/批量操作，支持双方向明细自动拆分）

### Service层 (src/main/java/com/erp/service/oa)
- **OaApprovalRecordService** - OA审核记录服务（审核记录统计/分页查询/详情/提交审核/撤回重提/审批通过/驳回，支持多种业务类型）

### Service层 (src/main/java/com/erp/service/report)
- **WarehouseInOutTrendService** - 仓库出入库趋势报表服务
- **ReceivableDetailService** - 应收账款明细表服务（支持Redis缓存，支持重新计算和清除缓存）
- **ReceivableBalanceTrendService** - 应收账款余额趋势报表服务
- **PayableDetailService** - 应付账款明细表服务
- **InoutDetailService** - 出入库明细报表服务
- **FundBalanceTrendService** - 资金余额趋势报表服务
- **EmployeePerformanceService** - 员工业绩占比饼图服务（支持Redis缓存，支持重新计算和清除缓存）
- **ContractSignTrendService** - 合同签订趋势报表服务

### Service层 (src/main/java/com/erp/service/dashboard)
- **DashboardService** - 工作台服务（获取工作台数据统计）

### Mapper层 (src/main/java/com/erp/mapper/customer)
- **CustomerMapper** - 客户分页/列表查询、批量插入、信用代码唯一校验、导出数据查询
- **CustomerFollowUpMapper** - 客户跟进数据访问（按业务员编码查询、按客户编码查询、按客户编码和业务员编码联合查询）
- **SupplierMapper** - 供应商数据访问（分页查询、详情查询、列表查询、批量插入、编码唯一性校验）

### Mapper层 (src/main/java/com/erp/mapper/system)
- **EmployeeRoleMapper** - 新增 `selectRoleNamesByEmployeeId` 方法，用于权限判定
- **EmployeeMapper** - 员工数据访问（根据登录账号查询员工、员工分页查询，用于正式员工列表与唯一性校验）
- **EmployeeRegistrationMapper** - 员工注册数据访问（按登录账号/手机号查询注册记录、注册信息分页查询）
- **HazardousWasteItemMapper** - 危险废物名录分页查询、批量插入（自动回填废物类别编号与名称）、引用统计、废物代码唯一校验、`countByCategoryId` 统计废物类别引用数量
- **HazardousWasteCategoryMapper** - 危险废物类别数据访问（按类别编码/名称查询、批量查询、批量插入并写入名称与限额默认值）
- **MessageMapper** - 消息通知数据访问（分页+多条件过滤、批量更新状态、类型/优先级/状态统计、未读数量统计、全部已读/清空操作）
- **EmailChannelConfigMapper** - 邮件通道配置数据访问
- **AiAgentConfigMapper** - 大模型智能体配置数据访问（基于 MyBatis-Plus + XML，支持按排序号查询全部智能体配置、清除默认智能体标记、设置指定智能体为默认）
- **AiAgentGlobalConfigMapper** - 大模型全局限流配置数据访问（单行配置表 `AI_AGENT_GLOBAL_CONFIG`，提供全局配置读取与更新）
- **LogMapper** - 系统日志数据访问（全部日志/操作日志/数据变更日志/登录日志分页查询、日志详情、导出日志列表）
- **PermissionMapper** - 权限管理数据访问，提供权限树构建、页面字段权限映射、分页查询、权限CRUD操作等
- **RoleMapper** - 角色管理数据访问，提供角色CRUD操作、角色权限关联、角色成员查询等
- **SystemMapper** - 系统管理通用Mapper接口，占位预留，后续用于与系统管理相关的通用数据访问

### Mapper层 (src/main/java/com/erp/mapper/contract)
- **ContractMapper** - 合同分页查询、详情查询，查询结果附带 `customer_snapshot` 字段供服务层/前端获取甲方快照
- **ContractApprovalFlowMapper** - 合同审批流数据访问（按合同编号查询、按合同编号和节点名称查询）
- **ContractItemMapper / ContractWasteItemMapper** - 合同条目及危废条目数据访问（根据合同编号批量查询/维护合同危废明细）
- **OutOfScopeServiceMapper** - 价外服务数据访问，提供价外服务的CRUD操作，支持按业务类型（报价单/合同）和业务ID查询价外服务列表
- **QuotationMapper** - 报价单数据访问（详情查询、分页/导出查询支持甲乙双方信息、内部编号、计价方式、有效期及PDF状态过滤，联合危废条目统计总数量和单位）
- **QuotationItemMapper** - 报价条目数据访问（按报价单编号批量查询与删除）
- **QuotationWasteItemMapper** - 报价危废条目明细数据访问（按报价条目编号批量查询与删除）
- **BusinessContractMapper** - 业务合作合同数据访问（分页查询、详情查询、业务费结算合同列表查询）
- **SalespersonMapper** - 业务员数据访问（分页查询、详情查询、编码唯一性校验）

### Mapper层 (src/main/java/com/erp/mapper/production)
- **PickupNoticeMapper / PickupNoticeItemMapper** - 收运通知单及其危废明细数据访问，支持按通知单号/编号查询、分页查询与导出
- **WeighingSlipMapper / WeighingSlipDispatchMapper** - 总磅单及其与运输单关联关系数据访问，保证一张运输单仅能关联一张总磅单
- **WarehousingMapper** - 入库单数据访问，提供根据入库单号查询（`selectByWarehousingNo`）、统计入库单号是否存在（`countByWarehousingNo`）等方法，支持入库单的创建、查询和唯一性校验
- **WarehousingWasteItemMapper** - 入库单危废明细数据访问，提供入库单危废明细的批量插入和查询功能
- **TransferManifestMapper / TransferManifestItemMapper** - 转移联单及其危废明细数据访问，支持分页查询、导出、详情查询
- **OutboundMapper / OutboundWasteItemMapper** - 出库单及其危废明细数据访问，支持出库单的创建、查询、审核
- **StockMapper** - 库存数据访问，支持库存查询和统计
- **ProductionMapper** - 生产执行通用Mapper接口，占位预留，后续用于生产执行综合统计与报表

### Mapper层 (src/main/java/com/erp/mapper/transport)
- **VehicleMapper** - 车辆管理数据访问（分页查询、详情查询、按车牌号查询、列表查询、批量插入、车牌号唯一性校验）
- **DispatchOrderMapper / DispatchOrderNoticeMapper** - 运输单及其与收运通知单关联关系数据访问，确保一张收运通知单仅能生成一张运输单，并支撑运输执行导出等功能

### Mapper层 (src/main/java/com/erp/mapper/common)
- **FileMapper** - 文件数据访问（按业务类型和业务ID查询文件列表、按业务类型和业务ID删除文件、按业务类型和业务ID集合批量查询文件）

### Mapper层 (src/main/java/com/erp/mapper/finance)
- **FinanceMapper** - 财务管理通用Mapper接口（获取合同关联的可结算入库单、获取入库单对应的危废明细）
- **InvoiceNoticeMapper** - 开票通知单数据访问（分页查询、详情查询）
- **InvoiceNoticeInvoiceMapper** - 开票通知单与发票关联数据访问
- **InvoiceMapper** - 发票数据访问（发票CRUD、发票号码查重、分页查询）
- **InvoiceItemMapper** - 发票明细数据访问
- **FundSubjectMapper** - 会计科目数据访问（科目CRUD、分页查询、树形结构查询、编码唯一性校验）
- **FundSubjectInitialBalanceMapper** - 会计科目期初余额数据访问
- **FundAccountMapper** - 资金账户数据访问（账户CRUD、分页查询、编码唯一性校验）
- **FundAccountGroupMapper** - 账户组数据访问
- **FundAccountInitialBalanceMapper** - 资金账户期初余额数据访问
- **FundTransactionMapper** - 资金流水数据访问（流水CRUD、分页查询、按账户统计）
- **FundSettlementMapper** - 资金结算数据访问（结算单CRUD、分页查询）
- **FundSettlementCheckMapper** - 资金结算核对数据访问
- **FundSettlementCheckItemMapper** - 资金结算核对明细数据访问
- **FundPeriodMapper** - 账期数据访问（账期CRUD、结账状态查询）
- **FundOrganizationMapper** - 组织架构数据访问（组织CRUD、树形查询）

### Mapper层 (src/main/java/com/erp/mapper/settlement)
- **SettlementMapper** - 结算单数据访问（结算单CRUD、分页查询、累积量统计）
- **SettlementReferenceMapper** - 结算单与入库单关联数据访问
- **SettlementWasteDetailMapper** - 结算单危废明细数据访问
- **SettlementInvoiceRelMapper** - 结算单与发票关联数据访问
- **SettlementFundTransactionRelMapper** - 结算单与资金流水关联数据访问
- **BusinessFeeHeaderMapper** - 业务费主表数据访问（分页查询、详情查询、业务费统计）
- **BusinessFeeItemMapper** - 业务费明细数据访问
- **BusinessFeeItemWasteInfoMapper** - 业务费明细危废信息数据访问
- **BusinessFeeSettlementRelMapper** - 业务费与结算单关联数据访问

### Mapper层 (src/main/java/com/erp/mapper/oa)
- **OaApprovalRecordMapper** - OA审核记录数据访问（分页查询、详情查询、审核记录统计）

### Mapper层 (src/main/java/com/erp/mapper/report)
- **ReportMapper** - 报表Mapper接口（综合报表数据汇总查询）
- **WarehouseInOutTrendMapper** - 仓库出入库趋势数据访问
- **ReceivableDetailMapper** - 应收账款明细数据访问
- **ReceivableBalanceTrendMapper** - 应收账款余额趋势数据访问
- **PayableDetailMapper** - 应付账款明细数据访问
- **InoutDetailMapper** - 出入库明细数据访问
- **FundBalanceTrendMapper** - 资金余额趋势数据访问
- **EmployeePerformanceMapper** - 员工业绩数据访问
- **ContractSignTrendMapper** - 合同签订趋势数据访问

### Mapper层 (src/main/java/com/erp/mapper/dashboard)
- **DashboardMapper** - 工作台数据访问（统计各模块业务数据）

### 实体类 (src/main/java/com/erp/entity/contract)
- **Contract** - 危废合同实体（对应 CONTRACT 表）
- **BusinessContract** - 业务合作合同实体（对应 BUSINESS_CONTRACT 表，支持合同文件上传）
- **Quotation** - 报价单实体（对应 QUOTATION 表，包含客户编码、甲乙双方抬头及联系人信息、报价日期、有效期、PDF关联等字段；业务统计型字段如总数量由服务层实时聚合）
- **QuotationItem** - 报价条目实体（对应 QUOTATION_ITEM 表，包含报价模式、付款方、计价方案、备注、小计摘要等字段）
- **QuotationWasteItem** - 报价危废条目明细实体（对应 QUOTATION_WASTE_ITEM 表，包含危废条目编号、废物类别、废物代码、计划数量及计价备注等字段）
- **OutOfScopeService** - 价外服务实体
- **Salesperson** - 业务员实体
- **ContractApprovalFlow** - 合同审批流实体
- **ContractItem / ContractWasteItem / BusinessContractWasteItem** - 合同条目及危废条目实体

### 实体类 (src/main/java/com/erp/entity/customer)
- **Customer** - 客户实体
- **CustomerFollowUp** - 客户跟进记录实体
- **Supplier** - 供应商实体

### 实体类 (src/main/java/com/erp/entity/production)
- **PickupNotice / PickupNoticeItem** - 收运通知单及其危废明细实体
- **WeighingSlip / WeighingSlipDispatch** - 总磅单及其与运输单关联实体
- **Warehousing / WarehousingWasteItem** - 入库单及其危废明细实体
- **TransferManifest / TransferManifestItem** - 转移联单及其危废明细实体
- **Outbound / OutboundWasteItem** - 出库单及其危废明细实体
- **Stock** - 库存实体

### 实体类 (src/main/java/com/erp/entity/transport)
- **Vehicle** - 车辆档案实体
- **DispatchOrder / DispatchOrderNotice** - 运输单及其与收运通知单关联实体

### 实体类 (src/main/java/com/erp/entity/finance)
- **InvoiceNotice** - 开票通知单实体
- **InvoiceNoticeInvoice** - 开票通知单与发票关联实体
- **Invoice / InvoiceItem** - 发票及其明细实体
- **FundSubject** - 会计科目实体
- **FundSubjectInitialBalance** - 会计科目期初余额实体
- **FundAccount** - 资金账户实体
- **FundAccountGroup** - 账户组实体
- **FundAccountInitialBalance** - 资金账户期初余额实体
- **FundTransaction** - 资金流水实体
- **FundPeriod** - 账期实体
- **FundOrganization** - 组织架构实体
- **FundSettlementCheck / FundSettlementCheckItem** - 资金结算核对及其明细实体

### 实体类 (src/main/java/com/erp/entity/settlement)
- **Settlement** - 结算单实体
- **SettlementReference** - 结算单与入库单关联实体
- **SettlementWasteDetail** - 结算单危废明细实体
- **SettlementInvoiceRel** - 结算单与发票关联实体
- **SettlementFundTransactionRel** - 结算单与资金流水关联实体
- **BusinessFeeHeader** - 业务费主表实体
- **BusinessFeeItem** - 业务费明细实体
- **BusinessFeeItemWasteInfo** - 业务费明细危废信息实体
- **BusinessFeeSettlementRel** - 业务费与结算单关联实体

### 实体类 (src/main/java/com/erp/entity/oa)
- **OaApprovalRecord** - OA审核记录实体

### 实体类 (src/main/java/com/erp/entity/system)
- **AiAgentConfig** - 大模型智能体配置实体（对应 AI_AGENT_CONFIG 表，维护智能体编码、名称、提供方、接口地址 Base URL、模型名称、API Key 密文、启用状态、默认智能体标记、排序号及审计字段）

### 实体类 (src/main/java/com/erp/entity/common)
- **BaseEntity** - 基础实体（包含公共字段如创建时间、更新时间、创建人、更新人等）
- **File** - 文件实体

### 工具类 (src/main/java/com/erp/util)
- **QuotationPdfGenerator** - 报价单PDF生成工具类，支持小计摘要合并显示、合计行智能计算（相同unit合并，不同unit分开）、总价包干模式单元格合并（使用|分隔符，合并时去掉quotationItemId）

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+

### 配置说明

1. 修改 `src/main/resources/application.yml` 中的数据库连接信息
2. 修改 `src/main/resources/application.yml` 中的Redis连接信息
3. 执行 `business-workflow-erp.sql` 初始化数据库
4. 根据运维要求设置 `email.channel.aes-key`（32位），用于 SMTP 授权码 AES-256 加密
5. 配置文件上传路径：当前本地存储路径为 `C:/erp`（可在 `file.storage.local.path` 调整；如改用OSS，请补齐相关配置）

### 运行项目

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run

# 打包项目
mvn clean package
```

### 访问地址

- 应用地址: http://localhost:8080/api
- API文档: http://localhost:8080/api/doc.html

## 开发规范与文档统一约定

为避免前后端、数据库文档在不同目录下发生内容不一致，项目约定**只在仓库根目录维护权威文档**，本模块仅做引用说明，不再单独维护重复版文档：

- **数据库结构**：统一查看并维护 `sql/business-workflow-erp.sql`
  - 本目录下已移除重复的 `business-workflow-erp.sql`，如需修改库表结构，请只改根目录 `sql/business-workflow-erp.sql`，并同步更新相关说明文档。

- **后端开发规范文档**：
  - 仅在仓库根目录维护：`后端开发文档.md`
  - 本文件（`business-workflow-erp-java/README.md`）只补充与实现强相关的模块清单和版本历史说明，**禁止再在本目录新建或维护完整版“后端开发文档”**，所有接口/业务设计规范统一写在根目录 `后端开发文档.md`。

- **前端开发规范文档**：
  - 仅在仓库根目录维护：`前端开发文档.md` 与 `README.md` 中的前端章节
  - 如需查看前端页面结构、路由设计、前端调用示例等，请打开根目录的 `前端开发文档.md`，本目录不再单独维护前端文档副本。

- **其他规范/需求文档**：
  - `危险废物处理企业ERP管理系统需求分析.md` 等需求/设计类文档也统一放在仓库根目录维护，本目录仅通过 README 进行引用。

> 后续如在本目录新增任何与“开发规范”相关的大文档，请优先考虑直接在根目录更新对应文档，并在此处增加链接，而不是复制一份完整内容。

## API接口文档

### AI 助手与大模型配置 API

#### 1. AI 助手对话接口

- **接口路径**: `POST /api/ai/chat`
- **Content-Type**: `application/json`
- **响应类型**: `text/event-stream` (SSE流式响应)
- **需要认证**: 是（沿用全局安全配置）

**请求参数（AiChatRequest）**

```json
{
  "mode": "general",
  "question": "如何在系统中创建新的收运通知单？",
  "contextSummary": "当前页面：收运通知单列表，业务类型：PRODUCTION"
}
```

- **mode** `String`：对话模式，可选，例如 `general`（通用问答）、`business`（业务说明）、`analysis`（数据分析提示）等
- **question** `String`：必填，用户问题内容，不能为空
- **contextSummary** `String`：可选，业务上下文摘要（如当前页面、业务模块），会一并传给大模型以便返回更贴合场景的回答

**成功响应（AiChatResponse）**

```json
{
  "code": 200,
  "message": "调用成功",
  "data": {
    "answer": "您可以在“生产执行 > 收运通知单”页面点击右上角的“新增收运通知单”按钮，按步骤填写合同号、客户信息和危废明细后提交审核……"
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

**响应说明**

- 该接口使用 SSE（Server-Sent Events）流式返回，响应类型为 `text/event-stream`
- 服务端会通过 SSE 推送多条 `data:` 消息，最后发送包含 `[STREAM_DONE]` 的结束事件
- 当前实现为一次性整段推送（非 token 级实时流式），前端按 SSE 协议解析即可
- 错误时会在 `data:` 消息中推送 `ERROR:` 前缀的错误信息

**成功响应示例（SSE格式）**

```
data: 您可以在"生产执行 > 收运通知单"页面点击右上角的"新增收运通知单"按钮，按步骤填写合同号、客户信息和危废明细后提交审核……
data: [STREAM_DONE]
```

**失败响应示例（SSE格式）**

```
data: ERROR:未配置可用的大模型智能体
data: [STREAM_DONE]
```

**注意事项**

- 当业务异常（如未配置可用智能体、密钥解密失败）时，会在 SSE 消息中推送 `ERROR:` 前缀的错误信息
- 当第三方大模型接口返回非 2xx 状态码时，会在 SSE 消息中推送 `ERROR:AI对话失败：` 前缀的错误信息
- 该接口不使用项目统一的 `Result<code,message,data,timestamp>` 响应包装，而是直接按 SSE 规范推送

---

#### 2. 大模型智能体配置接口

- **接口前缀**: `/api/system/ai-agent/*`
- **模块位置**: 系统管理 → 大模型配置

##### 2.1 查询智能体列表

- **接口路径**: `GET /api/system/ai-agent/list`
- **功能描述**: 按排序号返回所有已配置的大模型智能体，用于配置页表格展示

**响应示例**

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "agentId": 1,
      "agentCode": "DEEPSEEK_DEFAULT",
      "agentName": "DeepSeek 默认助手",
      "provider": "DEEPSEEK",
      "baseUrl": "https://api.deepseek.com",
      "modelName": "deepseek-chat",
      "status": "ENABLED",
      "defaultAgent": true,
      "sortOrder": 1,
      "remark": "用于 ERP 内置 AI 助手",
      "createTime": "2025-01-01T10:00:00",
      "updateTime": "2025-01-01T10:00:00"
    }
  ],
  "timestamp": "2025-01-01T12:00:00"
}
```

##### 2.2 保存或更新智能体配置

- **接口路径**: `POST /api/system/ai-agent/save`
- **Content-Type**: `application/json`
- **功能描述**: 新增或编辑单个智能体配置；新建时必须配置 API Key

**请求参数（AiAgentConfigSaveRequest）**

```json
{
  "agentId": 1,
  "agentCode": "DEEPSEEK_DEFAULT",
  "agentName": "DeepSeek 默认助手",
  "provider": "DEEPSEEK",
  "baseUrl": "https://api.deepseek.com",
  "modelName": "deepseek-chat",
  "apiKey": "sk-xxxxxx",
  "status": "ENABLED",
  "defaultAgent": true,
  "sortOrder": 1,
  "remark": "用于 ERP 内置 AI 助手"
}
```

- **agentId** `Integer`：可选，编辑时必填，新建时留空
- **agentCode** `String`：必填，智能体编码，系统内唯一
- **agentName** `String`：必填，智能体名称
- **provider** `String`：必填，提供方标识（如 `DEEPSEEK`、`OPENAI` 等）
- **baseUrl** `String`：必填，大模型接口 Base URL，例如 `https://api.deepseek.com`
- **modelName** `String`：必填，模型名称或 ID，例如 `deepseek-chat`
- **apiKey** `String`：新建时必填，编辑时可选（为空表示不修改）；后端使用 `ai.agent.aes-key` 对其进行 AES 加密后写入 `API密钥密文` 字段
- **status** `String`：必填，`ENABLED` / `DISABLED`
- **defaultAgent** `Boolean`：可选，是否设置为默认智能体；为 `true` 时会清除其他记录的默认标记
- **sortOrder** `Integer`：可选，排序号，列表按升序展示
- **remark** `String`：可选，备注

**成功响应**

```json
{
  "code": 200,
  "message": "保存成功",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

##### 2.3 删除智能体配置

- **接口路径**: `POST /api/system/ai-agent/delete/{agentId}`
- **功能描述**: 按智能体编号删除配置记录

**路径参数**

- **agentId** `Integer`：必填，智能体编号（`AI_AGENT_CONFIG.智能体编号`）

##### 2.4 设置默认智能体

- **接口路径**: `POST /api/system/ai-agent/set-default/{agentId}`
- **功能描述**: 将指定智能体标记为默认使用，内部会先清除其他记录的默认标记，再设置当前记录为默认

**路径参数**

- **agentId** `Integer`：必填，智能体编号

##### 2.5 查询全局限流配置

- **接口路径**: `GET /api/system/ai-agent/global-config`
- **功能描述**: 查询大模型调用的全局限流配置（单行配置表 `AI_AGENT_GLOBAL_CONFIG`）

**响应示例（AiAgentGlobalConfigResponse）**

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "maxRequestsPerSecond": 5,
    "maxTextLengthPerRequest": 1200,
    "maxParagraphsPerRequest": 4
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

##### 2.6 保存全局限流配置

- **接口路径**: `POST /api/system/ai-agent/global-config`
- **Content-Type**: `application/json`
- **功能描述**: 保存/更新全局限流配置；若配置表无记录则插入一行，否则执行更新

**请求参数（AiAgentGlobalConfigSaveRequest）**

```json
{
  "maxRequestsPerSecond": 5,
  "maxTextLengthPerRequest": 1200,
  "maxParagraphsPerRequest": 4
}
```

- **maxRequestsPerSecond** `Integer`：每秒最大请求数
- **maxTextLengthPerRequest** `Integer`：每次请求最大文本长度（字符数）
- **maxParagraphsPerRequest** `Integer`：每次请求最大段落数

**成功响应**

```json
{
  "code": 200,
  "message": "保存成功",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

### 用户登录接口

#### 接口信息

- **接口路径**: `/api/auth/login`
- **请求方法**: `POST`
- **Content-Type**: `application/json`

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 验证规则 |
|--------|------|------|------|----------|
| username | String | 是 | 登录账号 | 不能为空 |
| password | String | 是 | 密码 | 不能为空 |
| captcha | String | 是 | 验证码 | 不能为空 |
| captchaKey | String | 是 | 验证码Key | 不能为空 |

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "loginAccount": "13800138000",
      "employeeId": 1,
      "employeeName": "张三",
      "department": "市场部",
      "jobTitle": "业务员",
      "phone": "13800138000",
      "employeeStatus": "在职",
      "roles": ["ROLE_USER"],
      "permissions": ["customer:view", "customer:add"]
    }
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

**失败响应**
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

#### 业务逻辑

1. **验证码验证**
   - 验证验证码Key和验证码是否匹配
   - 验证码验证成功后自动删除
   - 验证码过期时间为5分钟

2. **员工信息查询**
   - 根据登录账号查询员工信息
   - 如果员工不存在，返回"用户名或密码错误"

3. **员工状态检查**
   - 检查员工状态是否为"在职"
   - 如果员工状态为"停用"或"离职"，直接返回"您的账号已停用，请联系管理员"

4. **密码验证**
   - 使用BCrypt比对密码
   - 密码错误时记录登录失败次数
   - 连续5次登录失败，账号锁定30分钟

5. **Spring Security认证**
   - 使用AuthenticationManager进行认证
   - 认证成功后设置SecurityContext

6. **Token生成**
   - 生成JWT访问Token（有效期2小时）
   - 生成JWT刷新Token（有效期7天）
   - Token存储在Redis中，用于快速验证和强制下线

7. **用户信息返回**
   - 返回用户基本信息（员工ID、姓名、部门、岗位等）
   - 返回用户角色列表
   - 返回用户权限列表

#### 前端调用示例

**JavaScript (Fetch)**
```javascript
fetch('/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: '13800138000',
    password: '123456',
    captcha: 'ABCD',
    captchaKey: 'captcha_key_123'
  })
})
.then(response => response.json())
.then(data => {
  if (data.code === 200) {
    // 保存Token到本地存储
    localStorage.setItem('token', data.data.token);
    localStorage.setItem('refreshToken', data.data.refreshToken);
    console.log('登录成功', data.data);
  } else {
    console.error('登录失败', data.message);
  }
});
```

**Axios**
```javascript
import axios from 'axios';

axios.post('/api/auth/login', {
  username: '13800138000',
  password: '123456',
  captcha: 'ABCD',
  captchaKey: 'captcha_key_123'
})
.then(response => {
  if (response.data.code === 200) {
    // 保存Token到本地存储
    localStorage.setItem('token', response.data.data.token);
    localStorage.setItem('refreshToken', response.data.data.refreshToken);
    // 设置请求拦截器，自动添加Token
    axios.defaults.headers.common['Authorization'] = 'Bearer ' + response.data.data.token;
    console.log('登录成功', response.data.data);
  }
})
.catch(error => {
  console.error('登录失败', error.response.data.message);
});
```

#### 错误码说明

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如：字段验证失败） |
| 401 | 登录失败（如：用户名或密码错误、验证码错误） |
| 403 | 账号被禁用 |

#### 注意事项

1. 登录前需要先获取验证码（调用验证码接口）
2. Token存储在Redis中，有效期2小时
3. RefreshToken有效期7天，用于刷新访问Token
4. 连续5次登录失败，账号将被锁定30分钟
5. 登录成功后，后续请求需要在Header中携带Token：`Authorization: Bearer {token}`

### 用户登出接口

#### 接口信息

- **接口路径**: `/api/auth/logout`
- **请求方法**: `POST`
- **Content-Type**: `application/json`
- **需要认证**: 是（需要在Header中携带Token）

#### 请求头

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token，格式：`Bearer {token}` |

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

**失败响应**
```json
{
  "code": 401,
  "message": "Token不存在或无效",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

#### 业务逻辑

1. **Token验证**
   - 从请求Header中获取Token（`Authorization: Bearer {token}`）
   - 如果Token不存在，返回"Token不存在或无效"

2. **Token解析**
   - 从Token中解析出用户名
   - 如果Token解析失败，返回"Token解析失败"

3. **Redis验证**
   - 从Redis中获取存储的Token
   - 验证请求的Token与Redis中的Token是否匹配
   - 如果不匹配，返回"Token已失效"

4. **删除Token**
   - 从Redis删除访问Token
   - 从Redis删除刷新Token
   - 清除SecurityContext

#### 前端调用示例

**JavaScript (Fetch)**
```javascript
const token = localStorage.getItem('token');

fetch('/api/auth/logout', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token
  }
})
.then(response => response.json())
.then(data => {
  if (data.code === 200) {
    // 清除本地存储的Token
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    console.log('登出成功');
  } else {
    console.error('登出失败', data.message);
  }
});
```

**Axios**
```javascript
import axios from 'axios';

axios.post('/api/auth/logout', {}, {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('token')
  }
})
.then(response => {
  if (response.data.code === 200) {
    // 清除本地存储的Token
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    // 清除请求拦截器中的Token
    delete axios.defaults.headers.common['Authorization'];
    console.log('登出成功');
  }
})
.catch(error => {
  console.error('登出失败', error.response.data.message);
});
```

#### 错误码说明

| 错误码 | 说明 |
|--------|------|
| 401 | Token不存在或无效 |
| 400 | 参数错误 |

#### 注意事项

1. 登出接口需要在Header中携带有效的Token
2. 登出成功后，Token将从Redis中删除，无法再次使用
3. 登出后需要清除前端本地存储的Token
4. 登出后需要清除请求拦截器中的Token

### 刷新Token接口

#### 接口信息

- **接口路径**: `/api/auth/refresh`
- **请求方法**: `POST`
- **Content-Type**: `application/json`
- **需要认证**: 是（需要在Header中携带RefreshToken）

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| refreshToken | String | 是 | 刷新Token |

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "刷新Token成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "loginAccount": "13800138000",
      "employeeId": 1,
      "employeeName": "张三",
      "department": "市场部",
      "jobTitle": "业务员",
      "phone": "13800138000",
      "employeeStatus": "在职",
      "roles": ["ROLE_USER"],
      "permissions": ["customer:view", "customer:add"]
    }
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

#### 业务逻辑

1. **RefreshToken验证**
   - 验证RefreshToken是否存在且有效
   - 从RefreshToken中解析用户信息
   - 验证RefreshToken是否在Redis中存在

2. **生成新Token**
   - 生成新的访问Token（有效期2小时）
   - 生成新的刷新Token（有效期7天）
   - 将新Token存储到Redis中

3. **返回用户信息**
   - 返回更新后的Token对
   - 返回用户基本信息和权限

### 查询登录状态接口

#### 接口信息

- **接口路径**: `/api/auth/status`
- **请求方法**: `GET`
- **需要认证**: 是

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "已登录",
  "data": {
    "username": "13800138000",
    "loggedIn": true
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

**未登录响应**
```json
{
  "code": 200,
  "message": "未登录",
  "data": {
    "username": null,
    "loggedIn": false
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

#### 业务逻辑

1. **安全检查**
   - 检查当前SecurityContext中是否存在认证信息
   - 验证认证信息是否有效且非匿名用户

2. **状态返回**
   - 已登录：返回用户名和登录状态
   - 未登录：返回null用户名和false状态

### 忘记密码 - 校验账号与手机号接口

#### 接口信息

- **接口路径**: `/api/auth/reset/verify-account`
- **请求方法**: `POST`
- **Content-Type**: `application/json`
- **需要认证**: 否

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| account | String | 是 | 登录账号（手机号） |
| phone | String | 是 | 手机号 |

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "账号与手机号验证通过",
  "data": {
    "employeeId": 1,
    "employeeName": "张三",
    "email": "zhangsan@example.com"
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

### 忘记密码 - 发送邮箱验证码接口

#### 接口信息

- **接口路径**: `/api/auth/reset/send-email-code`
- **请求方法**: `POST`
- **Content-Type**: `application/json`
- **需要认证**: 否

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| email | String | 是 | 邮箱地址 |

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "验证码发送成功",
  "data": {
    "email": "zhangsan@example.com",
    "codeKey": "reset_pwd_1234567890"
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

### 忘记密码 - 校验邮箱验证码接口

#### 接口信息

- **接口路径**: `/api/auth/reset/verify-email-code`
- **请求方法**: `POST`
- **Content-Type**: `application/json`
- **需要认证**: 否

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| email | String | 是 | 邮箱地址 |
| code | String | 是 | 验证码 |
| codeKey | String | 是 | 验证码Key |

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "验证码校验通过",
  "data": {
    "resetToken": "reset_token_1234567890"
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

### 忘记密码 - 重置密码接口

#### 接口信息

- **接口路径**: `/api/auth/reset/password`
- **请求方法**: `POST`
- **Content-Type**: `application/json`
- **需要认证**: 否

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeId | Integer | 是 | 员工ID |
| resetToken | String | 是 | 重置令牌 |
| newPassword | String | 是 | 新密码 |

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "密码重置成功，请使用新密码登录",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

### 系统就绪检查接口

#### 接口信息

- **接口路径**: `/api/auth/readiness`
- **请求方法**: `GET`
- **需要认证**: 否

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "系统就绪",
  "data": {
    "moduleName": "危险废物处理企业ERP管理系统",
    "description": "系统运行正常",
    "ready": true
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

#### 业务逻辑

1. **系统状态检查**
   - 检查应用是否正常启动
   - 检查数据库连接是否正常
   - 检查Redis连接是否正常

2. **就绪状态返回**
   - 返回模块名称和描述
   - 返回系统就绪状态

### 生成验证码接口

#### 接口信息

- **接口路径**: `/api/auth/captcha`
- **请求方法**: `GET`
- **需要认证**: 否

#### 响应格式

**成功响应**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "captchaKey": "CAP-1234567890-1234",
    "captchaImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

#### 响应字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| captchaKey | String | 验证码Key，用于登录时验证 |
| captchaImage | String | 验证码图片（Base64格式） |

#### 业务逻辑

1. **开发环境**：直接返回固定验证码"9999"，不写入Redis
2. **生产环境**：
   - 对IP进行限流，每分钟最多30次请求
   - 生成4位随机验证码（字母+数字）
   - 验证码存储在Redis中，5分钟过期
   - 返回验证码Key和Base64图片

#### 注意事项

1. 验证码Key需要在登录时一并提交
2. 验证码有效期为5分钟
3. 生产环境每个IP每分钟最多请求30次
4. 验证码图片为PNG格式，Base64编码

### 刷新Token接口

#### 接口信息

- **接口路径**: `/api/auth/refresh`
- **请求方法**: `POST`
- **Content-Type**: `application/json`
- **需要认证**: 是

#### 请求参数

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| refreshToken | String | 是 | 刷新Token |

#### 响应格式

```json
{
  "code": 200,
  "message": "刷新Token成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

#### 注意事项

1. RefreshToken有效期为7天
2. 刷新成功后，旧的Token将失效
3. 新的Token有效期为2小时

### 查询登录状态接口

#### 接口信息

- **接口路径**: `/api/auth/status`
- **请求方法**: `GET`
- **需要认证**: 是

#### 响应格式

```json
{
  "code": 200,
  "message": "已登录",
  "data": {
    "username": "13800138000",
    "loggedIn": true
  }
}
```

### 忘记密码接口

#### 第一步：校验账号与手机号

- **接口路径**: `POST /api/auth/reset/verify-account`
- **请求参数**:
```json
{
  "account": "13800138000",
  "phone": "13800138000"
}
```

#### 第二步：发送邮箱验证码

- **接口路径**: `POST /api/auth/reset/send-email-code`
- **请求参数**:
```json
{
  "email": "user@example.com",
  "employeeId": 1
}
```

#### 第三步：校验邮箱验证码

- **接口路径**: `POST /api/auth/reset/verify-email-code`
- **请求参数**:
```json
{
  "email": "user@example.com",
  "code": "123456",
  "employeeId": 1
}
```

#### 第四步：重置密码

- **接口路径**: `POST /api/auth/reset/password`
- **请求参数**:
```json
{
  "employeeId": 1,
  "newPassword": "newpass123",
  "confirmPassword": "newpass123",
  "verifyCode": "123456"
}
```

### 系统就绪检查接口

#### 接口信息

- **接口路径**: `/api/auth/readiness`
- **请求方法**: `GET`
- **需要认证**: 否

#### 响应格式

```json
{
  "code": 200,
  "message": "系统就绪",
  "data": {
    "moduleName": "危险废物处理企业ERP管理系统",
    "description": "系统运行正常",
    "ready": true
  }
}
```

### 员工注册接口

#### 接口信息

- **接口路径**: `/api/system/employee/register`
- **请求方法**: `POST`
- **Content-Type**: `multipart/form-data`（因为包含文件上传）

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 | 验证规则 |
|--------|------|------|------|----------|
| employeeName | String | 是 | 员工姓名 | 最大50个字符 |
| department | String | 是 | 部门 | 不能为空 |
| jobTitle | String | 是 | 岗位 | 不能为空 |
| phone | String | 是 | 手机号码 | 11位手机号格式，用作登录账号 |
| idCard | String | 否 | 身份证号码 | 18位身份证格式（选填） |
| idCardFront | MultipartFile | 是 | 身份证正面照片 | JPG/PNG格式，单个不超过5MB |
| idCardBack | MultipartFile | 是 | 身份证反面照片 | JPG/PNG格式，单个不超过5MB |
| password | String | 是 | 密码 | 最少6位 |
| confirmPassword | String | 是 | 确认密码 | 需与密码一致 |

#### 响应格式

**成功响应**
```json
{
  "code": 0,
  "message": "注册成功，等待管理员审核",
  "data": {
    "employeeId": 1,
    "employeeName": "张三",
    "loginAccount": "13800138000",
    "employeeStatus": "待审核",
    "message": "注册成功，等待管理员审核"
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

**失败响应**
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null,
  "timestamp": "2025-01-01T12:00:00"
}
```

#### 业务逻辑

1. **数据验证**
   - 验证所有必填字段
   - 验证手机号格式
   - 验证身份证号格式（如果提供）
   - 验证密码一致性
   - 验证文件大小（不超过5MB）
   - 验证文件格式（仅支持JPG/PNG）

2. **唯一性检查**
   - 检查手机号码是否已在 `EMPLOYEE_REGISTRATION` 表中注册
   - 检查手机号码是否已在 `EMPLOYEE` 表中注册
   - 检查登录账号是否已存在

3. **数据保存**
   - 创建 `EMPLOYEE_REGISTRATION` 记录
   - 状态设置为"待审核"
   - 权限分配状态设置为"待分配"
   - 密码使用BCrypt加密存储

4. **文件上传**
   - 员工注册时：身份证照片保存到 `D:/erp/employee_registration_id_card_front/{yyyy/MM/dd}/{UUID}.{扩展名}`，关联 `EMPLOYEE_REGISTRATION` 表的业务ID
   - 审核通过时：身份证照片保存到 `D:/erp/employee_id_card_front/{yyyy/MM/dd}/{UUID}.{扩展名}`，关联 `EMPLOYEE` 表的业务ID
   - 文件信息保存到 `FILE` 表
   - 文件编号关联到对应业务表（注册时关联 `EMPLOYEE_REGISTRATION`，审核通过后关联 `EMPLOYEE`）

5. **事务处理**
   - 整个注册过程在一个事务中完成
   - 如果文件上传失败，会回滚所有操作

#### 数据库表

**EMPLOYEE_REGISTRATION 表**
注册信息保存在此表中，状态为"待审核"，等待管理员审核。

**FILE 表**
身份证照片文件信息保存在此表中，通过文件编号关联到注册记录。

#### 前端调用示例

**JavaScript (FormData)**
```javascript
const formData = new FormData();
formData.append('employeeName', '张三');
formData.append('department', '市场部');
formData.append('jobTitle', '业务员');
formData.append('phone', '13800138000');
formData.append('idCard', '110101199001011234');
formData.append('idCardFront', fileInput1.files[0]);
formData.append('idCardBack', fileInput2.files[0]);
formData.append('password', '123456');
formData.append('confirmPassword', '123456');

fetch('/api/system/employee/register', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(data => {
  if (data.code === 0) {
    console.log('注册成功', data.data);
  } else {
    console.error('注册失败', data.message);
  }
});
```

**Axios**
```javascript
import axios from 'axios';

const formData = new FormData();
formData.append('employeeName', '张三');
formData.append('department', '市场部');
formData.append('jobTitle', '业务员');
formData.append('phone', '13800138000');
formData.append('idCardFront', fileInput1.files[0]);
formData.append('idCardBack', fileInput2.files[0]);
formData.append('password', '123456');
formData.append('confirmPassword', '123456');

axios.post('/api/system/employee/register', formData, {
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})
.then(response => {
  if (response.data.code === 0) {
    console.log('注册成功', response.data.data);
  }
})
.catch(error => {
  console.error('注册失败', error.response.data.message);
});
```

#### 错误码说明

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如：字段验证失败、文件格式不正确等） |
| 607 | 数据已存在（如：手机号已被注册） |
| 608 | 操作失败（如：文件上传失败） |

#### 注意事项

1. 文件上传必须使用 `multipart/form-data` 格式
2. 身份证照片仅支持 JPG/PNG 格式，单个文件不超过 5MB
3. 手机号码会自动作为登录账号使用
4. 注册成功后，数据状态为"待审核"，需要管理员审核后才能登录
5. 文件保存在本地磁盘（当前配置为 `C:/erp`，可在 `file.storage.local.path` 调整），按业务类型和日期分类存储

### 员工注册信息分页查询接口

#### 接口信息

- **接口路径**: `/api/system/employee/registrations`
- **请求方法**: `GET`
- **数据来源**: `EMPLOYEE_REGISTRATION` 表（员工审核数据）

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Long | 否 | 当前页码，默认第1页 |
| size | Long | 否 | 每页数量，默认10条 |
| employeeName | String | 否 | 员工姓名（模糊搜索） |
| department | String | 否 | 部门（模糊搜索） |
| jobTitle | String | 否 | 岗位（模糊搜索） |
| phone | String | 否 | 手机号码（模糊搜索） |
| loginAccount | String | 否 | 登录账号（模糊搜索） |
| auditStatus | String | 否 | 审核状态（精确匹配：待审核/已通过/已拒绝） |
| permissionStatus | String | 否 | 权限分配状态（精确匹配：待分配/已分配） |

#### 响应格式

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "registrationId": 1,
        "employeeName": "张三",
        "department": "市场部",
        "jobTitle": "业务员",
        "phone": "13800138000",
        "idCard": "110101199001011234",
        "loginAccount": "13800138000",
        "auditStatus": "待审核",
        "permissionStatus": "待分配",
        "submitTime": "2025-01-01T12:00:00",
        "auditTime": null,
        "auditorId": null,
        "auditOpinion": null,
        "assignerId": null,
        "assignCompleteTime": null,
        "employeeId": null,
        "idCardFrontFileUrl": "http://localhost:8080/api/files/2025/01/01/xxx-front.jpg",
        "idCardBackFileUrl": "http://localhost:8080/api/files/2025/01/01/xxx-back.jpg"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

#### 响应字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| registrationId | Integer | 注册编号 |
| employeeName | String | 员工姓名 |
| department | String | 部门 |
| jobTitle | String | 岗位 |
| phone | String | 手机号码 |
| idCard | String | 身份证号码 |
| loginAccount | String | 登录账号 |
| auditStatus | String | 审核状态：待审核/已通过/已拒绝 |
| permissionStatus | String | 权限分配状态：待分配/已分配 |
| submitTime | DateTime | 提交时间 |
| auditTime | DateTime | 审核时间 |
| auditorId | Integer | 审核人编码 |
| auditOpinion | String | 审核意见 |
| assignerId | Integer | 分配人编码 |
| assignCompleteTime | DateTime | 分配完成时间 |
| employeeId | Integer | 转员工编码（审核通过后生成） |
| idCardFrontFileUrl | String | 身份证正面照片URL |
| idCardBackFileUrl | String | 身份证反面照片URL |

### 正式员工列表分页查询接口

#### 接口信息

- **接口路径**: `/api/system/employee/list`
- **请求方法**: `GET`
- **数据来源**: `EMPLOYEE` 表（正式员工）

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Long | 否 | 当前页码，默认第1页 |
| size | Long | 否 | 每页数量，默认10条 |
| employeeName | String | 否 | 员工姓名（模糊搜索） |
| department | String | 否 | 部门（模糊搜索） |
| jobTitle | String | 否 | 岗位（模糊搜索） |
| phone | String | 否 | 手机号码（模糊搜索） |
| loginAccount | String | 否 | 登录账号（模糊搜索） |
| employeeStatus | String | 否 | 员工状态（精确匹配：在职/离职/停用等） |

#### 响应格式

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "employeeId": 1,
        "employeeName": "张三",
        "department": "市场部",
        "jobTitle": "业务员",
        "phone": "13800138000",
        "loginAccount": "13800138000",
        "employeeStatus": "在职",
        "createTime": "2025-01-01T12:00:00",
        "updateTime": "2025-01-10T08:00:00",
        "remark": "核心客户维护"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

#### 注意事项

1. 该接口直接从 `EMPLOYEE` 表读取正式员工数据
2. `employeeStatus` 参数与 `EMPLOYEE` 表的 `员工状态` 字段一致，可传在职/离职/停用等值
3. 返回的 `employeeId` 为 EMPLOYEE 表的员工编码
4. `createTime`、`updateTime` 分别对应 EMPLOYEE 表的创建时间、更新时间
5. `remark` 对应 EMPLOYEE 表的备注字段

### 新增正式员工接口

#### 接口信息

- **接口路径**: `/api/system/employee`
- **请求方法**: `POST`
- **Content-Type**: `application/json`

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeName | String | 是 | 员工姓名 |
| department | String | 是 | 部门 |
| jobTitle | String | 是 | 岗位 |
| phone | String | 是 | 手机号码，作为登录账号 |
| password | String | 是 | 登录密码 |
| confirmPassword | String | 是 | 确认密码 |
| roleIds | List<Integer> | 否 | 角色ID集合 |

#### 响应示例

```json
{
  "code": 200,
  "message": "新增员工成功",
  "data": {
    "employeeId": 1001,
    "employeeName": "李四",
    "loginAccount": "13900001111"
  }
}
```

#### 说明

1. 手机号会同时作为登录账号，必须唯一
2. 密码入库前通过 BCrypt 加密
3. 如传入角色ID，会自动写入 `EMPLOYEE_ROLE` 关系表
4. 接口主要用于管理员直接创建正式员工，不经过注册审核流程

### 编辑正式员工接口

#### 接口信息

- **接口路径**: `/api/system/employee/{employeeId}`
- **请求方法**: `PUT`
- **Content-Type**: `application/json`
- **数据来源**: `EMPLOYEE` 表（同步更新 `EMPLOYEE_ROLE` 关系表）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeId | Integer | 是 | 需要编辑的正式员工ID（`EMPLOYEE`.`员工编码`） |

#### 请求体参数

| 参数名 | 类型 | 必填 | 校验/说明 |
|--------|------|------|-----------|
| employeeName | String | 是 | 不能为空 |
| department | String | 是 | 不能为空 |
| jobTitle | String | 是 | 不能为空 |
| phone | String | 是 | 11位手机号格式，修改后同步为登录账号并校验唯一 |
| employeeStatus | String | 是 | 在职/离职/停用 |
| roleIds | List<Integer> | 否 | 角色ID集合；传入 `null` 表示不变，传入空集合表示清空角色 |
| remark | String | 否 | 备注信息 |

#### 响应示例

```json
{
  "code": 200,
  "message": "更新员工成功",
  "data": null
}
```

#### 业务逻辑

1. 根据 `employeeId` 查询正式员工，未找到抛出“员工不存在”。
2. 当手机号/登录账号发生变化时，调用 `EmployeeMapper.selectByLoginAccount` 校验唯一性，避免重复。
3. 更新 `EMPLOYEE` 表字段：姓名、部门、岗位、手机号、登录账号、员工状态、备注、更新时间。
4. 如果 `roleIds` 为 `null`，保持原角色不变；否则先删除员工现有关联，再根据列表批量写入 `EMPLOYEE_ROLE`。
5. 整个过程被 `@Transactional` 管理，任一环节失败都会回滚。

#### 注意事项

1. 手机号和登录账号保持一致，确保前后端展示与登录一致性。
2. 角色列表传空集合即可清空角色；不想修改角色时，请不要传递该字段或传 `null`。
3. 接口返回 `data: null`，只需前端提示“更新员工成功”即可。

### 重置员工密码接口

#### 接口信息

- **接口路径**: `/api/system/employee/{employeeId}/password`
- **请求方法**: `PUT`
- **Content-Type**: `application/json`

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeId | Integer | 是 | 员工ID |

#### 请求体参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| newPassword | String | 是 | 新密码（6-32位） |
| confirmPassword | String | 是 | 确认密码（需与新密码一致） |

#### 请求示例

```json
{
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "重置密码成功",
  "data": null
}
```

#### 业务逻辑

1. 验证两次输入的密码是否一致
2. 根据 `employeeId` 查询员工，不存在则抛出异常
3. 使用 BCrypt 加密新密码
4. 更新 `EMPLOYEE` 表的密码字段和更新时间

### 审核通过员工注册接口

#### 接口信息

- **接口路径**: `/api/system/employee/registration/{registrationId}/approve`
- **请求方法**: `POST`
- **Content-Type**: `application/json`

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| registrationId | Integer | 是 | 注册编号（`EMPLOYEE_REGISTRATION`.`注册编号`） |

#### 响应示例

**成功响应**
```json
{
  "code": 200,
  "message": "审核通过，员工账号已创建",
  "data": {
    "employeeId": 1001,
    "employeeName": "张三",
    "loginAccount": "13800138000"
  }
}
```

**失败响应**
```json
{
  "code": 400,
  "message": "该注册申请已审核通过，请勿重复操作",
  "data": null
}
```

#### 响应字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| employeeId | Integer | 新创建的正式员工编号 |
| employeeName | String | 员工姓名 |
| loginAccount | String | 登录账号 |

#### 业务逻辑

1. 根据 `registrationId` 查询注册信息，不存在则抛出"注册信息不存在"
2. 检查审核状态：
   - 如果已通过，抛出"该注册申请已审核通过，请勿重复操作"
   - 如果已拒绝，抛出"该注册申请已被拒绝，无法通过审核"
3. 检查登录账号是否已存在于 `EMPLOYEE` 表，存在则抛出"该登录账号已存在正式员工"
4. 创建正式员工：
   - 将注册信息（姓名、部门、岗位、手机、登录账号、密码）复制到 `EMPLOYEE` 表
   - 员工状态设为"在职"
   - 密码直接复用注册时已加密的密码
5. 更新注册信息：
   - 审核状态改为"已通过"
   - 记录审核时间
   - 关联新创建的员工编码（`转员工编码`字段）
6. 返回新创建的员工信息

#### 注意事项

1. 该接口需要管理员权限
2. 审核通过后，员工可使用注册时的手机号和密码登录系统
3. 同一注册申请只能审核通过一次
4. 整个操作在一个事务中完成，任一环节失败都会回滚

### 驳回员工注册接口

#### 接口信息

- **接口路径**: `/api/system/employee/registration/{registrationId}/reject`
- **请求方法**: `POST`
- **Content-Type**: `application/json`

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| registrationId | Integer | 是 | 注册编号（`EMPLOYEE_REGISTRATION`.`注册编号`） |

#### 响应示例

```json
{
  "code": 200,
  "message": "已驳回该注册申请",
  "data": null
}
```

#### 业务逻辑

1. 根据 `registrationId` 查询注册信息，不存在则抛出“注册信息不存在”
2. 状态校验：
   - 若已为“已拒绝”，提示“该注册申请已被驳回”
   - 若已为“已通过”，提示“该注册申请已通过，无需重复操作”
3. 更新注册信息：
   - `审核状态` 改为“已拒绝”
   - 清空 `转员工编码`
   - 记录 `审核时间`
4. 返回提示消息“已驳回该注册申请”

#### 注意事项

1. 驳回操作同样需要管理员权限
2. 已通过的注册申请不可驳回
3. 操作在事务中执行，若更新注册信息失败会整体回滚

### 邮件通道配置接口

#### 获取SMTP配置

- **接口路径**: `GET /api/email-channel/config`
- **数据来源**: `EMAIL_CHANNEL_CONFIG`（若无记录返回默认模板）

| 字段 | 说明 |
|------|------|
| displayName | 显示名称 |
| fromAddress | 默认发件地址 |
| replyTo | 回复地址（可为空） |
| smtpHost | SMTP 服务器地址 |
| smtpPort | 端口，默认 465 |
| authMethod | LOGIN/PLAIN/CRAM_MD5 |
| username | 登录账号 |
| encryption | SSL/STARTTLS/PLAIN |
| status | ENABLED/DISABLED |
| maxPerHour | 每小时最大发送量 |
| maxPerDay | 每日最大发送量 |
| hasPassword | 布尔值，仅指示授权码是否已配置 |
| updatedAt/updatedBy | 最近更新时间与维护人编码 |
| lastSelfTestTime | 最近一次自检时间 |

> 该接口启用了 5 分钟 Redis 缓存（键：`sys:email_channel_config`），保存接口会主动刷新缓存。

#### 保存SMTP配置

- **接口路径**: `POST /api/email-channel/config`
- **Content-Type**: `application/json`
- **权限建议**: `email-channel:edit`

| 字段 | 说明 |
|------|------|
| displayName | 必填，≤50 |
| fromAddress | 必填，邮箱格式 |
| replyTo | 选填，邮箱格式 |
| smtpHost | 必填 |
| smtpPort | 必填，1-65535 |
| authMethod | 必填，LOGIN/PLAIN/CRAM_MD5 |
| username | 必填，≤150 |
| password | 选填，8-64；启用状态且无历史密文时必填 |
| encryption | 必填，SSL/STARTTLS/PLAIN |
| status | 必填，ENABLED/DISABLED |
| maxPerHour | 必填，1-10000 |
| maxPerDay | 必填，1-100000，且必须 ≥ `maxPerHour` |

**业务规则**

1. 授权码使用 `email.channel.aes-key`（32位）进行 AES-256 加密后入库，仅返回 `hasPassword`。
2. 保存成功会写入/更新 `EMAIL_CHANNEL_CONFIG` 单行记录，并记录维护人（当前登录 `employeeId`）与时间戳。
3. 启用状态必须存在授权码密文。
4. 保存成功后刷新 Redis 缓存，并触发后续模块监听（`EmailConfigChangedEvent` 预留钩子）。

**示例请求**

```json
{
  "displayName": "ERP告警邮箱",
  "fromAddress": "alert@erp.com",
  "replyTo": "noc@erp.com",
  "smtpHost": "smtp.qq.com",
  "smtpPort": 465,
  "authMethod": "LOGIN",
  "username": "alert@erp.com",
  "password": "abcd1234",
  "encryption": "SSL",
  "status": "ENABLED",
  "maxPerHour": 200,
  "maxPerDay": 2000
}
```

#### 发送测试邮件

- **接口路径**: `POST /api/email-channel/test-send`
- **场景**: 邮件通道自检

| 字段 | 说明 |
|------|------|
| targetEmail | 目标邮箱，必填 |
| subject | 主题，必填，≤80 |
| message | 正文，必填，≤500 |

**响应示例**

```json
{
  "code": 200,
  "message": "测试邮件已发送",
  "data": {
    "trackingId": "c5a3d4c8c44f4b0b8b1a90a88482b5d3",
    "status": "SENT",
    "queuedAt": "2025-01-02T15:30:00",
    "targetEmail": "ops@example.com"
  }
}
```

**注意事项**

1. 自检前必须保存且启用 SMTP 配置，否则接口会返回业务异常。
2. 授权码密文会在内存中解密后调用 Hutool `MailUtil` 直连 SMTP，失败时返回错误信息。
3. 成功发送会刷新 `lastSelfTestTime` 并写入维护人。

#### 服务层复用（通用邮件组件）

- 其它业务如需发送邮件，可注入 `EmailChannelService`，调用 `sendEmail(EmailSendRequest request)`。
- `EmailSendRequest` 字段：
  - `toList`：收件人列表（必填，至少1个）
  - `subject`：邮件主题
  - `content`：邮件正文
  - `html`：是否按HTML发送（默认true）
- 返回 `EmailSendResult`，包含 `trackingId`、`status`、`queuedAt`、实际收件人。
- 调用示例：
  ```java
  @Autowired
  private EmailChannelService emailChannelService;
  
  EmailSendRequest sendRequest = new EmailSendRequest();
  sendRequest.setToList(Arrays.asList("ops@example.com"));
  sendRequest.setSubject("审批提醒");
  sendRequest.setContent("<p>请尽快处理待办事项</p>");
  EmailSendResult result = emailChannelService.sendEmail(sendRequest);
  ```
  若未配置SMTP或通道被禁用，将抛出 `BusinessException`，业务方需捕获并根据 `ResultCodeEnum` 做兜底处理。

---

### 系统配置接口

#### 获取系统配置

- **接口路径**: `GET /api/system/config/{name}`
- **功能描述**: 根据配置名称从 `SYS_CONFIG` 表中读取配置内容
- **需要认证**: 是

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 配置名称，例如：UNIT、OUT_OF_SCOPE_SERVICES |

**响应示例**

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "configId": 1,
    "configName": "UNIT",
    "configValue": "{\"units\":[\"吨\",\"公斤\",\"桶\",\"个\"]}",
    "remark": "计量单位配置",
    "createTime": "2025-01-01T10:00:00",
    "updateTime": "2025-01-15T12:30:00",
    "creatorId": 1001,
    "updaterId": 1001
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

#### 保存或更新系统配置

- **接口路径**: `POST /api/system/config/{name}`
- **Content-Type**: `application/json`
- **功能描述**: 根据配置名称在 `SYS_CONFIG` 表中保存或更新配置内容
- **需要认证**: 是

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 配置名称 |

**请求参数**

```json
{
  "value": "{\"units\":[\"吨\",\"公斤\",\"桶\",\"个\"]}",
  "remark": "计量单位配置"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| value | String | 是 | 配置内容（JSON 或 文本） |
| remark | String | 否 | 备注说明 |

**响应示例**

```json
{
  "code": 200,
  "message": "保存成功",
  "data": {
    "configId": 1,
    "configName": "UNIT",
    "configValue": "{\"units\":[\"吨\",\"公斤\",\"桶\",\"个\"]}",
    "remark": "计量单位配置",
    "updateTime": "2025-01-15T12:30:00",
    "updaterId": 1001
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

**业务逻辑**

1. 如果配置不存在，则创建新配置记录
2. 如果配置已存在，则更新配置内容和备注
3. 自动记录维护人（当前登录 `employeeId`）和更新时间
4. 配置值可以是 JSON 格式或纯文本格式

#### 批量获取系统配置

- **接口路径**: `POST /api/system/config/batch`
- **Content-Type**: `application/json`
- **功能描述**: 根据配置名称列表批量从 `SYS_CONFIG` 表中读取配置内容
- **需要认证**: 是

**请求参数**

```json
["UNIT", "OUT_OF_SCOPE_SERVICES", "INVOICE_COMPANY_INFO"]
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| - | Array<String> | 是 | 配置名称列表 |

**响应示例**

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "UNIT": {
      "configId": 1,
      "configName": "UNIT",
      "configValue": "{\"units\":[\"吨\",\"公斤\",\"桶\",\"个\"]}",
      "remark": "计量单位配置"
    },
    "OUT_OF_SCOPE_SERVICES": {
      "configId": 2,
      "configName": "OUT_OF_SCOPE_SERVICES",
      "configValue": "{\"services\":[]}",
      "remark": "价外服务配置"
    }
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

**业务逻辑**

1. 根据配置名称列表批量查询配置
2. 返回一个Map，key为配置名称，value为配置对象
3. 如果某个配置不存在，则不会出现在返回结果中

---

## 价外服务管理 API

**接口名称：** 价外服务管理服务
**功能描述：** 提供价外服务（OutOfScopeService）的管理功能，支持为报价单和合同添加、查询、更新、删除价外服务项目。

---

### 查询指定报价单的价外服务列表

**接口地址：** `GET /api/quotation/{quotationId}/out-of-scope-services`
**请求方式：** GET
**功能描述：** 查询指定报价单关联的所有价外服务列表

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| quotationId | Integer | 是 | 报价单ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "businessType": "QUOTATION",
      "businessId": 1001,
      "project": "设备维护",
      "spec": "专业维护服务",
      "unit": "项",
      "plannedQuantity": 1.0,
      "contractUnitPrice": 5000.00,
      "status": "ACTIVE",
      "createdAt": "2025-01-01T10:00:00",
      "createdBy": 1001
    }
  ],
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 为报价单新增价外服务

**接口地址：** `POST /api/quotation/{quotationId}/out-of-scope-service`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 为指定报价单批量新增价外服务项目

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| quotationId | Integer | 是 | 报价单ID |

#### 请求参数

```json
[
  {
    "project": "设备维护",
    "serviceType": "维护服务",
    "spec": "专业维护服务",
    "unit": "项",
    "plannedQuantity": 1.0,
    "contractUnitPrice": 5000.00
  }
]
```

#### 响应示例

```json
{
  "code": 200,
  "message": "新增成功",
  "data": [
    {
      "id": 1,
      "businessType": "QUOTATION",
      "businessId": 1001,
      "project": "设备维护",
      "spec": "专业维护服务",
      "unit": "项",
      "plannedQuantity": 1.0,
      "contractUnitPrice": 5000.00,
      "status": "ACTIVE",
      "createdAt": "2025-01-01T10:00:00",
      "createdBy": 1001
    }
  ],
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 查询指定合同的价外服务列表

**接口地址：** `GET /api/contract/{contractId}/out-of-scope-services`
**请求方式：** GET
**功能描述：** 查询指定合同关联的所有价外服务列表

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractId | Integer | 是 | 合同ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 2,
      "businessType": "CONTRACT",
      "businessId": 2001,
      "project": "技术培训",
      "spec": "系统操作培训",
      "unit": "人次",
      "plannedQuantity": 5.0,
      "contractUnitPrice": 800.00,
      "status": "ACTIVE",
      "createdAt": "2025-01-02T10:00:00",
      "createdBy": 1001
    }
  ],
  "timestamp": "2025-01-02T12:00:00"
}
```

---

### 为合同新增价外服务

**接口地址：** `POST /api/contract/{contractId}/out-of-scope-service`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 为指定合同批量新增价外服务项目

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractId | Integer | 是 | 合同ID |

#### 请求参数

```json
[
  {
    "project": "技术培训",
    "serviceType": "培训服务",
    "spec": "系统操作培训",
    "unit": "人次",
    "plannedQuantity": 5.0,
    "contractUnitPrice": 800.00
  }
]
```

---

### 更新价外服务

**接口地址：** `PUT /api/out-of-scope-service/{id}`
**请求方式：** PUT
**Content-Type：** `application/json`
**功能描述：** 更新指定价外服务的信息

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 价外服务ID |

#### 请求参数

```json
{
  "project": "设备维护升级",
  "spec": "专业维护服务升级版",
  "unit": "项",
  "plannedQuantity": 2.0,
  "contractUnitPrice": 6000.00
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1,
    "businessType": "QUOTATION",
    "businessId": 1001,
    "project": "设备维护升级",
    "spec": "专业维护服务升级版",
    "unit": "项",
    "plannedQuantity": 2.0,
    "contractUnitPrice": 6000.00,
    "status": "ACTIVE",
    "createdAt": "2025-01-01T10:00:00",
    "createdBy": 1001,
    "updatedAt": "2025-01-03T10:00:00",
    "updatedBy": 1001
  },
  "timestamp": "2025-01-03T12:00:00"
}
```

---

### 删除价外服务

**接口地址：** `DELETE /api/out-of-scope-service/{id}`
**请求方式：** DELETE
**功能描述：** 删除指定的价外服务记录

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 价外服务ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null,
  "timestamp": "2025-01-03T12:00:00"
}
```

---

## 权限管理 API

**接口名称：** 权限管理服务
**功能描述：** 提供权限定义的管理功能，支持权限树构建、模块-页面关联管理等。（字段级权限相关接口已移除）

---

### 权限分页查询

**接口地址：** `GET /api/permission/list`
**请求方式：** GET
**功能描述：** 分页查询权限列表，支持按权限类型ID、权限名称、权限编码、父权限ID筛选

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |
| permissionTypeId | Integer | 否 | 权限类型ID |
| permissionName | String | 否 | 权限名称（模糊查询） |
| permissionCode | String | 否 | 权限编码（模糊查询） |
| parentId | Integer | 否 | 父权限ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "permissionId": 1,
        "permissionCode": "customer:view",
        "permissionName": "客户查看",
        "permissionType": "MENU",
        "parentId": null,
        "sortOrder": 1,
        "status": "ENABLED",
        "remark": "查看客户信息权限",
        "createdAt": "2025-01-01T10:00:00"
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 获取权限树

**接口地址：** `GET /api/permission/tree`
**请求方式：** GET
**功能描述：** 构建并返回权限树结构

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "permissionId": 1,
      "permissionCode": "system",
      "permissionName": "系统管理",
      "permissionType": "MODULE",
      "parentId": null,
      "sortOrder": 1,
      "children": [
        {
          "permissionId": 2,
          "permissionCode": "user:manage",
          "permissionName": "用户管理",
          "permissionType": "MENU",
          "parentId": 1,
          "sortOrder": 1,
          "children": []
        }
      ]
    }
  ],
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 获取权限详情

**接口地址：** `GET /api/permission/{id}`
**请求方式：** GET
**功能描述：** 根据权限ID获取权限详细信息

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 权限ID |

---

### 创建权限

**接口地址：** `POST /api/permission`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 创建新的权限记录

#### 请求参数

```json
{
  "permissionCode": "customer:create",
  "permissionName": "创建客户",
  "permissionType": "BUTTON",
  "parentId": 1,
  "sortOrder": 1,
  "status": "ENABLED",
  "remark": "允许创建客户信息"
}
```

---

### 更新权限

**接口地址：** `PUT /api/permission/{id}`
**请求方式：** PUT
**Content-Type：** `application/json`
**功能描述：** 更新指定权限的信息

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 权限ID |

---

### 删除权限

**接口地址：** `DELETE /api/permission/{id}`
**请求方式：** DELETE
**功能描述：** 删除指定的权限记录

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 权限ID |

---

### 模块下新增页面权限

**接口地址：** `POST /api/permission/{moduleId}/pages`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 在指定模块下批量新增页面权限

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| moduleId | Integer | 是 | 模块ID |

#### 请求参数

```json
[
  {
    "permissionCode": "customer:list",
    "permissionName": "客户列表",
    "permissionType": "MENU",
    "sortOrder": 1,
    "status": "ENABLED"
  }
]
```

---

### 从模块中移除页面

**接口地址：** `DELETE /api/permission/{moduleId}/pages/{pageId}`
**请求方式：** DELETE
**功能描述：** 从指定模块中移除页面权限，支持确认删除

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| moduleId | Integer | 是 | 模块ID |
| pageId | Integer | 是 | 页面ID |

#### 查询参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| confirm | Boolean | 否 | 是否确认删除，默认false |

---

### 批量更新模块页面关联关系

**接口地址：** `POST /api/permission/{moduleId}/pages/batch-update`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 批量更新模块与页面的关联关系

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| moduleId | Integer | 是 | 模块ID |

#### 请求参数

```json
{
  "addedIds": ["page1", "page2"],
  "removedIds": ["page3"]
}
```

---

## 角色管理 API

**接口名称：** 角色管理服务
**功能描述：** 提供角色的创建、更新、删除、权限分配、成员管理等功能。

---

### 查询角色列表

**接口地址：** `GET /api/system/roles`
**请求方式：** GET
**功能描述：** 分页查询角色列表（当前简化实现返回全部角色）

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "roleId": 1,
      "roleCode": "ADMIN",
      "roleName": "管理员",
      "roleDescription": "系统管理员",
      "status": "ENABLED",
      "createdAt": "2025-01-01T10:00:00",
      "updatedAt": "2025-01-01T10:00:00"
    }
  ],
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 创建角色

**接口地址：** `POST /api/system/roles`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 创建新的角色，可同时分配权限

#### 请求参数

```json
{
  "roleCode": "SALES_MANAGER",
  "roleName": "销售经理",
  "roleDescription": "负责销售团队管理",
  "status": "ENABLED"
}
```

#### 查询参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| permissionCodes | List<String> | 否 | 权限编码列表 |

---

### 更新角色

**接口地址：** `PUT /api/system/roles/{id}`
**请求方式：** PUT
**Content-Type：** `application/json`
**功能描述：** 更新指定角色的信息

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 角色ID |

#### 请求参数

```json
{
  "roleName": "高级销售经理",
  "roleDescription": "负责高级销售团队管理",
  "status": "ENABLED"
}
```

---

### 删除角色

**接口地址：** `DELETE /api/system/roles/{id}`
**请求方式：** DELETE
**功能描述：** 删除指定的角色，返回受影响员工数量并支持二次确认

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 角色ID |

#### 查询参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| confirm | Boolean | 否 | 是否确认删除，默认false |

#### 响应示例

```json
{
  "code": 200,
  "message": "删除成功",
  "data": {
    "affectedEmployees": 5,
    "confirmRequired": true
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 为角色设置权限

**接口地址：** `POST /api/system/roles/{id}/permissions`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 为指定角色批量设置权限

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 角色ID |

#### 请求参数

```json
[1, 2, 3, 4]
```

---

### 获取角色的权限列表

**接口地址：** `GET /api/system/roles/{id}/permissions`
**请求方式：** GET
**功能描述：** 获取指定角色拥有的权限ID列表

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 角色ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [1, 2, 3, 4, 5],
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 获取角色成员

**接口地址：** `GET /api/system/roles/{id}/members`
**请求方式：** GET
**功能描述：** 获取指定角色的成员列表（当前简化实现返回全部成员ID）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 角色ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [1001, 1002, 1003],
  "timestamp": "2025-01-01T12:00:00"
}
```

---

## 系统管理 API

### 新增员工信息

**接口地址：** `POST /api/system/employee`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 直接向EMPLOYEE表写入员工信息，支持绑定角色

#### 请求参数

```json
{
  "loginAccount": "13800138000",
  "employeeName": "张三",
  "department": "市场部",
  "jobTitle": "业务员",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "employeeStatus": "在职",
  "roleIds": [1, 2]
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "新增员工成功",
  "data": {
    "employeeId": 1001,
    "loginAccount": "13800138000",
    "employeeName": "张三"
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 编辑员工信息

**接口地址：** `PUT /api/system/employee/{employeeId}`
**请求方式：** PUT
**Content-Type：** `application/json`
**功能描述：** 更新EMPLOYEE表并同步角色信息

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeId | Integer | 是 | 员工ID |

#### 请求参数

```json
{
  "employeeName": "张三（修改）",
  "department": "销售部",
  "jobTitle": "销售经理",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "employeeStatus": "在职",
  "roleIds": [2, 3]
}
```

---

### 重置员工密码

**接口地址：** `PUT /api/system/employee/{employeeId}/password`
**请求方式：** PUT
**Content-Type：** `application/json`
**功能描述：** 重置指定员工的登录密码

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeId | Integer | 是 | 员工ID |

#### 请求参数

```json
{
  "newPassword": "Abc123456"
}
```

---

### 获取员工最终权限视图

**接口地址：** `GET /api/system/employees/{employeeId}/permissions`
**请求方式：** GET
**功能描述：** 返回员工最终权限视图（含员工层 ALLOW/DENY 覆盖）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeId | Integer | 是 | 员工ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "userPermissions": ["customer:view", "customer:add"]
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 设置员工显式权限覆盖

**接口地址：** `POST /api/system/employees/{employeeId}/permissions`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 设置员工显式权限覆盖（ALLOW/DENY）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| employeeId | Integer | 是 | 员工ID |

#### 请求参数

```json
[
  {
    "permissionId": 1,
    "permissionType": "ALLOW"
  },
  {
    "permissionId": 2,
    "permissionType": "DENY"
  }
]
```

---

### 获取系统配置

**接口地址：** `GET /api/system/config/{name}`
**请求方式：** GET
**功能描述：** 根据配置名称从SYS_CONFIG表中读取配置内容

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 配置名称，例如：UNIT、OUT_OF_SCOPE_SERVICES |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "name": "UNIT",
    "value": "[\"kg\",\"t\",\"m³\"]",
    "remark": "计量单位配置",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T10:00:00"
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

---

### 保存系统配置

**接口地址：** `POST /api/system/config/{name}`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 根据配置名称在SYS_CONFIG表中保存或更新配置内容

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 配置名称 |

#### 请求参数

```json
{
  "value": "[\"kg\",\"t\",\"m³\",\"L\"]",
  "remark": "计量单位配置，新增升(L)"
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "保存成功",
  "data": {
    "name": "UNIT",
    "value": "[\"kg\",\"t\",\"m³\",\"L\"]",
    "remark": "计量单位配置，新增升(L)",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-01T12:00:00"
  },
  "timestamp": "2025-01-01T12:00:00"
}
```

---

## 消息通知管理 API

**接口名称：** 消息通知服务  
**功能描述：** 提供消息中心的分页查询、详情、未读数量、统计、标记已读、批量操作等能力，并通过RabbitMQ异步投递预警/业务/系统消息，所有消息最终持久化到 `MESSAGE` 表供前端轮询展示。

---

### 消息列表分页查询

**接口地址：** `GET /api/message/list`  
**功能描述：** 按消息类型、状态、优先级与关键词分页筛选当前登录人的消息，默认按照优先级（紧急→低）+创建时间倒序排序。

#### 请求参数

```json
{
  "current": 1,
  "size": 10,
  "messageType": "预警消息",
  "messageStatus": "未读",
  "messagePriority": "紧急",
  "keyword": "合同",
  "startTime": "2025-11-01 00:00:00",
  "endTime": "2025-12-02 23:59:59"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 是 | 当前页码（≥1） |
| size | Integer | 是 | 每页数量（≥1） |
| messageType | String | 否 | 消息类型：全部/预警消息/业务通知/系统消息（传全部或空表示不过滤） |
| messageStatus | String | 否 | 消息状态：全部/未读/已读 |
| messagePriority | String | 否 | 消息优先级：全部/紧急/高/中/低 |
| keyword | String | 否 | 标题或内容模糊匹配 |
| startTime | String | 否 | 创建时间起（预留字段，当前后端未启用） |
| endTime | String | 否 | 创建时间止（预留字段，当前后端未启用） |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "messageId": 1001,
        "messageType": "预警消息",
        "messageCategory": "合同",
        "messageTitle": "合同《HW-2025-001》将在7天后到期",
        "messageContent": "合同《HW-2025-001》将在7天后到期，请及时续签……",
        "messagePriority": "紧急",
        "senderName": "系统预警服务",
        "businessType": "CONTRACT",
        "businessId": 2001,
        "messageStatus": "未读",
        "createTime": "2025-12-01 09:00:00",
        "readTime": null
      }
    ],
    "total": 35,
    "size": 10,
    "current": 1,
    "pages": 4
  },
  "timestamp": "2025-12-02T10:00:00"
}
```

### 系统前缀别名

系统管理模块同时提供等价接口，路径为 `/api/system/email-channel/*`，能力与 `/api/email-channel/*` 一致：
- `GET /api/system/email-channel/config`
- `POST /api/system/email-channel/config`
- `POST /api/system/email-channel/test-send`

---

### 消息详情

**接口地址：** `GET /api/message/{messageId}`  
**功能描述：** 返回消息完整信息（标题、正文、优先级、发送/接收人、关联业务、创建/读取时间等），首次查看会自动标记为已读。

---

### 标记消息为已读

**接口地址：** `PUT /api/message/{messageId}/read`  
**功能描述：** 将指定消息标记为已读并记录 `readTime`，若消息已读会直接返回成功。

---

### 批量标记消息为已读

**接口地址：** `PUT /api/message/batch-read`  
**请求体：**

```json
[1001, 1002, 1003]
```

**功能描述：** 批量将当前登录人的未读消息更新为已读，接口内部自动忽略非本人或已读消息。

---

### 删除消息（软删除）

**接口地址：** `DELETE /api/message/{messageId}`  
**功能描述：** 将消息状态改为“已删除”并写入 `deleteTime`，数据仍可追溯。

---

### 批量删除消息

**接口地址：** `DELETE /api/message/batch`  
**请求体：**

```json
[1201, 1202]
```

**功能描述：** 批量软删除消息，仅影响当前登录用户。

---

### 获取未读消息数量

**接口地址：** `GET /api/message/unread-count`  
**功能描述：** 返回当前登录人的未读消息数量，用于前端角标。

---

### 获取消息统计信息

**接口地址：** `GET /api/message/statistics`  
**响应示例：**

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "unreadCount": 6,
    "totalCount": 128,
    "typeStatistics": {
      "预警消息": 30,
      "业务通知": 70,
      "系统消息": 28
    },
    "unreadTypeStatistics": {
      "预警消息": 3,
      "业务通知": 2,
      "系统消息": 1
    },
    "priorityStatistics": {
      "紧急": 5,
      "高": 12,
      "中": 48,
      "低": 63
    },
    "statusStatistics": {
      "未读": 6,
      "已读": 118,
      "已删除": 4
    }
  },
  "timestamp": "2025-12-02T10:05:00"
}
```

---

### 全部标记为已读

**接口地址：** `PUT /api/message/mark-all-read`  
**功能描述：** 一次性把当前用户的所有未读消息更新为已读。

---

### 清空所有消息

**接口地址：** `DELETE /api/message/clear-all`  
**功能描述：** 将当前用户的消息批量软删除，常用于历史归档清理。

---

### 消息发送与消费机制

- **消息发送：** `MessageNotificationService` 提供 `sendAlert`、`sendBusinessNotification`、`sendSystemMessage`、`sendBatchAlert`、`sendContractExpiryAlert`、`sendOverduePaymentAlert`、`sendAuditNotification` 等方法，封装 `MessageDTO` 构建，默认投递到 RabbitMQ；当 RabbitMQ 不可用时自动降级为直接调用 `MessageService.processMessage` 写库。
- **队列配置：** `RabbitMQConfig` 定义 `erp.alert.exchange/queue`、`erp.business.exchange/queue`、`erp.notification.exchange/queue`、`erp.system.exchange/queue` 四组交换机与队列，统一使用 JSON 序列化。
- **消息消费：** `MessageConsumerService` 监听上述队列，调用 `MessageService` 将消息持久化到 `MESSAGE` 表，并在处理紧急/高优先级预警时预留扩展钩子（例如邮件/SMS二次通知）。

---

## 危险废物名录管理 API

**接口名称：** 危险废物名录管理服务  
**功能描述：** 提供危险废物名录的增删改查、分页查询、引用统计、Excel 批量导入导出及模板下载功能。  
**接口地址：** `/api/waste-code/*`  
**请求方式：** GET / POST / PUT / DELETE

---

### 分页查询危险废物名录

**接口地址：** `/api/waste-code/list`  
**请求方式：** GET  
**功能描述：** 支持按关键字、危险特性、废物类别、行业来源等条件分页查询危废条目，返回引用统计信息。

#### 请求参数

```json
{
  "current": 1,
  "size": 10,
  "keyword": "HW01",
  "wasteCode": "841-001",
  "wasteName": "感染性",
  "hazardCharacteristic": "IN",
  "wasteCategory": "HW01",
  "wasteCategoryName": "医疗废物 / 感染性废物",
  "industrySource": "卫生"
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|--------|------|------|------|--------|
| current | Long | 否 | 当前页码（默认1） | 1 |
| size | Long | 否 | 每页数量（默认10） | 20 |
| keyword | String | 否 | 搜索关键词，模糊匹配废物代码/危险废物/废物类别 | HW01 |
| wasteCode | String | 否 | 废物代码（模糊匹配） | 841-001 |
| wasteName | String | 否 | 危险废物名称（模糊匹配） | 感染性 |
| hazardCharacteristic | String | 否 | 危险特性筛选（IN/T/C/I/R） | IN |
| wasteCategory | String | 否 | 废物类别编码筛选（模糊匹配） | HW01 |
| wasteCategoryName | String | 否 | 废物类别名称筛选（模糊匹配） | 医疗废物 / 感染性废物 |
| industrySource | String | 否 | 行业来源筛选（模糊匹配） | 卫生 |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "itemId": 1,
        "wasteCategory": "HW01",
        "wasteCategoryName": "医疗废物 / 感染性废物",
        "industrySource": "卫生",
        "wasteCode": "841-001-01",
        "wasteName": "感染性废物",
        "hazardCharacteristic": "IN",
        "customerCount": 5,
        "quotationCount": 3,
        "warehousingCount": 2,
        "stockCount": 1
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| records[].itemId | Integer | 条目编号 |
| records[].wasteCategory | String | 废物类别编码（如 HW01） |
| records[].wasteCategoryName | String | 废物类别名称（如 医疗废物） |
| records[].industrySource | String | 行业来源 |
| records[].wasteCode | String | 废物代码 |
| records[].wasteName | String | 危险废物名称 |
| records[].hazardCharacteristic | String | 危险特性（IN/T/C/I/R，多个用/分隔） |
| records[].customerCount | Long | 客户引用数量 |
| records[].quotationCount | Long | 报价单引用数量 |
| records[].warehousingCount | Long | 入库单引用数量 |
| records[].stockCount | Long | 库存引用数量 |

---

### 获取危险废物名录详情

**接口地址：** `/api/waste-code/{itemId}`  
**请求方式：** GET  
**功能描述：** 根据条目编号获取危废条目详情及业务引用统计。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | Integer | 是 | 危废条目编号 |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "itemId": 1,
    "wasteCategory": "HW01",
    "wasteCategoryName": "医疗废物 / 感染性废物",
    "industrySource": "卫生",
    "wasteCode": "841-001-01",
    "wasteName": "感染性废物",
    "hazardCharacteristic": "IN",
    "customerCount": 5,
    "quotationCount": 3,
    "warehousingCount": 2,
    "stockCount": 1
  }
}
```

---

### 新增危险废物名录

**接口地址：** `/api/waste-code`  
**请求方式：** POST  
**功能描述：** 创建新的危废条目，系统会校验废物代码唯一性。

#### 请求参数

```json
{
  "wasteCategory": "HW01",
  "wasteCategoryName": "医疗废物 / 感染性废物",
  "industrySource": "卫生",
  "wasteCode": "841-001-01",
  "wasteName": "感染性废物",
  "hazardCharacteristic": "IN"
}
```

| 参数名 | 类型 | 必填 | 说明 | 验证规则 |
|--------|------|------|------|----------|
| wasteCategory | String | 是 | 废物类别编码 | 不能为空 |
| wasteCategoryName | String | 是 | 废物类别名称 | 不能为空 |
| industrySource | String | 是 | 行业来源 | 不能为空 |
| wasteCode | String | 是 | 废物代码 | 不能为空，系统内唯一 |
| wasteName | String | 是 | 危险废物名称 | 不能为空 |
| hazardCharacteristic | String | 否 | 危险特性 | 仅支持 IN/T/C/I/R，多个用/分隔，支持中英文逗号、斜杠 |

#### 响应参数

```json
{
  "code": 200,
  "message": "新增成功",
  "data": null
}
```

---

### 更新危险废物名录

**接口地址：** `/api/waste-code/{itemId}`  
**请求方式：** PUT  
**功能描述：** 更新危废条目信息，系统会校验废物代码唯一性（排除自身）。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | Integer | 是 | 危废条目编号 |

#### 请求参数

同"新增危险废物名录"。

#### 响应参数

```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

---

### 删除危险废物名录

**接口地址：** `/api/waste-code/{itemId}`  
**请求方式：** DELETE  
**功能描述：** 删除危废条目，删除前会校验是否被客户、报价单、入库单、库存引用。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | Integer | 是 | 危废条目编号 |

#### 响应参数

**成功响应**
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

**失败响应（被引用）**
```json
{
  "code": 600,
  "message": "该危废条目已被业务数据引用，无法删除",
  "data": null
}
```

---

### 批量导入危险废物名录

**接口地址：** `/api/waste-code/batch-import`  
**请求方式：** POST（multipart/form-data）  
**功能描述：** 上传 Excel 文件批量导入危废条目，系统会校验数据格式、废物代码唯一性及危险特性规范。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | Excel 文件（.xls/.xlsx） |

#### Excel 模板格式

| 列 | 字段 | 说明 |
|----|------|------|
| A | 废物类别 | 必填 |
| B | 行业来源 | 必填 |
| C | 废物代码 | 必填，唯一 |
| D | 危险废物 | 必填 |
| E | 危险特性 | 选填，仅支持 IN/T/C/I/R |

#### 响应参数

```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "totalCount": 10,
    "successCount": 8,
    "failCount": 2,
    "errors": [
      {
        "rowIndex": 3,
        "wasteCode": "841-001-01",
        "message": "废物代码已存在于系统"
      },
      {
        "rowIndex": 7,
        "wasteCode": null,
        "message": "废物类别不能为空"
      }
    ]
  }
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| totalCount | Integer | 处理总行数 |
| successCount | Integer | 成功数量 |
| failCount | Integer | 失败数量 |
| errors | Array | 错误详情列表 |
| errors[].rowIndex | Integer | 错误行号（从1开始） |
| errors[].wasteCode | String | 废物代码（如有） |
| errors[].message | String | 错误信息 |

---

### 导出危险废物名录

**接口地址：** `/api/waste-code/export`  
**请求方式：** GET  
**功能描述：** 根据筛选条件导出危废条目 Excel，包含引用统计信息。

#### 请求参数

与"分页查询危险废物名录"一致，用于指定导出范围。

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `危险废物名录导出.xlsx`

**导出列：** 条目编号、废物类别、废物类别名称、行业来源、废物代码、危险废物、危险特性、客户引用数量、报价单引用数量、入库单引用数量、库存引用数量

---

### 下载导入模板

**接口地址：** `/api/waste-code/export-template`  
**请求方式：** GET  
**功能描述：** 下载危废条目导入模板，包含表头和示例数据。

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `危险废物名录导入模板.xlsx`

**模板列：** 废物类别、废物类别名称、行业来源、废物代码、危险废物、危险特性

## 客户管理 API

**接口名称：** 客户管理服务  
**功能描述：** 提供客户信息的新增、更新、删除、详情、分页查询以及批量导入、导出能力。  
**接口地址：** `/api/customer/*`  
**请求方式：** GET / POST / PUT / DELETE

---

### 分页查询客户

**接口地址：** `/api/customer/list`  
**请求方式：** GET  
**功能描述：** 按企业名称、统一社会信用代码、联系电话、业务员等条件分页查询客户列表。

#### 请求参数

```json
{
  "current": 1,
  "size": 10,
  "enterpriseName": "化工",
  "creditCode": "9111",
  "contactPhone": "138",
  "customerCode": "1",
  "ownerEmployeeId": 1001
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|--------|------|------|------|--------|
| current | Long | 否 | 当前页码（默认1） | 1 |
| size | Long | 否 | 每页数量（默认10） | 20 |
| enterpriseName | String | 否 | 企业名称（模糊匹配） | 化工 |
| creditCode | String | 否 | 统一社会信用代码（模糊匹配） | 9111 |
| contactPhone | String | 否 | 联系电话（模糊匹配） | 138 |
| customerCode | String | 否 | 客户编码（模糊匹配） | 1 |
| ownerEmployeeId | Integer | 否 | 业务员编码（仅管理员生效） | 1001 |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "customerId": 1,
        "enterpriseName": "XX化工有限公司",
        "creditCode": "91110000MA01234567",
        "address": "北京市朝阳区XX路XX号",
        "phone": "010-88888888",
        "legalRepresentative": "李四",
        "contactPerson": "王五",
        "contactPhone": "13800138000",
        "formerNames": "XX化工厂,XX实业公司",
        "ownerEmployeeId": 1001,
        "ownerEmployeeName": "张三",
        "createTime": "2025-01-01T10:00:00",
        "updateTime": "2025-01-15T12:30:00"
      }
    ],
    "total": 120,
    "size": 10,
    "current": 1,
    "pages": 12
  }
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| records[].customerId | Integer | 客户编码 |
| records[].enterpriseName | String | 企业名称 |
| records[].creditCode | String | 统一社会信用代码 |
| records[].address | String | 地址 |
| records[].phone | String | 电话 |
| records[].legalRepresentative | String | 法定代表人 |
| records[].contactPerson | String | 联系人 |
| records[].contactPhone | String | 联系电话 |
| records[].formerNames | String | 曾用名（按最近到最早顺序使用英文逗号分隔） |
| records[].ownerEmployeeId | Integer | 业务员编码 |
| records[].ownerEmployeeName | String | 业务员姓名 |
| records[].createTime | String | 创建时间 |
| records[].updateTime | String | 更新时间 |

---

### 新增客户

**接口地址：** `/api/customer`  
**请求方式：** POST  
**功能描述：** 创建单个客户，业务员编码自动使用当前登录员工编码。

#### 请求参数

```json
{
  "enterpriseName": "XX化工有限公司",
  "creditCode": "91110000MA01234567",
  "address": "北京市朝阳区XX路XX号",
  "phone": "010-88888888",
  "legalRepresentative": "李四",
  "contactPerson": "王五",
  "contactPhone": "13800138000",
  "formerNames": "XX化工厂,XX实业公司",
  "remark": "重点客户"
}
```

| 参数名 | 类型 | 必填 | 说明 | 验证规则 |
|--------|------|------|------|----------|
| enterpriseName | String | 是 | 企业名称 | 不能为空，最大100字符 |
| creditCode | String | 是 | 统一社会信用代码 | 不能为空，最大20字符，系统内唯一 |
| address | String | 否 | 地址 | 最大200字符 |
| phone | String | 否 | 电话 | 最大20字符 |
| legalRepresentative | String | 否 | 法定代表人 | 最大50字符 |
| contactPerson | String | 是 | 联系人 | 不能为空，最大50字符 |
| contactPhone | String | 是 | 联系电话 | 不能为空，最大20字符 |
| formerNames | String | 否 | 曾用名列表 | 最大255字符，英文逗号分隔，最多5条 |
| remark | String | 否 | 备注 | 最大255字符 |

> 说明：业务员编码（`业务员编码`）和创建人编码（`创建人编码`）由系统自动写入为当前登录员工编码，前端无需传入。

#### 响应参数

```json
{
  "code": 200,
  "message": "新增客户成功",
  "data": {
    "customerId": 1,
    "enterpriseName": "XX化工有限公司",
    "creditCode": "91110000MA01234567",
    "address": "北京市朝阳区XX路XX号",
    "phone": "010-88888888",
    "legalRepresentative": "李四",
    "contactPerson": "王五",
    "contactPhone": "13800138000",
    "formerNames": "XX化工厂,XX实业公司",
    "ownerEmployeeId": 1001,
    "ownerEmployeeName": "张三",
    "creatorId": 1001,
    "creatorName": "张三",
    "remark": "重点客户",
    "createTime": "2025-01-01T10:00:00",
    "updateTime": "2025-01-01T10:00:00"
  }
}
```

---

### 更新客户

**接口地址：** `/api/customer/{customerId}`  
**请求方式：** PUT  
**功能描述：** 更新客户基础信息，非管理员仅可编辑自己负责的客户。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户ID |

#### 请求参数

同"新增客户"。

#### 响应参数

```json
{
  "code": 200,
  "message": "更新客户成功",
  "data": null
}
```

---

### 删除客户

**接口地址：** `/api/customer/{customerId}`  
**请求方式：** DELETE  
**功能描述：** 删除客户，仅允许业务员本人或管理员操作。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "删除客户成功",
  "data": null
}
```

---

### 客户详情

**接口地址：** `/api/customer/{customerId}`  
**请求方式：** GET  
**功能描述：** 查询客户详情，普通用户仅能查看自己负责的客户。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "customerId": 1,
    "enterpriseName": "XX化工有限公司",
    "creditCode": "91110000MA01234567",
    "address": "北京市朝阳区XX路XX号",
    "phone": "010-88888888",
    "legalRepresentative": "李四",
    "contactPerson": "王五",
    "contactPhone": "13800138000",
    "formerNames": "XX化工厂,XX实业公司",
    "ownerEmployeeId": 1001,
    "ownerEmployeeName": "张三",
    "creatorId": 1001,
    "creatorName": "张三",
    "remark": "重点客户",
    "createTime": "2025-01-01T10:00:00",
    "updateTime": "2025-01-15T12:30:00"
  }
}
```

---

### 获取客户报价记录

**接口地址：** `/api/customer/{customerId}/quotations`  
**请求方式：** GET  
**功能描述：** 根据客户ID查询该客户的所有报价单记录（分层结构：报价单 → 报价条目 → 危废明细）。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "quotationId": 1,
      "quotationNo": "QT-20250115-00001",
      "quotationStatus": "已通过",
      "createTime": "2025-01-15T10:00:00",
      "items": [
        {
          "quotationItemId": 10,
          "quotationMode": "总价包干",
          "payer": "甲方",
          "pricingPlan": "一次性处置费用50000元",
          "remark": "包含运输和处置",
          "wasteItems": [
            {
              "quotationWasteItemId": 100,
              "hazardousWasteItemId": 1,
              "wasteCategory": "HW01 医疗废物",
              "industrySource": "卫生",
              "wasteCode": "841-001-01",
              "hazardousWaste": "感染性废物",
              "form": "固态",
              "unit": "吨",
              "plannedQuantity": 10.00,
              "payer": "甲方",
              "pricingPlan": "包含运输费",
              "remark": "需冷链"
            }
          ]
        }
      ]
    }
  ]
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| quotationId | Integer | 报价单编号 |
| quotationNo | String | 报价单号 |
| quotationStatus | String | 报价状态 |
| createTime | String | 创建时间 |
| items | Array | 报价条目列表 |
| items[].quotationItemId | Integer | 报价条目编号 |
| items[].quotationMode | String | 报价模式：总价包干/按量结算 |
| items[].payer | String | 付款方：甲方/乙方/共同 |
| items[].pricingPlan | String | 计价方案（总价包干时使用） |
| items[].remark | String | 备注 |
| items[].wasteItems | Array | 危废明细列表 |
| items[].wasteItems[].quotationWasteItemId | Integer | 报价危废明细编号 |
| items[].wasteItems[].hazardousWasteItemId | Integer | 危废条目编号 |
| items[].wasteItems[].wasteCategory | String | 废物类别 |
| items[].wasteItems[].industrySource | String | 行业来源 |
| items[].wasteItems[].wasteCode | String | 废物代码 |
| items[].wasteItems[].hazardousWaste | String | 危险废物名称 |
| items[].wasteItems[].form | String | 形态 |
| items[].wasteItems[].unit | String | 计量单位 |
| items[].wasteItems[].plannedQuantity | BigDecimal | 计划数量 |
| items[].wasteItems[].payer | String | 付款方（按量结算时使用） |
| items[].wasteItems[].pricingPlan | String | 计价方案（按量结算时使用） |
| items[].wasteItems[].remark | String | 备注 |

---

### 获取客户合同记录

**接口地址：** `/api/customer/{customerId}/contracts`  
**请求方式：** GET  
**功能描述：** 根据客户ID查询该客户的所有合同记录。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "contractId": 1,
      "contractAmount": 50000.00,
      "signTime": "2025-01-15T10:00:00",
      "contractStatus": "执行中",
      "validFrom": "2025-01-15T00:00:00",
      "validTo": "2026-01-14T23:59:59",
      "createTime": "2025-01-15T10:30:00"
    }
  ]
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| contractId | Integer | 合同编号 |
| contractAmount | BigDecimal | 合同金额 |
| signTime | String | 签订时间 |
| contractStatus | String | 合同状态 |
| validFrom | String | 合同有效期开始 |
| validTo | String | 合同有效期结束 |
| createTime | String | 创建时间 |

---

### 获取客户跟进记录

**接口地址：** `/api/customer/{customerId}/follows`  
**请求方式：** GET  
**功能描述：** 根据客户ID查询该客户的跟进记录。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户ID |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "followId": 1,
      "followTime": "2025-01-20T14:00:00",
      "followMethod": "电话",
      "followContent": "沟通合同续签事宜",
      "nextPlan": "下周提交新报价单",
      "followerId": 1001,
      "followerName": "张三",
      "createTime": "2025-01-20T14:00:00"
    }
  ]
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| followId | Integer | 跟进记录ID |
| followTime | String | 跟进时间 |
| followMethod | String | 跟进方式 |
| followContent | String | 跟进内容 |
| nextPlan | String | 下一步计划 |
| followerId | Integer | 跟进人编码 |
| followerName | String | 跟进人姓名 |
| createTime | String | 创建时间 |

---

### 批量导入客户

**接口地址：** `/api/customer/import`  
**请求方式：** POST（multipart/form-data）  
**功能描述：** 上传 Excel 文件批量创建客户，系统校验重复信用代码、必填字段及业务员权限。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | Excel 文件（.xls/.xlsx） |

#### Excel 模板格式

| 列 | 字段 | 说明 |
|----|------|------|
| A | 企业名称 | 必填 |
| B | 统一社会信用代码 | 必填，唯一 |
| C | 地址 | 选填 |
| D | 电话 | 选填 |
| E | 法定代表人 | 选填 |
| F | 联系人 | 必填 |
| G | 联系电话 | 必填 |

> 说明：模板未暴露曾用名、业务员编码、备注等列，系统会自动将业务员编码、创建人编码填充为当前登录员工。高级用户如需导入这些可选字段，可按后端列顺序在 Excel 中自行追加列。

#### 响应参数

```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "totalCount": 10,
    "successCount": 8,
    "failCount": 2,
    "errors": [
      {
        "rowIndex": 3,
        "message": "统一社会信用代码已存在"
      },
      {
        "rowIndex": 7,
        "message": "联系方式不能为空"
      }
    ]
  }
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| totalCount | Integer | 处理总行数 |
| successCount | Integer | 成功数量 |
| failCount | Integer | 失败数量 |
| errors | Array | 错误详情列表 |
| errors[].rowIndex | Integer | 错误行号（从1开始） |
| errors[].message | String | 错误信息 |

---

### 导出客户

**接口地址：** `/api/customer/export`  
**请求方式：** GET  
**功能描述：** 根据筛选条件导出客户表格，响应为 Excel 二进制流。

#### 请求参数

与"分页查询客户"一致，用于指定导出范围。

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `客户信息导出.xlsx`（UTF-8编码）

**导出列：** 客户编码、企业名称、统一社会信用代码、地址、电话、法定代表人、联系人、联系电话、曾用名、业务员、备注、创建时间

**格式说明：** 列宽固定20字符，日期格式 `yyyy-MM-dd HH:mm:ss`

---

### 客户跟进记录接口

#### 获取客户跟进记录（新接口）

- **接口路径**: `GET /api/customer/{customerId}/follow-ups`
- **功能描述**: 根据客户ID查询该客户的所有跟进记录

#### 查询当前用户的客户跟进记录

- **接口路径**: `GET /api/customer/follow-up/list`
- **功能描述**: 查询当前登录用户的所有客户跟进记录

#### 新增客户跟进记录

- **接口路径**: `POST /api/customer/follow-up`
- **Content-Type**: `application/json`
- **请求参数**:
```json
{
  "customerId": 1,
  "followTime": "2025-01-20T14:00:00",
  "followMethod": "电话",
  "followContent": "沟通合同续签事宜",
  "nextPlan": "下周提交新报价单"
}
```

---

### 权限说明

- **普通用户**：自动按业务员编码过滤，仅能查看/导出/变更自己负责的客户
- **管理员**：可任意筛选业务员并执行全部操作
- **业务员策略**：管理员可通过 `ownerEmployeeId` 指定业务员；普通用户若传入业务员会被强制覆盖为当前登录人；详情/编辑/删除操作需校验业务员一致性

---

## 合同管理 API

**接口名称：** 合同管理服务  
**功能描述：** 提供合同的新增（含报价单与合同PDF上传）、更新、详情、分页查询能力。合同从客户列表"生成报价单"入口创建。  
**接口地址：** `/api/contract/*`  
**请求方式：** GET / POST / PUT

---

### 新增合同

**接口地址：** `/api/contract`  
**请求方式：** POST（multipart/form-data）  
**功能描述：** 从客户列表页面点击"生成报价单"按钮新增合同，同时创建一条报价单记录，并可上传合同PDF文件。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contract | JSON | 是 | 合同及报价单信息（`@RequestPart("contract")`） |
| file | MultipartFile | 否 | 合同PDF文件（`@RequestPart("file")`） |

**contract JSON 字段：**

```json
{
  "customerId": 1,
  "contractAmount": 50000.00,
  "disposalScope": "HW01医疗废物",
  "signTime": "2025-01-15T10:00:00",
  "validFrom": "2025-01-15T00:00:00",
  "validTo": "2026-01-14T23:59:59",
  "remark": "年度处置合同",
  "hazardousWasteCategory": "HW01 医疗废物",
  "hazardousWasteItemId": 1,
  "unitPrice": 3500.00,
  "pricingMethod": "吨",
  "quotationDetail": "医疗废物处置服务"
}
```

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户编码 |
| contractAmount | BigDecimal | 是 | 合同金额 |
| disposalScope | String | 否 | 处置范围 |
| signTime | LocalDateTime | 是 | 签订时间 |
| validFrom | LocalDateTime | 否 | 合同有效期开始 |
| validTo | LocalDateTime | 否 | 合同有效期结束 |
| remark | String | 否 | 备注 |
| hazardousWasteCategory | String | 是 | 危废类别 |
| hazardousWasteItemId | Integer | 是 | 危废条目编号 |
| unitPrice | BigDecimal | 是 | 单价 |
| pricingMethod | String | 是 | 计价方式 |
| quotationDetail | String | 否 | 报价明细 |

#### 响应参数

```json
{
  "code": 200,
  "message": "新增合同成功",
  "data": {
    "contractId": 1,
    "customerId": 1,
    "enterpriseName": "XX化工有限公司",
    "contractAmount": 50000.00,
    "disposalScope": "HW01医疗废物",
    "signTime": "2025-01-15T10:00:00",
    "contractStatus": "待审核",
    "validFrom": "2025-01-15T00:00:00",
    "validTo": "2026-01-14T23:59:59",
    "auditorId": null,
    "auditorName": null,
    "contractFileId": 1,
    "contractFileUrl": "/files/contract/2025/01/15/xxx.pdf",
    "creatorId": 1001,
    "remark": "年度处置合同",
    "createTime": "2025-01-15T10:30:00",
    "updateTime": "2025-01-15T10:30:00",
    "quotationId": 1,
    "hazardousWasteCategory": "HW01 医疗废物",
    "hazardousWasteItemId": 1,
    "unitPrice": 3500.00,
    "pricingMethod": "吨",
    "quotationDetail": "医疗废物处置服务"
  }
}
```

#### 业务逻辑

1. 根据 `customerId` 校验客户存在
2. 创建报价单（QUOTATION）记录，关联客户、危废信息、创建人
3. 如上传合同PDF：调用 `FileService.uploadAndSave` 保存文件并获取文件编号
4. 创建合同（CONTRACT）记录，关联报价单、文件、创建人，初始状态为"待审核"
5. 返回合同详情（含客户名称、报价单信息、文件URL）

---

### 合同详情

**接口地址：** `/api/contract/{contractId}`  
**请求方式：** GET  
**功能描述：** 根据合同编号查询合同详情，包含客户名称、报价单信息、合同PDF访问URL。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractId | Integer | 是 | 合同编号 |

#### 响应参数

同"新增合同"响应格式。

---

### 合同分页查询

**接口地址：** `/api/contract/list`  
**请求方式：** GET  
**功能描述：** 按客户名称、合同状态分页查询合同列表。

#### 请求参数

```json
{
  "current": 1,
  "size": 10,
  "enterpriseName": "化工",
  "contractStatus": "执行中"
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|--------|------|------|------|--------|
| current | Long | 否 | 当前页码（默认1） | 1 |
| size | Long | 否 | 每页数量（默认10） | 20 |
| enterpriseName | String | 否 | 客户名称（模糊匹配） | 化工 |
| contractStatus | String | 否 | 合同状态：待审核/已通过/执行中/已完结/已归档/已拒绝 | 执行中 |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "contractId": 1,
        "customerId": 1,
        "enterpriseName": "XX化工有限公司",
        "contractAmount": 50000.00,
        "disposalScope": "HW01医疗废物",
        "signTime": "2025-01-15T10:00:00",
        "contractStatus": "执行中",
        "validFrom": "2025-01-15T00:00:00",
        "validTo": "2026-01-14T23:59:59",
        "auditorId": 1002,
        "auditorName": "李四",
        "remark": "年度处置合同",
        "createTime": "2025-01-15T10:30:00",
        "updateTime": "2025-01-20T08:00:00"
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| records[].contractId | Integer | 合同编号 |
| records[].customerId | Integer | 客户编码 |
| records[].enterpriseName | String | 客户名称 |
| records[].contractAmount | BigDecimal | 合同金额 |
| records[].disposalScope | String | 处置范围 |
| records[].signTime | LocalDateTime | 签订时间 |
| records[].contractStatus | String | 合同状态 |
| records[].validFrom | LocalDateTime | 合同有效期开始 |
| records[].validTo | LocalDateTime | 合同有效期结束 |
| records[].auditorId | Integer | 审核人编码 |
| records[].auditorName | String | 审核人姓名 |
| records[].remark | String | 备注 |
| records[].createTime | LocalDateTime | 创建时间 |
| records[].updateTime | LocalDateTime | 更新时间 |

---

### 编辑合同

**接口地址：** `/api/contract/{contractId}`  
**请求方式：** PUT（multipart/form-data）  
**功能描述：** 编辑合同基础信息，可重新上传合同PDF（新文件替换旧文件）。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractId | Integer | 是 | 合同编号 |

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contract | JSON | 是 | 合同更新信息（`@RequestPart("contract")`） |
| file | MultipartFile | 否 | 合同PDF文件（`@RequestPart("file")`） |

**contract JSON 字段：**

```json
{
  "contractAmount": 60000.00,
  "disposalScope": "HW01医疗废物、HW08油泥",
  "signTime": "2025-01-15T10:00:00",
  "contractStatus": "执行中",
  "validFrom": "2025-01-15T00:00:00",
  "validTo": "2026-01-14T23:59:59",
  "remark": "合同变更"
}
```

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractAmount | BigDecimal | 否 | 合同金额 |
| disposalScope | String | 否 | 处置范围 |
| signTime | LocalDateTime | 否 | 签订时间 |
| contractStatus | String | 否 | 合同状态 |
| validFrom | LocalDateTime | 否 | 合同有效期开始 |
| validTo | LocalDateTime | 否 | 合同有效期结束 |
| remark | String | 否 | 备注 |

#### 响应参数

```json
{
  "code": 200,
  "message": "更新合同成功",
  "data": null
}
```

#### 业务逻辑

1. 根据 `contractId` 查询合同，不存在则抛出异常
2. 对请求中非空字段进行更新
3. 如上传新PDF：保存新文件，更新 `contractFileId`，删除旧文件
4. 更新 `updateTime` 并保存

---

### 合同统计信息

**接口地址：** `/api/contract/statistics`  
**请求方式：** GET  
**功能描述：** 获取合同统计数据，包含总数、执行中、已完结、待审核、本月新增合同数及本月合同金额等。

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 150,
    "executing": 45,
    "completed": 80,
    "pendingAudit": 10,
    "monthlyNew": 15,
    "monthlyAmount": 750000.00
  }
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| total | Integer | 合同总数 |
| executing | Integer | 执行中数量 |
| completed | Integer | 已完结数量 |
| pendingAudit | Integer | 待审核数量 |
| monthlyNew | Integer | 本月新增合同数 |
| monthlyAmount | BigDecimal | 本月合同金额 |

---

### 前端调用示例

**新增合同（FormData + JSON）**

```javascript
const formData = new FormData();
formData.append('contract', new Blob([JSON.stringify({
  customerId: 1,
  contractAmount: 50000.00,
  disposalScope: 'HW01医疗废物',
  signTime: '2025-01-15T10:00:00',
  validFrom: '2025-01-15T00:00:00',
  validTo: '2026-01-14T23:59:59',
  remark: '年度处置合同',
  hazardousWasteCategory: 'HW01 医疗废物',
  hazardousWasteItemId: 1,
  unitPrice: 3500.00,
  pricingMethod: '吨',
  quotationDetail: '医疗废物处置服务'
})], { type: 'application/json' }));
formData.append('file', pdfFileInput.files[0]);

fetch('/api/contract', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token
  },
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

**Axios 示例**

```javascript
import axios from 'axios';

const formData = new FormData();
formData.append('contract', new Blob([JSON.stringify(contractData)], { type: 'application/json' }));
formData.append('file', pdfFile);

axios.post('/api/contract', formData, {
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'multipart/form-data'
  }
}).then(response => console.log(response.data));
```

---

### 合同号模糊查询

**接口地址：** `GET /api/contract/search`  
**请求方式：** GET  
**功能描述：** 根据合同号关键字模糊查询合同列表，用于运输申请等场景的合同选择

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 否 | 合同号关键字（模糊匹配） |
| current | Long | 否 | 当前页码，默认1 |
| size | Long | 否 | 每页数量，默认20 |

### 更新合同状态

**接口地址：** `PUT /api/contract/{contractId}/status`  
**请求方式：** PUT  
**功能描述：** 更新合同状态（待审核/已通过/执行中/已完结/已归档/已拒绝等），状态变更后会发送消息通知。  
**请求体：** `ContractStatusUpdateRequest`（包含 `contractStatus`、可选 `auditOpinion`）。  
**响应：** 成功/失败统一响应，`data` 为空。

### 生成合同PDF

**接口地址：** `POST /api/contract/{contractId}/pdf`  
**请求方式：** POST  
**功能描述：** 为指定合同生成PDF文件，文件信息保存到 `FILE` 表

#### 响应格式

```json
{
  "code": 200,
  "message": "PDF生成成功",
  "data": {
    "contractId": 1,
    "contractNo": "HT-20250115-00001",
    "pdfFileId": 1001,
    "pdfFileUrl": "/api/file/download?path=contracts/合同_HT-20250115-00001_20250115103000.pdf"
  }
}
```

### 获取合同执行进度

**接口地址：** `GET /api/contract/{contractId}/progress`  
**请求方式：** GET  
**功能描述：** 获取合同执行进度，包含创建、审核、收运通知、派车、入库、结算、发票、收款等节点的时间轴和统计信息。  
**响应：** `ContractProgressResponse`（时间轴+统计）。

### 导出合同Word

**接口地址：** `GET /api/contract/{contractId}/word`  
**请求方式：** GET  
**功能描述：** 生成并下载合同Word文档，文件名默认为合同号或 `contract-{id}.docx`。  
**响应：** `application/vnd.openxmlformats-officedocument.wordprocessingml.document` 二进制流。

---

## 报价单管理 API

**接口名称：** 报价单管理服务  
**功能描述：** 提供报价单的新增、更新、详情、分页查询、审核、导出、PDF生成等完整功能。支持"总价包干"和"按量结算"两种报价模式。  
**接口地址：** `/api/quotation/*`  
**请求方式：** GET / POST / PUT

---

### 新增报价单

**接口地址：** `/api/quotation`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 创建新的报价单，支持总价包干和按量结算两种模式。

#### 请求参数

```json
{
  "customerId": 1,
  "internalCode": "QT-20250101-00001",
  "quotationDate": "2025-01-15",
  "partyAName": "XX化工有限公司",
  "contactPerson": "王五",
  "contactPhone": "13800138000",
  "partyBName": "危险废物处理有限公司",
  "partyBContact": "张三",
  "partyBContactPhone": "13900139000",
  "partyBCreditCode": "91440101MA5XXXXXXX",
  "validFrom": "2025-01-15 00:00:00",
  "validTo": "2026-01-14 23:59:59",
  "remark": "年度报价",
  "items": [
    {
      "pricingMode": "总价包干",
      "payer": "甲方",
      "pricingStatement": "一次性处置费用50000元",
      "remark": "包含运输和处置",
      "wastes": [
        {
          "hazardousWasteItemId": 1,
          "wasteCategory": "HW01 医疗废物",
          "industrySource": "卫生",
          "wasteCode": "841-001-01",
          "wasteName": "感染性废物",
          "wasteState": "固态",
          "quantityUnit": "吨",
          "plannedQuantity": 10.00
        }
      ]
    }
  ]
}
```

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户编码 |
| internalCode | String | 否 | 内部编号（映射到报价单号） |
| quotationNo | String | 否 | 报价单号（未提供时系统自动生成格式：QT-YYYYMMDD-XXXXX） |
| quotationDate | String | 否 | 报价日期（格式：YYYY-MM-DD，默认当天） |
| partyAName | String | 否 | 甲方名称（默认使用客户企业名称） |
| contactPerson | String | 否 | 甲方联系人（默认使用客户联系人） |
| contactPhone | String | 否 | 甲方联系电话（默认使用客户联系电话） |
| partyBName | String | 是 | 乙方名称 |
| partyBContact | String | 是 | 乙方联系人 |
| partyBContactPhone | String | 是 | 乙方联系电话 |
| partyBCreditCode | String | 否 | 乙方统一社会信用代码 |
| validFrom | String/Object | 否 | 有效期开始（格式：YYYY-MM-DD HH:mm:ss） |
| validTo | String/Object | 否 | 有效期结束（格式：YYYY-MM-DD HH:mm:ss） |
| remark | String | 否 | 备注 |
| items | Array | 是 | 报价条目列表（至少一条） |
| items[].pricingMode | String | 是 | 报价模式：总价包干/按量结算 |
| items[].payer | String | 是 | 付款方：甲方/乙方/共同 |
| items[].pricingStatement | String | 否 | 计价方案（总价包干时使用） |
| items[].remark | String | 否 | 备注（总价包干时使用） |
| items[].wastes | Array | 否 | 危废条目明细列表（按量结算时必填） |
| items[].wastes[].hazardousWasteItemId | Integer | 否 | 危废条目编号 |
| items[].wastes[].wasteCategory | String | 是 | 废物类别（如：HW01 医疗废物） |
| items[].wastes[].industrySource | String | 否 | 行业来源 |
| items[].wastes[].wasteCode | String | 是 | 废物代码（如：841-001-01） |
| items[].wastes[].wasteName | String | 是 | 危险废物名称 |
| items[].wastes[].wasteState | String | 否 | 形态（固态/液态/气态/半固态等） |
| items[].wastes[].quantityUnit | String | 是 | 计量单位（吨/桶/个等） |
| items[].wastes[].plannedQuantity | BigDecimal | 是 | 计划数量 |
| items[].wastes[].pricingStatement | String | 否 | 计价方案（按量结算时使用） |
| items[].wastes[].payer | String | 否 | 付款方（按量结算时使用） |
| items[].wastes[].remark | String | 否 | 备注（按量结算时使用） |

#### 响应参数

```json
{
  "code": 200,
  "message": "新增报价单成功",
  "data": {
    "quotationId": 1,
    "quotationNo": "QT-20250115-00001",
    "customerId": 1,
    "customerName": "XX化工有限公司",
    "partyAName": "XX化工有限公司",
    "partyAContact": "王五",
    "partyAContactPhone": "13800138000",
    "partyBName": "危险废物处理有限公司",
    "partyBContact": "张三",
    "partyBContactPhone": "13900139000",
    "partyBCreditCode": "91440101MA5XXXXXXX",
    "quotationStatus": "待审批",
    "quotationDate": "2025-01-15",
    "validFrom": "2025-01-15T00:00:00",
    "validTo": "2026-01-14T23:59:59",
    "totalQuantity": 10.00,
    "pdfFileId": null,
    "pdfFileUrl": null,
    "remark": "年度报价",
    "creatorId": 1001,
    "creatorName": "李四",
    "createTime": "2025-01-15T10:30:00",
    "updateTime": "2025-01-15T10:30:00",
    "items": [
      {
        "quotationItemId": 1,
        "quotationMode": "总价包干",
        "payer": "甲方",
        "pricingPlan": "一次性处置费用50000元",
        "remark": "包含运输和处置",
        "wasteItems": [...]
      }
    ]
  }
}
```

#### 业务逻辑

1. 校验客户存在且状态正常
2. 甲方信息为空时自动回填客户档案信息
3. 校验乙方信息必填
4. 报价单号未提供时自动生成（格式：QT-YYYYMMDD-XXXXX）
5. 报价日期未提供时默认当天
6. 新建后状态默认为"待审批"
7. 按量结算模式必须包含危废明细
8. 总数量由服务层按危废明细动态计算

---

### 更新报价单

**接口地址：** `/api/quotation/{quotationId}`  
**请求方式：** PUT  
**Content-Type：** `application/json`  
**功能描述：** 更新报价单信息，只有待审批状态的报价单可以修改。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| quotationId | Integer | 是 | 报价单编号 |

#### 请求参数

请求体结构与新增报价单一致，但需注意：
- `quotationId` 字段在路径中传递，请求体中无需包含
- `items[].quotationItemId` 和 `items[].wasteItems[].quotationWasteItemId` 为更新时使用（可选）
- 更新时会整体删除旧的报价条目和危废明细，按新数据重建

#### 响应参数

```json
{
  "code": 200,
  "message": "更新报价单成功",
  "data": null
}
```

#### 业务逻辑

1. 验证报价单存在且状态为"待审批"
2. 整体删除旧的报价条目和危废明细
3. 按新数据重建报价条目和危废明细
4. 重新计算总数量

---

### 报价单详情

**接口地址：** `/api/quotation/{quotationId}`  
**请求方式：** GET  
**功能描述：** 根据报价单编号查询详情，包含甲乙双方信息、报价条目、危废明细、PDF信息等。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| quotationId | Integer | 是 | 报价单编号 |

#### 响应参数

同"新增报价单"响应格式，包含完整的报价条目和危废明细信息。

---

### 报价单分页查询

**接口地址：** `/api/quotation/list`  
**请求方式：** GET  
**功能描述：** 支持报价单编号、客户名称模糊查询，可筛选报价状态、计价方式、有效期、PDF状态等。

#### 请求参数

```json
{
  "current": 1,
  "size": 10,
  "keyword": "QT-2025",
  "quotationNoSearch": "QT-2025",
  "customerName": "化工",
  "customerId": 1,
  "quotationStatus": "待审批",
  "pricingMode": "PACKAGE",
  "quotationNo": "QT-20250115-00001",
  "internalCode": "QT-2025",
  "creatorName": "张三",
  "validFrom": "2025-01-01 00:00:00",
  "validTo": "2025-12-31 23:59:59",
  "pdfGenerated": true
}
```

| 参数名 | 类型 | 必填 | 说明 | 示例值 |
|--------|------|------|------|--------|
| current | Long | 是 | 当前页码（≥1） | 1 |
| size | Long | 是 | 每页数量（≥1） | 10 |
| keyword | String | 否 | 关键词（客户名称/报价单号） | QT-2025 |
| quotationNoSearch | String | 否 | 报价单号模糊查询 | QT-2025 |
| customerName | String | 否 | 客户名称模糊查询 | 化工 |
| customerId | Integer | 否 | 客户编码精确匹配 | 1 |
| quotationStatus | String | 否 | 报价状态：待审批/已通过/已拒绝/已失效 | 待审批 |
| pricingMode | String | 否 | 计价方式：PACKAGE(总价包干)/UNIT(按量结算)/MIXED(组合计价) | PACKAGE |
| quotationNo | String | 否 | 报价单号（内部编号） | QT-20250115-00001 |
| internalCode | String | 否 | 内部编号模糊查询 | QT-2025 |
| creatorName | String | 否 | 创建人姓名模糊查询 | 张三 |
| validFrom | LocalDateTime | 否 | 有效期开始（格式：yyyy-MM-dd HH:mm:ss） | 2025-01-01 00:00:00 |
| validTo | LocalDateTime | 否 | 有效期结束（格式：yyyy-MM-dd HH:mm:ss） | 2025-12-31 23:59:59 |
| pdfGenerated | Boolean | 否 | 是否已生成PDF | true |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "quotationId": 1,
        "quotationNo": "QT-20250115-00001",
        "internalCode": "QT-20250115-00001",
        "quotationCode": "QT-20250115-00001",
        "customerId": 1,
        "customerName": "XX化工有限公司",
        "partyAName": "XX化工有限公司",
        "partyAContact": "王五",
        "partyAContactPhone": "13800138000",
        "partyBName": "危险废物处理有限公司",
        "partyBContact": "张三",
        "partyBContactPhone": "13900139000",
        "partyBCreditCode": "91440101MA5XXXXXXX",
        "quotationStatus": "待审批",
        "quotationDate": "2025-01-15",
        "pricingMode": "总价包干",
        "validFrom": "2025-01-15T00:00:00",
        "validTo": "2026-01-14T23:59:59",
        "totalQuantity": 10.00,
        "quantityUnit": "吨",
        "pdfFileId": null,
        "pdfGenerated": false,
        "pdfUrl": null,
        "pdfFileName": null,
        "creatorName": "李四",
        "createTime": "2025-01-15T10:30:00",
        "remark": "年度报价"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

| 参数名 | 类型 | 说明 |
|--------|------|------|
| records[].quotationId | Integer | 报价单编号 |
| records[].quotationNo | String | 报价单号 |
| records[].internalCode | String | 内部编号 |
| records[].customerId | Integer | 客户编码 |
| records[].customerName | String | 客户名称 |
| records[].partyAName | String | 甲方名称 |
| records[].partyAContact | String | 甲方联系人 |
| records[].partyAContactPhone | String | 甲方联系电话 |
| records[].partyBName | String | 乙方名称 |
| records[].partyBContact | String | 乙方联系人 |
| records[].partyBContactPhone | String | 乙方联系电话 |
| records[].partyBCreditCode | String | 乙方统一社会信用代码 |
| records[].quotationStatus | String | 报价状态 |
| records[].quotationDate | LocalDate | 报价日期 |
| records[].pricingMode | String | 报价模式 |
| records[].validFrom | LocalDateTime | 有效期开始 |
| records[].validTo | LocalDateTime | 有效期结束 |
| records[].totalQuantity | BigDecimal | 总数量（动态计算） |
| records[].quantityUnit | String | 计量单位 |
| records[].pdfFileId | Integer | PDF文件编号 |
| records[].pdfGenerated | Boolean | 是否已生成PDF |
| records[].pdfUrl | String | PDF文件URL |
| records[].pdfFileName | String | PDF文件名 |
| records[].creatorName | String | 创建人姓名 |
| records[].createTime | LocalDateTime | 创建时间 |
| records[].remark | String | 备注 |

---

### 审核报价单

**接口地址：** `/api/quotation/audit`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 审核报价单并修改状态。状态取值支持【草稿/待审批/已通过/已拒绝/已失效】；当当前状态为“待审批”时仅允许流转为“已通过”或“已拒绝”，拒绝时审核意见必填，已失效单据不可再修改状态。

#### 请求参数

```json
{
  "quotationId": 1,
  "auditResult": "已通过",
  "auditOpinion": "审核通过，可以执行"
}
```

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| quotationId | Integer | 是 | 报价单编号 |
| auditResult | String | 是 | 审核结果：草稿/待审批/已通过/已拒绝/已失效。当前为“待审批”时只能流转为“已通过”或“已拒绝”；当前为“已失效”不可再修改 |
| auditOpinion | String | 否 | 审核意见（审核结果为"已拒绝"时必填） |

#### 响应参数

```json
{
  "code": 200,
  "message": "审核报价单成功",
  "data": null
}
```

#### 业务逻辑

1. 验证报价单存在
2. 待审批状态只能审核为"已通过"或"已拒绝"
3. 拒绝时审核意见必填
4. 审核意见会追加到备注字段（前缀：`[审核意见]`）
5. 已失效状态不可再修改

---

### 导出报价单

**接口地址：** `/api/quotation/export`  
**请求方式：** GET  
**功能描述：** 根据筛选条件导出报价单Excel，导出列包含报价单编号、报价单号、客户名称、报价状态、计价方式、有效期、总数量、计量单位、PDF状态、创建人、创建时间、备注等。

#### 请求参数

与"报价单分页查询"一致，用于指定导出范围。

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `报价单导出.xlsx`

**导出列：** 报价单编号、报价单号、客户名称、报价状态、计价方式、有效期开始、有效期结束、总数量、计量单位、PDF状态、创建人、创建时间、备注

---

### 导出报价单

**接口地址：** `GET /api/quotation/export`  
**请求方式：** GET  
**功能描述：** 根据筛选条件导出报价单Excel，导出列包含报价单编号、报价单号、客户名称、报价状态、计价方式、有效期、总数量、计量单位、PDF状态、创建人、创建时间、备注等。

#### 请求参数

与"报价单分页查询"一致，用于指定导出范围。

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `报价单导出.xlsx`

### 生成报价单PDF

**接口地址：** `POST /api/quotation/{quotationId}/pdf`  
**请求方式：** POST  
**功能描述：** 为指定报价单生成PDF文件，文件信息保存到 `FILE` 表（业务类型：QUOTATION）。如已存在旧PDF，会先软删除旧文件。

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| quotationId | Integer | 是 | 报价单编号 |

#### 响应参数

```json
{
  "code": 200,
  "message": "PDF生成成功",
  "data": {
    "quotationId": 1,
    "quotationNo": "QT-20250115-00001",
    "pdfFileId": 1001,
    "pdfFileUrl": "/api/file/download?path=quotations/报价单_QT-20250115-00001_20250115103000.pdf",
    ...
  }
}
```

#### 业务逻辑

1. 查询报价单详情
2. 如已存在PDF文件，先软删除旧文件（`文件状态`置为`已删除`）
3. 使用iText库生成PDF文件，包含以下特性：
   - 表格结构：序号、废物类别、废物代码、废物名称、废物形态、计划转移数量、金额、付款方
   - 小计摘要行：每个报价条目的小计摘要合并为一行显示，所有小计项用顿号（、）分隔
   - 合计行：根据所有报价条目的小计JSON数据计算，相同unit合并，不同unit用顿号分开
   - 总价包干模式单元格合并：金额和付款方列会合并相同值的单元格，合并时去掉quotationItemId
4. 保存文件信息到 `FILE` 表，`业务类型=QUOTATION`、`业务ID=报价单编号`
5. 返回报价单详情（含最新PDF信息）

#### PDF生成规则

1. **小计摘要行**：
   - 每个报价条目的小计摘要合并为一行显示
   - 格式：`"1110 吨、2220 公斤、1110 公斤"`（所有小计项用顿号分隔）
   - 金额列和付款方列显示"/"

2. **合计行**：
   - 根据所有报价条目的小计JSON数据计算
   - 相同unit进行加法运算，不同unit用顿号分开
   - 示例：`[{unit:"吨",total:100.5}, {unit:"桶",total:20}, {unit:"吨",total:50}]` → `"150.5 吨、20 桶"`

3. **总价包干模式显示**：
   - 金额列格式：`quotationItemId|pricingPlan`（使用|作为分隔符）
   - 付款方列格式：`quotationItemId|payer`（使用|作为分隔符）
   - 未合并单元格：显示完整内容，如 `"18|1500元/吨"`、`"20|甲方"`
   - 合并单元格：去掉quotationItemId，只显示内容，如 `"1500元/吨"`、`"甲方"`
   - 单条数据也会去掉quotationItemId，只显示内容部分

### 导出报价单Word文档

**接口地址：** `GET /api/quotation/{quotationId}/word`  
**请求方式：** GET  
**功能描述：** 为指定报价单生成并下载Word文档，文件名默认为报价单号或 `quotation-{id}.docx`。  
**响应：** `application/vnd.openxmlformats-officedocument.wordprocessingml.document` 二进制流。

---

### 前端调用示例

**新增报价单**

```javascript
import axios from 'axios';

const quotationData = {
  customerId: 1,
  quotationDate: '2025-01-15',
  partyBName: '危险废物处理有限公司',
  partyBContact: '张三',
  partyBContactPhone: '13900139000',
  validFrom: '2025-01-15 00:00:00',
  validTo: '2026-01-14 23:59:59',
  remark: '年度报价',
  items: [
    {
      pricingMode: '总价包干',
      payer: '甲方',
      pricingStatement: '一次性处置费用50000元',
      remark: '包含运输和处置',
      wastes: [
        {
          wasteCategory: 'HW01 医疗废物',
          industrySource: '卫生',
          wasteCode: '841-001-01',
          wasteName: '感染性废物',
          wasteState: '固态',
          quantityUnit: '吨',
          plannedQuantity: 10.00
        }
      ]
    }
  ]
};

axios.post('/api/quotation', quotationData, {
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  }
}).then(response => {
  if (response.data.code === 200) {
    console.log('新增报价单成功', response.data.data);
  }
});
```

**更新报价单**

```javascript
axios.put(`/api/quotation/${quotationId}`, quotationData, {
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  }
}).then(response => {
  if (response.data.code === 200) {
    console.log('更新报价单成功');
  }
});
```

**审核报价单**

```javascript
axios.post('/api/quotation/audit', {
  quotationId: 1,
  auditResult: '已通过',
  auditOpinion: '审核通过，可以执行'
}, {
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  }
}).then(response => {
  if (response.data.code === 200) {
    console.log('审核报价单成功');
  }
});
```

---

## 文件管理 API

**接口名称：** 文件管理服务  
**功能描述：** 提供文件上传、下载、预览等功能  
**接口地址：** `/api/file/*`  
**请求方式：** GET / POST

---

### 文件上传

**接口地址：** `POST /api/file/uploads`  
**请求方式：** POST  
**Content-Type：** `multipart/form-data`  
**功能描述：** 通用文件上传接口，返回文件ID和URL

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 上传的文件 |

#### 响应格式

```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "fileId": "1001",
    "fileUrl": "/api/file/download?path=transport_apply/2025/01/15/xxx.pdf"
  }
}
```

---

### 文件下载（根据路径）

**接口地址：** `GET /api/file/download`  
**请求方式：** GET  
**功能描述：** 根据文件相对路径下载文件

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| path | String | 是 | 文件相对路径 |

#### 响应

**Content-Type:** 根据文件类型自动识别  
**Content-Disposition:** `attachment; filename="文件名"`

---

### 文件下载（根据文件ID）

**接口地址：** `GET /api/file/download/{fileId}`  
**请求方式：** GET  
**功能描述：** 根据文件ID下载文件，支持本地存储和云端存储

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | Integer | 是 | 文件ID |

---

### 文件预览

**接口地址：** `GET /api/file/preview/{fileId}`  
**请求方式：** GET  
**功能描述：** 根据文件ID预览文件（PDF等），使用inline模式在浏览器中显示

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileId | Integer | 是 | 文件ID |

---

## 日志管理 API

**接口名称：** 日志管理服务  
**功能描述：** 提供系统日志的分页查询、详情查看、导出等功能  
**接口地址：** `/api/log/*`  
**请求方式：** GET / POST

---

### 分页查询全部日志

**接口地址：** `POST /api/log/page/all`  
**请求方式：** POST  
**功能描述：** 支持按关键字、类型、状态、模块、IP、时间范围筛选

#### 请求参数

```json
{
  "current": 1,
  "size": 10,
  "keyword": "客户",
  "type": "操作日志",
  "status": "success",
  "module": "客户管理",
  "ip": "192.168.1.1",
  "startTime": "2025-01-01 00:00:00",
  "endTime": "2025-01-31 23:59:59"
}
```

---

### 分页查询操作日志

**接口地址：** `POST /api/log/page/operation`  
**请求方式：** POST  
**功能描述：** 支持按关键字、模块、操作类型、IP、时间范围筛选

---

### 分页查询数据变更日志

**接口地址：** `POST /api/log/page/data-change`  
**请求方式：** POST  
**功能描述：** 支持按关键字、数据表、记录ID、变更类型、时间范围筛选

---

### 分页查询登录日志

**接口地址：** `POST /api/log/page/login`  
**请求方式：** POST  
**功能描述：** 支持按关键字、登录结果、IP、时间范围筛选

---

### 获取日志详情

**接口地址：** `GET /api/log/{logId}`  
**请求方式：** GET  
**功能描述：** 根据日志编号获取日志详情，包括字段差异对比

---

### 导出日志

**接口地址：** `POST /api/log/export`  
**请求方式：** POST  
**功能描述：** 支持导出全部或选中的日志，返回Excel文件

#### 请求参数

```json
{
  "mode": "all",
  "logIds": [1, 2, 3]
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| mode | String | 是 | 导出模式：all（全部）/selected（选中） |
| logIds | List<Integer> | 否 | 选中的日志ID列表（mode为selected时必填） |

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `系统日志导出.xlsx`

---

## 收运通知单管理 API

**接口名称：** 收运通知单管理服务  
**功能描述：** 提供收运通知单的新增、更新、查询、审核、删除等功能  
**接口地址：** `/api/transport/apply/*`  
**请求方式：** GET / POST / DELETE

---

### 新增收运通知单

**接口地址：** `POST /api/transport/apply/create`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 创建新的收运通知单，包含危废明细和附件信息

---

### 更新收运通知单

**接口地址：** `POST /api/transport/apply/update`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新收运通知单信息，包含危废明细和附件信息

---

### 获取收运通知单详情

**接口地址：** `GET /api/transport/apply/detail`  
**请求方式：** GET  
**功能描述：** 根据收运通知单号或编号查询详情

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeCode | String | 否 | 收运通知单号 |
| noticeId | Integer | 否 | 收运通知单编号 |

---

### 收运通知单分页查询

**接口地址：** `GET /api/transport/apply/list`  
**请求方式：** GET  
**功能描述：** 支持按收运通知单号、合同号、客户、状态等条件筛选

---

### 提交收运通知单审核

**接口地址：** `POST /api/transport/apply/submit`  
**请求方式：** POST  
**功能描述：** 将收运通知单状态从"未提交"变更为"审核中"

#### 请求参数

```json
{
  "noticeCode": "SY-20250115-00001"
}
```

---

### 撤回收运通知单

**接口地址：** `POST /api/transport/apply/revoke`  
**请求方式：** POST  
**功能描述：** 将收运通知单状态从"审核中"变更为"未提交"

---

### 审核收运通知单

**接口地址：** `POST /api/transport/apply/audit`  
**请求方式：** POST  
**功能描述：** 审核收运通知单并修改状态。审核中状态的收运通知单可审核为待调度（通过）或审核失败（拒绝），拒绝时审核意见必填

#### 请求参数

```json
{
  "noticeCode": "SY-20250115-00001",
  "auditResult": "待调度",
  "auditOpinion": "审核通过"
}
```

---

### 删除收运通知单

**接口地址：** `DELETE /api/transport/apply/{noticeCode}`  
**请求方式：** DELETE  
**功能描述：** 删除未提交状态的收运通知单

---

### 审核阶段补充合同号

**接口地址：** `POST /api/transport/apply/audit/bind-contract`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 仅审核中状态允许补充合同号，更新后清除待补标记

#### 请求参数

```json
{
  "noticeCode": "SY-20250115-00001",
  "contractCode": "HT-20250115-00001"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeCode | String | 是 | 收运通知单号 |
| contractCode | String | 是 | 合同号 |

---

### 查询危废类别限额与已入库量

**接口地址：** `GET /api/transport/apply/waste-category-limit`  
**请求方式：** GET  
**功能描述：** 根据废物代码列表返回对应类别的限额信息和限额期已入库量

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| codes | String | 是 | 废物代码列表，多个用逗号分隔 |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "wasteCode": "841-001-01",
      "wasteCategory": "HW01",
      "limitAmount": 1000.00,
      "limitStartTime": "2025-01-01T00:00:00",
      "limitEndTime": "2025-12-31T23:59:59",
      "warehousedAmount": 500.00,
      "availableAmount": 500.00
    }
  ]
}
```

---

### 导出车辆安排

**接口地址：** `GET /api/transport/apply/vehicle-arrange/export`  
**请求方式：** GET  
**功能描述：** 根据筛选条件导出车辆安排Excel，导出列包含通知单号、合同号、客户名称、统一社会信用代码、现场联系人、现场联系电话、转运地址、计划转移时间、创建人、状态、是否锁定、锁定原因、备注等。

#### 请求参数

与"收运通知单分页查询"一致，用于指定导出范围（不需要分页参数）。

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `车辆安排导出.xlsx`

**导出列：** 通知单号、合同号、客户名称、统一社会信用代码、现场联系人、现场联系电话、转运地址、计划转移时间、创建人、状态、是否锁定、锁定原因、备注

---

## 运输单管理 API

**接口名称：** 运输单管理服务  
**功能描述：** 提供运输单的创建、更新、查询、校验、打印、导出等功能  
**接口地址：** `/api/transport/dispatch/*`  
**请求方式：** GET / POST

---

### 创建运输单

**接口地址：** `POST /api/transport/dispatch/create`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 一张收运通知单仅可生成一张运输单

#### 请求参数

```json
{
  "noticeCode": "SY-20250115-00001",
  "carrierName": "XX运输公司",
  "carrierPhone": "13800138000",
  "driverId": 1,
  "vehicleId": 1,
  "escortId": 1,
  "startPoint": "XX化工厂",
  "endPoint": "XX处置中心",
  "dispatchAt": "2025-01-20T08:00:00",
  "planQuantityTon": 10.00,
  "remark": "注意安全"
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "dispatchId": 1,
    "dispatchCode": "YS-20250120-00001",
    "noticeCode": "SY-20250115-00001",
    "contractCode": "HT-20250115-00001",
    "carrierName": "XX运输公司",
    "driverName": "张三",
    "plateNo": "京A12345",
    "status": "待运输",
    "createTime": "2025-01-20T08:00:00"
  }
}
```

---

### 更新运输单

**接口地址：** `POST /api/transport/dispatch/update`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 仅未完成、未锁定的运输单可修改

#### 请求参数

同"创建运输单"，需包含 `dispatchCode` 字段。

---

### 获取运输单详情

**接口地址：** `GET /api/transport/dispatch/detail`  
**请求方式：** GET  
**功能描述：** `dispatchCode` 和 `noticeCode` 至少传一个

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| dispatchCode | String | 否 | 运输单号 |
| noticeCode | String | 否 | 收运通知单号 |

---

### 校验运输单风险

**接口地址：** `GET /api/transport/dispatch/validate`  
**请求方式：** GET  
**功能描述：** 校验合同缺失、超限等风险

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeCode | String | 否 | 收运通知单号 |
| dispatchCode | String | 否 | 运输单号 |

#### 响应示例

```json
{
  "code": 200,
  "message": "校验成功",
  "data": {
    "hasContract": true,
    "hasRisk": false,
    "riskMessages": [],
    "overLimitWastes": []
  }
}
```

---

### 分页查询运输单列表

**接口地址：** `POST /api/transport/dispatch/list`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 支持多条件查询和排序

#### 请求参数

```json
{
  "current": 1,
  "size": 10,
  "dispatchCode": "YS-2025",
  "noticeCode": "SY-2025",
  "contractCode": "HT-2025",
  "status": "待运输",
  "carrierName": "XX运输",
  "driverName": "张三",
  "plateNo": "京A",
  "startTime": "2025-01-01 00:00:00",
  "endTime": "2025-01-31 23:59:59"
}
```

---

### 打印运输单

**接口地址：** `GET /api/transport/dispatch/print`  
**请求方式：** GET  
**功能描述：** 生成运输单PDF并返回打印URL

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| dispatchCode | String | 是 | 运输单号 |

#### 响应示例

```json
{
  "code": 200,
  "message": "PDF生成成功",
  "data": "/api/file/download?path=dispatch/运输单_YS-20250120-00001_20250120080000.pdf"
}
```

---

### 批量打印运输单

**接口地址：** `POST /api/transport/dispatch/batch-print`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 批量生成运输单PDF并返回打印URL

#### 请求参数

```json
["YS-20250120-00001", "YS-20250120-00002", "YS-20250120-00003"]
```

---

### 导出运输执行Excel

**接口地址：** `GET /api/transport/dispatch/export`  
**请求方式：** GET  
**功能描述：** 根据筛选条件导出运输执行Excel文件

#### 请求参数

与"分页查询运输单列表"一致，用于指定导出范围（不需要分页参数）。

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `运输执行导出.xlsx`

**导出列：** 运输单号、收运通知单号、合同号、承运单位、承运单位电话、驾驶员姓名、驾驶员电话、车辆号牌、运输起点、运输终点、派车时间、实际起运时间、实际到达时间、计划转移数量(吨)、状态、锁定、锁定原因、调度员、创建时间

---

## 车辆管理 API

**接口名称：** 车辆管理服务  
**功能描述：** 提供车辆档案的分页查询、详情查询、创建、更新、删除、导出、批量导入等功能  
**接口地址：** `/api/transport/vehicle/*`  
**请求方式：** GET / POST / PUT / DELETE

---

### 获取车辆档案列表

**接口地址：** `GET /api/transport/vehicle/archive`  
**请求方式：** GET  
**功能描述：** 分页查询车辆档案，支持关键字搜索和状态筛选

---

### 获取车辆详情

**接口地址：** `GET /api/transport/vehicle/{vehicleId}`  
**请求方式：** GET  
**功能描述：** 根据车辆ID获取车辆详细信息

---

### 新增车辆

**接口地址：** `POST /api/transport/vehicle`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 创建车辆档案

---

### 更新车辆

**接口地址：** `PUT /api/transport/vehicle/{vehicleId}`  
**请求方式：** PUT  
**Content-Type：** `application/json`  
**功能描述：** 更新车辆档案信息

---

### 删除车辆

**接口地址：** `DELETE /api/transport/vehicle/{vehicleId}`  
**请求方式：** DELETE  
**功能描述：** 删除车辆档案

---

### 批量导入车辆

**接口地址：** `POST /api/transport/vehicle/import`  
**请求方式：** POST  
**Content-Type：** `multipart/form-data`  
**功能描述：** 支持上传Excel文件批量创建车辆信息

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | Excel文件（.xls/.xlsx） |

---

### 下载车辆导入模板

**接口地址：** `GET /api/transport/vehicle/import/template`  
**请求方式：** GET  
**功能描述：** 下载车辆导入模板Excel文件

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `车辆导入模板.xlsx`

---

### 导出车辆

**接口地址：** `GET /api/transport/vehicle/export`  
**请求方式：** GET  
**功能描述：** 根据筛选条件导出车辆Excel

#### 响应

**Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`  
**文件名：** `车辆信息导出.xlsx`

---

## 入库单管理 API

**接口名称：** 入库单管理服务  
**功能描述：** 提供入库单的批量创建、分页查询、详情、更新、审核、取消审核、删除等功能  
**接口地址：** `/api/warehouse/inbound/*`  
**请求方式：** GET / POST / DELETE

---

### 批量创建入库单

**接口地址：** `POST /api/warehouse/inbound/batch-create`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 根据总磅单批量创建入库单，为每个关联的收运通知单生成一个入库单

#### 请求参数

```json
{
  "weighingSlipNo": "ZB-20250120-00001",
  "warehousingTime": "2025-01-20 14:00:00",
  "warehouseKeeperId": 1001,
  "remark": "批量入库",
  "warehousingList": [
    {
      "pickupNoticeCode": "SY-20250115-00001",
      "dispatchCode": "YS-20250120-00001",
      "warehousingTime": "2025-01-20 14:00:00",
      "warehouseKeeperId": 1001,
      "remark": "入库备注",
      "items": [
        {
          "pickupNoticeItemId": 1,
          "actualQty": 10.50,
          "differenceReason": "实际接收数量与计划数量一致"
        }
      ]
    }
  ]
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| weighingSlipNo | String | 是 | 总磅单号 |
| warehousingTime | String | 否 | 入库时间（格式：yyyy-MM-dd HH:mm:ss） |
| warehouseKeeperId | Integer | 否 | 仓管员编码 |
| remark | String | 否 | 入库备注 |
| warehousingList | Array | 是 | 入库单列表（至少一条） |
| warehousingList[].pickupNoticeCode | String | 是 | 收运通知单号 |
| warehousingList[].dispatchCode | String | 是 | 运输单号 |
| warehousingList[].warehousingTime | String | 是 | 入库时间（格式：yyyy-MM-dd HH:mm:ss） |
| warehousingList[].warehouseKeeperId | Integer | 是 | 仓管员编码 |
| warehousingList[].remark | String | 否 | 入库备注 |
| warehousingList[].items | Array | 是 | 危废明细列表（至少一条） |
| warehousingList[].items[].pickupNoticeItemId | Integer | 是 | 收运通知单明细编号 |
| warehousingList[].items[].actualQty | BigDecimal | 是 | 实际收运数量（吨） |
| warehousingList[].items[].differenceReason | String | 否 | 差异原因 |

#### 响应参数

```json
{
  "code": 200,
  "message": "批量创建入库单成功",
  "data": {
    "weighingSlipNo": "ZB-20250120-00001",
    "totalCount": 2,
    "successCount": 2,
    "failCount": 0,
    "warehousingList": [
      {
        "warehousingId": 1,
        "warehousingNo": "RKD-20250120-0001",
        "pickupNoticeCode": "SY-20250115-00001",
        "dispatchCode": "YS-20250120-00001",
        "status": "待审核",
        "warehousingTime": "2025-01-20T14:00:00"
      }
    ]
  }
}
```

#### 业务逻辑

1. 根据总磅单号查询总磅单信息，验证总磅单存在
2. 为每个收运通知单创建一个入库单，入库单号自动生成（格式：RKD-YYYYMMDD-4位序号）
3. 入库单危废明细自动从收运通知单明细带出，用户只需填写实际接收数量和差异说明
4. 入库单初始状态为"待审核"
5. 入库单号自动生成规则：RKD-YYYYMMDD-4位序号

---

### 分页查询入库单列表

**接口地址：** `GET /api/warehouse/inbound/list`  
**请求方式：** GET  
**功能描述：** 支持按关键字、总磅单号、收运通知单号、状态、时间范围等条件筛选

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码（默认1） |
| size | Integer | 否 | 每页数量（默认20） |
| keyword | String | 否 | 关键字（入库单号/总磅单号/收运通知单号，模糊查询） |
| weighingSlipNo | String | 否 | 总磅单号（模糊查询） |
| pickupNoticeNo | String | 否 | 收运通知单号（模糊查询） |
| status | String | 否 | 状态：待审核/已审核/已结算 |
| startTime | String | 否 | 开始时间（格式：YYYY-MM-DD HH:mm:ss） |
| endTime | String | 否 | 结束时间（格式：YYYY-MM-DD HH:mm:ss） |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "warehousingId": 1,
        "warehousingNo": "RKD-20250120-0001",
        "weighingSlipNo": "ZB-20250120-00001",
        "pickupNoticeCode": "SY-20250115-00001",
        "dispatchCode": "YS-20250120-00001",
        "status": "待审核",
        "warehousingTime": "2025-01-20T14:00:00",
        "warehouseKeeperName": "张三",
        "createTime": "2025-01-20T14:00:00"
      }
    ],
    "total": 100,
    "size": 20,
    "current": 1,
    "pages": 5
  }
}
```

---

### 获取入库单详情

**接口地址：** `GET /api/warehouse/inbound/detail`  
**请求方式：** GET  
**功能描述：** 根据入库单编号或入库单号获取入库单详细信息

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| warehousingId | Integer | 否 | 入库单编号（与warehousingNo至少提供一个） |
| warehousingNo | String | 否 | 入库单号（与warehousingId至少提供一个） |

#### 响应参数

```json
{
  "code": 200,
  "message": "获取入库单详情成功",
  "data": {
    "warehousingId": 1,
    "warehousingNo": "RKD-20250120-0001",
    "weighingSlipNo": "ZB-20250120-00001",
    "pickupNoticeCode": "SY-20250115-00001",
    "dispatchCode": "YS-20250120-00001",
    "status": "待审核",
    "warehousingTime": "2025-01-20T14:00:00",
    "warehouseKeeperId": 1001,
    "warehouseKeeperName": "张三",
    "remark": "入库备注",
    "items": [
      {
        "warehousingItemId": 1,
        "pickupNoticeItemId": 1,
        "wasteCategory": "HW01",
        "wasteCode": "841-001-01",
        "wasteName": "感染性废物",
        "plannedQty": 10.00,
        "actualQty": 10.50,
        "differenceReason": "实际接收数量与计划数量一致",
        "unit": "吨"
      }
    ],
    "createTime": "2025-01-20T14:00:00",
    "updateTime": "2025-01-20T14:00:00"
  }
}
```

---

### 更新入库单

**接口地址：** `POST /api/warehouse/inbound/update`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新已存在的入库单信息（仅限待审核状态）

#### 请求参数

```json
{
  "warehousingNo": "RKD-20250120-0001",
  "warehousingTime": "2025-01-20 15:00:00",
  "warehouseKeeperId": 1001,
  "remark": "更新后的备注",
  "items": [
    {
      "warehousingItemId": 1,
      "actualQty": 10.80,
      "differenceReason": "更新后的差异原因"
    }
  ]
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| warehousingNo | String | 是 | 入库单号 |
| warehousingTime | String | 否 | 入库时间（格式：yyyy-MM-dd HH:mm:ss） |
| warehouseKeeperId | Integer | 否 | 仓管员编码 |
| remark | String | 否 | 入库备注 |
| items | Array | 否 | 危废明细列表 |
| items[].warehousingItemId | Integer | 是 | 入库单明细编号 |
| items[].actualQty | BigDecimal | 否 | 实际收运数量（吨） |
| items[].differenceReason | String | 否 | 差异原因 |

#### 响应参数

```json
{
  "code": 200,
  "message": "更新入库单成功",
  "data": {
    "warehousingId": 1,
    "warehousingNo": "RKD-20250120-0001",
    "status": "待审核",
    ...
  }
}
```

---

### 审核入库单

**接口地址：** `POST /api/warehouse/inbound/audit`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 审核入库单，支持通过或拒绝，可填写审核意见。只有待审核状态的入库单才能审核，拒绝时审核意见必填

#### 请求参数

```json
{
  "warehousingNo": "RKD-20250120-0001",
  "auditResult": "通过",
  "auditOpinion": "审核通过"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| warehousingNo | String | 是 | 入库单号 |
| auditResult | String | 是 | 审核结果：通过/拒绝 |
| auditOpinion | String | 否 | 审核意见（拒绝时必填） |

#### 响应参数

```json
{
  "code": 200,
  "message": "审核入库单成功",
  "data": null
}
```

---

### 取消审核入库单

**接口地址：** `POST /api/warehouse/inbound/cancel-audit`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 取消已审核的入库单，使其恢复为待审核状态，可以继续修改信息。只有已审核且未锁定的入库单才能取消审核

#### 请求参数

```json
{
  "warehousingNo": "RKD-20250120-0001",
  "reason": "需要修改入库信息"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| warehousingNo | String | 是 | 入库单号 |
| reason | String | 否 | 取消审核原因 |

#### 响应参数

```json
{
  "code": 200,
  "message": "取消审核成功",
  "data": null
}
```

---

### 删除入库单

**接口地址：** `DELETE /api/warehouse/inbound/{warehousingId}`  
**请求方式：** DELETE  
**功能描述：** 删除入库单及其关联的危废明细。只有未锁定且未审核的入库单才能删除

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| warehousingId | Integer | 是 | 入库单编号 |

#### 响应参数

```json
{
  "code": 200,
  "message": "删除入库单成功",
  "data": null
}
```

---

## 总磅单管理 API

**接口名称：** 总磅单管理服务  
**功能描述：** 提供总磅单的创建、查询、更新、分页查询等功能  
**接口地址：** `/api/warehouse/inbound/weighing-slip/*`  
**请求方式：** GET / POST

---

### 创建总磅单

**接口地址：** `POST /api/warehouse/inbound/weighing-slip/create`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 创建新的总磅单，支持关联多个运输单号，后端自动设置状态为"待细分"

#### 请求参数

```json
{
  "sequence": "001",
  "date": "2025-01-20",
  "firstWeighTime": "2025-01-20 08:00:00",
  "secondWeighTime": "2025-01-20 10:00:00",
  "plateNo": "粤E70123",
  "grossWeight": 50000.0,
  "tareWeight": 10000.0,
  "netWeight": 40000.0,
  "photoUrl": "/api/file/download?path=weighing-slip/2025/01/20/xxx.jpg",
  "dispatchCodes": ["YS-20250120-00001", "YS-20250120-00002"],
  "remark": "总磅备注"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sequence | String | 否 | 序号（手动输入，可选） |
| date | String | 是 | 日期（总磅日期，格式：yyyy-MM-dd） |
| firstWeighTime | String | 否 | 一次过秤时间（格式：yyyy-MM-dd HH:mm:ss） |
| secondWeighTime | String | 否 | 二次过秤时间（格式：yyyy-MM-dd HH:mm:ss） |
| plateNo | String | 是 | 车号（如：粤E70123） |
| grossWeight | Double | 否 | 总重（kg） |
| tareWeight | Double | 否 | 空重（kg） |
| netWeight | Double | 否 | 净重（kg），如果不提供则自动计算（总重-空重） |
| photoUrl | String | 否 | 总磅单照片URL（通过文件上传接口获取） |
| dispatchCodes | Array | 是 | 关联运输单号列表（至少一个） |
| remark | String | 否 | 总磅备注 |

#### 响应参数

```json
{
  "code": 200,
  "message": "创建总磅单成功",
  "data": {
    "weighingSlipId": 1,
    "weighingSlipNo": "ZB-20250120-00001",
    "sequence": "001",
    "date": "2025-01-20",
    "plateNo": "粤E70123",
    "grossWeight": 50000.0,
    "tareWeight": 10000.0,
    "netWeight": 40000.0,
    "status": "待细分",
    "dispatchCodes": ["YS-20250120-00001", "YS-20250120-00002"],
    "createTime": "2025-01-20T08:00:00"
  }
}
```

---

### 获取总磅单信息

**接口地址：** `GET /api/warehouse/inbound/weighing-slip/info`  
**请求方式：** GET  
**功能描述：** 根据总磅单号获取总磅单详细信息，包括关联的运输单号列表和收运通知单列表

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| weighingSlipNo | String | 是 | 总磅单号 |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "weighingSlipId": 1,
    "weighingSlipNo": "ZB-20250120-00001",
    "sequence": "001",
    "date": "2025-01-20",
    "firstWeighTime": "2025-01-20T08:00:00",
    "secondWeighTime": "2025-01-20T10:00:00",
    "plateNo": "粤E70123",
    "grossWeight": 50000.0,
    "tareWeight": 10000.0,
    "netWeight": 40000.0,
    "photoUrl": "/api/file/download?path=weighing-slip/2025/01/20/xxx.jpg",
    "status": "待细分",
    "dispatchCodes": ["YS-20250120-00001", "YS-20250120-00002"],
    "pickupNoticeList": [
      {
        "pickupNoticeCode": "SY-20250115-00001",
        "dispatchCode": "YS-20250120-00001",
        "items": [
          {
            "pickupNoticeItemId": 1,
            "wasteCategory": "HW01",
            "wasteCode": "841-001-01",
            "wasteName": "感染性废物",
            "plannedQty": 10.00,
            "unit": "吨"
          }
        ]
      }
    ],
    "remark": "总磅备注",
    "createTime": "2025-01-20T08:00:00",
    "updateTime": "2025-01-20T08:00:00"
  }
}
```

---

### 更新总磅单

**接口地址：** `POST /api/warehouse/inbound/weighing-slip/update`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新总磅单信息，支持修改基本信息、称重信息、关联运输单等，已细分状态不能修改

#### 请求参数

```json
{
  "weighingSlipNo": "ZB-20250120-00001",
  "sequence": "002",
  "date": "2025-01-20",
  "plateNo": "粤E70123",
  "grossWeight": 51000.0,
  "tareWeight": 10000.0,
  "netWeight": 41000.0,
  "photoUrl": "/api/file/download?path=weighing-slip/2025/01/20/xxx.jpg",
  "status": "待细分",
  "dispatchCodes": ["YS-20250120-00001", "YS-20250120-00002"],
  "remark": "更新后的备注"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| weighingSlipNo | String | 是 | 总磅单号 |
| sequence | String | 否 | 序号 |
| date | String | 否 | 日期（格式：yyyy-MM-dd） |
| plateNo | String | 否 | 车号 |
| grossWeight | Double | 否 | 总重（kg） |
| tareWeight | Double | 否 | 空重（kg） |
| netWeight | Double | 否 | 净重（kg） |
| photoUrl | String | 否 | 总磅单照片URL |
| status | String | 否 | 状态：待细分/已细分 |
| dispatchCodes | Array | 否 | 关联运输单号列表 |
| remark | String | 否 | 总磅备注 |

#### 响应参数

```json
{
  "code": 200,
  "message": "更新总磅单成功",
  "data": {
    "weighingSlipId": 1,
    "weighingSlipNo": "ZB-20250120-00001",
    ...
  }
}
```

---

### 分页查询总磅单列表

**接口地址：** `GET /api/warehouse/inbound/weighing-slip/list`  
**请求方式：** GET  
**功能描述：** 支持按关键字、总磅单号、序号、车号、状态、日期范围等条件筛选

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Long | 否 | 当前页码（默认1） |
| size | Long | 否 | 每页数量（默认10） |
| keyword | String | 否 | 关键字（总磅单号/序号/车号，模糊查询） |
| weighingSlipNo | String | 否 | 总磅单号（模糊查询） |
| sequence | String | 否 | 序号（模糊查询） |
| plateNo | String | 否 | 车号（模糊查询） |
| status | String | 否 | 状态：待细分/已细分 |
| startDate | String | 否 | 开始日期（格式：yyyy-MM-dd） |
| endDate | String | 否 | 结束日期（格式：yyyy-MM-dd） |

#### 响应参数

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "weighingSlipId": 1,
        "weighingSlipNo": "ZB-20250120-00001",
        "sequence": "001",
        "date": "2025-01-20",
        "plateNo": "粤E70123",
        "grossWeight": 50000.0,
        "tareWeight": 10000.0,
        "netWeight": 40000.0,
        "status": "待细分",
        "createTime": "2025-01-20T08:00:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

## 财务管理 API

**接口名称：** 财务管理服务
**功能描述：** 提供财务结算相关的功能，包括获取可结算入库单、获取入库单危废明细等
**接口地址：** `/api/finance/*`
**请求方式：** GET / POST

---

### 获取合同关联的可结算入库单

**接口地址：** `GET /api/finance/settlements/available-warehousing/{contractCode}`
**请求方式：** GET
**功能描述：** 根据合同号获取关联的通知单→运输单→入库单，且这些入库单未被其他结算单引用

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractCode | String | 是 | 合同号 |

#### 响应示例

```json
{
  "code": 200,
  "message": "获取可结算入库单成功",
  "data": [
    {
      "warehousingId": 1,
      "warehousingNo": "RKD-20250120-0001",
      "pickupNoticeCode": "SY-20250115-00001",
      "dispatchCode": "YS-20250120-00001",
      "contractCode": "HT-20250115-00001",
      "warehousingTime": "2025-01-20T14:00:00",
      "totalQty": 10.5,
      "warehouseKeeperName": "李四",
      "status": "已审核"
    }
  ],
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 获取入库单对应的危废明细

**接口地址：** `POST /api/finance/warehousing-waste-details`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 根据入库单号列表获取对应的危废明细，用于结算单合并

#### 请求参数

```json
[
  "RKD-20250120-0001",
  "RKD-20250120-0002"
]
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| - | Array<String> | 是 | 入库单号列表 |

#### 响应示例

```json
{
  "code": 200,
  "message": "获取入库单危废明细成功",
  "data": [
    {
      "warehousingNo": "RKD-20250120-0001",
      "wasteItemId": 1,
      "wasteCode": "HW01",
      "wasteName": "医疗废物",
      "actualQty": 5.5,
      "unitPrice": 3500.00,
      "totalAmount": 19250.00,
      "pricingMethod": "吨",
      "hazardousWasteCategory": "HW01 医疗废物"
    }
  ],
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 获取合同关联的可结算运输记录

**接口地址：** `GET /api/finance/transports/{contractCode}`
**请求方式：** GET
**功能描述：** 根据合同号获取关联的通知单→运输单，且这些运输单未被其他结算单引用

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractCode | String | 是 | 合同号 |

#### 响应示例

```json
{
  "code": 200,
  "message": "获取运输记录成功",
  "data": {
    "transportRecords": [
      {
        "transportId": 1,
        "dispatchCode": "YS-20250120-00001",
        "pickupNoticeCode": "SY-20250115-00001",
        "contractCode": "HT-20250115-00001",
        "dispatchTime": "2025-01-20T08:00:00",
        "status": "已完成"
      }
    ]
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 获取累积已结算量和合同计划总量

**接口地址：** `GET /api/finance/settlements/accumulated-quantity/{contractCode}/{wasteCategory}`
**请求方式：** GET
**功能描述：** 根据合同号和废物类别获取累积已结算量和合同计划总量

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractCode | String | 是 | 合同号 |
| wasteCategory | String | 是 | 废物类别 |

#### 响应示例

```json
{
  "code": 200,
  "message": "获取累积已结算量成功",
  "data": {
    "contractCode": "HT-20250115-00001",
    "wasteCategory": "HW01",
    "accumulatedQuantity": 50.5,
    "plannedQuantity": 100.0,
    "remainingQuantity": 49.5
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 创建结算单

**接口地址：** `POST /api/finance/settlements`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 根据合同数据创建结算单，支持根据付款方自动生成收款/付款结算单

#### 请求参数

```json
{
  "contractCode": "HT-20250115-00001",
  "settlementType": "收款结算单",
  "settlementPeriod": "2025-01",
  "warehousingCodes": ["RKD-20250120-0001", "RKD-20250120-0002"],
  "remark": "结算备注"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractCode | String | 是 | 合同号 |
| settlementType | String | 是 | 结算类型：收款结算单/付款结算单 |
| settlementPeriod | String | 是 | 结算周期（格式：YYYY-MM） |
| warehousingCodes | Array<String> | 是 | 入库单号列表 |
| remark | String | 否 | 结算备注 |

#### 响应示例

```json
{
  "code": 200,
  "message": "创建结算单成功",
  "data": {
    "settlementId": 1,
    "settlementNo": "JSD-20250120-0001",
    "contractCode": "HT-20250115-00001",
    "settlementType": "收款结算单",
    "totalAmount": 50000.00,
    "status": "待审核"
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 获取结算单详情

**接口地址：** `GET /api/finance/settlements/{settlementId}`
**请求方式：** GET
**功能描述：** 根据结算单ID获取完整的结算单信息，包括明细和价外服务

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| settlementId | Long | 是 | 结算单ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "获取结算单详情成功",
  "data": {
    "settlementId": 1,
    "settlementNo": "JSD-20250120-0001",
    "contractCode": "HT-20250115-00001",
    "settlementType": "收款结算单",
    "totalAmount": 50000.00,
    "status": "待审核",
    "items": [
      {
        "wasteCode": "HW01",
        "wasteName": "医疗废物",
        "quantity": 10.5,
        "unitPrice": 3500.00,
        "amount": 36750.00
      }
    ],
    "outOfScopeServices": []
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 审核结算单

**接口地址：** `POST /api/finance/settlements/{settlementId}/audit`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 审核结算单，更新状态为已审核或已驳回

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| settlementId | Long | 是 | 结算单ID |

#### 请求参数

```json
{
  "auditResult": "通过",
  "auditOpinion": "审核通过"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| auditResult | String | 是 | 审核结果：通过/驳回 |
| auditOpinion | String | 否 | 审核意见 |

#### 响应示例

```json
{
  "code": 200,
  "message": "审核结算单成功",
  "data": null,
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 结算单分页查询

**接口地址：** `GET /api/finance/settlements/list`
**请求方式：** GET
**功能描述：** 支持按结算类型、结算单单号、合同号、客户名称、状态、制单人、结算周期、创建时间等条件筛选

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码（默认1） |
| size | Integer | 否 | 每页数量（默认20） |
| settlementType | String | 否 | 结算类型：收款结算单/付款结算单 |
| settlementNo | String | 否 | 结算单单号（模糊查询） |
| contractCode | String | 否 | 合同号（模糊查询） |
| customerName | String | 否 | 客户名称（模糊查询） |
| status | String | 否 | 状态：待审核/已审核/已驳回 |
| creatorName | String | 否 | 制单人（模糊查询） |
| settlementPeriod | String | 否 | 结算周期（格式：YYYY-MM） |
| startTime | String | 否 | 开始时间（格式：YYYY-MM-DD HH:mm:ss） |
| endTime | String | 否 | 结束时间（格式：YYYY-MM-DD HH:mm:ss） |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "settlementId": 1,
        "settlementNo": "JSD-20250120-0001",
        "contractCode": "HT-20250115-00001",
        "settlementType": "收款结算单",
        "totalAmount": 50000.00,
        "status": "待审核",
        "creatorName": "张三",
        "createTime": "2025-01-20T10:00:00"
      }
    ],
    "total": 100,
    "size": 20,
    "current": 1,
    "pages": 5
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 获取结算统计信息

**接口地址：** `GET /api/finance/settlement-statistics`
**请求方式：** GET
**功能描述：** 根据结算类型获取结算单的统计信息（总数、总金额、已付金额、未付金额、逾期数量等）

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| settlementType | String | 否 | 结算类型：收款结算单/付款结算单（不传则统计全部） |

#### 响应示例

```json
{
  "code": 200,
  "message": "获取统计信息成功",
  "data": {
    "totalCount": 100,
    "totalAmount": 5000000.00,
    "paidAmount": 3000000.00,
    "unpaidAmount": 2000000.00,
    "overdueCount": 5,
    "overdueAmount": 500000.00
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 查询已结算入库量

**接口地址：** `POST /api/finance/settlements/settled-warehousing-quantity`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 根据合同号和废物信息查询已结算的入库量

#### 请求参数

```json
{
  "contractCode": "HT-20250115-00001",
  "wasteCode": "HW01",
  "wasteCategory": "HW01"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractCode | String | 是 | 合同号 |
| wasteCode | String | 否 | 废物代码 |
| wasteCategory | String | 否 | 废物类别 |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询已结算入库量成功",
  "data": {
    "contractCode": "HT-20250115-00001",
    "settledQuantity": 50.5,
    "warehousingList": [
      {
        "warehousingNo": "RKD-20250120-0001",
        "quantity": 10.5
      }
    ]
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

## 发票管理 API

**接口名称：** 发票管理服务
**功能描述：** 提供发票的创建、更新、查询、批量导入、文件补充等功能
**接口地址：** `/api/finance/invoice/*`
**请求方式：** GET / POST / PUT

---

### 批量导入发票（ZIP文件）

**接口地址：** `POST /api/finance/invoice/batch-import`
**请求方式：** POST
**Content-Type：** `multipart/form-data`
**功能描述：** 上传ZIP压缩包，批量导入发票数据，支持XML解析和数据验证

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | ZIP格式文件，大小不超过100MB |
| invoiceStatus | String | 是 | 发票状态：进项发票/销项发票 |

#### 数据验证规则

1. **文件格式验证**：只支持ZIP格式，文件大小不超过100MB
2. **发票状态验证**：必须是"进项发票"或"销项发票"
3. **公司信息验证**：
   - 进项发票：验证购买方名称和购买方统一社会信用代码是否与系统配置（INVOICE_COMPANY_INFO）一致
   - 销项发票：验证销售方名称和销售方统一社会信用代码是否与系统配置（INVOICE_COMPANY_INFO）一致
   - 如果验证失败，整个导入失败并回滚所有操作

#### 响应示例

```json
{
  "code": 200,
  "message": "批量导入成功",
  "data": {
    "total": 100,
    "success": 95,
    "error": 5,
    "errorDetails": [
      {
        "fileName": "invoice001.xml",
        "error": "公司信息验证失败"
      }
    ]
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### Excel导入发票

**接口地址：** `POST /api/finance/invoice/excel-import`
**请求方式：** POST
**Content-Type：** `multipart/form-data`
**功能描述：** 上传Excel文件，解析发票数据并批量导入

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| excelFile | MultipartFile | 是 | Excel格式文件（.xlsx、.xls），大小不超过100MB |
| invoiceStatus | String | 是 | 发票状态：进项发票/销项发票 |

#### 数据验证与事务规则

1. **文件格式验证**：只支持Excel格式（.xlsx、.xls），文件大小不超过100MB
2. **发票状态验证**：必须是"进项发票"或"销项发票"
3. **公司信息验证**：
   - 进项发票：验证购买方名称和购买方统一社会信用代码是否与系统配置（INVOICE_COMPANY_INFO）一致
   - 销项发票：验证销售方名称和销售方统一社会信用代码是否与系统配置（INVOICE_COMPANY_INFO）一致
4. **导入事务规则**：只要当前Excel中存在任意一张发票解析/校验/入库失败，本次导入操作将整体回滚，不会写入任何发票或明细数据

#### 响应示例

```json
{
  "code": 200,
  "message": "Excel导入成功",
  "data": {
    "total": 50,
    "success": 50,
    "error": 0
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 批量文件补充

**接口地址：** `POST /api/finance/invoice/file-supplement`
**请求方式：** POST
**Content-Type：** `multipart/form-data`
**功能描述：** 上传多个文件，根据文件名中的发票号匹配已存在的发票记录，补充文件附件

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| files | MultipartFile[] | 是 | 文件列表（最多50个） |
| invoiceStatus | String | 是 | 发票状态：进项发票/销项发票 |

#### 规则说明

1. **文件数量限制**：单次最多50个，超出直接返回错误
2. **文件类型限制**：仅支持 PDF/OFD/XML/图片（jpg、jpeg、png、gif、bmp）
3. **全量回滚**：任意文件失败则整体回滚

#### 响应示例

```json
{
  "code": 200,
  "message": "文件补充成功",
  "data": {
    "total": 10,
    "success": 10,
    "error": 0
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 创建发票

**接口地址：** `POST /api/finance/invoice`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 创建发票记录

#### 请求参数

```json
{
  "invoiceNumber": "12345678",
  "invoiceCode": "1234567890",
  "invoiceType": "增值税专用发票",
  "invoiceForm": "数电发票",
  "invoiceNature": "蓝字",
  "invoiceStatus": "进项发票",
  "invoiceDate": "2025-01-20",
  "sellerName": "XX公司",
  "sellerCreditCode": "91110000MA01234567",
  "buyerName": "YY公司",
  "buyerCreditCode": "91110000MA09876543",
  "totalAmount": 10000.00,
  "taxAmount": 1300.00,
  "amountInWords": "壹万元整",
  "items": [
    {
      "itemName": "服务费",
      "specification": "标准",
      "unit": "项",
      "quantity": 1.0,
      "unitPrice": 10000.00,
      "amount": 10000.00,
      "taxRate": 0.13,
      "taxAmount": 1300.00
    }
  ]
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "发票创建成功",
  "data": {
    "invoiceId": 1,
    "invoiceNumber": "12345678"
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 发票分页查询

**接口地址：** `GET /api/finance/invoice/list`
**请求方式：** GET
**功能描述：** 根据发票状态（进项发票/销项发票）以及其他筛选条件分页查询发票列表

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码（默认1） |
| size | Integer | 否 | 每页数量（默认20） |
| invoiceStatus | String | 是 | 发票状态（进项发票/销项发票，必填） |
| invoiceType | String | 否 | 发票类型（增值税专用发票/普通发票） |
| invoiceForm | String | 否 | 发票形式（数电发票/电子发票/纸质发票） |
| invoiceNature | String | 否 | 发票性质（红字/蓝字） |
| status | String | 否 | 业务状态（已开具/已作废） |
| invoiceDateStart | String | 否 | 开票日期开始（格式：YYYY-MM-DD） |
| invoiceDateEnd | String | 否 | 开票日期结束（格式：YYYY-MM-DD） |
| invoiceNumber | String | 否 | 发票号码（模糊查询） |
| sellerName | String | 否 | 销售方名称（模糊查询） |
| sellerCreditCode | String | 否 | 销售方统一社会信用代码（模糊查询） |
| buyerName | String | 否 | 购买方名称（模糊查询） |
| buyerCreditCode | String | 否 | 购买方统一社会信用代码（模糊查询） |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "invoiceId": 1,
        "invoiceNumber": "12345678",
        "invoiceCode": "1234567890",
        "invoiceType": "增值税专用发票",
        "invoiceForm": "数电发票",
        "invoiceStatus": "进项发票",
        "invoiceDate": "2025-01-20",
        "totalAmount": 10000.00,
        "status": "已开具"
      }
    ],
    "total": 100,
    "size": 20,
    "current": 1,
    "pages": 5
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 解析数电发票XML文件

**接口地址：** `POST /api/finance/invoice/parse-file`
**请求方式：** POST
**Content-Type：** `multipart/form-data`
**功能描述：** 上传单个XML文件，后端解析发票基础信息和明细并回填到前端表单，同时将XML文件上传到FILE表并返回对应文件编号

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | XML格式文件，大小不超过10MB |

#### 响应示例

```json
{
  "code": 200,
  "message": "解析成功",
  "data": {
    "invoiceNumber": "12345678",
    "invoiceCode": "1234567890",
    "invoiceType": "增值税专用发票",
    "invoiceForm": "数电发票",
    "invoiceDate": "2025-01-20",
    "sellerName": "XX公司",
    "sellerCreditCode": "91110000MA01234567",
    "buyerName": "YY公司",
    "buyerCreditCode": "91110000MA09876543",
    "totalAmount": 10000.00,
    "taxAmount": 1300.00,
    "items": [
      {
        "itemName": "服务费",
        "quantity": 1.0,
        "unitPrice": 10000.00,
        "amount": 10000.00
      }
    ],
    "xmlFileId": 1
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 校验数电发票XML文件

**接口地址：** `POST /api/finance/invoice/validate-xml`
**请求方式：** POST
**Content-Type：** `multipart/form-data`
**功能描述：** 上传XML文件，解析发票数据用于前端比对，不进行任何入库操作

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | XML格式文件，大小不超过10MB |

#### 响应示例

```json
{
  "code": 200,
  "message": "校验成功",
  "data": {
    "invoiceNumber": "12345678",
    "invoiceCode": "1234567890",
    "invoiceType": "增值税专用发票",
    "totalAmount": 10000.00
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 获取发票详情

**接口地址：** `GET /api/finance/invoice/{invoiceId}`
**请求方式：** GET
**功能描述：** 根据发票ID查询发票详情，包含所有字段和明细

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| invoiceId | Integer | 是 | 发票ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "invoiceId": 1,
    "invoiceNumber": "12345678",
    "invoiceCode": "1234567890",
    "invoiceType": "增值税专用发票",
    "invoiceForm": "数电发票",
    "invoiceStatus": "进项发票",
    "invoiceDate": "2025-01-20",
    "sellerName": "XX公司",
    "sellerCreditCode": "91110000MA01234567",
    "buyerName": "YY公司",
    "buyerCreditCode": "91110000MA09876543",
    "totalAmount": 10000.00,
    "taxAmount": 1300.00,
    "items": [
      {
        "itemName": "服务费",
        "specification": "标准",
        "unit": "项",
        "quantity": 1.0,
        "unitPrice": 10000.00,
        "amount": 10000.00,
        "taxRate": 0.13,
        "taxAmount": 1300.00
      }
    ]
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 更新发票

**接口地址：** `PUT /api/finance/invoice/{invoiceId}`
**请求方式：** PUT
**Content-Type：** `application/json`
**功能描述：** 更新发票记录，包含所有字段和明细

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| invoiceId | Integer | 是 | 发票ID |

#### 请求参数

参考创建发票接口的请求参数格式

#### 响应示例

```json
{
  "code": 200,
  "message": "发票更新成功",
  "data": {
    "invoiceId": 1,
    "invoiceNumber": "12345678"
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 查询发票文件编号

**接口地址：** `GET /api/finance/invoice/{invoiceId}/files`
**请求方式：** GET
**功能描述：** 查询发票是否有PDF、OFD、XML、图片文件

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| invoiceId | Integer | 是 | 发票ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "invoiceId": 1,
    "pdfFileId": 1,
    "ofdFileId": null,
    "xmlFileId": 2,
    "imageFileId": null
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

## 开票通知单管理 API

**接口名称：** 开票通知单管理服务
**功能描述：** 提供开票通知单的创建、更新、查询、提交审批、审批通过、驳回、取消等功能
**接口地址：** `/api/finance/invoice-notices/*`
**请求方式：** GET / POST

---

### 获取开票通知单分页列表

**接口地址：** `GET /api/finance/invoice-notices`
**请求方式：** GET
**功能描述：** 分页查询开票通知单列表，支持多条件筛选

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码（默认1） |
| size | Integer | 否 | 每页数量（默认20） |
| noticeNo | String | 否 | 通知单号（模糊查询） |
| contractCode | String | 否 | 合同号（模糊查询） |
| customerName | String | 否 | 客户名称（模糊查询） |
| status | String | 否 | 状态：草稿/待审批/待开票/已开票/已驳回/已取消 |
| startTime | String | 否 | 开始时间（格式：YYYY-MM-DD HH:mm:ss） |
| endTime | String | 否 | 结束时间（格式：YYYY-MM-DD HH:mm:ss） |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "noticeId": 1,
        "noticeNo": "KPTZ-20250120-0001",
        "contractCode": "HT-20250115-00001",
        "customerName": "XX公司",
        "status": "待审批",
        "totalAmount": 50000.00,
        "createTime": "2025-01-20T10:00:00"
      }
    ],
    "total": 100,
    "size": 20,
    "current": 1,
    "pages": 5
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 获取开票通知单详情

**接口地址：** `GET /api/finance/invoice-notices/{noticeId}`
**请求方式：** GET
**功能描述：** 根据通知单ID查询开票通知单详细信息

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeId | Integer | 是 | 通知单ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "noticeId": 1,
    "noticeNo": "KPTZ-20250120-0001",
    "contractCode": "HT-20250115-00001",
    "customerName": "XX公司",
    "status": "待审批",
    "totalAmount": 50000.00,
    "items": [
      {
        "itemName": "服务费",
        "quantity": 10.0,
        "unitPrice": 5000.00,
        "amount": 50000.00
      }
    ],
    "createTime": "2025-01-20T10:00:00",
    "updateTime": "2025-01-20T10:00:00"
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 创建开票通知单

**接口地址：** `POST /api/finance/invoice-notices`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 创建新的开票通知单，支持创建草稿或直接提交审批

#### 请求参数

```json
{
  "contractCode": "HT-20250115-00001",
  "customerName": "XX公司",
  "status": "草稿",
  "remark": "开票备注",
  "items": [
    {
      "itemName": "服务费",
      "quantity": 10.0,
      "unitPrice": 5000.00,
      "amount": 50000.00
    }
  ]
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| contractCode | String | 是 | 合同号 |
| customerName | String | 是 | 客户名称 |
| status | String | 否 | 状态：草稿/待审批（默认为"草稿"，传入"待审批"时直接创建为待审批状态） |
| remark | String | 否 | 备注 |
| items | Array | 是 | 开票明细列表 |

#### 响应示例

```json
{
  "code": 200,
  "message": "保存草稿成功",
  "data": {
    "noticeId": 1,
    "noticeNo": "KPTZ-20250120-0001"
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 更新开票通知单

**接口地址：** `POST /api/finance/invoice-notices/{noticeId}/update`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 更新草稿或驳回状态的通知单

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeId | Integer | 是 | 通知单ID |

#### 请求参数

参考创建开票通知单接口的请求参数格式

#### 响应示例

```json
{
  "code": 200,
  "message": "更新成功",
  "data": null,
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 提交审批

**接口地址：** `POST /api/finance/invoice-notices/{noticeId}/submit`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 提交开票通知单进行审批

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeId | Integer | 是 | 通知单ID |

#### 请求参数

```json
{
  "remark": "提交审批备注"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| remark | String | 否 | 提交备注 |

#### 响应示例

```json
{
  "code": 200,
  "message": "提交审批成功",
  "data": null,
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 取消/关闭开票通知单

**接口地址：** `POST /api/finance/invoice-notices/{noticeId}/cancel`
**请求方式：** POST
**功能描述：** 取消或关闭开票通知单（仅草稿、驳回、已开票允许）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeId | Integer | 是 | 通知单ID |

#### 响应示例

```json
{
  "code": 200,
  "message": "取消成功",
  "data": null,
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 审批通过开票通知单

**接口地址：** `POST /api/finance/invoice-notices/{noticeId}/approve`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 审批通过开票通知单，更新状态为待开票，并保存审批意见

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeId | Integer | 是 | 通知单ID |

#### 请求参数

```json
{
  "opinion": "审批通过"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| opinion | String | 否 | 审批意见 |

#### 响应示例

```json
{
  "code": 200,
  "message": "审批通过成功",
  "data": null,
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 驳回开票通知单

**接口地址：** `POST /api/finance/invoice-notices/{noticeId}/reject`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 驳回开票通知单，更新状态为已驳回，并保存驳回原因

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeId | Integer | 是 | 通知单ID |

#### 请求参数

```json
{
  "reason": "信息不完整，需要补充"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reason | String | 是 | 驳回原因 |

#### 响应示例

```json
{
  "code": 200,
  "message": "驳回成功",
  "data": null,
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 获取发票关联列表

**接口地址：** `GET /api/finance/invoice-notices/associate`
**请求方式：** GET
**功能描述：** 查询待开票和已开票状态的开票通知单列表，用于发票关联操作

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码（默认1） |
| size | Integer | 否 | 每页数量（默认20） |
| noticeNo | String | 否 | 通知单号（模糊查询） |
| contractCode | String | 否 | 合同号（模糊查询） |
| customerName | String | 否 | 客户名称（模糊查询） |

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [
      {
        "noticeId": 1,
        "noticeNo": "KPTZ-20250120-0001",
        "contractCode": "HT-20250115-00001",
        "customerName": "XX公司",
        "status": "待开票",
        "totalAmount": 50000.00
      }
    ],
    "total": 50,
    "size": 20,
    "current": 1,
    "pages": 3
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 关联发票

**接口地址：** `POST /api/finance/invoice-notices/{noticeId}/associate`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 将已存在的发票关联到开票通知单，支持差分保存（新增该新增的，删除该删除的）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeId | Integer | 是 | 通知单ID |

#### 请求参数

```json
{
  "invoiceIds": [1, 2, 3]
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| invoiceIds | Array<Integer> | 是 | 发票ID列表 |

#### 响应示例

```json
{
  "code": 200,
  "message": "关联发票成功",
  "data": null,
  "timestamp": "2025-01-20T12:00:00"
}
```

---

### 完成关联

**接口地址：** `POST /api/finance/invoice-notices/{noticeId}/complete`
**请求方式：** POST
**Content-Type：** `application/json`
**功能描述：** 完成发票关联，更新通知单状态为已开票，并回填已开票张数和金额汇总

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| noticeId | Integer | 是 | 通知单ID |

#### 请求参数

```json
{}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "完成关联成功",
  "data": null,
  "timestamp": "2025-01-20T12:00:00"
}
```

---

## 资金管理 API

### 资金账户管理 API

**接口名称：** 资金账户管理服务  
**功能描述：** 提供资金账户的创建、查询、更新、删除等功能  
**接口地址：** `/api/fund/accounts/*`  
**请求方式：** GET / POST / DELETE

---

#### 创建资金账户

**接口地址：** `POST /api/fund/accounts`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 创建新的资金账户（银行账户/备用金账户/现金账户）

#### 请求参数

```json
{
  "account_name": "工商银行",
  "account_type": "BANK",
  "account_bank_account": "6222021234567890123",
  "account_bank_institution": "工商银行XX支行",
  "remark": "备注信息"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| account_name | String | 是 | 账户名称 |
| account_type | String | 是 | 账户类型：BANK（银行账户）/PETTY_CASH（备用金账户）/CASH（现金账户） |
| account_bank_account | String | 否 | 银行账号 |
| account_bank_institution | String | 否 | 开户机构 |
| remark | String | 否 | 备注 |

#### 响应示例

```json
{
  "code": 200,
  "message": "创建账户成功",
  "data": {
    "account_id": 1,
    "account_code": "ACC-001"
  },
  "timestamp": "2025-01-20T12:00:00"
}
```

---

#### 分页查询资金账户列表

**接口地址：** `POST /api/fund/accounts/page`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 按账户名称、账户类型、启用状态筛选资金账户，支持字段排序，返回创建人/更新人姓名

#### 请求参数

```json
{
  "accountName": "工行",
  "accountType": "BANK",
  "isEnabled": true,
  "current": 1,
  "size": 10,
  "sortField": "createTime",
  "sortOrder": "desc"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| accountName | String | 否 | 账户名称（模糊查询） |
| accountType | String | 否 | 账户类型 |
| isEnabled | Boolean | 否 | 启用状态 |
| current | Integer | 否 | 当前页码（默认1） |
| size | Integer | 否 | 每页数量（默认10） |
| sortField | String | 否 | 排序字段 |
| sortOrder | String | 否 | 排序方向：asc/desc |

---

#### 查询资金账户详情

**接口地址：** `GET /api/fund/accounts/{accountId}`  
**请求方式：** GET  
**功能描述：** 根据账户ID查询资金账户详细信息

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| accountId | Long | 是 | 账户ID |

---

#### 更新资金账户

**接口地址：** `POST /api/fund/accounts/{accountId}/update`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新资金账户基础信息（不修改创建人编码和创建时间）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| accountId | Long | 是 | 账户ID |

#### 请求参数

参考创建资金账户接口的请求参数格式

---

#### 删除资金账户及其资金流水

**接口地址：** `POST /api/fund/accounts/{accountId}/delete`  
**请求方式：** POST  
**功能描述：** 删除资金账户及其所有资金流水，操作不可恢复

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| accountId | Long | 是 | 账户ID |

---

### 账户组合管理 API

**接口名称：** 账户组合管理服务  
**功能描述：** 提供账户组合的创建、查询、更新、删除等功能  
**接口地址：** `/api/fund/account-groups/*`  
**请求方式：** GET / POST / DELETE

---

#### 获取账户组合列表

**接口地址：** `GET /api/fund/account-groups`  
**请求方式：** GET  
**功能描述：** 查询所有账户组合

---

#### 分页查询账户组合列表

**接口地址：** `POST /api/fund/account-groups/page`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 按组合名称、启用状态筛选账户组合，支持字段排序

#### 请求参数

```json
{
  "groupName": "主账户",
  "isEnabled": true,
  "current": 1,
  "size": 10,
  "sortField": "createTime",
  "sortOrder": "desc"
}
```

---

#### 创建账户组合

**接口地址：** `POST /api/fund/account-groups`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 创建新的账户组合

#### 请求参数

```json
{
  "groupName": "主账户组合",
  "accountIds": [1, 2, 3],
  "enabled": true,
  "remark": "备注信息"
}
```

---

#### 查询账户组合详情

**接口地址：** `GET /api/fund/account-groups/{groupId}`  
**请求方式：** GET  
**功能描述：** 根据组合ID查询账户组合详细信息

---

#### 更新账户组合

**接口地址：** `POST /api/fund/account-groups/{groupId}`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新账户组合信息

---

#### 删除账户组合

**接口地址：** `DELETE /api/fund/account-groups/{groupId}`  
**请求方式：** DELETE  
**功能描述：** 删除账户组合

---

### 账期管理 API

**接口名称：** 账期管理服务  
**功能描述：** 提供账期的创建、查询、更新、删除、初始化等功能  
**接口地址：** `/api/fund/periods/*`  
**请求方式：** GET / POST / DELETE

---

#### 获取账期列表

**接口地址：** `GET /api/fund/periods`  
**请求方式：** GET  
**功能描述：** 查询所有账期，支持按年份筛选（不分页）

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 否 | 年份 |

---

#### 分页查询账期列表

**接口地址：** `POST /api/fund/periods/page`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 分页查询账期，支持年份、结账状态筛选和字段排序

#### 请求参数

```json
{
  "year": 2026,
  "isSettled": false,
  "periodCode": "2026",
  "current": 1,
  "size": 10,
  "sortField": "createTime",
  "sortOrder": "desc"
}
```

---

#### 初始化账期

**接口地址：** `POST /api/fund/periods/init`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 为指定年份创建12个月份的账期

#### 请求参数

```json
{
  "year": 2026,
  "overwrite": false
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| year | Integer | 是 | 年份 |
| overwrite | Boolean | 否 | 是否覆盖已存在的账期（默认false） |

---

#### 创建单个账期

**接口地址：** `POST /api/fund/periods`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 手动创建单个账期，支持非标准账期（如：自定义日期范围）

#### 请求参数

```json
{
  "year": 2026,
  "period": 1,
  "startDate": "2026-01-01",
  "endDate": "2026-01-31"
}
```

---

#### 更新账期

**接口地址：** `POST /api/fund/periods/{periodId}`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新未结账账期的日期范围

#### 请求参数

```json
{
  "startDate": "2026-01-01",
  "endDate": "2026-01-31"
}
```

---

#### 删除账期

**接口地址：** `DELETE /api/fund/periods/{periodId}`  
**请求方式：** DELETE  
**功能描述：** 删除未使用且未结账的账期

---

### 资金流水管理 API

**接口名称：** 资金流水管理服务  
**功能描述：** 提供资金流水的创建、更新、删除等功能（新增/修改/删除日记账）  
**接口地址：** `/api/fund/transactions/*`  
**请求方式：** POST / DELETE

---

#### 创建资金流水

**接口地址：** `POST /api/fund/transactions`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 创建新的资金流水记录（新增日记账）

#### 请求参数

```json
{
  "account_id": 1,
  "period_id": 3,
  "transaction_date": "2023-03-31",
  "transaction_type": "EXPENDITURE",
  "amount": 600.00,
  "counterparty": "供应商A",
  "summary": "支付运费",
  "fund_category": "运费",
  "internal_transfer": false,
  "related_account_id": null,
  "remark": "备注信息"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| account_id | Long | 是 | 账户ID |
| period_id | Long | 是 | 账期ID |
| transaction_date | String | 是 | 交易日期（格式：YYYY-MM-DD） |
| transaction_type | String | 是 | 交易类型：INCOME（收入）/EXPENDITURE（支出） |
| amount | BigDecimal | 是 | 金额 |
| counterparty | String | 否 | 往来单位 |
| summary | String | 否 | 摘要 |
| fund_category | String | 否 | 资金类别 |
| internal_transfer | Boolean | 否 | 是否内部转账 |
| related_account_id | Long | 否 | 关联账户ID（内部转账时必填） |
| remark | String | 否 | 备注 |

---

#### 更新资金流水

**接口地址：** `POST /api/fund/transactions/{transactionId}`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新资金流水记录（修改日记账）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| transactionId | Long | 是 | 交易ID |

#### 请求参数

参考创建资金流水接口的请求参数格式

---

#### 删除资金流水

**接口地址：** `DELETE /api/fund/transactions/{transactionId}`  
**请求方式：** DELETE  
**功能描述：** 删除资金流水记录（删除日记账）

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| transactionId | Long | 是 | 交易ID |

---

### 结账管理 API

**接口名称：** 结账管理服务  
**功能描述：** 提供账期结账、反结账、检查项设置等功能  
**接口地址：** `/api/fund/settlement/*`  
**请求方式：** GET / POST

---

#### 检查并结账

**接口地址：** `POST /api/fund/settlement/check-and-settle`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 检查指定账期的余额，如果通过则结账（目前只实现检查部分）

#### 请求参数

```json
{
  "periodId": 1
}
```

---

#### 反结账

**接口地址：** `POST /api/fund/settlement/reverse`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 反结账指定账期

#### 请求参数

```json
{
  "periodId": 1
}
```

---

#### 获取检查项设置

**接口地址：** `GET /api/fund/settlement/check-items`  
**请求方式：** GET  
**功能描述：** 获取结账检查项设置

---

#### 更新检查项设置

**接口地址：** `POST /api/fund/settlement/check-items`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新结账检查项设置

---

#### 结账

**接口地址：** `POST /api/fund/settlement/settle`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 更新账期为已结账状态，并为下一个账期创建期初余额

#### 请求参数

```json
{
  "periodId": 1
}
```

---

### 日记账管理 API

**接口名称：** 日记账管理服务  
**功能描述：** 提供日记账明细查询、打印等功能  
**接口地址：** `/api/fund/diary/*`  
**请求方式：** GET / POST

---

#### 获取日记账明细

**接口地址：** `GET /api/fund/diary`  
**请求方式：** GET  
**功能描述：** 查询指定账户在指定账期的明细账数据

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| account_id | Long | 是 | 账户ID |
| period_id | Long | 否 | 账期ID（与year+period二选一） |
| year | Integer | 否 | 年份（与period_id二选一） |
| period | Integer | 否 | 期间（与period_id二选一） |
| date_range_start | String | 否 | 日期范围开始（格式：YYYY-MM-DD） |
| date_range_end | String | 否 | 日期范围结束（格式：YYYY-MM-DD） |
| summary | String | 否 | 摘要（模糊查询） |
| counterparty | String | 否 | 往来单位（模糊查询） |

---

#### 打印日记账

**接口地址：** `GET /api/fund/diary/print`  
**请求方式：** GET  
**功能描述：** 生成日记账PDF文件并返回预览URL

#### 请求参数

参考获取日记账明细接口的请求参数

---

#### 连续打印日记账

**接口地址：** `POST /api/fund/diary/batch-print`  
**请求方式：** POST  
**Content-Type：** `application/json`  
**功能描述：** 批量生成多个账户的日记账PDF文件并合并为一个PDF，返回预览URL

#### 请求参数

```json
[
  {
    "account_id": 1,
    "period_id": 3,
    "date_range_start": "2023-03-01",
    "date_range_end": "2023-03-31",
    "summary": "",
    "counterparty": ""
  },
  {
    "account_id": 2,
    "period_id": 3,
    "date_range_start": "2023-03-01",
    "date_range_end": "2023-03-31",
    "summary": "",
    "counterparty": ""
  }
]
```

---

### 汇总表管理 API

**接口名称：** 汇总表管理服务  
**功能描述：** 提供资金汇总表查询、导出等功能  
**接口地址：** `/api/fund/summary/*`  
**请求方式：** GET

---

#### 获取汇总表数据

**接口地址：** `GET /api/fund/summary`  
**请求方式：** GET  
**功能描述：** 查询多个账户或账户组合在指定账期的汇总数据

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| account_ids | List<Long> | 否 | 账户ID列表（与group_id二选一） |
| group_id | Long | 否 | 账户组合ID（与account_ids二选一） |
| period_id | Long | 否 | 账期ID（与year+period二选一） |
| year | Integer | 否 | 年份（与period_id二选一） |
| period | Integer | 否 | 期间（与period_id二选一） |

---

#### 导出汇总表Excel

**接口地址：** `GET /api/fund/summary/export`  
**请求方式：** GET  
**功能描述：** 导出汇总表数据为Excel文件

#### 请求参数

参考获取汇总表数据接口的请求参数

---

## 版本历史

### v1.8.11 (2025-01-20)
- 新增资金管理API完整文档：
  - 资金账户管理API：创建账户、分页查询账户列表、查询账户详情、更新账户、删除账户
  - 账户组合管理API：获取账户组合列表、分页查询账户组合列表、创建账户组合、查询账户组合详情、更新账户组合、删除账户组合
  - 账期管理API：获取账期列表、分页查询账期列表、初始化账期、创建单个账期、更新账期、删除账期
  - 资金流水管理API：创建资金流水、更新资金流水、删除资金流水
  - 结账管理API：检查并结账、反结账、获取检查项设置、更新检查项设置、结账
  - 日记账管理API：获取日记账明细、打印日记账、连续打印日记账
  - 汇总表管理API：获取汇总表数据、导出汇总表Excel
- 确保README.md中的接口文档与后端Controller实现完全一致

### v1.8.10 (2025-01-20)
- 补充开票通知单管理API文档：
  - 关联发票接口（`POST /api/finance/invoice-notices/{noticeId}/associate`）
  - 完成关联接口（`POST /api/finance/invoice-notices/{noticeId}/complete`）
- 确保README.md中的接口文档与后端Controller实现完全一致

### v1.8.9 (2025-01-20)
- 新增开票通知单管理API完整文档：
  - 获取开票通知单分页列表
  - 获取开票通知单详情
  - 创建开票通知单（支持草稿或直接提交审批）
  - 更新开票通知单
  - 提交审批
  - 取消/关闭开票通知单
  - 审批通过开票通知单
  - 驳回开票通知单
  - 获取发票关联列表
- 补充系统配置接口文档：
  - 批量获取系统配置（`POST /api/system/config/batch`）
- 确保README.md中的接口文档与后端Controller实现完全一致

### v1.8.8 (2025-01-20)
- 新增发票管理API完整文档：
  - 批量导入发票（ZIP文件）
  - Excel导入发票
  - 批量文件补充
  - 创建发票
  - 发票分页查询
  - 解析数电发票XML文件
  - 校验数电发票XML文件
  - 获取发票详情
  - 更新发票
  - 查询发票文件编号
- 新增结算单管理API完整文档：
  - 获取合同关联的可结算运输记录
  - 获取累积已结算量和合同计划总量
  - 创建结算单
  - 获取结算单详情
  - 审核结算单
  - 结算单分页查询
  - 获取结算统计信息
  - 查询已结算入库量
- 确保README.md中的接口文档与后端Controller实现完全一致

### v1.8.7 (2025-12-28)
- 新增财务管理API完整文档：
  - 获取合同关联的可结算入库单
  - 获取入库单对应的危废明细
- 扫描所有后端接口，对比并更新README.md，确保文档与Controller实现完全一致
- 验证所有现有API文档的完整性和准确性

### v1.8.6 (2025-12-26)
- 新增权限管理API完整文档：
  - 页面字段权限映射管理（获取、设置、删除）
  - 权限树构建与权限CRUD操作
  - 模块页面关联关系管理（新增、移除、批量更新）
  - 页面字段关联关系批量管理
- 新增角色管理API完整文档：
  - 角色CRUD操作（创建、更新、删除，支持二次确认）
  - 角色权限分配与查询
  - 角色成员管理
- 新增价外服务管理API完整文档：
  - 报价单价外服务管理（查询、新增批量）
  - 合同价外服务管理（查询、新增批量）
  - 价外服务CRUD操作（更新、删除）
- 更新模块清单：
  - Service层：新增 `PermissionService`、`RoleService`、`OutOfScopeServiceMapper`
  - Mapper层：新增 `PermissionMapper`、`RoleMapper`、`OutOfScopeServiceMapper`
- 确保README.md中的接口文档与后端Controller实现完全一致

### v1.8.4 (2025-12-13)
- 新增入库单和总磅单管理 API 完整文档：
  - 入库单管理 API：批量创建入库单、分页查询入库单列表、获取入库单详情、更新入库单、审核入库单、取消审核入库单、删除入库单
  - 总磅单管理 API：创建总磅单、获取总磅单信息、更新总磅单、分页查询总磅单列表
  - 所有接口包含完整的请求参数、响应格式、业务逻辑说明
  - 确保 README.md 中的接口文档与后端 Controller 实现保持一致

### v1.8.3 (2025-12-13)
- 新增入库单批量创建功能：
  - 新增 `WarehousingService` 服务接口和实现类，提供 `batchCreateWarehousing` 方法，支持根据总磅单为每个关联的收运通知单自动生成入库单
  - 新增 `WarehousingController` 控制器，提供 `POST /api/warehouse/inbound/batch-create` 批量创建入库单接口
  - 新增 `WarehousingMapper` 和 `WarehousingWasteItemMapper` 数据访问层，支持入库单和危废明细的创建与查询
  - 新增 `BatchCreateWarehousingRequest` 和 `BatchCreateWarehousingResponse` DTO，封装批量创建请求和响应数据
  - 修改 `WeighingSlipInfoResponse`，新增 `pickupNoticeList` 字段，包含关联的收运通知单列表和危废明细信息
  - 修改 `WeighingSlipService`，在获取总磅单信息时自动加载关联的收运通知单列表和危废明细
  - 入库单号自动生成规则：RKD-YYYYMMDD-4位序号
  - 入库单危废明细自动从收运通知单明细带出，用户只需填写实际接收数量和差异说明
- 更新模块清单：在 Service 层和 Mapper 层中补充 `WarehousingService`、`WarehousingMapper`、`WarehousingWasteItemMapper` 的详细功能说明

### v1.8.2 (2025-12-11)
- 补充收运通知单管理API文档：
  - 新增"审核阶段补充合同号"接口（`POST /api/transport/apply/audit/bind-contract`）
  - 新增"查询危废类别限额与已入库量"接口（`GET /api/transport/apply/waste-category-limit`）
- 新增运输单管理API完整文档：
  - 创建运输单（`POST /api/transport/dispatch/create`）
  - 更新运输单（`POST /api/transport/dispatch/update`）
  - 获取运输单详情（`GET /api/transport/dispatch/detail`）
  - 校验运输单风险（`GET /api/transport/dispatch/validate`）
  - 分页查询运输单列表（`POST /api/transport/dispatch/list`）
  - 打印运输单（`GET /api/transport/dispatch/print`）
  - 批量打印运输单（`POST /api/transport/dispatch/batch-print`）
  - 导出运输执行Excel（`GET /api/transport/dispatch/export`）
- 确保README.md中的接口文档与后端Controller实现保持一致

### v1.8.1 (2025-12-10)
- 补充模块清单中遗漏的服务和Mapper：
  - Service层：新增 `CustomerFollowUpService`（客户跟进服务）、`ContractApprovalFlowService`（合同审批流服务）、`VehicleService`（车辆管理服务）、`FileService`（文件服务）、`EmailChannelService`（邮件通道配置服务）
  - Mapper层：新增 `CustomerFollowUpMapper`（客户跟进数据访问）、`ContractApprovalFlowMapper`（合同审批流数据访问）、`VehicleMapper`（车辆管理数据访问）、`FileMapper`（文件数据访问）、`EmailChannelConfigMapper`（邮件通道配置数据访问）
- 确保 README.md 模块清单与代码实现保持一致

### v1.8.0 (2025-12-09)
- 生产执行链路对齐《收运流程与单据设计》：收运通知单与运输单一对一、运输单与总磅单一对一，入库支持按总磅单拆分批次；车辆/司机/押运员需校验在职与空闲，新增 Mapper 唯一性与并发校验说明。

### v1.7.9 (2025-12-04)
- 报价单表 `QUOTATION` 新增“乙方统一社会信用代码”字段：实体 `Quotation` 及 `QuotationMapper.xml`、创建/更新请求 DTO 与 Service 实现已同步维护该字段。
- 更新报价单管理 API 文档（本文件与《后端开发文档.md》）：在新增/更新、详情、分页查询等接口中补充 `partyBCreditCode` 字段说明，确保前后端与数据库结构保持一致。

### v1.7.8 (2025-12-03)
- 合同表新增 `customer_snapshot` JSON 字段：更新 `business-workflow-erp.sql` 初始化脚本并提供 `doc/sql/2025-12-03-add-contract-customer-snapshot.sql` 变更脚本，`Contract` 实体与 `ContractMapper.xml` 全量映射该字段。
- `ContractService` 与合同DTO增强：新增 `ContractCustomerSnapshot` 数据模型，`ContractCreateRequest`/`ContractUpdateRequest` 支持快照入参，分页与详情响应返回客户快照，服务实现统一生成/序列化/反序列化快照以兼容临时客户抬头。
- README 模块清单同步 `customer_snapshot` 功能描述，确保 Service/Mapper 文档与实现保持一致。

### v1.7.7 (2025-12-02)
- README 与《后端开发文档》补充消息通知模块说明：列出 `MessageService`/`MessageNotificationService`/`MessageConsumerService`、RabbitMQ 队列配置与 `/api/message/*` 全量接口（分页、详情、标记已读、统计、批量删除、全部已读、清空、未读数量）。
- 模块清单新增消息服务与 `MessageMapper`，确保与 `src/main/java/com/erp/service|mapper` 实现一致。
- README “消息通知管理 API”章节新增请求参数/响应示例，明确 `MessagePageRequest` 字段命名与当前实现支持的功能。

### v1.7.6 (2025-11-29)
- 优化报价单PDF生成功能：
  - 小计摘要行合并为一行显示，所有小计项用顿号（、）分隔
  - 合计行根据小计JSON数据计算，相同unit合并，不同unit用顿号分开
  - 总价包干模式下金额和付款方使用|分隔符，合并时去掉quotationItemId
  - 支持总价包干模式单元格合并，连续相同值自动合并，小计行会打断合并
  - 单条数据也会去掉quotationItemId，只显示内容部分

### v1.7.5 (2025-11-28)
- 调通前端 `QuotationAddPage` 与后端 `/api/quotation` 接口：新增模式调用 `POST /quotation`，编辑模式调用 `PUT /quotation/{id}`，统一校验计价方案、危废条目和付款方逻辑
- 更新前端 `quotation.ts` 类型定义与 API 模块，补齐甲乙双方信息、报价日期、危废条目字段，确保与 `QuotationCreateRequest/QuotationUpdateRequest`/`QuotationDetailResponse` 对齐
- 完善报价单管理 API 文档：在 README.md 中新增完整的报价单管理接口文档，包含新增、更新、详情、分页查询、审核、导出、PDF生成等接口的详细说明、请求参数、响应格式和前端调用示例

### v1.7.4 (2025-11-28)
- 报价单服务字段与 `QUOTATION` 表对齐：补齐甲乙双方信息、报价日期映射，新增乙方字段校验并将甲方联系人/电话与客户档案联动
- 报价单分页/详情/导出接口统一按危废条目动态计算总数量与计量单位，移除对不存在列（总数量、PDF文件编号）的依赖
- 报价单PDF生成与查询改为基于 `FILE` 表按业务类型 `QUOTATION` 关联，重复生成会软删除旧文件并始终返回最新PDF信息

### v1.7.3 (2025-11-28)
- 修复危险废物名录分页引用统计 SQL 错误：旧版 `HazardousWasteItemMapper.selectReferenceStats` 误引用 `QUOTATION.危废条目编号` 字段导致接口报 `Unknown column` 并使前端页面空白
- 新实现基于 `QUOTATION_WASTE_ITEM` × `QUOTATION_ITEM` × `QUOTATION` 计算客户/报价引用数量，确保页面统计数据可正常返回

### v1.7.2 (2025-11-28)
- 危险废物名录删除逻辑新增废物类别引用检测：当类别仅被当前条目引用时，会在条目删除后同步删除 `HAZARDOUS_WASTE_CATEGORY` 中的记录，避免产生孤儿数据
- `HazardousWasteItemMapper` 新增 `countByCategoryId` 方法与SQL，实现废物类别引用数量统计
- 废物类别限额分页/单条查询接口响应新增 `wasteItemCount` 字段，回传 `HazardousWasteItemMapper.countByCategoryId` 计算的危废条目数量

### v1.7.1 (2025-01-XX)
- 删除合同表中的"合同类型"字段及相关逻辑
  - 移除 `Contract` 实体类中的 `contractType` 字段
  - 移除所有合同相关DTO中的 `contractType` 字段（ContractCreateRequest、ContractUpdateRequest、ContractDetailResponse、ContractPageRequest、ContractPageResponse、CustomerContractResponse）
  - 移除合同分页查询接口中的合同类型筛选功能
  - 更新 `ContractMapper` 和 `ContractMapper.xml`，删除合同类型相关查询条件
  - 更新 `ContractServiceImpl` 和 `CustomerServiceImpl`，删除所有合同类型相关业务逻辑

### v1.7.0 (2025-01-XX)
- 新增报价单管理模块完整后端能力
  - `/api/quotation` 系列接口支持报价单新增、更新、详情、分页查询、审核
  - 支持"总价包干"和"按量结算"两种报价模式
  - 总价包干模式：计价方案和备注存储在报价条目表（QUOTATION_ITEM）
  - 按量结算模式：计价方案和备注存储在危废条目明细表（QUOTATION_WASTE_ITEM）
  - 系统自动计算总数量（按量结算模式下的计划数量合计）
  - 自动生成报价单号（格式：QT-YYYYMMDD-XXXXX）
  - 报价单状态管理：草稿（可修改）、待审批（可审核）、已通过/已拒绝/已失效（只读）
- 新增 `Quotation`、`QuotationItem`、`QuotationWasteItem` 实体类
- 新增 `QuotationMapper`、`QuotationItemMapper`、`QuotationWasteItemMapper` 及对应XML映射文件
- 新增 `QuotationService`、`QuotationServiceImpl` 实现报价单业务逻辑
- 新增 `QuotationController` 提供报价单管理 REST API
- 新增报价单相关 DTO：`QuotationCreateRequest`、`QuotationUpdateRequest`、`QuotationDetailResponse`、`QuotationPageRequest`、`QuotationPageResponse`、`QuotationAuditRequest`

### v1.6.1 (2025-11-28)
- 危险废物名录接口 `HazardousWasteItemService` / `WasteCodeController` 全量支持“废物类别编码 + 废物类别名称”双字段：
  - 分页、导出、详情、新增、更新、导入模板均新增 `wasteCategoryName`，导入时自动回填类别表并与 `HAZARDOUS_WASTE_CATEGORY` 关联
  - Excel 导入模板与导出列新增“废物类别名称”，示例数据同步更新
- `HAZARDOUS_WASTE_ITEM` 仅保留 `废物类别编号` 外键，类别编码 / 名称统一从 `HAZARDOUS_WASTE_CATEGORY` 关联查询，避免字段冗余
- `HazardousWasteCategoryService` 支持按类别编码与名称联合筛选，返回数据包含 `wasteCategoryName`
- `HazardousWasteItemMapper` / `HazardousWasteCategoryMapper` SQL 同步 `废物类别名称` 列，确保与最新 DDL（HAZARDOUS_WASTE_CATEGORY / HAZARDOUS_WASTE_ITEM）一致

### v1.6.0 (2025-11-27)
- 新增消息通知模块完整后端能力，基于RabbitMQ实现异步消息处理
  - 新增 `RabbitMQConfig` 配置类，定义预警消息、业务通知、系统消息三类队列
  - 新增 `MessageNotificationService` 消息发送服务，支持合同到期预警、应收款逾期预警、审核通知等场景
  - 新增 `MessageConsumerService` 消息消费者，异步处理消息队列并保存到数据库
  - 新增 `MessageService` 消息管理服务，提供消息查询、标记已读、删除等功能
  - 新增 `MessageController` 提供消息管理 REST API (`/api/message/*`)
  - 新增 `Message` 实体类和 `MessageMapper` 数据访问层，对应 MESSAGE 表
  - 配置文件新增 RabbitMQ 连接配置，支持重试机制和连接超时设置
- 消息通知功能特性：
  - 支持按优先级（紧急/高/中/低）分类处理消息
  - 支持按类型（预警消息/业务通知/系统消息）路由到不同队列
  - 提供批量操作（批量标记已读、批量删除）
  - 自动标记已读和软删除机制
  - 消息统计和未读数量查询

### v1.5.3 (2025-11-27)
- 客户批量导入流程补齐 `customer_status`，Excel 模板未提供该列时默认写入"正常"，解决导入过程中状态为空报错
- 《后端开发文档》3.2.8 节补充模板说明，明确状态字段由系统自动赋值

### v1.5.2 (2025-11-27)
- 修复 `CustomerMapper.insertBatch` 列映射，移除失效的危废字段，补齐电话/联系人/曾用名等新字段，确保批量导入与数据库表结构一致
- 《后端开发文档》新增“3.2.8 字段与Excel模板说明”并更新批量导入校验描述，与 `business-workflow-erp.sql` 中 CUSTOMER 表定义保持同步
- 纠正客户导入模板列顺序说明，明确管理员与普通业务员的填报规则
- 后端开发文档同步 `/api/customer/import/template` 接口说明，模板列头按 `CustomerController.exportImportTemplate` 实际输出更新

### v1.5.1 (2025-11-26)
- 客户管理模块新增关联数据查询接口
  - `/api/customer/{customerId}/quotations`：获取客户报价记录
  - `/api/customer/{customerId}/contracts`：获取客户合同记录
  - `/api/customer/{customerId}/follows`：获取客户跟进记录
- 合同管理模块新增统计接口
  - `/api/contract/statistics`：获取合同统计信息（总数、执行中、已完结、待审核、本月新增合同数及本月合同金额）

### v1.5.0 (2025-11-26)
- 新增合同管理模块完整后端能力
  - `/api/contract` 系列接口支持合同新增（含报价单与合同PDF上传）、详情、分页查询、编辑
  - 新增合同时自动创建报价单（QUOTATION）记录，关联危废信息
  - 合同PDF上传集成现有文件服务（FileService），支持本地存储
  - 合同初始状态为"待审核"，支持状态流转：待审核/已通过/执行中/已完结/已归档/已拒绝
- 新增 `Contract` 实体（对应 CONTRACT 表）、`Quotation` 实体（对应 QUOTATION 表）
- 新增 `ContractMapper`、`QuotationMapper`、`ContractMapper.xml` 实现合同分页与详情查询
- 新增 `ContractService`、`ContractServiceImpl` 实现合同业务逻辑
- 新增 `ContractController` 提供合同管理 REST API
- 新增合同相关 DTO：`ContractCreateRequest`、`ContractUpdateRequest`、`ContractPageRequest`、`ContractPageResponse`、`ContractDetailResponse`

### v1.4.4 (2025-11-26)
- 客户表结构与客户模块全面对齐：移除产废类型/危废/附件字段，新增电话、法定代表人、联系人、联系电话、曾用名、业务员编码等字段映射
- 更新 `CustomerService` / `CustomerMapper` / `CustomerController` 逻辑：新增客户时业务员编码与创建人编码自动使用当前登录用户，分页/导出/导入均按新字段工作
- 调整客户批量导入模板与导出列，新增 `/api/customer/import/template` 接口用于下载导入模板
- 同步更新文档：`README.md`、`前端开发文档.md` 中客户管理相关接口说明和字段说明与最新实现一致

### v1.4.3 (2025-11-26)
- 危险废物类别限额配置支持分页查询
  - `GET /api/waste-code/category/config`：调整为分页查询废物类别限额配置接口，请求参数支持 `current`、`size`、`wasteCategory`（模糊匹配）
  - 分页返回 `wasteCategory`、`limitAmount`、`limitStartTime`、`limitEndTime` 字段

### v1.4.2 (2025-11-26)
- 危险废物名录与废物类别联动增强
  - 新增 `HAZARDOUS_WASTE_CATEGORY` 废物类别表，并通过 `废物类别编号` 与 `HAZARDOUS_WASTE_ITEM` 关联
  - 新增 `HazardousWasteCategoryMapper`、`HazardousWasteCategoryService`，以及废物类别限额配置接口  
    - `GET /api/waste-code/category/config`：按废物类别查询限额、开始时间、结束时间  
    - `PUT /api/waste-code/category/config`：仅支持修改限额、开始时间、结束时间
  - 危废条目新增与批量导入时：若废物类别不存在则自动在 `HAZARDOUS_WASTE_CATEGORY` 中创建，默认限额 `999999`，开始时间为当前时间，结束时间为一年后
  - 危险废物条目的 `available` 字段：不传时默认写入 `false`（数据库 `TINYINT DEFAULT 0`），前端需显式传 `true` 才启用

### v1.4.1 (2025-11-25)
- `/api/waste-code/list` 模糊搜索增强：废物类别、行业来源、危险特性、废物代码、危险废物全部支持 `LIKE` 匹配

### v1.4.0 (2025-11-25)
- 危险废物名录管理后端接口落地
  - `/waste-code` 系列接口支持 CRUD、分页、引用统计、Excel 导入导出及模板下载
  - 新增 `HazardousWasteItemService`、`HazardousWasteItemMapper`、`WasteCodeController`
  - Excel 导入校验废物代码唯一性与危险特性规范，导出提供引用数量

### v1.3.0 (2025-01-06)
- 新增客户管理完整后端能力
  - `/api/customer` 系列接口支持创建、更新、删除、详情与分页查询，非管理员自动按业务员编码过滤
  - `/api/customer/import` 支持Excel批量导入，校验重复信用代码与业务员有效性
  - `/api/customer/export` 支持按条件导出客户列表为xlsx
- CustomerService/CustomerMapper 实现客户CRUD、批量导入导出逻辑
- EmployeeRoleMapper 新增角色名称查询方法用于管理员判定

### v1.3.1 (2025-01-06)
- 客户表恢复“产废类型 / 危险废物代码 / 危废条目编号”字段并与导入、导出、实体/DTO同步
- Excel 模板与接口文档新增相应列，保障危废信息可追溯

### v1.2.0 (2025-01-05)
- 新增邮件通道配置全链路接口
  - `GET /api/email-channel/config`：读取 `EMAIL_CHANNEL_CONFIG` 并返回 `hasPassword`
  - `POST /api/email-channel/config`：AES-256 加密授权码、写库并刷新 Redis 缓存
  - `POST /api/email-channel/test-send`：使用 Hutool `MailUtil` 实时发送自检邮件
- 新增可复用邮件发送组件
  - `EmailChannelService#sendEmail(EmailSendRequest)`：提供统一SMTP发送能力
  - `EmailSendResult` 返回追踪ID和发送状态，可供审批通知、告警推送等场景复用

### v1.1.7 (2025-01-04)
- 调整驳回员工注册逻辑
  - 已通过的注册申请不可再驳回
  - 驳回时不再删除已创建的正式员工账号

### v1.1.6 (2025-01-04)
- 新增驳回员工注册接口
  - `/api/system/employee/registration/{registrationId}/reject`：支持驳回注册申请

### v1.1.5 (2025-01-03)
- 新增审核通过员工注册接口
  - `/api/system/employee/registration/{registrationId}/approve`：审核通过后自动创建正式员工

### v1.1.4 (2025-01-03)
- 新增重置员工密码接口
  - `/api/system/employee/{employeeId}/password`：重置指定员工的登录密码

### v1.1.3 (2025-01-03)
- 员工注册信息分页查询接口新增身份证正反面URL字段
  - `/api/system/employee/registrations` 响应新增 `idCardFrontFileUrl`、`idCardBackFileUrl` 字段
  - 后端根据注册记录中的文件编号自动查询 `FILE` 表获取访问URL

### v1.1.2 (2025-01-02)
- 新增注册管理与正式员工分离
  - `/api/system/employee/registrations`：员工注册审核列表，读取 `EMPLOYEE_REGISTRATION` 表
  - `/api/system/employee/list`：正式员工列表，读取 `EMPLOYEE` 表
  - `/api/system/employee`：新增正式员工接口
  - `/api/system/employee/{id}`：编辑正式员工接口

### v1.1.1 (2025-01-01)
- 修复员工列表分页查询接口，从 `EMPLOYEE_REGISTRATION` 表读取注册信息

### v1.1.0 (2025-01-01)
- 新增员工注册信息分页查询接口
  - 支持按员工姓名、部门、岗位、手机号码、登录账号、审核状态、权限分配状态进行筛选查询
  - 接口路径：`GET /api/system/employee/registration/list`
  - 支持分页显示，默认每页10条记录

### v1.0.0 (2025-01-01)
- 初始化项目框架
- 完成基础配置和公共模块
- 完成安全认证模块
- 完成员工注册功能



