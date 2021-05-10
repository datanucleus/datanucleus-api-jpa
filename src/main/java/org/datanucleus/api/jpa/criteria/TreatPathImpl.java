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

import javax.persistence.metamodel.Type;

import org.datanucleus.store.query.expression.DyadicExpression;
import org.datanucleus.store.query.expression.Expression;
import org.datanucleus.store.query.expression.Literal;

/**
 * Treated (cast) form of a Path (in a WHERE clause).
 */
public class TreatPathImpl extends PathImpl
{
    private static final long serialVersionUID = -5620689101678295895L;
    PathImpl path;
    Class type;

    public TreatPathImpl(CriteriaBuilderImpl cb, PathImpl path, Class cls)
    {
        super(cb, path, path.attribute, cls);
        this.path = path;
        this.type = cls;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.criteria.PathImpl#getQueryExpression()
     */
    @Override
    public Expression getQueryExpression()
    {
        if (queryExpr == null)
        {
            queryExpr = new DyadicExpression(path.getQueryExpression(), org.datanucleus.store.query.expression.Expression.OP_CAST, new Literal(type.getName()));
        }
        return queryExpr;
    }

    public Type<?> getType()
    {
        return cb.getEntityManagerFactory().getMetamodel().managedType(type);
    }

    public String toString()
    {
        return "TREAT(" + path.toString() + " AS " + type.getName() + ")";
    }
}
