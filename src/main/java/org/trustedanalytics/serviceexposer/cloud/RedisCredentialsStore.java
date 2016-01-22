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
package org.trustedanalytics.serviceexposer.cloud;

import com.google.common.base.Functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class RedisCredentialsStore implements CredentialsStore {

    private static final Logger LOG = LoggerFactory.getLogger(RedisCredentialsStore.class);

    private final HashOperations<String, String, CredentialProperties> hashOps;

    public RedisCredentialsStore(RedisOperations<String, CredentialProperties> template) {
        this.hashOps = template.opsForHash();
    }

    @Override
    public void put(String serviceType, CredentialProperties code) {
        hashOps.put(serviceType, code.getServiceInstaceGuid(), code);
        LOG.info("redis entry saved: " + code.getName());
    }

    @Override
    public void delete(String serviceType, UUID serviceInstanceGuid) {
        hashOps.delete(serviceType, serviceInstanceGuid.toString());
        LOG.info("redis entry deleted: " + serviceInstanceGuid.toString());
    }

    @Override
    public CredentialProperties get(String serviceType, UUID serviceInstanceGuid) {
        return hashOps.get(serviceType, serviceInstanceGuid.toString());
    }

    @Override
    public Boolean exists(String serviceType, UUID serviceInstanceGuid) {
        CredentialProperties hashEntry = hashOps.get(serviceType, serviceInstanceGuid.toString());
        return (hashEntry != null) ? true : false;
    }

    @Override
    public Set<String> getSurplusServicesGuids(String serviceType, Set<String> servicesGuids) {
        Set<String> serviceInstancesToDeleted = this.hashOps.keys(serviceType);
        serviceInstancesToDeleted.removeAll(servicesGuids);
        return serviceInstancesToDeleted;
    }

    @Override
    public List<CredentialProperties> getAllCredentialsEntries(String serviceType) {
        return hashOps.values(serviceType);
    }

    @Override
    public Map<String, Map<String, String>> getCredentialsInJson(String serviceType, UUID spaceGuid) {
        try {

            return hashOps.values(serviceType).stream().
                    filter(s -> s.getSpaceGuid().equals(spaceGuid.toString())).
                    collect(toMap(CredentialProperties::getName, CredentialProperties::retriveMapForm));

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    @Override
    public void cleanStore(String serviceType) {
        try {
            for (String serviceGuid : hashOps.keys(serviceType)) {
                LOG.info("deleted " + serviceType + "\t" + serviceGuid, "");
                hashOps.delete(serviceType, serviceGuid);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
