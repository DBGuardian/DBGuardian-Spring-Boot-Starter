package com.erp.controller.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePermissionDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer permissionId;
    private String status; // "ALLOW" or "DENY"
}



