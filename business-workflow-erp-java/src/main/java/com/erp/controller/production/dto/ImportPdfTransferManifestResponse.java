package com.erp.controller.production.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 转移联单 PDF 批量导入响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportPdfTransferManifestResponse {

    /** PDF 中识别到的总分页数（每个「危险废物转移联单」标题页为一个分页） */
    private Integer total;

    /** 成功匹配并绑定的联单数 */
    private Integer matched;

    /** 未能匹配到数据库记录的分页数 */
    private Integer unmatched;

    /** 未匹配详情列表（每项格式："页码范围: 提取数字=[xxx], 原因"） */
    private List<String> unmatchedDetails;
}
