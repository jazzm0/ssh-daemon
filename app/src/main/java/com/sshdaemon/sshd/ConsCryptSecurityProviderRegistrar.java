package com.sshdaemon.sshd;

import org.apache.sshd.common.util.security.AbstractSecurityProviderRegistrar;
import org.conscrypt.Conscrypt;

import java.security.Provider;

public class ConsCryptSecurityProviderRegistrar extends AbstractSecurityProviderRegistrar {

    protected ConsCryptSecurityProviderRegistrar() {
        super("ConsCryptWrapper");
        String baseName = getBasePropertyName();
        props.put(baseName + ".Cipher", "AES");
        props.put(baseName + ".Mac", "HmacSha1,HmacSha224,HmacSha256,HmacSha384,HmacSha512");
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
