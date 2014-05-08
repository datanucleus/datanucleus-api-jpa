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

import javax.persistence.Cache;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.cache.Level2Cache;
import org.datanucleus.metadata.AbstractClassMetaData;

/**
 * Implementation of the JPA DataStoreCache.
 * Provides a wrapper and hands off calls to the underlying Level2 cache.
 */
public class JPADataStoreCache implements Cache
{
    NucleusContext nucleusCtx;

    /** Underlying Level 2 cache. */
    Level2Cache cache = null;

    /**
     * Constructor.
     * @param nucleusCtx Context
     * @param cache Level 2 Cache
     */
    public JPADataStoreCache(NucleusContext nucleusCtx, Level2Cache cache)
    {
        this.nucleusCtx = nucleusCtx;
        this.cache = cache;
    }

    /**
     * Accessor for the underlying Level 2 cache.
     * @return Underlying L2 cache.
     */
    public Level2Cache getLevel2Cache()
    {
        return cache;
    }

    /**
     * Accessor for whether the object with specified PK is contained in the cache.
     * @param cls The class
     * @param pk PK of the instance to evict.
     * @return whether it is contained
     */
    public boolean contains(Class cls, Object pk)
    {
        ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(null);
        AbstractClassMetaData acmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(cls, clr);
        if (acmd == null)
        {
            throw new EntityNotFoundException();
        }

        return cache.containsOid(pk);
    }

    /**
     * Evict all instances from the second-level cache.
     */
    public void evictAll()
    {
        cache.evictAll();
    }

    /**
     * Evict the parameter instance from the second-level cache.
     * @param cls Class of which to evict all objects
     */
    public void evict(Class cls)
    {
        cache.evictAll(cls, true);
    }

    /**
     * Evict the parameter instance from the second-level cache.
     * @param cls Class of which to evict the object
     * @param pk PK of the instance to evict.
     */
    public void evict(Class cls, Object pk)
    {
        ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(null);
        AbstractClassMetaData acmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(cls, clr);
        if (acmd == null)
        {
            throw new EntityNotFoundException();
        }

        cache.evict(pk);
    }

    /**
     * Return an object of the specified type to allow access to the provider-specific API.
     * If the provider's Cache implementation does not support the specified class, the
     * PersistenceException is thrown.
     * @param cls the class of the object to be returned. This is normally either the underlying 
     * Cache implementation class or an interface that it implements.
     * @return an instance of the specified class
     * @throws PersistenceException if the provider does not support the call.
     */
    public <T> T unwrap(Class<T> cls)
    {
        if (Level2Cache.class.isAssignableFrom(cls))
        {
            return (T) cache;
        }

        throw new PersistenceException("Not yet supported");
    }

}