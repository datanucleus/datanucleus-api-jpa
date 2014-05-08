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
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.SetAttribute;

import org.datanucleus.api.jpa.metamodel.SetAttributeImpl;

/**
 * Implementation of JPA2 Criteria "SetJoin".
 * 
 * @param <Z> The source type of the join
 * @param <E> The element type of the target Set
 */
public class SetJoinImpl<Z, E> extends PluralJoinImpl<Z, java.util.Set<E>, E> implements SetJoin<Z, E>
{
    public SetJoinImpl(CriteriaBuilderImpl cb, FromImpl parent, SetAttributeImpl attr, JoinType joinType)
    {
        super(cb, parent, attr, joinType);
    }

    public SetAttribute<? super Z, E> getModel()
    {
        return (SetAttribute<? super Z, E>)attribute;
    }

    public SetJoin<Z, E> on(Expression<Boolean> restriction)
    {
        // TODO JPA2.1 addition
        throw new UnsupportedOperationException(
                "Not yet implemented. Provide a testcase that uses this and raise a JIRA issue attaching your testcase");
    }

    public SetJoin<Z, E> on(Predicate... restrictions)
    {
        // TODO JPA2.1 addition
        throw new UnsupportedOperationException(
                "Not yet implemented. Provide a testcase that uses this and raise a JIRA issue attaching your testcase");
    }
}