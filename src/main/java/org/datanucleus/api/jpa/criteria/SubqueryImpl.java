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

import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.datanucleus.query.expression.VariableExpression;

/**
 * Implementation of JPA2 Criteria "Subquery".
 * A subquery in DataNucleus is represented as a variable (as it is in JDOQL), consequently this
 * expression is backed by a VariableExpression.
 */
public class SubqueryImpl<T> extends ExpressionImpl<T> implements Subquery<T>
{
    private static final long serialVersionUID = 6197187043693743756L;

    protected CriteriaQueryImpl<?> parent;

    protected CriteriaQueryImpl<T> delegate;

    private Set<Join<?,?>> correlatedJoins = null; 

    /** Random number generator, for use in naming subqueries */
    public static final Random random = new Random();

    public SubqueryImpl(CriteriaBuilderImpl cb, Class<T> type, CriteriaQuery<?> query)
    {
        super(cb, type);
        this.parent = (CriteriaQueryImpl<?>) query;
        this.delegate = new CriteriaQueryImpl<T>(cb, type);
        String variableName = "SUB" + random.nextInt();
        this.queryExpr = new VariableExpression(variableName);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#getCorrelatedJoins()
     */
    public Set<Join<?,?>> getCorrelatedJoins()
    {
        return correlatedJoins;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#getParent()
     */
    public AbstractQuery<?> getParent()
    {
        return parent;
    }

    /**
     * Convenience accessor for the delegate criteria query that provides this subquery.
     * @return The delegate
     */
    public CriteriaQuery<?> getDelegate()
    {
        return delegate;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#correlate(javax.persistence.criteria.CollectionJoin)
     */
    public <X, Y> CollectionJoin<X, Y> correlate(CollectionJoin<X, Y> arg0)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#correlate(javax.persistence.criteria.Join)
     */
    public <X, Y> Join<X, Y> correlate(Join<X, Y> arg0)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#correlate(javax.persistence.criteria.ListJoin)
     */
    public <X, Y> ListJoin<X, Y> correlate(ListJoin<X, Y> arg0)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#correlate(javax.persistence.criteria.MapJoin)
     */
    public <X, K, V> MapJoin<X, K, V> correlate(MapJoin<X, K, V> arg0)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#correlate(javax.persistence.criteria.Root)
     */
    public <Y> Root<Y> correlate(Root<Y> arg0)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#correlate(javax.persistence.criteria.SetJoin)
     */
    public <X, Y> SetJoin<X, Y> correlate(SetJoin<X, Y> arg0)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#distinct(boolean)
     */
    public Subquery<T> distinct(boolean flag)
    {
        delegate.distinct(flag);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#isDistinct()
     */
    public boolean isDistinct()
    {
        return delegate.isDistinct();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getResultType()
     */
    public Class<T> getResultType()
    {
        return (Class<T>) getJavaType();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#select(javax.persistence.criteria.Expression)
     */
    public Subquery<T> select(Expression<T> expr)
    {
        delegate.select(expr);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#getSelection()
     */
    public Expression<T> getSelection()
    {
        return (Expression<T>) delegate.getSelection();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#from(java.lang.Class)
     */
    public <X> Root<X> from(Class<X> cls)
    {
        return delegate.from(cls);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#from(javax.persistence.metamodel.EntityType)
     */
    public <X> Root<X> from(EntityType<X> type)
    {
        return delegate.from(type);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getRestriction()
     */
    public Predicate getRestriction()
    {
        return delegate.getRestriction();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getRoots()
     */
    public Set<Root<?>> getRoots()
    {
        return delegate.getRoots();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#where(javax.persistence.criteria.Expression)
     */
    public Subquery<T> where(Expression<Boolean> expr)
    {
        delegate.where(expr);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#where(javax.persistence.criteria.Predicate[])
     */
    public Subquery<T> where(Predicate... exprs)
    {
        delegate.where(exprs);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#groupBy(javax.persistence.criteria.Expression<?>[])
     */
    public Subquery<T> groupBy(Expression<?>... exprs)
    {
        delegate.groupBy(exprs);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#groupBy(java.util.List)
     */
    public Subquery<T> groupBy(List<Expression<?>> exprs)
    {
        delegate.groupBy(exprs);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getGroupList()
     */
    public List<Expression<?>> getGroupList()
    {
        return delegate.getGroupList();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getGroupRestriction()
     */
    public Predicate getGroupRestriction()
    {
        return delegate.getGroupRestriction();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#having(javax.persistence.criteria.Expression)
     */
    public Subquery<T> having(Expression<Boolean> expr)
    {
        delegate.having(expr);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Subquery#having(javax.persistence.criteria.Predicate[])
     */
    public Subquery<T> having(Predicate... exprs)
    {
        delegate.having(exprs);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#subquery(java.lang.Class)
     */
    public <U> Subquery<U> subquery(Class<U> type)
    {
        return delegate.subquery(type);
    }

    public String toString()
    {
        return delegate.toString();
    }

    public CommonAbstractCriteria getContainingQuery()
    {
        return parent;
    }
}