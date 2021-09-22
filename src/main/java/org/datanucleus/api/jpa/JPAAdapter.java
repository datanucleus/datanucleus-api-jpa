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

import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.api.jpa.metadata.JPAXmlMetaDataHelper;
import org.datanucleus.api.jpa.state.LifeCycleStateFactory;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.exceptions.NucleusCanRetryException;
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.exceptions.ReachableObjectNotCascadedException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.state.LifeCycleState;
import org.datanucleus.store.query.QueryTimeoutException;

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

    @Override
    public String getName()
    {
        return "JPA";
    }

    @Override
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

    @Override
    public String getXMLMetaDataForClass(AbstractClassMetaData cmd, String prefix, String indent)
    {
        return new JPAXmlMetaDataHelper().getXMLForMetaData(cmd, prefix, indent);
    }

    @Override
    public String getDefaultMappingFileLocation() 
    {
        return "META-INF/orm.xml";
    }

    // ------------------------------ Object Lifecycle --------------------------------

    @Override
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

    @Override
    public LifeCycleState getLifeCycleState(int stateType)
    {
        return LifeCycleStateFactory.getLifeCycleState(stateType);
    }

    @Override
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

    @Override
    public boolean isValidPrimaryKeyClass(Class pkClass, AbstractClassMetaData cmd, ClassLoaderResolver clr, int noOfPkFields, MetaDataManager mmgr)
    {
        return true;
    }

    // ------------------------------ Persistence --------------------------------

    @Override
    public boolean allowPersistOfDeletedObject()
    {
        // JPA allows re-persist of deleted objects
        return true;
    }

    @Override
    public boolean allowDeleteOfNonPersistentObject()
    {
        // JPA allows delete of transient objects so they cascade to all persistent objects
        return true;
    }

    @Override
    public boolean allowReadFieldOfDeletedObject()
    {
        return true;
    }

    @Override
    public boolean clearLoadedFlagsOnDeleteObject()
    {
        return false;
    }

    @Override
    public boolean getDefaultCascadePersistForField()
    {
        return false;
    }

    @Override
    public boolean getDefaultCascadeAttachForField()
    {
        return false;
    }

    @Override
    public boolean getDefaultCascadeDeleteForField()
    {
        return false;
    }

    @Override
    public boolean getDefaultCascadeDetachForField()
    {
        return false;
    }

    @Override
    public boolean getDefaultCascadeRefreshForField()
    {
        return false;
    }

    @Override
    public boolean getDefaultDFGForPersistableField()
    {
        // 1-1/N-1 default to being EAGER loaded
        return true;
    }

    @Override
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

    @Override
    public boolean getDefaultPersistentPropertyWhenNotSpecified()
    {
        return true;
    }

    @Override
    public RuntimeException getUserExceptionForException(String msg, Exception e)
    {
        return new javax.persistence.PersistenceException(msg, e);
    }

    @Override
    public RuntimeException getDataStoreExceptionForException(String msg, Exception e)
    {
        return new javax.persistence.PersistenceException(msg, e);
    }

    @Override
    public RuntimeException getApiExceptionForNucleusException(NucleusException ne)
    {
        return JPAAdapter.getJPAExceptionForNucleusException(ne);
    }

    /**
     * Convenience method to convert a Nucleus exception into a JPA exception.
     * If the incoming exception has a "failed object" then create the new exception with
     * a failed object. Otherwise if the incoming exception has nested exceptions then
     * create this exception with those nested exceptions. Else create this exception with
     * the incoming exception as its nested exception.
     * @param ne NucleusException
     * @return The JPAException
     */
    public static PersistenceException getJPAExceptionForNucleusException(NucleusException ne)
    {
        if (ne instanceof ReachableObjectNotCascadedException)
        {
            // Reachable object not persistent but field doesn't allow cascade-persist
            throw new IllegalStateException(ne.getMessage(), ne);
        }
        else if (ne instanceof QueryTimeoutException)
        {
            return new javax.persistence.QueryTimeoutException(ne.getMessage(), ne);
        }
        else if (ne instanceof NucleusDataStoreException)
        {
            // JPA doesn't have "datastore" exceptions so just give a PersistenceException
            if (ne.getNestedExceptions() != null)
            {
                return new PersistenceException(ne.getMessage(), ne.getCause());
            }
            return new PersistenceException(ne.getMessage(), ne);
        }
        else if (ne instanceof NucleusCanRetryException)
        {
            // JPA doesn't have "retry" exceptions so just give a PersistenceException
            if (ne.getNestedExceptions() != null)
            {
                return new PersistenceException(ne.getMessage(), ne.getCause());
            }
            return new PersistenceException(ne.getMessage(), ne);
        }
        else if (ne instanceof NucleusObjectNotFoundException)
        {
            return new EntityNotFoundException(ne.getMessage());
        }
        else if (ne instanceof NucleusUserException)
        {
            // JPA doesnt have "user" exceptions so just give a PersistenceException
            if (ne.getNestedExceptions() != null)
            {
                return new PersistenceException(ne.getMessage(), ne.getCause());
            }
            return new PersistenceException(ne.getMessage(), ne);
        }
        else if (ne instanceof NucleusOptimisticException)
        {
            if (ne.getNestedExceptions() != null)
            {
                return new OptimisticLockException(ne.getMessage(), ne.getCause());
            }
            return new OptimisticLockException(ne.getMessage(), ne);
        }
        else
        {
            // JPA doesnt have "internal" exceptions so just give a PersistenceException
            if (ne.getNestedExceptions() != null)
            {
                return new PersistenceException(ne.getMessage(), ne.getCause());
            }
            return new PersistenceException(ne.getMessage(), ne);
        }
    }
}