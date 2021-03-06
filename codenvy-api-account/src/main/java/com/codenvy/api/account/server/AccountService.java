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
package com.codenvy.api.account.server;

import com.codenvy.api.account.server.dao.Account;
import com.codenvy.api.account.server.dao.AccountDao;
import com.codenvy.api.account.server.dao.Billing;
import com.codenvy.api.account.server.dao.Member;
import com.codenvy.api.account.server.dao.PlanDao;
import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.account.server.dao.SubscriptionAttributes;
import com.codenvy.api.account.shared.dto.AccountDescriptor;
import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.api.account.shared.dto.AccountUpdate;
import com.codenvy.api.account.shared.dto.BillingDescriptor;
import com.codenvy.api.account.shared.dto.CycleTypeDescriptor;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.NewAccount;
import com.codenvy.api.account.shared.dto.NewBilling;
import com.codenvy.api.account.shared.dto.NewMembership;
import com.codenvy.api.account.shared.dto.NewSubscription;
import com.codenvy.api.account.shared.dto.NewSubscriptionAttributes;
import com.codenvy.api.account.shared.dto.NewSubscriptionTemplate;
import com.codenvy.api.account.shared.dto.Plan;
import com.codenvy.api.account.shared.dto.SubscriptionAttributesDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.account.shared.dto.UpdateResourcesDescriptor;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.LinksHelper;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.codenvy.api.core.util.LinksHelper.createLink;
import static com.codenvy.commons.lang.Size.parseSizeToMegabytes;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

/**
 * Account API
 *
 * @author Eugene Voevodin
 * @author Alex Garagatyi
 */
@Api(value = "/account",
     description = "Account manager")
