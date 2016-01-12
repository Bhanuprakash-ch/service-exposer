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
package org.trustedanalytics.serviceexposer.nats.registrator;

import nats.client.Nats;
import nats.client.spring.NatsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.serviceexposer.cloud.CredentialProperties;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;
import org.trustedanalytics.serviceexposer.nats.registrator.externaltools.ExternalTool;
import org.trustedanalytics.serviceexposer.nats.registrator.externaltools.ExternalTools;

import java.util.List;
import java.util.Vector;

@Configuration
public class RegistratorConfig {

    @Value("${nats.connection}")
    private String natsUri;

    @Value("${nats.registrating.triggerExpression}")
    private String natsTriggerExpression;

    @Value("#{'${serviceTypes}'.split(',')}")
    private List<String> serviceTypes;

    @Autowired
    private ExternalTools externalTools;

    @Bean
    public Nats nats(ApplicationEventPublisher applicationEventPublisher) {
        return new NatsBuilder(applicationEventPublisher)
                .addHost(natsUri)
                .connect();
    }

    @Bean
    protected NatsMessagingQueue natsOps(Nats nats) {
        return new NatsMessagingQueue(nats);
    }

    @Bean
    protected List<CredentialProperties> visualisationToolsCredentials() {
        List<CredentialProperties> visualisationTools = new Vector<>();
        for (ExternalTool entry : externalTools.getVisualizations()) {
            if (entry.isAvailable() && !entry.getHost().contains("None")) {
                String[] ipAndPort = entry.getHost().split(":");
                String url = entry.getUrl().split("/")[2];
                visualisationTools.add(new CredentialProperties("", "", "", "", ipAndPort[0], ipAndPort[1], url, "", ""));
            }
        }
        return visualisationTools;
    }


    @Bean
    public RegistratorJob registratorJob(NatsMessagingQueue nats, CredentialsStore store) {
        return new RegistratorJob(nats, store, serviceTypes, visualisationToolsCredentials());
    }

    @Bean(initMethod = "start")
    public RegistratorScheduler registratorScheduler(RegistratorJob registratorJob) {
        return new RegistratorScheduler(registratorJob, natsTriggerExpression);
    }
}
