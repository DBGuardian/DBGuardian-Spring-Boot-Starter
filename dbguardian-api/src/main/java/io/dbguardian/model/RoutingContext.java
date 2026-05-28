package io.dbguardian.model;

public class RoutingContext {

    private String operation = "write";
    private boolean forceMaster;
    private boolean transactional;
    private boolean readOnlyTransaction;
    private String shardKey;
    private String ormType = "unknown";

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean isForceMaster() {
        return forceMaster;
    }

    public void setForceMaster(boolean forceMaster) {
        this.forceMaster = forceMaster;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    public boolean isReadOnlyTransaction() {
        return readOnlyTransaction;
    }

    public void setReadOnlyTransaction(boolean readOnlyTransaction) {
        this.readOnlyTransaction = readOnlyTransaction;
    }

    public String getShardKey() {
        return shardKey;
    }

    public void setShardKey(String shardKey) {
        this.shardKey = shardKey;
    }

    public String getOrmType() {
        return ormType;
    }

    public void setOrmType(String ormType) {
        this.ormType = ormType;
    }
}