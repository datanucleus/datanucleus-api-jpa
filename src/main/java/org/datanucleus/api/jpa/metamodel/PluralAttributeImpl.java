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
package org.datanucleus.api.jpa.metamodel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Type;

import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Implementation of JPA2 Metamodel "PluralAttribute".
 * 
 * @param <X> the type that owns this member
 * @param <C> the container type that holds this member (e.g. java.util.Set&lt;Employee&gt;)
 * @param <E> the type of the element held by this member (e.g. Employee).
 */
public class PluralAttributeImpl<X, C, E> extends AttributeImpl<X, C> implements PluralAttribute<X, C, E>
{
    public PluralAttributeImpl(AbstractMemberMetaData mmd, ManagedTypeImpl<X> owner)
    {
        super(mmd, owner);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Bindable#getBindableJavaType()
     */
    public Class<E> getBindableJavaType()
    {
        if (mmd.hasCollection())
        {
            return owner.model.getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
        }
        else if (mmd.hasArray())
        {
            return owner.model.getClassLoaderResolver().classForName(mmd.getArray().getElementType());
        }
        else if (mmd.hasMap())
        {
            // We have to guess what an "element" of a Map is supposed to be, because it doesn't have one
            return owner.model.getClassLoaderResolver().classForName(mmd.getMap().getValueType());
        }
        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Bindable#getBindableType()
     */
    public javax.persistence.metamodel.Bindable.BindableType getBindableType()
    {
        return BindableType.PLURAL_ATTRIBUTE;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.PluralAttribute#getCollectionType()
     */
    public javax.persistence.metamodel.PluralAttribute.CollectionType getCollectionType()
    {
        if (List.class.isAssignableFrom(mmd.getType()))
        {
            return CollectionType.LIST;
        }
        else if (Map.class.isAssignableFrom(mmd.getType()))
        {
            return CollectionType.MAP;
        }
        else if (Set.class.isAssignableFrom(mmd.getType()))
        {
            return CollectionType.SET;
        }
        else if (Collection.class.isAssignableFrom(mmd.getType()))
        {
            return CollectionType.COLLECTION;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.PluralAttribute#getElementType()
     */
    public Type<E> getElementType()
    {
        if (mmd.hasCollection())
        {
            Class elementCls = owner.model.getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
            return owner.model.getType(elementCls);
        }
        else if (mmd.hasArray())
        {
            Class elementCls = owner.model.getClassLoaderResolver().classForName(mmd.getArray().getElementType());
            return owner.model.getType(elementCls);
        }
        else if (mmd.hasMap())
        {
            Class elementCls = owner.model.getClassLoaderResolver().classForName(mmd.getMap().getValueType());
            return owner.model.getType(elementCls);
        }
        return null;
    }
}