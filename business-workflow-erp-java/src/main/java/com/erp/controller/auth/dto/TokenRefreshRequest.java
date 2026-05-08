package com.erp.controller.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Token刷新请求DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
public class TokenRefreshRequest {

    /**
     * 刷新Token
     */
    @NotBlank(message = "刷新Token不能为空")
    private String refreshToken;
}





