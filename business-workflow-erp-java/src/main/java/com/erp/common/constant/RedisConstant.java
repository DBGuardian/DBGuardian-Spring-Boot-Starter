package com.erp.common.constant;

/**
 * Redis常量
 *
 * @author ERP System
 * @date 2025-01-01
 */
public class RedisConstant {

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "token:";

    /**
     * 刷新Token前缀
     */
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * 验证码前缀
     */
    public static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * 权限信息前缀
     */
    public static final String PERMISSION_PREFIX = "permission:";

    /**
     * 字典数据前缀
     */
    public static final String DICT_PREFIX = "dict:";

    /**
     * 危险废物名录前缀
     */
    public static final String WASTE_CODE_PREFIX = "waste_code:";

    /**
     * 用户信息前缀
     */
    public static final String USER_PREFIX = "user:";

    /**
     * 登录失败次数前缀
     */
    public static final String LOGIN_FAIL_COUNT_PREFIX = "login_fail_count:";

    /**
     * 登录锁定前缀
     */
    public static final String LOGIN_LOCK_PREFIX = "login_lock:";

    /**
     * 邮件通道配置缓存Key
     */
    public static final String EMAIL_CHANNEL_CONFIG_KEY = "sys:email_channel_config";

    /**
     * 忘记密码邮箱验证码缓存前缀
     */
    public static final String RESET_EMAIL_CODE_PREFIX = "auth:reset_email_code:";

    /**
     * 验证码IP限流前缀
     */
    public static final String CAPTCHA_RATE_LIMIT_PREFIX = "captcha_rate:";

    /**
     * IP登录失败次数前缀
     */
    public static final String IP_LOGIN_FAIL_COUNT_PREFIX = "ip_login_fail_count:";

    /**
     * IP登录锁定前缀
     */
    public static final String IP_LOGIN_LOCK_PREFIX = "ip_login_lock:";

    /**
     * 业务闭环校验缓存前缀
     */
    public static final String CLOSURE_VALIDATION_PREFIX = "closure_validation:";

    /**
     * 业务闭环校验问题列表缓存key
     */
    public static final String CLOSURE_VALIDATION_ISSUES_KEY = CLOSURE_VALIDATION_PREFIX + "issues";

    /**
     * 业务闭环校验看板统计缓存key
     */
    public static final String CLOSURE_VALIDATION_DASHBOARD_KEY = CLOSURE_VALIDATION_PREFIX + "dashboard";

    /**
     * 业务闭环校验最后更新时间缓存key
     */
    public static final String CLOSURE_VALIDATION_LAST_UPDATE_KEY = CLOSURE_VALIDATION_PREFIX + "last_update";

    /**
     * 业务闭环校验缓存过期时间（24小时）
     */
    public static final long CLOSURE_VALIDATION_CACHE_TTL = 24 * 60 * 60;
}














