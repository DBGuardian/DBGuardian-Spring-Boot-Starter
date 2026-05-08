package com.erp.common.enums;

/**
 * 入库单状态枚举
 */
public enum WarehousingStatusEnum {

    /** 待结算 */
    PENDING_SETTLEMENT("待结算", "warning"),

    /** 结算中 */
    SETTLING("结算中", "info"),

    /** 已结算 */
    SETTLED("已结算", "success"),

    /** 已锁定 */
    LOCKED("已锁定", "danger");

    private final String value;
    private final String color;

    WarehousingStatusEnum(String value, String color) {
        this.value = value;
        this.color = color;
    }

    public String getValue() {
        return value;
    }

    public String getColor() {
        return color;
    }

    /**
     * 根据字符串值获取枚举
     * @param value 字符串值
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果值不存在
     */
    public static WarehousingStatusEnum fromValue(String value) {
        for (WarehousingStatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown warehousing status: " + value);
    }

    /**
     * 获取所有状态值数组
     * @return 状态值数组
     */
    public static String[] getAllValues() {
        WarehousingStatusEnum[] values = values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].value;
        }
        return result;
    }
}
