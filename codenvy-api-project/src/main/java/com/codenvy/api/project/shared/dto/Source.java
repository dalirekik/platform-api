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
package com.codenvy.api.project.shared.dto;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import java.util.Map;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.MANDATORY;

/**
 * Describes project source with additional sources such as runner's
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface Source {
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "project")
    ImportSourceDescriptor getProject();

    void setProject(ImportSourceDescriptor project);

    Source withProject(ImportSourceDescriptor project);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "runners")
    Map<String, RunnerSource> getRunners();

    void setRunners(Map<String, RunnerSource> runners);

    Source withRunners(Map<String, RunnerSource> runners);
}

