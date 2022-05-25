/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.properties.PropertyValidator;
import org.datanucleus.util.NucleusLogger;

/**
 * Validator for persistence properties used by JPA.
 */
public class JPAPropertyValidator implements PropertyValidator
{
    /**
     * Validate the specified property.
     * @param name Name of the property
     * @param value Value
     * @return Whether it is valid
     */
    public boolean validate(String name, Object value)
    {
        if (name == null)
        {
            return false;
        }
        else if (name.equals(JPAPropertyNames.PROPERTY_JPA_PERSISTENCE_CONTEXT_TYPE))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("transaction") ||
                    strVal.equals("extended"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(JPAPropertyNames.PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_DATABASE_ACTION) ||
                name.equals(JPAPropertyNames.PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_SCRIPTS_ACTION))
        {
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("none") ||
                    strVal.equals("create") ||
                    strVal.equals("drop-and-create") ||
                    strVal.equals("drop"))
                {
                    return true;
                }
            }
        }
        else if (name.equals(JPAPropertyNames.PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_CREATE_SRC) ||
                name.equals(JPAPropertyNames.PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_DROP_SRC))
        {
            NucleusLogger.METADATA.warn(name + " is currently ignored. Execute the scripts yourself!");
            if (value instanceof String)
            {
                String strVal = ((String)value).toLowerCase();
                if (strVal.equals("metadata") ||
                    strVal.equals("script") ||
                    strVal.equals("metadata-then-script") ||
                    strVal.equals("script-then-metadata"))
                {
                    return true;
                }
            }
        }
        return false;
    }
}