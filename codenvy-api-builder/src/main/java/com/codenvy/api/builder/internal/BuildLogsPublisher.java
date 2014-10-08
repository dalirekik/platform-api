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
package com.codenvy.api.builder.internal;

import com.codenvy.api.core.notification.EventService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Publishes builder's outputs to the EventService.
 *
 * @author andrew00x
 */
class BuildLogsPublisher extends DelegateBuildLogger {
    private final AtomicInteger lineCounter;
    private final EventService eventService;
    private final long         taskId;
    private final String       workspace;
    private final String       project;

    public static final int SOURCES_DOWNLOAD_START_MESSAGE_LINE = 1;
    public static final int SOURCES_DOWNLOAD_END_MESSAGE_LINE = 2;

    BuildLogsPublisher(BuildLogger delegate, EventService eventService, long taskId, String workspace, String project) {
        super(delegate);
        this.eventService = eventService;
        this.taskId = taskId;
        this.workspace = workspace;
        this.project = project;
        lineCounter = new AtomicInteger(SOURCES_DOWNLOAD_END_MESSAGE_LINE + 1);
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (line != null) {
            eventService.publish(BuilderEvent.messageLoggedEvent(taskId, workspace, project,
                                                                 new BuilderEvent.LoggedMessage(line, lineCounter.getAndIncrement())));
        }
        super.writeLine(line);
    }
}
