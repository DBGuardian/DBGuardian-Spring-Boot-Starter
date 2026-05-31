package com.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源状态监控控制器
 */
@Tag(name = "数据源状态监控", description = "数据源协调状态相关接口")
@RestController
@RequestMapping("/datasource")
public class DatasourceController {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    @Qualifier("datasourceCoordinationService")
    private Object coordinationService;

    @Operation(summary = "获取数据源协调状态")
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("coordinationAvailable", coordinationService != null);
        if (coordinationService != null) {
            result.put("beanName", "datasourceCoordinationService");
            result.put("beanType", coordinationService.getClass().getName());
            result.put("instanceId", invokeGetter("getInstanceId"));
            result.put("applicationName", invokeGetter("getApplicationName"));
            result.put("masterStatus", invokeGetter("getMasterStatus"));
            result.put("masterInstanceId", invokeGetter("getMasterInstanceId"));
        } else {
            result.put("message", "协调服务不可用");
            result.put("beanPresent", applicationContext.containsBean("datasourceCoordinationService"));
        }
        return result;
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "dbguardian-test-java17-sb30-mbp");
        return result;
    }

    private Object invokeGetter(String methodName) {
        try {
            Method method = coordinationService.getClass().getMethod(methodName);
            return method.invoke(coordinationService);
        } catch (Exception ex) {
            return null;
        }
    }
}
