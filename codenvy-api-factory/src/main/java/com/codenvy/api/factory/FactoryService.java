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
package com.codenvy.api.factory;

import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.Pair;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.ProjectAttributes;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.lang.URLEncodedUtils;
import com.codenvy.dto.server.DtoFactory;
import com.google.gson.JsonSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.commons.lang.Strings.nullToEmpty;

/** Service for factory rest api features */
@Path("factory")
public class FactoryService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    @Inject
    private FactoryStore        factoryStore;

    @Inject
    private FactoryUrlValidator validator;

    @Inject
    private LinksHelper         linksHelper;

    @Inject
    private FactoryBuilder      factoryBuilder;

    /**
     * Save factory to storage and return stored data. Field 'factoryUrl' should contains factory url information. Fields with images should
     * be named 'image'. Acceptable image size 100x100 pixels. If vcs is not set in factory URL it will be set with "git" value.
     * 
     * @param request - http request
     * @param uriInfo - url context
     * @return - stored data
     * @throws FactoryUrlException - with response code 400 if factory url json is not found - with response code 400 if vcs is unsupported
     *             - with response code 400 if image content can't be read - with response code 400 if image media type is unsupported -
     *             with response code 400 if image height or length isn't equal to 100 pixels - with response code 413 if image is too big -
     *             with response code 500 if internal server error occurs
     */
    @RolesAllowed("user")
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    public Factory saveFactory(@Context HttpServletRequest request, @Context UriInfo uriInfo) throws FactoryUrlException {
        try {
            EnvironmentContext context = EnvironmentContext.getCurrent();
            if (context.getUser() == null || context.getUser().getName() == null || context.getUser().getId() == null) {
                throw new FactoryUrlException(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to identify user from context");
            }

            Set<FactoryImage> images = new HashSet<>();
            Factory factoryUrl = null;

            for (Part part : request.getParts()) {
                String fieldName = part.getName();
                if (fieldName.equals("factoryUrl")) {
                    try {
                        factoryUrl = factoryBuilder.buildEncoded(part.getInputStream());
                    } catch (JsonSyntaxException e) {
                        throw new FactoryUrlException(
                                                      "You have provided an invalid JSON.  For more information, please visit http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/");
                    }
                } else if (fieldName.equals("image")) {
                    try (InputStream inputStream = part.getInputStream()) {
                        FactoryImage factoryImage =
                                                    FactoryImage.createImage(inputStream, part.getContentType(),
                                                                             NameGenerator.generate(null, 16));
                        if (factoryImage.hasContent()) {
                            images.add(factoryImage);
                        }
                    }
                }
            }

            if (factoryUrl == null) {
                LOG.warn("No factory URL information found in 'factoryUrl' section of multipart form-data.");
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(),
                                              "No factory URL information found in 'factoryUrl' section of multipart/form-data.");
            }

            if (factoryUrl.getV().equals("1.0")) {
                throw new FactoryUrlException("Storing of Factory 1.0 is unsupported.");
            }

            factoryUrl.setUserid(context.getUser().getId());
            factoryUrl.setCreated(System.currentTimeMillis());
            String factoryId = factoryStore.saveFactory(factoryUrl, images);
            factoryUrl = factoryStore.getFactory(factoryId);
            factoryUrl = factoryUrl.withLinks(linksHelper.createLinks(factoryUrl, images, uriInfo));

            String createProjectLink = "";
            Iterator<Link> createProjectLinksIterator = linksHelper.getLinkByRelation(factoryUrl.getLinks(), "create-project").iterator();
            if (createProjectLinksIterator.hasNext()) {
                createProjectLink = createProjectLinksIterator.next().getHref();
            }
            ProjectAttributes attributes = factoryUrl.getProjectattributes();
            LOG.info(
                     "EVENT#factory-created# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# REPO-URL#{}# FACTORY-URL#{}# AFFILIATE-ID#{}# ORG-ID#{}#",
                     "",
                     context.getUser().getName(),
                     "",
                     nullToEmpty(attributes != null ? attributes.getPtype() : ""),
                     factoryUrl.getVcsurl(),
                     createProjectLink,
                     nullToEmpty(factoryUrl.getAffiliateid()),
                     nullToEmpty(factoryUrl.getOrgid()));

            return factoryUrl;
        } catch (IOException | ServletException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new FactoryUrlException(Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get factory json from non encoded version of factory.
     * 
     * @param uriInfo - url context
     * @return - stored data, if id is correct.
     * @throws FactoryUrlException - with response code 404 if factory with given id doesn't exist
     */
    @GET
    @Path("/nonencoded")
    @Produces({MediaType.APPLICATION_JSON})
    public Factory getFactoryFromNonEncoded(@DefaultValue("false") @QueryParam("legacy") Boolean legacy, @Context UriInfo uriInfo)
                                                                                                                                  throws FactoryUrlException {
        URI uri = UriBuilder.fromUri(uriInfo.getRequestUri()).replaceQueryParam("legacy", null).replaceQueryParam("token", null).build();
        Factory factory = factoryBuilder.buildNonEncoded(uri);
        if (legacy) {
            factory = factoryBuilder.convertToLatest(factory);
        }
        validator.validate(factory, false);
        return factory;
    }

    /**
     * Get factory information from storage by its id.
     * 
     * @param id - id of factory
     * @param uriInfo - url context
     * @return - stored data, if id is correct.
     * @throws FactoryUrlException - with response code 404 if factory with given id doesn't exist
     */
    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Factory getFactory(@PathParam("id") String id, @DefaultValue("false") @QueryParam("legacy") Boolean legacy,
                              @Context UriInfo uriInfo) throws FactoryUrlException {
        Factory factoryUrl = factoryStore.getFactory(id);
        if (factoryUrl == null) {
            LOG.warn("Factory URL with id {} is not found.", id);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), "Factory URL with id " + id + " is not found.");
        }

        if (legacy) {
            factoryUrl = factoryBuilder.convertToLatest(factoryUrl);
        }
        try {
            factoryUrl = factoryUrl.withLinks(linksHelper.createLinks(factoryUrl, factoryStore.getFactoryImages(id, null), uriInfo));
        } catch (UnsupportedEncodingException e) {
            throw new FactoryUrlException(e.getLocalizedMessage(), e);
        }
        validator.validate(factoryUrl, true);
        return factoryUrl;
    }

    /**
     * Get list of factory links which conform specified attributes.
     * 
     * @param uriInfo - url context
     * @return - stored data, if id is correct.
     * @throws FactoryUrlException - with response code 404 if factory with given id doesn't exist
     */
    @RolesAllowed("user")
    @GET
    @Path("/find")
    @Produces({MediaType.APPLICATION_JSON})
    @SuppressWarnings("unchecked")
    public List<Link> getFactoryByAttribute(@Context UriInfo uriInfo) throws FactoryUrlException {
        List<Link> result = new ArrayList<>();
        URI uri = UriBuilder.fromUri(uriInfo.getRequestUri()).replaceQueryParam("token", null).build();
        Map<String, Set<String>> queryParams = URLEncodedUtils.parse(uri, "UTF-8");
        if (queryParams.isEmpty())
            throw new IllegalArgumentException("Query must contain at least one attribute.");
        if (queryParams.containsKey("accountid"))
            queryParams.put("orgid", queryParams.remove("accountid"));
        ArrayList<Pair> pairs = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : queryParams.entrySet()) {
            if (!entry.getValue().isEmpty())
                pairs.add(Pair.of(entry.getKey(), entry.getValue().iterator().next()));
        }
        for (Factory factory : factoryStore.findByAttribute(pairs.toArray(new Pair[pairs.size()]))) {
            result.add(DtoFactory.getInstance().createDto(Link.class)
                                 .withMethod("GET")
                                 .withRel("self")
                                 .withProduces(MediaType.APPLICATION_JSON)
                                 .withConsumes(null)
                                 .withHref(UriBuilder.fromUri(uriInfo.getBaseUri())
                                                     .path(FactoryService.class)
                                                     .path(FactoryService.class, "getFactory").build(factory.getId())
                                                     .toString())
                                 .withParameters(null));
        }
        return result;
    }

    /**
     * Get image information by its id.
     * 
     * @param factoryId - id of factory
     * @param imageId - image id.
     * @return - image information if ids are correct. If imageId is not set, random image of factory will be returned. But if factory has
     *         no images, exception will be thrown.
     * @throws FactoryUrlException - with response code 404 if factory with given id doesn't exist - with response code 404 if imgId is not
     *             set in request and there is no default image for factory with given id - with response code 404 if image with given image
     *             id doesn't exist
     */
    @GET
    @Path("{factoryId}/image")
    @Produces("image/*")
    public Response getImage(@PathParam("factoryId") String factoryId, @DefaultValue("") @QueryParam("imgId") String imageId)
                                                                                                                             throws FactoryUrlException {
        Set<FactoryImage> factoryImages = factoryStore.getFactoryImages(factoryId, null);
        if (factoryImages == null) {
            LOG.warn("Factory URL with id {} is not found.", factoryId);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), "Factory URL with id " + factoryId + " is not found.");
        }
        if (imageId.isEmpty()) {
            if (factoryImages.size() > 0) {
                FactoryImage image = factoryImages.iterator().next();
                return Response.ok(image.getImageData(), image.getMediaType()).build();
            } else {
                LOG.warn("Default image for factory {} is not found.", factoryId);
                throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(),
                                              "Default image for factory " + factoryId + " is not found.");
            }
        } else {
            for (FactoryImage image : factoryImages) {
                if (image.getName().equals(imageId)) {
                    return Response.ok(image.getImageData(), image.getMediaType()).build();
                }
            }
        }
        LOG.warn("Image with id {} is not found.", imageId);
        throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), "Image with id " + imageId + " is not found.");
    }

    /**
     * Get factory snippet by factory id and snippet type. If snippet type is not set, "url" type will be used as default.
     * 
     * @param id - factory id.
     * @param type - type of snippet.
     * @param uriInfo - url context
     * @return - snippet content.
     * @throws FactoryUrlException - with response code 404 if factory with given id doesn't exist - with response code 400 if snippet type
     *             is unsupported
     */
    @GET
    @Path("{id}/snippet")
    @Produces({MediaType.TEXT_PLAIN})
    public String getFactorySnippet(@PathParam("id") String id, @DefaultValue("url") @QueryParam("type") String type,
                                    @Context UriInfo uriInfo)
                                                             throws FactoryUrlException {
        Factory factory = factoryStore.getFactory(id);
        if (factory == null) {
            LOG.warn("Factory URL with id {} is not found.", id);
            throw new FactoryUrlException(Status.NOT_FOUND.getStatusCode(), "Factory URL with id " + id + " is not found.");
        }

        switch (type) {
            case "url":
                return UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("factory").queryParam("id", id).build().toString();
            case "html":
                return SnippetGenerator.generateHtmlSnippet(id, factory.getStyle(), UriBuilder.fromUri(uriInfo.getBaseUri())
                                                                                              .replacePath("").build().toString());
            case "iframe":
                return SnippetGenerator.generateiFrameSnippet(UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("factory")
                                                                        .queryParam("id", id).build().toString());
            case "markdown":
                Set<FactoryImage> factoryImages = factoryStore.getFactoryImages(id, null);
                String imageId = (factoryImages.size() > 0) ? factoryImages.iterator().next().getName() : null;
                return SnippetGenerator
                                       .generateMarkdownSnippet(UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("factory")
                                                                          .queryParam("id", id).build().toString(), id,
                                                                imageId, factory.getStyle(),
                                                                UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("").build().toString());
            default:
                LOG.warn("Snippet type {} is unsupported", type);
                throw new FactoryUrlException(Status.BAD_REQUEST.getStatusCode(), "Snippet type \"" + type + "\" is unsupported.");
        }
    }
}
