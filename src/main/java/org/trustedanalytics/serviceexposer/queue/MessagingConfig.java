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

package org.trustedanalytics.serviceexposer.queue;

import nats.client.Nats;
import nats.client.spring.NatsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


public class MessagingConfig {

    @Profile("cloud")
    @Configuration
    public static class NatsMessagingConfig {

        @Value("${nats.connection}")
        private String natsUri;

        @Bean
        Nats nats(ApplicationEventPublisher applicationEventPublisher) {
            return new NatsBuilder(applicationEventPublisher)
                    .addHost(natsUri)
                    .connect();
        }

        @Bean
        MessagingQueue natsQueueOps(Nats nats) {
            return new NatsMessagingQueue(nats);
        }
    }

    @Profile("in-memory")
    @Configuration
    public static class InMemoryMessagingConfig {

        @Bean
        MessagingQueue InMemoryQueueOps() {
            return new InMemoryMessagingQueue();
        }
    }
}