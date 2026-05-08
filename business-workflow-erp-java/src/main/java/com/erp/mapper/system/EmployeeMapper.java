package com.erp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.entity.system.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 员工Mapper接口
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 根据登录账号查询员工
     *
     * @param loginAccount 登录账号
     * @return 员工信息
     */
    Employee selectByLoginAccount(@Param("loginAccount") String loginAccount);

    /**
     * 根据邮箱查询员工
     *
     * @param email 邮箱地址
     * @return 员工信息
     */
    Employee selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询员工
     *
     * @param phone 手机号码
     * @return 员工信息
     */
    Employee selectByPhone(@Param("phone") String phone);

    /**
     * 分页查询员工列表
     *
     * @param page 分页对象
     * @param employeeName 员工姓名（模糊搜索）
     * @param department 部门（模糊搜索）
     * @param jobTitle 岗位（模糊搜索）
     * @param phone 联系方式（模糊搜索）
     * @param loginAccount 登录账号（模糊搜索）
     * @param employeeStatus 员工状态（精确匹配）
     * @param sortField 排序字段
     * @param sortOrder 排序方向：asc/desc
     * @return 分页结果
     */
    IPage<Employee> selectPageList(
            Page<Employee> page,
            @Param("employeeName") String employeeName,
            @Param("department") String department,
            @Param("jobTitle") String jobTitle,
            @Param("phone") String phone,
            @Param("loginAccount") String loginAccount,
            @Param("employeeStatus") String employeeStatus,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );
}





