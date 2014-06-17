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
package org.datanucleus.api.jpa.criteria;

import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Type;

/**
 * Implementation of JPA2 Criteria "Root".
 */
public class RootImpl<X> extends FromImpl<X, X> implements Root<X>
{
    private static final long serialVersionUID = 2822821218384976477L;
    private final EntityType<X> entity;

    public RootImpl(CriteriaBuilderImpl cb, EntityType<X> type)
    {
        super(cb, type);
        this.entity = type;
        this.alias("DN_THIS"); // Default to "DN_THIS" and let the user override
    }

    public EntityType<X> getModel()
    {
        return entity;
    }

    public Type<?> getType()
    {
        return entity;
    }
}