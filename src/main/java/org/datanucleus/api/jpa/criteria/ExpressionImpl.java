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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Selection;

import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Literal;

/**
 * Implementation of JPA2 Criteria "Expression".
 */
public class ExpressionImpl<T> implements Expression<T>, Serializable
{
    static final long serialVersionUID = -9180595377551709140L;

    protected CriteriaBuilderImpl cb;
    private final Class<T> cls;
    private String alias;

    /** The underlying (generic) query expression. */
    org.datanucleus.query.expression.Expression queryExpr;

    public ExpressionImpl(CriteriaBuilderImpl cb, Class<T> cls)
    {
        this.cb = cb;
        this.cls = cls;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Expression#as(java.lang.Class)
     */
    public <X> Expression<X> as(Class<X> cls)
    {
        ExpressionImpl<X> expr = new ExpressionImpl<X>(cb, cls);
        expr.queryExpr = queryExpr;
        return expr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Expression#in(java.util.Collection)
     */
    public Predicate in(Collection<?> values)
    {
        return cb.in(this, values.toArray());
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Expression#in(javax.persistence.criteria.Expression<?>[])
     */
    public Predicate in(Expression<?>... values)
    {
        return cb.in(this, (Expression[])values);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Expression#in(javax.persistence.criteria.Expression)
     */
    public Predicate in(Expression<Collection<?>> values)
    {
        return cb.in(this, values);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Expression#in(java.lang.Object[])
     */
    public Predicate in(Object... values)
    {
        return cb.in(this, values);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Expression#isNotNull()
     */
    public Predicate isNotNull()
    {
        PredicateImpl pred = new PredicateImpl(cb);
        Literal lit = new Literal(null);
        org.datanucleus.query.expression.Expression queryExpr =
            new DyadicExpression(getQueryExpression(), org.datanucleus.query.expression.Expression.OP_NOTEQ, lit);
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Expression#isNull()
     */
    public Predicate isNull()
    {
        PredicateImpl pred = new PredicateImpl(cb);
        Literal lit = new Literal(null);
        org.datanucleus.query.expression.Expression queryExpr =
            new DyadicExpression(this.getQueryExpression(), org.datanucleus.query.expression.Expression.OP_EQ, lit);
        pred.queryExpr = queryExpr;
        return pred;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Selection#alias(java.lang.String)
     */
    public Selection<T> alias(String alias)
    {
        this.alias = alias;
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Selection#getCompoundSelectionItems()
     */
    public List<Selection<?>> getCompoundSelectionItems()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Selection#isCompoundSelection()
     */
    public boolean isCompoundSelection()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see javax.persistence.TupleElement#getAlias()
     */
    public String getAlias()
    {
        return alias;
    }

    /* (non-Javadoc)
     * @see javax.persistence.TupleElement#getJavaType()
     */
    public Class<? extends T> getJavaType()
    {
        return cls;
    }

    /**
     * Accessor for the underlying (generic) query expression.
     * @return The query expression
     */
    public org.datanucleus.query.expression.Expression getQueryExpression()
    {
        return queryExpr;
    }

    /**
     * Method to print out the expression as it would appear in JPQL single-string form.
     * @return The JPQL single string form of this expression
     */
    public String toString()
    {
        return JPQLHelper.getJPQLForExpression(queryExpr);
    }
}