/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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
package org.exoplatform.ide.vfs.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Set of abstract items for paging view.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 */
public class ItemListImpl implements ItemList {
    /**
     * Total number of items.
     *
     * @see #getNumItems()
     */
    private int numItems = -1;

    /** Has more items in result set. */
    private boolean hasMoreItems;

    /** Current range of items. */
    private List<Item> list;

    public ItemListImpl() {
    }

    /**
     * @param list
     *         the list of items.
     */
    public ItemListImpl(List<Item> list) {
        this.list = list;
    }

    /** @return set of items */
    @Override
    public List<Item> getItems() {
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    @Override
    public void setItems(List<Item> list) {
        this.list = list;
    }

    @Override
    public int getNumItems() {
        return numItems;
    }

    @Override
    public void setNumItems(int numItems) {
        this.numItems = numItems;
    }

    @Override
    public boolean isHasMoreItems() {
        return hasMoreItems;
    }

    @Override
    public void setHasMoreItems(boolean hasMoreItems) {
        this.hasMoreItems = hasMoreItems;
    }
}