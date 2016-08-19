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

import javax.persistence.metamodel.EntityType;

import org.datanucleus.metadata.AbstractClassMetaData;

/**
 * Implementation of JPA Metamodel "EntityType".
 */
public class EntityTypeImpl<X> extends IdentifiableTypeImpl<X> implements EntityType<X>
{
    protected EntityTypeImpl(Class<X> cls, AbstractClassMetaData cmd, MetamodelImpl model)
    {
        super(cls, cmd, model);
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.EntityType#getName()
     */
    public String getName()
    {
        return cmd.getEntityName();
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Bindable#getBindableJavaType()
     */
    public Class<X> getBindableJavaType()
    {
        return getJavaType();
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Bindable#getBindableType()
     */
    public javax.persistence.metamodel.Bindable.BindableType getBindableType()
    {
        return BindableType.ENTITY_TYPE;
    }

    public PersistenceType getPersistenceType()
    {
        return PersistenceType.ENTITY;
    }
}