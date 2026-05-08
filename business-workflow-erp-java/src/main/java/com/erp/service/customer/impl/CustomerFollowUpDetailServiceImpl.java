package com.erp.service.customer.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.customer.dto.CustomerFollowUpDetailCreateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpDetailResponse;
import com.erp.controller.customer.dto.CustomerFollowUpDetailUpdateRequest;
import com.erp.controller.customer.dto.CustomerFollowUpResponse;
import com.erp.controller.customer.dto.CustomerFollowUpWithDetailsResponse;
import com.erp.entity.customer.CustomerFollowUp;
import com.erp.entity.customer.CustomerFollowUpDetail;
import com.erp.mapper.customer.CustomerFollowUpDetailMapper;
import com.erp.mapper.customer.CustomerFollowUpMapper;
import com.erp.mapper.system.EmployeeMapper;
import com.erp.service.customer.CustomerFollowUpDetailService;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户跟进明细服务实现
 */
@Slf4j
@Service
public class CustomerFollowUpDetailServiceImpl implements CustomerFollowUpDetailService {

    @Autowired
    private CustomerFollowUpDetailMapper detailMapper;

    @Autowired
    private CustomerFollowUpMapper followUpMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 获取当前登录用户ID
     */
    private Integer getCurrentUserId() {
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        return userId;
    }

    @Override
    public CustomerFollowUpWithDetailsResponse getFollowUpWithDetails(Integer followUpId) {
        // 查询主记录
        CustomerFollowUp followUp = followUpMapper.selectById(followUpId);
        if (followUp == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "跟进记录不存在");
        }

        // 转换为响应对象
        CustomerFollowUpWithDetailsResponse response = new CustomerFollowUpWithDetailsResponse();
        BeanUtils.copyProperties(followUp, response);

        // 查询明细列表
        List<CustomerFollowUpDetailResponse> details = getDetailsByFollowUpId(followUpId);
        response.setDetails(details);

        return response;
    }

    @Override
    public List<CustomerFollowUpDetailResponse> getDetailsByFollowUpId(Integer followUpId) {
        List<CustomerFollowUpDetail> details = detailMapper.selectByFollowUpId(followUpId);
        return details.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerFollowUpDetailResponse createDetail(CustomerFollowUpDetailCreateRequest request) {
        Integer currentUserId = getCurrentUserId();

        // 验证跟进记录是否存在
        CustomerFollowUp followUp = followUpMapper.selectById(request.getFollowUpId());
        if (followUp == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "跟进记录不存在");
        }

        // 创建明细记录
        CustomerFollowUpDetail detail = new CustomerFollowUpDetail();
        detail.setFollowUpId(request.getFollowUpId());
        detail.setFollowTime(request.getFollowTime());
        detail.setFollowContent(request.getFollowContent());
        detail.setFollowStatus(request.getFollowStatus() != null ? request.getFollowStatus() : "未完成");
        detail.setCreatorId(currentUserId);
        detail.setRemark(request.getRemark());
        detail.setCreateTime(LocalDateTime.now());

        int result = detailMapper.insert(detail);
        if (result <= 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "新增跟进明细失败");
        }

        // 查询完整的明细记录
        CustomerFollowUpDetail savedDetail = detailMapper.selectById(detail.getDetailId());
        return convertToDetailResponse(savedDetail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerFollowUpDetailResponse updateDetail(CustomerFollowUpDetailUpdateRequest request) {
        Integer currentUserId = getCurrentUserId();

        // 查询现有明细
        CustomerFollowUpDetail detail = detailMapper.selectById(request.getDetailId());
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "跟进明细不存在");
        }

        // 已完成的明细不能修改
        if ("已完成".equals(detail.getFollowStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "已完成的明细不能修改");
        }

        // 更新明细
        detail.setFollowTime(request.getFollowTime());
        detail.setFollowContent(request.getFollowContent());
        detail.setFollowStatus(request.getFollowStatus());
        detail.setRemark(request.getRemark());
        detail.setUpdateTime(LocalDateTime.now());

        int result = detailMapper.updateById(detail);
        if (result <= 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "更新跟进明细失败");
        }

        // 查询更新后的明细
        CustomerFollowUpDetail updatedDetail = detailMapper.selectById(request.getDetailId());
        return convertToDetailResponse(updatedDetail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerFollowUpDetailResponse completeDetail(Integer detailId) {
        Integer currentUserId = getCurrentUserId();

        // 查询现有明细
        CustomerFollowUpDetail detail = detailMapper.selectById(detailId);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "跟进明细不存在");
        }

        // 已完成的明细不能再标记为完成
        if ("已完成".equals(detail.getFollowStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "该明细已经是已完成状态");
        }

        // 标记为完成
        detail.setFollowStatus("已完成");
        detail.setUpdateTime(LocalDateTime.now());

        int result = detailMapper.updateById(detail);
        if (result <= 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "完成跟进明细失败");
        }

        // 查询更新后的明细
        CustomerFollowUpDetail completedDetail = detailMapper.selectById(detailId);
        return convertToDetailResponse(completedDetail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDetail(Integer detailId) {
        Integer currentUserId = getCurrentUserId();

        // 查询现有明细
        CustomerFollowUpDetail detail = detailMapper.selectById(detailId);
        if (detail == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "跟进明细不存在");
        }

        // 已完成的明细不能删除
        if ("已完成".equals(detail.getFollowStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "已完成的明细不能删除");
        }

        int result = detailMapper.deleteByDetailId(detailId);
        if (result <= 0) {
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "删除跟进明细失败");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDetailsBatch(List<Integer> detailIds) {
        if (detailIds == null || detailIds.isEmpty()) {
            return 0;
        }

        // 检查是否有已完成的明细
        for (Integer detailId : detailIds) {
            CustomerFollowUpDetail detail = detailMapper.selectById(detailId);
            if (detail != null && "已完成".equals(detail.getFollowStatus())) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "已完成的明细不能删除，ID：" + detailId);
            }
        }

        return detailMapper.deleteBatchByIds(detailIds);
    }

    /**
     * 转换为明细响应对象
     */
    private CustomerFollowUpDetailResponse convertToDetailResponse(CustomerFollowUpDetail detail) {
        CustomerFollowUpDetailResponse response = new CustomerFollowUpDetailResponse();
        response.setDetailId(detail.getDetailId());
        response.setFollowUpId(detail.getFollowUpId());
        response.setFollowTime(detail.getFollowTime());
        response.setFollowContent(detail.getFollowContent());
        response.setFollowStatus(detail.getFollowStatus());
        response.setCreatorId(detail.getCreatorId());
        response.setCreateTime(detail.getCreateTime());
        response.setUpdateTime(detail.getUpdateTime());
        response.setRemark(detail.getRemark());

        // 查询创建人姓名
        if (detail.getCreatorId() != null) {
            try {
                var employee = employeeMapper.selectById(detail.getCreatorId());
                if (employee != null) {
                    response.setCreatorName(employee.getEmployeeName());
                }
            } catch (Exception e) {
                log.warn("查询明细创建人姓名失败：detailId={}, creatorId={}", detail.getDetailId(), detail.getCreatorId());
            }
        }

        return response;
    }
}
