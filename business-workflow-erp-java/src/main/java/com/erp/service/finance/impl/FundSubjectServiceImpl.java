package com.erp.service.finance.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.enums.ResultCodeEnum;
import com.erp.common.exception.BusinessException;
import com.erp.common.result.ImportResult;
import com.erp.common.util.SecurityUtil;
import com.erp.controller.finance.dto.FundSubjectListItemResponse;
import com.erp.controller.finance.dto.FundSubjectOption;
import com.erp.controller.finance.dto.FundSubjectPageRequest;
import com.erp.entity.finance.FundSubject;
import com.erp.mapper.finance.FundSubjectMapper;
import com.erp.service.finance.FundSubjectService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 会计科目服务实现
 */
@Slf4j
@Service
public class FundSubjectServiceImpl extends ServiceImpl<FundSubjectMapper, FundSubject> implements FundSubjectService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundSubject createSubject(String subjectCode, String subjectName, String subjectCategory,
                                   String balanceDirection, Boolean isCashSubject, Boolean auxiliaryAccounting,
                                   Boolean quantityAccounting, Boolean foreignCurrencyAccounting, String remark) {
        return createSubjectWithParent(null, subjectCode, subjectName, subjectCategory, balanceDirection,
                                     isCashSubject, auxiliaryAccounting, quantityAccounting, foreignCurrencyAccounting, remark);
    }

    /**
     * 创建科目（带上级科目）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundSubject createSubjectWithParent(String parentSubjectCode, String subjectCode, String subjectName,
                                             String subjectCategory, String balanceDirection, Boolean isCashSubject,
                                             Boolean auxiliaryAccounting, Boolean quantityAccounting,
                                             Boolean foreignCurrencyAccounting, String remark) {
        log.info("创建会计科目：parentSubjectCode={}, subjectCode={}, subjectName={}", parentSubjectCode, subjectCode, subjectName);

        // 基本校验
        if (StrUtil.isBlank(subjectCode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目编码不能为空");
        }
        if (StrUtil.isBlank(subjectName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目名称不能为空");
        }
        if (StrUtil.isBlank(subjectCategory)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目类别不能为空");
        }
        if (StrUtil.isBlank(balanceDirection)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "余额方向不能为空");
        }

        // 校验上级科目是否存在
        FundSubject parentSubject = null;
        if (parentSubjectCode != null) {
            parentSubject = lambdaQuery()
                    .eq(FundSubject::getSubjectCode, parentSubjectCode)
                    .one();
            if (parentSubject == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "上级科目不存在：" + parentSubjectCode);
            }
        }

        // 校验科目编码唯一性
        FundSubject existingSubject = baseMapper.selectBySubjectCode(subjectCode);
        if (existingSubject != null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目编码已存在：" + subjectCode);
        }

        // 校验科目类别和余额方向的枚举值
        if (!"流动资产".equals(subjectCategory) && !"非流动资产".equals(subjectCategory)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目类别只能是：流动资产、非流动资产");
        }
        if (!"收入".equals(balanceDirection) && !"支出".equals(balanceDirection)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "余额方向只能是：收入、支出");
        }

        // 计算科目级次
        Integer subjectLevel = calculateSubjectLevel(parentSubjectCode);

        // 生成全科目名称
        String fullSubjectName = generateFullSubjectName(parentSubjectCode, subjectName);

        // 创建科目实体
        FundSubject subject = new FundSubject();
        subject.setSubjectCode(subjectCode);
        subject.setSubjectName(subjectName);
        subject.setParentSubjectCode(parentSubjectCode);
        subject.setSubjectLevel(subjectLevel);
        subject.setFullSubjectName(fullSubjectName);
        subject.setSubjectCategory(subjectCategory);
        subject.setBalanceDirection(balanceDirection);
        subject.setIsCashSubject(isCashSubject != null ? isCashSubject : false);
        subject.setAuxiliaryAccounting(auxiliaryAccounting != null ? auxiliaryAccounting : false);
        subject.setQuantityAccounting(quantityAccounting != null ? quantityAccounting : false);
        subject.setForeignCurrencyAccounting(foreignCurrencyAccounting != null ? foreignCurrencyAccounting : false);
        subject.setEnabled(true); // 默认启用
        subject.setRemark(remark);
        subject.setCreateTime(LocalDateTime.now()); // 设置创建时间
        subject.setUpdateTime(LocalDateTime.now()); // 设置更新时间
        subject.setCreateUserId(SecurityUtil.getCurrentUserId());
        subject.setUpdateUserId(SecurityUtil.getCurrentUserId());

        // 保存到数据库
        boolean success = save(subject);
        if (!success) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "创建会计科目失败");
        }

        log.info("会计科目创建成功：id={}, subjectCode={}", subject.getSubjectId(), subjectCode);
        return subject;
    }

    @Override
    public IPage<FundSubjectListItemResponse> getSubjectPage(Page<?> page, FundSubjectPageRequest request) {
        log.debug("分页查询会计科目：current={}, size={}, subjectCode={}, subjectName={}",
                 request.getCurrent(), request.getSize(), request.getSubjectCode(), request.getSubjectName());

        return baseMapper.selectFundSubjectPage(page, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundSubject updateSubject(Long subjectId, String subjectCode, String subjectName, String subjectCategory,
                                   String balanceDirection, Boolean isCashSubject, Boolean auxiliaryAccounting,
                                   Boolean quantityAccounting, Boolean foreignCurrencyAccounting, Boolean enabled,
                                   String remark, Integer version) {
        log.info("更新会计科目：id={}, subjectCode={}", subjectId, subjectCode);

        // 基本校验
        if (subjectId == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目ID不能为空");
        }
        if (StrUtil.isBlank(subjectCode)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目编码不能为空");
        }
        if (StrUtil.isBlank(subjectName)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目名称不能为空");
        }
        if (StrUtil.isBlank(subjectCategory)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目类别不能为空");
        }
        if (StrUtil.isBlank(balanceDirection)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "余额方向不能为空");
        }
        if (version == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "版本号不能为空");
        }

        // 校验科目是否存在
        FundSubject existingSubject = getById(subjectId);
        if (existingSubject == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目不存在：" + subjectId);
        }

        // 校验乐观锁版本
        if (!version.equals(existingSubject.getVersion())) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "科目已被其他用户修改，请刷新后重试");
        }

        // 如果科目编码改变，校验唯一性
        if (!subjectCode.equals(existingSubject.getSubjectCode())) {
            FundSubject subjectWithSameCode = baseMapper.selectBySubjectCode(subjectCode);
            if (subjectWithSameCode != null && !subjectWithSameCode.getSubjectId().equals(subjectId)) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目编码已存在：" + subjectCode);
            }
        }

        // 校验科目类别和余额方向的枚举值
        if (!"流动资产".equals(subjectCategory) && !"非流动资产".equals(subjectCategory)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目类别只能是：流动资产、非流动资产");
        }
        if (!"收入".equals(balanceDirection) && !"支出".equals(balanceDirection)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "余额方向只能是：收入、支出");
        }

        // 更新科目实体
        existingSubject.setSubjectCode(subjectCode);
        existingSubject.setSubjectName(subjectName);
        existingSubject.setSubjectCategory(subjectCategory);
        existingSubject.setBalanceDirection(balanceDirection);
        existingSubject.setIsCashSubject(isCashSubject != null ? isCashSubject : false);
        existingSubject.setAuxiliaryAccounting(auxiliaryAccounting != null ? auxiliaryAccounting : false);
        existingSubject.setQuantityAccounting(quantityAccounting != null ? quantityAccounting : false);
        existingSubject.setForeignCurrencyAccounting(foreignCurrencyAccounting != null ? foreignCurrencyAccounting : false);
        existingSubject.setEnabled(enabled != null ? enabled : true);
        existingSubject.setRemark(remark);
        existingSubject.setUpdateTime(LocalDateTime.now()); // 设置更新时间
        existingSubject.setUpdateUserId(SecurityUtil.getCurrentUserId());

        // 更新数据库（乐观锁）
        LambdaQueryWrapper<FundSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundSubject::getSubjectId, subjectId)
               .eq(FundSubject::getVersion, version);

        boolean success = update(existingSubject, wrapper);
        if (!success) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "更新会计科目失败，可能已被其他用户修改");
        }

        log.info("会计科目更新成功：id={}, subjectCode={}", subjectId, subjectCode);
        return existingSubject;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubject(Long subjectId) {
        log.info("删除会计科目：id={}", subjectId);

        // 校验科目是否存在
        FundSubject subject = getById(subjectId);
        if (subject == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目不存在：" + subjectId);
        }

        // 删除科目
        boolean success = removeById(subjectId);
        if (!success) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "删除会计科目失败");
        }

        log.info("会计科目删除成功：id={}, subjectCode={}", subjectId, subject.getSubjectCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteSubjects(List<Long> subjectIds) {
        log.info("批量删除会计科目：subjectIds={}", subjectIds);

        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目ID列表不能为空");
        }

        int successCount = 0;
        List<String> failedSubjects = new ArrayList<>();

        for (Long subjectId : subjectIds) {
            try {
                // 检查科目是否存在
                FundSubject subject = getById(subjectId);
                if (subject == null) {
                    failedSubjects.add("ID=" + subjectId + "（科目不存在）");
                    continue;
                }

                // 检查是否有子级科目
                boolean hasChildren = lambdaQuery()
                        .eq(FundSubject::getParentSubjectCode, subject.getSubjectCode())
                        .exists();
                if (hasChildren) {
                    failedSubjects.add(subject.getSubjectCode() + " " + subject.getSubjectName() + "（存在子级科目）");
                    continue;
                }

                // 执行删除
                boolean success = removeById(subjectId);
                if (success) {
                    successCount++;
                    log.info("会计科目删除成功：id={}, subjectCode={}", subjectId, subject.getSubjectCode());
                } else {
                    failedSubjects.add(subject.getSubjectCode() + " " + subject.getSubjectName() + "（删除失败）");
                }
            } catch (Exception e) {
                log.error("删除科目失败：id={}", subjectId, e);
                failedSubjects.add("ID=" + subjectId + "（" + e.getMessage() + "）");
            }
        }

        // 如果全部失败，抛出异常
        if (successCount == 0 && !failedSubjects.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "批量删除失败：" + String.join("；", failedSubjects));
        }

        log.info("批量删除会计科目完成：成功={}，失败={}", successCount, failedSubjects.size());
        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateSubjectStatus(List<Long> subjectIds, Boolean enabled) {
        log.info("批量更新科目状态：subjectIds={}, enabled={}", subjectIds, enabled);

        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目ID列表不能为空");
        }
        if (enabled == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "状态值不能为空");
        }

        // 更新数据库
        int updatedCount = baseMapper.batchUpdateEnabled(subjectIds, enabled, SecurityUtil.getCurrentUserId());
        if (updatedCount != subjectIds.size()) {
            throw new BusinessException(ResultCodeEnum.ERROR.getCode(), "批量更新科目状态失败，部分科目可能不存在");
        }

        log.info("批量更新科目状态成功：updatedCount={}", updatedCount);
    }

    @Override
    public List<FundSubjectOption> getSubjectOptions() {
        log.debug("查询科目选项列表");

        List<FundSubjectOption> options = baseMapper.selectFundSubjectOptions();

        // 为每个选项设置显示标签
        for (FundSubjectOption option : options) {
            option.setLabel(option.getSubjectCode() + " - " + option.getSubjectName());
        }

        log.debug("查询科目选项列表成功：count={}", options.size());
        return options;
    }

    @Override
    public List<FundSubjectOption> searchSubjectOptions(String keyword, Integer limit) {
        log.debug("搜索科目选项：keyword={}, limit={}", keyword, limit);

        // 如果关键词为空，返回所有已启用的科目选项（限制数量）
        if (StrUtil.isBlank(keyword)) {
            List<FundSubjectOption> options = baseMapper.selectFundSubjectOptions();
            // 为每个选项设置显示标签
            for (FundSubjectOption option : options) {
                option.setLabel(option.getSubjectCode() + " - " + option.getSubjectName());
            }
            // 限制返回数量
            if (limit != null && limit > 0 && options.size() > limit) {
                options = options.subList(0, limit);
            }
            return options;
        }

        // 使用模糊搜索
        LambdaQueryWrapper<FundSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundSubject::getEnabled, true) // 只搜索启用的科目
               .and(w -> w.like(FundSubject::getSubjectCode, keyword)
                        .or()
                        .like(FundSubject::getSubjectName, keyword))
               .orderByAsc(FundSubject::getSubjectCode) // 按科目编码排序
               .last("LIMIT " + (limit != null && limit > 0 ? limit : 50)); // 限制返回数量，默认50个

        List<FundSubject> subjects = list(wrapper);

        // 转换为FundSubjectOption格式
        List<FundSubjectOption> options = new ArrayList<>();
        for (FundSubject subject : subjects) {
            FundSubjectOption option = new FundSubjectOption();
            option.setSubjectId(subject.getSubjectId());
            option.setSubjectCode(subject.getSubjectCode());
            option.setSubjectName(subject.getSubjectName());
            option.setSubjectCategory(subject.getSubjectCategory());
            option.setLabel(subject.getSubjectCode() + " - " + subject.getSubjectName());
            options.add(option);
        }

        log.debug("搜索科目选项成功：keyword={}, count={}", keyword, options.size());
        return options;
    }

    @Override
    public FundSubject getSubjectByCode(String subjectCode) {
        log.debug("根据科目编码查询科目：subjectCode={}", subjectCode);

        if (StrUtil.isBlank(subjectCode)) {
            return null;
        }

        return baseMapper.selectBySubjectCode(subjectCode);
    }

    @Override
    public List<FundSubject> getChildSubjects(String parentSubjectCode) {
        log.debug("查询子科目列表：parentSubjectCode={}", parentSubjectCode);

        if (parentSubjectCode == null) {
            return new ArrayList<>();
        }

        return baseMapper.selectChildSubjects(parentSubjectCode);
    }

    @Override
    public List<FundSubjectListItemResponse> getSubjectTree() {
        log.debug("获取科目树形结构数据");
        // 原 mapper 返回按科目编码排序的扁平列表，需组织成树形结构
        List<FundSubjectListItemResponse> flatList = baseMapper.selectSubjectTree();
        if (flatList == null || flatList.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建 code -> node 映射
        Map<String, FundSubjectListItemResponse> codeToNode = new HashMap<>();
        for (FundSubjectListItemResponse node : flatList) {
            codeToNode.put(node.getSubjectCode(), node);
        }

        // 组装树形结构
        List<FundSubjectListItemResponse> roots = new ArrayList<>();
        for (FundSubjectListItemResponse node : flatList) {
            String parentCode = node.getParentSubjectCode();
            if (parentCode == null) {
                roots.add(node);
            } else {
                FundSubjectListItemResponse parent = codeToNode.get(parentCode);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                } else {
                    // 如果父节点不存在（数据不一致），当作根节点返回，避免丢失
                    roots.add(node);
                }
            }
        }

        return roots;
    }

    @Override
    public String generateSubjectCode(String parentSubjectCode) {
        log.debug("生成科目编码：parentSubjectCode={}", parentSubjectCode);

        if (parentSubjectCode == null) {
            // 生成一级科目编码（4位）
            // 查询现有的最大一级科目编码
            LambdaQueryWrapper<FundSubject> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNull(FundSubject::getParentSubjectCode)
                   .orderByDesc(FundSubject::getSubjectCode)
                   .last("LIMIT 1");

            FundSubject lastSubject = getOne(wrapper);
            if (lastSubject == null) {
                return "1001"; // 默认起始编码
            }

            try {
                int code = Integer.parseInt(lastSubject.getSubjectCode()) + 1;
                return String.format("%04d", code);
            } catch (NumberFormatException e) {
                return "1001"; // 解析失败时返回默认值
            }
        } else {
            // 生成下级科目编码
            FundSubject parentSubject = lambdaQuery()
                    .eq(FundSubject::getSubjectCode, parentSubjectCode)
                    .one();
            if (parentSubject == null) {
                throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "上级科目不存在");
            }

            // 根据上级科目编码和级次规则生成编码
            String parentCode = parentSubject.getSubjectCode();
            int parentLevel = parentSubject.getSubjectLevel();

            // 查询同级最大编码
            LambdaQueryWrapper<FundSubject> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FundSubject::getParentSubjectCode, parentSubjectCode)
                   .likeRight(FundSubject::getSubjectCode, parentCode)
                   .orderByDesc(FundSubject::getSubjectCode)
                   .last("LIMIT 1");

            FundSubject lastChild = getOne(wrapper);
            if (lastChild == null) {
                // 第一个子科目
                return parentCode + "01";
            }

            // 提取最后两位并加1
            String lastCode = lastChild.getSubjectCode();
            if (lastCode.length() > parentCode.length()) {
                String suffix = lastCode.substring(parentCode.length());
                try {
                    int suffixNum = Integer.parseInt(suffix) + 1;
                    return parentCode + String.format("%02d", suffixNum);
                } catch (NumberFormatException e) {
                    return parentCode + "01";
                }
            }

            return parentCode + "01";
        }
    }

    @Override
    public String generateFullSubjectName(String parentSubjectCode, String subjectName) {
        log.debug("生成全科目名称：parentSubjectCode={}, subjectName={}", parentSubjectCode, subjectName);

        if (parentSubjectCode == null) {
            return subjectName;
        }

        FundSubject parentSubject = lambdaQuery()
                .eq(FundSubject::getSubjectCode, parentSubjectCode)
                .one();
        if (parentSubject == null) {
            return subjectName;
        }

        return parentSubject.getFullSubjectName() + "-" + subjectName;
    }

    @Override
    public Integer calculateSubjectLevel(String parentSubjectCode) {
        log.debug("计算科目级次：parentSubjectCode={}", parentSubjectCode);

        if (parentSubjectCode == null) {
            return 1; // 一级科目
        }

        FundSubject parentSubject = lambdaQuery()
                .eq(FundSubject::getSubjectCode, parentSubjectCode)
                .one();
        if (parentSubject == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "上级科目不存在");
        }

        // 级次不能超过4级
        int nextLevel = parentSubject.getSubjectLevel() + 1;
        if (nextLevel > 4) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "科目级次不能超过4级");
        }

        return nextLevel;
    }

    @Override
    public void downloadSubjectTemplate(javax.servlet.http.HttpServletResponse response) {
        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = "科目导入模板.xlsx";
            response.setHeader("Content-disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));

            // 创建工作簿
            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("科目导入模板");

            // 创建样式
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            // 定义字段列表（中文名称）
            String[] fieldNames = {
                "科目编码",
                "科目名称",
                "上级科目编码",
                "科目级次",
                "科目类别",
                "余额方向",
                "是否为现金科目",
                "辅助核算",
                "数量核算",
                "外币核算",
                "是否启用",
                "备注"
            };

            // 创建表头 - 第一行每列一个字段名，用户可以在下方行填写数据
            org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < fieldNames.length; i++) {
                org.apache.poi.xssf.usermodel.XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(fieldNames[i]);
                cell.setCellStyle(headerStyle);

                // 设置列宽度 - 根据字段名长度调整
                int columnWidth = Math.max(fieldNames[i].length() * 2, 15) * 256;
                sheet.setColumnWidth(i, columnWidth);
            }

            // 创建几行空数据行供用户填写（比如3行）
            for (int rowIndex = 1; rowIndex <= 3; rowIndex++) {
                sheet.createRow(rowIndex);
            }

            // 写入响应流
            workbook.write(response.getOutputStream());
            workbook.close();

            log.info("科目导入模板下载成功");

        } catch (Exception e) {
            log.error("生成科目导入模板失败", e);
            throw new BusinessException("生成导入模板失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importSubjectsFromExcel(MultipartFile file) {
        log.info("开始导入科目数据，文件：{}", file.getOriginalFilename());

        List<String> errorMessages = new ArrayList<>();
        List<FundSubject> validSubjects = new ArrayList<>();
        int totalRecords = 0;
        int successCount = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel文件格式错误，未找到工作表");
            }

            // 从第2行开始读取数据（第1行是表头）
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                totalRecords++;

                try {
                    FundSubject subject = validateAndParseRow(row, rowIndex + 1);
                    if (subject != null) {
                        validSubjects.add(subject);
                    }
                } catch (Exception e) {
                    errorMessages.add("第" + (rowIndex + 1) + "行：" + e.getMessage());
                    log.warn("第{}行数据验证失败：{}", rowIndex + 1, e.getMessage());
                }
            }

            // 批量保存有效数据
            if (!validSubjects.isEmpty()) {
                // 打印调试信息，确认每条记录的上级科目编码是否已设置
                for (FundSubject s : validSubjects) {
                    log.debug("准备保存科目：code={}, name={}, parentSubjectCode={}", s.getSubjectCode(), s.getSubjectName(), s.getParentSubjectCode());
                }

                successCount = validSubjects.size();
                this.saveBatch(validSubjects);
                log.info("成功导入{}条科目数据", successCount);
            }

        } catch (IOException e) {
            log.error("读取Excel文件失败", e);
            throw new BusinessException("读取Excel文件失败：" + e.getMessage());
        }

        ImportResult result = ImportResult.builder()
                .totalRecords(totalRecords)
                .successCount(successCount)
                .failureCount(errorMessages.size())
                .errorMessages(errorMessages)
                .build();

        log.info("科目导入完成，总记录：{}，成功：{}，失败：{}",
                totalRecords, successCount, errorMessages.size());

        return result;
    }

    /**
     * 验证并解析Excel行数据
     */
    private FundSubject validateAndParseRow(Row row, int rowNum) {
        // 定义字段顺序（对应Excel模板）
        String[] expectedFields = {
            "科目编码", "科目名称", "上级科目编码", "科目级次",
            "科目类别", "余额方向", "是否为现金科目", "辅助核算",
            "数量核算", "外币核算", "是否启用", "备注"
        };

        // 检查必填字段
        String subjectCode = getCellValueAsString(row.getCell(0));
        String subjectName = getCellValueAsString(row.getCell(1));

        if (StrUtil.isBlank(subjectCode)) {
            throw new BusinessException("科目编码不能为空");
        }
        if (StrUtil.isBlank(subjectName)) {
            throw new BusinessException("科目名称不能为空");
        }

        // 验证科目编码格式
        validateSubjectCode(subjectCode);

        // 验证科目编码唯一性
        if (getSubjectByCode(subjectCode) != null) {
            throw new BusinessException("科目编码已存在：" + subjectCode);
        }

        // 解析上级科目编码（Excel中填写的是上级科目的编码）
        FundSubject parentSubject = null;
        String parentSubjectCode = getCellValueAsString(row.getCell(2));
        if (StrUtil.isNotBlank(parentSubjectCode)) {
            parentSubjectCode = parentSubjectCode.trim();
            // 再次检查trim后的字符串是否为空
            if (StrUtil.isNotBlank(parentSubjectCode)) {
                log.debug("解析上级科目原始值：'{}' (row={})", parentSubjectCode, rowNum);

            // 先按科目编码精确查找
            parentSubject = getSubjectByCode(parentSubjectCode);
            if (parentSubject == null) {
                // 有些用户可能填写了 "1001 - 库存现金" 或者填写了ID值，尝试进一步解析
                String digitsOnly = parentSubjectCode.replaceAll("[^0-9]", "");
                if (StrUtil.isNotBlank(digitsOnly)) {
                    // 先尝试按编码（去除非数字字符后）查找
                    parentSubject = getSubjectByCode(digitsOnly);
                }
                if (parentSubject == null) {
                    // 再尝试按ID查找（如果全是数字并且长度合理）
                    try {
                        Long possibleId = Long.parseLong(digitsOnly);
                        FundSubject byId = this.getById(possibleId);
                        if (byId != null) {
                            parentSubject = byId;
                        }
                    } catch (NumberFormatException ignore) {
                        // ignore
                    }
                }
            }

                if (parentSubject == null) {
                    // 明确报错：上级科目编码在系统中不存在
                    throw new BusinessException("上级科目不存在，编码：" + parentSubjectCode);
                }
            } else {
                // trim后为空字符串，也当作一级科目处理
                parentSubjectCode = null;
            }
        } else {
            // 上级科目编码为空，设为null（一级科目）
            parentSubjectCode = null;
        }

        // 解析科目级次
        Integer subjectLevel;
        String subjectLevelStr = getCellValueAsString(row.getCell(3)).trim();
        if (StrUtil.isBlank(subjectLevelStr)) {
            // 如果未填写，自动计算级次（若有父级则为父级级次+1）
            subjectLevel = calculateSubjectLevel(parentSubjectCode);
        } else {
            try {
                subjectLevel = Integer.parseInt(subjectLevelStr);
                if (subjectLevel < 1 || subjectLevel > 4) {
                    throw new BusinessException("科目级次必须在1-4之间");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException("科目级次格式错误：" + subjectLevelStr);
            }
        }

        // 验证级次与上级科目的一致性
        if (parentSubject == null) {
            // 没有填写上级科目：必须为一级科目
            if (subjectLevel != 1) {
                throw new BusinessException("一级科目级次必须为1");
            }
        } else {
            // 填写了上级科目：校验级次是否为父级级次+1
            Integer expectedLevel = parentSubject.getSubjectLevel() == null ? calculateSubjectLevel(parentSubject.getParentSubjectCode()) + 1 : parentSubject.getSubjectLevel() + 1;
            if (!subjectLevel.equals(expectedLevel)) {
                throw new BusinessException("科目级次与上级科目不匹配，应为 " + expectedLevel + "（上级：" + parentSubject.getSubjectCode() + "，级次：" + parentSubject.getSubjectLevel() + "）");
            }
        }

        // 生成全科目名称（自动根据父子关系生成）
        String fullSubjectName = generateFullSubjectName(parentSubjectCode, subjectName);

        // 验证科目类别
        String subjectCategory = getCellValueAsString(row.getCell(4));
        if (StrUtil.isBlank(subjectCategory)) {
            throw new BusinessException("科目类别不能为空");
        }
        validateSubjectCategory(subjectCategory);

        // 验证余额方向
        String balanceDirection = getCellValueAsString(row.getCell(5));
        if (StrUtil.isBlank(balanceDirection)) {
            throw new BusinessException("余额方向不能为空");
        }
        validateBalanceDirection(balanceDirection);

        // 解析布尔字段
        Boolean isCashSubject = parseBooleanValue(getCellValueAsString(row.getCell(6)));
        Boolean auxiliaryAccounting = parseBooleanValue(getCellValueAsString(row.getCell(7)));
        Boolean quantityAccounting = parseBooleanValue(getCellValueAsString(row.getCell(8)));
        Boolean foreignCurrencyAccounting = parseBooleanValue(getCellValueAsString(row.getCell(9)));
        Boolean enabled = parseBooleanValue(getCellValueAsString(row.getCell(10)));

        // 默认值为true（如果未填写）
        if (enabled == null) {
            enabled = true;
        }

        // 备注
        String remark = getCellValueAsString(row.getCell(11));

        // 若级次>1但未解析到上级科目，则认为数据不完整，直接报错以避免插入不一致数据
        if (subjectLevel > 1 && parentSubjectCode == null) {
            throw new BusinessException("科目级次为" + subjectLevel + "但未找到对应的上级科目，请确认上级科目编码是否正确");
        }

        // 创建科目实体
        FundSubject subject = new FundSubject();
        subject.setSubjectCode(subjectCode);
        subject.setSubjectName(subjectName);
        subject.setParentSubjectCode(parentSubjectCode);
        subject.setSubjectLevel(subjectLevel);
        subject.setFullSubjectName(fullSubjectName);
        subject.setSubjectCategory(subjectCategory);
        subject.setBalanceDirection(balanceDirection);
        subject.setIsCashSubject(isCashSubject != null ? isCashSubject : false);
        subject.setAuxiliaryAccounting(auxiliaryAccounting != null ? auxiliaryAccounting : false);
        subject.setQuantityAccounting(quantityAccounting != null ? quantityAccounting : false);
        subject.setForeignCurrencyAccounting(foreignCurrencyAccounting != null ? foreignCurrencyAccounting : false);
        subject.setEnabled(enabled);
        subject.setRemark(remark);
        subject.setVersion(0);
        subject.setCreateTime(LocalDateTime.now());
        subject.setUpdateTime(LocalDateTime.now());
        subject.setCreateUserName(SecurityUtil.getEmployeeName());
        subject.setUpdateUserName(SecurityUtil.getEmployeeName());

        return subject;
    }

    /**
     * 验证科目编码格式
     */
    private void validateSubjectCode(String subjectCode) {
        if (subjectCode.length() < 4 || subjectCode.length() > 10) {
            throw new BusinessException("科目编码长度必须在4-10位之间");
        }

        // 检查是否为数字
        if (!Pattern.matches("\\d+", subjectCode)) {
            throw new BusinessException("科目编码只能包含数字");
        }

        // 验证4-2-2-2规则
        if (subjectCode.length() == 4) {
            // 一级科目：4位
        } else if (subjectCode.length() == 6) {
            // 二级科目：上级4位+2位
        } else if (subjectCode.length() == 8) {
            // 三级科目：上级6位+2位
        } else if (subjectCode.length() == 10) {
            // 四级科目：上级8位+2位
        } else {
            throw new BusinessException("科目编码不符合4-2-2-2规则");
        }
    }

    /**
     * 验证科目类别
     */
    private void validateSubjectCategory(String subjectCategory) {
        if (!"流动资产".equals(subjectCategory) && !"非流动资产".equals(subjectCategory)) {
            throw new BusinessException("科目类别只能是'流动资产'或'非流动资产'");
        }
    }

    /**
     * 验证余额方向
     */
    private void validateBalanceDirection(String balanceDirection) {
        if (!"收入".equals(balanceDirection) && !"支出".equals(balanceDirection)) {
            throw new BusinessException("余额方向只能是'收入'或'支出'");
        }
    }

    /**
     * 解析布尔值
     */
    private Boolean parseBooleanValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        String lowerValue = value.toLowerCase().trim();
        if ("true".equals(lowerValue) || "是".equals(value) || "1".equals(value) || "yes".equals(lowerValue)) {
            return true;
        } else if ("false".equals(lowerValue) || "否".equals(value) || "0".equals(value) || "no".equals(lowerValue)) {
            return false;
        } else {
            return null; // 无法识别的布尔值
        }
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // 处理数字类型，可能是整数或小数
                double numericValue = cell.getNumericCellValue();
                if (numericValue == (long) numericValue) {
                    // 如果是整数，直接转换为字符串
                    return String.valueOf((long) numericValue);
                } else {
                    // 如果是小数，转换为字符串
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // 计算公式结果
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }

    @Override
    public void exportSubjectsToExcel(FundSubjectPageRequest request, HttpServletResponse response) {
        log.info("开始导出科目数据，查询条件：{}", request);

        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = "会计科目导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            response.setHeader("Content-disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));

            // 查询所有符合条件的科目数据（不分页）
            Page<FundSubjectListItemResponse> page = new Page<>(1, Integer.MAX_VALUE);
            IPage<FundSubjectListItemResponse> subjectPage = getSubjectPage(page, request);
            List<FundSubjectListItemResponse> subjectList = subjectPage.getRecords();

            log.info("查询到{}条科目数据待导出", subjectList.size());

            // 为导出补齐“备注”字段：FundSubjectListItemResponse 未包含 remark，这里批量查询实体避免循环查库
            Map<Long, String> remarkBySubjectId = new HashMap<>();
            if (subjectList != null && !subjectList.isEmpty()) {
                List<Long> subjectIds = new ArrayList<>();
                for (FundSubjectListItemResponse s : subjectList) {
                    if (s != null && s.getSubjectId() != null) {
                        subjectIds.add(s.getSubjectId());
                    }
                }
                if (!subjectIds.isEmpty()) {
                    List<FundSubject> subjects = this.listByIds(subjectIds);
                    if (subjects != null) {
                        for (FundSubject s : subjects) {
                            if (s != null && s.getSubjectId() != null) {
                                remarkBySubjectId.put(s.getSubjectId(), s.getRemark());
                            }
                        }
                    }
                }
            }

            // 创建工作簿
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("会计科目数据");

            // 创建样式
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            // 设置边框
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 数据单元格样式
            XSSFCellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 定义字段列表（导出时的列名）
            String[] fieldNames = {
                "科目编码",
                "科目名称",
                "上级科目编码",
                "科目级次",
                "科目类别",
                "余额方向",
                "是否为现金科目",
                "辅助核算",
                "数量核算",
                "外币核算",
                "是否启用",
                "备注"
            };

            // 创建表头
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < fieldNames.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(fieldNames[i]);
                cell.setCellStyle(headerStyle);

                // 设置列宽度 - 根据字段名长度调整
                int columnWidth = Math.max(fieldNames[i].length() * 2, 15) * 256;
                sheet.setColumnWidth(i, columnWidth);
            }

            // 填充数据
            for (int rowIndex = 0; rowIndex < subjectList.size(); rowIndex++) {
                FundSubjectListItemResponse subject = subjectList.get(rowIndex);
                XSSFRow dataRow = sheet.createRow(rowIndex + 1);

                // 科目编码
                XSSFCell cell0 = dataRow.createCell(0);
                cell0.setCellValue(subject.getSubjectCode() != null ? subject.getSubjectCode() : "");
                cell0.setCellStyle(dataStyle);

                // 科目名称
                XSSFCell cell1 = dataRow.createCell(1);
                cell1.setCellValue(subject.getSubjectName() != null ? subject.getSubjectName() : "");
                cell1.setCellStyle(dataStyle);

                // 上级科目编码
                XSSFCell cell2 = dataRow.createCell(2);
                cell2.setCellValue(subject.getParentSubjectCode() != null ? subject.getParentSubjectCode() : "");
                cell2.setCellStyle(dataStyle);

                // 科目级次
                XSSFCell cell3 = dataRow.createCell(3);
                cell3.setCellValue(subject.getSubjectLevel() != null ? subject.getSubjectLevel() : 0);
                cell3.setCellStyle(dataStyle);

                // 科目类别
                XSSFCell cell4 = dataRow.createCell(4);
                cell4.setCellValue(subject.getSubjectCategory() != null ? subject.getSubjectCategory() : "");
                cell4.setCellStyle(dataStyle);

                // 余额方向
                XSSFCell cell5 = dataRow.createCell(5);
                cell5.setCellValue(subject.getBalanceDirection() != null ? subject.getBalanceDirection() : "");
                cell5.setCellStyle(dataStyle);

                // 是否为现金科目
                XSSFCell cell6 = dataRow.createCell(6);
                cell6.setCellValue(subject.getIsCashSubject() != null && subject.getIsCashSubject() ? "是" : "否");
                cell6.setCellStyle(dataStyle);

                // 辅助核算
                XSSFCell cell7 = dataRow.createCell(7);
                cell7.setCellValue(subject.getAuxiliaryAccounting() != null && subject.getAuxiliaryAccounting() ? "是" : "否");
                cell7.setCellStyle(dataStyle);

                // 数量核算
                XSSFCell cell8 = dataRow.createCell(8);
                cell8.setCellValue(subject.getQuantityAccounting() != null && subject.getQuantityAccounting() ? "是" : "否");
                cell8.setCellStyle(dataStyle);

                // 外币核算
                XSSFCell cell9 = dataRow.createCell(9);
                cell9.setCellValue(subject.getForeignCurrencyAccounting() != null && subject.getForeignCurrencyAccounting() ? "是" : "否");
                cell9.setCellStyle(dataStyle);

                // 是否启用
                XSSFCell cell10 = dataRow.createCell(10);
                cell10.setCellValue(subject.getEnabled() != null && subject.getEnabled() ? "启用" : "停用");
                cell10.setCellStyle(dataStyle);

                // 备注
                XSSFCell cell11 = dataRow.createCell(11);
                String remark = subject.getSubjectId() != null ? remarkBySubjectId.get(subject.getSubjectId()) : null;
                cell11.setCellValue(remark != null ? remark : "");
                cell11.setCellStyle(dataStyle);
            }

            // 写入响应流
            workbook.write(response.getOutputStream());
            workbook.close();

            log.info("科目数据导出成功，共导出{}条记录", subjectList.size());

        } catch (Exception e) {
            log.error("导出科目数据失败", e);
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }
}