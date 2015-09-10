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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class StoreConfig {

    private static final Logger LOG = LoggerFactory.getLogger(StoreConfig.class);

    @Value("${redis.hostname}")
    private String redisHostname;

    @Value("${redis.port}")
    private String redisPort;

    @Bean
    protected CredentialsStore redisCredentialsStore(RedisOperations<String, CredentialProperties> redisTemplate) {
        return new RedisCredentialsStore(redisTemplate);
    }

    @Bean
    public RedisOperations<String, CredentialProperties> redisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String, CredentialProperties> template = new RedisTemplate<String, CredentialProperties>();

        jedisConnectionFactory.setPort(Integer.parseInt(redisPort));
        jedisConnectionFactory.setHostName(redisHostname);

        template.setConnectionFactory(jedisConnectionFactory);

        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        RedisSerializer<CredentialProperties> albumSerializer = new JacksonJsonRedisSerializer<CredentialProperties>(CredentialProperties.class);

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(albumSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(albumSerializer);

        return template;
    }
}
