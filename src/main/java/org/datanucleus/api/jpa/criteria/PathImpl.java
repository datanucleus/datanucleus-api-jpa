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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.Type.PersistenceType;

import org.datanucleus.api.jpa.metamodel.AttributeImpl;
import org.datanucleus.api.jpa.metamodel.MapAttributeImpl;
import org.datanucleus.api.jpa.metamodel.PluralAttributeImpl;
import org.datanucleus.query.expression.ClassExpression;
import org.datanucleus.query.expression.PrimaryExpression;

/**
 * Implementation of JPA Criteria "Path".
 * @param <X> Type referenced by the path
 */
public class PathImpl<Z,X> extends ExpressionImpl<X> implements Path<X>
{
    static final long serialVersionUID = -5105905357945361262L;

    protected final PathImpl<?,Z> parent;
    protected final AttributeImpl<? super Z,?> attribute;

    public PathImpl(CriteriaBuilderImpl cb, Class<X> cls)
    {
        super(cb, cls);
        parent = null;
        attribute = null;
    }

    public PathImpl(CriteriaBuilderImpl cb, PathImpl<?,Z> parent, AttributeImpl<? super Z, ?> attr, Class<X> cls)
    {
        super(cb, cls);
        this.parent = parent;
        this.attribute = attr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Path#get(javax.persistence.metamodel.MapAttribute)
     */
    public <K, V, M extends Map<K, V>> Expression<M> get(MapAttribute<X, K, V> attr)
    {
        return new PathImpl<X,M>(cb, this, (MapAttributeImpl<? super X,K,V>)attr, (Class<M>)attr.getJavaType());
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Path#get(javax.persistence.metamodel.PluralAttribute)
     */
    public <E, C extends Collection<E>> Expression<C> get(PluralAttribute<X, C, E> attr)
    {
        return new PathImpl<X,C>(cb, this, (PluralAttributeImpl<? super X, C, E>)attr, attr.getJavaType());
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Path#get(javax.persistence.metamodel.SingularAttribute)
     */
    public <Y> Path<Y> get(SingularAttribute<? super X, Y> attr)
    {
        return new PathImpl<X,Y>(cb, this, (AttributeImpl<? super X, ?>) attr, attr.getJavaType());
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Path#get(java.lang.String)
     */
    public <Y> Path<Y> get(String attrName)
    {
        Type<?> type = this.getType();
        if (type.getPersistenceType() == PersistenceType.BASIC)
        {
            throw new IllegalArgumentException(this + " is BASIC and we cannot navigate to " + attrName);
        }

        AttributeImpl<? super X, Y> next = (AttributeImpl<? super X, Y>) ((ManagedType<? super X>)type).getAttribute(attrName);
        return new PathImpl<X,Y>(cb, this, next, next.getJavaType());
    }

    public Type<?> getType()
    {
        return attribute.getType();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Path#getModel()
     */
    public Bindable<X> getModel()
    {
        if (attribute instanceof Bindable<?> == false)
        {
            throw new IllegalArgumentException(this + " is basic path, but needs to be a bindable for this operation");
        }
        return (Bindable<X>)attribute;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Path#getParentPath()
     */
    public Path<?> getParentPath()
    {
        return parent;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.Path#type()
     */
    public Expression<Class<? extends X>> type()
    {
        // TODO Implement this
        throw new UnsupportedOperationException("Not yet implemented. Provide a testcase that uses this and raise a JIRA attaching your testcase");
    }

    /**
     * Accessor for the underlying DataNucleus expression for this path.
     * Creates it if not yet existing.
     * @return The DataNucleus query expression
     */
    public org.datanucleus.query.expression.Expression getQueryExpression()
    {
        // TODO Don't cache the query expression?
        if (queryExpr == null)
        {
            List tuples = new ArrayList();
            if (parent != null)
            {
                org.datanucleus.query.expression.Expression parentExpr = parent.getQueryExpression();
                if (parentExpr instanceof ClassExpression)
                {
                    tuples.add(((ClassExpression)parentExpr).getAlias());
                    if (attribute == null && getAlias() != null)
                    {
                        tuples.add(getAlias());
                    }
                    else if (attribute != null)
                    {
                        tuples.add(attribute.getName());
                    }
                    queryExpr = new PrimaryExpression(null, tuples);
                }
                else if (parentExpr instanceof PrimaryExpression)
                {
                    tuples.addAll(((PrimaryExpression)parentExpr).getTuples());
                    if (attribute == null && getAlias() != null)
                    {
                        tuples.add(getAlias());
                    }
                    else if (attribute != null)
                    {
                        tuples.add(attribute.getName());
                    }
                    queryExpr = new PrimaryExpression(null, tuples);
                }
                else
                {
                    if (attribute == null && getAlias() != null)
                    {
                        tuples.add(getAlias());
                    }
                    else if (attribute != null)
                    {
                        tuples.add(attribute.getName());
                    }
                    queryExpr = new PrimaryExpression(parentExpr, tuples);
                }
            }
            else
            {
                if (attribute == null && getAlias() != null)
                {
                    tuples.add(getAlias());
                }
                else if (attribute != null)
                {
                    tuples.add(attribute.getName());
                }
                queryExpr = new PrimaryExpression(null, tuples);
            }
        }
        return queryExpr;
    }

    /**
     * Method to return the path as something suitable for use as JPQL single-string.
     * @return The JPQL form
     */
    public String toString()
    {
        org.datanucleus.query.expression.Expression queryExpr = getQueryExpression();
        if (queryExpr.getLeft() != null)
        {
            return parent.toString() + "." + ((PrimaryExpression)queryExpr).getId();
        }
        return ((PrimaryExpression)queryExpr).getId();
    }
}