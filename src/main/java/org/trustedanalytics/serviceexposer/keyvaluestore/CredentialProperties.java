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

import com.google.common.collect.ImmutableMap;
import lombok.Data;

import java.util.Map;
import java.util.Objects;

@Data
public class CredentialProperties {

    private String serviceInstaceGuid;
    private String spaceGuid;
    private String name;
    private String hostName;
    private String ipAddress;
    private String port;
    private String login;
    private String password;
    private boolean credentialsExtracted;

    public CredentialProperties(boolean credentialsExtraced, String domainName, String instaceGuid, String spaceGuid, String name, String ipAdress, String portNumber, String hostName, String login, String password) {
        this.credentialsExtracted = credentialsExtraced;
        this.serviceInstaceGuid = instaceGuid;
        this.spaceGuid = spaceGuid;
        this.name = name;
        if ("".equals(hostName)) {
            String sanitizedServiceName = this.name.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$","");
            this.hostName = sanitizedServiceName + "-" + serviceInstaceGuid + domainName;
        } else {
            this.hostName = hostName;
        }
        this.ipAddress = ipAdress;
        this.port = portNumber;
        this.login = login;
        this.password = password;
    }

    public CredentialProperties() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostName, serviceInstaceGuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CredentialProperties other = (CredentialProperties) obj;
        return Objects.equals(hostName, other.hostName) && Objects.equals(serviceInstaceGuid, other.serviceInstaceGuid);
    }

    @Override
    public String toString() {
        return "{\"host\":\"" + ipAddress +
                "\",\"port\":" + port +
                ",\"uris\":[\"" + hostName + "\"]}";
    }

    public Map<String, String> retriveMapForm() {
        return ImmutableMap.
                of("guid", serviceInstaceGuid,
                        "hostname", hostName,
                        "login", login,
                        "password", password);

    }
}
