/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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

/**
 * Utility providing convenience naming of JPA persistence properties.
 */
public class JPAPropertyNames
{
    public static final String PROPERTY_JPA_ADD_CLASS_TRANSFORMER = "datanucleus.jpa.addClassTransformer";
    public static final String PROPERTY_JPA_PERSISTENCE_CONTEXT_TYPE = "datanucleus.jpa.persistenceContextType";
    public static final String PROPERTY_JPA_TRANSACTION_ROLLBACK_ON_EXCEPTION = "datanucleus.jpa.txnMarkForRollbackOnException";
    public static final String PROPERTY_JPA_FIND_TYPE_CONVERSION = "datanucleus.jpa.findTypeConversion";
    public static final String PROPERTY_JPA_SINGLETON_EMF_FOR_NAME = "datanucleus.singletonEMFForName";

    public static final String PROPERTY_JPA_STANDARD_JDBC_URL = "javax.persistence.jdbc.url";
    public static final String PROPERTY_JPA_STANDARD_JDBC_DRIVER = "javax.persistence.jdbc.driver";
    public static final String PROPERTY_JPA_STANDARD_JDBC_USER = "javax.persistence.jdbc.user";
    public static final String PROPERTY_JPA_STANDARD_JDBC_PASSWORD = "javax.persistence.jdbc.password";

    public static final String PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_DATABASE_ACTION = "javax.persistence.schema-generation.database.action";
    public static final String PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_SCRIPTS_ACTION = "javax.persistence.schema-generation.scripts.action";

    public static final String PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_CREATE_SCRIPT_SRC = "javax.persistence.schema-generation.create-script-source";
    public static final String PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_CREATE_SRC = "javax.persistence.schema-generation.create-source";

    public static final String PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_DROP_SCRIPT_SRC = "javax.persistence.schema-generation.drop-script-source";
    public static final String PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_DROP_SRC = "javax.persistence.schema-generation.drop-source";

    public static final String PROPERTY_JPA_STANDARD_SQL_LOAD_SCRIPT_SRC = "javax.persistence.sql.load-script-source";
}