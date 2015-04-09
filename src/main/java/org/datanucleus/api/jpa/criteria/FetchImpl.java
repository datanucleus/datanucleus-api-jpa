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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.datanucleus.api.jpa.metamodel.AttributeImpl;
import org.datanucleus.api.jpa.metamodel.SingularAttributeImpl;

/**
 * Implementation of JPA Criteria "Fetch".
 * @param <Z> type from which joining
 * @param <X> type of the attribute being joined
 */
public class FetchImpl<Z, X> extends PathImpl<Z, X> implements Fetch<Z, X>
{
    private static final long serialVersionUID = 1615652791594292349L;
    protected Set<Fetch<X,?>> fetches;
    protected JoinType joinType;

    /**
     * Constructor for a fetch join to an entity.
     * @param cb Criteria Builder
     * @param parent Parent object
     * @param attr The type joining to
     * @param joinType Type of join (Inner/LeftOuter/RightOuter)
     */
    public FetchImpl(CriteriaBuilderImpl cb, PathImpl<?, Z> parent, AttributeImpl<? super Z, X> attr, JoinType joinType)
    {
        super(cb, parent, attr, attr.getJavaType());
        this.joinType = joinType;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.FetchParent#fetch(javax.persistence.metamodel.PluralAttribute, javax.persistence.criteria.JoinType)
     */
    public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> attr, JoinType type)
    {
        Fetch<X, Y> fetch = new FetchImpl<X, Y>(cb, this, (AttributeImpl<? super X, Y>) attr, joinType);
        if (fetches == null)
        {
            fetches = new HashSet<Fetch<X, ?>>();
        }
        fetches.add(fetch);
        return fetch;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.FetchParent#fetch(javax.persistence.metamodel.PluralAttribute)
     */
    public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> attr)
    {
        return fetch(attr, JoinType.INNER);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.FetchParent#fetch(javax.persistence.metamodel.SingularAttribute, javax.persistence.criteria.JoinType)
     */
    public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> attr, JoinType type)
    {
        Fetch<X, Y> fetch = new FetchImpl<X, Y>(cb, this, (SingularAttributeImpl<X, Y>) attr, joinType);
        if (fetches == null)
        {
            fetches = new HashSet<Fetch<X, ?>>();
        }
        fetches.add(fetch);
        return fetch;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.FetchParent#fetch(javax.persistence.metamodel.SingularAttribute)
     */
    public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> attr)
    {
        return fetch(attr, JoinType.INNER);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.FetchParent#fetch(java.lang.String, javax.persistence.criteria.JoinType)
     */
    @SuppressWarnings("hiding")
    public <X, Y> Fetch<X, Y> fetch(String attrName, JoinType type)
    {
        ManagedType clsType = cb.getEntityManagerFactory().getMetamodel().managedType(getJavaType());
        Attribute attr = clsType.getAttribute(attrName);
        if (attr instanceof PluralAttribute)
        {
            return fetch((PluralAttribute)attr, type);
        }
        else if (attr instanceof SingularAttribute)
        {
            return fetch((SingularAttribute)attr, type);
        }
        throw new IllegalArgumentException("Attempt to fetch attribute " + attrName + " but attribute was " + attr);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.FetchParent#fetch(java.lang.String)
     */
    @SuppressWarnings("hiding")
    public <X, Y> Fetch<X, Y> fetch(String attrName)
    {
        return fetch(attrName, JoinType.INNER);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.FetchParent#getFetches()
     */
    public Set<Fetch<X, ?>> getFetches()
    {
        if (fetches == null)
        {
            return Collections.EMPTY_SET;
        }
        return fetches;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Fetch#getAttribute()
     */
    public Attribute<? super Z, ?> getAttribute()
    {
        return attribute;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Fetch#getJoinType()
     */
    public JoinType getJoinType()
    {
        return joinType;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Fetch#getParent()
     */
    public FetchParent<?, Z> getParent()
    {
        return (FetchParent<?, Z>) parent;
    }
}