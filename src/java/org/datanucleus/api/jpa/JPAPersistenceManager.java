/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.jdo.Extent;
import javax.jdo.FetchGroup;
import javax.jdo.FetchPlan;
import javax.jdo.JDOException;
import javax.jdo.ObjectState;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.datastore.JDOConnection;
import javax.jdo.datastore.Sequence;
import javax.jdo.listener.InstanceLifecycleListener;
import javax.persistence.EntityManager;

import org.datanucleus.ExecutionContext;

/**
 * Dummy PersistenceManager for use with JPA, so that we can use JDO enhancement contract which makes reference
 * to the PersistenceManager.
 */
public class JPAPersistenceManager implements PersistenceManager
{
    JPAEntityManager em;

    public JPAPersistenceManager(JPAEntityManager em)
    {
        this.em = em;
    }

    public EntityManager getEntityManager()
    {
        return em;
    }

    public ExecutionContext getExecutionContext()
    {
        return em.getExecutionContext();
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#addInstanceLifecycleListener(javax.jdo.listener.InstanceLifecycleListener, java.lang.Class[])
     */
    public void addInstanceLifecycleListener(InstanceLifecycleListener arg0, Class... arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#checkConsistency()
     */
    public void checkConsistency()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#close()
     */
    public void close()
    {
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#currentTransaction()
     */
    public Transaction currentTransaction()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#deletePersistent(java.lang.Object)
     */
    public void deletePersistent(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#deletePersistentAll(java.lang.Object[])
     */
    public void deletePersistentAll(Object... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#deletePersistentAll(java.util.Collection)
     */
    public void deletePersistentAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#detachCopy(java.lang.Object)
     */
    public <T> T detachCopy(T arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#detachCopyAll(java.util.Collection)
     */
    public <T> Collection<T> detachCopyAll(Collection<T> arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#detachCopyAll(T[])
     */
    public <T> T[] detachCopyAll(T... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#evict(java.lang.Object)
     */
    public void evict(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#evictAll()
     */
    public void evictAll()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#evictAll(java.lang.Object[])
     */
    public void evictAll(Object... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#evictAll(java.util.Collection)
     */
    public void evictAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#evictAll(boolean, java.lang.Class)
     */
    public void evictAll(boolean arg0, Class arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#flush()
     */
    public void flush()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getCopyOnAttach()
     */
    public boolean getCopyOnAttach()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getDataStoreConnection()
     */
    public JDOConnection getDataStoreConnection()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getDatastoreReadTimeoutMillis()
     */
    public Integer getDatastoreReadTimeoutMillis()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getDatastoreWriteTimeoutMillis()
     */
    public Integer getDatastoreWriteTimeoutMillis()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getDetachAllOnCommit()
     */
    public boolean getDetachAllOnCommit()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getExtent(java.lang.Class)
     */
    public <T> Extent<T> getExtent(Class<T> arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getExtent(java.lang.Class, boolean)
     */
    public <T> Extent<T> getExtent(Class<T> arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getFetchGroup(java.lang.Class, java.lang.String)
     */
    public FetchGroup getFetchGroup(Class arg0, String arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getFetchPlan()
     */
    public FetchPlan getFetchPlan()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getIgnoreCache()
     */
    public boolean getIgnoreCache()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getManagedObjects()
     */
    public Set getManagedObjects()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getManagedObjects(java.util.EnumSet)
     */
    public Set getManagedObjects(EnumSet<ObjectState> arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getManagedObjects(java.lang.Class[])
     */
    public Set getManagedObjects(Class... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getManagedObjects(java.util.EnumSet, java.lang.Class[])
     */
    public Set getManagedObjects(EnumSet<ObjectState> arg0, Class... arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getMultithreaded()
     */
    public boolean getMultithreaded()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectById(java.lang.Object)
     */
    public Object getObjectById(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectById(java.lang.Object, boolean)
     */
    public Object getObjectById(Object arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectById(java.lang.Class, java.lang.Object)
     */
    public <T> T getObjectById(Class<T> arg0, Object arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectId(java.lang.Object)
     */
    public Object getObjectId(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectIdClass(java.lang.Class)
     */
    public Class getObjectIdClass(Class arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectsById(java.util.Collection)
     */
    public Collection getObjectsById(Collection arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectsById(java.lang.Object[])
     */
    public Object[] getObjectsById(Object... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectsById(java.util.Collection, boolean)
     */
    public Collection getObjectsById(Collection arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectsById(java.lang.Object[], boolean)
     */
    public Object[] getObjectsById(Object[] arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getObjectsById(boolean, java.lang.Object[])
     */
    public Object[] getObjectsById(boolean arg0, Object... arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getPersistenceManagerFactory()
     */
    public PersistenceManagerFactory getPersistenceManagerFactory()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getSequence(java.lang.String)
     */
    public Sequence getSequence(String arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getServerDate()
     */
    public Date getServerDate()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getTransactionalObjectId(java.lang.Object)
     */
    public Object getTransactionalObjectId(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getUserObject()
     */
    public Object getUserObject()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getUserObject(java.lang.Object)
     */
    public Object getUserObject(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#isClosed()
     */
    public boolean isClosed()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeNontransactional(java.lang.Object)
     */
    public void makeNontransactional(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeNontransactionalAll(java.lang.Object[])
     */
    public void makeNontransactionalAll(Object... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeNontransactionalAll(java.util.Collection)
     */
    public void makeNontransactionalAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makePersistent(java.lang.Object)
     */
    public <T> T makePersistent(T arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makePersistentAll(T[])
     */
    public <T> T[] makePersistentAll(T... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makePersistentAll(java.util.Collection)
     */
    public <T> Collection<T> makePersistentAll(Collection<T> arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransactional(java.lang.Object)
     */
    public void makeTransactional(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransactionalAll(java.lang.Object[])
     */
    public void makeTransactionalAll(Object... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransactionalAll(java.util.Collection)
     */
    public void makeTransactionalAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransient(java.lang.Object)
     */
    public void makeTransient(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransient(java.lang.Object, boolean)
     */
    public void makeTransient(Object arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransientAll(java.lang.Object[])
     */
    public void makeTransientAll(Object... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransientAll(java.util.Collection)
     */
    public void makeTransientAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransientAll(java.lang.Object[], boolean)
     */
    public void makeTransientAll(Object[] arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransientAll(boolean, java.lang.Object[])
     */
    public void makeTransientAll(boolean arg0, Object... arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#makeTransientAll(java.util.Collection, boolean)
     */
    public void makeTransientAll(Collection arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newInstance(java.lang.Class)
     */
    public <T> T newInstance(Class<T> arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newNamedQuery(java.lang.Class, java.lang.String)
     */
    public Query newNamedQuery(Class arg0, String arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newObjectIdInstance(java.lang.Class, java.lang.Object)
     */
    public Object newObjectIdInstance(Class arg0, Object arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery()
     */
    public Query newQuery()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(java.lang.Object)
     */
    public Query newQuery(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(java.lang.String)
     */
    public Query newQuery(String arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(java.lang.Class)
     */
    public Query newQuery(Class arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(javax.jdo.Extent)
     */
    public Query newQuery(Extent arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(java.lang.String, java.lang.Object)
     */
    public Query newQuery(String arg0, Object arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(java.lang.Class, java.util.Collection)
     */
    public Query newQuery(Class arg0, Collection arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(java.lang.Class, java.lang.String)
     */
    public Query newQuery(Class arg0, String arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(javax.jdo.Extent, java.lang.String)
     */
    public Query newQuery(Extent arg0, String arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#newQuery(java.lang.Class, java.util.Collection, java.lang.String)
     */
    public Query newQuery(Class arg0, Collection arg1, String arg2)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#putUserObject(java.lang.Object, java.lang.Object)
     */
    public Object putUserObject(Object arg0, Object arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#refresh(java.lang.Object)
     */
    public void refresh(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#refreshAll()
     */
    public void refreshAll()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#refreshAll(java.lang.Object[])
     */
    public void refreshAll(Object... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#refreshAll(java.util.Collection)
     */
    public void refreshAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#refreshAll(javax.jdo.JDOException)
     */
    public void refreshAll(JDOException arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#removeInstanceLifecycleListener(javax.jdo.listener.InstanceLifecycleListener)
     */
    public void removeInstanceLifecycleListener(InstanceLifecycleListener arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#removeUserObject(java.lang.Object)
     */
    public Object removeUserObject(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#retrieve(java.lang.Object)
     */
    public void retrieve(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#retrieve(java.lang.Object, boolean)
     */
    public void retrieve(Object arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#retrieveAll(java.util.Collection)
     */
    public void retrieveAll(Collection arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#retrieveAll(java.lang.Object[])
     */
    public void retrieveAll(Object... arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#retrieveAll(java.util.Collection, boolean)
     */
    public void retrieveAll(Collection arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#retrieveAll(java.lang.Object[], boolean)
     */
    public void retrieveAll(Object[] arg0, boolean arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#retrieveAll(boolean, java.lang.Object[])
     */
    public void retrieveAll(boolean arg0, Object... arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#setCopyOnAttach(boolean)
     */
    public void setCopyOnAttach(boolean arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#setDatastoreReadTimeoutMillis(java.lang.Integer)
     */
    public void setDatastoreReadTimeoutMillis(Integer arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#setDatastoreWriteTimeoutMillis(java.lang.Integer)
     */
    public void setDatastoreWriteTimeoutMillis(Integer arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#setDetachAllOnCommit(boolean)
     */
    public void setDetachAllOnCommit(boolean arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#setIgnoreCache(boolean)
     */
    public void setIgnoreCache(boolean arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#setMultithreaded(boolean)
     */
    public void setMultithreaded(boolean arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#setUserObject(java.lang.Object)
     */
    public void setUserObject(Object arg0)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getProperties()
     */
    public Map<String, Object> getProperties()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#getSupportedProperties()
     */
    public Set<String> getSupportedProperties()
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }

    /* (non-Javadoc)
     * @see javax.jdo.PersistenceManager#setProperty(java.lang.String, java.lang.Object)
     */
    public void setProperty(String arg0, Object arg1)
    {
        throw new UnsupportedOperationException("Method not supported with JPA");
    }
}