/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.factory.parameter;

import com.codenvy.api.factory.FactoryUrlException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.ProjectAttributes;
import com.codenvy.dto.server.DtoFactory;

/**
 * Move 'pname' parameter into projectattributes object.
 *
 * @author Alexander Garagatyi
 */
public class ProjectNameConverter implements LegacyConverter {
    @Override
    public void convert(Factory factory) throws FactoryUrlException {
        if (factory.getPname() != null) {
            ProjectAttributes attributes = factory.getProjectattributes();
            if (null == attributes || attributes.getPname() == null) {
                attributes =
                        attributes == null ? DtoFactory.getInstance().createDto(ProjectAttributes.class) : attributes;
                attributes.setPname(factory.getPname());
                factory.setPname(null);
                factory.setProjectattributes(attributes);
            } else if (attributes.getPname() != null) {
                throw new FactoryUrlException(
                        "Parameters 'pname' and 'projectsttributes.pname' are mutually exclusive.");
            }
        }
    }
}
