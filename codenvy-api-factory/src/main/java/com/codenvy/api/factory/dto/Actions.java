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
import com.codenvy.api.vfs.shared.dto.ReplacementSet;
import com.codenvy.dto.shared.DTO;

import java.util.List;

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Describes actions that should be done after loading of the IDE
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
@Deprecated
public interface Actions {
    /**
     * Welcome page configuration.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "welcome", format = ENCODED, trackedOnly = true)
    WelcomePage getWelcome();

    @Deprecated
    void setWelcome(WelcomePage welcome);

    @Deprecated
    Actions withWelcome(WelcomePage welcome);

    /**
     * Allow to use text replacement in project files after clone
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "findReplace")
    @Deprecated
    List<ReplacementSet> getFindReplace();

    @Deprecated
    void setFindReplace(List<ReplacementSet> variable);

    @Deprecated
    Actions withFindReplace(List<ReplacementSet> variable);


    /**
     * Path of the file to open in the project.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "openFile")
    @Deprecated
    String getOpenFile();

    @Deprecated
    void setOpenFile(String openFile);

    @Deprecated
    Actions withOpenFile(String openFile);

    /**
     * Warn on leave page
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "warnOnClose")
    @Deprecated
    Boolean getWarnOnClose();

    @Deprecated
    void setWarnOnClose(Boolean warnOnClose);

    @Deprecated
    Actions withWarnOnClose(Boolean warnOnClose);

}
