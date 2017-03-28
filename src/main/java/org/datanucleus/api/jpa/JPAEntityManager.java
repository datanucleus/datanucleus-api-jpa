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
2006 Andy Jefferson - provded exception handling
2008 Andy Jefferson - change query interface to be independent of store.rdbms
2009 Andy Jefferson - add JPA2 methods
2013 Andy Jefferson - support persist of Collection/array of entities
    ...
**********************************************************************/
package org.datanucleus.api.jpa;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.SynchronizationType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchPlan;
import org.datanucleus.JTATransactionImpl;
import org.datanucleus.Configuration;
import org.datanucleus.DetachState;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.jpa.criteria.CriteriaBuilderImpl;
import org.datanucleus.api.jpa.criteria.CriteriaDeleteImpl;
import org.datanucleus.api.jpa.criteria.CriteriaQueryImpl;
import org.datanucleus.api.jpa.criteria.CriteriaUpdateImpl;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.QueryLanguage;
import org.datanucleus.metadata.QueryMetaData;
import org.datanucleus.metadata.QueryResultMetaData;
import org.datanucleus.metadata.StoredProcQueryMetaData;
import org.datanucleus.metadata.StoredProcQueryParameterMetaData;
import org.datanucleus.metadata.TransactionType;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.state.LockManager;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.NucleusConnection;
import org.datanucleus.store.query.AbstractStoredProcedureQuery;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * EntityManager implementation for JPA.
 */
public class JPAEntityManager implements EntityManager, AutoCloseable
{
    protected boolean closed = false;

    /** The underlying ExecutionContext managing the persistence. */
    protected ExecutionContext ec;

    /** Parent EntityManagerFactory. */
    protected JPAEntityManagerFactory emf;

    /** Current Transaction (when using ResourceLocal). Will be null if using JTA. */
    protected EntityTransaction tx;

    /** The Flush Mode. */
    protected FlushModeType flushMode = FlushModeType.AUTO;

    /** Type of Persistence Context */
    protected PersistenceContextType persistenceContextType;

    protected SynchronizationType syncType;

    /** Fetch Plan (extension). */
    protected JPAFetchPlan fetchPlan = null;

    /**
     * Constructor.
     * @param theEMF The parent EntityManagerFactory
     * @param nucleusCtx Nucleus Context
     * @param contextType The Persistence Context type
     * @param syncType The Synchronisation type
     */
    public JPAEntityManager(JPAEntityManagerFactory theEMF, PersistenceNucleusContext nucleusCtx, PersistenceContextType contextType, SynchronizationType syncType)
    {
        this.emf = theEMF;
        this.persistenceContextType = contextType;
        this.syncType = syncType;

        // Allocate our ExecutionContext
        Map<String, Object> options = null;
        if (this.syncType == SynchronizationType.UNSYNCHRONIZED)
        {
            // Default is to auto-join, but user requests unsynchronized so pass this requirement to our ExecutionContext
            options = new HashMap<>();
            options.put(ExecutionContext.OPTION_JTA_AUTOJOIN, "false");
        }
        ec = nucleusCtx.getExecutionContext(this, options);

        if (nucleusCtx.getConfiguration().getStringProperty(PropertyNames.PROPERTY_TRANSACTION_TYPE).equalsIgnoreCase(
            TransactionType.RESOURCE_LOCAL.toString()))
        {
            // Using ResourceLocal transaction so allocate a transaction
            tx = new JPAEntityTransaction(ec);
        }

        CallbackHandler beanValidator = nucleusCtx.getValidationHandler(ec);
        if (beanValidator != null)
        {
            ec.getCallbackHandler().setValidationListener(beanValidator);
        }

        fetchPlan = new JPAFetchPlan(ec.getFetchPlan());
    }

    /**
     * Clear the persistence context, causing all managed entities to become detached. 
     * Changes made to entities that have not been flushed to the database will not be persisted.
     */
    public void clear()
    {
        assertIsOpen();
        ec.detachAll();
        ec.clearDirty();
        ec.evictAllObjects();
    }

    public boolean isContainerManaged()
    {
        return emf.isContainerManaged();
    }

    /**
     * Determine whether the EntityManager is open.
     * @return true until the EntityManager has been closed.
     */
    public boolean isOpen()
    {
        return !closed;
    }

    public ExecutionContext getExecutionContext()
    {
        return ec;
    }

    /**
     * Close an (application-managed) EntityManager.
     * After the close method has been invoked, all methods on the EntityManager instance and any Query objects obtained
     * from it will throw the  IllegalStateException except for getTransaction and isOpen (which will return false).
     * If this method is called when the EntityManager is associated with an active transaction, the persistence context 
     * remains managed until the transaction completes.
     */
    public void close()
    {
        assertIsOpen();

        try
        {
            ec.close();
        }
        catch (NucleusException ne)
        {
            throw NucleusJPAHelper.getJPAExceptionForNucleusException(ne);
        }

        fetchPlan = null;
        ec = null;
        emf = null;

        closed = true;
    }

    /**
     * Return the entity manager factory for the entity manager.
     * @return EntityManagerFactory instance
     * @throws IllegalStateException if the entity manager has
     * been closed.
     */
    public EntityManagerFactory getEntityManagerFactory()
    {
        return emf;
    }

    /**
     * Acessor for the current FetchPlan
     * @return The FetchPlan
     */
    public JPAFetchPlan getFetchPlan()
    {
        return fetchPlan;
    }

    /**
     * Check if the instance belongs to the current persistence context.
     * @param entity The entity
     * @return Whether it is contained in the current context
     * @throws IllegalArgumentException if not an entity
     */
    public boolean contains(Object entity)
    {
        assertIsOpen();
        assertEntity(entity);
        if (ec.getApiAdapter().getExecutionContext(entity) != ec)
        {
            return false;
        }
        if (ec.getApiAdapter().isDeleted(entity))
        {
            return false;
        }
        if (ec.getApiAdapter().isDetached(entity))
        {
            return false;
        }
        return true;
    }

    /**
     * Method to find an object from its primary key.
     * @param entityClass The entity class
     * @param primaryKey The PK value
     * @return the found entity instance or null if the entity does not exist
     * @throws IllegalArgumentException if the first argument does not denote an entity type or the second argument is not a valid type for that entity's primary key
     */
    public Object find(Class entityClass, Object primaryKey)
    {
        return find(entityClass, primaryKey, null, null);
    }

