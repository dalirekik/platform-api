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
package com.codenvy.api.factory.dto;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.api.project.shared.dto.NewProject;
import com.codenvy.api.project.shared.dto.Source;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.MANDATORY;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.core.factory.FactoryParameter.Version.V2_1;

/**
 * Factory of version 2.0
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
public interface FactoryV2_0 {

    /**
     * @return Version for Codenvy Factory API.
     */
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "v")
    String getV();

    void setV(String v);

    FactoryV2_0 withV(String v);

    /**
     * Describes source where project's files can be retrieved
     */
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "source")
    Source getSource();

    void setSource(Source source);

    FactoryV2_0 withSource(Source source);

    /**
     * Describes parameters of the workspace that should be used for factory
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "workspace")
    Workspace getWorkspace();

    void setWorkspace(Workspace workspace);

    FactoryV2_0 withWorkspace(Workspace workspace);

    /**
     * Describe restrictions of the factory
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "policies", trackedOnly = true)
    Policies getPolicies();

    void setPolicies(Policies policies);

    FactoryV2_0 withPolicies(Policies policies);

    /**
     * Describes project that should be factory-created
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "project")
    NewProject getProject();

    void setProject(NewProject project);

    FactoryV2_0 withProject(NewProject project);

    /**
     * Identifying information of author
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "creator")
    Author getCreator();

    void setCreator(Author creator);

    FactoryV2_0 withCreator(Author creator);

    /**
     * Describes actions that should be done after loading of the IDE
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "actions", deprecatedSince = V2_1)
    Actions getActions();

    @Deprecated
    void setActions(Actions actions);

    @Deprecated
    FactoryV2_0 withActions(Actions actions);

    /**
     * Describes factory button
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "button", format = ENCODED)
    Button getButton();

    void setButton(Button button);

    FactoryV2_0 withButton(Button button);


    /**
     * @return - id of stored factory object
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "id", format = ENCODED, setByServer = true)
    String getId();

    void setId(String id);

    FactoryV2_0 withId(String id);

}
