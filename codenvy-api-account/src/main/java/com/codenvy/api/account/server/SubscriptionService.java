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


import com.codenvy.api.account.server.dao.Subscription;
import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ServerException;

/**
 * Base class for any service which may communicate with account via subscriptions
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
public abstract class SubscriptionService {
    public static final String SUBSCRIPTION_LIMIT_EXHAUSTED_MESSAGE =
            "Impossible to add the subscription as current subscription list already exhaustive for this account";

    private final String serviceId;
    private final String displayName;

    /**
     * @param serviceId
     *         service identifier
     * @param displayName
     *         service name
     */
    public SubscriptionService(String serviceId, String displayName) {
        this.serviceId = serviceId;
        this.displayName = displayName;
    }

    /**
     * Should be invoked before subscription creation. It can change subscription attributes or fields
     * to prepare subscription for creating
     *
     * @param subscription
     *         subscription to prepare
     * @throws ConflictException
     *         when subscription is incompatible with system
     * @throws ServerException
     *         if internal error occurs
     */
    public abstract void beforeCreateSubscription(Subscription subscription) throws ConflictException, ServerException;

    /**
     * Should be invoked after subscription creation
     *
     * @param subscription
     *         created subscription
     * @throws ApiException
     *         when some error occurs while processing {@code subscription}
     */
    public abstract void afterCreateSubscription(Subscription subscription) throws ApiException;

    /**
     * Should be invoked after subscription removing
     *
     * @param subscription
     *         subscription that was removed
     * @throws ApiException
     *         when some error occurs while processing {@code subscription}
     */
    public abstract void onRemoveSubscription(Subscription subscription) throws ApiException;

    /**
     * Should be invoked to check subscription.
     * The one of use cases is use this method to check subscription expiration etc
     *
     * @param subscription
     *         subscription that need to be checked
     * @throws ApiException
     *         when some error occurs while checking {@code subscription}
     */
    public abstract void onCheckSubscription(Subscription subscription) throws ApiException;

    /**
     * Should be invoked after subscription update
     *
     * @param oldSubscription
     *         subscription before update
     * @param newSubscription
     *         subscription after update
     * @throws ApiException
     *         when some error occurs while processing {@code subscription}
     */
    public abstract void onUpdateSubscription(Subscription oldSubscription, Subscription newSubscription) throws ApiException;

    /**
     * @return service identifier
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * @return service name
     */
    public String getDisplayName() {
        return displayName;
    }
}
