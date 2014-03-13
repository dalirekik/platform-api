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
package com.codenvy.api.organization.server;

import com.codenvy.api.organization.shared.dto.Subscription;

/**
 * Base interface for all subscription handlers
 *
 * @author Eugene Voevodin
 * @see SubscriptionService
 */
public interface SubscriptionHandler {

    void onCreateSubscription(Subscription subscription);

    void onRemoveSubscription(Subscription subscription);

    void onCheckSubscription(Subscription subscription, String property);
}