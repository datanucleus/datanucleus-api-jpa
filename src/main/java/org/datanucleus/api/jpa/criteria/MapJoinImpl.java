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

import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.MapAttribute;

import org.datanucleus.api.jpa.metamodel.PluralAttributeImpl;

/**
 * Implementation of JPA2 Criteria "MapJoin".
 * 
 * @param <Z> The source type of the join
 * @param <K> The type of the target Map key
 * @param <V> The type of the target Map value
 */
public class MapJoinImpl<Z, K, V> extends PluralJoinImpl<Z, java.util.Map<K, V>, V> implements MapJoin<Z, K, V>
{
    private static final long serialVersionUID = -3496521920475333402L;

    public MapJoinImpl(CriteriaBuilderImpl cb, FromImpl parent, PluralAttributeImpl attr, JoinType joinType)
    {
        super(cb, parent, attr, joinType);
    }

    public MapAttribute<? super Z, K,V> getModel()
    {
        return (MapAttribute<? super Z, K, V>)attribute;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.MapJoin#entry()
     */
    public Expression<Entry<K, V>> entry()
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.MapJoin#joinKey()
     */
    public Join<Map<K, V>, K> joinKey()
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.MapJoin#joinKey(javax.persistence.criteria.JoinType)
     */
    public Join<Map<K, V>, K> joinKey(JoinType arg0)
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.MapJoin#key()
     */
    public Path<K> key()
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.MapJoin#value()
     */
    public Path<V> value()
    {
        // TODO Implement this
        throw new UnsupportedOperationException(
            "Not yet implemented. Provide a testcase that uses this and raise an issue attaching your testcase");
    }

    public MapJoin<Z, K, V> on(Expression<Boolean> restriction)
    {
        return (MapJoin<Z, K, V>) super.on(restriction);
    }

    public MapJoin<Z, K, V> on(Predicate... restrictions)
    {
        return (MapJoin<Z, K, V>) super.on(restrictions);
    }
}