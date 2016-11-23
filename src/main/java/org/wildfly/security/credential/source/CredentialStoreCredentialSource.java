/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.security.credential.source;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;

import org.wildfly.common.Assert;
import org.wildfly.security._private.ElytronMessages;
import org.wildfly.security.auth.SupportLevel;
import org.wildfly.security.credential.AlgorithmCredential;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.credential.store.UnsupportedCredentialTypeException;

/**
 * A credential source which is backed by an entry in a credential store.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class CredentialStoreCredentialSource implements CredentialSource {
    private final CredentialStore credentialStore;
    private final String alias;

    /**
     * Construct a new instance.
     *
     * @param credentialStore the credential store (must not be {@code null})
     * @param alias the entry alias name (must not be {@code null})
     */
    public CredentialStoreCredentialSource(final CredentialStore credentialStore, final String alias) {
        Assert.checkNotNullParam("credentialStore", credentialStore);
        Assert.checkNotNullParam("alias", alias);
        this.credentialStore = credentialStore;
        this.alias = alias;
    }

    public SupportLevel getCredentialAcquireSupport(final Class<? extends Credential> credentialType, final String algorithmName, final AlgorithmParameterSpec parameterSpec) throws IOException {
        try {
            return credentialStore.exists(alias, credentialType) ? SupportLevel.POSSIBLY_SUPPORTED : SupportLevel.UNSUPPORTED;
        } catch (CredentialStoreException e) {
            throw ElytronMessages.log.unableToReadCredential(e);
        } catch (UnsupportedCredentialTypeException e) {
            return SupportLevel.UNSUPPORTED;
        }
    }

    public <C extends Credential> C getCredential(final Class<C> credentialType, final String algorithmName, final AlgorithmParameterSpec parameterSpec) throws IOException {
        final C credential;
        try {
            credential = credentialStore.retrieve(alias, credentialType);
        } catch (CredentialStoreException e) {
            throw ElytronMessages.log.unableToReadCredential(e);
        } catch (UnsupportedCredentialTypeException e) {
            return null;
        }
        if (credentialType.isInstance(credential)
            && (parameterSpec == null || credential.impliesParameters(parameterSpec))
            && (algorithmName == null || credential instanceof AlgorithmCredential && algorithmName.equals(((AlgorithmCredential) credential).getAlgorithm()))) {
            return credential;
        } else {
            return null;
        }
    }
}