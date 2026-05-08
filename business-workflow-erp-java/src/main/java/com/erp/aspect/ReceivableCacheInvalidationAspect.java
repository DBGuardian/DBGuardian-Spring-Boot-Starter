package com.erp.aspect;

import com.erp.service.report.ReceivableDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 应收账款缓存失效切面
 * 
 * 功能描述：
 * - 监听合同、结算单、发票、应收明细的新增/修改操作
 * - 自动清除应收账款缓存
 * 
 * 缓存失效场景：
 * 1. 新增合同时
 * 2. 修改合同时
 * 3. 新增结算单时
 * 4. 修改结算单时
 * 5. 新增发票时
 * 6. 修改发票时
 * 7. 新增应收明细时
 * 8. 修改应收明细时
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ReceivableCacheInvalidationAspect {

  private final ReceivableDetailService receivableDetailService;

  /**
   * 合同新增后清除缓存
   */
  @AfterReturning("execution(* com.erp.service.contract.ContractService.create(..))")
  public void afterContractCreate() {
    log.info("合同新增，清除应收账款缓存");
    receivableDetailService.clearAllCache();
  }

  /**
   * 合同修改后清除缓存
   */
  @AfterReturning("execution(* com.erp.service.contract.ContractService.update(..))")
  public void afterContractUpdate() {
    log.info("合同修改，清除应收账款缓存");
    receivableDetailService.clearAllCache();
  }

  /**
   * 结算单新增后清除缓存
   */
  @AfterReturning("execution(* com.erp.service.settlement.SettlementService.create(..))")
  public void afterSettlementCreate() {
    log.info("结算单新增，清除应收账款缓存");
    receivableDetailService.clearAllCache();
  }

  /**
   * 结算单修改后清除缓存
   */
  @AfterReturning("execution(* com.erp.service.settlement.SettlementService.update(..))")
  public void afterSettlementUpdate() {
    log.info("结算单修改，清除应收账款缓存");
    receivableDetailService.clearAllCache();
  }

  /**
   * 发票新增后清除缓存
   */
  @AfterReturning("execution(* com.erp.service.finance.InvoiceService.create(..))")
  public void afterInvoiceCreate() {
    log.info("发票新增，清除应收账款缓存");
    receivableDetailService.clearAllCache();
  }

  /**
   * 发票修改后清除缓存
   */
  @AfterReturning("execution(* com.erp.service.finance.InvoiceService.update(..))")
  public void afterInvoiceUpdate() {
    log.info("发票修改，清除应收账款缓存");
    receivableDetailService.clearAllCache();
  }
}
