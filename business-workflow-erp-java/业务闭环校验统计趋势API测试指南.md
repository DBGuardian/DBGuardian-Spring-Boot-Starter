# 业务闭环校验统计趋势API测试指南

## 概述

本文档介绍新增的业务闭环校验统计信息和趋势数据API接口的使用方法和测试步骤。

## 新增接口

### 1. 获取校验统计信息

**接口地址**: `GET /finance/closure/statistics`

**功能描述**: 获取业务闭环校验的统计信息，包括校验次数、问题统计、风险等级分布等

**请求参数**:
```json
{
  "dateRange": "2025-01-01,2025-12-31",    // 可选，时间范围
  "organizationId": 1                        // 可选，组织ID
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取校验统计信息成功",
  "data": {
    "totalValidations": 1,
    "successfulValidations": 1,
    "failedValidations": 0,
    "totalIssuesFound": 15,
    "resolvedIssues": 0,
    "unresolvedIssues": 15,
    "criticalIssues": 3,
    "highRiskIssues": 5,
    "mediumRiskIssues": 4,
    "lowRiskIssues": 3,
    "contractsValidated": 10,
    "settlementsValidated": 25,
    "averageValidationTime": 500,
    "lastValidationTime": "2025-02-06 14:30:25",
    "statisticsStartDate": "2025-01-01",
    "statisticsEndDate": "2025-02-06"
  }
}
```

### 2. 获取趋势数据

**接口地址**: `GET /finance/closure/dashboard/trends`

**功能描述**: 获取业务闭环监控的趋势数据，支持不同指标类型和时间周期

**请求参数**:
```json
{
  "metricType": "ISSUE_COUNT",     // 必填，指标类型：ISSUE_COUNT/VALIDATION_COUNT/RESOLUTION_RATE
  "period": "DAY",                 // 必填，时间周期：DAY/WEEK/MONTH
  "dateRange": "2025-01-01,2025-02-06"  // 可选，时间范围
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "获取趋势数据成功",
  "data": [
    {
      "timePoint": "2025-01-31",
      "value": 12,
      "metricName": "问题数量",
      "metricType": "ISSUE_COUNT",
      "period": "DAY",
      "growthRate": 2.5,
      "details": {}
    },
    {
      "timePoint": "2025-02-01",
      "value": 15,
      "metricName": "问题数量",
      "metricType": "ISSUE_COUNT",
      "period": "DAY",
      "growthRate": 1.2,
      "details": {}
    }
  ]
}
```

## 前端集成

### 1. API调用方法

```typescript
// 获取校验统计信息
const loadStatistics = async () => {
  try {
    const response = await financeApi.getValidationStatistics({
      dateRange: '2025-01-01,2025-12-31',
      organizationId: 1
    })
    console.log('统计信息:', response.data)
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

// 获取趋势数据
const loadTrends = async () => {
  try {
    const response = await financeApi.getTrendData({
      metricType: 'ISSUE_COUNT',
      period: 'DAY',
      dateRange: '2025-01-01,2025-02-06'
    })
    console.log('趋势数据:', response.data)
  } catch (error) {
    console.error('获取趋势数据失败:', error)
  }
}
```

### 2. 页面组件使用

在 `BusinessClosureMonitor.vue` 页面中：

1. **统计信息面板**: 点击"统计信息"按钮加载校验统计数据
2. **趋势图表面板**: 点击"趋势图表"按钮加载并显示趋势图表
3. **图表交互**: 支持切换指标类型（问题数量/校验次数/解决率）和时间周期（日/周/月）

### 3. 数据可视化

使用 ECharts 实现趋势图表：
- 支持折线图显示
- 自动适应不同指标类型
- 响应式设计，支持窗口大小调整

## 测试步骤

### 1. 后端API测试

1. 启动后端服务
2. 使用 Postman 或类似工具测试接口：

```bash
# 测试统计信息接口
GET http://localhost:8080/finance/closure/statistics

# 测试趋势数据接口
GET http://localhost:8080/finance/closure/dashboard/trends?metricType=ISSUE_COUNT&period=DAY
```

### 2. 前端页面测试

1. 启动前端开发服务器
2. 访问业务闭环监控页面
3. 点击"执行全量校验"按钮
4. 点击"统计信息"按钮查看统计面板
5. 点击"趋势图表"按钮查看趋势图表
6. 测试指标类型和时间周期切换功能

### 3. 数据验证

1. **统计信息**: 验证各字段数据正确性
2. **趋势数据**: 验证7天数据点，数值范围合理
3. **图表显示**: 验证ECharts图表正常渲染

## 注意事项

1. **数据来源**: 当前实现使用模拟数据，生产环境需要连接真实数据库
2. **性能考虑**: 趋势数据查询可能需要优化大量历史数据
3. **缓存策略**: 考虑对统计数据添加缓存机制
4. **权限控制**: 根据用户角色控制数据访问权限

## 故障排除

### 常见问题

1. **接口404错误**: 检查后端服务是否正常启动
2. **图表不显示**: 检查ECharts是否正确引入
3. **数据为空**: 执行全量校验后再查看统计信息

### 日志查看

```bash
# 查看后端日志
tail -f business-workflow-erp-java/logs/app.log

# 查看前端控制台
# 在浏览器开发者工具中查看Console
```

## 扩展计划

1. **实时数据**: 支持WebSocket实时更新统计信息
2. **更多指标**: 添加更多业务指标类型
3. **高级图表**: 支持柱状图、饼图等多种图表类型
4. **数据导出**: 支持统计数据和图表导出功能











