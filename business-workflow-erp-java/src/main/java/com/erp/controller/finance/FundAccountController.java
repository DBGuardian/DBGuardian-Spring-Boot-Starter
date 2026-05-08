package com.erp.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.result.Result;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundAccountCreateRequest;
import com.erp.controller.finance.dto.FundAccountCreateResponse;
import com.erp.controller.finance.dto.FundAccountListItemResponse;
import com.erp.controller.finance.dto.FundAccountPageRequest;
import com.erp.controller.finance.dto.FundAccountUpdateRequest;
import com.erp.controller.finance.dto.UpdateOrganizationAccountsRequest;
import com.erp.entity.finance.FundAccount;
import com.erp.service.finance.FundAccountService;
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
 * 资金账户管理控制器
 *
 * 接口设计严格参考《资金管理实现说明》中的账户管理接口：
 * - 创建账户：POST /api/fund/accounts
 */
@Slf4j
@RestController
@RequestMapping("/fund")
@Api(tags = "资金账户管理")
@Validated
public class FundAccountController {

    @Autowired
    private FundAccountService fundAccountService;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private MessageNotificationService messageNotificationService;

    /**
     * 创建账户
     *
     * 接口名称：创建账户
     * 功能描述：创建新的账户
     * 接口地址：/api/fund/accounts
     * 请求方式：POST
     *
     * 请求体（JSON）：
     * {
     *   "account_name": "工商银行",
     *   "account_type": "BANK",
     *   "remark": ""
     * }
     *
     * 返回体 data：
     * {
     *   "account_id": 1,
     *   "account_code": "ACC-001"
     * }
     */
    @PostMapping("/accounts")
    @ApiOperation(value = "创建资金账户", notes = "创建新的资金账户（银行账户/备用金账户/现金账户）")
    public Result<FundAccountCreateResponse> createAccount(@RequestBody FundAccountCreateRequest request,
                                                          HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean createSuccess = false;
        String errorMessage = null;
        FundAccountCreateResponse response = null;

        try {
            // 校验必须的组织ID，避免数据库因为无组织编号导致插入失败
            if (request.getOrganizationId() == null) {
                log.warn("创建资金账户失败，缺少 organizationId, request={}", request);
                errorMessage = "所属组织ID不能为空";
                return Result.error("创建账户失败：" + errorMessage);
            }
            FundAccount account = fundAccountService.createAccount(
                    request.getAccountName(),
                    request.getAccountType(),
                    request.getAccountBankAccount(),
                    request.getAccountBankInstitution(),
                    request.getRemark(),
                    request.getOrganizationId()
            );

            response = new FundAccountCreateResponse();
            response.setAccountId(account.getAccountId());
            response.setAccountCode(account.getAccountCode());
            createSuccess = true;

            return Result.success("创建账户成功", response);
        } catch (Exception e) {
            log.error("创建资金账户失败，request={}", request, e);
            errorMessage = e.getMessage();
            return Result.error("创建账户失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("创建资金账户：账户名称=%s，账户类型=%s，组织ID=%s",
                        request.getAccountName(), request.getAccountType(), request.getOrganizationId());
                logRecordService.recordOperationLog("资金账户管理", "新增",
                        logContent, userId, ipAddress, createSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录创建账户操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (createSuccess && response != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_ACCOUNT_CREATE",
                            response.getAccountId() != null ? response.getAccountId().intValue() : null,
                            String.format("资金账户【%s】", request.getAccountName()),
                            "新增",
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送账户创建通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 分页查询资金账户列表
     *
     * 接口名称：查询账户列表
     * 功能描述：按账户名称、账户类型、启用状态筛选资金账户，支持字段排序，返回创建人/更新人姓名
     * 接口地址：/api/fund/accounts/page
     * 请求方式：POST
     *
     * 请求体（JSON）：
     * {
     *   "accountName": "工行",
     *   "accountType": "BANK",
     *   "isEnabled": true,
     *   "current": 1,
     *   "size": 10,
     *   "sortField": "createTime",
     *   "sortOrder": "desc"
     * }
     *
     * 返回体 data：IPage<FundAccountListItemResponse>
     */
    @RequirePagePermission({
            "财务管理:账户设置:页面",
            "财务管理:账户设置:账户管理:页面",
            "财务管理:资金管理:日记账:页面",
            "财务管理:资金管理:汇总表:页面"
    })
    @PostMapping("/accounts/page")
    @ApiOperation(value = "分页查询资金账户列表", notes = "支持账户名称、账户类型、启用状态筛选和字段排序，并返回创建人/更新人姓名")
    public Result<IPage<FundAccountListItemResponse>> getAccountPage(@RequestBody @Valid FundAccountPageRequest request) {
        Page<FundAccountListItemResponse> page = new Page<>(
                request.getCurrent() == null ? 1 : request.getCurrent(),
                request.getSize() == null ? 10 : request.getSize()
        );
        IPage<FundAccountListItemResponse> resultPage = fundAccountService.getAccountPage(page, request);
        return Result.success("查询成功", resultPage);
    }

    /**
     * 查询资金账户详情
     *
     * 接口名称：查询账户详情
     * 功能描述：根据账户ID查询资金账户详细信息
     * 接口地址：/api/fund/accounts/{accountId}
     * 请求方式：GET
     */
    @GetMapping("/accounts/{accountId}")
    @ApiOperation(value = "查询资金账户详情", notes = "根据账户ID查询资金账户详细信息")
    public Result<FundAccount> getAccountDetail(@PathVariable("accountId") Long accountId) {
        FundAccount account = fundAccountService.getById(accountId);
        if (account == null) {
            return Result.error("资金账户不存在");
        }
        return Result.success("查询成功", account);
    }

    /**
     * 更新资金账户
     *
     * 接口名称：更新账户
     * 功能描述：更新资金账户基础信息（不修改创建人编码和创建时间）
     * 接口地址：/api/fund/accounts/{accountId}/update
     * 请求方式：POST
     */
    @PostMapping("/accounts/{accountId}/update")
    @ApiOperation(value = "更新资金账户", notes = "更新资金账户基础信息（不修改创建人编码和创建时间）")
    public Result<FundAccount> updateAccount(@PathVariable("accountId") Long accountId,
                                             @RequestBody @Valid FundAccountUpdateRequest request,
                                             HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean updateSuccess = false;
        String errorMessage = null;
        FundAccount updatedAccount = null;

        try {
            updatedAccount = fundAccountService.updateAccount(
                    accountId,
                    request.getAccountName(),
                    request.getAccountType(),
                    request.getAccountBankAccount(),
                    request.getAccountBankInstitution(),
                    request.getEnabled(),
                    request.getRemark()
            );
            updateSuccess = true;
            return Result.success("更新成功", updatedAccount);
        } catch (Exception e) {
            log.error("更新资金账户失败，accountId={}, request={}", accountId, request, e);
            errorMessage = e.getMessage();
            return Result.error("更新失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("更新资金账户：账户ID=%s，账户名称=%s",
                        accountId, request.getAccountName());
                logRecordService.recordOperationLog("资金账户管理", "更新",
                        logContent, userId, ipAddress, updateSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录更新账户操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (updateSuccess && updatedAccount != null) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_ACCOUNT_UPDATE",
                            accountId != null ? accountId.intValue() : null,
                            String.format("资金账户【%s】", request.getAccountName()),
                            "更新",
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送账户更新通知失败", msgEx);
                }
            }
        }
    }

    /**
     * 删除资金账户及其所有资金流水
     *
     * 接口名称：删除账户
     * 功能描述：删除指定资金账户，并同时删除该账户下所有资金流水，操作不可恢复
     * 接口地址：/api/fund/accounts/{accountId}/delete
     * 请求方式：POST
     */
    @PostMapping("/accounts/{accountId}/delete")
    @ApiOperation(value = "删除资金账户及其资金流水", notes = "删除资金账户及其所有资金流水，操作不可恢复")
    public Result<Void> deleteAccount(@PathVariable("accountId") Long accountId,
                                      HttpServletRequest httpRequest) {
        Integer userId = SecurityUtil.getCurrentUserId();
        String ipAddress = logRecordService.getClientIp(httpRequest);
        boolean deleteSuccess = false;
        String errorMessage = null;

        try {
            fundAccountService.deleteAccountWithTransactions(accountId);
            deleteSuccess = true;
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除资金账户失败，accountId={}", accountId, e);
            errorMessage = e.getMessage();
            return Result.error("删除失败：" + e.getMessage());
        } finally {
            // 记录操作日志
            try {
                String logContent = String.format("删除资金账户：账户ID=%s", accountId);
                logRecordService.recordOperationLog("资金账户管理", "删除",
                        logContent, userId, ipAddress, deleteSuccess, errorMessage);
            } catch (Exception logEx) {
                log.warn("记录删除账户操作日志失败", logEx);
            }

            // 发送消息通知（仅成功时）
            if (deleteSuccess) {
                try {
                    messageNotificationService.sendBusinessOperationNotification(
                            "FUND_ACCOUNT_DELETE",
                            accountId != null ? accountId.intValue() : null,
                            String.format("资金账户ID=%s", accountId),
                            "删除",
                            userId
                    );
                } catch (Exception msgEx) {
                    log.warn("发送账户删除通知失败", msgEx);
                }
            }
        }
    }


    /**
     * 分页查询组织下的账户
     *
     * 接口名称：分页查询组织下的账户
     * 功能描述：分页查询指定组织下的账户，支持筛选和排序
     * 接口地址：/api/fund/organizations/{organizationId}/accounts/page
     * 请求方式：POST
     */
    @RequirePagePermission({
            "财务管理:账户设置:页面",
            "财务管理:账户设置:账户管理:页面",
            "财务管理:资金管理:日记账:页面",
            "财务管理:资金管理:汇总表:页面"
    })
    @PostMapping("/organizations/{organizationId}/accounts/page")
    @ApiOperation(value = "分页查询组织下的账户", notes = "分页查询指定组织下的账户，支持筛选和排序")
    public Result<IPage<FundAccountListItemResponse>> getAccountsByOrganizationIdPage(@PathVariable("organizationId") Long organizationId,
                                                                                     @RequestBody @Valid FundAccountPageRequest request) {
        try {
            Page<FundAccountListItemResponse> page = new Page<>(
                    request.getCurrent() == null ? 1 : request.getCurrent(),
                    request.getSize() == null ? 10 : request.getSize()
            );
            IPage<FundAccountListItemResponse> resultPage = fundAccountService.getAccountsByOrganizationIdPage(page, organizationId, request);
            return Result.success("查询成功", resultPage);
        } catch (Exception e) {
            log.error("分页查询组织下的账户失败，organizationId={}, request={}", organizationId, request, e);
            return Result.error("分页查询失败：" + e.getMessage());
        }
    }
}


