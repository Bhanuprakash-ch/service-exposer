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
package org.trustedanalytics.serviceexposer.checker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.serviceexposer.keyvaluestore.CredentialProperties;
import org.trustedanalytics.serviceexposer.keyvaluestore.CredentialsStore;
import org.trustedanalytics.serviceexposer.retriver.CredentialsRetriver;
import org.trustedanalytics.serviceexposer.retriver.ServicesRetriver;

import java.util.List;

@Configuration
public class CheckerConfig {

    @Value("${checker.triggerExpression}")
    private String checkerTriggerExpression;

    @Value("#{'${serviceTypes}'.split(',')}")
    private List<String> serviceTypes;


    @Bean
    protected RestOperations userRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CheckerJob checkerJob(ServicesRetriver servicesRetriver, CredentialsRetriver credentialsRetriver, CredentialsStore<CredentialProperties> store) {
        return new CheckerJob(servicesRetriver, credentialsRetriver, store, serviceTypes);
    }

    @Bean(initMethod = "start")
    public CheckerScheduler checkerScheduler(CheckerJob checkerJob) {
        return new CheckerScheduler(checkerJob, checkerTriggerExpression);
    }
}
