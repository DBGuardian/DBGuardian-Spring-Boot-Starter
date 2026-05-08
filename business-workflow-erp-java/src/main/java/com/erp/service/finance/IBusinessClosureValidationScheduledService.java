package com.erp.service.finance;

/**
 * 业务闭环校验定时任务服务接口
 *
 * @author ERP System
 * @date 2025-02-07
 */
public interface IBusinessClosureValidationScheduledService {

    /**
     * 执行每日全量业务闭环校验
     * 每天9点自动执行全量业务闭环校验，将结果保存到Redis缓存中
     */
    void executeDailyFullValidation();
}
