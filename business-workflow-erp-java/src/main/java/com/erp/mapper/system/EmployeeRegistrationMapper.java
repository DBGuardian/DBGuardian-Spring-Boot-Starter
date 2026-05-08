package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.system.EmployeeRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 员工注册Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface EmployeeRegistrationMapper extends BaseMapper<EmployeeRegistration> {

    /**
     * 根据登录账号查询注册信息
     *
     * @param loginAccount 登录账号
     * @return 注册信息
     */
    EmployeeRegistration selectByLoginAccount(@Param("loginAccount") String loginAccount);

    /**
     * 根据手机号码查询注册信息
     *
     * @param phone 手机号码
     * @return 注册信息
     */
    EmployeeRegistration selectByPhone(@Param("phone") String phone);

    /**
     * 分页查询员工注册信息
     *
     * @param page 分页对象
     * @param employeeName 员工姓名（模糊搜索）
     * @param department 部门（模糊搜索）
     * @param jobTitle 岗位（模糊搜索）
     * @param phone 手机号码（模糊搜索）
     * @param loginAccount 登录账号（模糊搜索）
     * @param auditStatus 审核状态（精确匹配）
     * @param permissionStatus 权限分配状态（精确匹配）
     * @return 分页结果
     */
    IPage<EmployeeRegistration> selectPageList(
            Page<EmployeeRegistration> page,
            @Param("employeeName") String employeeName,
            @Param("department") String department,
            @Param("jobTitle") String jobTitle,
            @Param("phone") String phone,
            @Param("loginAccount") String loginAccount,
            @Param("auditStatus") String auditStatus,
            @Param("permissionStatus") String permissionStatus
    );
}



