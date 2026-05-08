package io.dbguardian;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@EnableConfigurationProperties
@Import(io.dbguardian.config.DbGuardianAutoConfiguration.class)
public @interface EnableDbGuardian {
}
