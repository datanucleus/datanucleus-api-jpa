/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved.
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
2008 Andy Jefferson - addition of persistence, identity methods
     ...
 **********************************************************************/
package org.datanucleus.api.jpa;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.api.jpa.state.LifeCycleStateFactory;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.state.LifeCycleState;

/**
 * Adapter for the JPA API, to allow the DataNucleus core runtime to expose multiple APIs to clients.
 */
public class JPAAdapter implements ApiAdapter
{
    private static final long serialVersionUID = 7676231809409935625L;
    protected final static Set<String> defaultPersistentTypeNames = new HashSet<String>();

    static
    {
        defaultPersistentTypeNames.add(ClassNameConstants.BOOLEAN);
        defaultPersistentTypeNames.add(ClassNameConstants.BYTE);
        defaultPersistentTypeNames.add(ClassNameConstants.CHAR);
        defaultPersistentTypeNames.add(ClassNameConstants.DOUBLE);
        defaultPersistentTypeNames.add(ClassNameConstants.FLOAT);
        defaultPersistentTypeNames.add(ClassNameConstants.INT);
        defaultPersistentTypeNames.add(ClassNameConstants.LONG);
        defaultPersistentTypeNames.add(ClassNameConstants.SHORT);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_BOOLEAN);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_BYTE);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_CHARACTER);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_DOUBLE);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_FLOAT);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_INTEGER);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_LONG);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_SHORT);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_STRING);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_UTIL_DATE);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_SQL_DATE);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_SQL_TIME);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_SQL_TIMESTAMP);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_MATH_BIGDECIMAL);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_MATH_BIGINTEGER);
        defaultPersistentTypeNames.add(ClassNameConstants.BYTE_ARRAY);
        defaultPersistentTypeNames.add(ClassNameConstants.CHAR_ARRAY);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_BYTE_ARRAY);
        defaultPersistentTypeNames.add(ClassNameConstants.JAVA_LANG_CHARACTER_ARRAY);
    }

    /**
     * Accessor for the name of the API.
     * @return Name of the API
     */
    public String getName()
    {
        return "JPA";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.ApiAdapter#isMemberDefaultPersistent(java.lang.Class)
     */
    public boolean isMemberDefaultPersistent(Class type)
    {
        String typeName = type.getName();
        if (defaultPersistentTypeNames.contains(typeName))
        {
            return true;
        }
        else if (java.util.Calendar.class.isAssignableFrom(type) ||
            Enum.class.isAssignableFrom(type) ||
            Serializable.class.isAssignableFrom(type))
        {
            return true;
        }
        else if (isPersistable(type))
        {
            return true;
        }
        return false;
    }

    // ------------------------------ Object Lifecycle --------------------------------

    /**
     * Method to return the ExecutionContext (if any) associated with the passed object.
     * Supports persistable objects, and EntityManager.
     * @param obj The object
     * @return The ExecutionContext
     */
    public ExecutionContext getExecutionContext(Object obj)
    {
        if (obj == null)
        {
            return null;
        }
        if (obj instanceof Persistable)
        {
            return (ExecutionContext) ((Persistable)obj).dnGetExecutionContext();
        }
        else if (obj instanceof JPAEntityManager)
        {
            return ((JPAEntityManager)obj).getExecutionContext();
        }
        return null;
    }

    /**
     * Returns the LifeCycleState for the state constant.
     * @param stateType the type as integer
     * @return the type as LifeCycleState object
     */
    public LifeCycleState getLifeCycleState(int stateType)
    {
        return LifeCycleStateFactory.getLifeCycleState(stateType);
    }

    /**
     * Accessor for the object state.
     * @param pc Object
     * @return The state ("persistent-clean", "detached-dirty" etc)
     */
    public String getObjectState(Object pc)
    {
        if (pc == null)
        {
            return null;
        }

        if (isDetached(pc))
        {
            if (isDirty(pc))
            {
                // Detached Dirty
                return "detached-dirty";
            }
            // Detached Not Dirty
            return "detached-clean";
        }

        if (isPersistent(pc))
        {
            if (isTransactional(pc))
            {
                if (isDirty(pc))
                {
                    if (isNew(pc))
                    {
                        if (isDeleted(pc))
                        {
                            // Persistent Transactional Dirty New Deleted
                            return "persistent-new-deleted";
                        }
                        // Persistent Transactional Dirty New Not Deleted
                        return "persistent-new";
                    }

                    if (isDeleted(pc))
                    {
                        // Persistent Transactional Dirty Not New Deleted
                        return "persistent-deleted";
                    }
                    // Persistent Transactional Dirty Not New Not Deleted
                    return "persistent-dirty";
                }

                // Persistent Transactional Not Dirty
                return "persistent-clean";
            }

            if (isDirty(pc))
            {
                // Persistent Nontransactional Dirty
                return "persistent-nontransactional-dirty";
            }
            // Persistent Nontransactional Not Dirty
            return "hollow/persistent-nontransactional";
        }

        if (isTransactional(pc))
        {
            if (isDirty(pc))
            {
                // Not Persistent Transactional Dirty
                return "transient-dirty";
            }

            // Not Persistent Transactional Not Dirty
            return "transient-clean";
        }

        // Not Persistent Not Transactional
        return "transient";
    }

    // ------------------------------ Object Identity  --------------------------------

    /**
     * Utility to check if a primary-key class is valid.
     * Will throw a InvalidPrimaryKeyException if it is invalid, otherwise returning true.
     * @param pkClass The Primary Key class
     * @param cmd AbstractClassMetaData for the Persistable class
     * @param clr the ClassLoaderResolver
     * @param noOfPkFields Number of primary key fields
     * @param mmgr MetaData manager
     * @return Whether it is valid
     */
    public boolean isValidPrimaryKeyClass(Class pkClass, AbstractClassMetaData cmd, ClassLoaderResolver clr, int noOfPkFields, MetaDataManager mmgr)
    {
        return true;
    }

    // ------------------------------ Persistence --------------------------------

    /**
     * Whether the API allows (re-)persistence of a deleted object.
     * @return Whether you can call persist on a deleted object
     */
    public boolean allowPersistOfDeletedObject()
    {
        // JPA allows re-persist of deleted objects
        return true;
    }

    /**
     * Whether the API allows deletion of a non-persistent object.
     * @return Whether you can call delete on an object not yet persisted
     */
    public boolean allowDeleteOfNonPersistentObject()
    {
        // JPA allows delete of transient objects so they cascade to all persistent objects
        return true;
    }

    /**
     * Whether the API allows reading a field of a deleted object.
     * @return Whether you can read after deleting
     */
    public boolean allowReadFieldOfDeletedObject()
    {
        return true;
    }

    /**
     * Whether the API requires clearing of the fields of an object when it is deleted.
     * @return Whether to clear loaded fields at delete
     */
    public boolean clearLoadedFlagsOnDeleteObject()
    {
        return false;
    }

    /**
     * Returns the default cascade-persist setting. JPA defaults to not persisting by reachability.
     * @return The default cascade-persist (false)
     */
    public boolean getDefaultCascadePersistForField()
    {
        return false;
    }

    /**
     * Returns the default cascade-update setting. JPA defaults to not updating by reachability.
     * @return The default cascade-update (false)
     */
    public boolean getDefaultCascadeUpdateForField()
    {
        return false;
    }

    /**
     * Returns the default cascade-delete setting. JPA defaults to not deleting by reachability.
     * @return The default cascade-delete (false)
     */
    public boolean getDefaultCascadeDeleteForField()
    {
        return false;
    }

    /**
     * Returns the default cascade-refresh setting.
     * @return The default cascade-refresh (false)
     */
    public boolean getDefaultCascadeRefreshForField()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.ApiAdapter#getDefaultDFGForPersistableField()
     */
    public boolean getDefaultDFGForPersistableField()
    {
        // 1-1/N-1 default to being EAGER loaded
        return true;
    }

    /**
     * Method to return the default factory properties for this API.
     * @return The default props
     */
    public Map getDefaultFactoryProperties()
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT, "true"); // detachAllOnCommit in JPA
        props.put(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK, "true"); // detachAllOnRollback in JPA
        props.put(PropertyNames.PROPERTY_COPY_ON_ATTACH, "true"); // JPA spec 3.2.7.1 attach onto copy
        props.put(PropertyNames.PROPERTY_RETAIN_VALUES, "true");
