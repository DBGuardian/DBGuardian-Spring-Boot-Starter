package com.erp.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 页面级权限控制注解
 *
 * <p>用于在 Controller 列表 / 主查询接口上声明所需的页面级权限编码。
 * 支持一个接口绑定一个或多个页面权限编码，只要当前员工拥有其中任意一个编码，即认为具备访问权限。</p>
 *
 * <pre>
 * 示例：
 * {@code
 * @RequirePagePermission("档案管理:客户档案:页面")
 * public Result<IPage<CustomerPageResponse>> getCustomerPage(...) { ... }
 *
 * @RequirePagePermission({
 *     "人事管理:员工档案:页面",
 *     "系统管理:员工管理:页面"
 * })
 * public Result<IPage<EmployeePageResponse>> getEmployeePage(...) { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePagePermission {

    /**
     * 页面级权限编码列表，例如 "档案管理:客户档案:页面"。
     *
     * <p>当配置多个编码时，表示当前接口可被拥有任意一个编码的员工访问。</p>
     */
    String[] value();
}

