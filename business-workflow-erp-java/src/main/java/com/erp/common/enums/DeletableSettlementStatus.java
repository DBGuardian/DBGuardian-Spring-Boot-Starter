package com.erp.common.enums;

/**
 * 可删除的结算单状态枚举
 * 定义允许删除的结算单状态
 *
 * @author ERP System
 * @date 2026-01-25
 */
public enum DeletableSettlementStatus {

    /** 待审核 */
    PENDING_AUDIT("待审核"),

    /** 已驳回 */
    REJECTED("已驳回");

    private final String value;

    DeletableSettlementStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 判断给定的状态是否允许删除
     * @param status 结算单状态
     * @return true如果允许删除，false否则
     */
    public static boolean isDeletable(String status) {
        for (DeletableSettlementStatus deletableStatus : values()) {
            if (deletableStatus.value.equals(status)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有可删除状态的值数组
     * @return 可删除状态值数组
     */
    public static String[] getDeletableStatuses() {
        DeletableSettlementStatus[] values = values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].value;
        }
        return result;
    }
}
