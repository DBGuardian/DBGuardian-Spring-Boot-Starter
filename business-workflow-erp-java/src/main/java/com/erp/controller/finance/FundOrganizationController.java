package com.erp.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.*;
import com.erp.entity.finance.FundOrganization;
import com.erp.service.finance.FundOrganizationService;
import com.erp.service.system.ILogRecordService;
import com.erp.service.system.MessageNotificationService;
import com.erp.common.annotation.RequirePagePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 资金组织管理控制器
 *
 * 实现组织-账户的层次化管理：
 * - 组织作为一级实体，可以包含多个账户
 * - 支持组织的联系信息和基本信息管理
 * - 账户直接通过organizationId字段从属于组织（一对多关系）
 */
@Slf4j
@RestController
@RequestMapping("/fund")
@Api(tags = "资金组织管理")
@Validated
public class FundOrganizationController {

    @Autowired
    private FundOrganizationService fundOrganizationService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 获取资金组织树形结构
     *
     * 接口名称：获取资金组织树形结构
     * 功能描述：获取包含组织和账户的完整树形结构
     * 接口地址：/api/fund/organizations/tree
     * 请求方式：GET
     *
     * 返回体 data：OrganizationTreeNode[]
     */
    @RequirePagePermission("财务管理:账户设置:账户管理:页面")
    @GetMapping("/organizations/tree")
    @ApiOperation(value = "获取资金组织树形结构", notes = "获取包含组织和账户的完整树形结构")
    public Result<List<OrganizationTreeNode>> getOrganizationTree() {
        try {
            List<OrganizationTreeNode> treeNodes = fundOrganizationService.getOrganizationTree();
            return Result.success("获取资金组织树形结构成功", treeNodes);
        } catch (Exception e) {
            log.error("获取资金组织树形结构失败", e);
            return Result.error("获取资金组织树形结构失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询资金组织列表
     *
     * 接口名称：分页查询资金组织列表
     * 功能描述：按组织名称、启用状态筛选资金组织，支持字段排序
     * 接口地址：/api/fund/organizations/page
     * 请求方式：POST
     *
     * 请求体（JSON）：
     * {
     *   "organizationName": "主账户",
     *   "enabled": true,
     *   "current": 1,
     *   "size": 10,
     *   "sortField": "createTime",
     *   "sortOrder": "desc"
     * }
     *
     * 返回体 data：IPage<FundOrganizationListItemResponse>
     */
    @RequirePagePermission({
            "财务管理:账户设置:页面",
            "财务管理:账户设置:账户管理:页面",
            "财务管理:资金管理:日记账:页面",
            "财务管理:资金管理:汇总表:页面"
    })
    @PostMapping("/organizations/page")
    @ApiOperation(value = "分页查询资金组织列表", notes = "支持组织名称、启用状态筛选和字段排序")
    public Result<IPage<FundOrganizationListItemResponse>> getOrganizationPage(@RequestBody @Valid FundOrganizationPageRequest request) {
        try {
            Page<FundOrganizationListItemResponse> page = new Page<>(
                    request.getCurrent() == null ? 1 : request.getCurrent(),
                    request.getSize() == null ? 10 : request.getSize()
            );
            IPage<FundOrganizationListItemResponse> resultPage = fundOrganizationService.getOrganizationPage(page, request);
            return Result.success("查询成功", resultPage);
        } catch (Exception e) {
            log.error("分页查询资金组织列表失败", e);
            return Result.error("分页查询资金组织列表失败：" + e.getMessage());
        }
    }

    /**
     * 搜索资金组织选项
     *
     * 接口名称：搜索资金组织选项
     * 功能描述：根据关键词模糊搜索资金组织，用于下拉框选择
     * 接口地址：/api/fund/organizations/search
     * 请求方式：GET
     *
     * 请求参数：
     * - keyword：搜索关键词（可选，支持组织名称模糊搜索）
     * - limit：返回结果数量限制，默认20
     *
     * 返回体 data：OrganizationTreeNode[]
     */
    @GetMapping("/organizations/search")
    @ApiOperation(value = "搜索资金组织选项", notes = "根据关键词模糊搜索资金组织，用于下拉框选择")
    public Result<List<OrganizationTreeNode>> searchOrganizations(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        try {
            List<OrganizationTreeNode> options = fundOrganizationService.searchOrganizations(keyword, limit);
            return Result.success("搜索资金组织成功", options);
        } catch (Exception e) {
            log.error("搜索资金组织失败，keyword={}, limit={}", keyword, limit, e);
            return Result.error("搜索资金组织失败：" + e.getMessage());
        }
    }

    /**
     * 创建资金组织
     *
     * 接口名称：创建资金组织
     * 功能描述：创建新的资金组织
     * 接口地址：/api/fund/organizations
     * 请求方式：POST
     */
    @PostMapping("/organizations")
    @ApiOperation(value = "创建资金组织", notes = "创建新的资金组织")
    public Result<FundOrganization> createOrganization(@RequestBody @Valid FundOrganizationCreateRequest request,
                                                       HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean createSuccess = false;
        String errorMessage = null;
        FundOrganization organization = null;

        try {
            organization = fundOrganizationService.createOrganization(request);
            createSuccess = true;
            return Result.success("创建资金组织成功", organization);
        } catch (Exception e) {
            log.error("创建资金组织失败，request={}", request, e);
            errorMessage = e.getMessage();
            return Result.error("创建资金组织失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("创建资金组织：组织名称=%s", request.getOrganizationName());
                logRecordService.recordOperationLog("资金组织管理", "新增",
                        logContent, userId, ipAddress, createSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录创建组织操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (createSuccess && organization != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_ACCOUNT_CREATE", organization.getOrganizationId() != null ? organization.getOrganizationId().intValue() : null, "资金组织已创建", "新增", userId);
                } catch (Exception msgEx) {
                    log.warn("发送组织创建通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 查询资金组织详情
     *
     * 接口名称：查询资金组织详情
     * 功能描述：根据组织ID查询资金组织详细信息
     * 接口地址：/api/fund/organizations/{organizationId}
     * 请求方式：GET
     */
    @GetMapping("/organizations/{organizationId}")
    @ApiOperation(value = "查询资金组织详情", notes = "根据组织ID查询资金组织详细信息")
    public Result<FundOrganizationListItemResponse> getOrganizationDetail(@PathVariable("organizationId") Long organizationId) {
        try {
            FundOrganizationListItemResponse detail = fundOrganizationService.getOrganizationDetail(organizationId);
            return Result.success("查询资金组织详情成功", detail);
        } catch (Exception e) {
            log.error("查询资金组织详情失败，organizationId={}", organizationId, e);
            return Result.error("查询资金组织详情失败：" + e.getMessage());
        }
    }

    /**
     * 更新资金组织
     *
     * 接口名称：更新资金组织
     * 功能描述：更新资金组织信息
     * 接口地址：/api/fund/organizations/{organizationId}
     * 请求方式：PUT
     */
    @PutMapping("/organizations/{organizationId}")
    @ApiOperation(value = "更新资金组织", notes = "更新资金组织信息")
    public Result<FundOrganization> updateOrganization(@PathVariable("organizationId") Long organizationId,
                                                        @RequestBody @Valid FundOrganizationUpdateRequest request,
                                                        HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean updateSuccess = false;
        String errorMessage = null;
        FundOrganization organization = null;

        try {
            organization = fundOrganizationService.updateOrganization(organizationId, request);
            updateSuccess = true;
            return Result.success("更新资金组织成功", organization);
        } catch (Exception e) {
            log.error("更新资金组织失败，organizationId={}, request={}", organizationId, request, e);
            errorMessage = e.getMessage();
            return Result.error("更新资金组织失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("更新资金组织：组织ID=%s，组织名称=%s",
                        organizationId, request.getOrganizationName());
                logRecordService.recordOperationLog("资金组织管理", "更新",
                        logContent, userId, ipAddress, updateSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录更新组织操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (updateSuccess && organization != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_ACCOUNT_UPDATE", organizationId != null ? organizationId.intValue() : null, "资金组织已更新", "更新", userId);
                } catch (Exception msgEx) {
                    log.warn("发送组织更新通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 删除资金组织
     *
     * 接口名称：删除资金组织
     * 功能描述：删除资金组织
     * 接口地址：/api/fund/organizations/{organizationId}
     * 请求方式：DELETE
     */
    @DeleteMapping("/organizations/{organizationId}")
    @ApiOperation(value = "删除资金组织", notes = "删除资金组织")
    public Result<Void> deleteOrganization(@PathVariable("organizationId") Long organizationId,
                                           HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;

        try {
            fundOrganizationService.deleteOrganization(organizationId);
            deleteSuccess = true;
            return Result.success("删除资金组织成功", null);
        } catch (Exception e) {
            log.error("删除资金组织失败，organizationId={}", organizationId, e);
            errorMessage = e.getMessage();
            return Result.error("删除资金组织失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("删除资金组织：组织ID=%s", organizationId);
                logRecordService.recordOperationLog("资金组织管理", "删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录删除组织操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (deleteSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_ACCOUNT_DELETE", organizationId != null ? organizationId.intValue() : null, "资金组织已删除", "删除", userId);
                } catch (Exception msgEx) {
                    log.warn("发送组织删除通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 为组织添加账户（已废弃）
     *
     * 接口名称：为组织添加账户
     * 功能描述：该方法已废弃，现在账户直接通过organizationId字段从属于组织
     * 接口地址：/api/fund/organizations/{organizationId}/accounts/{accountId}
     * 请求方式：POST
     */
    @PostMapping("/organizations/{organizationId}/accounts/{accountId}")
    @ApiOperation(value = "为组织添加账户", notes = "建立组织与账户的关联关系")
    public Result<Void> addAccountToOrganization(@PathVariable("organizationId") Long organizationId,
                                                @PathVariable("accountId") Long accountId,
                                                HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean addSuccess = false;
        String errorMessage = null;

        try {
            fundOrganizationService.addAccountToOrganization(organizationId, accountId);
            addSuccess = true;
            return Result.success("为组织添加账户成功", null);
        } catch (Exception e) {
            log.error("为组织添加账户失败，organizationId={}, accountId={}", organizationId, accountId, e);
            errorMessage = e.getMessage();
            return Result.error("为组织添加账户失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("为组织添加账户：组织ID=%s，账户ID=%s", organizationId, accountId);
                logRecordService.recordOperationLog("资金组织管理", "新增",
                        logContent, userId, ipAddress, addSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录添加账户操作日志失败", logEx);
            }
        }
    }

    /**
     * 从组织移除账户（已废弃）
     *
     * 接口名称：从组织移除账户
     * 功能描述：该方法已废弃，现在账户直接从属于组织，无法移除关联
     * 接口地址：/api/fund/organizations/{organizationId}/accounts/{accountId}
     * 请求方式：DELETE
     */
    @DeleteMapping("/organizations/{organizationId}/accounts/{accountId}")
    @ApiOperation(value = "从组织移除账户", notes = "移除组织与账户的关联关系")
    public Result<Void> removeAccountFromOrganization(@PathVariable("organizationId") Long organizationId,
                                                     @PathVariable("accountId") Long accountId,
                                                     HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean removeSuccess = false;
        String errorMessage = null;

        try {
            fundOrganizationService.removeAccountFromOrganization(organizationId, accountId);
            removeSuccess = true;
            return Result.success("从组织移除账户成功", null);
        } catch (Exception e) {
            log.error("从组织移除账户失败，organizationId={}, accountId={}", organizationId, accountId, e);
            errorMessage = e.getMessage();
            return Result.error("从组织移除账户失败：" + e.getMessage());
        } finally {
            try {
                String logContent = String.format("从组织移除账户：组织ID=%s，账户ID=%s", organizationId, accountId);
                logRecordService.recordOperationLog("资金组织管理", "删除",
                        logContent, userId, ipAddress, removeSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录移除账户操作日志失败", logEx);
            }
        }
    }

    /**
     * 获取组织账户列表
     *
     * 接口名称：获取组织账户列表
     * 功能描述：分页获取指定组织的关联账户列表
     * 接口地址：/api/fund/organizations/{organizationId}/accounts
     * 请求方式：POST
     */
    @RequirePagePermission({
            "财务管理:账户设置:页面",
            "财务管理:账户设置:账户管理:页面",
            "财务管理:资金管理:日记账:页面",
            "财务管理:资金管理:汇总表:页面"
    })
    @PostMapping("/organizations/{organizationId}/accounts")
    @ApiOperation(value = "获取组织账户列表", notes = "分页获取指定组织的关联账户列表")
    public Result<IPage<FundAccountListItemResponse>> getOrganizationAccounts(@PathVariable("organizationId") Long organizationId,
                                                                              @RequestBody @Valid FundAccountPageRequest request) {
        try {
            Page<FundAccountListItemResponse> page = new Page<>(
                    request.getCurrent() == null ? 1 : request.getCurrent(),
                    request.getSize() == null ? 10 : request.getSize()
            );
            IPage<FundAccountListItemResponse> resultPage = fundOrganizationService.getOrganizationAccountsPage(page, organizationId, request);
            return Result.success("查询成功", resultPage);
        } catch (Exception e) {
            log.error("获取组织账户列表失败，organizationId={}, request={}", organizationId, request, e);
            return Result.error("获取组织账户列表失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新组织账户关联关系
     *
     * 接口名称：批量更新组织账户关联关系
     * 功能描述：批量设置账户所属的组织（直接更新账户的organizationId字段）
     * 接口地址：/api/fund/organizations/{organizationId}/accounts
     * 请求方式：PUT
     */
    @PutMapping("/organizations/{organizationId}/accounts")
    @ApiOperation(value = "批量更新组织账户关联关系", notes = "批量设置组织包含的账户列表")
    public Result<Void> updateOrganizationAccounts(@PathVariable("organizationId") Long organizationId,
                                                   @RequestBody @Valid UpdateOrganizationAccountsRequest request,
                                                   HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean updateSuccess = false;
        String errorMessage = null;

        try {
            fundOrganizationService.updateOrganizationAccounts(organizationId, request.getAccountIds());
            updateSuccess = true;
            return Result.success("批量更新组织账户关联关系成功", null);
        } catch (Exception e) {
            log.error("批量更新组织账户关联关系失败，organizationId={}, request={}", organizationId, request, e);
            errorMessage = e.getMessage();
            return Result.error("批量更新组织账户关联关系失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                int accountCount = request.getAccountIds() != null ? request.getAccountIds().size() : 0;
                String logContent = String.format("批量更新组织账户关联关系：组织ID=%s，账户数量=%s", organizationId, accountCount);
                logRecordService.recordOperationLog("资金组织管理", "更新",
                        logContent, userId, ipAddress, updateSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录更新组织账户关联操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (updateSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_ACCOUNT_UPDATE", organizationId != null ? organizationId.intValue() : null, "组织账户关联已更新", "更新", userId);
                } catch (Exception msgEx) {
                    log.warn("发送组织账户更新通知失败", msgEx);
                }
            }
        }
    }

}