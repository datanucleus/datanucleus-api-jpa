/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
   ...
**********************************************************************/
package org.datanucleus.api.jpa.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.Selection;

/**
 * Implementation of JPA2 Criteria "CompoundSelection".
 */
public class CompoundSelectionImpl<X> extends ExpressionImpl<X> implements CompoundSelection<X>
{
    private final List<Selection<?>> args;

    public CompoundSelectionImpl(CriteriaBuilderImpl cb, Class<X> cls, Selection<?>...args) 
    {
        super(cb, cls);
        this.args = args == null ? (List<Selection<?>>)Collections.EMPTY_LIST : Arrays.asList(args);
    }

    public CompoundSelectionImpl(CriteriaBuilderImpl cb, Class<X> cls, List<Selection<?>> args) 
    {
        super(cb, cls);
        this.args = args == null ? (List<Selection<?>>)Collections.EMPTY_LIST : args;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.criteria.ExpressionImpl#getCompoundSelectionItems()
     */
    @Override
    public List<Selection<?>> getCompoundSelectionItems()
    {
        List<Selection<?>> items = new ArrayList<Selection<?>>();
        items.addAll(args);
        return items;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.criteria.ExpressionImpl#isCompoundSelection()
     */
    @Override
    public boolean isCompoundSelection()
    {
        return true;
    }
}