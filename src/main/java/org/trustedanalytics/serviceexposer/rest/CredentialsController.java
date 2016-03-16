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
package org.trustedanalytics.serviceexposer.rest;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.serviceexposer.keyvaluestore.CredentialProperties;
import org.trustedanalytics.serviceexposer.keyvaluestore.CredentialsStore;
import rx.Observable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class CredentialsController {

    private static final Logger LOG = LoggerFactory.getLogger(CredentialsController.class);
    public static final String GET_SERVICES_LIST_URL = "/rest/tools/service_instances";
    public static final String GET_CREDENTIALS_LIST_FOR_ORG_URL = "/rest/credentials/organizations/{org}";

    private final CcOperations ccOperations;
    private final CredentialsStore<CredentialProperties> store;

    @Autowired
    public CredentialsController(@Qualifier("ControllerClient") CcOperations ccOperations, CredentialsStore<CredentialProperties> store) {
        this.ccOperations = ccOperations;
        this.store = store;
    }

    @ApiOperation(
            value = "Returns list of all service instance credentials of given type for given space.",
            notes = "Privilege level: Consumer of this endpoint must be a member of specified space based on valid access token"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ResponseEntity.class),
            @ApiResponse(code = 401, message = "User is Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error, see logs for details")
    })
    @RequestMapping(value = GET_SERVICES_LIST_URL, method = GET, produces = APPLICATION_JSON_VALUE)

    public ResponseEntity<?> getAllCredentials(
            @RequestParam(required = true) UUID space,
            @RequestParam(required = true) String service) {
        return ccOperations.getSpace(space)
                .map(s -> new ResponseEntity<>(getCredentialsInJson(service, s.getGuid()), HttpStatus.OK))
                .onErrorReturn(er -> {
                    LOG.error("Exception occurred:", er);
                    return new ResponseEntity<>(Collections.emptyMap(), HttpStatus.UNAUTHORIZED);
                })
                .toBlocking()
                .single();
    }

    @ApiOperation(
            value = "Returns list of all service instance credentials of given type for given organization.",
            notes = "Privilege level: Consumer of this endpoint must be a member of specified organization based on valid access token"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ResponseEntity.class),
            @ApiResponse(code = 401, message = "User is Unauthorized"),
            @ApiResponse(code = 500, message = "Internal server error, see logs for details")
    })
    @RequestMapping(value = GET_CREDENTIALS_LIST_FOR_ORG_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllCredentialsInOrg(
            @PathVariable UUID org,
            @RequestParam(required = true) String service) {
        return ccOperations.getSpaces(org)
                .map(s -> getCredentialsInJson(service, s.getGuid()))
                .flatMap(json -> Observable.from(getFlattenedCredentials(json)))
                .toList()
                .map(instances -> new ResponseEntity<>(instances, HttpStatus.OK))
                .onErrorReturn(er -> {
                    LOG.error("Exception occurred:", er);
                    return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
                })
                .toBlocking()
                .single();
    }

    private static Collection<Map<String, String>> getFlattenedCredentials(Map<String, Map<String, String>> instances) {
        return instances.entrySet().stream()
                .map(entry -> ImmutableMap.<String, String>builder()
                        .putAll(entry.getValue())
                        .put("name", entry.getKey())
                        .build())
                .collect(Collectors.toList());
    }

    public Map<String, Map<String, String>> getCredentialsInJson(String serviceType, UUID spaceGuid) {
        try {
            return store.values(serviceType).stream().
                    filter(s -> s.getSpaceGuid().equals(spaceGuid.toString())).
                    collect(toMap(CredentialProperties::getName, CredentialProperties::retriveMapForm));

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}
