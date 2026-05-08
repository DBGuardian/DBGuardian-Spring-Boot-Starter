package com.erp.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SIMPLE 模式写操作权限控制注解
 *
 * <p>用于 SIMPLE 模式页面（pageMode=SIMPLE）的写操作接口（新增、编辑、删除、导入等）。
 * 校验当前员工对指定页面的 {@code can_edit=1}，不区分数据范围（不检查 operateScope）。</p>
 *
 * <p>校验逻辑：</p>
 * <ol>
 *     <li>员工必须已登录（JWT 有效）；</li>
 *     <li>员工在 {@code employee_permission} 表中对 {@code value()} 指定页面的 {@code can_edit=1}；</li>
 *     <li>若未找到权限配置记录，视为 {@code can_edit=0}，拒绝访问；</li>
 *     <li>超级管理员（super_admin）直接放行。</li>
 * </ol>
 *
 * <pre>
 * 示例：
 * {@code
 * @RequireSimplePageEdit("财务管理:账户设置:科目管理:页面")
 * @PostMapping
 * public Result<FundSubject> createSubject(...) { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSimplePageEdit {

    /**
     * 页面级权限编码，例如 "财务管理:账户设置:科目管理:页面"。
     * 切面将查询该页面对应的 employee_permission 记录，校验 can_edit=1。
     */
    String value();
}
