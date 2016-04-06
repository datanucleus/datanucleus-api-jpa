/**********************************************************************
Copyright (c) 2015 Andy Jefferson and others. All rights reserved.
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

import javax.persistence.TupleElement;

/**
 * Implementation of a JPA TupleElement, for a query result.
 */
public class JPAQueryTupleElement implements TupleElement
{
    protected String alias;
    protected Class type;
    protected Object value;

    public JPAQueryTupleElement(String alias, Class type, Object value)
    {
        this.alias = alias;
        this.type = type;
        this.value = value;
    }

    @Override
    public Class getJavaType()
    {
        return type;
    }

    @Override
    public String getAlias()
    {
        return alias;
    }

    public Object getValue()
    {
        return value;
    }

    public String toString()
    {
        return "TupleElement : alias=" + alias + " type=" + (type != null ? type.getName() : null);
    }
}