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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression.DyadicOperator;

/**
 * Implementation of JPA2 Criteria "Predicate".
 */
public class PredicateImpl extends ExpressionImpl<Boolean> implements Predicate
{
    private static final long serialVersionUID = 8514831195383224552L;

    protected List<Predicate> exprs = null;

    boolean negated = false;

    BooleanOperator operator = null;

    public PredicateImpl(CriteriaBuilderImpl cb)
    {
        super(cb, Boolean.class);
        this.operator = BooleanOperator.AND;
    }

    public PredicateImpl(CriteriaBuilderImpl cb, BooleanOperator op)
    {
        super(cb, Boolean.class);
        this.operator = op;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Predicate#not()
     */
    public Predicate not()
    {
        negated = !negated;
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Predicate#getExpressions()
     */
    public List<Expression<Boolean>> getExpressions()
    {
        if (exprs == null || exprs.isEmpty())
        {
            return Collections.EMPTY_LIST;
        }
        return new ArrayList<Expression<Boolean>>(exprs);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Predicate#getOperator()
     */
    public BooleanOperator getOperator()
    {
        return operator;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Predicate#isNegated()
     */
    public boolean isNegated()
    {
        return negated;
    }

    /**
     * Convenience method to add on the supplied predicate to this one
     * @param pred The supplied predicate to append to this
     * @return The resultant predicate
     */
    public PredicateImpl append(Predicate pred)
    {
        if (exprs == null)
        {
            exprs = new ArrayList<Predicate>();
        }
        exprs.add(pred);
        return this;
    }

    /**
     * Accessor for the underlying DataNucleus query expression for this predicate.
     * @return The DataNucleus query expression
     */
    public org.datanucleus.query.expression.Expression getQueryExpression()
    {
        if (exprs != null && !exprs.isEmpty())
        {
            // Generate series of nested DyadicExpressions all using the defined operator
            DyadicOperator op = (operator == BooleanOperator.AND ?
                    org.datanucleus.query.expression.Expression.OP_AND :
                    org.datanucleus.query.expression.Expression.OP_OR);

            Iterator<Predicate> iter = exprs.iterator();
            org.datanucleus.query.expression.Expression left = null;
            org.datanucleus.query.expression.Expression right = null;
            while (iter.hasNext())
            {
                PredicateImpl pred = (PredicateImpl) iter.next();
                if (left == null)
                {
                    left = pred.getQueryExpression();
                    right = left;
                }
                else
                {
                    left = right;
                    right = new DyadicExpression(left, op, pred.getQueryExpression());
                }
            }

            if (negated)
            {
                return new DyadicExpression(org.datanucleus.query.expression.Expression.OP_NOT, right);
            }

            return right;
        }

        if (negated)
        {
            return new DyadicExpression(org.datanucleus.query.expression.Expression.OP_NOT, queryExpr);
        }
        return queryExpr;
    }

    /**
     * Method to print out the predicate as it would appear in JPQL single-string form.
     * @return The JPQL single string form of this predicate
     */
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        if (negated)
        {
            str.append("!(");
        }

        if (exprs == null || exprs.isEmpty())
        {
            // Base expression
            str.append(JPQLHelper.getJPQLForExpression(queryExpr));
        }
        else
        {
            // Sub-predicates, using the defined operator
            if (operator == BooleanOperator.OR)
            {
                str.append("(");
            }
            Iterator<Predicate> iter = exprs.iterator();
            while (iter.hasNext())
            {
                Predicate pred = iter.next();
                str.append(pred);
                if (iter.hasNext())
                {
                    str.append(operator == BooleanOperator.AND ? " AND " : " OR ");
                }
            }
            if (operator == BooleanOperator.OR)
            {
                str.append(")");
            }
        }
        if (negated)
        {
            str.append(")");
        }

        return str.toString();
    }
}