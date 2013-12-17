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
package com.codenvy.deploy;

import com.codenvy.api.builder.BuildQueue;
import com.codenvy.api.builder.BuilderService;
import com.codenvy.inject.DynaModule;
import com.google.inject.Binder;
import com.google.inject.Module;

/** @author andrew00x */
@DynaModule
public class TestModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(BuilderService.class);
        binder.bind(BuildQueue.class);
    }
}
