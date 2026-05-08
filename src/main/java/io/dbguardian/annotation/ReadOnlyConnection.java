package io.dbguardian.annotation;

import java.lang.annotation.*;

/**
 * 标记方法强制使用只读连接（从库）
 * 用于特殊情况下的读写分离控制
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnlyConnection {
}
