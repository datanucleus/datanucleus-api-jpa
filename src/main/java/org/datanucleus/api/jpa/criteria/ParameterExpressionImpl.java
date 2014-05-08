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

import javax.persistence.criteria.ParameterExpression;

/**
 * Implementation of JPA2 Criteria "ParameterExpression".
 */
public class ParameterExpressionImpl<T> extends ExpressionImpl<T> implements ParameterExpression<T>
{
    private String name;
    private int position = -1;

    public ParameterExpressionImpl(CriteriaBuilderImpl cb, Class<T> cls, String name)
    {
        super(cb, cls);
        this.name = name;
    }

    /* (non-Javadoc)
     * @see javax.persistence.Parameter#getName()
     */
    public String getName()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see javax.persistence.Parameter#getParameterType()
     */
    public Class<T> getParameterType()
    {
        return (Class<T>) getJavaType();
    }

    /* (non-Javadoc)
     * @see javax.persistence.Parameter#getPosition()
     */
    public Integer getPosition()
    {
        // Depends if we decide to support positional parameters in criteria queries
        return position;
    }

    /**
     * Accessor for the underlying DataNucleus expression for this path.
     * @return The DataNucleus query expression
     */
    public org.datanucleus.query.expression.ParameterExpression getQueryExpression()
    {
        return new org.datanucleus.query.expression.ParameterExpression(name, position);
    }

    /**
     * Method to return the JPQL single-string that this equates to.
     * @return The JPQL single-string form of this order
     */
    public String toString()
    {
        if (name != null)
        {
            return ":" + name;
        }
        else
        {
            return ":UNKNOWN";
        }
    }
}