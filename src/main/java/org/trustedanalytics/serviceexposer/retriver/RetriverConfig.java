/**
 * Copyright (c) 2015 Intel Corporation
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
 */
package org.trustedanalytics.serviceexposer.retriver;

import feign.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.trustedanalytics.cloud.auth.AuthTokenRetriever;
import org.trustedanalytics.cloud.auth.OAuth2TokenRetriever;
import org.trustedanalytics.cloud.cc.FeignClient;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;
import org.trustedanalytics.serviceexposer.nats.registrator.NatsMessagingQueue;

import java.util.List;

import static org.springframework.context.annotation.ScopedProxyMode.INTERFACES;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

@Configuration
public class RetriverConfig {

    @Value("${oauth.resource:/}")
    private String apiBaseUrl;

    @Value("#{'${restrictedServicesNames}'.split(',')}")
    private List<String> restricedNames;

    @Bean
    @ConfigurationProperties("spring.oauth2.client")
    public OAuth2ProtectedResourceDetails clientCredentials() {
        return new ClientCredentialsResourceDetails();
    }

    @Bean
    public OAuth2RestTemplate clientRestTemplate() {
        OAuth2RestTemplate template = new OAuth2RestTemplate(clientCredentials());
        ClientCredentialsAccessTokenProvider provider = new ClientCredentialsAccessTokenProvider();
        template.setAccessTokenProvider(provider);
        return template;
    }

    @Bean
    @Qualifier("CredentialRetriverClient")
    protected CcOperations ccPrivilegedClient() {
        return new FeignClient(apiBaseUrl, builder -> builder
                .requestInterceptor(template -> template.header("Authorization", "bearer " + clientRestTemplate().getAccessToken().toString()))
                .logLevel(Logger.Level.NONE));
    }

    @Bean
    protected CustomCFOperations customCFOperations() {
        return new CustomCFOperations(clientRestTemplate(), apiBaseUrl);
    }

    @Bean
    protected CredentialsRetriver credentialsRetriver(NatsMessagingQueue natsOps, CredentialsStore redisCredentialsStore) {
        return new CredentialsRetriver(ccPrivilegedClient(), customCFOperations(), redisCredentialsStore, natsOps, apiBaseUrl);
    }

    @Bean
    protected ServicesRetriver servicesRetriver() {
        return new ServicesRetriver(ccPrivilegedClient(), apiBaseUrl, restricedNames);
    }

    @Bean
    public AuthTokenRetriever authTokenRetriever() {
        return new OAuth2TokenRetriever();
    }

    @Bean
    @Qualifier("ControllerClient")
    @Scope(value = SCOPE_REQUEST, proxyMode = INTERFACES)
    public CcOperations ccOperations() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final String token = authTokenRetriever().getAuthToken(auth);

        return new FeignClient(apiBaseUrl, builder -> builder
                .requestInterceptor(template -> template.header("Authorization", "bearer " + token))
                .logLevel(Logger.Level.NONE));

    }
}
