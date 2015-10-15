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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class CredentialsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialsController.class);
    public static final String GET_SERVICES_LIST_URL = "/rest/tools/service_instances";

    private final CcOperations ccOperations;
    private final CredentialsStore store;

    @Autowired
    public CredentialsController(@Qualifier("ControllerClient") CcOperations ccOperations, CredentialsStore store) {
        this.ccOperations = ccOperations;
        this.store = store;
    }

    @RequestMapping(value = GET_SERVICES_LIST_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllCredentials(@RequestParam(required = true) UUID space, @RequestParam(required = true) String service) {

        List<UUID> spaces = new ArrayList<>();
        for (CcSpace currentSpace : ccOperations.getSpaces().toBlocking().toIterable()) {
            spaces.add(currentSpace.getGuid());
        }

        if(spaces.contains(space)){
            Map<String, Map<String, String>> r = store.getCredentialsInJSON(service, space);
            return new ResponseEntity<Map<String, Map<String, String>>>(r, HttpStatus.OK);
        }else{
            return new ResponseEntity<>("", HttpStatus.UNAUTHORIZED);
        }
    }
}
