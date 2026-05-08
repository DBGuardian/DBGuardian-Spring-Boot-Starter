package com.erp.service.contract.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务合作合同批量更新响应DTO
 *
 * @author ERP System
 */
public class BusinessContractBatchUpdateResponse {

    /**
     * 成功的合同ID列表
     */
    private List<Integer> successIds = new ArrayList<>();

    /**
     * 失败的合同ID列表
     */
    private List<Integer> failedIds = new ArrayList<>();

    /**
     * 失败原因列表
     */
    private List<FailedReason> failedReasons = new ArrayList<>();

    /**
     * 是否全部成功
     */
    private Boolean allSuccess;

    /**
     * 失败原因
     */
    public static class FailedReason {
        private Integer contractId;
        private String reason;

        public FailedReason() {}

        public FailedReason(Integer contractId, String reason) {
            this.contractId = contractId;
            this.reason = reason;
        }

        public Integer getContractId() {
            return contractId;
        }

        public void setContractId(Integer contractId) {
            this.contractId = contractId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public List<Integer> getSuccessIds() {
        return successIds;
    }

    public void setSuccessIds(List<Integer> successIds) {
        this.successIds = successIds;
    }

    public List<Integer> getFailedIds() {
        return failedIds;
    }

    public void setFailedIds(List<Integer> failedIds) {
        this.failedIds = failedIds;
    }

    public List<FailedReason> getFailedReasons() {
        return failedReasons;
    }

    public void setFailedReasons(List<FailedReason> failedReasons) {
        this.failedReasons = failedReasons;
    }

    public Boolean getAllSuccess() {
        return allSuccess;
    }

    public void setAllSuccess(Boolean allSuccess) {
        this.allSuccess = allSuccess;
    }
}
