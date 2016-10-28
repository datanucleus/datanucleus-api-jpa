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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;

import org.datanucleus.metadata.MetaDataManager;

/**
 * Base for JPAEntityGraph and JPASubgraph.
 */
public abstract class JPAGraph<T>
{
    MetaDataManager mmgr;

    Class<T> classType;

    Map<String, JPAAttributeNode<?>> attributeNodeMap = null;

    boolean mutable = true;

    public JPAGraph(MetaDataManager mmgr, Class<T> clsType)
    {
        this.mmgr = mmgr;
        this.classType = clsType;
    }

    public Class<T> getClassType()
    {
        return classType;
    }

    public void setNotMutable()
    {
        this.mutable = false;
    }

    public List<AttributeNode<?>> getAttributeNodes()
    {
        if (attributeNodeMap == null)
        {
            return null;
        }
        List<AttributeNode<?>> attributeNodes = new ArrayList<AttributeNode<?>>();
        attributeNodes.addAll(attributeNodeMap.values());
        return attributeNodes;
    }

    public void addAttributeNodes(String... attributeNames)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        if (attributeNodeMap == null)
        {
            attributeNodeMap = new HashMap<String, JPAAttributeNode<?>>();
        }
        for (int i=0;i<attributeNames.length;i++)
        {
            JPAAttributeNode node = new JPAAttributeNode<T>(mmgr, this, attributeNames[i]);
            attributeNodeMap.put(node.getAttributeName(), node);
        }
    }

    public void addAttributeNodes(Attribute<? super T, ?>... attributes)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        if (attributeNodeMap == null)
        {
            attributeNodeMap = new HashMap<String, JPAAttributeNode<?>>();
        }
        for (int i=0;i<attributes.length;i++)
        {
            JPAAttributeNode node = new JPAAttributeNode<T>(mmgr, this, attributes[i].getName());
            attributeNodeMap.put(node.getAttributeName(), node);
        }
    }

    public <X> Subgraph<X> addSubgraph(Attribute<? super T, X> attribute)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        if (attributeNodeMap == null)
        {
            attributeNodeMap = new HashMap<String, JPAAttributeNode<?>>();
        }
        JPAAttributeNode node = attributeNodeMap.get(attribute.getName());
        if (node == null)
        {
            node = new JPAAttributeNode<T>(mmgr, this, attribute.getName());
            attributeNodeMap.put(node.getAttributeName(), node);
        }

        JPASubgraph<X> subgraph = new JPASubgraph<X>(mmgr, attribute.getJavaType());
        node.addSubgraph(subgraph);
        return subgraph;
    }

    public <X> Subgraph<? extends X> addSubgraph(Attribute<? super T, X> attribute, Class<? extends X> type)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        if (attributeNodeMap == null)
        {
            attributeNodeMap = new HashMap<String, JPAAttributeNode<?>>();
        }
        JPAAttributeNode node = attributeNodeMap.get(attribute.getName());
        if (node == null)
        {
            node = new JPAAttributeNode<T>(mmgr, this, attribute.getName());
            attributeNodeMap.put(node.getAttributeName(), node);
        }

        JPASubgraph<? extends X> subgraph = new JPASubgraph(mmgr, type);
        node.addSubgraph(subgraph);
        return subgraph;
    }

    public <X> Subgraph<X> addSubgraph(String attributeName)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        if (attributeNodeMap == null)
        {
            attributeNodeMap = new HashMap<String, JPAAttributeNode<?>>();
        }
        JPAAttributeNode node = attributeNodeMap.get(attributeName);
        if (node == null)
        {
            node = new JPAAttributeNode<T>(mmgr, this, attributeName);
            attributeNodeMap.put(node.getAttributeName(), node);
        }

        JPASubgraph<X> subgraph = new JPASubgraph<X>(mmgr, null); // TODO Get the type, from where?
        node.addSubgraph(subgraph);
        return subgraph;
    }

    public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        if (attributeNodeMap == null)
        {
            attributeNodeMap = new HashMap<String, JPAAttributeNode<?>>();
        }
        JPAAttributeNode node = attributeNodeMap.get(attributeName);
        if (node == null)
        {
            node = new JPAAttributeNode<T>(mmgr, this, attributeName);
            attributeNodeMap.put(node.getAttributeName(), node);
        }

        JPASubgraph<X> subgraph = new JPASubgraph<X>(mmgr, type);
        node.addSubgraph(subgraph);
        return subgraph;
    }

    public <X> Subgraph<X> addKeySubgraph(Attribute<? super T, X> attribute)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public <X> Subgraph<? extends X> addKeySubgraph(Attribute<? super T, X> attribute, Class<? extends X> type)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public <X> Subgraph<X> addKeySubgraph(String attributeName)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public <X> Subgraph<X> addKeySubgraph(String attributeName, Class<X> type)
    {
        if (!mutable)
        {
            throw new IllegalStateException("This Graph is not mutable");
        }

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented");
    }
}