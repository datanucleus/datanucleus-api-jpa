/**********************************************************************
Copyright (c) 2021 Andy Jefferson and others. All rights reserved.
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

import java.lang.reflect.Field;

import javax.persistence.EntityManager;

import org.datanucleus.ExecutionContext;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.identity.DatastoreId;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.util.ClassUtils;

/**
 * Helper class for accessing DataNucleus internals from a JPA environment.
 * This is a supplement to using the wrap() methods on EntityManagerFactory and EntityManager.
 */
public class DataNucleusHelperJPA
{
    // ------------------------------ Object Management --------------------------------

    /**
     * Convenience method to allow access to the version of an object when it is using DN Extension "surrogate-version".
     * @param obj The entity object
     * @return The (surrogate) version
     */
    public static Object getSurrogateVersionForEntity(Object obj)
    {
        if (obj instanceof Persistable)
        {
            return ((Persistable)obj).dnGetVersion();
        }
        return null;
    }

    /**
     * Convenience method to allow access to the datastore id of an object when it is using DN Extension "datastore-id".
     * @param obj The entity object
     * @return The datastore id key
     */
    public static Object getDatastoreIdForEntity(Object obj)
    {
        if (obj instanceof Persistable)
        {
            Object id = ((Persistable)obj).dnGetObjectId();
            if (id instanceof DatastoreId)
            {
                return ((DatastoreId)id).getKeyAsObject();
            }
        }
        return null;
    }

    public static Object getObjectId(Object obj)
    {
        if (obj instanceof Persistable)
        {
            return ((Persistable)obj).dnGetObjectId();
        }
        return null;
    }

    /**
     * Accessor for the EntityManager for the supplied (persistable) object.
     * If the object is detached or transient then returns null.
     * @param obj The persistable object
     * @return The entity manager for this object
     */
    public static EntityManager getEntityManager(Object obj)
    {
        if (obj instanceof Persistable)
        {
            return (EntityManager) ((Persistable)obj).dnGetExecutionContext().getOwner();
        }
        return null;
    }

    /**
     * Convenience accessor for whether the object is persistent.
     * @param obj The object
     * @return Whether it is persistent
     */
    public static boolean isPersistent(Object obj)
    {
        if (obj instanceof Persistable)
        {
            return ((Persistable)obj).dnIsPersistent();
        }
        return false;
    }

    /**
     * Convenience accessor for whether the object is deleted.
     * @param obj The object
     * @return Whether it is deleted
     */
    public static boolean isDeleted(Object obj)
    {
        if (obj instanceof Persistable)
        {
            return ((Persistable)obj).dnIsDeleted();
        }
        return false;
    }

    /**
     * Convenience accessor for whether the object is detached.
     * @param obj The object
     * @return Whether it is persistent
     */
    public static boolean isDetached(Object obj)
    {
        if (obj instanceof Persistable)
        {
            return ((Persistable)obj).dnIsDetached();
        }
        return false;
    }

    /**
     * Convenience accessor for whether the object is transactional.
     * @param obj The object
     * @return Whether it is transactional
     */
    public static boolean isTransactional(Object obj)
    {
        if (obj instanceof Persistable)
        {
            return ((Persistable)obj).dnIsTransactional();
        }
        return false;
    }

    /**
     * Convenience method to return a string of the state of an object.
     * Will return things like "detached", "persistent", etc
     * @param obj The object
     * @return The state
     */
    public static String getObjectState(Object obj)
    {
        if (obj == null)
        {
            return null;
        }

        if (isDetached(obj))
        {
            return "detached";
        }
        else if (isPersistent(obj))
        {
            if (isTransactional(obj))
            {
                if (isDeleted(obj))
                {
                    return "persistent-deleted";
                }
                return "persistent";
            }
            // Likely HOLLOW for some reason
            return "persistent";
        }

        return "transient";
    }

    // ------------------------------ Convenience --------------------------------

    /**
     * Accessor for the jdoDetachedState field of a detached object.
     * The returned array is made up of :
     * <ul>
     * <li>0 - the identity of the object</li>
     * <li>1 - the version of the object (upon detach)</li>
     * <li>2 - loadedFields BitSet</li>
     * <li>3 - dirtyFields BitSet</li>
     * </ul>
     * @param obj The detached object
     * @return The detached state
     */
    public static Object[] getDetachedStateForObject(Object obj)
    {
        if (obj == null || !isDetached(obj))
        {
            return null;
        }
        try
        {
            Field fld = ClassUtils.getFieldForClass(obj.getClass(), "dnDetachedState");
            fld.setAccessible(true);
            return (Object[]) fld.get(obj);
        }
        catch (Exception e)
        {
            throw new NucleusException("Exception accessing dnDetachedState field", e);
        }
    }

    /**
     * Accessor for the names of the dirty fields of the persistable object.
     * @param obj The persistable object
     * @param em The Entity Manager (only required if the object is detached)
     * @return Names of the dirty fields
     */
    public static String[] getDirtyFields(Object obj, EntityManager em)
    {
        if (obj == null || !(obj instanceof Persistable))
        {
            return null;
        }

        Persistable pc = (Persistable)obj;
        if (isDetached(pc))
        {
            ExecutionContext ec = ((JPAEntityManager)em).getExecutionContext();

            // Temporarily attach a StateManager to access the detached field information
            ObjectProvider op = ec.getNucleusContext().getObjectProviderFactory().newForDetached(ec, pc, pc.dnGetObjectId(), null);
            pc.dnReplaceStateManager(op);
            op.retrieveDetachState(op);
            String[] dirtyFieldNames = op.getDirtyFieldNames();
            pc.dnReplaceStateManager(null);

            return dirtyFieldNames;
        }

        ExecutionContext ec = (ExecutionContext) pc.dnGetExecutionContext();
        ObjectProvider op = ec.findObjectProvider(pc);
        return op == null ? null : op.getDirtyFieldNames();
    }

