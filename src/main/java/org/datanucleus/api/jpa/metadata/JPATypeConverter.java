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
package org.datanucleus.api.jpa.metadata;

import javax.persistence.AttributeConverter;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Wrapper for a type converter provided by a user, so it can be used by the internal TypeConverter mechanism.
 */
public class JPATypeConverter<X, Y> implements TypeConverter<X, Y>
{
    private static final long serialVersionUID = -4533920769365489446L;

    /** The user-provided entity converter. */
    AttributeConverter<X, Y> entityConverter;

    public JPATypeConverter(AttributeConverter<X, Y> entityConv)
    {
        this.entityConverter = entityConv;
    }

    public Y toDatastoreType(X memberValue)
    {
        return entityConverter.convertToDatabaseColumn(memberValue);
    }

    public X toMemberType(Y datastoreValue)
    {
        return entityConverter.convertToEntityAttribute(datastoreValue);
    }

    public String toString()
    {
        return "JPATypeConverter for " + entityConverter;
    }
}