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

    public UUID getSpaceGUID(UUID serviceGuidGUID) throws Exception {
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
            throw e;
        }
    }

    public UUID getAppGUIDFromGivenSpace(String appName, UUID spaceGUID) {
        try {
            String appsListPath = apiBaseUrl + "/v2/apps?q=name:" + appName + "&q=space_guid:" + spaceGUID.toString();
            HttpEntity<String> bindingResponse = restTemplate.exchange(appsListPath, HttpMethod.GET, new HttpEntity<>(""), String.class, "");
            JsonNode envNode = mapper.readTree(bindingResponse.getBody());
            boolean appExists = envNode.get("total_results").asInt() == 1 ? true : false;
            if (appExists) {
                for (JsonNode node : envNode.get("resources")) {
                    return UUID.fromString(node.get("metadata").get("guid").asText());
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public UUID createAppInGivenSpace(String appName, UUID spaceGUID) {
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
            LOG.info("Created temporary app : " + appGuidText);
            return UUID.fromString(appGuidText);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }
}
