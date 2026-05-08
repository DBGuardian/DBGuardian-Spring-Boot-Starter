package com.erp.service.transport.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.util.SecurityUtil;
import com.erp.common.util.ViewScopeHelper;
import com.erp.controller.transport.dto.TransportStat;
import com.erp.controller.transport.dto.VehicleArchiveResponse;
import com.erp.controller.transport.dto.VehicleCreateRequest;
import com.erp.controller.transport.dto.VehicleDetailResponse;
import com.erp.controller.transport.dto.VehicleImportError;
import com.erp.controller.transport.dto.VehicleImportResponse;
import com.erp.controller.transport.dto.VehiclePageRequest;
import com.erp.controller.transport.dto.VehiclePageResponse;
import com.erp.controller.transport.dto.VehicleUpdateRequest;
import com.erp.entity.transport.Vehicle;
import com.erp.entity.system.EmployeePermission;
import com.erp.entity.system.Permission;
import com.erp.mapper.transport.VehicleMapper;
import com.erp.mapper.transport.TransportContractVehicleMapper;
import com.erp.mapper.system.EmployeeRoleMapper;
import com.erp.mapper.system.EmployeePermissionMapper;
import com.erp.mapper.system.PermissionMapper;
import com.erp.service.auth.AuthService;
import com.erp.service.transport.VehicleService;
import com.erp.service.system.ILogRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * 车辆管理服务实现
 */
