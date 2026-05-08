package com.erp.controller.transport.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 收运通知单附件响应
 */
@Data
@ApiModel("收运通知单附件响应")
public class TransportAttachmentResponse {

    @ApiModelProperty("文件ID")
    private String fileId;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("文件URL")
    private String fileUrl;

    @ApiModelProperty("附件类型：cargo（货物照片/视频）、qrcode（二维码）")
    private String type;

    @ApiModelProperty("文件大小（字节）")
    private Long size;
}

