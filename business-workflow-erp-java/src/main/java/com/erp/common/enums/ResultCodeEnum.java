package com.erp.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    /**
     * 成功
     */
    SUCCESS(200, "成功"),

    /**
     * 失败
     */
    ERROR(500, "失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 方法不允许
     */
    METHOD_NOT_ALLOWED(405, "方法不允许"),

    /**
     * 服务器错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器错误"),

    /**
     * 业务异常
     */
    BUSINESS_ERROR(600, "业务异常"),

    /**
     * 登录失败
     */
    LOGIN_ERROR(601, "登录失败"),

    /**
     * 验证码错误
     */
    CAPTCHA_ERROR(602, "验证码错误"),

    /**
     * Token过期
     */
    TOKEN_EXPIRED(603, "Token过期"),

    /**
     * Token无效
     */
    TOKEN_INVALID(604, "Token无效"),

    /**
     * 权限不足
     */
    PERMISSION_DENIED(605, "权限不足"),

    /**
     * 数据不存在
     */
    DATA_NOT_FOUND(606, "数据不存在"),

    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS(607, "数据已存在"),

    /**
     * 操作失败
     */
    OPERATION_FAILED(608, "操作失败"),

    /**
     * 操作不允许
     */
    OPERATION_NOT_ALLOWED(609, "操作不允许"),

    /**
     * 参数无效
     */
    PARAM_INVALID(610, "参数无效"),

    /**
     * 数据冲突（乐观锁冲突）
     */
    DATA_CONFLICT(611, "数据已被修改，请刷新后重试"),

    /**
     * 合同已锁定，无法编辑/删除
     */
    CONTRACT_LOCKED(612, "合同已锁定，无法操作"),

    /**
     * 合同状态不允许删除
     */
    CONTRACT_CANNOT_DELETE(613, "当前状态不允许删除合同"),

    /**
     * 用户未登录
     */
    USER_NOT_LOGIN(614, "用户未登录");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;
}











































