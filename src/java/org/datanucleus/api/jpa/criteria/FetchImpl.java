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

import java.util.Set;

import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.datanucleus.api.jpa.metamodel.AttributeImpl;

/**
 * Implementation of JPA2 Criteria "Fetch".
 *
 * @param <Z> type from which joining
 * @param <X> type of the attribute being joined
 */
public class FetchImpl<Z, X> extends PathImpl<Z, X> implements Fetch<Z, X>
{
    protected Set<Fetch<X,?>> fetches;
    protected JoinType joinType;

    /**
     * Constructor for a fetch join to an entity.
     * @param parent Parent object
     * @param attr The type joining to
     * @param joinType Type of join (Inner/LeftOuter/RightOuter)
     */
    public FetchImpl(CriteriaBuilderImpl cb, FromImpl<?, Z> parent, AttributeImpl<? super Z, X> attr, JoinType joinType)
    {
        super(cb, parent, attr, (Class<X>)attr.getJavaType());
        this.joinType = joinType;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.FetchParent#fetch(javax.persistence.metamodel.PluralAttribute, javax.persistence.criteria.JoinType)
     */
    public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> attr, JoinType type)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet supported");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet supported");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet supported");
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
        return fetches;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Fetch#getAttribute()
     */
    public Attribute<? super Z, ?> getAttribute()
    {
        return (Attribute<? super Z, ?>)attribute;
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