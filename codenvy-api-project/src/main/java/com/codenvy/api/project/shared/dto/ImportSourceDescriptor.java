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
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.Map;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.MANDATORY;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * @author Vitaly Parfonov
 */
@DTO
public interface ImportSourceDescriptor {
    /**
     * @return type of importer e.g zip, git
     */
    @ApiModelProperty(value = "Importer type", required = true, allowableValues = "zip,git,svn")
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "type")
    String getType();

    /**
     * @param type e.g git, zip
     */
    void setType(String type);

    ImportSourceDescriptor withType(String type);

    /**
     * @return location to the resource
     */
    @ApiModelProperty(value = "Location of remote resources to be imported", required = true)
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "location")
    String getLocation();

    /**
     * @param location to the resource
     */
    void setLocation(String location);

    ImportSourceDescriptor withLocation(String location);

    /**
     * @return import parameters
     */
    @ApiModelProperty(value = "Optional import parameters", required = false)
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "parameters")
    Map<String, String> getParameters();

    /**
     * @param parameters import parameters
     */
    void setParameters(Map<String, String> parameters);

    ImportSourceDescriptor withParameters(Map<String, String> parameters);
}
