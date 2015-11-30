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
package org.trustedanalytics.serviceexposer.nats.registrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class RegistratorScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RegistratorScheduler.class);

    private final RegistratorJob registeringJob;
    private final String natsTriggerExpression;

    @Autowired
    public RegistratorScheduler(RegistratorJob natsRegisteringJob, String natsTriggerExpression) {
        this.registeringJob = natsRegisteringJob;
        this.natsTriggerExpression = natsTriggerExpression;
    }

    public void start() {
        LOG.info("Preparing RegistratorScheduler");
        TaskScheduler s = new DefaultManagedTaskScheduler();
        s.schedule(registeringJob::run, new CronTrigger(natsTriggerExpression));
        LOG.info("RegistratorScheduler started {}", natsTriggerExpression);
    }
}
