package io.dbguardian.registry;

import io.dbguardian.loadbalance.DataSourceWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据源注册中心
 * 统一管理所有主库和从库的生命周期和可用性
 */
@Slf4j
@Component
public class DataSourceRegistry {

    // 主库Map: id -> DataSourceWrapper
    private final Map<String, DataSourceWrapper> masters = new ConcurrentHashMap<>();

    // 从库Map: id -> DataSourceWrapper
    private final Map<String, DataSourceWrapper> slaves = new ConcurrentHashMap<>();

    // 可用主库ID集合
    private final Set<String> availableMasterIds = ConcurrentHashMap.newKeySet();

    // 可用从库ID集合
    private final Set<String> availableSlaveIds = ConcurrentHashMap.newKeySet();

    /**
     * 注册主库
     */
    public void registerMaster(String id, DataSourceWrapper wrapper) {
        masters.put(id, wrapper);
        availableMasterIds.add(id);
        log.info("主库已注册: id={}, url={}", id, wrapper.getUrl());
    }

    /**
     * 注册从库
     */
    public void registerSlave(String id, DataSourceWrapper wrapper) {
        slaves.put(id, wrapper);
        availableSlaveIds.add(id);
        log.info("从库已注册: id={}, url={}", id, wrapper.getUrl());
    }

    /**
     * 注销主库
     */
    public void unregisterMaster(String id) {
        masters.remove(id);
        availableMasterIds.remove(id);
        log.info("主库已注销: id={}", id);
    }

    /**
     * 注销从库
     */
    public void unregisterSlave(String id) {
        slaves.remove(id);
        availableSlaveIds.remove(id);
        log.info("从库已注销: id={}", id);
    }

    /**
     * 获取所有已注册主库
     */
    public List<DataSourceWrapper> getAllMasters() {
        return new ArrayList<>(masters.values());
    }

    /**
     * 获取所有已注册从库
     */
    public List<DataSourceWrapper> getAllSlaves() {
        return new ArrayList<>(slaves.values());
    }

    /**
     * 获取所有可用主库
     */
    public List<DataSourceWrapper> getAvailableMasters() {
        return availableMasterIds.stream()
                .map(masters::get)
                .filter(Objects::nonNull)
                .filter(DataSourceWrapper::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有可用从库
     */
    public List<DataSourceWrapper> getAvailableSlaves() {
        return availableSlaveIds.stream()
                .map(slaves::get)
                .filter(Objects::nonNull)
                .filter(DataSourceWrapper::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定主库
     */
    public DataSourceWrapper getMaster(String id) {
        return masters.get(id);
    }

    /**
     * 获取指定从库
     */
    public DataSourceWrapper getSlave(String id) {
        return slaves.get(id);
    }

    /**
     * 获取可用主库数量
     */
    public int getAvailableMasterCount() {
        return (int) availableMasterIds.stream()
                .map(masters::get)
                .filter(Objects::nonNull)
                .filter(DataSourceWrapper::isAvailable)
                .count();
    }

    /**
     * 获取可用从库数量
     */
    public int getAvailableSlaveCount() {
        return (int) availableSlaveIds.stream()
                .map(slaves::get)
                .filter(Objects::nonNull)
                .filter(DataSourceWrapper::isAvailable)
                .count();
    }

    /**
     * 检查主库是否可用
     */
    public boolean isMasterAvailable(String id) {
        DataSourceWrapper wrapper = masters.get(id);
        return wrapper != null && wrapper.isAvailable();
    }

    /**
     * 检查从库是否可用
     */
    public boolean isSlaveAvailable(String id) {
        DataSourceWrapper wrapper = slaves.get(id);
        return wrapper != null && wrapper.isAvailable();
    }

    /**
     * 更新主库可用性
     */
    public void updateMasterAvailability(String id, boolean available) {
        DataSourceWrapper wrapper = masters.get(id);
        if (wrapper != null) {
            if (available) {
                wrapper.markAvailable();
                availableMasterIds.add(id);
                log.info("主库可用: {}", id);
            } else {
                wrapper.markUnavailable();
                availableMasterIds.remove(id);
                log.warn("主库不可用: {}", id);
            }
        }
    }

    /**
     * 更新从库可用性
     */
    public void updateSlaveAvailability(String id, boolean available) {
        DataSourceWrapper wrapper = slaves.get(id);
        if (wrapper != null) {
            if (available) {
                wrapper.markAvailable();
                availableSlaveIds.add(id);
                log.info("从库可用: {}", id);
            } else {
                wrapper.markUnavailable();
                availableSlaveIds.remove(id);
                log.warn("从库不可用: {}", id);
            }
        }
    }

    /**
     * 按优先级获取最高优先级主库
     */
    public Optional<DataSourceWrapper> getHighestPriorityMaster() {
        return getAvailableMasters().stream()
                .max(Comparator.comparingInt(DataSourceWrapper::getPriority));
    }

    /**
     * 获取所有从库所属主库
     */
    public List<DataSourceWrapper> getSlavesByMaster(String masterId) {
        return getAllSlaves().stream()
                .filter(s -> masterId.equals(s.getId())) // 实际应该用 masterRef 字段
                .collect(Collectors.toList());
    }

    /**
     * 批量更新从库状态
     */
    public void updateSlavesAvailability(Map<String, Boolean> availability) {
        for (Map.Entry<String, Boolean> entry : availability.entrySet()) {
            updateSlaveAvailability(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 清除所有注册
     */
    public void clear() {
        masters.clear();
        slaves.clear();
        availableMasterIds.clear();
        availableSlaveIds.clear();
        log.info("数据源注册中心已清除");
    }

    /**
     * 获取注册统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("masters", Map.of(
                "total", masters.size(),
                "available", getAvailableMasterCount()
        ));
        stats.put("slaves", Map.of(
                "total", slaves.size(),
                "available", getAvailableSlaveCount()
        ));
        return stats;
    }
}