    /**
     * Accessor for the names of the loaded fields of the persistable object.
     * @param obj Persistable object
     * @param em The Entity Manager (only required if the object is detached)
     * @return Names of the loaded fields
     */
    public static String[] getLoadedFields(Object obj, EntityManager em)
    {
        if (obj == null || !(obj instanceof Persistable))
        {
            return null;
        }

        Persistable pc = (Persistable)obj;
        if (isDetached(pc))
        {
            // Temporarily attach a StateManager to access the detached field information
            ExecutionContext ec = ((JPAEntityManager)em).getExecutionContext();
            ObjectProvider op = ec.getNucleusContext().getObjectProviderFactory().newForDetached(ec, pc, pc.dnGetObjectId(), null);
            pc.dnReplaceStateManager(op);
            op.retrieveDetachState(op);
            String[] loadedFieldNames = op.getLoadedFieldNames();
            pc.dnReplaceStateManager(null);

            return loadedFieldNames;
        }

        ExecutionContext ec = (ExecutionContext) pc.dnGetExecutionContext();
        ObjectProvider op = ec.findObjectProvider(pc);
        return op == null ? null : op.getLoadedFieldNames();
    }

    /**
     * Accessor for whether the specified member (field/property) of the passed persistable object is loaded.
     * @param obj The persistable object
     * @param memberName Name of the field/property
     * @param em EntityManager (if the object is detached)
     * @return Whether the member is loaded
     */
    public static Boolean isFieldLoaded(Object obj, String memberName, EntityManager em)
    {
        if (obj == null || !(obj instanceof Persistable))
        {
            return null;
        }

        Persistable pc = (Persistable)obj;
        if (pc.dnIsDetached())
        {
            // Temporarily attach a StateManager to access the detached field information
            ExecutionContext ec = ((JPAEntityManager)em).getExecutionContext();
            ObjectProvider op = ec.getNucleusContext().getObjectProviderFactory().newForDetached(ec, pc, pc.dnGetObjectId(), null);
            pc.dnReplaceStateManager(op);
            op.retrieveDetachState(op);
            int position = op.getClassMetaData().getAbsolutePositionOfMember(memberName);
            boolean loaded = op.isFieldLoaded(position);
            pc.dnReplaceStateManager(null);

            return loaded;
        }

        ExecutionContext ec = (ExecutionContext) pc.dnGetExecutionContext();
        ObjectProvider op = ec.findObjectProvider(pc);
        if (op == null)
        {
            return null;
        }
        int position = op.getClassMetaData().getAbsolutePositionOfMember(memberName);
        return op.isFieldLoaded(position);
    }

    /**
     * Accessor for whether the specified member (field/property) of the passed persistable object is dirty.
     * @param obj The persistable object
     * @param memberName Name of the field/property
     * @param pm PersistenceManager (if the object is detached)
     * @return Whether the member is dirty
     */
    public static Boolean isFieldDirty(Object obj, String memberName, EntityManager em)
    {
        if (obj == null || !(obj instanceof Persistable))
        {
            return null;
        }

        Persistable pc = (Persistable)obj;
        if (pc.dnIsDetached())
        {
            // Temporarily attach a StateManager to access the detached field information
            ExecutionContext ec = ((JPAEntityManager)em).getExecutionContext();
            ObjectProvider op = ec.getNucleusContext().getObjectProviderFactory().newForDetached(ec, pc, pc.dnGetObjectId(), null);
            pc.dnReplaceStateManager(op);
            op.retrieveDetachState(op);
            int position = op.getClassMetaData().getAbsolutePositionOfMember(memberName);
            boolean[] dirtyFieldNumbers = op.getDirtyFields();
            pc.dnReplaceStateManager(null);

            return dirtyFieldNumbers[position];
        }

        ExecutionContext ec = (ExecutionContext) pc.dnGetExecutionContext();
        ObjectProvider op = ec.findObjectProvider(pc);
        if (op == null)
        {
            return null;
        }
        int position = op.getClassMetaData().getAbsolutePositionOfMember(memberName);
        boolean[] dirtyFieldNumbers = op.getDirtyFields();
        return dirtyFieldNumbers[position];
    }

    /**
     * Convenience method to mark the specified member (field/property) as dirty, when managed.
     * Normally, <code>Persistable</code> classes are able to detect changes made to their fields, however if a reference to an array is 
     * given to a method outside the class, and the array is modified, then the persistent instance is not aware of the change.  
     * This API allows the application to notify the instance that a change was made to a field.
     * @param obj The persistable object
     * @param memberName The member to mark as dirty
     */
    public static void makeFieldDirty(Object obj, String memberName)
    {
        if (obj == null || !(obj instanceof Persistable))
        {
            return;
        }

        ((Persistable)obj).dnMakeDirty(memberName);
    }
}