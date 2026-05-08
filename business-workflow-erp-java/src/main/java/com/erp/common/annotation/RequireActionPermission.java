package com.erp.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动作级权限控制注解
 *
 * <p>用于在 Controller 的“按钮/操作类接口”上声明所需的动作级权限编码。</p>
 *
 * <pre>
 * 示例：
 * {@code
 * @RequireActionPermission("业务管理:收运通知:新增")
 * public Result<Void> createTransportApply(...) { ... }
 *
 * @RequireActionPermission({
 *     "合同结算:危险废物结算-收款结算:审核",
 *     "合同结算:危险废物结算-付款结算:审核"
 * })
 * public Result<Void> auditSettlement(...) { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireActionPermission {

    /**
     * 动作级权限编码列表，例如 "业务管理:收运通知:新增"。
     *
     * <p>当配置多个编码时，表示当前接口可被拥有任意一个编码的员工访问。</p>
     */
    String[] value();
}

