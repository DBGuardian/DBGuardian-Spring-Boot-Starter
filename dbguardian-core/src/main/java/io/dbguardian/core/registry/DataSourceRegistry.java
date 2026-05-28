package io.dbguardian.core.registry;

import io.dbguardian.core.datasource.DataSourceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DataSourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRegistry.class);

    private final Map<String, DataSourceWrapper> masters = new ConcurrentHashMap<String, DataSourceWrapper>();
    private final Map<String, DataSourceWrapper> slaves = new ConcurrentHashMap<String, DataSourceWrapper>();
    private final Set<String> availableMasterIds = ConcurrentHashMap.newKeySet();
    private final Set<String> availableSlaveIds = ConcurrentHashMap.newKeySet();

    public void registerMaster(String id, DataSourceWrapper wrapper) {
        masters.put(id, wrapper);
        availableMasterIds.add(id);
        log.info("主库已注册: id={}, url={}", id, wrapper.getUrl());
    }

    public void registerSlave(String id, DataSourceWrapper wrapper) {
        slaves.put(id, wrapper);
        availableSlaveIds.add(id);
        log.info("从库已注册: id={}, url={}", id, wrapper.getUrl());
    }

    public void unregisterMaster(String id) {
        masters.remove(id);
        availableMasterIds.remove(id);
    }

    public void unregisterSlave(String id) {
        slaves.remove(id);
        availableSlaveIds.remove(id);
    }

    public List<DataSourceWrapper> getAllMasters() {
        return new ArrayList<DataSourceWrapper>(masters.values());
    }

    public List<DataSourceWrapper> getAllSlaves() {
        return new ArrayList<DataSourceWrapper>(slaves.values());
    }

    public List<DataSourceWrapper> getAvailableMasters() {
        return availableMasterIds.stream()
                .map(masters::get)
                .filter(Objects::nonNull)
                .filter(DataSourceWrapper::isAvailable)
                .collect(Collectors.toList());
    }

    public List<DataSourceWrapper> getAvailableSlaves() {
        return availableSlaveIds.stream()
                .map(slaves::get)
                .filter(Objects::nonNull)
                .filter(DataSourceWrapper::isAvailable)
                .collect(Collectors.toList());
    }

    public DataSourceWrapper getMaster(String id) {
        return masters.get(id);
    }

    public DataSourceWrapper getSlave(String id) {
        return slaves.get(id);
    }

    public int getAvailableMasterCount() {
        return (int) getAvailableMasters().size();
    }

    public int getAvailableSlaveCount() {
        return (int) getAvailableSlaves().size();
    }

    public void updateMasterAvailability(String id, boolean available) {
        DataSourceWrapper wrapper = masters.get(id);
        if (wrapper == null) {
            return;
        }
        if (available) {
            wrapper.markAvailable();
            availableMasterIds.add(id);
        } else {
            wrapper.markUnavailable();
            availableMasterIds.remove(id);
        }
    }

    public void updateSlaveAvailability(String id, boolean available) {
        DataSourceWrapper wrapper = slaves.get(id);
        if (wrapper == null) {
            return;
        }
        if (available) {
            wrapper.markAvailable();
            availableSlaveIds.add(id);
        } else {
            wrapper.markUnavailable();
            availableSlaveIds.remove(id);
        }
    }

    public Optional<DataSourceWrapper> getHighestPriorityMaster() {
        return getAvailableMasters().stream().max(Comparator.comparingInt(DataSourceWrapper::getPriority));
    }

    public void updateSlavesAvailability(Map<String, Boolean> availability) {
        for (Map.Entry<String, Boolean> entry : availability.entrySet()) {
            updateSlaveAvailability(entry.getKey(), entry.getValue().booleanValue());
        }
    }

    public void clear() {
        masters.clear();
        slaves.clear();
        availableMasterIds.clear();
        availableSlaveIds.clear();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        Map<String, Object> masterStats = new HashMap<String, Object>();
        masterStats.put("total", Integer.valueOf(masters.size()));
        masterStats.put("available", Integer.valueOf(getAvailableMasterCount()));
        stats.put("masters", masterStats);

        Map<String, Object> slaveStats = new HashMap<String, Object>();
        slaveStats.put("total", Integer.valueOf(slaves.size()));
        slaveStats.put("available", Integer.valueOf(getAvailableSlaveCount()));
        stats.put("slaves", slaveStats);
        return stats;
    }
}