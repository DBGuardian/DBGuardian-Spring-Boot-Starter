package com.erp.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据查看范围枚举
 * 用于控制列表查询时的数据权限范围
 *
 * @author ERP System
 */
@Getter
@AllArgsConstructor
public enum ViewScopeEnum {

    /**
     * 仅查看自己创建的数据
     */
    SELF("SELF", "仅查看自己"),

    /**
     * 查看全部数据（需权限）
     */
    ALL("ALL", "查看全部");

    private final String code;
    private final String description;

    /**
     * 根据编码获取枚举
     *
     * @param code 编码
     * @return 枚举值
     */
    public static ViewScopeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ViewScopeEnum scope : values()) {
            if (scope.code.equalsIgnoreCase(code)) {
                return scope;
            }
        }
        return null;
    }

    /**
     * 判断是否为有效的作用域
     *
     * @param code 编码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        return SELF.code.equalsIgnoreCase(code) || ALL.code.equalsIgnoreCase(code);
    }

    /**
     * 判断是否为SELF模式
     *
     * @param code 编码
     * @return 是否为SELF模式
     */
    public static boolean isSelf(String code) {
        return SELF.code.equalsIgnoreCase(code);
    }

    /**
     * 判断是否为ALL模式
     *
     * @param code 编码
     * @return 是否为ALL模式
     */
    public static boolean isAll(String code) {
        return ALL.code.equalsIgnoreCase(code);
    }
}
