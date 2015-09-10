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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.serviceexposer.cloud.CredentialProperties;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class CustomCFOperations {

    private static final Logger LOG = LoggerFactory.getLogger(CustomCFOperations.class);

    private String apiBaseUrl;
    private RestTemplate restTemplate;
    private ObjectMapper mapper;

    public CustomCFOperations(OAuth2RestTemplate clientRestTemplate, String apiBaseUrl) {
        this.restTemplate = clientRestTemplate;
        this.apiBaseUrl = apiBaseUrl;
        this.mapper = new ObjectMapper();
    }

    public UUID getSpaceGUID(UUID serviceGuidGUID) {
        try {
            String serviceInfoPath = apiBaseUrl + "/v2/service_instances/" + serviceGuidGUID.toString();
            HttpEntity<String> bindingResponse = restTemplate.exchange(serviceInfoPath, HttpMethod.GET, new HttpEntity<>(""), String.class, "");
            JsonNode serviceRootNode = mapper.readTree(bindingResponse.getBody());
            JsonNode serviceEntity = serviceRootNode.get("entity");
            JsonNode spaceNode = serviceEntity.get("space_guid");
            String spaceGuidText = spaceNode.asText();
            return UUID.fromString(spaceGuidText);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public  Set<String>  getServicesForGivenPlan(String planGuidGUID) {
        try {
            String serviceInfoPath = apiBaseUrl + "/v2/service_instances?q=service_plan_guid:" + planGuidGUID;
            HttpEntity<String> bindingResponse = restTemplate.exchange(serviceInfoPath, HttpMethod.GET, new HttpEntity<>(""), String.class, "");
            JsonNode serviceRootNode = mapper.readTree(bindingResponse.getBody());
            JsonNode resourcesNode = serviceRootNode.get("resources");

            Set<String> serviceGuids = new HashSet<>();
            for(JsonNode node : resourcesNode ){
                serviceGuids.add(node.get("metadata").get("guid").asText());
            }
            return serviceGuids;

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }


    public String getServiceName(UUID serviceGuidGUID) {
        try {
            String serviceInfoPath = apiBaseUrl + "/v2/service_instances/" + serviceGuidGUID.toString();
            HttpEntity<String> bindingResponse = restTemplate.exchange(serviceInfoPath, HttpMethod.GET, new HttpEntity<>(""), String.class, "");
            JsonNode serviceRootNode = mapper.readTree(bindingResponse.getBody());
            JsonNode serviceEntity = serviceRootNode.get("entity");
            JsonNode spaceNode = serviceEntity.get("name");
            String serviceName = spaceNode.asText();
            return serviceName;

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }


    public boolean appExistsInGivenSpace(String appName, UUID spaceGUID) {
        try {
            String appsListPath = apiBaseUrl + "/v2/apps?q=name:" + appName +"&q=space_guid:" + spaceGUID.toString();
            HttpEntity<String> bindingResponse = restTemplate.exchange(appsListPath, HttpMethod.GET, new HttpEntity<>(""), String.class, "");
            JsonNode envNode = mapper.readTree(bindingResponse.getBody());
            return envNode.get("total_results").asInt() == 1 ? true : false;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return true;
    }

    public UUID createAppInGivenSpace(String appName, UUID spaceGUID){
        try {
            String appsListPath = apiBaseUrl + "/v2/apps";
            ObjectNode node = mapper.getNodeFactory().objectNode();
            node.put("name", appName);
            node.put("space_guid", spaceGUID.toString());
            HttpEntity<String> appCrateResponse = restTemplate.exchange(appsListPath, HttpMethod.POST, new HttpEntity<>(node.toString()), String.class, "");
            JsonNode appRootNode = mapper.readTree(appCrateResponse.getBody());
            JsonNode appMetadata = appRootNode.get("metadata");
            JsonNode appGuidNode = appMetadata.get("guid");
            String appGuidText = appGuidNode.asText();
            return UUID.fromString(appGuidText);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public CredentialProperties getCredentialsFromApp(String serviceType, UUID appGuid, UUID serviceGUID, UUID spaceGuid) {
        try {
            String envPath = apiBaseUrl + "/v2/apps/" + appGuid.toString() + "/env";
            HttpEntity<String> envRes = restTemplate.exchange(envPath, HttpMethod.GET, new HttpEntity<>(""), String.class, "");
            JsonNode envNode = mapper.readTree(envRes.getBody());
            JsonNode servicesNode = envNode.get("system_env_json").get("VCAP_SERVICES").get(serviceType);
            JsonNode firstServiceInstancesNode = servicesNode.elements().next();
            String serviceName = firstServiceInstancesNode.get("name").asText();
            JsonNode credentialsNode = firstServiceInstancesNode.get("credentials");
            String ipAddress = credentialsNode.get("hostname").asText();
            String portNumber = credentialsNode.get("port").asText();
            String password = credentialsNode.get("password").asText();
            String username = serviceType.equals("ipython") ? "" : credentialsNode.get("username").asText();
            String domainName = apiBaseUrl.split("api")[1];
            CredentialProperties serviceInfo = new CredentialProperties(domainName, serviceGUID.toString(), spaceGuid.toString(), serviceName, ipAddress, portNumber, username, password);
            return serviceInfo;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public CredentialProperties getCredentialsUsingServiceKeys(String serviceType, String serviceName, UUID spaceGUID, UUID serviceGUID) {
        try {
            String appsListPath = apiBaseUrl + "/v2/service_keys";
            String serviceKeysName = UUID.randomUUID() + "-keys";
            JsonNode responseNode = createServiceKeys(serviceGUID, serviceKeysName);
            String serviceKeysGUID = responseNode.get("metadata").get("guid").asText();
            JsonNode serviceEntity = responseNode.get("entity");
            JsonNode credentialsNode = serviceEntity.get("credentials");
            String ipAddress = credentialsNode.get("hostname").asText();
            String portNumber = credentialsNode.get("port").asText();
            String password = credentialsNode.get("password").asText();
            String username = serviceType.equals("ipython") ? "" : credentialsNode.get("username").asText();
            String domainName = apiBaseUrl.split("api")[1];
            CredentialProperties serviceInfo = new CredentialProperties(domainName, serviceGUID.toString(), spaceGUID.toString(), serviceName, ipAddress, portNumber, username, password);
            deleteServiceKeys(serviceKeysGUID);
            return serviceInfo;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public JsonNode createServiceKeys(UUID serviceGUID,String serviceKeysName){
        try {
            String appsListPath = apiBaseUrl + "/v2/service_keys";
            ObjectNode node = mapper.getNodeFactory().objectNode();
            node.put("service_instance_guid", serviceGUID.toString());
            node.put("name", serviceKeysName);
            HttpEntity<String> serviceKeyCreateResponse = restTemplate.exchange(appsListPath, HttpMethod.POST, new HttpEntity<>(node.toString()), String.class, "");
            JsonNode serviceRootNode = mapper.readTree(serviceKeyCreateResponse.getBody());
            return serviceRootNode;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public void deleteServiceKeys(String serviceKeysGUID) {
        try {
            String appsListPath = apiBaseUrl + "/v2/service_keys/" + serviceKeysGUID.toString();
            HttpEntity<String> serviceKeyCreateResponse = restTemplate.exchange(appsListPath, HttpMethod.DELETE, new HttpEntity<>(""), String.class, "");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
