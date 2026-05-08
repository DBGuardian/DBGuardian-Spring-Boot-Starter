package com.erp.service.finance;

import com.erp.controller.finance.dto.FundDiaryImportResult;
import com.erp.controller.finance.dto.FundDiaryRequest;
import com.erp.controller.finance.dto.FundDiaryResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 日记账服务接口
 */
public interface FundDiaryService {

    /**
     * 获取日记账明细
     *
     * @param request 查询请求
     * @return 日记账响应数据
     */
    FundDiaryResponse getDiary(FundDiaryRequest request);

    /**
     * 生成日记账PDF
     *
     * @param request 查询请求
     * @return PDF文件预览URL
     */
    String generateDiaryPdf(FundDiaryRequest request);

    /**
     * 批量生成日记账PDF（连续打印）
     *
     * @param requests 查询请求列表（每个请求对应一个账户）
     * @return PDF文件预览URL
     */
    String generateBatchDiaryPdf(List<FundDiaryRequest> requests);

    /**
     * 下载日记账导入Excel模板
     *
     * @param response HTTP响应对象
     */
    void downloadFundDiaryTemplate(HttpServletResponse response);

    /**
     * Excel导入日记账数据
     *
     * @param excelFile Excel文件
     * @param accountId 账户ID
     * @param periodId 账期ID
     * @return 导入结果
     */
    FundDiaryImportResult excelImportFundDiary(org.springframework.web.multipart.MultipartFile excelFile, Long accountId, Long periodId);

    /**
     * 导出日记账Excel文件
     *
     * @param request 查询请求
     * @param response HTTP响应对象
     */
    void exportFundDiaryExcel(FundDiaryRequest request, HttpServletResponse response);

    /**
     * 导出日记账回单文件（ZIP包）
     *
     * 从当前账期中查询哪些流水有回单文件编号，然后从服务器中找到这些文件并打包成ZIP下载
     * 如果回单文件编号有值但是服务器中没有对应文件，则忽略，找下一个
     * 如果找到的文件为0个，才弹出提示，没有找到对应的回单文件
     *
     * @param periodId 账期ID（必填）
     * @param organizationId 组织ID（必填）
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    void exportFundDiaryReceipts(Long periodId, Long organizationId, HttpServletResponse response) throws IOException;
}


