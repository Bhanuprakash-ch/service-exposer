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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.cloud.cc.api.CcExtendedServiceInstance;
import org.trustedanalytics.cloud.cc.api.CcNewServiceKey;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.CcServiceKey;
import org.trustedanalytics.serviceexposer.keyvaluestore.CredentialProperties;
import org.trustedanalytics.serviceexposer.keyvaluestore.CredentialsStore;
import org.trustedanalytics.serviceexposer.queue.MessagingQueue;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CredentialsRetriver {

    private static final Logger LOG = LoggerFactory.getLogger(CredentialsRetriver.class);

    private CcOperations ccClient;
    private CredentialsStore<CredentialProperties> store;
    private MessagingQueue natsOps;
    private String apiBaseUrl;

    public CredentialsRetriver(CcOperations ccClient, CredentialsStore<CredentialProperties> store, MessagingQueue natsOps, String apiBaseUrl) {
        this.ccClient = ccClient;
        this.store = store;
        this.natsOps = natsOps;
        this.apiBaseUrl = apiBaseUrl;
    }

    public void saveCredentialsUsingEnvs(String serviceType, CcExtendedServiceInstance serviceInstance) {
        UUID serviceInstanceGuid = serviceInstance.getMetadata().getGuid();
        try {
            if (!store.exists(serviceType, serviceInstanceGuid)) {
                LOG.info("detected creation of service instance : " + serviceInstanceGuid);

                CcServiceKey serviceInstanceKey = prepareServiceKey(serviceInstance);
                UUID serviceInstanceKeyGuid = serviceInstanceKey.getMetadata().getGuid();
                LOG.info("service key prepared: " + serviceInstanceKeyGuid);

                CredentialProperties credentials = parseCredentials(serviceInstance, serviceInstanceKey);
                LOG.info("service credentials retrieved from key: " + serviceInstanceKeyGuid);

                ccClient.deleteServiceKey(serviceInstanceKeyGuid);
                LOG.info("service key deleted: " + serviceInstanceKeyGuid);

                store.put(serviceType, serviceInstanceGuid, credentials);
                natsOps.registerPathInGoRouter(credentials);
            }
        } catch (Exception e) {
            LOG.error("failed to get credentials from service instance: " + serviceInstanceGuid);
            LOG.error(e.getMessage(), e);
        }
    }

    private CcServiceKey prepareServiceKey(CcExtendedServiceInstance serviceInstance) {
        UUID instanceGuid = serviceInstance.getMetadata().getGuid();
        CcServiceKey existingKey = ccClient.getServiceKeys()
                .filter(k -> k.getEntity().getName().contains(instanceGuid.toString()))
                .firstOrDefault(null)
                .toBlocking()
                .first();

        return Optional
                .ofNullable(existingKey)
                .orElseGet(() -> ccClient.createServiceKey(new CcNewServiceKey(instanceGuid, instanceGuid + "-key"))
                        .toBlocking()
                        .first());
    }

    private CredentialProperties parseCredentials(CcExtendedServiceInstance serviceInstance, CcServiceKey serviceInstanceKey) {
        Map<String, String> credentials = (Map<String, String>) serviceInstanceKey.getEntity().getCredentials();
        String domainName = apiBaseUrl.split("api")[1];
        String instanceGuid = serviceInstance.getMetadata().getGuid().toString();
        String spaceGuid = serviceInstance.getEntity().getSpaceGuid().toString();
        String serviceName = serviceInstance.getEntity().getName();
        String ipAddress = Optional.ofNullable(credentials.get("hostname")).orElse("");
        String port = Optional.ofNullable(credentials.get("port")).orElse("");
        String externalUrl = Optional.ofNullable(credentials.get("dashboardUrl")).orElse("");
        String username = Optional.ofNullable(credentials.get("username")).orElse("");
        String password = Optional.ofNullable(credentials.get("password")).orElse("");
        return new CredentialProperties(domainName, instanceGuid, spaceGuid, serviceName, ipAddress, port, externalUrl, username, password);
    }

    public void deleteServiceInstance(String serviceType, UUID serviceInstanceGuid) {
        try {
            if (store.exists(serviceType, serviceInstanceGuid)) {
                LOG.info("detected deletion of service instance: " + serviceInstanceGuid);
                CredentialProperties serviceInfo = store.get(serviceType, serviceInstanceGuid);
                store.delete(serviceType, serviceInstanceGuid);
                natsOps.unregisterPathInGoRouter(serviceInfo);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}

