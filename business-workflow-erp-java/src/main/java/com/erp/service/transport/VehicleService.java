package com.erp.service.transport;

import com.erp.controller.transport.dto.VehicleArchiveResponse;
import com.erp.controller.transport.dto.VehicleCreateRequest;
import com.erp.controller.transport.dto.VehicleDetailResponse;
import com.erp.controller.transport.dto.VehicleImportResponse;
import com.erp.controller.transport.dto.VehiclePageRequest;
import com.erp.controller.transport.dto.VehicleUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 车辆管理服务接口
 */
public interface VehicleService {

    /**
     * 获取车辆档案列表（分页，包含统计信息）
     *
     * @param request 查询条件
     * @return 车辆档案响应（包含统计信息）
     */
    VehicleArchiveResponse getVehicleArchive(VehiclePageRequest request);

    /**
     * 获取车辆详情
     *
     * @param vehicleId 车辆ID
     * @return 车辆详情
     */
    VehicleDetailResponse getVehicleDetail(Integer vehicleId);

    /**
     * 创建车辆档案
     *
     * @param request 创建请求
     * @return 车辆详情
     */
    VehicleDetailResponse createVehicle(VehicleCreateRequest request);

    /**
     * 更新车辆档案
     *
     * @param request 更新请求
     */
    void updateVehicle(VehicleUpdateRequest request);

    /**
     * 删除车辆档案
     *
     * @param vehicleId 车辆ID
     */
    void deleteVehicle(Integer vehicleId);

    /**
     * 导出车辆列表
     *
     * @param request 查询条件
     * @return 车辆列表
     */
    List<VehicleDetailResponse> listVehiclesForExport(VehiclePageRequest request);

    /**
     * 批量导入车辆
     *
     * @param file Excel文件
     * @return 导入结果
     */
    VehicleImportResponse importVehicles(MultipartFile file);
}

