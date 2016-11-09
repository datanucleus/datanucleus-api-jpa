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

import javax.persistence.metamodel.SingularAttribute;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.NullValue;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.metadata.VersionMetaData;

/**
 * Implementation of JPA Metamodel "SingularAttribute".
 *
 * @param <X> The type containing the represented attribute
 * @param <T> The type of the represented attribute
 */
public class SingularAttributeImpl<X, T> extends AttributeImpl<X, T> implements SingularAttribute<X, T>
{
    /**
     * Constructor for a single-valued attribute.
     * @param mmd Metadata for the member
     * @param owner The owner type
     */
    public SingularAttributeImpl(AbstractMemberMetaData mmd, ManagedTypeImpl<X> owner)
    {
        super(mmd, owner);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.SingularAttribute#isId()
     */
    public boolean isId()
    {
        return mmd.isPrimaryKey();
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.SingularAttribute#isOptional()
     */
    public boolean isOptional() // Really wants to know if it is nullable
    {
        if (mmd.isPrimaryKey())
        {
            return false;
        }
        else if (mmd.getType().isPrimitive() || mmd.getNullValue() == NullValue.EXCEPTION)
        {
            return false;
        }
        else if (mmd.getColumnMetaData() != null && mmd.getColumnMetaData().length > 0 && !mmd.getColumnMetaData()[0].isAllowsNull())
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.SingularAttribute#isVersion()
     */
    public boolean isVersion()
    {
        VersionMetaData vermd = mmd.getAbstractClassMetaData().getVersionMetaData();
        if (vermd != null && vermd.getFieldName() != null && vermd.getFieldName().equals(mmd.getName()))
        {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Bindable#getBindableJavaType()
     */
    public Class<T> getBindableJavaType()
    {
        return mmd.getType();
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Bindable#getBindableType()
     */
    public javax.persistence.metamodel.Bindable.BindableType getBindableType()
    {
        if (mmd.getEmbeddedMetaData() != null)
        {
            // JPA spec says nothing about whether embeddable should be ENTITY_TYPE or SINGULAR_ATTRIBUTE or what.
            return BindableType.SINGULAR_ATTRIBUTE;
        }

        RelationType relationType = mmd.getRelationType(owner.model.clr);
        if (RelationType.isRelationSingleValued(relationType))
        {
             return BindableType.ENTITY_TYPE;
        }

        return BindableType.SINGULAR_ATTRIBUTE;
    }
}