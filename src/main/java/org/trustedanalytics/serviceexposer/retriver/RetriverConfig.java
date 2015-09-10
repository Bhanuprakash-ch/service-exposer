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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.trustedanalytics.cloud.cc.FeignClient;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;
import org.trustedanalytics.serviceexposer.nats.registrator.NatsMessagingQueue;

import java.util.List;

@Configuration
public class RetriverConfig {

    @Value("${oauth.resource:/}")
    private String apiBaseUrl;

    @Value("#{'${restrictedServicesNames}'.split(',')}")
    private List<String> restricedNames;
    @Bean
    public OAuth2ClientContext oauth2ClientContext() {
        return new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest());
    }

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
    protected CcOperations ccPrivilegedClient() {
        return new FeignClient(apiBaseUrl, builder -> builder
                .requestInterceptor(template -> template.header("Authorization", "bearer "+clientRestTemplate().getAccessToken().toString())).logLevel(Logger.Level.NONE));
    }

    @Bean
    protected CustomCFOperations customCFOperations() {
        return new CustomCFOperations(clientRestTemplate(), apiBaseUrl);
    }

    @Bean
    protected CredentialsRetriver credentialsRetriver (NatsMessagingQueue natsOps,CredentialsStore redisCredentialsStore) {
        return new CredentialsRetriver(ccPrivilegedClient(), customCFOperations(),redisCredentialsStore,natsOps, apiBaseUrl);
    }

    @Bean
    protected ServicesRetriver servicesRetriver () {
        return new ServicesRetriver(ccPrivilegedClient(), apiBaseUrl,restricedNames);
    }
}
