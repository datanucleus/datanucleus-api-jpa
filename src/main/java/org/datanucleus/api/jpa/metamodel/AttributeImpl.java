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

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.Map;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;

/**
 * Implementation of JPA Metamodel "Attribute".
 * Provides a wrapper to AbstractMemberMetaData.
 *
 * @param <X> The type containing the represented attribute
 * @param <Y> The type of the represented attribute
 */
public class AttributeImpl<X, Y> implements Attribute<X, Y>
{
    AbstractMemberMetaData mmd;
    ManagedTypeImpl<X> owner;

    public AttributeImpl(AbstractMemberMetaData mmd, ManagedTypeImpl<X> owner)
    {
        this.mmd = mmd;
        this.owner = owner;
    }

    /**
     * Convenience accessor for the metadata underlying this member.
     * @return The metadata
     */
    public AbstractMemberMetaData getMetadata()
    {
        return mmd;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Attribute#getDeclaringType()
     */
    public ManagedType<X> getDeclaringType()
    {
        Class ownerCls = owner.getModel().getClassLoaderResolver().classForName(mmd.getClassName(true));
        return owner.getModel().managedType(ownerCls);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Attribute#getJavaMember()
     */
    public Member getJavaMember()
    {
        return mmd.getMemberRepresented();
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Attribute#getJavaType()
     */
    public Class<Y> getJavaType()
    {
        return mmd.getType();
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Attribute#getName()
     */
    public String getName()
    {
        return mmd.getName();
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Attribute#getPersistentAttributeType()
     */
    public javax.persistence.metamodel.Attribute.PersistentAttributeType getPersistentAttributeType()
    {
        if (mmd.getEmbeddedMetaData() != null)
        {
            return PersistentAttributeType.EMBEDDED;
        }

        RelationType relationType = mmd.getRelationType(owner.getModel().getClassLoaderResolver());
        if (relationType == RelationType.MANY_TO_ONE_BI)
        {
            return PersistentAttributeType.MANY_TO_ONE;
        }
        else if (relationType == RelationType.ONE_TO_ONE_UNI || relationType == RelationType.ONE_TO_ONE_BI)
        {
            if (mmd.getRelationTypeString() != null && mmd.getRelationTypeString().equals("ManyToOne"))
            {
                // 1-1 and N-1 (uni) are to all intents and purposes the exact same thing yet JPA insists on a user artificially discriminating
                return PersistentAttributeType.MANY_TO_ONE;
            }
            return PersistentAttributeType.ONE_TO_ONE;
        }
        else if (relationType == RelationType.ONE_TO_MANY_UNI || relationType == RelationType.ONE_TO_MANY_BI)
        {
            return PersistentAttributeType.ONE_TO_MANY;
        }
        else if (relationType == RelationType.MANY_TO_MANY_BI)
        {
            return PersistentAttributeType.MANY_TO_MANY;
        }
        else
        {
            if (Collection.class.isAssignableFrom(mmd.getType()))
            {
                return PersistentAttributeType.ELEMENT_COLLECTION;
            }
            else if (Map.class.isAssignableFrom(mmd.getType()))
            {
                return PersistentAttributeType.ELEMENT_COLLECTION;
            }
            // TODO Detect embedded
            return PersistentAttributeType.BASIC;
        }
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Attribute#isAssociation()
     */
    public boolean isAssociation()
    {
        RelationType relationType = mmd.getRelationType(owner.getModel().getClassLoaderResolver());
        return relationType != RelationType.NONE;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Attribute#isCollection()
     */
    public boolean isCollection()
    {
        // In JPA a "collection" is a Collection or Map ...
        return Collection.class.isAssignableFrom(mmd.getType()) || Map.class.isAssignableFrom(mmd.getType());
    }

    /**
     * Return the type of the attribute.
     * If the type is simple then returns that java type, otherwise if a collection then returns the element type.
     * @return The type of attribute
     */
    public final Type<Y> getType()
    {
        ClassLoaderResolver clr = owner.getModel().getClassLoaderResolver();
        if (mmd.hasCollection())
        {
            return owner.model.getType(clr.classForName(mmd.getCollection().getElementType()));
        }
        else if (mmd.hasMap())
        {
            return owner.model.getType(clr.classForName(mmd.getMap().getValueType()));
        }
        else if (mmd.hasArray())
        {
            return owner.model.getType(mmd.getType().getComponentType());
        }
        else
        {
            return owner.model.getType(mmd.getType());
        }
    }
}