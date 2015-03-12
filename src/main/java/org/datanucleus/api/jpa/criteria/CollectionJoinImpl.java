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

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.CollectionAttribute;

import org.datanucleus.api.jpa.metamodel.CollectionAttributeImpl;

/**
 * Implementation of JPA2 Criteria "CollectionJoin".
 */
public class CollectionJoinImpl<Z, E> extends PluralJoinImpl<Z,java.util.Collection<E>,E> implements CollectionJoin<Z, E>
{
    private static final long serialVersionUID = -4796767580325361976L;

    public CollectionJoinImpl(CriteriaBuilderImpl cb, FromImpl parent, CollectionAttributeImpl attr, JoinType joinType)
    {
        super(cb, parent, attr, joinType);
    }

    public CollectionAttribute<? super Z, E> getModel()
    {
        return (CollectionAttribute<? super Z, E>)attribute;
    }

    public CollectionJoin<Z, E> on(Expression<Boolean> restriction)
    {
        return (CollectionJoin<Z, E>) super.on(restriction);
    }

    public CollectionJoin<Z, E> on(Predicate... restrictions)
    {
        return (CollectionJoin<Z, E>) super.on(restrictions);
    }
}