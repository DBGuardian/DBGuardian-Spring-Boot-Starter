package io.dbguardian.support;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

public class DbGuardianImportSelector implements ImportSelector, EnvironmentAware {

    private static final String BOOT2_AUTO_CONFIGURATION = "io.dbguardian.boot2.config.DbGuardianBoot2AutoConfiguration";
    private static final String BOOT3_AUTO_CONFIGURATION = "io.dbguardian.boot3.config.DbGuardianBoot3AutoConfiguration";

    private Environment environment;

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        ClassLoader classLoader = getClass().getClassLoader();
        if (isPresent(BOOT3_AUTO_CONFIGURATION, classLoader) && isPresent("org.springframework.boot.autoconfigure.AutoConfiguration", classLoader)) {
            return new String[]{BOOT3_AUTO_CONFIGURATION};
        }
        if (isPresent(BOOT2_AUTO_CONFIGURATION, classLoader)) {
            return new String[]{BOOT2_AUTO_CONFIGURATION};
        }
        return new String[0];
    }

    private boolean isPresent(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public Environment getEnvironment() {
        return environment;
    }
}