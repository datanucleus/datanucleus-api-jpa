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

import javax.persistence.criteria.Expression;

import org.datanucleus.store.query.JPQLQueryHelper;
import org.datanucleus.store.query.expression.DyadicExpression;

/**
 * Representation of a CONCAT of two expressions.
 */
public class ConcatExpression extends ExpressionImpl<String>
{
    private static final long serialVersionUID = 1077351002419048933L;
    Expression<String> expr1;
    Expression<String> expr2;

    public ConcatExpression(CriteriaBuilderImpl cb, Expression<String> expr1, Expression<String> expr2)
    {
        super(cb, String.class);
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.criteria.ExpressionImpl#getQueryExpression()
     */
    @Override
    public org.datanucleus.store.query.expression.Expression getQueryExpression()
    {
        if (queryExpr == null)
        {
            queryExpr = new DyadicExpression(((ExpressionImpl)expr1).getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_ADD,
                ((ExpressionImpl)expr2).getQueryExpression());
        }
        return queryExpr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.criteria.ExpressionImpl#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("CONCAT(");
        str.append(JPQLQueryHelper.getJPQLForExpression(((ExpressionImpl)expr1).getQueryExpression()));
        str.append(",");
        str.append(JPQLQueryHelper.getJPQLForExpression(((ExpressionImpl)expr2).getQueryExpression()));
        str.append(")");
        return str.toString();
    }
}