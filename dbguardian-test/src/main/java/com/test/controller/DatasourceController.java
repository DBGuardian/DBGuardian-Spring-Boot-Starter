package com.test.controller;

import io.dbguardian.coordination.DatasourceCoordinationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据源状态监控控制器
 */
@Api(tags = "数据源状态监控")
@RestController
@RequestMapping("/datasource")
public class DatasourceController {

    @Autowired(required = false)
    private DatasourceCoordinationService coordinationService;

    @ApiOperation("获取数据源协调状态")
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        
        if (coordinationService != null) {
            result.put("coordinationAvailable", true);
            result.put("coordinationStatus", coordinationService.getCoordinationStatus());
        } else {
            result.put("coordinationAvailable", false);
            result.put("message", "协调服务不可用");
        }
        
        return result;
    }

    @ApiOperation("健康检查")
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "dbguardian-test");
        return result;
    }
}
