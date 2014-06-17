/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.datanucleus.query.expression.DyadicExpression;

/**
 * Representation of an IN expression, obtained from "QueryBuilder.in".
 * @param <X> type of the (member) expression that this is the IN expression for.
 */
public class InPredicate<X> extends PredicateImpl implements In<X>
{
    private static final long serialVersionUID = -831538482168317142L;

    ExpressionImpl<? extends X> expr;

    List<Expression<? extends X>> values;

    boolean negated = false;

    public InPredicate(CriteriaBuilderImpl cb, Expression<? extends X> expr)
    {
        super(cb);
        this.expr = (ExpressionImpl<? extends X>) expr;
    }

    public InPredicate(CriteriaBuilderImpl cb, Expression<? extends X> expr, X... values)
    {
        this(cb, expr);
        for (int i=0;i<values.length;i++)
        {
            value(values[i]);
        }
    }

    public InPredicate(CriteriaBuilderImpl cb, Expression<? extends X> expr, List<Expression<? extends X>> values)
    {
        this(cb, expr);
        this.values = values;
    }

    public InPredicate(CriteriaBuilderImpl cb, Expression<? extends X> expr, Expression<? extends X>... values)
    {
        this(cb, expr, Arrays.asList(values));
    }

    public InPredicate(CriteriaBuilderImpl cb, Expression<? extends X> expr, Collection<X> values)
    {
        this(cb, expr);
        for (X val : values)
        {
            value(val);
        }
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Predicate#getOperator()
     */
    public BooleanOperator getOperator()
    {
        return BooleanOperator.AND;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Predicate#isNegated()
     */
    public boolean isNegated()
    {
        return negated;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Predicate#getExpressions()
     */
    public List<Expression<Boolean>> getExpressions()
    {
        return Collections.EMPTY_LIST;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Predicate#not()
     */
    public Predicate not()
    {
        negated = !negated;
        return this;
    }

    public Expression<X> getExpression()
    {
        return (Expression<X>) expr;
    }

    public In<X> value(X value)
    {
        if (values == null)
        {
            values = new ArrayList<Expression<? extends X>>();
        }
        ExpressionImpl<X> litExpr = new LiteralExpression<X>(cb, value);
        values.add(litExpr);
        queryExpr = null; // Reset it for recalculation

        return this;
    }

    public In<X> value(Expression<? extends X> value)
    {
        if (values == null)
        {
            values = new ArrayList<Expression<? extends X>>();
        }
        values.add(value);
        queryExpr = null; // Reset it for recalculation

        return this;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.criteria.ExpressionImpl#getQueryExpression()
     */
    @Override
    public org.datanucleus.query.expression.Expression getQueryExpression()
    {
        if (queryExpr == null)
        {
            if (values == null || values.isEmpty())
            {
                return null;
            }

            // Generate the query expression
            DyadicExpression dyExpr = null;
            for (Expression valExpr : values)
            {
                DyadicExpression valDyExpr = new DyadicExpression(expr.getQueryExpression(), org.datanucleus.query.expression.Expression.OP_EQ, 
                    ((ExpressionImpl)valExpr).getQueryExpression());
                if (dyExpr == null)
                {
                    dyExpr = valDyExpr;
                }
                else
                {
                    dyExpr = new DyadicExpression(dyExpr, org.datanucleus.query.expression.Expression.OP_OR, valDyExpr);
                }
            }
            queryExpr = dyExpr;
        }
        return queryExpr;
    }

    /**
     * Method to print out the predicate as it would appear in JPQL single-string form.
     * Will be of the form <pre>field IN (val1,val2[,val3])</pre>.
     * @return The JPQL single string form of this predicate
     */
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        if (negated)
        {
            str.append("!(");
        }

        str.append(JPQLHelper.getJPQLForExpression(expr.getQueryExpression())).append(" IN (");
        boolean firstValue = true;
        for (Expression valExpr : values)
        {
            if (!firstValue)
            {
                str.append(",");
            }
            str.append(valExpr.toString());
            firstValue = false;
        }
        str.append(")");

        if (negated)
        {
            str.append(")");
        }

        return str.toString();
    }
}