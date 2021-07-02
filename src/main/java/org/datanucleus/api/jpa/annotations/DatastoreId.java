/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.api.jpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.Column;
import javax.persistence.GenerationType;

/**
 * Extension annotation allowing for use of datastore-identity with JPA.
 */
@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
public @interface DatastoreId
{
    /** 
     * Strategy for generating values for the datastore identity. 
     * @return The generation type
     */
    GenerationType generationType() default GenerationType.AUTO;

    /**
     * Name of the generator to use (if using generationType of TABLE or SEQUENCE).
     * @return The generator
     */
    String generator() default "";

    /** 
     * Name of the column to use for datastore identity.
     * @return The column name
     */
    String column() default "";

    /**
     * Column definition(s) to use for the datastore identity surrogate.
     * Only processes a single column, but annotations cant have a default of null.
     * @return The surrogate column definition(s)
     */
    Column[] columns() default {};
}