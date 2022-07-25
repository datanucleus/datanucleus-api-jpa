/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.CriteriaBuilder.Coalesce;

import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.Literal;

/**
 * Implementation of JPA Criteria "Coalesce".
 */
public class CoalesceExpression<T> extends ExpressionImpl<T> implements Coalesce<T>
{
    private static final long serialVersionUID = -2906713117803554462L;

    public CoalesceExpression(CriteriaBuilderImpl cb, Class<? extends T> cls) 
    {
        super(cb, cls);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.QueryBuilder.Coalesce#value(javax.persistence.criteria.Expression)
     */
    public Coalesce<T> value(Expression<? extends T> expr)
    {
        List args = null;
        if (queryExpr != null)
        {
            args = ((InvokeExpression)queryExpr).getArguments();
        }
        else
        {
            args = new ArrayList();
        }
        args.add(expr);
        queryExpr = new InvokeExpression(null, "COALESCE", args);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.QueryBuilder.Coalesce#value(java.lang.Object)
     */
    public Coalesce<T> value(T val)
    {
        List args = null;
        if (queryExpr != null)
        {
            args = ((InvokeExpression)queryExpr).getArguments();
        }
        else
        {
            args = new ArrayList();
        }
        args.add(new Literal(val));
        queryExpr = new InvokeExpression(null, "COALESCE", args);
        return this;
    }
}