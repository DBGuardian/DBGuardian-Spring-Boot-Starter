package com.erp.service.production;

import com.erp.controller.transport.dto.TransportApplyDetailRequest;
import com.erp.controller.transport.dto.TransportApplyDetailResponse;
import com.erp.controller.transport.dto.TransportApplyPageRequest;
import com.erp.controller.transport.dto.WasteCategoryLimitResponse;

import java.util.List;

/**
 * 收运通知单服务接口
 */
public interface PickupNoticeService {

    /**
     * 创建收运通知单
     *
     * @param request 创建请求
     * @return 收运通知单详情
     */
    TransportApplyDetailResponse createPickupNotice(TransportApplyDetailRequest request);

    /**
     * 更新收运通知单
     *
     * @param request 更新请求
     * @return 收运通知单详情
     */
    TransportApplyDetailResponse updatePickupNotice(TransportApplyDetailRequest request);

    /**
     * 根据收运通知单号查询详情
     *
     * @param noticeCode 收运通知单号
     * @return 收运通知单详情
     */
    TransportApplyDetailResponse getPickupNoticeDetail(String noticeCode);

    /**
     * 根据收运通知单编号查询详情
     *
     * @param noticeId 收运通知单编号
     * @return 收运通知单详情
     */
    TransportApplyDetailResponse getPickupNoticeDetailById(Integer noticeId);

    /**
     * 分页查询收运通知单列表
     *
     * @param request 分页查询请求
     * @return 分页结果（包含统计信息）
     */
    com.erp.controller.transport.dto.TransportApplyListResponse getPickupNoticePage(TransportApplyPageRequest request);

    /**
     * 提交收运通知单审核
     *
     * @param noticeCode 收运通知单号
     */
    void submitPickupNotice(String noticeCode);

    /**
     * 撤回收运通知单
     *
     * @param noticeCode 收运通知单号
     */
    void revokePickupNotice(String noticeCode);

    /**
     * 删除收运通知单
     *
     * @param noticeCode 收运通知单号
     */
    void deletePickupNotice(String noticeCode);

    /**
     * 审核收运通知单
     *
     * @param noticeCode 收运通知单号
     * @param auditResult 审核结果：待调度（通过）/审核失败（拒绝）
     * @param auditOpinion 审核意见（拒绝时必填）
     */
    void auditPickupNotice(String noticeCode, String auditResult, String auditOpinion);

    /**
     * 审核阶段补充/更新合同号
     *
     * @param noticeCode 收运通知单号
     * @param contractCode 合同号
     */
    void bindContractDuringAudit(String noticeCode, String contractCode);

    /**
     * 根据废物代码列表查询对应危废类别的限额与已入库量
     *
     * @param wasteCodes 废物代码列表
     * @return 危废类别限额与已入库量
     */
    List<WasteCategoryLimitResponse> getWasteCategoryLimits(List<String> wasteCodes);

    /**
     * 查询收运通知单列表用于导出（不分页，返回所有符合条件的数据）
     *
     * @param request 查询请求（筛选条件）
     * @return 收运通知单列表
     */
    List<com.erp.controller.transport.dto.TransportApplyPageResponse> listPickupNoticesForExport(TransportApplyPageRequest request);

    /**
     * 车辆安排分页查询（使用"运输管理:车辆安排:页面"权限编码查询viewScope）
     *
     * @param request 分页查询请求
     * @return 分页结果（包含统计信息）
     */
    com.erp.controller.transport.dto.TransportApplyListResponse getVehicleArrangeNoticePage(TransportApplyPageRequest request);

    /**
     * 车辆安排导出（使用"运输管理:车辆安排:页面"权限编码查询viewScope）
     *
     * @param request 查询请求（筛选条件）
     * @return 收运通知单列表
     */
    List<com.erp.controller.transport.dto.TransportApplyPageResponse> listVehicleArrangeNoticesForExport(TransportApplyPageRequest request);

    /**
     * 批量提交收运通知单审核
     *
     * @param noticeCodes 收运通知单号列表
     */
    void batchSubmitPickupNotices(List<String> noticeCodes);

    /**
     * 批量撤回收运通知单
     *
     * @param noticeCodes 收运通知单号列表
     */
    void batchRevokePickupNotices(List<String> noticeCodes);

    /**
     * 批量删除收运通知单
     *
     * @param noticeCodes 收运通知单号列表
     */
    void batchDeletePickupNotices(List<String> noticeCodes);
}

