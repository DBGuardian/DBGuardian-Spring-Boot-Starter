package io.dbguardian.config;

/**
 * 国产数据库适配器配置
 * 统一管理国产数据库的特定配置
 */
public class DomesticDatabaseAdapters {

    /**
     * 达梦数据库 (DM)
     */
    public static DatabaseAdapterProperties.DatabaseAdapter createDMAdapter() {
        DatabaseAdapterProperties.DatabaseAdapter adapter = new DatabaseAdapterProperties.DatabaseAdapter();
        adapter.setName("达梦 (DM)");
        adapter.setDriverClassName("dm.jdbc.driver.DmDriver");
        adapter.setDefaultPort(5236);
        adapter.setUrlPrefix("jdbc:dm://");
        adapter.setHealthCheckSql("SELECT 1 FROM DUAL");

        DatabaseAdapterProperties.PaginationFormat pagination = new DatabaseAdapterProperties.PaginationFormat();
        pagination.setFetchNextTemplate("FETCH FIRST %d ROWS ONLY");
        pagination.setOffsetTemplate("OFFSET %d ROWS");
        adapter.setPagination(pagination);

        return adapter;
    }

    /**
     * 人大金仓 (KingBase)
     */
    public static DatabaseAdapterProperties.DatabaseAdapter createKingBaseAdapter() {
        DatabaseAdapterProperties.DatabaseAdapter adapter = new DatabaseAdapterProperties.DatabaseAdapter();
        adapter.setName("人大金仓 (KingBase)");
        adapter.setDriverClassName("com.kingbase.Driver");
        adapter.setDefaultPort(54321);
        adapter.setUrlPrefix("jdbc:kingbase://");
        adapter.setHealthCheckSql("SELECT 1");

        DatabaseAdapterProperties.PaginationFormat pagination = new DatabaseAdapterProperties.PaginationFormat();
        pagination.setLimitOffsetTemplate("LIMIT %d OFFSET %d");
        pagination.setFetchNextTemplate("FETCH FIRST %d ROWS ONLY");
        adapter.setPagination(pagination);

        return adapter;
    }

    /**
     * 华为高斯 (GaussDB)
     */
    public static DatabaseAdapterProperties.DatabaseAdapter createGaussDBAdapter() {
        DatabaseAdapterProperties.DatabaseAdapter adapter = new DatabaseAdapterProperties.DatabaseAdapter();
        adapter.setName("华为高斯 (GaussDB)");
        adapter.setDriverClassName("com.huawei.gauss.row.driver.JdbcDriver");
        adapter.setDefaultPort(5432);
        adapter.setUrlPrefix("jdbc:gauss://");
        adapter.setHealthCheckSql("SELECT 1");

        DatabaseAdapterProperties.PaginationFormat pagination = new DatabaseAdapterProperties.PaginationFormat();
        pagination.setLimitOffsetTemplate("LIMIT %d OFFSET %d");
        pagination.setFetchNextTemplate("FETCH FIRST %d ROWS ONLY");
        pagination.setOffsetTemplate("OFFSET %d ROWS");
        adapter.setPagination(pagination);

        return adapter;
    }

    /**
     * OceanBase
     */
    public static DatabaseAdapterProperties.DatabaseAdapter createOceanBaseAdapter() {
        DatabaseAdapterProperties.DatabaseAdapter adapter = new DatabaseAdapterProperties.DatabaseAdapter();
        adapter.setName("OceanBase");
        adapter.setDriverClassName("com.oceanbase.jdbc.Driver");
        adapter.setDefaultPort(2883);
        adapter.setUrlPrefix("jdbc:oceanbase://");
        adapter.setHealthCheckSql("SELECT 1");

        DatabaseAdapterProperties.PaginationFormat pagination = new DatabaseAdapterProperties.PaginationFormat();
        pagination.setLimitOffsetTemplate("LIMIT %d OFFSET %d");
        adapter.setPagination(pagination);

        return adapter;
    }

