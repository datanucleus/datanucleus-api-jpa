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
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.JDONullIdentityException;
import javax.jdo.PersistenceManager;
import javax.jdo.identity.ByteIdentity;
import javax.jdo.identity.CharIdentity;
import javax.jdo.identity.IntIdentity;
import javax.jdo.identity.LongIdentity;
import javax.jdo.identity.ObjectIdentity;
import javax.jdo.identity.ShortIdentity;
import javax.jdo.identity.SingleFieldIdentity;
import javax.jdo.identity.StringIdentity;
import javax.jdo.spi.Detachable;
import javax.jdo.spi.PersistenceCapable;
import javax.jdo.spi.PersistenceCapable.ObjectIdFieldConsumer;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.state.AppIdObjectIdFieldConsumer;
import org.datanucleus.api.jpa.state.LifeCycleStateFactory;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.OID;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.state.LifeCycleState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Adapter for the JPA API, to allow the DataNucleus core runtime to expose multiple APIs to clients.
 */
public class JPAAdapter implements ApiAdapter
{
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        NucleusJPAHelper.class.getClassLoader());

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
     * Whether the provided object is currently managed (has an ExecutionContext).
     * @return Whether it is managed
     */
    public boolean isManaged(Object pc)
    {
        return (getExecutionContext(pc) != null);
    }

    /**
     * Method to return the ExecutionContext (if any) associated with the passed object.
     * Supports persistable objects, and PersistenceManager.
     * @param obj The object
     * @return The ExecutionContext
     */
    public ExecutionContext getExecutionContext(Object obj)
    {
        if (obj == null)
        {
            return null;
        }

        // TODO Try to avoid JDO-specific class usage here
        if (obj instanceof PersistenceCapable)
        {
            PersistenceManager pm = JDOHelper.getPersistenceManager(obj);
            if (pm == null)
            {
                return null;
            }
            return ((JPAPersistenceManager)pm).getExecutionContext();
        }
        else if (obj instanceof PersistenceManager)
        {
            return ((JPAPersistenceManager)obj).getExecutionContext();
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
     * Accessor for whether the passed object is persistent.
     * @param obj The object
     * @return Whether it is persistent
     */
    public boolean isPersistent(Object obj)
    {
        // Relay through to JDOHelper - TODO Change this when we JPOX-JPA doesnt depend on JDO
        return JDOHelper.isPersistent(obj);
    }

    /**
     * Accessor for whether the passed object is new.
     * @param obj The object
     * @return Whether it is new
     */
    public boolean isNew(Object obj)
    {
        // Relay through to JDOHelper - TODO Change this when we JPOX-JPA doesnt depend on JDO
        return JDOHelper.isNew(obj);
    }

    /**
     * Accessor for whether the passed object is dirty.
     * @param obj The object
     * @return Whether it is dirty
     */
    public boolean isDirty(Object obj)
    {
        // Relay through to JDOHelper - TODO Change this when we JPOX-JPA doesnt depend on JDO
        return JDOHelper.isDirty(obj);
    }

    /**
     * Accessor for whether the passed object is deleted.
     * @param obj The object
     * @return Whether it is deleted
     */
    public boolean isDeleted(Object obj)
    {
        // Relay through to JDOHelper - TODO Change this when we JPOX-JPA doesnt depend on JDO
        return JDOHelper.isDeleted(obj);
    }

    /**
     * Accessor for whether the passed object is detached.
     * @param obj The object
     * @return Whether it is detached
     */
    public boolean isDetached(Object obj)
    {
        // Relay through to JDOHelper - TODO Change this when we JPOX-JPA doesnt depend on JDO
        return JDOHelper.isDetached(obj);
    }

    /**
     * Accessor for whether the passed object is transactional.
     * @param obj The object
     * @return Whether it is transactional
     */
    public boolean isTransactional(Object obj)
    {
        // Relay through to JDOHelper - TODO Change this when we JPOX-JPA doesnt depend on JDO
        return JDOHelper.isTransactional(obj);
    }

    /**
     * Method to return if the passed object is persistable using this API.
     * @param obj The object
     * @return Whether it is persistable
     */
    public boolean isPersistable(Object obj)
    {
        if (obj == null)
        {
            return false;
        }

        // TODO Change this to org.datanucleus.api.jpa.Persistable when we enhance to that
        return (obj instanceof PersistenceCapable);
    }

    /**
     * Utility method to check if the specified class is of a type that can be persisted for this API.
     * @param cls The class to check
     * @return Whether the class is persistable
     */
    public boolean isPersistable(Class cls)
    {
        if (cls == null)
        {
            return false;
        }
        // TODO Change this to org.datanucleus.api.jpa.Persistable when we enhance to that
        return (PersistenceCapable.class.isAssignableFrom(cls));
    }

    /**
     * Method to return if the passed object is detachable using this API.
     * Returns whether the object is an instance of javax.jdo.spi.Detachable.
     * @param obj The object
     * @return Whether it is detachable
     */
    public boolean isDetachable(Object obj)
    {
        if (obj == null)
        {
            return false;
        }

        // TODO Change this to org.datanucleus.api.jpa.Persistable when we enhance to that
        return (obj instanceof Detachable);
    }

    /**
     * Accessor for the object state.
     * @param obj Object
     * @return The state ("persistent-clean", "detached-dirty" etc)
     */
    public String getObjectState(Object obj)
    {
        return JDOHelper.getObjectState(obj).toString();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.ApiAdapter#makeDirty(java.lang.Object, java.lang.String)
     */
    public void makeDirty(Object obj, String member)
    {
        ((PersistenceCapable)obj).jdoMakeDirty(member);
    }

    // ------------------------------ Object Identity  --------------------------------

    /**
     * Method to return the object identity for the passed persistable object.
     * Returns null if it is not persistable, or has no identity.
     * @param obj The object
     * @return The identity
     */
    public Object getIdForObject(Object obj)
    {
        if (!isPersistable(obj))
        {
            return null;
        }
        // TODO Change this to org.datanucleus.api.jpa.Persistable when we enhance to that
        return ((PersistenceCapable)obj).jdoGetObjectId();
    }

    /**
     * Method to return the object version for the passed persistable object.
     * Returns null if it is not persistable, or not versioned.
     * @param obj The object
     * @return The version
     */
    public Object getVersionForObject(Object obj)
    {
        if (!isPersistable(obj))
        {
            return null;
        }
        // TODO Change this to org.datanucleus.api.jpa.Persistable when we enhance to that
        return ((PersistenceCapable)obj).jdoGetVersion();
    }

    /**
     * Utility to check if a primary-key class is valid.
     * Will throw a InvalidPrimaryKeyException if it is invalid, otherwise returning true.
     * @param pkClass The Primary Key class
     * @param cmd AbstractClassMetaData for the PersistenceCapable class
     * @param clr the ClassLoaderResolver
     * @param noOfPkFields Number of primary key fields
     * @param mmgr MetaData manager
     * @return Whether it is valid
     */
    public boolean isValidPrimaryKeyClass(Class pkClass, AbstractClassMetaData cmd, ClassLoaderResolver clr,
            int noOfPkFields, MetaDataManager mmgr)
    {
        return true;
    }

    /**
     * Accessor for whether the passed identity is a valid single-field application-identity for this API.
     * @return Whether it is valid
     */
    public boolean isSingleFieldIdentity(Object id)
    {
        // TODO Use DN-internal SingleFieldIdentity
        return (id instanceof SingleFieldIdentity);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.ApiAdapter#isDatastoreIdentity(java.lang.Object)
     */
    public boolean isDatastoreIdentity(Object id)
    {
        return (id != null && id instanceof OID);
    }

    /**
     * Checks whether the passed class name is valid for a single field application-identity for this API.
     * @param className the full class name
     * @return Whether it is a single field class
     */
    public boolean isSingleFieldIdentityClass(String className)
    {
        if (className == null || className.length() < 1)
        {
            return false;
        }

        // TODO Use internal SingleFieldIdentity
        return (className.equals(getSingleFieldIdentityClassNameForByte()) || 
                className.equals(getSingleFieldIdentityClassNameForChar()) || 
                className.equals(getSingleFieldIdentityClassNameForInt()) ||
                className.equals(getSingleFieldIdentityClassNameForLong()) || 
                className.equals(getSingleFieldIdentityClassNameForObject()) || 
                className.equals(getSingleFieldIdentityClassNameForShort()) ||
                className.equals(getSingleFieldIdentityClassNameForString()));
    }

    /**
     * Accessor for the class name to use for identities when there is a single Long/long field.
     * @return Class name of identity class
     */
    public String getSingleFieldIdentityClassNameForLong()
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        return LongIdentity.class.getName();
    }

    /**
     * Accessor for the class name to use for identities when there is a single Integer/int field.
     * @return Class name of identity class
     */
    public String getSingleFieldIdentityClassNameForInt()
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        return IntIdentity.class.getName();
    }

    /**
     * Accessor for the class name to use for identities when there is a single Short/short field.
     * @return Class name of identity class
     */
    public String getSingleFieldIdentityClassNameForShort()
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        return ShortIdentity.class.getName();
    }

    /**
     * Accessor for the class name to use for identities when there is a single Byte/byte field.
     * @return Class name of identity class
     */
    public String getSingleFieldIdentityClassNameForByte()
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        return ByteIdentity.class.getName();
    }

    /**
     * Accessor for the class name to use for identities when there is a single Character/char field.
     * @return Class name of identity class
     */
    public String getSingleFieldIdentityClassNameForChar()
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        return CharIdentity.class.getName();
    }

    /**
     * Accessor for the class name to use for identities when there is a single String field.
     * @return Class name of identity class
     */
    public String getSingleFieldIdentityClassNameForString()
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        return StringIdentity.class.getName();
    }

    /**
     * Accessor for the class name to use for identities when there is a single Object field.
     * @return Class name of identity class
     */
    public String getSingleFieldIdentityClassNameForObject()
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        return ObjectIdentity.class.getName();
    }

    /**
     * Accessor for the target class for the specified single field application-identity.
     * @param id The identity
     * @return The target class
     */
    public Class getTargetClassForSingleFieldIdentity(Object id)
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        if (id instanceof SingleFieldIdentity)
        {
            return ((SingleFieldIdentity)id).getTargetClass();
        }
        return null;
    }

    /**
     * Accessor for the target class name for the specified single field identity.
     * @param id The identity
     * @return The target class name
     */
    public String getTargetClassNameForSingleFieldIdentity(Object id)
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        if (id instanceof SingleFieldIdentity)
        {
            return ((SingleFieldIdentity)id).getTargetClassName();
        }
        return null;
    }

    /**
     * Accessor for the key object for the specified single field application-identity.
     * @param id The identity
     * @return The key object
     */
    public Object getTargetKeyForSingleFieldIdentity(Object id)
    {
        // TODO Use JPOX-internal SingleFieldIdentity
        if (id instanceof SingleFieldIdentity)
        {
            return ((SingleFieldIdentity)id).getKeyAsObject();
        }
        return null;
    }

    /**
     * Accessor for the type of the single field application-identity key given the single field identity type.
     * @param idType Single field identity type
     * @return key type
     */
    public Class getKeyTypeForSingleFieldIdentityType(Class idType)
    {
        if (idType == null)
        {
            return null;
        }
        if (!isSingleFieldIdentityClass(idType.getName()))
        {
            return null;
        }

        // TODO Use DN-internal SingleFieldIdentity
        if (LongIdentity.class.isAssignableFrom(idType))
        {
            return Long.class;
        }
        else if (IntIdentity.class.isAssignableFrom(idType))
        {
            return Integer.class;
        }
        else if (ShortIdentity.class.isAssignableFrom(idType))
        {
            return Short.class;
        }
        else if (ByteIdentity.class.isAssignableFrom(idType))
        {
            return Byte.class;
        }
        else if (CharIdentity.class.isAssignableFrom(idType))
        {
            return Character.class;
        }
        else if (StringIdentity.class.isAssignableFrom(idType))
        {
            return String.class;
        }
        else if (ObjectIdentity.class.isAssignableFrom(idType))
        {
            return Object.class;
        }
        return null;
    }

    /**
     * Utility to create a new SingleFieldIdentity using reflection when you know the
     * type of the PersistenceCapable, and also which SingleFieldIdentity, and the value of the key.
     * @param idType Type of SingleFieldIdentity
     * @param pcType Type of the PersistenceCapable
     * @param value The value for the identity (the Long, or Int, or ... etc).
     * @return Single field identity
     * @throws NucleusException if invalid input is received
     */
    public Object getNewSingleFieldIdentity(Class idType, Class pcType, Object value)
    {
        // TODO Use internal SingleFieldIdentity
        if (idType == null)
        {
            throw new NucleusException(LOCALISER.msg("029001", pcType)).setFatal();
        }
        if (pcType == null)
        {
            throw new NucleusException(LOCALISER.msg("029000", idType)).setFatal();
        }
        if (value == null)
        {
            throw new NucleusException(LOCALISER.msg("029003", idType, pcType)).setFatal();
        }
        if (!SingleFieldIdentity.class.isAssignableFrom(idType))
        {
            throw new NucleusException(LOCALISER.msg("029002", idType.getName(), pcType.getName())).setFatal();
        }

        SingleFieldIdentity id = null;
        Class keyType = null;
        if (idType == LongIdentity.class)
        {
            keyType = Long.class;
            if (!(value instanceof Long))
            {
                throw new NucleusException(LOCALISER.msg("029004", idType.getName(), 
                    pcType.getName(), value.getClass().getName(), "Long")).setFatal();
            }
        }
        else if (idType == IntIdentity.class)
        {
            keyType = Integer.class;
            if (!(value instanceof Integer))
            {
                throw new NucleusException(LOCALISER.msg("029004", idType.getName(), 
                    pcType.getName(), value.getClass().getName(), "Integer")).setFatal();
            }
        }
        else if (idType == StringIdentity.class)
        {
            keyType = String.class;
            if (!(value instanceof String))
            {
                throw new NucleusException(LOCALISER.msg("029004", idType.getName(), 
                    pcType.getName(), value.getClass().getName(), "String")).setFatal();
            }
        }
        else if (idType == ByteIdentity.class)
        {
            keyType = Byte.class;
            if (!(value instanceof Byte))
            {
                throw new NucleusException(LOCALISER.msg("029004", idType.getName(), 
                    pcType.getName(), value.getClass().getName(), "Byte")).setFatal();
            }
        }
        else if (idType == ShortIdentity.class)
        {
            keyType = Short.class;
            if (!(value instanceof Short))
            {
                throw new NucleusException(LOCALISER.msg("029004", idType.getName(), 
                    pcType.getName(), value.getClass().getName(), "Short")).setFatal();
            }
        }
        else if (idType == CharIdentity.class)
        {
            keyType = Character.class;
            if (!(value instanceof Character))
            {
                throw new NucleusException(LOCALISER.msg("029004", idType.getName(), 
                    pcType.getName(), value.getClass().getName(), "Character")).setFatal();
            }
        }
        else
        {
            // ObjectIdentity
            keyType = Object.class;
        }

        try
        {
            Class[] ctrArgs = new Class[] {Class.class, keyType};
            Constructor ctr = idType.getConstructor(ctrArgs);

            Object[] args = new Object[] {pcType, value};
            id = (SingleFieldIdentity)ctr.newInstance(args);
        }
        catch (Exception e)
        {
            NucleusLogger.PERSISTENCE.error("Error encountered while creating SingleFieldIdentity instance of type \"" + idType.getName() + "\"");
            NucleusLogger.PERSISTENCE.error(e);

            return null;
        }

        return id;
    }

    /**
     * Utility to create a new application identity when you know the metadata for the target class,
     * and the toString() output of the identity.
     * @param clr ClassLoader resolver
     * @param acmd MetaData for the target class
     * @param value String form of the key
     * @return The identity
     * @throws NucleusException if invalid input is received
     */
    public Object getNewApplicationIdentityObjectId(ClassLoaderResolver clr, AbstractClassMetaData acmd, 
            String value)
    {
        // TODO Use internal SingleFieldIdentity
        if (acmd.getIdentityType() != IdentityType.APPLICATION)
        {
            // TODO Localise this
            throw new NucleusException("This class (" + acmd.getFullClassName() + 
                ") doesn't use application-identity!");
        }

        Class targetClass = clr.classForName(acmd.getFullClassName());
        Class idType = clr.classForName(acmd.getObjectidClass());
        Object id = null;
        if (acmd.usesSingleFieldIdentityClass())
        {
            try
            {
                Class[] ctrArgs;
                if (ObjectIdentity.class.isAssignableFrom(idType))
                {
                    ctrArgs = new Class[] {Class.class, Object.class};
                }
                else
                {
                    ctrArgs = new Class[] {Class.class, String.class};
                }
                Constructor ctr = idType.getConstructor(ctrArgs);

                Object[] args = new Object[] {targetClass, value};
                id = ctr.newInstance(args);
            }
            catch (Exception e)
            {
                // TODO Localise this
                throw new NucleusException("Error encountered while creating SingleFieldIdentity instance with key \"" + value + "\"", e);
            }
        }
        else
        {
            if (Modifier.isAbstract(targetClass.getModifiers()) && acmd.getObjectidClass() != null) 
            {
                try
                {
                    Constructor c = clr.classForName(acmd.getObjectidClass()).getDeclaredConstructor(
                        new Class[] {java.lang.String.class});
                    id = c.newInstance(new Object[] {value});
                }
                catch (Exception e) 
                {
                    String msg = LOCALISER.msg("010030", acmd.getObjectidClass(), acmd.getFullClassName());
                    NucleusLogger.PERSISTENCE.error(msg);
                    NucleusLogger.PERSISTENCE.error(e);

                    throw new NucleusUserException(msg);
                }
            }
            else
            {
                clr.classForName(targetClass.getName(), true);
                id = NucleusJPAHelper.getJDOImplHelper().newObjectIdInstance(targetClass, value);
            }
        }

        return id;
    }

    /**
     * Method to create a new object identity for the passed object with the supplied MetaData.
     * Only applies to application-identity cases.
     * @param pc The persistable object
     * @param cmd Its metadata
     * @return The new identity object
     */
    public Object getNewApplicationIdentityObjectId(Object pc, AbstractClassMetaData cmd)
    {
        if (pc == null || cmd == null)
        {
            return null;
        }

        // TODO Change this to Persistable
        try
        {
            Object id = ((PersistenceCapable)pc).jdoNewObjectIdInstance();
            if (!cmd.usesSingleFieldIdentityClass())
            {
                ((PersistenceCapable)pc).jdoCopyKeyFieldsToObjectId(id);
            }
            return id;
        }
        catch (JDONullIdentityException nie)
        {
            return null;
        }
    }

    /**
     * Method to return a new object identity for the specified class, and key (possibly toString() output).
     * @param cls Persistable class
     * @param key form of the object id
     * @return The object identity
     */
    public Object getNewApplicationIdentityObjectId(Class cls, Object key)
    {
        // TODO Replace this with a non-JDO method
        return NucleusJPAHelper.getJDOImplHelper().newObjectIdInstance(cls, key);
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
        props.put(PropertyNames.PROPERTY_NONTX_ATOMIC, "false"); // JPA assumes non-atomic non-tx ops
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

    /* (non-Javadoc)
     * @see org.datanucleus.api.ApiAdapter#getCopyOfPersistableObject(java.lang.Object, org.datanucleus.store.ObjectProvider, int[])
     */
    public Object getCopyOfPersistableObject(Object obj, ObjectProvider op, int[] fieldNumbers)
    {
        PersistenceCapable pc = (PersistenceCapable)obj;
        PersistenceCapable copy = pc.jdoNewInstance((javax.jdo.spi.StateManager)op);
        copy.jdoCopyFields(pc, fieldNumbers);
        return copy;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.ApiAdapter#copyFieldsFromPersistableObject(java.lang.Object, int[], java.lang.Object)
     */
    public void copyFieldsFromPersistableObject(Object pc, int[] fieldNumbers, Object pc2)
    {
        ((PersistenceCapable)pc2).jdoCopyFields(pc, fieldNumbers);
    }

    public void copyPkFieldsToPersistableObjectFromId(Object pc, Object id, FieldManager fm)
    {
        ObjectIdFieldConsumer consumer = new AppIdObjectIdFieldConsumer(this, fm);
        ((PersistenceCapable)pc).jdoCopyKeyFieldsFromObjectId(consumer, id);
    }
}