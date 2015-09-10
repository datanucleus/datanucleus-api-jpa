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
package org.datanucleus.api.jpa;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;

import org.datanucleus.metadata.MetaDataManager;

/**
 * Implementation of JPA2.1 AttributeNode
 */
public class JPAAttributeNode<T> implements AttributeNode<T>
{
    MetaDataManager mmgr;

    AbstractJPAGraph parent;

    String name;

    Map<Class, Subgraph> subgraphsByType = null;

    public JPAAttributeNode(MetaDataManager mmgr, AbstractJPAGraph parent, String name)
    {
        this.mmgr = mmgr;
        this.parent = parent;
        this.name = name;
    }

    /* (non-Javadoc)
     * @see javax.persistence.AttributeNode#getAttributeName()
     */
    public String getAttributeName()
    {
        return name;
    }

    public void addSubgraph(JPASubgraph<T> subgraph)
    {
        if (subgraphsByType == null)
        {
            subgraphsByType = new HashMap<Class, Subgraph>();
        }
        subgraphsByType.put(subgraph.getClassType(), subgraph);
    }

    public Map<Class, Subgraph> getSubgraphs()
    {
        if (subgraphsByType == null)
        {
            return Collections.EMPTY_MAP;
        }
        return subgraphsByType;
    }

    public Map<Class, Subgraph> getKeySubgraphs()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String toString()
    {
        return "\"" + name + "\"";
    }
}