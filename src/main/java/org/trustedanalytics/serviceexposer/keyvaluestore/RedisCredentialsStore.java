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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RedisCredentialsStore<T> implements CredentialsStore<T> {

    private static final Logger LOG = LoggerFactory.getLogger(RedisCredentialsStore.class);

    private final HashOperations<String, String, T> hashOps;

    public RedisCredentialsStore(RedisOperations<String, T> template) {
        this.hashOps = template.opsForHash();
    }

    @Override
    public void put(String serviceType, UUID serviceInstanceGuid, T code) {
        hashOps.put(serviceType, serviceInstanceGuid.toString(), code);
        LOG.info("redis entry saved: " + serviceInstanceGuid);
    }

    @Override
    public void delete(String serviceType, UUID serviceInstanceGuid) {
        hashOps.delete(serviceType, serviceInstanceGuid.toString());
        LOG.info("redis entry deleted: " + serviceInstanceGuid);
    }

    @Override
    public T get(String serviceType, UUID serviceInstanceGuid) {
        return hashOps.get(serviceType, serviceInstanceGuid.toString());
    }

    @Override
    public Boolean exists(String serviceType, UUID serviceInstanceGuid) {
        T hashEntry = hashOps.get(serviceType, serviceInstanceGuid.toString());
        return (hashEntry != null) ? true : false;
    }

    @Override
    public Set<String> getSurplusServicesGuids(String serviceType, Set<String> servicesGuids) {
        Set<String> serviceInstancesToDeleted = this.hashOps.keys(serviceType);
        serviceInstancesToDeleted.removeAll(servicesGuids);
        return serviceInstancesToDeleted;
    }

    @Override
    public List<T> values(String serviceType) {
        return hashOps.values(serviceType);
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
