package com.erp.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Excel导入结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /**
     * 总记录数
     */
    private Integer totalRecords;

    /**
     * 成功导入数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failureCount;

    /**
     * 错误信息列表
     */
    private List<String> errorMessages;

    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return failureCount == 0;
    }
}