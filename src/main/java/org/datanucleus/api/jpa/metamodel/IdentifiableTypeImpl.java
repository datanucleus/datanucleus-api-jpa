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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.datanucleus.identity.SingleFieldId;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Implementation of JPA Metamodel "IdentifiableType".
 */
public class IdentifiableTypeImpl<X> extends ManagedTypeImpl<X> implements IdentifiableType<X>
{
    protected IdentifiableTypeImpl(Class<X> cls, AbstractClassMetaData cmd, MetamodelImpl model)
    {
        super(cls, cmd, model);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#getDeclaredId(java.lang.Class)
     */
    public <Y> SingularAttribute<X, Y> getDeclaredId(Class<Y> cls)
    {
        return (SingularAttribute<X, Y>) getId(cls);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#getDeclaredVersion(java.lang.Class)
     */
    public <Y> SingularAttribute<X, Y> getDeclaredVersion(Class<Y> cls)
    {
        return (SingularAttribute<X, Y>) getVersion(cls);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#getId(java.lang.Class)
     */
    public <Y> SingularAttribute<? super X, Y> getId(Class<Y> cls)
    {
        if (cmd.getNoOfPrimaryKeyMembers() > 1)
        {
            throw new IllegalArgumentException("More than 1 PK field, so use getIdClassAttributes()");
        }

        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(cmd.getPKMemberPositions()[0]);
        if (mmd.getType().isAssignableFrom(cls) || cls.isAssignableFrom(mmd.getType()))
        {
            // User passed in the type of the field
            SingularAttribute attr = (SingularAttribute) attributes.get(mmd.getName());
            if (attr == null)
            {
                IdentifiableType supertype = getSupertype();
                if (supertype != null)
                {
                    // Relay to the supertype
                    return supertype.getId(cls);
                }
            }
            return attr;
        }

        Class pkCls = model.getClassLoaderResolver().classForName(cmd.getObjectidClass());
        if (cls.isAssignableFrom(pkCls))
        {
            // User passed in the type of the id
            SingularAttribute attr = (SingularAttribute) attributes.get(mmd.getName());
            if (attr == null)
            {
                IdentifiableType supertype = getSupertype();
                if (supertype != null)
                {
                    // Relay to the supertype
                    return supertype.getId(cls);
                }
            }
            return attr;
        }

        throw new IllegalArgumentException("PK member is not of specified type (" + cls.getName() + "). Should be " + mmd.getTypeName());            
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#getIdClassAttributes()
     */
    public Set<SingularAttribute<? super X, ?>> getIdClassAttributes()
    {
        Set<SingularAttribute<? super X, ?>> pks = new HashSet<SingularAttribute<? super X,?>>();
        int[] pkPositions = cmd.getPKMemberPositions();
        for (int i=0;i<pkPositions.length;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkPositions[i]);
            SingularAttribute pkAttr = (SingularAttribute) attributes.get(mmd.getName());
            if (pkAttr == null)
            {
                IdentifiableType supertype = getSupertype();
                if (supertype != null)
                {
                    // Relay to the supertype
                    pkAttr = supertype.getId(cls);
                }
            }
            pks.add(pkAttr);
        }

        return pks;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#getIdType()
     */
    public Type<?> getIdType()
    {
        String objectIdClass = cmd.getObjectidClass();
        Class pkCls = model.getClassLoaderResolver().classForName(objectIdClass);
        if (SingleFieldId.class.isAssignableFrom(pkCls))
        {
        	// Special case of single id field. But what if IdClass defined???
        	int[] pkMemberNumbers = cmd.getPKMemberPositions();
        	if (pkMemberNumbers.length == 1)
        	{
        		AbstractMemberMetaData pkMmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkMemberNumbers[0]);
        		return model.getType(pkMmd.getType());
        	}
        }
        return model.getType(pkCls);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#getSupertype()
     */
    public IdentifiableType<? super X> getSupertype()
    {
        AbstractClassMetaData superCmd = cmd.getSuperAbstractClassMetaData();
        Class superCls = model.getClassLoaderResolver().classForName(superCmd.getFullClassName());
        return (IdentifiableType<? super X>)model.managedType(superCls);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#getVersion(java.lang.Class)
     */
    public <Y> SingularAttribute<? super X, Y> getVersion(Class<Y> cls)
    {
        String verFieldName = (cmd.getVersionMetaData() != null ? cmd.getVersionMetaData().getFieldName() : null);
        if (verFieldName != null)
        {
            AbstractMemberMetaData verMmd = cmd.getMetaDataForMember(verFieldName);
            if (cls.isAssignableFrom(verMmd.getType()) || verMmd.getType().isAssignableFrom(getClass()))
            {
                SingularAttribute attr = (SingularAttribute) attributes.get(verFieldName);
                if (attr == null)
                {
                    IdentifiableType supertype = getSupertype();
                    if (supertype != null)
                    {
                        // Relay to the supertype
                        return supertype.getVersion(cls);
                    }
                }
                return attr;
            }
            throw new IllegalArgumentException("Version is not of specified type (" + cls.getName() + "). Should be " + verMmd.getTypeName());
        }

        // Maybe defined in superclass
        IdentifiableType supertype = getSupertype();
        if (supertype != null)
        {
            // Relay to the supertype
            return supertype.getVersion(cls);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#hasSingleIdAttribute()
     */
    public boolean hasSingleIdAttribute()
    {
        return cmd.getNoOfPrimaryKeyMembers() == 1;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.IdentifiableType#hasVersionAttribute()
     */
    public boolean hasVersionAttribute()
    {
        return cmd.getVersionMetaData() != null;
    }
}