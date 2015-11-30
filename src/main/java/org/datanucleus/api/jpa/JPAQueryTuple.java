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
package org.datanucleus.api.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.datanucleus.util.NucleusLogger;

/**
 * Implementation of a JPA Tuple, for a query result.
 */
public class JPAQueryTuple implements Tuple
{
    protected List<JPAQueryTupleElement> elements = new ArrayList<JPAQueryTupleElement>();
    protected Map<String, JPAQueryTupleElement> elementByAlias = new HashMap<String, JPAQueryTupleElement>();

    public JPAQueryTuple()
    {
    }

    /**
     * Method used by DataNucleus query mechanism to load up the row results into this tuple.
     * @param key Key of the result
     * @param val Value of the result
     */
    public void put(Object key, Object val)
    {
        NucleusLogger.GENERAL.info(">> JPAQueryTuple.put key=" + key + " val=" + val);
        JPAQueryTupleElement te = new JPAQueryTupleElement((String) key, (val != null) ? val.getClass() : null, val);
        elements.add(te);
        elementByAlias.put((String)key, te);
    }

    /* (non-Javadoc)
     * @see javax.persistence.Tuple#get(javax.persistence.TupleElement)
     */
    @Override
    public <X> X get(TupleElement<X> tupleElement)
    {
        if (!elements.contains(tupleElement))
        {
            throw new IllegalArgumentException("TupleElement is not present in this Tuple");
        }

        for (JPAQueryTupleElement te : elements)
        {
            if (te.equals(tupleElement))
            {
                return (X) te.getValue();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.Tuple#get(java.lang.String, java.lang.Class)
     */
    @Override
    public <X> X get(String alias, Class<X> type)
    {
        JPAQueryTupleElement te = elementByAlias.get(alias);
        if (te == null)
        {
            throw new IllegalArgumentException("Cannot find element of Tuple with alias=" + alias);
        }

        if (!type.isAssignableFrom(te.getJavaType()))
        {
            throw new IllegalArgumentException("Cannot return value for alias=" + alias + " of this Tuple to be type=" + type.getName() + " because type is " + te.getJavaType());
        }
        return (X) te.getValue();
    }

    /* (non-Javadoc)
     * @see javax.persistence.Tuple#get(java.lang.String)
     */
    @Override
    public Object get(String alias)
    {
        JPAQueryTupleElement te = elementByAlias.get(alias);
        if (te == null)
        {
            throw new IllegalArgumentException("Cannot find element of Tuple with alias=" + alias);
        }

        return te.getValue();
    }

    /* (non-Javadoc)
     * @see javax.persistence.Tuple#get(int, java.lang.Class)
     */
    @Override
    public <X> X get(int i, Class<X> type)
    {
        if (i < 0 || i >= elements.size())
        {
            throw new IllegalArgumentException("Cannot return value for position " + i + " of this Tuple. Max position=" + (elements.size()-1));
        }

        JPAQueryTupleElement te = elements.get(i);
        if (!type.isAssignableFrom(te.getJavaType()))
        {
            throw new IllegalArgumentException("Cannot return value for position " + i + " of this Tuple to be type=" + type.getName() + " because type is " + te.getJavaType());
        }
        return (X) te.getValue();
    }

    /* (non-Javadoc)
     * @see javax.persistence.Tuple#get(int)
     */
    @Override
    public Object get(int i)
    {
        if (i < 0 || i >= elements.size())
        {
            throw new IllegalArgumentException("Cannot return value for position " + i + " of this Tuple. Max position=" + (elements.size()-1));
        }

        JPAQueryTupleElement te = elements.get(i);
        return te.getValue();
    }

    /* (non-Javadoc)
     * @see javax.persistence.Tuple#toArray()
     */
    @Override
    public Object[] toArray()
    {
        Object[] values = new Object[elements.size()];
        int i = 0;
        for (JPAQueryTupleElement te : elements)
        {
            values[i] = te.getValue();
        }
        return values;
    }

    /* (non-Javadoc)
     * @see javax.persistence.Tuple#getElements()
     */
    @Override
    public List<TupleElement<?>> getElements()
    {
        List<TupleElement<?>> tupleElements = new ArrayList();
        for (JPAQueryTupleElement te : elements)
        {
            tupleElements.add(te);
        }
        return tupleElements;
    }

    public String toString()
    {
        return "JPAQueryTuple : " + elements.size() + " elements";
    }
}