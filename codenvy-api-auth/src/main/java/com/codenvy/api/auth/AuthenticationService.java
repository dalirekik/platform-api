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

import com.codenvy.api.auth.shared.dto.Credentials;
import com.codenvy.api.auth.shared.dto.Token;
import com.codenvy.api.core.ApiException;
import com.codenvy.dto.server.DtoFactory;
import com.wordnik.swagger.annotations.*;


import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Authenticate user by username and password.
 * <p/>
 * In response user receive "token". This token user can use
 * to identify him in all other request to API, to do that he should pass it as query parameter.
 *
 * @author Sergii Kabashniuk
 * @author Alexander Garagatyi
 */

@Api(value = "/auth",
     description = "Authentication manager")
@Path("/auth")
public class AuthenticationService {

    private final AuthenticationDao dao;

    @Inject
    public AuthenticationService(AuthenticationDao dao) {
        this.dao = dao;
    }

    /**
     * Get token to be able to call secure api methods.
     *
     * @param credentials
     *         - username and password
     * @return - auth token in JSON, session-based and persistent cookies
     * @throws ApiException
     */
    @ApiOperation(value = "Login",
                  notes = "Login to a Codenvy account. Either auth token or cookie are used",
                  response = Token.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Authentication error")})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/login")
    public Token authenticate(Credentials credentials,
                              @Context UriInfo uriInfo)
            throws ApiException {

        if (credentials == null
            || credentials.getPassword() == null
            || credentials.getPassword().isEmpty()
            || credentials.getUsername() == null
            || credentials.getUsername().isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        return dao.login(credentials);
    }

    /**
     * Perform logout for the given token.
     *
     * @param token
     *         - authentication token
     */
    @ApiOperation(value = "Logout",
                  notes = "Logout from a Codenvy account",
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Authentication error")})
    @POST
    @Path("/logout")
    public void logout(@ApiParam(value = "Auth token", required = true) @QueryParam("token") String token, @Context UriInfo uriInfo) {
        if (token == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        dao.logout(DtoFactory.getInstance().createDto(Token.class).withValue(token));
    }

}
