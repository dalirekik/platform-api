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
package com.codenvy.api.core.rest.shared.dto;

import com.codenvy.dto.shared.DTO;

/**
 * Describes error which may be serialized to JSON format with {@link com.codenvy.api.core.rest.ApiExceptionMapper}
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.ApiException
 * @see com.codenvy.api.core.rest.ApiExceptionMapper
 */
@DTO
public interface ServiceError {
    /**
     * Get error message.
     *
     * @return error message
     */
    String getMessage();

    ServiceError withMessage(String message);

    /**
     * Set error message.
     *
     * @param message
     *         error message
     */
    void setMessage(String message);
}
