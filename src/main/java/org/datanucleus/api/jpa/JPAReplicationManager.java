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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Manager to control the replication of objects from one datastore to another.
 * Allow replication of specified objects, or all objects of particular types.
 * Supports a series of properties defining the replication behaviour.
 */
public class JPAReplicationManager
{
    /** EMF for the source datastore. */
    final EntityManagerFactory emfSource;

    /** EMF for the target datastore. */
    final EntityManagerFactory emfTarget;

    /** Properties defining the replication process. */
    protected Properties properties = new Properties();

    /**
     * Constructor for replicating between source and target EMF.
     * @param emf1 EMF source
     * @param emf2 EMF target
     */
    public JPAReplicationManager(EntityManagerFactory emf1, EntityManagerFactory emf2)
    {
        if (emf1 == null || !emf1.isOpen())
        {
            throw new NucleusException(Localiser.msg("012050"));
        }
        else if (emf2 == null || !emf2.isOpen())
        {
            throw new NucleusException(Localiser.msg("012050"));
        }

        emfSource = emf1;
        emfTarget = emf2;

        properties.setProperty("datanucleus.replicateObjectGraph", "true");
        // TODO Implement support for these properties
        properties.setProperty("datanucleus.deleteUnknownObjects", "false");
    }

    /**
     * Method to set a property for replication.
     * @param key Property key
     * @param value Property value
     */
    public void setProperty(String key, String value)
    {
        properties.setProperty(key, value);
    }

    /**
     * Accessor for the replication properties.
     * Supported properties include
     * <ul>
     * <li>datanucleus.replicateObjectGraph - whether we replicate the object graph from an object.
     *     if this is set we attempt to replicate the graph from this object. Otherwise just the object
     *     and its near neighbours.</li>
     * </ul>
     * @return Replication properties
     */
    public Properties getProperties()
    {
        return properties;
    }

    protected boolean getBooleanProperty(String key)
    {
        String val = properties.getProperty(key);
        if (val == null)
        {
            return false;
        }
        return val.equalsIgnoreCase("true");
    }

    /**
     * Method to perform the replication for all objects of the specified types.
     * @param types Classes to replicate
     */
    public void replicate(Class... types)
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("012052", emfSource, emfTarget, 
                StringUtils.objectArrayToString(types)));
        }

        // Detach from datastore 1
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("012053"));
        }
        EntityManager em1 = emfSource.createEntityManager();
        EntityTransaction tx1 = em1.getTransaction();
        if (getBooleanProperty("datanucleus.replicateObjectGraph"))
        {
            ExecutionContext ec = (ExecutionContext)em1.getDelegate();
            ec.getFetchPlan().setGroup("all");
            ec.getFetchPlan().setMaxFetchDepth(-1);
        }

        ArrayList objects = new ArrayList();
        try
        {
            tx1.begin();

            for (int i=0;i<types.length;i++)
            {
                List results = em1.createQuery(
                    "SELECT Object(T) FROM " + types[i].getName() + " T").getResultList();
                objects.addAll(results);
            }

            tx1.commit(); // Objects detached at commit with JPA
        }
        finally
        {
            if (tx1.isActive())
            {
                tx1.rollback();
            }
            em1.close();
        }

        replicateInTarget(objects.toArray());
    }

    /**
     * Method to perform the replication for all objects of the specified class names.
     * @param classNames Classes to replicate
     */
    public void replicate(String... classNames)
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("012052", emfSource, emfTarget, 
                StringUtils.objectArrayToString(classNames)));
        }

        // Detach from datastore 1
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("012053"));
        }
        EntityManager em1 = emfSource.createEntityManager();
        EntityTransaction tx1 = em1.getTransaction();
        if (getBooleanProperty("datanucleus.replicateObjectGraph"))
        {
            ExecutionContext ec = (ExecutionContext)em1.getDelegate();
            ec.getFetchPlan().setGroup("all");
            ec.getFetchPlan().setMaxFetchDepth(-1);
        }

        ArrayList objects = new ArrayList();
        try
        {
            tx1.begin();

            for (int i=0;i<classNames.length;i++)
            {
                List results = em1.createQuery(
                    "SELECT Object(T) FROM " + classNames[i] + " T").getResultList();
                objects.addAll(results);
            }

            tx1.commit(); // Objects detached at commit with JPA
        }
        finally
        {
            if (tx1.isActive())
            {
                tx1.rollback();
            }
            em1.close();
        }

        replicateInTarget(objects.toArray());
    }

    /**
     * Method to perform the replication of the objects defined by the supplied identities.
     * @param oids Identities of the objects to replicate
     */
    public void replicate(Object... oids)
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("012051", emfSource, emfTarget,
                StringUtils.objectArrayToString(oids)));
        }

        // Detach from datastore 1
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("012053"));
        }
        EntityManager em1 = emfSource.createEntityManager();
        EntityTransaction tx1 = em1.getTransaction();
        ExecutionContext ec = (ExecutionContext)em1.getDelegate();
        if (getBooleanProperty("datanucleus.replicateObjectGraph"))
        {
            ec.getFetchPlan().setGroup("all");
            ec.getFetchPlan().setMaxFetchDepth(-1);
        }

        ArrayList objects = new ArrayList();
        StoreManager storeMgr = ec.getStoreManager();
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        try
        {
            tx1.begin();

            for (int i=0;i<oids.length;i++)
            {
                try
                {
                    String className = storeMgr.getClassNameForObjectID(oids[i], clr, ec);
                    if (className == null)
                    {
                        throw new NucleusException("Unable to find the class name for the identity " + oids[i]);
                    }
                    Class cls = clr.classForName(className);
                    Object obj = em1.find(cls, oids[i]);
                    objects.add(obj);
                }
                catch (ClassNotResolvedException cnre)
                {
                    throw new NucleusException("Object with id " + oids[i] + " threw exception when determing class name", cnre);
                }
            }

            tx1.commit(); // Objects detached at commit with JPA
        }
        finally
        {
            if (tx1.isActive())
            {
                tx1.rollback();
            }
            em1.close();
        }

        replicateInTarget(objects.toArray());
    }

    /**
     * Method to replicate the provided detached objects in the target datastore.
     * @param detachedObjects The detached objects (from the source datastore)
     */
    protected void replicateInTarget(Object... detachedObjects)
    {
        // Attach to datastore 2
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("012054"));
        }

        EntityManager em2 = emfTarget.createEntityManager();
        EntityTransaction tx2 = em2.getTransaction();
        try
        {
            tx2.begin();
            for (int i=0;i<detachedObjects.length;i++)
            {
                em2.merge(detachedObjects[i]);
            }
            tx2.commit();
        }
        finally
        {
            if (tx2.isActive())
            {
                tx2.rollback();
            }
            em2.close();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("012055"));
        }
    }
}