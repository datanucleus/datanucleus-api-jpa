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

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;

import org.datanucleus.store.query.expression.OrderExpression;

/**
 * Implementation of JPA Criteria "Order".
 */
public class OrderImpl implements Order
{
    Expression<?> expr;

    boolean ascending = true;

    private enum NullPrecedence {NULLS_FIRST, NULLS_LAST}

    NullPrecedence nullPrecedence = null;

    /**
     * Constructor for an Order.
     * @param expr The expression
     * @param asc Whether it is ascending
     */
    public OrderImpl(Expression<?> expr, boolean asc)
    {
        this.expr = expr;
        this.ascending = asc;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Order#getExpression()
     */
    public Expression<?> getExpression()
    {
        return expr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Order#isAscending()
     */
    public boolean isAscending()
    {
        return ascending;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Order#reverse()
     */
    public Order reverse()
    {
        ascending = !ascending;
        return this;
    }

    public Order nullsFirst()
    {
        this.nullPrecedence = NullPrecedence.NULLS_FIRST;
        return this;
    }

    public Order nullsLast()
    {
        this.nullPrecedence = NullPrecedence.NULLS_LAST;
        return this;
    }

    /**
     * Method to return the underlying DataNucleus query expression that this equates to.
     * @return The order Expression
     */
    public org.datanucleus.store.query.expression.OrderExpression getQueryExpression()
    {
        OrderExpression orderExpr = null;
        if (nullPrecedence == null)
        {
            orderExpr = new OrderExpression(((ExpressionImpl)expr).getQueryExpression(), ascending ? "ascending" : "descending");
        }
        else
        {
            orderExpr = new OrderExpression(((ExpressionImpl)expr).getQueryExpression(), ascending ? "ascending" : "descending", 
                    nullPrecedence == NullPrecedence.NULLS_FIRST ? "nulls first" : "nulls last");
        }
        
        return orderExpr;
    }

    /**
     * Method to return the JPQL single-string that this equates to.
     * @return The JPQL single-string form of this order
     */
    public String toString()
    {
        return expr.toString() + " " + (ascending ? "ASC" : "DESC") + (nullPrecedence != null ? ((nullPrecedence == NullPrecedence.NULLS_FIRST) ? " NULLS FIRST" : " NULLS LAST") : "");
    }
}