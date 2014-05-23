/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.runner.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Describes single item of stats of application's environment.
 *
 * @author andrew00x
 */
@DTO
public interface ApplicationEnvironmentStatsItem {
    String getName();

    ApplicationEnvironmentStatsItem withName(String name);

    void setName(String name);

    String getValue();

    ApplicationEnvironmentStatsItem withValue(String value);

    void setValue(String value);

    String getDescription();

    ApplicationEnvironmentStatsItem withDescription(String description);

    void setDescription(String description);
}
