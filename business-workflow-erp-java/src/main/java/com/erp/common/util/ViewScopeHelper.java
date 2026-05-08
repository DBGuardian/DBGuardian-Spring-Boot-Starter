package com.erp.common.util;

import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.enums.ViewScopeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.service.auth.AuthService;

/**
 * 数据范围视图辅助工具
 * 用于处理列表查询时的数据范围权限控制
 *
 * <p>
 * 使用规则：
 * <ul>
 *   <li>如果传入viewScope为有效值(SELF/ALL)，直接返回该值</li>
 *   <li>如果传入viewScope为null或空，根据当前用户权限自动判断</li>
 * </ul>
 *
 * @author ERP System
 */
public class ViewScopeHelper {

    private ViewScopeHelper() {
        // 私有构造函数，防止实例化
    }

    /**
     * 解析视图范围
     * <p>
     * 规则：
     * <ul>
     *   <li>如果传入值为有效范围(SELF/ALL)，直接返回</li>
     *   <li>如果传入值为null或空，根据当前用户权限自动判断</li>
     * </ul>
     *
     * @param pageCode  页面权限编码
     * @param viewScope 传入的视图范围（可为null）
     * @return 解析后的视图范围（SELF/ALL）
     */
    public static String resolveViewScope(String pageCode, String viewScope) {
        // 1. 校验登录状态
        Integer currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_LOGIN.getCode(), ResultCodeEnum.USER_NOT_LOGIN.getMessage());
        }

        // 2. 如果传入明确的viewScope，直接返回
        if (ViewScopeEnum.isValid(viewScope)) {
            return viewScope.toUpperCase();
        }

        // 3. 根据当前用户权限自动判断
        return getViewScopeFromPermission(pageCode);
    }

    /**
     * 判断是否为SELF模式（仅查看自己）
     *
     * @param viewScope 视图范围
     * @return 是否为SELF模式
     */
    public static boolean isSelfScope(String viewScope) {
        return ViewScopeEnum.isSelf(viewScope);
    }

    /**
     * 判断是否为ALL模式（查看全部）
     *
     * @param viewScope 视图范围
     * @return 是否为ALL模式
     */
    public static boolean isAllScope(String viewScope) {
        return ViewScopeEnum.isAll(viewScope);
    }

    /**
     * 获取默认的视图范围（用于新用户或无权限配置时）
     *
     * @return 默认视图范围（SELF）
     */
    public static String getDefaultScope() {
        return ViewScopeEnum.SELF.getCode();
    }

    /**
     * 从权限服务获取视图范围
     *
     * @param pageCode 页面权限编码
     * @return 视图范围
     */
    private static String getViewScopeFromPermission(String pageCode) {
        try {
            AuthService authService = SpringContextHolder.getBean(AuthService.class);
            return authService.getViewScope(pageCode);
        } catch (Exception e) {
            // 获取失败时，默认返回SELF（安全策略）
            return ViewScopeEnum.SELF.getCode();
        }
    }
}
