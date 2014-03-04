/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

package com.codenvy.api.analytics;


import com.codenvy.api.analytics.dto.MetricInfoDTO;
import com.codenvy.api.analytics.dto.MetricInfoListDTO;
import com.codenvy.api.analytics.dto.MetricValueDTO;
import com.codenvy.api.analytics.exception.MetricNotFoundException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Service is responsible for processing REST requests for analytics data.
 *
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
 */
@Path("analytics")
@Singleton
public class AnalyticsService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);

    @Inject
    private MetricHandler metricHandler;

    @GenerateLink(rel = "metric value")
    @GET
    @Path("metric/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user"})
    public Response getValue(@PathParam("name") String metricName,
                             @QueryParam("page") String page,
                             @QueryParam("per_page") String perPage,
                             @Context UriInfo uriInfo,
                             @Context SecurityContext securityContext) {
        try {
            Map<String, String> metricContext = Utils.extractContext(uriInfo,
                                                                     securityContext.getUserPrincipal(),
                                                                     page,
                                                                     perPage);

            MetricValueDTO value = metricHandler.getValue(metricName, metricContext, uriInfo);
            return Response.status(Response.Status.OK).entity(value).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GenerateLink(rel = "metric value")
    @GET
    @Path("public-metric/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublicValue(@PathParam("name") String metricName,
                                   @QueryParam("page") String page,
                                   @QueryParam("per_page") String perPage,
                                   @Context UriInfo uriInfo,
                                   @Context SecurityContext securityContext) {

        return getValue(metricName,
                        page,
                        perPage,
                        uriInfo,
                        securityContext);
    }
    
    @GenerateLink(rel = "list metric value")
    @POST
    @Path("/metric/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getValues(List<String> metricNames,
                             @QueryParam("page") String page,
                             @QueryParam("per_page") String perPage,
                             @Context UriInfo uriInfo,
                             @Context SecurityContext securityContext) {
        try {
            Map<String, String> metricContext = Utils.extractContext(uriInfo,
                                                                     securityContext.getUserPrincipal(),
                                                                     page,
                                                                     perPage);
            
            return Response.status(Response.Status.OK).entity(metricHandler.getValues(metricNames, metricContext, uriInfo)).build();
        } catch (MetricNotFoundException e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GenerateLink(rel = "metric info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("metricinfo/{name}")
    @RolesAllowed({"user"})
    public Response getInfo(@PathParam("name") String metricName,
                            @Context UriInfo uriInfo,
                            @Context SecurityContext securityContext) {
        try {
            MetricInfoDTO metricInfoDTO = metricHandler.getInfo(metricName, uriInfo);
            return Response.status(Response.Status.OK).entity(metricInfoDTO).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GenerateLink(rel = "all metric info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("metricinfo")
    @RolesAllowed({"user"})
    public Response getAllInfo(@Context UriInfo uriInfo,
                               @Context SecurityContext securityContext) {
        try {
            MetricInfoListDTO metricInfoListDTO = metricHandler.getAllInfo(uriInfo);
            return Response.status(Response.Status.OK).entity(metricInfoListDTO).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
