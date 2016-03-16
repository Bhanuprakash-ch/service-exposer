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
package org.trustedanalytics.serviceexposer.keyvaluestore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InMemoryCredentialsStore<T> implements CredentialsStore<T> {

    private static final Logger LOG = LoggerFactory.getLogger(RedisCredentialsStore.class);

    final private Map<String, Map<String, T>> hashOps;

    public InMemoryCredentialsStore() {
        this.hashOps = new HashMap<String, Map<String, T>>();
    }

    @Override
    public void cleanStore(String serviceType) {
        try {
            if (hashOps.containsKey(serviceType)) {
                for (String serviceGuid : hashOps.get(serviceType).keySet()) {
                    LOG.info("deleted " + serviceType + "\t" + serviceGuid, "");
                    hashOps.get(serviceType).remove(serviceGuid);
                }
            } else {
                hashOps.put(serviceType, new HashMap<String, T>());
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void put(String serviceType, UUID serviceInstanceGuid, T code) {
        hashOps.get(serviceType).put(serviceInstanceGuid.toString(), code);
        LOG.info("in-memory redis entry saved: " + serviceInstanceGuid);
    }

    @Override
    public void delete(String serviceType, UUID serviceInstanceGuid) {
        hashOps.get(serviceType).remove(serviceInstanceGuid.toString());
        LOG.info("in-memory redis entry deleted: " + serviceInstanceGuid.toString());
    }

    @Override
    public Boolean exists(String serviceType, UUID serviceInstanceGuid) {
        T hashEntry = hashOps.get(serviceType).get(serviceInstanceGuid.toString());
        return (hashEntry != null) ? true : false;
    }

    @Override
    public T get(String serviceType, UUID serviceInstanceGuid) {
        return hashOps.get(serviceType).get(serviceInstanceGuid.toString());
    }

    @Override
    public Set<String> getSurplusServicesGuids(String serviceType, Set<String> retrievedServiceGuids) {
        Set<String> serviceInstancesToDeleted = new HashSet<String>(this.hashOps.get(serviceType).keySet());
        serviceInstancesToDeleted.removeAll(retrievedServiceGuids);
        return serviceInstancesToDeleted;
    }

    @Override
    public List<T> values(String serviceType) {
        return new ArrayList<T>(hashOps.get(serviceType).values());
    }
}