@Path("/account")
public class AccountService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);
    private final AccountDao                      accountDao;
    private final UserDao                         userDao;
    private final SubscriptionServiceRegistry     registry;
    private final PaymentService                  paymentService;
    private final PlanDao                         planDao;
    private final ResourcesManager                resourcesManager;
    private final SubscriptionAttributesValidator subscriptionAttributesValidator;

    @Inject
    public AccountService(AccountDao accountDao,
                          UserDao userDao,
                          SubscriptionServiceRegistry registry,
                          PaymentService paymentService,
                          PlanDao planDao,
                          ResourcesManager resourcesManager,
                          SubscriptionAttributesValidator subscriptionAttributesValidator) {
        this.accountDao = accountDao;
        this.userDao = userDao;
        this.registry = registry;
        this.paymentService = paymentService;
        this.planDao = planDao;
        this.resourcesManager = resourcesManager;
        this.subscriptionAttributesValidator = subscriptionAttributesValidator;
    }

    /**
     * Creates new account and adds current user as member to created account
     * with role <i>"account/owner"</i>. Returns status <b>201 CREATED</b>
     * and {@link AccountDescriptor} of created account if account has been created successfully.
     * Each new account should contain at least name.
     *
     * @param newAccount
     *         new account
     * @return descriptor of created account
     * @throws NotFoundException
     *         when some error occurred while retrieving account
     * @throws ConflictException
     *         when new account is {@code null}
     *         or new account name is {@code null}
     *         or when any of new account attributes is not valid
     * @throws ServerException
     * @see AccountDescriptor
     * @see #getById(String, SecurityContext)
     * @see #getByName(String, SecurityContext)
     */
    @ApiOperation(value = "Create a new account",
                  notes = "Create a new account",
                  response = Account.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Conflict Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_ACCOUNT)
    @RolesAllowed("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext,
                           @Required NewAccount newAccount) throws NotFoundException,
                                                                   ConflictException,
                                                                   ServerException {
        requiredNotNull(newAccount, "New account");
        requiredNotNull(newAccount.getName(), "Account name");
        if (newAccount.getAttributes() != null) {
            for (String attributeName : newAccount.getAttributes().keySet()) {
                validateAttributeName(attributeName);
            }
        }
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        //for now account <-One to One-> user
        if (accountDao.getByOwner(current.getId()).size() != 0) {
            throw new ConflictException(format("Account which owner is %s already exists", current.getId()));
        }
        try {
            accountDao.getByName(newAccount.getName());
            throw new ConflictException(format("Account with name %s already exists", newAccount.getName()));
        } catch (NotFoundException ignored) {
        }
        final String accountId = NameGenerator.generate(Account.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        final Account account = new Account().withId(accountId)
                                             .withName(newAccount.getName())
                                             .withAttributes(newAccount.getAttributes());
        //account should have owner
        final Member owner = new Member().withAccountId(accountId)
                                         .withUserId(current.getId())
                                         .withRoles(Arrays.asList("account/owner"));
        accountDao.create(account);
        accountDao.addMember(owner);
        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(account, securityContext))
                       .build();
    }

    /**
     * Returns all accounts memberships for current user.
     *
     * @return accounts memberships of current user
     * @throws NotFoundException
     *         when any of memberships contains account that doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving accounts or memberships
     * @see MemberDescriptor
     */
    @ApiOperation(value = "Get current user memberships",
                  notes = "This API call returns a JSON with all user membership in a single or multiple accounts",
                  response = MemberDescriptor.class,
                  responseContainer = "List",
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMemberships(@Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final List<Member> memberships = accountDao.getByMember(current.getId());
        final List<MemberDescriptor> result = new ArrayList<>(memberships.size());
        for (Member membership : memberships) {
            result.add(toDescriptor(membership, accountDao.getById(membership.getAccountId()), securityContext));
        }
        return result;
    }

    /**
     * Returns all accounts memberships for user with given identifier.
     *
     * @param userId
     *         user identifier to search memberships
     * @return accounts memberships
     * @throws ConflictException
     *         when user identifier is {@code null}
     * @throws NotFoundException
     *         when user with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving user or memberships
     * @see MemberDescriptor
     */
    @ApiOperation(value = "Get memberships of a specific user",
                  notes = "ID of a user should be specified as a query parameter. JSON with membership details is returned. For this API call system/admin or system/manager role is required",
                  response = MemberDescriptor.class,
                  responseContainer = "List",
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "No User ID specified"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/memberships")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfSpecificUser(@ApiParam(value = "User ID", required = true)
                                                               @Required @QueryParam("userid") String userId,
                                                               @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                                ServerException,
                                                                                                                ConflictException {
        requiredNotNull(userId, "User identifier");
        final User user = userDao.getById(userId);
        final List<Member> memberships = accountDao.getByMember(user.getId());
        final List<MemberDescriptor> result = new ArrayList<>(memberships.size());
        for (Member membership : memberships) {
            result.add(toDescriptor(membership, accountDao.getById(membership.getAccountId()), securityContext));
        }
        return result;
    }

    /**
     * Removes attribute with given name from certain account.
     *
     * @param accountId
     *         account identifier
     * @param attributeName
     *         attribute name to remove attribute
     * @throws ConflictException
     *         if attribute name is not valid
     * @throws NotFoundException
     *         if account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while getting/updating account
     */
    @ApiOperation(value = "Delete account attribute",
                  notes = "Remove attribute from an account. Attribute name is used as a quary parameter. For this API request account/owner, system/admin or system/manager role is required",
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Invalid attribute name"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}/attribute")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeAttribute(@ApiParam(value = "Account ID", required = true)
                                @PathParam("id") String accountId,
                                @ApiParam(value = "Attribute name to be removed", required = true)
                                @QueryParam("name") String attributeName) throws ConflictException, NotFoundException, ServerException {
        validateAttributeName(attributeName);
        final Account account = accountDao.getById(accountId);
        account.getAttributes().remove(attributeName);
        accountDao.update(account);
    }

    /**
     * Searches for account with given identifier and returns {@link AccountDescriptor} for it.
     *
     * @param id
     *         account identifier
     * @return descriptor of found account
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving account
     * @see AccountDescriptor
     * @see #getByName(String, SecurityContext)
     */
    @ApiOperation(value = "Get account by ID",
                  notes = "Get account information by its ID. JSON with account details is returned. This API call requires account/owner, system/admin or system/manager role.",
                  response = AccountDescriptor.class,
                  position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getById(@ApiParam(value = "Account ID", required = true)
                                     @PathParam("id") String id,
                                     @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
        return toDescriptor(account, securityContext);
    }

    /**
     * Searches for account with given name and returns {@link AccountDescriptor} for it.
     *
     * @param name
     *         account name
     * @return descriptor of found account
     * @throws NotFoundException
     *         when account with given name doesn't exist
     * @throws ConflictException
     *         when account name is {@code null}
     * @throws ServerException
     *         when some error occurred while retrieving account
     * @see AccountDescriptor
     * @see #getById(String, SecurityContext)
     */
    @ApiOperation(value = "Get account by name",
                  notes = "Get account information by its name. JSON with account details is returned. This API call requires system/admin or system/manager role.",
                  response = AccountDescriptor.class,
                  position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "No account name specified"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/find")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_NAME)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getByName(@ApiParam(value = "Account name", required = true)
                                       @Required @QueryParam("name") String name,
                                       @Context SecurityContext securityContext) throws NotFoundException,
                                                                                        ServerException,
                                                                                        ConflictException {
        requiredNotNull(name, "Account name");
        final Account account = accountDao.getByName(name);
        return toDescriptor(account, securityContext);
    }

    /**
     * Creates new account member with role <i>"account/member"</i>.
     *
     * @param accountId
     *         account identifier
     * @param membership
     *         new membership
     * @return descriptor of created member
     * @throws ConflictException
     *         when user identifier is {@code null}
     * @throws NotFoundException
     *         when user or account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while getting user or adding new account member
     * @see MemberDescriptor
     * @see #removeMember(String, String)
     * @see #getMembers(String, SecurityContext)
     */
    @ApiOperation(value = "Add a new member to account",
                  notes = "Add a new user to an account. This user will have account/member role. This API call requires account/owner, system/admin or system/manager role.",
                  response = MemberDescriptor.class,
                  position = 6)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "No user ID specified"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{id}/members")
    @RolesAllowed({"account/owner", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addMember(@ApiParam(value = "Account ID")
                              @PathParam("id")
                              String accountId,
                              @ApiParam(value = "New membership", required = true)
                              @Required
                              NewMembership membership,
                              @Context SecurityContext context) throws ConflictException,
                                                                       NotFoundException,
                                                                       ServerException {
        requiredNotNull(membership, "New membership");
        requiredNotNull(membership.getUserId(), "User ID");
        requiredNotNull(membership.getRoles(), "Roles");
        if (membership.getRoles().isEmpty()) {
            throw new ConflictException("Roles should not be empty");
        }
        userDao.getById(membership.getUserId());//check user exists
        final Member newMember = new Member().withAccountId(accountId)
                                             .withUserId(membership.getUserId())
                                             .withRoles(membership.getRoles());
        accountDao.addMember(newMember);
        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(newMember, accountDao.getById(accountId), context))
                       .build();
    }

    /**
     * Returns all members of certain account.
     *
     * @param id
     *         account identifier
     * @return account members
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving accounts or members
     * @see MemberDescriptor
     * @see #addMember(String, NewMembership, SecurityContext)
     * @see #removeMember(String, String)
     */
    @ApiOperation(value = "Get account members",
                  notes = "Get all members for a specific account. This API call requires account/owner, system/admin or system/manager role.",
                  response = MemberDescriptor.class,
                  responseContainer = "List",
                  position = 7)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}/members")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembers(@ApiParam(value = "Account ID")
                                             @PathParam("id") String id,
                                             @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
        final List<Member> members = accountDao.getMembers(id);
        final List<MemberDescriptor> result = new ArrayList<>(members.size());
        for (Member member : members) {
            result.add(toDescriptor(member, account, securityContext));
        }
        return result;
    }

    /**
     * Removes user with given identifier as member from certain account.
     *
     * @param accountId
     *         account identifier
     * @param userId
     *         user identifier
     * @throws NotFoundException
     *         when user or account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving account members or removing certain member
     * @throws ConflictException
     *         when removal member is last <i>"account/owner"</i>
     * @see #addMember(String, NewMembership, SecurityContext)
     * @see #getMembers(String, SecurityContext)
     */
    @ApiOperation(value = "Remove user from account",
                  notes = "Remove user from a specific account. This API call requires account/owner, system/admin or system/manager role.",
                  position = 8)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 409, message = "Account should have at least 1 owner"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}/members/{userid}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeMember(@ApiParam(value = "Account ID", required = true)
                             @PathParam("id") String accountId,
                             @ApiParam(value = "User ID")
                             @PathParam("userid") String userId) throws NotFoundException, ServerException, ConflictException {
        final List<Member> members = accountDao.getMembers(accountId);
        //search for member
        Member target = null;
        int owners = 0;
        for (Member member : members) {
            if (member.getRoles().contains("account/owner")) owners++;
            if (member.getUserId().equals(userId)) target = member;
        }
        if (target == null) {
            throw new ConflictException(format("User %s doesn't have membership with account %s", userId, accountId));
        }
        //account should have at least 1 owner
        if (owners == 1 && target.getRoles().contains("account/owner")) {
            throw new ConflictException("Account should have at least 1 owner");
        }
        accountDao.removeMember(target);
    }

    /**
     * <p>Updates account.</p>
     * <strong>Note:</strong> existed account attributes with same names as
     * update attributes will be replaced with update attributes.
     *
     * @param accountId
     *         account identifier
     * @param update
     *         account update
     * @return descriptor of updated account
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ConflictException
     *         when account update is {@code null}
     *         or when account with given name already exists
     * @throws ServerException
     *         when some error occurred while retrieving/persisting account
     * @see AccountDescriptor
     */
    @ApiOperation(value = "Update account",
                  notes = "Update account. This API call requires account/owner role.",
                  response = AccountDescriptor.class,
                  position = 9)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 409, message = "Invalid account ID or account name already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{id}")
    @RolesAllowed({"account/owner"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public AccountDescriptor update(@ApiParam(value = "Account ID", required = true)
                                    @PathParam("id") String accountId,
                                    AccountUpdate update,
                                    @Context SecurityContext securityContext) throws NotFoundException,
                                                                                     ConflictException,
                                                                                     ServerException {
        requiredNotNull(update, "Account update");
        final Account account = accountDao.getById(accountId);
        //current user should be account owner to update it
        if (update.getName() != null) {
            if (!account.getName().equals(update.getName()) && accountDao.getByName(update.getName()) != null) {
                throw new ConflictException(format("Account with name %s already exists", update.getName()));
            } else {
                account.setName(update.getName());
            }
        }
        if (update.getAttributes() != null) {
            for (String attributeName : update.getAttributes().keySet()) {
                validateAttributeName(attributeName);
            }
            account.getAttributes().putAll(update.getAttributes());
        }
        accountDao.update(account);
        return toDescriptor(account, securityContext);
    }

    /**
     * Returns list of subscriptions descriptors for certain account.
     * If service identifier is provided returns subscriptions that matches provided service.
     *
     * @param accountId
     *         account identifier
     * @param serviceId
     *         service identifier
     * @return subscriptions descriptors
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving subscriptions
     * @see SubscriptionDescriptor
     */
    @ApiOperation(value = "Get account subscriptions",
                  notes = "Get information on account subscriptions. This API call requires account/owner, account/member, system/admin or system/manager role.",
                  response = SubscriptionDescriptor.class,
                  responseContainer = "List",
                  position = 10)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{accountId}/subscriptions")
    @RolesAllowed({"account/member", "account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionDescriptor> getSubscriptions(@ApiParam(value = "Account ID", required = true)
                                                         @PathParam("accountId") String accountId,
                                                         @ApiParam(value = "Service ID", required = false)
                                                         @QueryParam("service") String serviceId,
                                                         @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                          ServerException {
        final List<Subscription> subscriptions =
                accountDao.getSubscriptions(accountId, serviceId != null && serviceId.isEmpty() ? null : serviceId);
        final List<SubscriptionDescriptor> result = new ArrayList<>(subscriptions.size());
        for (Subscription subscription : subscriptions) {
            result.add(toDescriptor(subscription, securityContext, null));
        }
        return result;
    }

    /**
     * Returns {@link SubscriptionDescriptor} for subscription with given identifier.
     *
     * @param subscriptionId
     *         subscription identifier
     * @return descriptor of subscription
     * @throws NotFoundException
     *         when subscription with given identifier doesn't exist
     * @throws ForbiddenException
     *         when user hasn't access to call this method
     * @see SubscriptionDescriptor
     * @see #getSubscriptions(String, String serviceId, SecurityContext)
     * @see #removeSubscription(String, SecurityContext)
     */
    @ApiOperation(value = "Get subscription details",
                  notes = "Get information on a particular subscription by its unique ID.",
                  response = SubscriptionDescriptor.class,
                  position = 11)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this method"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/subscriptions/{subscriptionId}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public SubscriptionDescriptor getSubscriptionById(@ApiParam(value = "Subscription ID", required = true)
                                                      @PathParam("subscriptionId") String subscriptionId,
                                                      @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                       ServerException,
                                                                                                       ForbiddenException {
        final Subscription subscription = accountDao.getSubscriptionById(subscriptionId);
        Set<String> roles = null;
        if (securityContext.isUserInRole("user")) {
            roles = resolveRolesForSpecificAccount(subscription.getAccountId());
            if (!roles.contains("account/owner") && !roles.contains("account/member")) {
                throw new ForbiddenException("Access denied");
            }
        }
        return toDescriptor(subscription, securityContext, roles);
    }

    /**
     * <p>Creates new subscription. Returns {@link SubscriptionDescriptor}
     * when subscription has been created successfully.
     * <p>Each new subscription should contain plan id and account id </p>
     *
     * @param newSubscription
     *         new subscription
     * @return descriptor of created subscription
     * @throws ConflictException
     *         when new subscription is {@code null}
     *         or new subscription plan identifier is {@code null}
     *         or new subscription account identifier is {@code null}
     * @throws NotFoundException
     *         if plan with certain identifier is not found
     * @throws com.codenvy.api.core.ApiException
     * @see SubscriptionDescriptor
     * @see #getSubscriptionById(String, SecurityContext)
     * @see #removeSubscription(String, SecurityContext)
     */
    @ApiOperation(value = "Add new subscription",
                  notes = "Add a new subscription to an account. JSON with subscription details is sent. Roles: account/owner, system/admin.",
                  response = SubscriptionDescriptor.class,
                  position = 12)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Invalid subscription parameter"),
            @ApiResponse(code = 409, message = "Unknown ServiceID is used or payment token is invalid"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSubscription(@ApiParam(value = "Subscription details", required = true)
                                    @Required NewSubscription newSubscription,
                                    @Context SecurityContext securityContext)
            throws ApiException {
        requiredNotNull(newSubscription, "New subscription");
        requiredNotNull(newSubscription.getAccountId(), "Account identifier");
        requiredNotNull(newSubscription.getPlanId(), "Plan identifier");
        //check user has access to add subscription
        final Set<String> roles = new HashSet<>();
        if (securityContext.isUserInRole("user")) {
            roles.addAll(resolveRolesForSpecificAccount(newSubscription.getAccountId()));
            if (!roles.contains("account/owner")) {
                throw new ForbiddenException("Access denied");
            }
        }

        final Plan plan = planDao.getPlanById(newSubscription.getPlanId());
        //check service exists
        final SubscriptionService service = registry.get(plan.getServiceId());
        if (null == service) {
            throw new ConflictException("Unknown serviceId is used");
        }
        //create new subscription
        final Subscription subscription = new Subscription().withAccountId(newSubscription.getAccountId())
                                                            .withServiceId(plan.getServiceId())
                                                            .withPlanId(plan.getId())
                                                            .withProperties(plan.getProperties());
        subscription.setId(NameGenerator.generate(Subscription.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH));

        NewSubscriptionAttributes newSubscriptionAttributes = newSubscription.getSubscriptionAttributes();
        subscriptionAttributesValidator.validate(newSubscriptionAttributes);
        service.beforeCreateSubscription(subscription);

        if ("false".equals(newSubscriptionAttributes.getBilling().getUsePaymentSystem()) &&
            !securityContext.isUserInRole("system/admin") &&
            !securityContext.isUserInRole("system/manager")) {
            throw new ConflictException("Given value of billing attribute usePaymentSystem is not allowed");
        }
        if (plan.getSalesOnly() && !securityContext.isUserInRole("system/admin") && !securityContext.isUserInRole("system/manager")) {
            throw new ConflictException("User not authorized to add this subscription, please contact support");
        }

        LOG.info("Add subscription# id#{}# userId#{}# accountId#{}# planId#{}#", subscription.getId(),
                 EnvironmentContext.getCurrent().getUser().getId(), subscription.getAccountId(), subscription.getPlanId());

        try {
            if (plan.isPaid() && "true".equals(newSubscriptionAttributes.getBilling().getUsePaymentSystem())) {
                newSubscriptionAttributes = paymentService.addSubscription(subscription, newSubscription.getSubscriptionAttributes());
            }
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw e;
        }
        try {
            accountDao.addSubscription(subscription);
            accountDao.saveSubscriptionAttributes(subscription.getId(), toSubscriptionAttributes(newSubscriptionAttributes));
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
            if (plan.isPaid() && "true".equals(newSubscriptionAttributes.getBilling().getUsePaymentSystem())) {
                paymentService.removeSubscription(subscription.getId());
            }
            throw e;
        }

        service.afterCreateSubscription(subscription);

        LOG.info("Added subscription. Subscription ID #{}# Account ID #{}#", subscription.getId(), subscription.getAccountId());

        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(subscription, securityContext, roles))
                       .build();
    }

    /**
     * Removes subscription by id
     *
     * @param subscriptionId
     *         id of the subscription to remove
     * @throws NotFoundException
     *         if subscription with such id is not found
     * @throws ForbiddenException
     *         if user hasn't permissions
     * @throws ServerException
     *         if internal server error occurs
     * @throws com.codenvy.api.core.ApiException
     * @see #addSubscription(NewSubscription, SecurityContext)
     * @see #getSubscriptions(String, String, SecurityContext)
     */
    @ApiOperation(value = "Remove subscription",
                  notes = "Remove subscription from account. Roles: account/owner, system/admin.",
                  position = 13)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Invalid subscription ID"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/subscriptions/{subscriptionId}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeSubscription(@ApiParam(value = "Subscription ID", required = true)
                                   @PathParam("subscriptionId") String subscriptionId, @Context SecurityContext securityContext)
            throws ApiException {
        final Subscription toRemove = accountDao.getSubscriptionById(subscriptionId);
        if (securityContext.isUserInRole("user") && !resolveRolesForSpecificAccount(toRemove.getAccountId()).contains("account/owner")) {
            throw new ForbiddenException("Access denied");
        }
        final SubscriptionAttributes attributes = accountDao.getSubscriptionAttributes(subscriptionId);

        LOG.info("Remove subscription# id#{}# userId#{}# accountId#{}#", subscriptionId, EnvironmentContext.getCurrent().getUser().getId(),
                 toRemove.getAccountId());

        if ("true".equals(attributes.getBilling().getUsePaymentSystem())) {
            try {
                paymentService.removeSubscription(subscriptionId);
            } catch (NotFoundException ignored) {
                LOG.error(ignored.getLocalizedMessage(), ignored);
            }
        }
        accountDao.removeSubscription(subscriptionId);
        accountDao.removeSubscriptionAttributes(subscriptionId);
        final SubscriptionService service = registry.get(toRemove.getServiceId());
        service.onRemoveSubscription(toRemove);
    }

    @ApiOperation(value = "Remove account",
                  notes = "Remove subscription from account. JSON with subscription details is sent. Can be performed only by system/admin.",
                  position = 14)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Invalid account ID"),
            @ApiResponse(code = 409, message = "Cannot delete account with associated workspaces"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}")
    @RolesAllowed("system/admin")
    public void remove(@ApiParam(value = "Account ID", required = true)
                       @PathParam("id") String id) throws NotFoundException, ServerException, ConflictException {
        accountDao.remove(id);
    }

    /**
     * Returns billing properties of certain subscription
     *
     * @param subscriptionId
     *         identifier of the subscription
     * @return billing properties
     * @throws NotFoundException
     *         if subscription doesn't exist or billing properties are missing
     * @throws ForbiddenException
     *         if user is not allowed to call this method
     * @throws ServerException
     */
    @ApiOperation(value = "Get subscription attributes",
                  notes = "Get attributes of a specified subscription by its ID. Roles: account/owner, system/admin.",
                  response = SubscriptionAttributesDescriptor.class,
                  position = 15)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Invalid susbscription ID"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/subscriptions/{subscriptionId}/attributes")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public SubscriptionAttributesDescriptor getSubscriptionAttributes(@ApiParam(value = "Subscription ID")
                                                                      @PathParam("subscriptionId") String subscriptionId,
                                                                      @Context SecurityContext securityContext)
            throws ServerException, NotFoundException, ForbiddenException {
        final Subscription subscription = accountDao.getSubscriptionById(subscriptionId);
        if (securityContext.isUserInRole("user") &&
            !resolveRolesForSpecificAccount(subscription.getAccountId()).contains("account/owner")) {
            throw new ForbiddenException("Access denied");
        }
        return toDescriptor(accountDao.getSubscriptionAttributes(subscriptionId));
    }

    /**
     * Validates addition of the subscription
     *
     * @param subscriptionTemplate
     *         template of the subscription
     * @return {@link com.codenvy.api.account.shared.dto.NewSubscriptionTemplate}
     * @throws NotFoundException
     *         if requested plan is not found
     * @throws ConflictException
     *         if requested subscription can't be added
     * @throws ServerException
     */
    @ApiOperation(value = "Validate new subscription",
                  notes = "This method can be used prior to adding a new subscription to an account to make sure such a subscription can be added.",
                  response = NewSubscriptionTemplate.class,
                  position = 16)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Invalid subscription ID"),
            @ApiResponse(code = 409, message = "Plan and account identifier required"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/subscriptions/validate")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public NewSubscriptionTemplate validateSubscriptionAddition(@ApiParam(value = "Subscription template", required = true)
                                                                NewSubscriptionTemplate subscriptionTemplate,
                                                                @Context SecurityContext securityContext)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException {
        if (null == subscriptionTemplate || null == subscriptionTemplate.getAccountId() || null == subscriptionTemplate.getPlanId()) {
            throw new ConflictException("Plan and account identifier required");
        }
        if (securityContext.isUserInRole("user") &&
            !resolveRolesForSpecificAccount(subscriptionTemplate.getAccountId()).contains("account/owner")) {
            throw new ForbiddenException("Access denied");
        }
        final Plan plan = planDao.getPlanById(subscriptionTemplate.getPlanId());
        final SubscriptionService service = registry.get(plan.getServiceId());
        //create new subscription
        final Subscription subscription = new Subscription().withAccountId(subscriptionTemplate.getAccountId())
                                                            .withServiceId(plan.getServiceId())
                                                            .withPlanId(plan.getId())
                                                            .withProperties(plan.getProperties());
        service.beforeCreateSubscription(subscription);

        return DtoFactory.getInstance().createDto(NewSubscriptionTemplate.class)
                         .withPlanId(subscriptionTemplate.getPlanId())
                         .withAccountId(subscriptionTemplate.getAccountId());
    }

    /**
     * Redistributes resources between workspaces
     *
     * @param id
     *         account id
     * @param updateResourcesDescriptors
     *         descriptor of resources for updating
     * @throws ForbiddenException
     *         when account hasn't permission for setting attribute in workspace
     * @throws NotFoundException
     *         when account or workspace with given id doesn't exist
     * @throws ConflictException
     *         when account hasn't required Saas subscription
     *         or user want to use more RAM than he has
     * @throws ServerException
     */
    @ApiOperation(value = "Redistributes resources",
                  notes = "Redistributes resources between workspaces. Roles: account/owner, system/admin.",
                  position = 17)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Conflict Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{id}/resources")
    @RolesAllowed({"account/owner", "system/manager", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void redistributeResources(@ApiParam(value = "Account ID", required = true)
                                      @PathParam("id") String id,
                                      @ApiParam(value = "Resources description", required = true)
                                      @Required
                                      List<UpdateResourcesDescriptor> updateResourcesDescriptors,
                                      @Context SecurityContext securityContext) throws ForbiddenException,
                                                                                       ConflictException,
                                                                                       NotFoundException,
                                                                                       ServerException {
        if (securityContext.isUserInRole("system/admin")) {
            //redistributing resources without limitation of RAM
            resourcesManager.redistributeResources(id, updateResourcesDescriptors);
            return;
        }

        //getting allowed RAM
        final List<Subscription> saasSubscriptions = accountDao.getSubscriptions(id, "Saas");
        if (saasSubscriptions.isEmpty()) {
            throw new ConflictException("Account hasn't Saas subscription");
        }
        if (saasSubscriptions.size() > 1) {
            throw new ConflictException("Account has more than 1 Saas subscription");
        }
        final Subscription saas = saasSubscriptions.get(0);

        if ("Community".equals(saas.getProperties().get("Package"))) {
            throw new ConflictException("Users who have community subscription can't distribute resources");
        }
        final int allowedRAM = (int)parseSizeToMegabytes(saas.getProperties().get("RAM"));

        resourcesManager.redistributeResources(id, allowedRAM, updateResourcesDescriptors);
    }

    /**
     * Can be used only in methods that is restricted with @RolesAllowed. Require "user" role.
     *
     * @param currentAccountId
     *         account id to resolve roles for
     * @return set of user roles
     */
    private Set<String> resolveRolesForSpecificAccount(String currentAccountId) {
        try {
            final String userId = EnvironmentContext.getCurrent().getUser().getId();
            for (Member membership : accountDao.getByMember(userId)) {
                if (membership.getAccountId().equals(currentAccountId)) {
                    return new HashSet<>(membership.getRoles());
                }
            }
        } catch (ApiException ignored) {
        }
        return Collections.emptySet();
    }

    private void validateAttributeName(String attributeName) throws ConflictException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new ConflictException(format("Attribute name '%s' is not valid", attributeName));
        }
    }

    /** Converts {@link Account} to {@link AccountDescriptor} */
    private AccountDescriptor toDescriptor(Account account, SecurityContext securityContext) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Link> links = new LinkedList<>();
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getMemberships")
                                                   .build()
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_ACCOUNTS));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getSubscriptions")
                                                   .build(account.getId())
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_SUBSCRIPTIONS));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getMembers")
                                                   .build(account.getId())
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_MEMBERS));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getById")
                                                   .build(account.getId())
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_ACCOUNT_BY_ID));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getByName")
                                                       .queryParam("name", account.getName())
                                                       .build()
                                                       .toString(),
                                             null,
                                             MediaType.APPLICATION_JSON,
                                             Constants.LINK_REL_GET_ACCOUNT_BY_NAME));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(LinksHelper.createLink(HttpMethod.DELETE,
                                             uriBuilder.clone().path(getClass(), "remove")
                                                       .build(account.getId())
                                                       .toString(),
                                             null,
                                             null,
                                             Constants.LINK_REL_REMOVE_ACCOUNT));
        }
        return DtoFactory.getInstance().createDto(AccountDescriptor.class)
                         .withId(account.getId())
                         .withName(account.getName())
                         .withAttributes(account.getAttributes())
                         .withLinks(links);
    }

    /**
     * Converts {@link Member} to {@link MemberDescriptor}
     */
    private MemberDescriptor toDescriptor(Member member, Account account, SecurityContext securityContext) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final Link removeMember = LinksHelper.createLink(HttpMethod.DELETE,
                                                         uriBuilder.clone()
                                                                   .path(getClass(), "removeMember")
                                                                   .build(account.getId(), member.getUserId())
                                                                   .toString(),
                                                         null,
                                                         null,
                                                         Constants.LINK_REL_REMOVE_MEMBER);
        final Link allMembers = LinksHelper.createLink(HttpMethod.GET,
                                                       uriBuilder.clone()
                                                                 .path(getClass(), "getMembers")
                                                                 .build(account.getId())
                                                                 .toString(),
                                                       null,
                                                       MediaType.APPLICATION_JSON,
                                                       Constants.LINK_REL_GET_MEMBERS);
        final AccountReference accountRef = DtoFactory.getInstance().createDto(AccountReference.class)
                                                      .withId(account.getId())
                                                      .withName(account.getName());
        if (member.getRoles().contains("account/owner") ||
            securityContext.isUserInRole("system/admin") ||
            securityContext.isUserInRole("system/manager")) {
            accountRef.setLinks(singletonList(LinksHelper.createLink(HttpMethod.GET,
                                                                     uriBuilder.clone()
                                                                               .path(getClass(), "getById")
                                                                               .build(account.getId())
                                                                               .toString(),
                                                                     null,
                                                                     MediaType.APPLICATION_JSON,
                                                                     Constants.LINK_REL_GET_ACCOUNT_BY_ID)));
        }
        return DtoFactory.getInstance().createDto(MemberDescriptor.class)
                         .withUserId(member.getUserId())
                         .withRoles(member.getRoles())
                         .withAccountReference(accountRef)
                         .withLinks(Arrays.asList(removeMember, allMembers));
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws ConflictException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws ConflictException {
        if (object == null) {
            throw new ConflictException(subject + " required");
        }
    }

    /**
     * Create {@link SubscriptionDescriptor} from {@link Subscription}.
     * Set with roles should be used if account roles can't be resolved with {@link SecurityContext}
     * (If there is no id of the account in the REST path.)
     *
     * @param subscription
     *         subscription that should be converted to {@link SubscriptionDescriptor}
     * @param resolvedRoles
     *         resolved roles. Do not use if id of the account presents in REST path.
     */
    private SubscriptionDescriptor toDescriptor(Subscription subscription, SecurityContext securityContext, Set<String> resolvedRoles) {
        List<Link> links = new ArrayList<>(0);
        if (!"sas-community".equals(subscription.getPlanId())) {
            final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getSubscriptionById")
                                                       .build(subscription.getId())
                                                       .toString(),
                                             null,
                                             MediaType.APPLICATION_JSON,
                                             Constants.LINK_REL_GET_SUBSCRIPTION));
            if (securityContext.isUserInRole("account/owner") || securityContext.isUserInRole("system/admin") ||
                securityContext.isUserInRole("system/manager") ||
                (securityContext.isUserInRole("user") && resolvedRoles != null && resolvedRoles.contains("account/owner"))) {
                links.add(LinksHelper.createLink(HttpMethod.DELETE,
                                                 uriBuilder.clone()
                                                           .path(getClass(), "removeSubscription")
                                                           .build(subscription.getId())
                                                           .toString(),
                                                 null,
                                                 null,
                                                 Constants.LINK_REL_REMOVE_SUBSCRIPTION));
                links.add(LinksHelper.createLink(HttpMethod.GET,
                                                 uriBuilder.clone()
                                                           .path(getClass(), "getSubscriptionAttributes")
                                                           .build(subscription.getId())
                                                           .toString(),
                                                 null,
                                                 MediaType.APPLICATION_JSON,
                                                 Constants.LINK_REL_GET_SUBSCRIPTION_ATTRIBUTES));
            }
        }
        return DtoFactory.getInstance().createDto(SubscriptionDescriptor.class)
                         .withId(subscription.getId())
                         .withAccountId(subscription.getAccountId())
                         .withServiceId(subscription.getServiceId())
                         .withProperties(subscription.getProperties())
                         .withPlanId(subscription.getPlanId())
                         .withLinks(links);
    }

    private SubscriptionAttributesDescriptor toDescriptor(SubscriptionAttributes subscriptionAttributes) {
        final Billing billing = subscriptionAttributes.getBilling();
        BillingDescriptor billingDescriptor =
                DtoFactory.getInstance().createDto(BillingDescriptor.class).withContractTerm(billing.getContractTerm())
                          .withUsePaymentSystem(billing.getUsePaymentSystem())
                          .withStartDate(billing.getStartDate())
                          .withEndDate(billing.getEndDate())
                          .withCycle(billing.getCycle())
                          .withCycleTypeDescriptor(DtoFactory.getInstance().createDto(CycleTypeDescriptor.class).withId(
                                  billing.getCycleType()));
        switch (billing.getCycleType()) {
            case 1:
                billingDescriptor.getCycleTypeDescriptor().withDescription("Auto-renew");
                break;
            case 2:
                billingDescriptor.getCycleTypeDescriptor().withDescription("One-time");
                break;
            case 3:
                billingDescriptor.getCycleTypeDescriptor().withDescription("No-renewal");
                break;
            default:
                LOG.error("Unknown billing cycle type {} is used", billing.getCycleType());
        }

        return DtoFactory.getInstance().createDto(SubscriptionAttributesDescriptor.class).withCustom(subscriptionAttributes.getCustom())
                         .withTrialDuration(subscriptionAttributes.getTrialDuration())
                         .withDescription(subscriptionAttributes.getDescription()).withStartDate(subscriptionAttributes.getStartDate())
                         .withEndDate(subscriptionAttributes.getEndDate()).withBillingDescriptor(billingDescriptor);
    }

    private SubscriptionAttributes toSubscriptionAttributes(NewSubscriptionAttributes newSubscriptionAttributes) {
        NewBilling newBilling = newSubscriptionAttributes.getBilling();
        return new SubscriptionAttributes().withDescription(newSubscriptionAttributes.getDescription())
                                           .withEndDate(newSubscriptionAttributes.getEndDate())
                                           .withStartDate(newSubscriptionAttributes.getStartDate())
                                           .withTrialDuration(newSubscriptionAttributes.getTrialDuration())
                                           .withCustom(newSubscriptionAttributes.getCustom())
                                           .withBilling(new Billing().withCycleType(newBilling.getCycleType())
                                                                     .withCycle(newBilling.getCycle())
                                                                     .withStartDate(newBilling.getStartDate())
                                                                     .withEndDate(newBilling.getEndDate())
                                                                     .withContractTerm(newBilling.getContractTerm())
                                                                     .withUsePaymentSystem(newBilling.getUsePaymentSystem()));
    }
}