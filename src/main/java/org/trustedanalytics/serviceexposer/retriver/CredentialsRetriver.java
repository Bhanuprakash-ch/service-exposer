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

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trustedanalytics.cloud.cc.api.CcAppEnv;
import org.trustedanalytics.cloud.cc.api.CcNewServiceBinding;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.CcServiceBinding;
import org.trustedanalytics.serviceexposer.cloud.CredentialProperties;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;
import org.trustedanalytics.serviceexposer.nats.registrator.NatsMessagingQueue;

import java.util.UUID;

public class CredentialsRetriver {

    private static final Logger LOG = LoggerFactory.getLogger(CredentialsRetriver.class);

    private CcOperations ccClient;
    private CredentialsStore store;
    private NatsMessagingQueue natsOps;
    private CustomCFOperations customCFOps;
    private String apiBaseUrl;

    public CredentialsRetriver(CcOperations ccClient, CustomCFOperations cfOps, CredentialsStore store, NatsMessagingQueue natsOps, String apiBaseUrl) {
        this.ccClient = ccClient;
        this.customCFOps = cfOps;
        this.store = store;
        this.natsOps = natsOps;
        this.apiBaseUrl = apiBaseUrl;
    }

    public void saveCredentialsUsingEnvs(String serviceType, UUID serviceInstanceGUID) {
        try {
            if (!store.exists(serviceType, serviceInstanceGUID)) {
                LOG.info("service instance created: " + serviceInstanceGUID);
                UUID spaceGUID = customCFOps.getSpaceGUID(serviceInstanceGUID);
                String appName = serviceType + "-credentials-generator";

                if (!customCFOps.appExistsInGivenSpace(appName, spaceGUID)) {
                    UUID appGUID = customCFOps.createAppInGivenSpace(appName, spaceGUID);
                    CcNewServiceBinding binding = new CcNewServiceBinding(appGUID, serviceInstanceGUID);
                    CcServiceBinding bindingGuid = ccClient.createServiceBinding(binding);
                    CredentialProperties serviceCredentials = getCredentialsFromApp(serviceType, appGUID, serviceInstanceGUID, spaceGUID);
                    ccClient.deleteServiceBinding(bindingGuid.getMetadata().getGuid());
                    ccClient.deleteApp(appGUID);
                    store.put(serviceType, serviceCredentials);
                    natsOps.registerPathInGoRouter(serviceCredentials);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CredentialProperties getCredentialsFromApp(String serviceType, UUID appGUID, UUID serviceGUID, UUID spaceGuid) {
        try {
            CcAppEnv env = ccClient.getAppEnv(appGUID).toBlocking().single();
            String filter = "$..[?(@.label==\'" + serviceType + "\')]";
            String serviceName = env.getValueByFilter(filter).findValue("name").asText();
            String ipAddress = env.findCredentialsPropertyByServiceLabel(serviceType, "hostname");
            String portNumber = env.findCredentialsPropertyByServiceLabel(serviceType, "port");
            String password = env.findCredentialsPropertyByServiceLabel(serviceType, "password");
            String username = serviceType.equals("ipython") ? "" : env.findCredentialsPropertyByServiceLabel(serviceType, "username");
            String domainName = apiBaseUrl.split("api")[1];
            CredentialProperties serviceInfo = new CredentialProperties(domainName, serviceGUID.toString(), spaceGuid.toString(), serviceName, ipAddress, portNumber, username, password);
            return serviceInfo;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public void deleteServiceInstance(String serviceType, UUID serviceInstanceGUID) {
        if (store.exists(serviceType, serviceInstanceGUID)) {
            LOG.info("service instance deleted: " + serviceInstanceGUID);
            CredentialProperties serviceInfo = store.get(serviceType, serviceInstanceGUID);
            store.delete(serviceType, serviceInstanceGUID);
            natsOps.unregisterPathInGoRouter(serviceInfo);
        }
    }

    private void saveCredentialsUsingServiceKeys(String serviceType, UUID serviceInstanceGUID) {
        if (!store.exists(serviceType, serviceInstanceGUID)) {
            LOG.info("service instance created: " + serviceInstanceGUID);
            String name = customCFOps.getServiceName(serviceInstanceGUID);
            UUID spaceGUID = customCFOps.getSpaceGUID(serviceInstanceGUID);
            CredentialProperties serviceCredentials = customCFOps.getCredentialsUsingServiceKeys(serviceType, name, spaceGUID, serviceInstanceGUID);
            store.put(serviceType, serviceCredentials);
            natsOps.registerPathInGoRouter(serviceCredentials);
        }
    }
}

