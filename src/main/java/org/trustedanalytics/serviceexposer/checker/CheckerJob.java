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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;
import org.trustedanalytics.serviceexposer.retriver.CredentialsRetriver;
import org.trustedanalytics.serviceexposer.retriver.ServicesRetriver;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CheckerJob {

    private static final Logger LOG = LoggerFactory.getLogger(CheckerJob.class);

    private final CredentialsStore credentialsStore;
    private final CredentialsRetriver credentialsRetriver;
    private final ServicesRetriver servicesRetriver;
    private final List<String> serviceTypes;

    @Autowired
    public CheckerJob(ServicesRetriver servicesRetriver,CredentialsRetriver credentialsRetriver, CredentialsStore store, List<String> serviceTypes) {
        this.servicesRetriver = servicesRetriver;
        this.credentialsRetriver = credentialsRetriver;
        this.credentialsStore = store;
        this.serviceTypes = serviceTypes;
    }

    public void run() {
        for (String serviceType : serviceTypes) {
            Set<String> servicesGUIDS = servicesRetriver.getServiceInstances(serviceType);
            updateCreatedServiceInstances(serviceType, servicesGUIDS);
            updateDeletedServiceInstances(serviceType, servicesGUIDS);
        }
        LOG.info("Checking services finished");
    }

    public void updateCreatedServiceInstances(String serviceType, Set<String> servicesGUIDS) {
        for (String serviceGUID : servicesGUIDS) {
            credentialsRetriver.saveCredentialsUsingEnvs(serviceType, UUID.fromString(serviceGUID));
        }
    }

    public void updateDeletedServiceInstances(String serviceType, Set<String> servicesGUIDS) {
        for (String serviceGUID : credentialsStore.getSurplusServicesGUIDs(serviceType, servicesGUIDS)) {
            credentialsRetriver.deleteServiceInstance(serviceType, UUID.fromString(serviceGUID));
        }
    }
}