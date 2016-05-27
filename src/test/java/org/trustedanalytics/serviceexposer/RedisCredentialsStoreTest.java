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

package org.trustedanalytics.serviceexposer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.trustedanalytics.serviceexposer.keyvaluestore.CredentialProperties;
import org.trustedanalytics.serviceexposer.keyvaluestore.CredentialsStore;
import org.trustedanalytics.serviceexposer.keyvaluestore.RedisCredentialsStore;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RedisCredentialsStoreTest {

    private static final String SERVICE_TYPE = "rstudio";

    private CredentialsStore<CredentialProperties> sut;

    @Mock
    private RedisOperations<String, CredentialProperties> template;

    @Mock
    private CredentialProperties mockCredentialsProperties;

    @Mock
    private HashOperations<String, Object, Object> mockHashOps;

    @Before
    public void setUp() {
        when(template.opsForHash()).thenReturn(mockHashOps);
        sut = new RedisCredentialsStore<>(template);
    }

    @Test
    public void testGetSurplusKeys() {
        Set<Object> redisGuids = Sets.newHashSet("1", "2", "3", "4");
        Set<String> retrievedGuids = Sets.newHashSet("1", "2");

        when(mockHashOps.keys(SERVICE_TYPE)).thenReturn(redisGuids);
        Set<String>  keysToDelete = sut.getSurplusServicesGuids(SERVICE_TYPE, retrievedGuids);
        boolean eligible = keysToDelete.containsAll(Arrays.asList("3", "4"));
        assertEquals(true, eligible);
    }

    @Test
    public void testServiceInstanceExists() {
        UUID randomGuid = UUID.randomUUID();
        CredentialProperties existingEntry = new CredentialProperties(true,"",randomGuid.toString(),"","","","","","","");
        when(mockHashOps.get(SERVICE_TYPE, randomGuid.toString())).thenReturn(existingEntry);
        boolean eligible = sut.exists(SERVICE_TYPE, randomGuid);
        assertEquals(true, eligible);
    }

    @Test
    public void testGetCredentialsGeneration() {
        String malformedName1 = "my--___+!^!)--(*&instance";
        String malformedName2 = "-my--___+!^!)--(*&instance-";
        String malformedName3 = "my--___+!^!)--(*&instance-";
        String malformedName4 = "-my--___+!^!)--(*&instance";
        String malformedName5 = "-----my--+!^!)--(&instance";
        String malformedName6 = "&$%&%$&my--+!^!)--(*&instance";

        String domain = ".daily-nokrb.gotapass.eu";
        String randomServiceGuid = UUID.randomUUID().toString();

        String properName = "my-instance";
        String properUrl = properName+"-"+randomServiceGuid+domain;

        CredentialProperties entry1 = new CredentialProperties(true,domain,randomServiceGuid,randomServiceGuid,malformedName1,"","","","","");
        CredentialProperties entry2 = new CredentialProperties(true,domain,randomServiceGuid,randomServiceGuid,malformedName2,"","","","","");
        CredentialProperties entry3 = new CredentialProperties(true,domain,randomServiceGuid,randomServiceGuid,malformedName3,"","","","","");
        CredentialProperties entry4 = new CredentialProperties(true,domain,randomServiceGuid,randomServiceGuid,malformedName4,"","","","","");
        CredentialProperties entry5 = new CredentialProperties(true,domain,randomServiceGuid,randomServiceGuid,malformedName5,"","","","","");
        CredentialProperties entry6 = new CredentialProperties(true,domain,randomServiceGuid,randomServiceGuid,malformedName6,"","","","","");

        assertEquals(properUrl, entry1.getHostName());
        assertEquals(properUrl, entry2.getHostName());
        assertEquals(properUrl, entry3.getHostName());
        assertEquals(properUrl, entry4.getHostName());
        assertEquals(properUrl, entry5.getHostName());
        assertEquals(properUrl, entry6.getHostName());
    }

    @Test
    public void testGetCredentialsInJSON() {
        String serviceName = "tested";
        UUID guid = UUID.randomUUID();
        String randomServiceGuid = guid.toString();
        UUID randomSpaceGuid = UUID.randomUUID();
        UUID randomSpaceGuid2 = UUID.randomUUID();

        Map<String, String> testEntry = ImmutableMap.of("guid", randomServiceGuid, "hostname", serviceName + "-" + randomServiceGuid, "login", "", "password", "");

        when(mockCredentialsProperties.retriveMapForm()).thenReturn(testEntry);
        when(mockCredentialsProperties.getSpaceGuid()).thenReturn(randomSpaceGuid.toString());

        CredentialProperties existingEntry = new CredentialProperties(true,"",randomServiceGuid,randomSpaceGuid.toString(),serviceName,"","","","","");

        List<Object> serviceEntries = ImmutableList.of(existingEntry);
        when(mockHashOps.get(SERVICE_TYPE, guid.toString())).thenReturn(existingEntry);
        CredentialProperties entry = sut.get(SERVICE_TYPE, guid);

        Map ref = ImmutableMap.of(serviceName, testEntry);
        Map actual = ImmutableMap.of(entry.getName(), entry.retriveMapForm());
        assertEquals(ref, actual);
    }
}