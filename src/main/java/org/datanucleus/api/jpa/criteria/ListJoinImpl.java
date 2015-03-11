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

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.ListAttribute;

import org.datanucleus.api.jpa.metamodel.ListAttributeImpl;

/**
 * Implementation of JPA2 Criteria "ListJoin".
 *
 * @param <Z> The source type of the join
 * @param <E> The element type of the target List
 */
public class ListJoinImpl<Z, E> extends PluralJoinImpl<Z,java.util.List<E>,E> implements ListJoin<Z, E>
{
    private static final long serialVersionUID = 3656855953397784490L;

    /**
     * Constructor for a list join.
     * @param cb Criteria Builder
     * @param parent The parent
     * @param attr The attribute being joined
     * @param joinType Type of join
     */
    public ListJoinImpl(CriteriaBuilderImpl cb, FromImpl<?, Z> parent, ListAttributeImpl attr, JoinType joinType)
    {
        super(cb, parent, attr, joinType);
    }

    public ListAttribute<? super Z, E> getModel()
    {
        return (ListAttribute<? super Z, E>)attribute;
    }

    public Expression<Integer> index()
    {
        // TODO Implement this
        throw new UnsupportedOperationException("Not yet implemented. Provide a testcase that uses this and raise a JIRA attaching your testcase");
    }

    public ListJoin<Z, E> on(Expression<Boolean> restriction)
    {
        return (ListJoin<Z, E>) super.on(restriction);
    }

    public ListJoin<Z, E> on(Predicate... restrictions)
    {
        return (ListJoin<Z, E>) super.on(restrictions);
    }
}