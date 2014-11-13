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
package com.codenvy.api.auth;

import com.codenvy.api.auth.shared.dto.Token;

import javax.ws.rs.core.Response;

/**
 * Allow to extend login and logout responses by
 * adding or removing custom cookies or headers.
 *
 * @author Sergii Kabashniuk
 */
public interface ResponseBuilder {

    Response buildLoginResponse(Token token);

    Response buildLogoutResponse(Token token);
}
