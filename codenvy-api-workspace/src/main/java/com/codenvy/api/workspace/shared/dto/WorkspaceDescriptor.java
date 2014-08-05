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
package com.codenvy.api.workspace.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModel;

import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
@ApiModel
public interface WorkspaceDescriptor {
    String getId();

    void setId(String id);

    WorkspaceDescriptor withId(String id);

    String getName();

    void setName(String name);

    WorkspaceDescriptor withName(String name);

    void setTemporary(boolean temporary);

    boolean isTemporary();

    WorkspaceDescriptor withTemporary(boolean temporary);

    String getAccountId();

    void setAccountId(String accountId);

    WorkspaceDescriptor withAccountId(String accountId);

    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    WorkspaceDescriptor withAttributes(Map<String, String> attributes);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    WorkspaceDescriptor withLinks(List<Link> links);
}
