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

import javax.persistence.criteria.CriteriaBuilder.SimpleCase;
import javax.persistence.criteria.Expression;

/**
 * Implementation of JPA SimpleCase expression.
 * @param <C>
 * @param <R>
 */
public class SimpleCaseExpressionImpl<C, R> extends ExpressionImpl<R> implements SimpleCase<C, R>
{
    private static final long serialVersionUID = 6512810442458077049L;

    public SimpleCaseExpressionImpl(CriteriaBuilderImpl cb)
    {
        super(cb, null);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.SimpleCase#getExpression()
     */
    @Override
    public Expression<C> getExpression()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.SimpleCase#when(java.lang.Object, java.lang.Object)
     */
    @Override
    public SimpleCase<C, R> when(C condition, R result)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.SimpleCase#when(java.lang.Object, javax.persistence.criteria.Expression)
     */
    @Override
    public SimpleCase<C, R> when(C condition, Expression<? extends R> result)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.SimpleCase#otherwise(java.lang.Object)
     */
    @Override
    public Expression<R> otherwise(R result)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaBuilder.SimpleCase#otherwise(javax.persistence.criteria.Expression)
     */
    @Override
    public Expression<R> otherwise(Expression<? extends R> result)
    {
        // TODO Auto-generated method stub
        return null;
    }
}