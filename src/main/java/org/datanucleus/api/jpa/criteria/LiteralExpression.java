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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.datanucleus.store.query.expression.Expression;
import org.datanucleus.store.query.expression.Literal;

/**
 * Representation of a Literal in a criteria query.
 */
public class LiteralExpression<X> extends ExpressionImpl
{
    private static final long serialVersionUID = -867487639375209191L;
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
        else if (value instanceof Time)
        {
            // Convert to JDBC escape syntax
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            String timeStr = formatter.format((Time)value);
            return "{t '" + timeStr + "'}";
        }
        else if (value instanceof Date)
        {
            // Convert to JDBC escape syntax
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = formatter.format((Date)value);
            return "{d '" + dateStr + "'}";
        }
        else if (value instanceof Timestamp)
        {
            // Convert to JDBC escape syntax
            return "{ts '" + value.toString() + "'}";
        }
        else if (value instanceof java.util.Date)
        {
            // Convert to JDBC escape syntax
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String datetimeStr = formatter.format((java.util.Date)value);
            return "{ts '" + datetimeStr + "'}";
        }
        else
        {
            return "" + value;
        }
    }
}