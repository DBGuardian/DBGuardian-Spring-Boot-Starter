package com.erp.service.system;

import com.erp.controller.system.dto.PermissionPageRequest;
import com.erp.controller.system.dto.PermissionRequest;
import com.erp.controller.system.dto.PermissionResponse;
import com.erp.controller.system.dto.PermissionTreeNode;

import java.util.List;

public interface PermissionService {
    List<PermissionResponse> list(Integer typeId, String keyword);
    com.baomidou.mybatisplus.core.metadata.IPage<PermissionResponse> listPage(Integer typeId, String keyword, Integer current, Integer size);
    com.baomidou.mybatisplus.core.metadata.IPage<PermissionResponse> getPermissionPage(PermissionPageRequest request);
    List<PermissionTreeNode> tree();
    PermissionResponse get(Integer id);
    Integer create(PermissionRequest req);
    void update(Integer id, PermissionRequest req);
    void delete(Integer id);
    List<Integer> addPagesToModule(Integer moduleId, List<PermissionRequest> pages);
    java.util.Map<String, Object> removePageFromModule(Integer moduleId, Integer pageId, boolean confirm);
    void batchUpdateModulePageAssociations(Integer moduleId, java.util.List<String> addedIds, java.util.List<String> removedIds);
    void batchUpdatePageFieldAssociations(Integer pageId, java.util.List<String> addedIds, java.util.List<String> removedIds);
    void batchDelete(List<Integer> ids);
    String generatePermissionCode(String permissionName, Integer permissionTypeId, Integer parentPermissionId);
}


