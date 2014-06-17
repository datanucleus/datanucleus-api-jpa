/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.util.Collection;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.PluralJoin;
import javax.persistence.metamodel.PluralAttribute;

import org.datanucleus.api.jpa.metamodel.PluralAttributeImpl;

/**
 * Implementation of JPA2 Criteria "PluralJoin".
 * 
 * @param <Z> The source type
 * @param <C> The collection type
 * @param <E> The element type of the collection
 */
public class PluralJoinImpl<Z, C, E> extends JoinImpl<Z, E> implements PluralJoin<Z, C, E>
{
    private static final long serialVersionUID = 1700153438199489689L;

    /**
     * Constructor
     * @param cb Criteria Builder
     * @param parent Parent component
     * @param attr The attribute being joined to
     * @param joinType Type of join
     */
    public PluralJoinImpl(CriteriaBuilderImpl cb, FromImpl parent, PluralAttributeImpl<? super Z, Collection<E>, E> attr, JoinType joinType)
    {
        super(cb, parent, attr, joinType);
    }

    public PluralAttribute<? super Z, C, E> getModel()
    {
        return (PluralAttribute<? super Z, C, E>) attribute.getType();
    }
}