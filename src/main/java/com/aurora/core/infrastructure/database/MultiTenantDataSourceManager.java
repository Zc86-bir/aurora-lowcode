package com.aurora.core.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Multi-tenant Data Source Manager
 *
 * Supports schema-based tenant isolation with connection pooling.
 * Each tenant gets a dedicated HikariCP pool for optimal performance.
 *
 * Connection routing:
 * - Tenant context → schema selection
 * - Shared pool for system operations
 * - Per-tenant pools for isolation
 */
public class MultiTenantDataSourceManager {

    private final Map<String, HikariConfig> tenantConfigs = new ConcurrentHashMap<>();
    private final Map<String, HikariDataSource> tenantDataSources = new ConcurrentHashMap<>();
    private final DataSource systemDataSource;

    public MultiTenantDataSourceManager(DataSource systemDataSource) {
        this.systemDataSource = systemDataSource;
    }

    private final ReentrantLock writeLock = new ReentrantLock();

    /**
     * Register a new tenant data source.
     * Uses ReentrantLock instead of synchronized to prevent thread pinning with virtual threads.
     */
    public void registerTenant(UUID tenantId, String jdbcUrl, String username,
                                String password, String schemaName,
                                Map<String, String> poolConfig) {
        writeLock.lock();
        try {
        String tenantKey = tenantId.toString();

        if (tenantDataSources.containsKey(tenantKey)) {
            throw new IllegalArgumentException(
                "Data source already registered for tenant: " + tenantId);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setSchema(schemaName);
        config.setPoolName("HikariPool-Tenant-" + tenantId.toString().substring(0, 8));

        // Apply pool configuration
        config.setMaximumPoolSize(parseIntOr(poolConfig.get("maxPoolSize"), 10));
        config.setMinimumIdle(parseIntOr(poolConfig.get("minIdle"), 2));
        config.setConnectionTimeout(parseLongOr(poolConfig.get("connectionTimeoutMs"), 30000L));
        config.setIdleTimeout(parseLongOr(poolConfig.get("idleTimeoutMs"), 600000L));
        config.setMaxLifetime(parseLongOr(poolConfig.get("maxLifetimeMs"), 1800000L));

        // Virtual thread optimization
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        tenantConfigs.put(tenantKey, config);

        HikariDataSource dataSource = new HikariDataSource(config);
        tenantDataSources.put(tenantKey, dataSource);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Get data source for a specific tenant.
     */
    public DataSource getTenantDataSource(UUID tenantId) {
        String tenantKey = tenantId.toString();
        HikariDataSource dataSource = tenantDataSources.get(tenantKey);
        if (dataSource == null) {
            throw new IllegalArgumentException(
                "No data source registered for tenant: " + tenantId);
        }
        return dataSource;
    }

    /**
     * Get system data source (shared).
     */
    public DataSource getSystemDataSource() {
        return systemDataSource;
    }

    /**
     * Get or resolve data source based on tenant context.
     */
    public DataSource resolveDataSource(UUID tenantId) {
        String tenantKey = tenantId.toString();
        HikariDataSource dataSource = tenantDataSources.get(tenantKey);
        return dataSource != null ? dataSource : systemDataSource;
    }

    /**
     * Check if tenant data source exists.
     */
    public boolean hasTenantDataSource(UUID tenantId) {
        return tenantDataSources.containsKey(tenantId.toString());
    }

    /**
     * Get tenant data source pool statistics.
     */
    public PoolStatistics getPoolStatistics(UUID tenantId) {
        String tenantKey = tenantId.toString();
        HikariDataSource dataSource = tenantDataSources.get(tenantKey);
        if (dataSource == null) {
            return PoolStatistics.unavailable(tenantId);
        }

        return new PoolStatistics(
            tenantId,
            dataSource.getPoolName(),
            dataSource.getMaximumPoolSize(),
            dataSource.getMinimumIdle(),
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Get all registered tenant IDs.
     */
    public java.util.List<UUID> getRegisteredTenants() {
        return tenantDataSources.keySet().stream()
            .map(UUID::fromString)
            .toList();
    }

    /**
     * Get total connection count across all pools.
     */
    public int getTotalConnections() {
        return tenantDataSources.values().stream()
            .mapToInt(ds -> ds.getHikariPoolMXBean().getTotalConnections())
            .sum();
    }

    /**
     * Remove a tenant data source.
     * Uses ReentrantLock to prevent thread pinning with virtual threads.
     */
    public void removeTenant(UUID tenantId) {
        writeLock.lock();
        try {
            String tenantKey = tenantId.toString();
            HikariDataSource dataSource = tenantDataSources.remove(tenantKey);
            if (dataSource != null) {
                dataSource.close();
            }
            tenantConfigs.remove(tenantKey);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Close all tenant data sources.
     * Uses ReentrantLock to prevent thread pinning with virtual threads.
     */
    public void closeAll() {
        writeLock.lock();
        try {
            for (HikariDataSource dataSource : tenantDataSources.values()) {
                dataSource.close();
            }
            tenantDataSources.clear();
            tenantConfigs.clear();
        } finally {
            writeLock.unlock();
        }
    }

    // Internal

    private int parseIntOr(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long parseLongOr(String value, long defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Connection pool statistics.
     */
    public record PoolStatistics(
        UUID tenantId,
        String poolName,
        int maxPoolSize,
        int minIdle,
        int activeConnections,
        int idleConnections,
        int totalConnections,
        int threadsAwaitingConnection
    ) {
        public static PoolStatistics unavailable(UUID tenantId) {
            return new PoolStatistics(tenantId, "unavailable", 0, 0, 0, 0, 0, 0);
        }
    }
}