    /**
     * Find by primary key, using the specified properties.
     * Search for an entity of the specified class and primary key.
     * If the entity instance is contained in the persistence context it is returned from there.
     * If a vendor-specific property or hint is not recognised, it is silently ignored.
     * @param entityClass Class of the entity required
     * @param primaryKey The PK value
     * @param properties standard and vendor-specific properties
     * @return the found entity instance or null if the entity does not exist
     * @throws IllegalArgumentException if the first argument does not denote an entity type or the 
     *     second argument is is not a valid type for that entity's primary key or is null
     */
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties)
    {
        return find(entityClass, primaryKey, null, properties);
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lock)
    {
        return find(entityClass, primaryKey, lock, null);
    }

    /**
     * Method to return the persistent object of the specified entity type with the provided PK.
     * @param entityClass Entity type
     * @param primaryKey PK. Can be an instanceof the PK type, or the key when using single-field
     * @param lock Any locking to apply
     * @param properties Any optional properties to control the operation
     */
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lock, Map<String, Object> properties)
    {
        assertIsOpen();
        assertLockModeValid(lock);
        assertEntity(entityClass);

        Object pc;
        String tmpEntityGraphName = null;
        try
        {
            boolean fetchGraphSpecified = false;
            if (properties != null) // TODO Should be for just this operation
            {
                if (properties.containsKey(JPAEntityGraph.FETCHGRAPH_PROPERTY))
                {
                    EntityGraph eg = (EntityGraph) properties.get(JPAEntityGraph.FETCHGRAPH_PROPERTY);
                    String egName = eg.getName();
                    if (eg.getName() == null)
                    {
                        tmpEntityGraphName = emf.getDefinedEntityGraphName();
                        emf.registerEntityGraph((JPAEntityGraph) eg, tmpEntityGraphName);
                        egName = tmpEntityGraphName;
                    }
                    ec.getFetchPlan().setGroup(egName);
                    fetchGraphSpecified = true;
                }
                if (properties.containsKey(JPAEntityGraph.LOADGRAPH_PROPERTY))
                {
                    EntityGraph eg = (EntityGraph) properties.get(JPAEntityGraph.LOADGRAPH_PROPERTY);
                    String egName = eg.getName();
                    if (eg.getName() == null)
                    {
                        tmpEntityGraphName = emf.getDefinedEntityGraphName();
                        emf.registerEntityGraph((JPAEntityGraph) eg, tmpEntityGraphName);
                        egName = tmpEntityGraphName;
                    }
                    ec.getFetchPlan().addGroup(egName);
                    fetchGraphSpecified = true;
                }
                ec.setProperties(properties);
            }

            AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(entityClass, ec.getClassLoaderResolver());
            if (cmd == null)
            {
                throwException(new EntityNotFoundException());
            }

            try
            {
                // Get the identity
                Object id = primaryKey;
                if (cmd.getIdentityType() == IdentityType.DATASTORE)
                {
                    if (!IdentityUtils.isDatastoreIdentity(id))
                    {
                        // Create an OID
                        id = ec.getNucleusContext().getIdentityManager().getDatastoreId(cmd.getFullClassName(), primaryKey);
                    }
                }
                else if (!primaryKey.getClass().getName().equals(cmd.getObjectidClass()))
                {
                    // primaryKey is just the key (when using single-field identity), so create a PK object
                    try
                    {
                        id = ec.newObjectId(entityClass, primaryKey);
                    }
                    catch (NucleusException ne)
                    {
                        throw new IllegalArgumentException(ne);
                    }
                }

                if (lock != null && lock != LockModeType.NONE)
                {
                    // Register the object for locking
                    ec.getLockManager().lock(id, getLockTypeForJPALockModeType(lock));
                }

                pc = ec.findObjectById(id, true);
                if (pc != null && fetchGraphSpecified)
                {
                    // Force loading of FetchPlan fields of primary object since entity graph specified
                    ObjectProvider op = ec.findObjectProvider(pc);
                    op.loadUnloadedFieldsInFetchPlan();
                }
            }
            catch (NucleusObjectNotFoundException ex)
            {
                // in JPA, if object not found return null
                return null;
            }

            if (ec.getApiAdapter().isTransactional(pc))
            {
                // transactional instances are not validated, so we check if a deleted instance has been flushed
                ObjectProvider sm = ec.findObjectProvider(pc);
                if (ec.getApiAdapter().isDeleted(pc))
                {
                    try
                    {
                        sm.locate();
                    }
                    catch (NucleusObjectNotFoundException ex)
                    {
                        // the instance has been flushed, and it was not found, so we return null
                        return null;
                    }
                }
            }
        }
        finally
        {
            if (tmpEntityGraphName != null)
            {
                emf.deregisterEntityGraph(tmpEntityGraphName);
            }
            ec.getFetchPlan().setGroup(FetchPlan.DEFAULT);
        }
        return (T)pc;
    }

    /**
     * Return the underlying provider object for the EntityManager, if available.
     * The result of this method is implementation specific.
     * @return The ExecutionContext
     */
    public Object getDelegate()
    {
        assertIsOpen();

        return ec;
    }

    /**
     * Return an object of the specified type to allow access to the provider-specific API.
     * If the provider's EntityManager implementation does not support the specified class, the PersistenceException is thrown.
     * @param cls the class of the object to be returned. This is normally either the underlying 
     * EntityManager implementation class or an interface that it implements.
     * @return an instance of the specified class
     * @throws PersistenceException if the provider does not support the call.
     */
    public <T> T unwrap(Class<T> cls)
    {
        if (ClassConstants.EXECUTION_CONTEXT.isAssignableFrom(cls))
        {
            return (T) ec;
        }
        if (ClassConstants.STORE_MANAGER.isAssignableFrom(cls))
        {
            return (T) ec.getStoreManager();
        }
        if (ClassConstants.METADATA_MANAGER.isAssignableFrom(cls))
        {
            return (T) ec.getMetaDataManager();
        }
        if (ClassConstants.NUCLEUS_CONTEXT.isAssignableFrom(cls))
        {
            return (T) ec.getNucleusContext();
        }
        if (NucleusConnection.class.isAssignableFrom(cls))
        {
            return (T)ec.getStoreManager().getNucleusConnection(ec);
        }
        if (java.sql.Connection.class.isAssignableFrom(cls))
        {
            NucleusConnection nconn = ec.getStoreManager().getNucleusConnection(ec);
            if (nconn instanceof java.sql.Connection)
            {
                return (T)nconn.getNativeConnection();
            }
        }

        return (T)throwException(new PersistenceException("We don't support accessing object of type " + cls.getName() + " using unwrap() method"));
    }

    /**
     * Get an instance, whose state may be lazily fetched. If the requested instance does not exist in the database, the EntityNotFoundException is
     * thrown when the instance state is first accessed. The persistence provider runtime is permitted to throw the EntityNotFoundException when
     * getReference is called. The application should not expect that the instance state will be available upon detachment, unless it was accessed
     * by the application while the entity manager was open.
     * @param entityClass Class of the entity
     * @param primaryKey The PK
     * @return the found entity instance
     * @throws IllegalArgumentException if the first argument does not denote an entity type or the second argument is not a valid type for that entities PK
     * @throws EntityNotFoundException if the entity state cannot be accessed
     */
    @SuppressWarnings("unchecked")
    public Object getReference(Class entityClass, Object primaryKey)
    {
        assertIsOpen();
        assertEntity(entityClass);

        Object id = null;
        try
        {
            id = ec.newObjectId(entityClass, primaryKey);
        }
        catch (NucleusException ne)
        {
            // Assumes that ec.newObjectId will throw an exception if we have an IntId case passed in but with a String key
            throw new IllegalArgumentException(ne);
        }

        try
        {
            return ec.findObjectById(id, false);
        }
        catch (NucleusObjectNotFoundException ne)
        {
            throw NucleusJPAHelper.getJPAExceptionForNucleusException(ne);
        }
    }

    /**
     * Set the lock mode for an entity object contained in the persistence context.
     * @param entity The Entity
     * @param lockMode Lock mode
     * @throws PersistenceException if an unsupported lock call is made
     * @throws IllegalArgumentException if the instance is not an entity or is a detached entity
     * @throws TransactionRequiredException if there is no transaction
     */
    public void lock(Object entity, LockModeType lockMode)
    {
        lock(entity, lockMode, null);
    }

    /**
     * Set the lock mode for an entity object contained in the persistence context.
     * @param entity The Entity
     * @param lock Lock mode
     * @param properties Optional properties controlling the operation
     * @throws PersistenceException if an unsupported lock call is made
     * @throws IllegalArgumentException if the instance is not an entity or is a detached entity
     * @throws TransactionRequiredException if there is no transaction
     */
    public void lock(Object entity, LockModeType lock, Map<String, Object> properties)
    {
        assertIsOpen();
        assertLockModeValid(lock);
        assertTransactionActive();
        assertEntity(entity);
        if (ec.getApiAdapter().isDetached(entity))
        {
            throw new IllegalArgumentException(Localiser.msg("EM.EntityIsDetached",
                StringUtils.toJVMIDString(entity), "" + ec.getApiAdapter().getIdForObject(entity)));
        }
        if (!contains(entity))
        {
            // The object is not contained (the javadoc doesnt explicitly say which exception to throw here)
            throwException(new PersistenceException("Entity is not contained in this persistence context so cant lock it"));
        }
        if (properties != null) // TODO Should be for just this operation
        {
            ec.setProperties(properties);
        }

        AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(entity.getClass(), ec.getClassLoaderResolver());
        if ((lock == LockModeType.OPTIMISTIC || lock == LockModeType.OPTIMISTIC_FORCE_INCREMENT) && !cmd.isVersioned())
        {
            throw new PersistenceException("Object of type " + entity.getClass().getName() + " is not versioned so cannot lock optimistically!");
        }

        if (lock != null && lock != LockModeType.NONE)
        {
            ec.getLockManager().lock(ec.findObjectProvider(entity), getLockTypeForJPALockModeType(lock));
        }
    }

    /**
     * Make an instance managed and persistent.
     * @param entity The Entity
     * @throws EntityExistsException if the entity already exists.
     *     (The EntityExistsException may be thrown when the persist operation is invoked, 
     *     or the EntityExistsException/PersistenceException may be thrown at flush/commit time.)
     * @throws IllegalArgumentException if not an entity
     * @throws TransactionRequiredException if invoked on a container-managed entity manager
     *     of type PersistenceContextType.TRANSACTION and there is no transaction.
     */
    public void persist(Object entity)
    {
        assertIsOpen();
        assertTransactionNotRequired();

        if (entity != null)
        {
            if (entity instanceof Collection)
            {
                // DN extension : persist of Collection of entities
                persist((Collection)entity);
                return;
            }
            else if (entity.getClass().isArray())
            {
                // DN extension : persist of array of entities
                persist((Object[])entity);
                return;
            }
        }

        assertEntity(entity);
        if (ec.exists(entity))
        {
            if (ec.getApiAdapter().isDetached(entity))
            {
                // The JPA spec is very confused about when this exception is thrown, however the JPA TCK invokes this operation multiple times over the same instance
                // Entity is already persistent. Maybe the ExecutionContext.exists method isnt the best way of checking
                throwException(new EntityExistsException(Localiser.msg("EM.EntityIsPersistent", StringUtils.toJVMIDString(entity))));
            }
        }

        try
        {
            ec.persistObject(entity, false);
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
    }

    public void persist(Collection entities)
    {
        persist(entities.toArray());
    }

    public void persist(Object... entities)
    {
        for (Object entity : entities)
        {
            assertEntity(entity);
            if (ec.exists(entity))
            {
                if (ec.getApiAdapter().isDetached(entity))
                {
                    // The JPA spec is very confused about when this exception is thrown, however the JPA TCK invokes this operation multiple times over the same instance
                    // Entity is already persistent. Maybe the ExecutionContext.exists method isnt the best way of checking
                    throwException(new EntityExistsException(Localiser.msg("EM.EntityIsPersistent", StringUtils.toJVMIDString(entity))));
                }
            }
        }

        try
        {
            ec.persistObjects(entities);
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
    }

    /**
     * Merge the state of the given entity into the current persistence context.
     * @param entity The Entity
     * @return the instance that the state was merged to
     * @throws IllegalArgumentException if instance is not an entity or is a removed entity
     * @throws TransactionRequiredException if invoked on a container-managed entity manager of type PersistenceContextType.TRANSACTION and there is no transaction.
     */
    public Object merge(Object entity)
    {
        assertIsOpen();
        assertTransactionNotRequired();
        if (entity != null)
        {
            if (entity instanceof Collection)
            {
                // DN extension : merge of Collection of entities
                return merge((Collection)entity);
            }
            else if (entity.getClass().isArray())
            {
                // DN extension : merge of array of entities
                return merge((Object[])entity);
            }
        }

        assertEntity(entity);
        if (ec.getApiAdapter().isDeleted(entity))
        {
            throw new IllegalArgumentException(Localiser.msg("EM.EntityIsDeleted", 
                StringUtils.toJVMIDString(entity), "" + ec.getApiAdapter().getIdForObject(entity)));
        }

        try
        {
            return ec.persistObject(entity, true);
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
        return null;
    }

    public Collection merge(Collection entities)
    {
        Object[] merged = merge(entities.toArray());
        return Arrays.asList(merged);
    }

    public Object[] merge(Object... entities)
    {
        for (Object entity : entities)
        {
            assertEntity(entity);
            if (ec.getApiAdapter().isDeleted(entity))
            {
                throw new IllegalArgumentException(Localiser.msg("EM.EntityIsDeleted", StringUtils.toJVMIDString(entity), "" + ec.getApiAdapter().getIdForObject(entity)));
            }
        }

        try
        {
            return ec.persistObjects(entities);
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
        return null;
    }

    /**
     * Remove the given entity from the persistence context, causing a managed entity to become 
     * detached. Unflushed changes made to the entity if any (including removal of the entity),
     * will not be synchronized to the database. Entities which previously referenced the detached 
     * entity will continue to reference it.
     * @param entity The entity
     * @throws IllegalArgumentException if the instance is not an entity
     */
    public void detach(Object entity)
    {
        assertIsOpen();

        if (entity != null)
        {
            if (entity instanceof Collection)
            {
                // DN extension : detach of Collection of entities
                detach((Collection)entity);
                return;
            }
            else if (entity.getClass().isArray())
            {
                // DN extension : detach of array of entities
                detach((Object[])entity);
                return;
            }
        }

        assertEntity(entity);

        try
        {
            ec.detachObject(entity, new DetachState(ec.getApiAdapter()));
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
    }

    public void detach(Collection entities)
    {
        detach(entities.toArray());
    }

    public void detach(Object... entities)
    {
        for (Object entity : entities)
        {
            assertEntity(entity);

            // TODO Change this when we have detachObjects in ExecutionContext
            try
            {
                ec.detachObject(entity, new DetachState(ec.getApiAdapter()));
            }
            catch (NucleusException ne)
            {
                throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
            }
        }
    }

    /**
     * Refresh the state of the instance from the database, overwriting changes made to the entity, if any.
     * @param entity The Entity
     * @throws IllegalArgumentException if not an entity or entity is not managed
     * @throws TransactionRequiredException if invoked on a container-managed entity manager
     *     of type PersistenceContextType.TRANSACTION and there is no transaction.
     * @throws EntityNotFoundException if the entity no longer exists in the database
     */
    public void refresh(Object entity)
    {
        refresh(entity, null, null);
    }

    /**
     * Refresh the state of the instance from the database, using the specified properties, 
     * and overwriting changes made to the entity, if any.
     * If a vendor-specific property or hint is not recognised, it is silently ignored.
     * @param entity The entity
     * @param properties standard and vendor-specific properties
     * @throws IllegalArgumentException if the instance is not an entity or the entity is not managed
     * @throws TransactionRequiredException if invoked on a container-managed entity manager 
     *     of type PersistenceContextType.TRANSACTION and there is no transaction.
     * @throws EntityNotFoundException if the entity no longer exists in the database
     */
    public void refresh(Object entity, Map<String, Object> properties)
    {
        refresh(entity, null, properties);
    }

    public void refresh(Object entity, LockModeType lock)
    {
        refresh(entity, lock, null);
    }

    public void refresh(Object entity, LockModeType lock, Map<String, Object> properties)
    {
        assertIsOpen();
        assertLockModeValid(lock);
        assertTransactionNotRequired();
        assertEntity(entity);
        if (ec.getApiAdapter().getExecutionContext(entity) != ec)
        {
            throw new IllegalArgumentException(Localiser.msg("EM.EntityIsNotManaged", StringUtils.toJVMIDString(entity)));
        }
        if (!ec.exists(entity))
        {
            throwException(new EntityNotFoundException(Localiser.msg("EM.EntityNotInDatastore", StringUtils.toJVMIDString(entity))));
        }
        if (properties != null) // TODO Should be for just this operation
        {
            ec.setProperties(properties);
        }

        try
        {
            if (lock != null && lock != LockModeType.NONE)
            {
                // Register the object for locking
                ec.getLockManager().lock(ec.getApiAdapter().getIdForObject(entity), getLockTypeForJPALockModeType(lock));
            }

            ec.refreshObject(entity);
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
    }

    /**
     * Remove the entity instance.
     * @param entity The Entity
     * @throws IllegalArgumentException if not an entity or if a detached entity
     * @throws TransactionRequiredException if invoked on a container-managed entity manager
     *     of type PersistenceContextType.TRANSACTION and there is no transaction.
     */
    public void remove(Object entity)
    {
        assertIsOpen();
        assertTransactionNotRequired();
        if (entity != null)
        {
            if (entity instanceof Collection)
            {
                // DN extension : remove of collection of entities
                remove((Collection)entity);
                return;
            }
            else if (entity.getClass().isArray())
            {
                // DN extension : remove of array of entities
                remove((Object[])entity);
                return;
            }
        }

        assertEntity(entity);

        // What if the object doesnt exist in the datastore ? IllegalArgumentException. Spec says nothing
        if (ec.getApiAdapter().isDetached(entity))
        {
            throw new IllegalArgumentException(Localiser.msg("EM.EntityIsDetached", StringUtils.toJVMIDString(entity), "" + ec.getApiAdapter().getIdForObject(entity)));
        }

        try
        {
            ec.deleteObject(entity);
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
    }

    public void remove(Collection entities)
    {
        remove(entities.toArray());
    }

    public void remove(Object... entities)
    {
        for (Object entity : entities)
        {
            assertEntity(entity);

            // What if the object doesn't exist in the datastore ? IllegalArgumentException. Spec says nothing
            if (ec.getApiAdapter().isDetached(entity))
            {
                throw new IllegalArgumentException(Localiser.msg("EM.EntityIsDetached", StringUtils.toJVMIDString(entity), "" + ec.getApiAdapter().getIdForObject(entity)));
            }
        }

        try
        {
            ec.deleteObjects(entities);
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
    }

    /**
     * Synchronize the persistence context to the underlying database.
     * @throws TransactionRequiredException if there is no transaction
     * @throws PersistenceException if the flush fails
     */
    public void flush()
    {
        assertIsOpen();
        assertTransactionActive();

        try
        {
            ec.flush();
        }
        catch (NucleusException ne)
        {
            throwException(NucleusJPAHelper.getJPAExceptionForNucleusException(ne));
        }
    }

    /**
     * Get the flush mode that applies to all objects contained in the persistence context.
     * @return flushMode
     */
    public FlushModeType getFlushMode()
    {
        assertIsOpen();
        return flushMode;
    }

    /**
     * Set the flush mode that applies to all objects contained in the persistence context.
     * @param flushMode Mode of flush
     */
    public void setFlushMode(FlushModeType flushMode)
    {
        assertIsOpen();
        this.flushMode = flushMode;
    }

    /**
     * Get the current lock mode for the entity instance.
     * @param entity The entity in question
     * @return lock mode
     * @throws TransactionRequiredException if there is no transaction
     * @throws IllegalArgumentException if the instance is not a managed entity and a transaction is active
     */
    public LockModeType getLockMode(Object entity)
    {
        assertTransactionActive();
        assertEntity(entity);
        if (ec.getApiAdapter().getExecutionContext(entity) != ec)
        {
            throw new IllegalArgumentException(Localiser.msg("EM.EntityIsNotManaged", StringUtils.toJVMIDString(entity)));
        }
        if (ec.getApiAdapter().isDetached(entity))
        {
            throw new IllegalArgumentException(Localiser.msg("EM.EntityIsNotManaged", StringUtils.toJVMIDString(entity)));
        }

        ObjectProvider sm = ec.findObjectProvider(entity);
        return getJPALockModeTypeForLockType(sm.getLockMode());
    }

    // ------------------------------------ Transactions --------------------------------------

    /**
     * Return the resource-level transaction object.
     * The EntityTransaction instance may be used serially to begin and commit multiple transactions.
     * @return EntityTransaction instance
     * @throws IllegalStateException if invoked on a JTA EntityManager.
     */
    public EntityTransaction getTransaction()
    {
        if (tx == null)
        {
            throw new IllegalStateException(Localiser.msg("EM.TransactionNotLocal"));
        }

        return tx;
    }

    /**
     * Indicate to the EntityManager that a JTA transaction is active so join to it.
     * This method should be called on a JTA application managed EntityManager that was created 
     * outside the scope of the active transaction to associate it with the current JTA transaction.
     * @throws TransactionRequiredException if there is no transaction.
     */
    public void joinTransaction()
    {
        assertIsOpen();
        if (tx != null)
        {
            // Not in the JPA spec but why would anyone call this with local txns?
            throw new IllegalStateException(Localiser.msg("EM.TransactionLocal"));
        }

        // Extract the ExecutionContext transaction (JTATransactionImpl) and force the lookup+join
        JTATransactionImpl ecTx = (JTATransactionImpl) ec.getTransaction();

        ecTx.joinTransaction(); // Enforce the join
        boolean active = ecTx.getIsActive();
        if (!active)
        {
            // No UserTransaction present to join to
            throw new TransactionRequiredException("Attempt to joinTransaction but UserTransaction is not present");
        }
    }

    /**
     * Return whether this EntityManager is joined to the current transaction.
     * If we are using local transactions then returns whether the txn is active.
     */
    public boolean isJoinedToTransaction()
    {
        if (tx != null)
        {
            // Not explicitly mentioned in JPA spec what to do for local txn
            return tx.isActive();
        }

        JTATransactionImpl ecTx = (JTATransactionImpl) ec.getTransaction();
        return ecTx.isJoined();
    }

    // ------------------------------------ Query Methods --------------------------------------

    /**
     * Method to return a query for the specified Criteria Query.
     * @param criteriaQuery The Criteria query
     * @return The JPA query to use
     */
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery)
    {
        CriteriaQueryImpl<T> criteria = (CriteriaQueryImpl<T>)criteriaQuery;
        String jpqlString = criteria.toString();
        TypedQuery<T> query = null;
        QueryCompilation compilation = criteria.getCompilation(ec.getMetaDataManager(), ec.getClassLoaderResolver());
        if (criteria.getResultType() != null && criteria.getResultType() != compilation.getCandidateClass())
        {
            query = createQuery(jpqlString, criteria.getResultType());
        }
        else
        {
            query = createQuery(jpqlString);
        }
        org.datanucleus.store.query.Query internalQuery = ((JPAQuery)query).getInternalQuery();
        if (compilation.getExprResult() == null)
        {
            // If the result was "Object(e)" or "e" then this is meaningless so remove
            internalQuery.setResult(null);
        }
        internalQuery.setCompilation(compilation);

        return query;
    }

    public Query createQuery(CriteriaUpdate crit)
    {
        CriteriaUpdateImpl criteria = (CriteriaUpdateImpl)crit;
        String jpqlString = criteria.toString();
        QueryCompilation compilation = criteria.getCompilation(ec.getMetaDataManager(), ec.getClassLoaderResolver());
        TypedQuery query = createQuery(jpqlString);
        org.datanucleus.store.query.Query internalQuery = ((JPAQuery)query).getInternalQuery();
        internalQuery.setCompilation(compilation);
        return query;
    }

    public Query createQuery(CriteriaDelete crit)
    {
        CriteriaDeleteImpl criteria = (CriteriaDeleteImpl)crit;
        String jpqlString = criteria.toString();
        QueryCompilation compilation = criteria.getCompilation(ec.getMetaDataManager(), ec.getClassLoaderResolver());
        TypedQuery query = createQuery(jpqlString);
        org.datanucleus.store.query.Query internalQuery = ((JPAQuery)query).getInternalQuery();
        internalQuery.setCompilation(compilation);
        return query;
    }

    /**
     * Return an instance of QueryBuilder for the creation of Criteria API QueryDefinition objects.
     * @return QueryBuilder instance
     * @throws IllegalStateException if the entity manager has been closed.
     */
    public CriteriaBuilder getCriteriaBuilder()
    {
        assertIsOpen();

        return new CriteriaBuilderImpl(emf);
    }

    /**
     * Create an instance of Query for executing a named query (in JPQL or native).
     * @param queryName the name of a query defined in metadata
     * @param resultClass Result class for this query
     * @return the new query instance
     * @throws IllegalArgumentException if a query has not been defined with the given name
     */
    public <T> TypedQuery<T> createNamedQuery(String queryName, Class<T> resultClass)
    {
        return createNamedQuery(queryName).setResultClass(resultClass);
    }

    /**
     * Create an instance of Query for executing a named query (in JPQL or native).
     * @param queryName the name of a query defined in metadata
     * @return the new query instance
     * @throws IllegalArgumentException if a query has not been defined with the given name
     */
    public JPAQuery createNamedQuery(String queryName)
    {
        assertIsOpen();

        if (queryName == null)
        {
            throw new IllegalArgumentException(Localiser.msg("Query.NamedQueryNotFound", queryName));
        }

        // Find the Query for the specified class
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        QueryMetaData qmd = ec.getMetaDataManager().getMetaDataForQuery(null, clr, queryName);
        if (qmd == null)
        {
            throw new IllegalArgumentException(Localiser.msg("Query.NamedQueryNotFound", queryName));
        }

        // Create the Query
        try
        {
            if (!ec.getStoreManager().supportsQueryLanguage(qmd.getLanguage()))
            {
                throw new IllegalArgumentException(Localiser.msg("Query.LanguageNotSupportedByStore", qmd.getLanguage()));
            }

            if (qmd.getLanguage().equals(QueryLanguage.JPQL.toString()))
            {
                // "named-query" so return JPQL
                org.datanucleus.store.query.Query internalQuery = ec.getStoreManager().getQueryManager().newQuery(qmd.getLanguage(), ec, qmd.getQuery());
                return new JPAQuery(this, internalQuery, qmd.getLanguage());
            }

            // "named-native-query" so return native query
            org.datanucleus.store.query.Query internalQuery = ec.getStoreManager().getQueryManager().newQuery(qmd.getLanguage(), ec, qmd.getQuery());
            if (qmd.getResultClass() != null)
            {
                // Named native query with result class
                String resultClassName = qmd.getResultClass();
                Class resultClass = null;
                try
                {
                    resultClass = ec.getClassLoaderResolver().classForName(resultClassName);
                    internalQuery.setResultClass(resultClass);
                    return new JPAQuery(this, internalQuery, qmd.getLanguage());
                }
                catch (Exception e)
                {
                    // Result class not found so throw exception (not defined in the JPA spec)
                    throw new IllegalArgumentException(Localiser.msg("Query.ResultClassNotFound", qmd.getName(), resultClassName));
                }
            }
            else if (qmd.getResultMetaDataName() != null)
            {
                QueryResultMetaData qrmd = ec.getMetaDataManager().getMetaDataForQueryResult(qmd.getResultMetaDataName());
                if (qrmd == null)
                {
                    throw new IllegalArgumentException(Localiser.msg("Query.ResultSetMappingNotFound", qmd.getResultMetaDataName()));
                }
                internalQuery.setResultMetaData(qrmd);
                return new JPAQuery(this, internalQuery, qmd.getLanguage());
            }
            else
            {
                return new JPAQuery(this, internalQuery, qmd.getLanguage());
            }
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(ne.getMessage(), ne);
        }
    }

    /**
     * Create an instance of Query for executing a native query statement.
     * @param queryString a native query string
     * @return the new query instance
     */
    public Query createNativeQuery(String queryString)
    {
        return createNativeQuery(queryString, (Class)null);
    }

    /**
     * Create an instance of Query for executing a "native" query.
     * The native query language could be SQL (for RDBMS), or CQL (for Cassandra), or some other depending on the store.
     * @param queryString a native query string
     * @param resultClass the class of the resulting instance(s)
     * @return the new query instance
     */
    public Query createNativeQuery(String queryString, Class resultClass)
    {
        assertIsOpen();
        try
        {
            String nativeQueryLanguage = ec.getStoreManager().getNativeQueryLanguage();
            if (nativeQueryLanguage == null)
            {
                throw new IllegalArgumentException("This datastore does not support 'native' queries");
            }

            org.datanucleus.store.query.Query internalQuery = ec.getStoreManager().getQueryManager().newQuery(nativeQueryLanguage, ec, queryString);
            if (resultClass != null)
            {
                internalQuery.setResultClass(resultClass);
            }
            return new JPAQuery(this, internalQuery, nativeQueryLanguage);
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(ne.getMessage(), ne);
        }
    }

    /**
     * Create an instance of Query for executing a native query.
     * The native query language could be SQL (for RDBMS), or CQL (for Cassandra), or some other depending on the store.
     * @param queryString a native query string
     * @param resultSetMapping the name of the result set mapping
     * @return the new query instance
     */
    public Query createNativeQuery(String queryString, String resultSetMapping)
    {
        assertIsOpen();
        try
        {
            String nativeQueryLanguage = ec.getStoreManager().getNativeQueryLanguage();
            if (nativeQueryLanguage == null)
            {
                throw new IllegalArgumentException("This datastore does not support 'native' queries");
            }

            org.datanucleus.store.query.Query internalQuery = ec.getStoreManager().getQueryManager().newQuery(nativeQueryLanguage, ec, queryString);
            QueryResultMetaData qrmd = ec.getMetaDataManager().getMetaDataForQueryResult(resultSetMapping);
            if (qrmd == null)
            {
                throw new IllegalArgumentException(Localiser.msg("Query.ResultSetMappingNotFound", resultSetMapping));
            }
            internalQuery.setResultMetaData(qrmd);
            return new JPAQuery(this, internalQuery, nativeQueryLanguage);
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(ne.getMessage(), ne);
        }
    }

    /**
     * Create an instance of Query for executing a stored procedure.
     * @param procName the name of the stored procedure defined in metadata
     * @return the new query instance
     * @throws IllegalArgumentException if a stored procedure has not been defined with the given name
     */
    public StoredProcedureQuery createNamedStoredProcedureQuery(String procName)
    {
        if (procName == null)
        {
            throw new IllegalArgumentException(Localiser.msg("Query.NamedStoredProcedureQueryNotFound", procName));
        }

        // Find the Query for the specified stored procedure "name"
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        StoredProcQueryMetaData qmd = ec.getMetaDataManager().getMetaDataForStoredProcQuery(null, clr, procName);
        if (qmd == null)
        {
            throw new IllegalArgumentException(Localiser.msg("Query.NamedStoredProcedureQueryNotFound", procName));
        }

        // Create the Stored Procedure query
        try
        {
            org.datanucleus.store.query.AbstractStoredProcedureQuery internalQuery =
                (AbstractStoredProcedureQuery) ec.getStoreManager().getQueryManager().newQuery(QueryLanguage.STOREDPROC.toString(), ec, qmd.getProcedureName());

            if (qmd.getParameters() != null)
            {
                for (StoredProcQueryParameterMetaData parammd : qmd.getParameters())
                {
                    Class type = clr.classForName(parammd.getType());
                    internalQuery.registerParameter(parammd.getName(), type, parammd.getMode());
                }
            }
            if (qmd.getResultClasses() != null)
            {
                Class[] resultClasses = new Class[qmd.getResultClasses().size()];
                int i=0;
                for (String clsName : qmd.getResultClasses())
                {
                    resultClasses[i++] = clr.classForName(clsName);
                }
                internalQuery.setResultClasses(resultClasses);
            }
            else if (qmd.getResultSetMappings() != null)
            {
                QueryResultMetaData[] qrmds = new QueryResultMetaData[qmd.getResultSetMappings().size()];
                int i=0;
                for (String resultSetMappingName : qmd.getResultSetMappings())
                {
                    qrmds[i] = ec.getMetaDataManager().getMetaDataForQueryResult(resultSetMappingName);
                    if (qrmds[i] == null)
                    {
                        throw new IllegalArgumentException(Localiser.msg("Query.ResultSetMappingNotFound", resultSetMappingName));
                    }
                    i++;
                }
                internalQuery.setResultMetaData(qrmds);
            }

            return new JPAStoredProcedureQuery(this, internalQuery);
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(ne.getMessage(), ne);
        }
    }

    /**
     * Create an instance of Query for executing a stored procedure.
     * @param procName Name of stored procedure defined in metadata
     * @return the new stored procedure query instance
     */
    public StoredProcedureQuery createStoredProcedureQuery(String procName)
    {
        assertIsOpen();
        try
        {
            org.datanucleus.store.query.Query internalQuery = ec.getStoreManager().getQueryManager().newQuery(
                QueryLanguage.STOREDPROC.toString(), ec, procName);
            return new JPAStoredProcedureQuery(this, internalQuery);
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(ne.getMessage(), ne);
        }
    }

    /**
     * Create an instance of StoredProcedureQuery for executing a stored procedure in the database.
     * Parameters must be registered before the stored procedure can be executed.
     * The resultClass arguments must be specified in the order in which the result sets will be returned by the stored procedure invocation.
     * @param procedureName name of the stored procedure in the database
     * @param resultClasses classes to which the result sets produced by the stored procedure are to be mapped
     * @return the new stored procedure query instance
     * @throws IllegalArgumentException if a stored procedure of the given name does not exist or the query execution will fail
     */
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses)
    {
        assertIsOpen();
        try
        {
            org.datanucleus.store.query.Query internalQuery = ec.getStoreManager().getQueryManager().newQuery(QueryLanguage.STOREDPROC.toString(), ec, procedureName);
            if (resultClasses != null && resultClasses.length > 0)
            {
                ((AbstractStoredProcedureQuery)internalQuery).setResultClasses(resultClasses);
            }
            return new JPAStoredProcedureQuery(this, internalQuery);
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(ne.getMessage(), ne);
        }
    }

    /**
     * Create an instance of StoredProcedureQuery for executing a stored procedure in the database.
     * Parameters must be registered before the stored procedure can be executed.
     * The resultSetMappings argument must be specified in the order in which the result sets will be returned by the stored procedure invocation.
     * @param procedureName name of the stored procedure in the database
     * @param resultSetMappings the names of the result set mappings to be used in mapping result sets returned by the stored procedure
     * @return the new stored procedure query instance
     * @throws IllegalArgumentException if a stored procedure or result set mapping of the given name does not exist
     * or the query execution will fail
     */
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings)
    {
        assertIsOpen();
        try
        {
            org.datanucleus.store.query.Query internalQuery = ec.getStoreManager().getQueryManager().newQuery(QueryLanguage.STOREDPROC.toString(), ec, procedureName);
            if (resultSetMappings != null && resultSetMappings.length > 0)
            {
                QueryResultMetaData[] qrmds = new QueryResultMetaData[resultSetMappings.length];
                for (int i=0;i<qrmds.length;i++)
                {
                    qrmds[i] = ec.getMetaDataManager().getMetaDataForQueryResult(resultSetMappings[i]);
                    if (qrmds[i] == null)
                    {
                        throw new IllegalArgumentException(Localiser.msg("Query.ResultSetMappingNotFound", resultSetMappings[i]));
                    }
                }
                ((AbstractStoredProcedureQuery)internalQuery).setResultMetaData(qrmds);
            }
            return new JPAStoredProcedureQuery(this, internalQuery);
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(ne.getMessage(), ne);
        }
    }

    /* (non-Javadoc)
     * @see javax.persistence.EntityManager#createQuery(java.lang.String, java.lang.Class)
     */
    public <T> TypedQuery<T> createQuery(String queryString, Class<T> resultClass)
    {
        JPAQuery jpaQuery = createQuery(queryString);
        if (resultClass != null)
        {
            // Catch special case of javax.persistence.Tuple
            if (resultClass.getName().equals("javax.persistence.Tuple"))
            {
                jpaQuery.setResultClass(JPAQueryTuple.class);
            }
            else
            {
                jpaQuery.setResultClass(resultClass);
            }
        }
        return jpaQuery;
    }

    /**
     * Create an instance of Query for executing a JPQL statement.
     * @param queryString a Java Persistence query string
     * @return the new query instance
     * @throws IllegalArgumentException if query string is not valid
     */
    public JPAQuery createQuery(String queryString)
    {
        assertIsOpen();
        try
        {
            org.datanucleus.store.query.Query internalQuery = ec.getStoreManager().getQueryManager().newQuery(
                QueryLanguage.JPQL.toString(), ec, queryString);
            return new JPAQuery(this, internalQuery, QueryLanguage.JPQL.toString());
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(ne.getMessage(), ne);
        }
    }

    /**
     * Set an entity manager property.
     * If a vendor-specific property is not recognized, it is silently ignored.
     * @param propertyName Name of the property
     * @param value The value
     * @throws IllegalArgumentException if the second argument is not valid for the implementation
     */
    public void setProperty(String propertyName, Object value)
    {
        try
        {
            ec.setProperty(propertyName, value);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Property '" + propertyName + "' value=" + value + " invalid");
        }
    }

    /**
     * Get the properties and associated values that are in effect for the entity manager. 
     * Changing the contents of the map does not change the configuration in effect.
     */
    public Map<String,Object> getProperties()
    {
        return ec.getProperties();
    }

    /**
     * Get the names of the properties that are supported for use with the entity manager.
     * These correspond to properties and hints that may be passed to the methods of the EntityManager 
     * interface that take a properties argument or used with the PersistenceContext annotation. 
     * These properties include all standard entity manager hints and properties as well as 
     * vendor-specific one supported by the provider. These properties may or may not currently be in effect.
     * @return property names Names of the properties accepted
     */
    public Set<String> getSupportedProperties()
    {
        return ec.getSupportedProperties();
    }

    /**
     * Return an instance of Metamodel interface for access to the metamodel of the persistence unit.
     * @return Metamodel instance
     * @throws IllegalStateException if the entity manager has been closed.
     */
    public Metamodel getMetamodel()
    {
        return emf.getMetamodel();
    }

    // ------------------------------------------ Assertions --------------------------------------------

    /**
     * Assert if the EntityManager is closed.
     * @throws IllegalStateException When the EntityManaged is closed
     */
    private void assertIsOpen()
    {
        if (closed)
        {
            throw new IllegalStateException(Localiser.msg("EM.IsClosed"));
        }
    }

    /**
     * Assert if the transaction is not active.
     * @throws TransactionRequiredException if we dont have an active transaction
     */
    private void assertTransactionActive()
    {
        if (!isTransactionActive())
        {
            throw new TransactionRequiredException(Localiser.msg("EM.TransactionRequired"));
        }
    }

    public boolean isTransactionActive()
    {
        if (tx != null)
        {
            return tx.isActive();
        }
        JTATransactionImpl jtaTxn = (JTATransactionImpl) ec.getTransaction();
        if (jtaTxn.isJoined())
        {
            return true;
        }

        // Check the JTA tx, which will try to join if appropriate
        return jtaTxn.isActive();
    }

    /**
     * Method to throw a TransactionRequiredException if the provided lock mode is not valid
     * for the current transaction situation (i.e if lock mode other than NONE is specified then
     * the transaction has to be active).
     * @param lock The lock mode
     */
    private void assertLockModeValid(LockModeType lock)
    {
        if (lock != null && lock != LockModeType.NONE && !isTransactionActive())
        {
            throw new TransactionRequiredException(Localiser.msg("EM.TransactionRequired"));
        }
    }

    /**
     * Assert if the passed entity is not persistable, or has no persistence information.
     * @param entity The entity (or class of the entity)
     * @throws IllegalArgumentException Thrown if not an entity
     */
    private void assertEntity(Object entity)
    {
        if (entity == null)
        {
            throw new IllegalArgumentException(Localiser.msg("EM.EntityNotAnEntity", entity));
        }

        Class cls = null;
        if (entity instanceof Class)
        {
            // Class passed in so just check that
            cls = (Class)entity;
        }
        else
        {
            // Object passed in so check its class
            cls = entity.getClass();
        }

        try
        {
            ec.assertClassPersistable(cls);
        }
        catch (NucleusException ne)
        {
            throw new IllegalArgumentException(Localiser.msg("EM.EntityNotAnEntity", cls.getName()), ne);
        }
    }

    /**
     * Assert if container managed, persistence context is TRANSACTION, and the transaction is not active.
     * @throws TransactionRequiredException thrown if the context requires a txn but isnt active
     */
    private void assertTransactionNotRequired()
    {
        if (isContainerManaged() && persistenceContextType == PersistenceContextType.TRANSACTION && !isTransactionActive())
        {
            throw new TransactionRequiredException(Localiser.msg("EM.TransactionRequired"));
        }
    }

    /**
     * Convenience method to throw the supplied exception.
     * If the supplied exception is a PersistenceException then also marks the current transaction for rollback.
     * @param re The exception
     */
    private Object throwException(RuntimeException re)
    {
        if (re instanceof PersistenceException)
        {
            Configuration conf = ec.getNucleusContext().getConfiguration();
            if (tx != null && tx.isActive())
            {
                if (conf.getBooleanProperty(JPAPropertyNames.PROPERTY_JPA_TRANSACTION_ROLLBACK_ON_EXCEPTION))
                {
                    // The JPA spec says that all PersistenceExceptions thrown should mark the transaction for 
                    // rollback. Seems stupid to me. e.g you try to find an object with a particular id and it 
                    // doesn't exist so you then have to rollback the txn and start again. FFS.
                    getTransaction().setRollbackOnly();
                }
            }
        }
        throw re;
    }

    /**
     * Convenience method to convert from the JPA LockModeType to the type expected by LockManager
     * @param lock JPA LockModeType
     * @return The lock type
     */
    public static short getLockTypeForJPALockModeType(LockModeType lock)
    {
        short lockModeType = LockManager.LOCK_MODE_NONE;
        if (lock == LockModeType.OPTIMISTIC || lock == LockModeType.READ)
        {
            lockModeType = LockManager.LOCK_MODE_OPTIMISTIC_READ;
        }
        else if (lock == LockModeType.OPTIMISTIC_FORCE_INCREMENT || lock == LockModeType.WRITE)
        {
            lockModeType = LockManager.LOCK_MODE_OPTIMISTIC_WRITE;
        }
        else if (lock == LockModeType.PESSIMISTIC_READ)
        {
            lockModeType = LockManager.LOCK_MODE_PESSIMISTIC_READ;
        }
        else if (lock == LockModeType.PESSIMISTIC_FORCE_INCREMENT || lock == LockModeType.PESSIMISTIC_WRITE)
        {
            lockModeType = LockManager.LOCK_MODE_PESSIMISTIC_WRITE;
        }
        return lockModeType;
    }

    /**
     * Convenience method to convert from LockManager lock type to JPA LockModeType
     * @param lockType Lock type
     * @return JPA LockModeType
     */
    public static LockModeType getJPALockModeTypeForLockType(short lockType)
    {
        if (lockType == LockManager.LOCK_MODE_OPTIMISTIC_READ)
        {
            return LockModeType.OPTIMISTIC;
        }
        else if (lockType == LockManager.LOCK_MODE_OPTIMISTIC_WRITE)
        {
            return LockModeType.OPTIMISTIC_FORCE_INCREMENT;
        }
        else if (lockType == LockManager.LOCK_MODE_PESSIMISTIC_READ)
        {
            return LockModeType.PESSIMISTIC_READ;
        }
        else if (lockType == LockManager.LOCK_MODE_PESSIMISTIC_WRITE)
        {
            return LockModeType.PESSIMISTIC_WRITE;
        }
        return LockModeType.NONE;
    }

    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType)
    {
        // Return an unnamed EntityGraph for the user to play with (and then use with EM.find or Query)
        return new JPAEntityGraph<T>(ec.getMetaDataManager(), null, rootType);
    }

    public javax.persistence.EntityGraph<?> createEntityGraph(String graphName)
    {
        EntityGraph<?> entityGraph = emf.getNamedEntityGraph(graphName);
        if (entityGraph != null)
        {
            // TODO Do we need to re-register this with the EMF? or does the user?
            return ((JPAEntityGraph)entityGraph).cloneMutableEntityGraph();
        }
        return null;
    }

    public EntityGraph<?> getEntityGraph(String graphName)
    {
        EntityGraph<?> entityGraph = emf.getNamedEntityGraph(graphName);
        if (entityGraph == null)
        {
            throw new IllegalArgumentException("There is no registered EntityGraph of name \"" + graphName + "\"");
        }
        return entityGraph;
    }

    public <T> List<javax.persistence.EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass)
    {
        final EntityType<T> entityType = getMetamodel().entity(entityClass);
        if (entityType == null)
        {
            throw new IllegalArgumentException("Provided class (\"" + entityClass.getName() + "\") is not an entity");
        }
        return emf.getEntityGraphsByType(entityClass);
    }
}