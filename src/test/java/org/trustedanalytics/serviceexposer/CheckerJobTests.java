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
package org.trustedanalytics.routermetrics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;
import org.trustedanalytics.serviceexposer.checker.CheckerJob;
import org.trustedanalytics.serviceexposer.retriver.CredentialsRetriver;
import org.trustedanalytics.serviceexposer.retriver.ServicesRetriver;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckerJobTests {

    private CheckerJob sut;

    private static final String SERVICE_TYPE_RSTUDIO = "rstudio";
    private static final String SERVICE_TYPE_IPYTHON= "ipython";

    @Mock
    private ServicesRetriver servicesRetriver;
    @Mock
    private CredentialsRetriver credentialsRetriver;
    @Mock
    private CredentialsStore credentialsStore;

    private List<String> serviceTypes;

    @Before
    public void setup() {
        serviceTypes = ImmutableList.of(SERVICE_TYPE_RSTUDIO,SERVICE_TYPE_IPYTHON);
        sut = new CheckerJob(servicesRetriver, credentialsRetriver, credentialsStore,serviceTypes);
    }

    @Test
    public void testCheckerJobRunMethodForIPythonServices() {

        String[] guids = {UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()};

        Set<String> rstudioGUIDS = Sets.newHashSet(guids);

        when(servicesRetriver.getServiceInstances("ipython")).thenReturn(rstudioGUIDS);

        sut.run();

        verify(credentialsRetriver).saveCredentialsUsingEnvs("ipython", UUID.fromString(guids[0]));
        verify(credentialsRetriver).saveCredentialsUsingEnvs("ipython", UUID.fromString(guids[1]));
        verify(credentialsRetriver).saveCredentialsUsingEnvs("ipython", UUID.fromString(guids[2]));
    }

    @Test
    public void testCheckerJobUdpateDeletedRStudioInstances() {

        String[] guids = {UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()};

        Set<String> surplusGUIDS = Sets.newHashSet(guids);
        Set<String> retrievedGUIDS = new HashSet<>();
        when(credentialsStore.getSurplusServicesGUIDs(SERVICE_TYPE_RSTUDIO, retrievedGUIDS)).thenReturn(surplusGUIDS);

        sut.updateDeletedServiceInstances(SERVICE_TYPE_RSTUDIO, retrievedGUIDS);

        verify(credentialsRetriver).deleteServiceInstance(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[0]));
        verify(credentialsRetriver).deleteServiceInstance(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[1]));
        verify(credentialsRetriver).deleteServiceInstance(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[2]));
    }

    @Test
    public void testCheckerJobUdpateCreatedRStudioInstances() {

        String[] guids = {UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()};

        Set<String> retrievedGUIDS = Sets.newHashSet(guids);

        sut.updateCreatedServiceInstances(SERVICE_TYPE_RSTUDIO, retrievedGUIDS);

        verify(credentialsRetriver).saveCredentialsUsingEnvs(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[0]));
        verify(credentialsRetriver).saveCredentialsUsingEnvs(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[1]));
        verify(credentialsRetriver).saveCredentialsUsingEnvs(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[2]));
    }
}
