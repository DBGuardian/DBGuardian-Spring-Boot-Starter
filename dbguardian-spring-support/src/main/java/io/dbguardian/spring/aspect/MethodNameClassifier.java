package io.dbguardian.spring.aspect;

/**
 * 基于方法名前缀的读写操作分类器
 */
public class MethodNameClassifier {

    private static final String[] READ_PREFIXES = {
        "select", "get", "query", "find", "count",
        "list", "page", "search", "load", "fetch",
        "exist", "exists", "check", "has", "is"
    };

    private static final String[] WRITE_PREFIXES = {
        "insert", "save", "add", "create",
        "update", "modify", "edit",
        "delete", "remove", "drop",
        "batch", "bulk"
    };

    private MethodNameClassifier() {
    }

    /**
     * 判断方法是否为读操作
     *
     * @param methodName 方法名
     * @return true if read operation
     */
    public static boolean isReadMethod(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return false;
        }
        String lowerName = methodName.toLowerCase();
        for (String prefix : READ_PREFIXES) {
            if (lowerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断方法是否为写操作
     *
     * @param methodName 方法名
     * @return true if write operation
     */
    public static boolean isWriteMethod(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return false;
        }
        String lowerName = methodName.toLowerCase();
        for (String prefix : WRITE_PREFIXES) {
            if (lowerName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据方法名判断读写操作类型
     *
     * @param methodName 方法名
     * @return "read" 或 "write"
     */
    public static String classify(String methodName) {
        return isReadMethod(methodName) ? "read" : "write";
    }
}
