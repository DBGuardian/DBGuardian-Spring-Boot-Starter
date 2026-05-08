package com.erp.controller.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token刷新响应DTO
 *
 * @author ERP System
 * @date 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新的访问Token
     */
    private String token;

    /**
     * 新的刷新Token
     */
    private String refreshToken;
}





