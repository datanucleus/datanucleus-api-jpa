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
package org.datanucleus.api.jpa;

import javax.persistence.Parameter;

/**
 * Implementation of a JPA query parameter.
 * @param <T> Type of the parameter
 */
public class JPAQueryParameter<T> implements Parameter<T>
{
    String name = null;
    Integer position = null;
    Class<T> type = null;

    public JPAQueryParameter(String name, Class<T> type)
    {
        this.name = name;
        this.type = type;
    }

    public JPAQueryParameter(Integer pos, Class<T> type)
    {
        this.position = pos;
        this.type = type;
    }

    /**
     * Return the parameter name, or null if the parameter is not a named parameter.
     * @return parameter name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Return the parameter position, or null if the parameter is not a positional parameter.
     * @return position of parameter
     */
    public Integer getPosition()
    {
        return position;
    }

    /* (non-Javadoc)
     * @see javax.persistence.Parameter#getParameterType()
     */
    public Class<T> getParameterType()
    {
        return type;
    }
}