@Slf4j
@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private TransportContractVehicleMapper transportContractVehicleMapper;

    @Autowired
    private EmployeeRoleMapper employeeRoleMapper;

    @Autowired
    private EmployeePermissionMapper employeePermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private ILogRecordService logRecordService;

    @Autowired
    private AuthService authService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public VehicleArchiveResponse getVehicleArchive(VehiclePageRequest request) {
        // 车辆档案页面权限编码
        String pageCode = "档案管理:车辆档案:页面";

        // 使用 ViewScopeHelper 解析视图范围
        String viewScope = ViewScopeHelper.resolveViewScope(pageCode, request.getViewScope());

        // 获取当前用户ID
        Integer currentUserId = SecurityUtil.getCurrentUserId();

        // SELF 模式时添加创建人过滤条件，ALL 模式时不限制
        Integer creatorFilter = ViewScopeHelper.isSelfScope(viewScope) ? currentUserId : null;

        // 构建分页对象
        Page<Vehicle> page = new Page<>(request.getCurrent(), request.getSize());

        // 查询车辆列表
        IPage<Vehicle> entityPage = vehicleMapper.selectVehiclePage(
                page,
                request.getPlateNo(),
                request.getVehicleCode(),
                request.getCompanyName(),
                request.getStatus(),
                creatorFilter,
                request.getOrderBy(),
                request.getOrderDirection()
        );

        // 转换为响应对象
        List<VehiclePageResponse> records = entityPage.getRecords().stream()
                .map(this::convertToPageResponse)
                .collect(Collectors.toList());

        // 如果需要包含关联合同信息
        if (Boolean.TRUE.equals(request.getIncludeContracts()) && !records.isEmpty()) {
            enrichContractsInfo(records);
        }

        // 统计各状态车辆数量
        List<TransportStat> stats = calculateStats();

        // 构建响应
        VehicleArchiveResponse response = new VehicleArchiveResponse();
        response.setStats(stats);
        response.setRecords(records);
        response.setTotal(entityPage.getTotal());
        response.setCurrent(entityPage.getCurrent());
        response.setSize(entityPage.getSize());

        return response;
    }

    @Override
    public VehicleDetailResponse getVehicleDetail(Integer vehicleId) {
        Vehicle vehicle = vehicleMapper.selectDetailById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "车辆不存在");
        }
        return convertToDetailResponse(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleDetailResponse createVehicle(VehicleCreateRequest request) {
        Integer currentUserId = getCurrentUserId();

        // 检查车牌号是否已存在
        Vehicle existed = vehicleMapper.selectByPlateNo(request.getPlateNo());
        if (existed != null) {
            throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "车牌号已存在");
        }

        // 构建实体对象
        Vehicle vehicle = buildVehicleEntity(request, currentUserId);
        vehicleMapper.insert(vehicle);
        log.info("创建车辆成功：vehicleId={}, plateNo={}, operator={}", vehicle.getVehicleId(), vehicle.getPlateNo(), currentUserId);

        // 记录数据变更日志
        try {
            VehicleDetailResponse detail = convertToDetailResponse(vehicleMapper.selectDetailById(vehicle.getVehicleId()));
            logRecordService.recordDataChangeLog("车辆管理", "VEHICLE",
                    String.valueOf(vehicle.getVehicleId()), "新增",
                    "新增车辆：" + vehicle.getPlateNo(),
                    null, detail, currentUserId, null, true, null);
        } catch (Exception e) {
            log.warn("记录车辆新增数据变更日志失败", e);
        }

        Vehicle detail = vehicleMapper.selectDetailById(vehicle.getVehicleId());
        return convertToDetailResponse(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVehicle(VehicleUpdateRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        Vehicle vehicle = vehicleMapper.selectDetailById(request.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "车辆不存在");
        }

        // 应用操作范围控制（operateScope）
        if (!admin) {
            // 获取当前员工对"车辆档案"页面的权限配置
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:车辆档案:页面");
            
            if (permission != null) {
                if ("SELF".equalsIgnoreCase(permission.getOperateScope())) {
                    // 仅能操作自己创建的车辆
                    if (!Objects.equals(vehicle.getCreatorId(), currentUserId)) {
                        throw new BusinessException(ResultCodeEnum.PERMISSION_DENIED.getCode(), "您只能编辑自己创建的车辆");
                    }
                }
            }
            // 如果是 ALL 或没有配置，不添加限制
        }

        // 保存旧数据用于日志记录（必须在修改vehicle对象之前获取）
        VehicleDetailResponse oldDetail = null;
        try {
            oldDetail = convertToDetailResponse(vehicle);
        } catch (Exception e) {
            log.warn("获取车辆旧数据失败，将跳过数据变更日志记录", e);
        }

        // 如果车牌号变更，检查新车牌号是否已存在
        if (!StrUtil.equals(request.getPlateNo(), vehicle.getPlateNo())) {
            Vehicle existed = vehicleMapper.selectByPlateNo(request.getPlateNo());
            if (existed != null && !existed.getVehicleId().equals(request.getVehicleId())) {
                throw new BusinessException(ResultCodeEnum.DATA_ALREADY_EXISTS.getCode(), "车牌号已存在");
            }
        }

        // 更新字段
        vehicle.setCompanyName(request.getCompanyName());
        vehicle.setCompanyAddress(request.getCompanyAddress());
        vehicle.setPlateNo(request.getPlateNo());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setLoadCapacity(request.getLoadCapacity());
        vehicle.setSeatCount(request.getSeatCount());
        if (StrUtil.isNotBlank(request.getStatus())) {
            vehicle.setStatus(request.getStatus());
        }
        vehicle.setOperationScope(request.getOperationScope());
        vehicle.setOperationLicenseNo(request.getOperationLicenseNo());
        vehicle.setIssuingAuthority(request.getIssuingAuthority());
        vehicle.setIssuingDate(request.getIssuingDate());
        vehicle.setLicenseValidUntil(request.getLicenseValidUntil());
        vehicle.setInspectionValidUntil(request.getInspectionValidUntil());
        vehicle.setTechLevelDate(request.getTechLevelDate());
        vehicle.setVehicleLengthMm(request.getVehicleLengthMm());
        vehicle.setVehicleWidthMm(request.getVehicleWidthMm());
        vehicle.setVehicleHeightMm(request.getVehicleHeightMm());
        vehicle.setRemarks(request.getRemarks());
        vehicle.setUpdateTime(LocalDateTime.now());

        int rows = vehicleMapper.updateById(vehicle);
        if (rows == 0) {
            throw new BusinessException("更新车辆失败：记录已被其他用户修改");
        }
        log.info("更新车辆成功：vehicleId={}, plateNo={}, operator={}", vehicle.getVehicleId(), vehicle.getPlateNo(), currentUserId);

        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                VehicleDetailResponse newDetail = convertToDetailResponse(vehicleMapper.selectDetailById(vehicle.getVehicleId()));
                logRecordService.recordDataChangeLog("车辆管理", "VEHICLE",
                        String.valueOf(vehicle.getVehicleId()), "更新",
                        "更新车辆：" + vehicle.getPlateNo(),
                        oldDetail, newDetail, currentUserId, null, true, null);
            } catch (Exception e) {
                log.error("记录车辆更新数据变更日志失败", e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVehicle(Integer vehicleId) {
        Integer currentUserId = getCurrentUserId();

        Vehicle vehicle = vehicleMapper.selectDetailById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(ResultCodeEnum.DATA_NOT_FOUND.getCode(), "车辆不存在");
        }

        // 保存旧数据用于日志记录
        VehicleDetailResponse oldDetail = null;
        try {
            oldDetail = convertToDetailResponse(vehicle);
        } catch (Exception e) {
            log.warn("获取车辆旧数据失败，将跳过数据变更日志记录", e);
        }

        int rows = vehicleMapper.deleteById(vehicleId);
        if (rows == 0) {
            throw new BusinessException("删除车辆失败：记录不存在或已被删除");
        }
        log.info("删除车辆成功：vehicleId={}, plateNo={}, operator={}", vehicleId, vehicle.getPlateNo(), currentUserId);

        // 记录数据变更日志
        if (oldDetail != null) {
            try {
                logRecordService.recordDataChangeLog("车辆管理", "VEHICLE",
                        String.valueOf(vehicleId), "删除",
                        "删除车辆：" + vehicle.getPlateNo(),
                        oldDetail, null, currentUserId, null, true, null);
            } catch (Exception e) {
                log.error("记录车辆删除数据变更日志失败", e);
            }
        }
    }

    /**
     * 构建车辆实体对象
     */
    private Vehicle buildVehicleEntity(VehicleCreateRequest request, Integer creatorId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setCompanyName(request.getCompanyName());
        vehicle.setCompanyAddress(request.getCompanyAddress());
        vehicle.setPlateNo(request.getPlateNo());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setLoadCapacity(request.getLoadCapacity());
        vehicle.setSeatCount(request.getSeatCount());
        vehicle.setStatus(StrUtil.isNotBlank(request.getStatus()) ? request.getStatus() : "空闲");
        vehicle.setOperationScope(request.getOperationScope());
        vehicle.setOperationLicenseNo(request.getOperationLicenseNo());
        vehicle.setIssuingAuthority(request.getIssuingAuthority());
        vehicle.setIssuingDate(request.getIssuingDate());
        vehicle.setLicenseValidUntil(request.getLicenseValidUntil());
        vehicle.setInspectionValidUntil(request.getInspectionValidUntil());
        vehicle.setTechLevelDate(request.getTechLevelDate());
        vehicle.setVehicleLengthMm(request.getVehicleLengthMm());
        vehicle.setVehicleWidthMm(request.getVehicleWidthMm());
        vehicle.setVehicleHeightMm(request.getVehicleHeightMm());
        vehicle.setCreatorId(creatorId);
        vehicle.setRemarks(request.getRemarks());
        vehicle.setCreateTime(LocalDateTime.now());
        vehicle.setUpdateTime(LocalDateTime.now());
        return vehicle;
    }

    /**
     * 转换为分页响应对象
     */
    private VehiclePageResponse convertToPageResponse(Vehicle vehicle) {
        VehiclePageResponse response = new VehiclePageResponse();
        response.setVehicleId(vehicle.getVehicleId());
        response.setVehicleCode(vehicle.getVehicleCode());
        response.setCompanyName(vehicle.getCompanyName());
        response.setCompanyAddress(vehicle.getCompanyAddress());
        response.setPlateNo(vehicle.getPlateNo());
        response.setVehicleType(vehicle.getVehicleType());
        response.setLoadCapacity(vehicle.getLoadCapacity());
        response.setSeatCount(vehicle.getSeatCount());
        response.setStatus(vehicle.getStatus());
        response.setOperationScope(vehicle.getOperationScope());
        response.setOperationLicenseNo(vehicle.getOperationLicenseNo());
        response.setIssuingAuthority(vehicle.getIssuingAuthority());
        response.setIssuingDate(vehicle.getIssuingDate());
        response.setLicenseValidUntil(vehicle.getLicenseValidUntil());
        response.setInspectionValidUntil(vehicle.getInspectionValidUntil());
        response.setTechLevelDate(vehicle.getTechLevelDate());
        response.setVehicleLengthMm(vehicle.getVehicleLengthMm());
        response.setVehicleWidthMm(vehicle.getVehicleWidthMm());
        response.setVehicleHeightMm(vehicle.getVehicleHeightMm());
        response.setCreatorId(vehicle.getCreatorId());
        response.setCreatorName(vehicle.getCreatorName());
        response.setRemarks(vehicle.getRemarks());
        if (vehicle.getCreateTime() != null) {
            response.setCreatedAt(vehicle.getCreateTime().format(DATE_TIME_FORMATTER));
        }
        if (vehicle.getUpdateTime() != null) {
            response.setUpdatedAt(vehicle.getUpdateTime().format(DATE_TIME_FORMATTER));
        }
        return response;
    }

    /**
     * 批量填充车辆关联的合同信息
     */
    private void enrichContractsInfo(List<VehiclePageResponse> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        List<Integer> vehicleIds = records.stream()
                .map(VehiclePageResponse::getVehicleId)
                .collect(Collectors.toList());
        
        // 批量查询车辆关联的合同
        List<com.erp.controller.transport.dto.ContractVehicleResponse> contractList = 
                transportContractVehicleMapper.selectContractsByVehicleIds(vehicleIds);
        
        // 按车辆ID分组
        Map<Integer, List<com.erp.controller.transport.dto.ContractVehicleResponse>> contractMap = 
                contractList.stream()
                        .collect(Collectors.groupingBy(com.erp.controller.transport.dto.ContractVehicleResponse::getVehicleId));
        
        // 填充到每辆车的响应中
        for (VehiclePageResponse record : records) {
            List<com.erp.controller.transport.dto.ContractVehicleResponse> vehicleContracts = contractMap.get(record.getVehicleId());
            if (vehicleContracts != null && !vehicleContracts.isEmpty()) {
                List<VehiclePageResponse.ContractSimpleInfo> simpleInfos = vehicleContracts.stream()
                        .map(cp -> {
                            VehiclePageResponse.ContractSimpleInfo info = new VehiclePageResponse.ContractSimpleInfo();
                            info.setContractId(cp.getContractId());
                            info.setContractNo(cp.getContractNo());
                            info.setCarrierName(cp.getCarrierName());
                            return info;
                        })
                        .collect(Collectors.toList());
                record.setContracts(simpleInfos);
            } else {
                record.setContracts(new java.util.ArrayList<>());
            }
        }
    }

    /**
     * 转换为详情响应对象
     */
    private VehicleDetailResponse convertToDetailResponse(Vehicle vehicle) {
        VehicleDetailResponse response = new VehicleDetailResponse();
        response.setVehicleId(vehicle.getVehicleId());
        response.setVehicleCode(vehicle.getVehicleCode());
        response.setCompanyName(vehicle.getCompanyName());
        response.setCompanyAddress(vehicle.getCompanyAddress());
        response.setPlateNo(vehicle.getPlateNo());
        response.setVehicleType(vehicle.getVehicleType());
        response.setLoadCapacity(vehicle.getLoadCapacity());
        response.setSeatCount(vehicle.getSeatCount());
        response.setStatus(vehicle.getStatus());
        response.setOperationScope(vehicle.getOperationScope());
        response.setOperationLicenseNo(vehicle.getOperationLicenseNo());
        response.setIssuingAuthority(vehicle.getIssuingAuthority());
        response.setIssuingDate(vehicle.getIssuingDate());
        response.setLicenseValidUntil(vehicle.getLicenseValidUntil());
        response.setInspectionValidUntil(vehicle.getInspectionValidUntil());
        response.setTechLevelDate(vehicle.getTechLevelDate());
        response.setVehicleLengthMm(vehicle.getVehicleLengthMm());
        response.setVehicleWidthMm(vehicle.getVehicleWidthMm());
        response.setVehicleHeightMm(vehicle.getVehicleHeightMm());
        response.setCreatorId(vehicle.getCreatorId());
        response.setCreatorName(vehicle.getCreatorName());
        response.setRemarks(vehicle.getRemarks());
        if (vehicle.getCreateTime() != null) {
            response.setCreatedAt(vehicle.getCreateTime().format(DATE_TIME_FORMATTER));
        }
        if (vehicle.getUpdateTime() != null) {
            response.setUpdatedAt(vehicle.getUpdateTime().format(DATE_TIME_FORMATTER));
        }
        return response;
    }

    /**
     * 计算统计数据
     */
    private List<TransportStat> calculateStats() {
        List<Vehicle> allVehicles = vehicleMapper.selectList(null);
        
        Map<String, Long> statusCountMap = new HashMap<>();
        statusCountMap.put("空闲", 0L);
        statusCountMap.put("在途", 0L);
        statusCountMap.put("维修", 0L);
        
        for (Vehicle vehicle : allVehicles) {
            String status = vehicle.getStatus();
            if (status != null && statusCountMap.containsKey(status)) {
                statusCountMap.put(status, statusCountMap.get(status) + 1);
            }
        }

        List<TransportStat> stats = new ArrayList<>();
        
        // 空闲车辆
        TransportStat idleStat = new TransportStat();
        idleStat.setLabel("空闲");
        idleStat.setValue(String.valueOf(statusCountMap.get("空闲")));
        idleStat.setColor("success");
        stats.add(idleStat);

        // 在途车辆
        TransportStat inTransitStat = new TransportStat();
        inTransitStat.setLabel("在途");
        inTransitStat.setValue(String.valueOf(statusCountMap.get("在途")));
        inTransitStat.setColor("warning");
        stats.add(inTransitStat);

        // 维修车辆
        TransportStat maintenanceStat = new TransportStat();
        maintenanceStat.setLabel("维修");
        maintenanceStat.setValue(String.valueOf(statusCountMap.get("维修")));
        maintenanceStat.setColor("danger");
        stats.add(maintenanceStat);

        // 总计
        long total = statusCountMap.values().stream().mapToLong(Long::longValue).sum();
        TransportStat totalStat = new TransportStat();
        totalStat.setLabel("总计");
        totalStat.setValue(String.valueOf(total));
        totalStat.setColor("primary");
        stats.add(totalStat);

        return stats;
    }

    @Override
    public List<VehicleDetailResponse> listVehiclesForExport(VehiclePageRequest request) {
        Integer currentUserId = getCurrentUserId();
        boolean admin = authService.isAdmin(currentUserId);

        // 导出数据范围受 viewScope 控制，不受 operateScope 限制
        // 后端在 Service 层做安全校验并强制覆盖，防止前端传入越权参数
        Integer creatorFilter;
        if (admin) {
            // 超级管理员导出全部
            creatorFilter = null;
        } else {
            EmployeePermission permission = getEmployeePagePermission(currentUserId, "档案管理:车辆档案:页面");
            if (permission == null) {
                // 无权限配置时，遵循最小权限原则，只导出自己创建的数据
                creatorFilter = currentUserId;
            } else if ("SELF".equalsIgnoreCase(permission.getViewScope())) {
                // viewScope=SELF：强制只导出自己创建的数据，防止越权
                creatorFilter = currentUserId;
            } else {
                // viewScope=ALL：导出全部，忽略前端传入的 creatorFilter
                creatorFilter = null;
            }
        }

        List<Vehicle> list = vehicleMapper.selectVehicleList(
                request.getPlateNo(),
                request.getVehicleCode(),
                request.getCompanyName(),
                request.getStatus(),
                creatorFilter,
                request.getOrderBy(),
                request.getOrderDirection()
        );
        return list.stream().map(this::convertToDetailResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleImportResponse importVehicles(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请上传Excel文件");
        }
        Integer currentUserId = getCurrentUserId();

        VehicleImportResponse response = new VehicleImportResponse();
        List<Vehicle> preparedList = new ArrayList<>();
        List<VehicleImportError> errors = response.getErrors();
        Map<String, Integer> plateNoRowMap = new HashMap<>();
        Set<String> seenPlateNos = new HashSet<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "Excel中没有可用的Sheet");
            }
            int totalRow = sheet.getLastRowNum();
            int processedRows = 0;
            for (int i = 1; i <= totalRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                processedRows++;
                int rowNumber = i + 1;
                try {
                    Vehicle vehicle = buildVehicleFromRow(row, currentUserId);
                    if (vehicle == null) {
                        continue;
                    }
                    String plateNo = vehicle.getPlateNo();
                    if (seenPlateNos.contains(plateNo)) {
                        addImportError(errors, rowNumber, "Excel中车牌号重复");
                        continue;
                    }
                    seenPlateNos.add(plateNo);
                    plateNoRowMap.put(plateNo, rowNumber);
                    preparedList.add(vehicle);
                } catch (BusinessException ex) {
                    addImportError(errors, rowNumber, ex.getMessage());
                }
            }
            response.setTotalCount(processedRows);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入车辆失败", e);
            throw new BusinessException(ResultCodeEnum.OPERATION_FAILED.getCode(), "导入失败：" + e.getMessage());
        }

        if (!preparedList.isEmpty()) {
            List<String> plateNos = preparedList.stream()
                    .map(Vehicle::getPlateNo)
                    .distinct()
                    .collect(Collectors.toList());
            List<String> existedPlateNos = vehicleMapper.selectExistingPlateNos(plateNos);
            if (!CollectionUtils.isEmpty(existedPlateNos)) {
                for (String plateNo : existedPlateNos) {
                    Integer rowNumber = plateNoRowMap.getOrDefault(plateNo, -1);
                    if (rowNumber != -1) {
                        addImportError(response.getErrors(), rowNumber, "车牌号已存在于系统");
                    }
                }
                Iterator<Vehicle> iterator = preparedList.iterator();
                while (iterator.hasNext()) {
                    Vehicle next = iterator.next();
                    if (existedPlateNos.contains(next.getPlateNo())) {
                        iterator.remove();
                    }
                }
            }
        }

        if (!preparedList.isEmpty()) {
            preparedList.forEach(item -> {
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
            });
            vehicleMapper.insertBatch(preparedList);
            log.info("批量导入车辆成功：count={}, operator={}", preparedList.size(), currentUserId);
        }

        response.setSuccessCount(preparedList.size());
        response.setFailCount(response.getErrors().size());
        return response;
    }

    private Vehicle buildVehicleFromRow(Row row, Integer currentUserId) {
        String companyName = readCellString(row.getCell(0));
        String companyAddress = readCellString(row.getCell(1));
        String plateNo = readCellString(row.getCell(2));
        String vehicleType = readCellString(row.getCell(3));
        String loadCapacityStr = readCellString(row.getCell(4));
        String seatCountStr = readCellString(row.getCell(5));
        String status = readCellString(row.getCell(6));
        String operationScope = readCellString(row.getCell(7));
        String operationLicenseNo = readCellString(row.getCell(8));
        String issuingAuthority = readCellString(row.getCell(9));
        String issuingDateStr = readCellString(row.getCell(10));
        String licenseValidUntilStr = readCellString(row.getCell(11));
        String inspectionValidUntilStr = readCellString(row.getCell(12));
        String techLevelDateStr = readCellString(row.getCell(13));
        String vehicleLengthMmStr = readCellString(row.getCell(14));
        String vehicleWidthMmStr = readCellString(row.getCell(15));
        String vehicleHeightMmStr = readCellString(row.getCell(16));
        String remarks = readCellString(row.getCell(17));

        if (StrUtil.isBlank(companyName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "公司名称不能为空");
        }
        if (StrUtil.isBlank(companyAddress)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "公司地址不能为空");
        }
        if (StrUtil.isBlank(plateNo)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "车牌号不能为空");
        }
        if (StrUtil.isBlank(vehicleType)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "车辆类型不能为空");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setCompanyName(companyName.trim());
        vehicle.setCompanyAddress(companyAddress.trim());
        vehicle.setPlateNo(plateNo.trim());
        vehicle.setVehicleType(vehicleType.trim());
        vehicle.setLoadCapacity(parseBigDecimal(loadCapacityStr));
        vehicle.setSeatCount(parseBigDecimal(seatCountStr));
        vehicle.setStatus(StrUtil.isNotBlank(status) ? status.trim() : "空闲");
        vehicle.setOperationScope(normalize(operationScope));
        vehicle.setOperationLicenseNo(normalize(operationLicenseNo));
        vehicle.setIssuingAuthority(normalize(issuingAuthority));
        vehicle.setIssuingDate(parseDate(issuingDateStr));
        vehicle.setLicenseValidUntil(parseDate(licenseValidUntilStr));
        vehicle.setInspectionValidUntil(parseDate(inspectionValidUntilStr));
        vehicle.setTechLevelDate(parseDate(techLevelDateStr));
        vehicle.setVehicleLengthMm(parseInteger(vehicleLengthMmStr));
        vehicle.setVehicleWidthMm(parseInteger(vehicleWidthMmStr));
        vehicle.setVehicleHeightMm(parseInteger(vehicleHeightMmStr));
        vehicle.setCreatorId(currentUserId);
        vehicle.setRemarks(normalize(remarks));
        vehicle.setCreateTime(LocalDateTime.now());
        vehicle.setUpdateTime(LocalDateTime.now());
        return vehicle;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void addImportError(List<VehicleImportError> errors, int rowNumber, String message) {
        VehicleImportError error = new VehicleImportError();
        error.setRowIndex(rowNumber);
        error.setMessage(message);
        errors.add(error);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < 18; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && StrUtil.isNotBlank(readCellString(cell))) {
                return false;
            }
        }
        return true;
    }

    private String readCellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 获取当前用户ID
     */
    private Integer getCurrentUserId() {
        return SecurityUtil.getCurrentUserId();
    }


    /**
     * 获取员工的页面权限配置
     * 
     * @param employeeId 员工ID
     * @param pageCode 页面权限编码
     * @return 员工页面权限配置
     */
    private EmployeePermission getEmployeePagePermission(Integer employeeId, String pageCode) {
        try {
            // 从数据库查询页面权限ID
            Permission permission = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getPermissionCode, pageCode)
                    .eq(Permission::getPermissionTypeId, 2) // 2 = 页面级权限
            );
            
            if (permission == null) {
                return null;
            }

            // 查询员工页面权限配置
            EmployeePermission employeePermission = employeePermissionMapper.selectOne(
                new LambdaQueryWrapper<EmployeePermission>()
                    .eq(EmployeePermission::getEmployeeId, employeeId)
                    .eq(EmployeePermission::getPagePermissionId, permission.getPermissionId())
            );
            
            return employeePermission;
        } catch (Exception e) {
            log.error("获取员工页面权限配置失败：employeeId={}, pageCode={}", employeeId, pageCode, e);
            return null;
        }
    }
}

