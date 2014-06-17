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

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Predicate.BooleanOperator;
import javax.persistence.metamodel.Attribute;

import org.datanucleus.api.jpa.metamodel.PluralAttributeImpl;
import org.datanucleus.api.jpa.metamodel.SingularAttributeImpl;

/**
 * Implementation of JPA2 Criteria "Join".
 * 
 * @param <Z> type from which joining
 * @param <X> type of the attribute being joined
 */
public class JoinImpl<Z, X> extends FromImpl<Z, X> implements Join<Z, X>
{
    private static final long serialVersionUID = 6370220293015162441L;

    private final JoinType joinType;

    private Predicate onExpr;

    /**
     * Constructor for a join to an entity (1-1, N-1 relations).
     * @param cb Criteria Builder
     * @param parent Parent object
     * @param attr The type joining to
     * @param joinType Type of join (Inner/LeftOuter/RightOuter)
     */
    public JoinImpl(CriteriaBuilderImpl cb, FromImpl<?, Z> parent, SingularAttributeImpl<Z, X> attr, JoinType joinType)
    {
        super(cb, parent, attr);
        this.joinType = joinType;
    }

    /**
     * Constructor for a join to a collection (1-N, M-N relations).
     * @param cb Criteria Builder
     * @param parent Parent object
     * @param attr The type joining to
     * @param joinType Type of join (Inner/LeftOuter/RightOuter)
     */
    public JoinImpl(CriteriaBuilderImpl cb, FromImpl<?, Z> parent, PluralAttributeImpl<? super Z, Collection<X>, X> attr, JoinType joinType)
    {
        super(cb, parent, attr);
        this.joinType = joinType;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Join#getAttribute()
     */
    public Attribute<? super Z, ?> getAttribute()
    {
        return attribute;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Join#getJoinType()
     */
    public JoinType getJoinType()
    {
        return joinType;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Join#getParent()
     */
    public From<?, Z> getParent()
    {
        return (FromImpl<?, Z>)parent;
    }

    public Predicate getOn()
    {
        return onExpr;
    }

    public Join<Z, X> on(Expression<Boolean> restriction)
    {
        onExpr = new PredicateImpl(cb, BooleanOperator.AND);
        ((PredicateImpl)onExpr).append((Predicate)restriction);

        return this;
    }

    public Join<Z, X> on(Predicate... restrictions)
    {
        onExpr = new PredicateImpl(cb, BooleanOperator.AND);
        for (int i=0;i<restrictions.length;i++)
        {
            ((PredicateImpl)onExpr).append(restrictions[i]);
        }

        return this;
    }
}