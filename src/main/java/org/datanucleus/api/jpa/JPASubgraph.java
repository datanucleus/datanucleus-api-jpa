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

import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;

import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.util.StringUtils;

/**
 * Implementation of JPA2.1 Subgraph
 */
public class JPASubgraph<T> extends AbstractJPAGraph<T>
{
    public JPASubgraph(MetaDataManager mmgr, Class<T> clsType)
    {
        super(mmgr, clsType);
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("Graph(");
        str.append("type=").append(classType.getName());
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
                if (!hasSubgraphs && attr.getSubgraphs() != null && !attr.getSubgraphs().isEmpty())
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
                    if (subgraphs != null && !subgraphs.isEmpty())
                    {
                        str.append(StringUtils.collectionToString(subgraphs.values()));
                    }
                }
                str.append("]");
            }
        }
        str.append(")");

        return str.toString();
    }
}