/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.security;

import com.thoughtworks.go.util.ClassMockery;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(org.jmock.integration.junit4.JMock.class)
public class AuthSSLProtocolSocketFactoryTest {

    Mockery context = new ClassMockery();

    @Test
    public void shouldRecreateSslSessionIfInitialisedWithEmptyKeystore()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        final AuthSSLKeyManagerFactory keyManagerFactory = context.mock(AuthSSLKeyManagerFactory.class);
        context.checking(new Expectations() {
            {
                exactly(2).of(keyManagerFactory).keyManagers();
                will(returnValue(null));
            }
        });

        AuthSSLProtocolSocketFactory factory = new AuthSSLProtocolSocketFactory(null, keyManagerFactory, new SSLContextInstanceFactory());

        // first time: create ssl context with empty keystore
        SSLContext oldSslContext = factory.getSSLContext();

        context.checking(new Expectations() {
            {
                allowing(keyManagerFactory).keyManagers();
                will(returnValue(new KeyManager[] { }));
            }
        });

        SSLContext newSslContext = factory.getSSLContext();
        assertThat(oldSslContext, not(newSslContext));
    }

    @Test
    public void shouldEnableOnlyTheValidCiphersForJetty6() throws Exception {
        SSLContextInstanceFactory instanceFactory = mock(SSLContextInstanceFactory.class);
        SSLContext sslContext = mock(SSLContext.class);
        when(instanceFactory.create()).thenReturn(sslContext);
        when(sslContext.getServerSocketFactory()).thenReturn(mock(SSLServerSocketFactory.class));

        AuthSSLProtocolSocketFactory socketFactory = new AuthSSLProtocolSocketFactory(null, null, instanceFactory);
        socketFactory.createSocket("abc", 2020);
    }
}
