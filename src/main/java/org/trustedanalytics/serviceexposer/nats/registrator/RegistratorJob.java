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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.serviceexposer.cloud.CredentialProperties;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;

import java.util.List;

public class RegistratorJob {

    private static final Logger LOG = LoggerFactory.getLogger(RegistratorJob.class);
    private static final String NATS_ROUTE_REGISTER = "router.register";

    private NatsMessagingQueue natsOps;
    private CredentialsStore store;
    private List<String> serviceTypes;

    public RegistratorJob(NatsMessagingQueue natsOps, CredentialsStore store, List<String> serviceTypes) {
        this.natsOps = natsOps;
        this.store = store;
        this.serviceTypes = serviceTypes;
    }

    public void run() {
        for (String serviceType : serviceTypes) {
            for (CredentialProperties entry : store.getAllCredentialsEntries(serviceType)) {
                natsOps.registerPathInGoRouter(entry);
            }
        }
    }
}
