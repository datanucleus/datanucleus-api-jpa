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

import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.Literal;

/**
 * Representation of a Literal in a criteria query.
 */
public class LiteralExpression<X> extends ExpressionImpl
{
    X value;

    public LiteralExpression(CriteriaBuilderImpl cb, X value)
    {
        super(cb, value.getClass());
        this.value = value;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.criteria.ExpressionImpl#getQueryExpression()
     */
    @Override
    public Expression getQueryExpression()
    {
        if (queryExpr == null)
        {
            queryExpr = new Literal(value);
        }
        return queryExpr;
    }

    public String toString()
    {
        if (value instanceof String || value instanceof Character)
        {
            return "'" + value.toString() + "'";
        }
        else if (value instanceof Boolean)
        {
            return ((Boolean)value ? "TRUE" : "FALSE");
        }
        else
        {
            return "" + value;
        }
    }
}