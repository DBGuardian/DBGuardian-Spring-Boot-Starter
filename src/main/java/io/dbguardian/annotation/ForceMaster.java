package io.dbguardian.annotation;

import java.lang.annotation.*;

/**
 * 标记方法强制使用主库连接
 * 优先级高于 ReadOnlyConnection
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ForceMaster {
}
