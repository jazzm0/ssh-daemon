package com.sshdaemon.sshd;

import static com.sshdaemon.util.AndroidLogger.getLogger;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.security.AbstractSecurityProviderRegistrar;
import org.conscrypt.Conscrypt;
import org.slf4j.Logger;

import java.security.Provider;
import java.util.HashMap;
import java.util.Map;

public class ConsCryptSecurityProviderRegistrar extends AbstractSecurityProviderRegistrar {

    private static final Logger logger = getLogger();
    public static String NAME = "ConsCryptSecurityProvider";
    private final Map<String, String> defaultProperties = new HashMap<>();

    protected ConsCryptSecurityProviderRegistrar() {
        super(NAME);
        String baseName = getBasePropertyName();
        defaultProperties.put(baseName + ".Cipher", "AES");
        defaultProperties.put(baseName + ".Mac", "HmacMD5,HmacSha1,HmacSha224,HmacSha256,HmacSha384,HmacSha512");
        defaultProperties.put(baseName + ".KeyPairGenerator", "EC,RSA");
        defaultProperties.put(baseName + ".KeyFactory", "EC,RSA");
    }

    @Override
    public boolean isSecurityEntitySupported(Class<?> entityType, String name) {
        final var supported = super.isSecurityEntitySupported(entityType, name);
        logger.info("isSecurityEntitySupported: entityType={}, name={}, supported={}", entityType, name, supported);
        return supported;
    }

    @Override
    public String getString(String name) {
        String configured = super.getString(name);
        if (GenericUtils.isEmpty(configured)) {
            String byDefault = defaultProperties.get(name);
            if (byDefault != null) {
                return byDefault;
            }
        }
        return configured;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isNamedProviderUsed() {
        return false;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public Provider getSecurityProvider() {
        return Conscrypt.newProvider();
    }
}
