package com.erp.controller.system.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * AI 助手对话响应
 *
 * @author ERP
 */
@Data
@ApiModel("AI助手对话响应")
public class AiChatResponse {

    /**
     * AI 返回的内容
     */
    @ApiModelProperty(value = "AI回复内容", example = "您可以在生产执行模块的收运通知单列表中点击“新增收运通知单”…")
    private String answer;
}




