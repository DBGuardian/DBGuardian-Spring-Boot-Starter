package com.erp.controller.finance.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 运输记录查询响应DTO
 */
@Data
@ApiModel("运输记录查询响应")
public class TransportRecordResponseDTO {

    @ApiModelProperty("运输记录列表")
    private List<TransportRecordDTO> transportRecords;
}