//        props.put("datanucleus.RestoreValues", "true");
        props.put(PropertyNames.PROPERTY_OPTIMISTIC, "true"); // JPA uses optimistic txns
        props.put(PropertyNames.PROPERTY_TRANSACTION_NONTX_ATOMIC, "false"); // JPA assumes non-atomic non-tx ops
        props.put(PropertyNames.PROPERTY_METADATA_ALLOW_LOAD_AT_RUNTIME, "false"); // Default to no load later on
        props.put(PropertyNames.PROPERTY_IDENTIFIER_NAMING_FACTORY, "jpa"); // JPA identifier naming (non-RDBMS)
        props.put(PropertyNames.PROPERTY_IDENTIFIER_FACTORY, "jpa"); // JPA identifier naming (RDBMS)
        props.put(PropertyNames.PROPERTY_PERSISTENCE_BY_REACHABILITY_AT_COMMIT, "false"); // No PBR at commit with JPA
        props.put(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS, "false"); // JPA doesn't have this
        props.put(PropertyNames.PROPERTY_MANAGE_RELATIONSHIPS_CHECKS, "false"); // JPA doesn't have this
        props.put(PropertyNames.PROPERTY_QUERY_SQL_ALLOWALL, "true"); // No restrictions on SQL statements in JPA
        props.put(PropertyNames.PROPERTY_MAX_FETCH_DEPTH, "-1"); // Don't limit fetches for JPA
        props.put(PropertyNames.PROPERTY_FIND_OBJECT_VALIDATE_WHEN_CACHED, "false"); // Don't validate with JPA
        props.put(PropertyNames.PROPERTY_USE_IMPLEMENTATION_CREATOR, "false"); // Feature of JDO only
        props.put(PropertyNames.PROPERTY_ALLOW_ATTACH_OF_TRANSIENT, "true"); // Some JPA impls assume this even though not in the spec
        props.put(PropertyNames.PROPERTY_METADATA_USE_DISCRIMINATOR_DEFAULT_CLASS_NAME, "false"); // DN <= 5.0.2 effectively used value-map (class-name), but now we use entity-name
        props.put(PropertyNames.PROPERTY_METADATA_USE_DISCRIMINATOR_FOR_SINGLE_TABLE, "true"); // DN <= 5.0.2 didn't automatically add a discriminator for single-table, but now we do

        props.put("datanucleus.rdbms.allowColumnReuse", "true"); // So that JPA usage defaults to how other implementations do it, ignoring safety of this feature

        return props;
    }

    /**
     * Convenience method to return an exception to throw for this API when an unexpected
     * exception occurs. This is considered a user exception.
     * @param msg The message
     * @param e The cause
     * @return The JPA exception
     */
    public RuntimeException getUserExceptionForException(String msg, Exception e)
    {
        return new javax.persistence.PersistenceException(msg, e);
    }

    /**
     * Convenience method to return a datastore exception appropriate for this API.
     * @param msg The message
     * @param e Any root cause exception
     * @return The exception
     */
    public RuntimeException getDataStoreExceptionForException(String msg, Exception e)
    {
        return new javax.persistence.PersistenceException(msg, e);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.ApiAdapter#getApiExceptionForNucleusException(org.datanucleus.exceptions.NucleusException)
     */
    public RuntimeException getApiExceptionForNucleusException(NucleusException ne)
    {
        return NucleusJPAHelper.getJPAExceptionForNucleusException(ne);
    }
}