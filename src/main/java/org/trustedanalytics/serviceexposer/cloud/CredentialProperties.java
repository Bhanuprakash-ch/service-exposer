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

import com.google.common.collect.ImmutableMap;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class CredentialProperties {

    private String serviceInstaceGUID;
    private String spaceGuid;
    private String name;
    private String hostname;
    private String ipaddress;
    private String port;
    private String login;
    private String password;

    public CredentialProperties(String domainName, String serviceInstaceGUID, String spaceGuid, String name, String IPAdress, String PortNumber, String login, String password) {
        this.serviceInstaceGUID = serviceInstaceGUID;
        this.spaceGuid = spaceGuid;
        this.name = name;
        this.hostname = name.replaceAll("[^A-Za-z0-9]", "_")+"-"+serviceInstaceGUID + domainName;
        this.ipaddress = IPAdress;
        this.port = PortNumber;
        this.login = login;
        this.password = password;
    }

    public CredentialProperties() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, serviceInstaceGUID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CredentialProperties other = (CredentialProperties) obj;
        return Objects.equals(hostname, other.hostname) && Objects.equals(serviceInstaceGUID, other.serviceInstaceGUID);
    }

    @Override
    public String toString() {
        return "Service Instance [serviceInstaceGUID=" + serviceInstaceGUID + ", hostname=" + hostname + ", login=" + login
                + "]";
    }

    public Map<String, String> retriveMapForm() {

        return ImmutableMap.
                of("guid", serviceInstaceGUID,
                        "hostname", hostname,
                        "login", login,
                        "password", password);

    }

    public String retrieveRegisterMsg() {
        String registerMsg = "{\"host\":\"" + ipaddress + "\",\"port\":" + port + ",\"uris\":[\"" + hostname + "\"]}";
        return registerMsg;
    }
}
