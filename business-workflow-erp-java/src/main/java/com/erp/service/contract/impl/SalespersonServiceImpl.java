package com.erp.service.contract.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.contract.dto.SalespersonCreateRequest;
import com.erp.controller.contract.dto.SalespersonDetailResponse;
import com.erp.controller.contract.dto.SalespersonPageRequest;
import com.erp.controller.contract.dto.SalespersonPageResponse;
import com.erp.controller.contract.dto.SalespersonSelectResponse;
import com.erp.entity.contract.Salesperson;
import com.erp.entity.system.EmployeePermission;
import com.erp.mapper.contract.SalespersonMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.entity.system.Permission;
import com.erp.service.auth.AuthService;
import com.erp.service.contract.SalespersonService;
import com.erp.service.system.ILogRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 业务员 Service 实现
 */
@Slf4j
@Service
public class SalespersonServiceImpl implements SalespersonService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PAGE_CODE = "合同管理:业务合作合同:业务员信息:页面";

    @Autowired
    private SalespersonMapper salespersonMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public IPage<SalespersonPageResponse> getPage(SalespersonPageRequest request) {
        LambdaQueryWrapper<Salesperson> wrapper = buildQueryWrapper(request);
        // 应用数据范围控制（viewScope）
        Integer creatorFilter = resolveCreatorFilter(request.getCreatorFilter());
        if (creatorFilter != null) {
            wrapper.eq(Salesperson::getCreatorId, creatorFilter);
        }
        wrapper.orderByDesc(Salesperson::getSalespersonId);
        Page<Salesperson> page = salespersonMapper.selectPage(
                new Page<>(request.getCurrent(), request.getSize()), wrapper);
        return page.convert(this::toPageResponse);
    }

    /**
     * 根据viewScope解析creatorFilter
     * viewScope=SELF时强制返回当前用户ID，viewScope=ALL时返回null（查看全部）
     */
    private Integer resolveCreatorFilter(Integer requestCreatorFilter) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = currentUserId != null && authService.isAdmin(currentUserId);
        if (isAdmin) {
            log.debug("[resolveCreatorFilter] 管理员用户，不应用数据范围控制");
            return null;
        }
        EmployeePermission permission = getEmployeePagePermission(currentUserId, PAGE_CODE);
        if (permission != null && "SELF".equalsIgnoreCase(permission.getViewScope())) {
            log.debug("[resolveCreatorFilter] viewScope=SELF，强制creatorFilter={}", currentUserId);
            return currentUserId;
        }
        // viewScope=ALL或无配置时，返回null（查看全部数据）
        log.debug("[resolveCreatorFilter] viewScope=ALL或无配置，查看全部数据");
        return null;
    }

    /**
     * 获取员工对指定页面的权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        if (employeeId == null || pageCode == null) {
            return null;
        }
        try {
            // 从数据库查询页面权限ID
            Permission permission = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getPermissionCode, pageCode)
                    .eq(Permission::getPermissionTypeId, 2) // 2 = 页面级权限
            );

            if (permission == null) {
                return null;
            }

            // 查询员工页面权限配置
            return employeePermissionMapper.selectOne(
                new LambdaQueryWrapper<EmployeePermission>()
                    .eq(EmployeePermission::getEmployeeId, employeeId)
                    .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
            );
        } catch (Exception e) {
            log.warn("查询员工页面权限配置失败：employeeId={}, pageCode={}, error={}",
                    employeeId, pageCode, e.getMessage());
            return null;
        }
    }

    /**
     * 校验操作范围（operateScope）
     */
    private void validateOperateScope(Integer salespersonId, Integer creatorId) {
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "用户未登录");
        }
        boolean isAdmin = authService.isAdmin(currentUserId);
        if (isAdmin) {
            return; // 管理员不限制
        }
        EmployeePermission permission = getEmployeePagePermission(currentUserId, PAGE_CODE);
        if (permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
            if (!Objects.equals(creatorId, currentUserId)) {
                throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(),
                        "您只能操作自己创建的业务员档案");
            }
        }
    }

    @Override
    public IPage<SalespersonPageResponse> search(SalespersonPageRequest request) {
        LambdaQueryWrapper<Salesperson> wrapper = new LambdaQueryWrapper<Salesperson>()
                .eq(Salesperson::getDeleted, 0)
                .orderByDesc(Salesperson::getSalespersonId);
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(Salesperson::getSalespersonName, request.getKeyword())
                    .or().like(Salesperson::getPartyAName, request.getKeyword()));
        }
        int size = request.getSize() != null ? request.getSize() : 20;
        int current = request.getCurrent() != null ? request.getCurrent() : 1;
        Page<Salesperson> page = salespersonMapper.selectPage(new Page<>(current, size), wrapper);
        return page.convert(this::toPageResponse);
    }

    @Override
    public List<SalespersonSelectResponse> getSelectList(String keyword) {
        LambdaQueryWrapper<Salesperson> wrapper = new LambdaQueryWrapper<Salesperson>()
                .eq(Salesperson::getDeleted, 0)
                .orderByAsc(Salesperson::getSalespersonName);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Salesperson::getSalespersonName, keyword)
                    .or().like(Salesperson::getPartyAName, keyword));
        }
        List<Salesperson> list = salespersonMapper.selectList(wrapper);
        return list.stream().map(this::toSelectResponse).collect(Collectors.toList());
    }

    @Override
    public SalespersonDetailResponse getDetail(Integer salespersonId) {
        Salesperson sp = salespersonMapper.selectById(salespersonId);
        if (sp == null || Integer.valueOf(1).equals(sp.getDeleted())) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "业务员档案不存在");
        }
        SalespersonDetailResponse resp = new SalespersonDetailResponse();
        BeanUtils.copyProperties(toPageResponse(sp), resp);
        resp.setCustomerId(sp.getCustomerId());
        if (sp.getUpdateTime() != null) {
            resp.setUpdateTime(sp.getUpdateTime().format(DATETIME_FMT));
        }
        return resp;
    }

    @Override
    public Integer create(SalespersonCreateRequest request) {
        Salesperson sp = new Salesperson();
        copyFromRequest(request, sp);
        sp.setDeleted(0);

        // 设置创建人信息
        try {
            Integer userId = SecurityUtil.getCurrentUserId();
            sp.setCreatorId(userId);
        } catch (Exception e) {
            log.warn("获取当前用户ID失败", e);
        }

        salespersonMapper.insert(sp);

        // 记录数据变更日志（新增）
        logRecordService.recordDataChangeLog("业务员档案", "SALESPERSON", String.valueOf(sp.getSalespersonId()),
                "新增", "新增业务员，姓名=" + sp.getSalespersonName(),
                null, sp, sp.getCreatorId(), null, true, null);

        return sp.getSalespersonId();
    }

    @Override
    public void update(Integer salespersonId, SalespersonCreateRequest request) {
        Salesperson sp = salespersonMapper.selectById(salespersonId);
        if (sp == null || Integer.valueOf(1).equals(sp.getDeleted())) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "业务员档案不存在");
        }

        // 操作范围校验（operateScope）
        validateOperateScope(salespersonId, sp.getCreatorId());

        copyFromRequest(request, sp);
        salespersonMapper.updateById(sp);

        // 获取更新后的数据并记录数据变更日志
        Salesperson afterUpdate = salespersonMapper.selectById(salespersonId);
        logRecordService.recordDataChangeLog("业务员档案", "SALESPERSON", String.valueOf(salespersonId),
                "更新", "更新业务员，姓名=" + sp.getSalespersonName(),
                sp, afterUpdate, null, null, true, null);
    }

    @Override
    public void delete(Integer salespersonId) {
        Salesperson sp = salespersonMapper.selectById(salespersonId);
        if (sp == null || Integer.valueOf(1).equals(sp.getDeleted())) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "业务员档案不存在");
        }

        // 操作范围校验（operateScope）
        validateOperateScope(salespersonId, sp.getCreatorId());

        Salesperson update = new Salesperson();
        update.setSalespersonId(salespersonId);
        update.setDeleted(1);
        salespersonMapper.updateById(update);

        // 记录数据变更日志（删除）
        logRecordService.recordDataChangeLog("业务员档案", "SALESPERSON", String.valueOf(salespersonId),
                "删除", "删除业务员，姓名=" + sp.getSalespersonName(),
                sp, update, null, null, true, null);
    }

    @Override
    public void batchDelete(List<Integer> salespersonIds) {
        if (salespersonIds == null || salespersonIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请选择要删除的业务员");
        }

        // 操作范围校验（operateScope）- 批量操作时预先检查
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = currentUserId != null && authService.isAdmin(currentUserId);
        EmployeePermission permission = null;
        if (!isAdmin) {
            permission = getEmployeePagePermission(currentUserId, PAGE_CODE);
        }

        List<Salesperson> toDeleteList = salespersonMapper.selectBatchIds(salespersonIds).stream()
                .filter(sp -> !Integer.valueOf(1).equals(sp.getDeleted()))
                .collect(Collectors.toList());

        if (toDeleteList.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "没有找到可删除的业务员档案");
        }

        // operateScope=SELF时，仅允许删除自己创建的业务员
        if (!isAdmin && permission != null && "SELF".equalsIgnoreCase(permission.getOperateScope())) {
            for (Salesperson sp : toDeleteList) {
                if (!Objects.equals(sp.getCreatorId(), currentUserId)) {
                    throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(),
                            "您只能删除自己创建的业务员档案，包含ID=" + sp.getSalespersonId());
                }
            }
        }

        // 批量更新删除状态
        List<Salesperson> updateList = toDeleteList.stream().map(sp -> {
            Salesperson update = new Salesperson();
            update.setSalespersonId(sp.getSalespersonId());
            update.setDeleted(1);
            return update;
        }).collect(Collectors.toList());

        for (Salesperson update : updateList) {
            salespersonMapper.updateById(update);
        }

        // 记录批量删除日志
        logRecordService.recordDataChangeLog("业务员档案", "SALESPERSON", String.join(",",
                        toDeleteList.stream().map(sp -> String.valueOf(sp.getSalespersonId())).collect(Collectors.toList())),
                "批量删除", "批量删除业务员，共" + toDeleteList.size() + "个",
                null, updateList, null, null, true, null);
    }

    private LambdaQueryWrapper<Salesperson> buildQueryWrapper(SalespersonPageRequest request) {
        LambdaQueryWrapper<Salesperson> wrapper = new LambdaQueryWrapper<Salesperson>()
                .eq(Salesperson::getDeleted, 0);
        if (StringUtils.hasText(request.getSalespersonName())) {
            wrapper.like(Salesperson::getSalespersonName, request.getSalespersonName());
        }
        if (StringUtils.hasText(request.getPartyAName())) {
            wrapper.like(Salesperson::getPartyAName, request.getPartyAName());
        }
        if (StringUtils.hasText(request.getSalespersonPhone())) {
            wrapper.like(Salesperson::getSalespersonPhone, request.getSalespersonPhone());
        }
        return wrapper;
    }

    private void copyFromRequest(SalespersonCreateRequest req, Salesperson sp) {
        sp.setEmployeeId(req.getEmployeeId());
        sp.setSalespersonName(req.getSalespersonName());
        sp.setSalespersonPhone(req.getSalespersonPhone());
        sp.setSalespersonIdCard(req.getSalespersonIdCard());
        sp.setPartyAName(req.getPartyAName());
        sp.setPartyACreditCode(req.getPartyACreditCode());
        sp.setPartyBName(req.getPartyBName());
        sp.setPartyBCreditCode(req.getPartyBCreditCode());
        sp.setPartyBContactPerson(req.getPartyBContactPerson());
        sp.setPartyBContactPhone(req.getPartyBContactPhone());
        sp.setBankName(req.getBankName());
        sp.setCardNumber(req.getCardNumber());
        sp.setAccountName(req.getAccountName());
        sp.setRemark(req.getRemark());
    }

    private SalespersonSelectResponse toSelectResponse(Salesperson sp) {
        SalespersonSelectResponse resp = new SalespersonSelectResponse();
        resp.setSalespersonId(sp.getSalespersonId());
        resp.setSalespersonName(sp.getSalespersonName());
        return resp;
    }

    private SalespersonPageResponse toPageResponse(Salesperson sp) {
        SalespersonPageResponse resp = new SalespersonPageResponse();
        resp.setSalespersonId(sp.getSalespersonId());
        resp.setEmployeeId(sp.getEmployeeId());
        resp.setSalespersonName(sp.getSalespersonName());
        resp.setSalespersonPhone(sp.getSalespersonPhone());
        resp.setSalespersonIdCard(sp.getSalespersonIdCard());
        resp.setPartyAName(sp.getPartyAName());
        resp.setPartyACreditCode(sp.getPartyACreditCode());
        resp.setPartyBName(sp.getPartyBName());
        resp.setPartyBCreditCode(sp.getPartyBCreditCode());
        resp.setPartyBContactPerson(sp.getPartyBContactPerson());
        resp.setPartyBContactPhone(sp.getPartyBContactPhone());
        resp.setBankName(sp.getBankName());
        resp.setCardNumber(sp.getCardNumber());
        resp.setAccountName(sp.getAccountName());
        resp.setRemark(sp.getRemark());
        resp.setCreatorId(sp.getCreatorId());
        resp.setCreatorName(sp.getCreatorName());
        if (sp.getCreateTime() != null) {
            resp.setCreateTime(sp.getCreateTime().format(DATETIME_FMT));
        }
        return resp;
    }
}
