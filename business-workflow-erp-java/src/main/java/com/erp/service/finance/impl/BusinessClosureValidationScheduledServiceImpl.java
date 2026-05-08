package com.erp.service.finance.impl;

import com.erp.controller.finance.dto.ClosureValidationRequest;
import com.erp.controller.finance.dto.ClosureValidationResponse;
import com.erp.service.finance.BusinessClosureValidationService;
import com.erp.service.finance.IBusinessClosureValidationScheduledService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 业务闭环校验定时任务服务实现
 *
 * @author ERP System
 * @date 2025-02-07
 */
@Slf4j
@Service
public class BusinessClosureValidationScheduledServiceImpl implements IBusinessClosureValidationScheduledService {

    @Autowired
    private BusinessClosureValidationService businessClosureValidationService;

    @Override
    @Scheduled(cron = "0 0 9 * * ?")
    public void executeDailyFullValidation() {
        try {
            log.info("开始执行每日定时全量业务闭环校验任务");

            ClosureValidationRequest request = new ClosureValidationRequest();
            request.setValidateType("FULL");

            ClosureValidationResponse response = businessClosureValidationService.executeFullValidation(request);

            log.info("每日定时全量业务闭环校验任务执行完成，发现{}个问题，耗时{}ms",
                    response.getIssuesFound(), response.getExecutionTime());

        } catch (Exception e) {
            log.error("每日定时全量业务闭环校验任务执行失败", e);
        }
    }
}
