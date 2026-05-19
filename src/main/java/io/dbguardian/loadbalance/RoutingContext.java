package io.dbguardian.loadbalance;

/**
 * 路由上下文
 * 携带路由决策所需的所有信息
 */
public class RoutingContext {

    private String shardKey;           // 分片键
    private String tableName;         // 表名
    private String methodName;        // 方法名
    private OperationType operationType; // 操作类型
    private boolean forceMaster;      // 强制使用主库
    private boolean readOnlyHint;     // 只读提示

    // 线程本地存储
    private static final ThreadLocal<RoutingContext> CONTEXT = new ThreadLocal<>();

    public static void set(RoutingContext ctx) {
        CONTEXT.set(ctx);
    }

    public static RoutingContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    // Getters and Setters
    public String getShardKey() {
        return shardKey;
    }

    public void setShardKey(String shardKey) {
        this.shardKey = shardKey;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public boolean isForceMaster() {
        return forceMaster;
    }

    public void setForceMaster(boolean forceMaster) {
        this.forceMaster = forceMaster;
    }

    public boolean isReadOnlyHint() {
        return readOnlyHint;
    }

    public void setReadOnlyHint(boolean readOnlyHint) {
        this.readOnlyHint = readOnlyHint;
    }

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        READ,
        WRITE,
        TRANSACTION,
        UNKNOWN
    }

    /**
     * 路由上下文构建器
     */
    public static class Builder {
        private final RoutingContext ctx = new RoutingContext();

        public Builder shardKey(String shardKey) {
            ctx.setShardKey(shardKey);
            return this;
        }

        public Builder tableName(String tableName) {
            ctx.setTableName(tableName);
            return this;
        }

        public Builder methodName(String methodName) {
            ctx.setMethodName(methodName);
            return this;
        }

        public Builder operationType(OperationType operationType) {
            ctx.setOperationType(operationType);
            return this;
        }

        public Builder forceMaster(boolean forceMaster) {
            ctx.setForceMaster(forceMaster);
            return this;
        }

        public Builder readOnlyHint(boolean readOnlyHint) {
            ctx.setReadOnlyHint(readOnlyHint);
            return this;
        }

        public RoutingContext build() {
            return ctx;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
