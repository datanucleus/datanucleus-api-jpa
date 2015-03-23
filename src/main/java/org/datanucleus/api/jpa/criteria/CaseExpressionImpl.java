/**********************************************************************
Copyright (c) 2015 Andy Jefferson and others. All rights reserved.
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

import javax.persistence.criteria.CriteriaBuilder.Case;
import javax.persistence.criteria.Expression;

import org.datanucleus.query.expression.CaseExpression;
import org.datanucleus.query.expression.Literal;

/**
 * Implementation of JPA Case expression.
 * @param <R> The type of the result
 */
public class CaseExpressionImpl<R> extends ExpressionImpl<R> implements Case<R>
{
    private static final long serialVersionUID = -3491352539051873014L;

    public CaseExpressionImpl(CriteriaBuilderImpl cb)
    {
        super(cb, null);
        queryExpr = new CaseExpression();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.Case#when(javax.persistence.criteria.Expression, java.lang.Object)
     */
    @Override
    public Case<R> when(Expression<Boolean> condition, R result)
    {
        org.datanucleus.query.expression.Expression condQueryExpr = ((ExpressionImpl)condition).getQueryExpression();
        ((CaseExpression)queryExpr).addCondition(condQueryExpr, new Literal(result));
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.Case#when(javax.persistence.criteria.Expression, javax.persistence.criteria.Expression)
     */
    @Override
    public Case<R> when(Expression<Boolean> condition, Expression<? extends R> result)
    {
        org.datanucleus.query.expression.Expression condQueryExpr = ((ExpressionImpl)condition).getQueryExpression();
        org.datanucleus.query.expression.Expression resultExpr = ((ExpressionImpl)result).getQueryExpression();
        ((CaseExpression)queryExpr).addCondition(condQueryExpr, resultExpr);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.Case#otherwise(java.lang.Object)
     */
    @Override
    public Expression<R> otherwise(R result)
    {
        ((CaseExpression)queryExpr).setElseExpression(new Literal(result));
        return this; // Correct?
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.Case#otherwise(javax.persistence.criteria.Expression)
     */
    @Override
    public Expression<R> otherwise(Expression<? extends R> result)
    {
        ExpressionImpl resultExpr = (ExpressionImpl) result;
        ((CaseExpression)queryExpr).setElseExpression(resultExpr.getQueryExpression());
        return this; // Correct?
    }
}