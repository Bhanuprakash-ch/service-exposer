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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface CredentialsStore {

    void put(String serviceType, CredentialProperties code);

    void delete(String serviceType, UUID serviceInstanceGuid);

    Boolean exists(String serviceType, UUID serviceInstanceGuid);

    CredentialProperties get(String serviceType, UUID serviceInstanceGuid);

    Set<String> getSurplusServicesGuids(String serviceType, Set<String> retrievedServiceGuids);

    List<CredentialProperties> getAllCredentialsEntries(String serviceType);

    Map<String, Map<String, String>> getCredentialsInJson(String serviceType, UUID spaceGuid);

    void cleanStore(String serviceType);
}
