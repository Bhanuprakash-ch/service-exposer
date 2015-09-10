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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class CredentialsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialsController.class);
    public static final String GET_SERVICES_LIST_URL = "/rest/tools/service_instances";

    private final CredentialsStore store;

    @Autowired
    public CredentialsController(CredentialsStore store) {
        this.store = store;
    }

    @RequestMapping(value = GET_SERVICES_LIST_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Map<String, Map<String, String>> getAllCredentials(@RequestParam(required = false) UUID org, @RequestParam(required = false) UUID space,@RequestParam(required = false) String service) {
        return store.getCredentialsInJSON(service, space);
    }
}