    /**
     * TiDB
     * 兼容 MySQL 协议
     */
    public static DatabaseAdapterProperties.DatabaseAdapter createTiDBAdapter() {
        DatabaseAdapterProperties.DatabaseAdapter adapter = new DatabaseAdapterProperties.DatabaseAdapter();
        adapter.setName("TiDB");
        adapter.setDriverClassName("com.mysql.cj.jdbc.Driver");
        adapter.setDefaultPort(4000);
        adapter.setUrlPrefix("jdbc:mysql://");
        adapter.setHealthCheckSql("SELECT 1");

        DatabaseAdapterProperties.PaginationFormat pagination = new DatabaseAdapterProperties.PaginationFormat();
        pagination.setLimitOffsetTemplate("LIMIT %d OFFSET %d");
        adapter.setPagination(pagination);

        return adapter;
    }

    /**
     * 神通国产数据库
     */
    public static DatabaseAdapterProperties.DatabaseAdapter createShenTongAdapter() {
        DatabaseAdapterProperties.DatabaseAdapter adapter = new DatabaseAdapterProperties.DatabaseAdapter();
        adapter.setName("神通国产数据库");
        adapter.setDriverClassName("com.oscar.Driver");
        adapter.setDefaultPort(2003);
        adapter.setUrlPrefix("jdbc:oscar://");
        adapter.setHealthCheckSql("SELECT 1 FROM DUAL");

        DatabaseAdapterProperties.PaginationFormat pagination = new DatabaseAdapterProperties.PaginationFormat();
        pagination.setLimitOffsetTemplate("LIMIT %d OFFSET %d");
        adapter.setPagination(pagination);

        return adapter;
    }

    /**
     * 瀚高 (HighGo)
     */
    public static DatabaseAdapterProperties.DatabaseAdapter createHighGoAdapter() {
        DatabaseAdapterProperties.DatabaseAdapter adapter = new DatabaseAdapterProperties.DatabaseAdapter();
        adapter.setName("瀚高 (HighGo)");
        adapter.setDriverClassName("com.highgo.jdbc.Driver");
        adapter.setDefaultPort(5866);
        adapter.setUrlPrefix("jdbc:highgo://");
        adapter.setHealthCheckSql("SELECT 1");

        DatabaseAdapterProperties.PaginationFormat pagination = new DatabaseAdapterProperties.PaginationFormat();
        pagination.setLimitOffsetTemplate("LIMIT %d OFFSET %d");
        pagination.setFetchNextTemplate("FETCH FIRST %d ROWS ONLY");
        adapter.setPagination(pagination);

        return adapter;
    }

    /**
     * 优炫 (UXSino)
     */
    public static DatabaseAdapterProperties.DatabaseAdapter createUXSinoAdapter() {
        DatabaseAdapterProperties.DatabaseAdapter adapter = new DatabaseAdapterProperties.DatabaseAdapter();
        adapter.setName("优炫 (UXSino)");
        adapter.setDriverClassName("com.uxsino.jdbc.Driver");
        adapter.setDefaultPort(3218);
        adapter.setUrlPrefix("jdbc:uxsino://");
        adapter.setHealthCheckSql("SELECT 1 FROM DUAL");

        DatabaseAdapterProperties.PaginationFormat pagination = new DatabaseAdapterProperties.PaginationFormat();
        pagination.setLimitOffsetTemplate("LIMIT %d OFFSET %d");
        adapter.setPagination(pagination);

        return adapter;
    }

    /**
     * 获取所有国产数据库适配器
     */
    public static java.util.Map<String, DatabaseAdapterProperties.DatabaseAdapter> getAllDomesticAdapters() {
        java.util.Map<String, DatabaseAdapterProperties.DatabaseAdapter> adapters = new java.util.HashMap<>();
        adapters.put("dm", createDMAdapter());
        adapters.put("kingbase", createKingBaseAdapter());
        adapters.put("gaussdb", createGaussDBAdapter());
        adapters.put("oceanbase", createOceanBaseAdapter());
        adapters.put("tidb", createTiDBAdapter());
        adapters.put("shentong", createShenTongAdapter());
        adapters.put("highgo", createHighGoAdapter());
        adapters.put("uxsino", createUXSinoAdapter());
        return adapters;
    }
}
