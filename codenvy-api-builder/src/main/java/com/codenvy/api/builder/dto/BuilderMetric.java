/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.builder.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author andrew00x
 */
@DTO
public interface BuilderMetric {

    String WAITING_TIME_LIMIT = "waitingTimeLimit";

    String START_TIME = "startTime";

    String TERMINATION_TIME = "terminationTime";

    String END_TIME = "endTime";

    String RUNNING_TIME  = "runningTime";

    String NUMBER_OF_WORKERS = "numberOfWorkers";

    String NUMBER_OF_ACTIVE_WORKERS = "numberOfActiveWorkers";

    String QUEUE_SIZE = "queueSize";

    String MAX_QUEUE_SIZE = "maxQueueSize";


    String getName();

    BuilderMetric withName(String name);

    void setName(String name);

    String getValue();

    BuilderMetric withValue(String value);

    void setValue(String value);

    String getDescription();

    BuilderMetric withDescription(String description);

    void setDescription(String description);
}
