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
/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
import java.util.Iterator;
import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;

import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.util.StringUtils;

/**
 * Implementation of JPA2.1 EntityGraph.
 * @param <T> Type of the entity
 */
public class JPAEntityGraph<T> extends AbstractJPAGraph<T> implements EntityGraph<T>
{
    public static final String LOADGRAPH_PROPERTY = "javax.persistence.loadgraph";
    public static final String FETCHGRAPH_PROPERTY = "javax.persistence.fetchgraph";

    String name;

    boolean includeAllAttributes = false;

    Map<Class, Subgraph> subclassSubgraphsByType = null;

    public JPAEntityGraph(MetaDataManager mmgr, String name, Class clsType)
    {
        super(mmgr, clsType);
        this.name = name;
    }

    public void setName(String name)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setIncludeAll()
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }
        this.includeAllAttributes = true;
    }

    public boolean getIncludeAllAttributes()
    {
        return includeAllAttributes;
    }

    public JPAEntityGraph<T> cloneMutableEntityGraph()
    {
        JPAEntityGraph<T> eg = new JPAEntityGraph<T>(mmgr, name, classType);
        if (attributeNodeMap != null)
        {
            eg.attributeNodeMap = new HashMap<String, JPAAttributeNode<?>>();
            eg.attributeNodeMap.putAll(attributeNodeMap);
        }
        if (subclassSubgraphsByType != null)
        {
            eg.subclassSubgraphsByType = new HashMap();
            eg.subclassSubgraphsByType.putAll(subclassSubgraphsByType);
        }
        return eg;
    }

    public <V> Subgraph<? extends V> addSubclassSubgraph(Class<? extends V> type)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        if (subclassSubgraphsByType == null)
        {
            subclassSubgraphsByType = new HashMap<Class, Subgraph>();
        }
        JPASubgraph<? extends V> subgraph = new JPASubgraph(mmgr, type);
        subclassSubgraphsByType.put(type, subgraph);
        return subgraph;
    }

    public Map<Class, Subgraph> getSubclassSubgraphs()
    {
        if (subclassSubgraphsByType == null)
        {
            return Collections.EMPTY_MAP;
        }
        return subclassSubgraphsByType;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("Graph(");
        str.append("\"").append(name).append("\"");
        str.append(", type=").append(classType.getName());
        if (attributeNodeMap != null)
        {
            boolean hasSubgraphs = false;
            str.append(", attributes=[");
            int i = 0;
            for (AttributeNode attr : attributeNodeMap.values())
            {
                if (i > 0)
                {
                    str.append(",");
                }
                if (!hasSubgraphs && attr.getSubgraphs() != null && attr.getSubgraphs().size() > 0)
                {
                    hasSubgraphs = true;
                }
                str.append(attr.toString());
                i++;
            }
            str.append("]");

            if (hasSubgraphs)
            {
                str.append(", subgraphs=[");
                for (AttributeNode attr : attributeNodeMap.values())
                {
                    Map<Class, Subgraph> subgraphs = attr.getSubgraphs();
                    if (subgraphs != null && subgraphs.size() > 0)
                    {
                        str.append(StringUtils.collectionToString(subgraphs.values()));
                    }
                }
                str.append("]");
            }
            if (subclassSubgraphsByType != null)
            {
                str.append(", subclasses=[");
                Iterator<Map.Entry<Class, Subgraph>> subclassIter = subclassSubgraphsByType.entrySet().iterator();
                while (subclassIter.hasNext())
                {
                    Map.Entry<Class, Subgraph> entry = subclassIter.next();
                    str.append("{cls=" + entry.getKey());
                    str.append(", graph=");
                    str.append(entry.getValue());
                    str.append("}");
                    if (subclassIter.hasNext())
                    {
                        str.append(",");
                    }
                }
                str.append("]");
            }
        }
        str.append(")");

        return str.toString();
    }
}