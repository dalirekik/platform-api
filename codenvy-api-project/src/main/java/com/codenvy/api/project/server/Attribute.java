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
package com.codenvy.api.project.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Attribute2 of Project.
 *
 * @author andrew00x
 * @see ValueProvider
 *
 * @deprecated
 */
public class Attribute {

    private final String        name;
    private final ValueProvider valueProvider;

    /**
     * Creates new Attribute2 with specified {@code name} and use specified {@code ValueProvider2} to reading and updating value of this
     * Attribute2.
     *
     * @throws IllegalArgumentException
     *         If {@code name} is {@code null} or empty
     */
    public Attribute(String name, ValueProvider valueProvider) {
        if (name == null) {
            throw new IllegalArgumentException("Null name is not allowed. ");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name may not be empty. ");
        }
        this.name = name;
        this.valueProvider = valueProvider;
    }

    /** Creates new Attribute2. */
    public Attribute(String name, String value) {
        this(name, new DefaultValueProvider(value));
    }

    /** Creates new Attribute2. */
    public Attribute(String name, List<String> values) {
        this(name, new DefaultValueProvider(values));
    }

    /** Creates new Attribute2. */
    public Attribute(String name, String... values) {
        this(name, new DefaultValueProvider(values));
    }

    /** Copy constructor. */
    public Attribute(Attribute origin) throws ValueStorageException {
        this(origin.getName(), new DefaultValueProvider(origin.getValues()));
    }

    /** Get name of this attribute. */
    public final String getName() {
        return name;
    }

    /**
     * Get single value of attribute. If attribute has more than one value this method returns only first value.
     * <p/>
     * This method is shortcut for:
     * <pre>
     *    String name = ...
     *    Attribute2 attribute = ...;
     *    List&lt;String&gt; values = attribute.getValues();
     *    if (values != null && !values.isEmpty()) {
     *       return values.get(0);
     *    }
     *    return null;
     * </pre>
     *
     * @return current value of attribute
     */
    public final String getValue() throws ValueStorageException {
        final List<String> values = valueProvider.getValues(name);
        return !(values == null || values.isEmpty()) ? values.get(0) : null;
    }

    /**
     * Get all values of attribute. Modifications to the returned {@code List} will not affect the internal {@code List}.
     *
     * @return current value of attribute
     */
    public final List<String> getValues() throws ValueStorageException {
        final List<String> values = valueProvider.getValues(name);
        return values == null ? new ArrayList<String>(0) : new ArrayList<>(values);
    }

    /**
     * Set single value of attribute.
     *
     * @param value
     *         new value of attribute
     */
    public final void setValue(String value) throws ValueStorageException, InvalidValueException {
        if (value != null) {
            final List<String> list = new ArrayList<>(1);
            list.add(value);
            valueProvider.setValues(name, list);
        } else {
            valueProvider.setValues(name, null);
        }
    }

    /**
     * Set values of attribute.
     *
     * @param values
     *         new value of attribute
     */
    public final void setValues(List<String> values) throws ValueStorageException, InvalidValueException {
        if (values != null) {
            valueProvider.setValues(name, new ArrayList<>(values));
        } else {
            valueProvider.setValues(name, null);
        }
    }

    /**
     * Set values of attribute.
     *
     * @param values
     *         new value of attribute
     */
    public final void setValues(String... values) throws InvalidValueException, ValueStorageException {
        if (values != null) {
            final List<String> list = new ArrayList<>();
            Collections.addAll(list, values);
            valueProvider.setValues(name, list);
        } else {
            valueProvider.setValues(name, null);
        }
    }
}
