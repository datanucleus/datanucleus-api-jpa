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

import java.util.Set;

import javax.persistence.metamodel.SetAttribute;

import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Implementation of JPA2 metamodel "SetAttribute".
 * 
 * @param <X> The type the represented Set belongs to
 * @param <E> The element type of the represented Set
 */
public class SetAttributeImpl<X, E> extends PluralAttributeImpl<X, Set<E>, E> implements SetAttribute<X, E>
{
    /**
     * Constructor for a SetAttribute.
     * @param mmd Metadata for the member
     * @param owner The owner class
     */
    public SetAttributeImpl(AbstractMemberMetaData mmd, ManagedTypeImpl owner)
    {
        super(mmd, owner);
    }

    public CollectionType getCollectionType()
    {
        return CollectionType.SET;
    }
}