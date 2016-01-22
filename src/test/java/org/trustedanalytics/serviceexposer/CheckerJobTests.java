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
import org.trustedanalytics.cloud.cc.api.CcExtendedServiceInstance;
import org.trustedanalytics.cloud.cc.api.CcMetadata;
import org.trustedanalytics.serviceexposer.checker.CheckerJob;
import org.trustedanalytics.serviceexposer.cloud.CredentialsStore;
import org.trustedanalytics.serviceexposer.retriver.CredentialsRetriver;
import org.trustedanalytics.serviceexposer.retriver.ServicesRetriver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

        UUID[] guids = {UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()};


        Set<CcExtendedServiceInstance> rstudioGuids = new HashSet<>();

        CcExtendedServiceInstance s1 = new CcExtendedServiceInstance();
        CcExtendedServiceInstance s2 = new CcExtendedServiceInstance();
        CcExtendedServiceInstance s3 = new CcExtendedServiceInstance();

        s1.setMetadata(new CcMetadata());
        s2.setMetadata(new CcMetadata());
        s3.setMetadata(new CcMetadata());

        s1.getMetadata().setGuid(guids[0]);
        s2.getMetadata().setGuid(guids[1]);
        s3.getMetadata().setGuid(guids[2]);

        rstudioGuids.add(s1);
        rstudioGuids.add(s2);
        rstudioGuids.add(s3);

        when(servicesRetriver.getServiceInstances("ipython")).thenReturn(rstudioGuids);

        sut.run();

        verify(credentialsRetriver).saveCredentialsUsingEnvs("ipython", s1);
        verify(credentialsRetriver).saveCredentialsUsingEnvs("ipython", s2);
        verify(credentialsRetriver).saveCredentialsUsingEnvs("ipython", s3);

    }

    @Test
    public void testCheckerJobUdpateDeletedRStudioInstances() {

        String[] guids = {UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()};

        Set<String> surplusGuids = Sets.newHashSet(guids);
        Set<String> serviceInstancessGuids = Sets.newHashSet();

        Set<CcExtendedServiceInstance> retrievedGuids = new HashSet<>();

        when(credentialsStore.getSurplusServicesGuids(SERVICE_TYPE_RSTUDIO, serviceInstancessGuids)).thenReturn(surplusGuids);

        sut.updateDeletedServiceInstances(SERVICE_TYPE_RSTUDIO, retrievedGuids);

        verify(credentialsRetriver).deleteServiceInstance(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[0]));
        verify(credentialsRetriver).deleteServiceInstance(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[1]));
        verify(credentialsRetriver).deleteServiceInstance(SERVICE_TYPE_RSTUDIO, UUID.fromString(guids[2]));
    }

    @Test
    public void testCheckerJobUdpateCreatedRStudioInstances() {

        UUID[] guids = {UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()};

        Set<CcExtendedServiceInstance> rstudioGuids = new HashSet<>();

        CcExtendedServiceInstance s1 = new CcExtendedServiceInstance();
        CcExtendedServiceInstance s2 = new CcExtendedServiceInstance();
        CcExtendedServiceInstance s3 = new CcExtendedServiceInstance();

        s1.setMetadata(new CcMetadata());
        s2.setMetadata(new CcMetadata());
        s3.setMetadata(new CcMetadata());

        s1.getMetadata().setGuid(guids[0]);
        s2.getMetadata().setGuid(guids[1]);
        s3.getMetadata().setGuid(guids[2]);

        rstudioGuids.add(s1);
        rstudioGuids.add(s2);
        rstudioGuids.add(s3);

        sut.updateCreatedServiceInstances(SERVICE_TYPE_RSTUDIO, rstudioGuids);

        verify(credentialsRetriver).saveCredentialsUsingEnvs(SERVICE_TYPE_RSTUDIO, s1);
        verify(credentialsRetriver).saveCredentialsUsingEnvs(SERVICE_TYPE_RSTUDIO, s2);
        verify(credentialsRetriver).saveCredentialsUsingEnvs(SERVICE_TYPE_RSTUDIO, s3);
    }
}
