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
2006 Andy Jefferson - javadocs, reading of persistence.xml, overriding props
2008 Andy Jefferson - getCache(), getProperties(), getSupportedProperties()
2011 Andy Jefferson - removed all use of PMF, using NucleusContext instead
    ...
**********************************************************************/
package org.datanucleus.api.jpa;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.AttributeNode;
import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.Subgraph;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.datanucleus.AbstractNucleusContext;
import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolverImpl;
import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchGroup;
import org.datanucleus.NucleusContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PersistenceNucleusContextImpl;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.jpa.criteria.CriteriaBuilderImpl;
import org.datanucleus.api.jpa.exceptions.NoPersistenceUnitException;
import org.datanucleus.api.jpa.exceptions.NotProviderException;
import org.datanucleus.api.jpa.metadata.JPAEntityGraphRegistrationListener;
import org.datanucleus.api.jpa.metadata.JPAMetaDataManager;
import org.datanucleus.api.jpa.metamodel.MetamodelImpl;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.PersistenceFileMetaData;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.datanucleus.metadata.QueryLanguage;
import org.datanucleus.metadata.QueryMetaData;
import org.datanucleus.metadata.TransactionType;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.query.compiler.QueryCompilationCache;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.connection.ConnectionFactory;
import org.datanucleus.store.connection.ConnectionResourceType;
import org.datanucleus.store.query.cache.QueryDatastoreCompilationCache;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;

/**
 * EntityManagerFactory implementation.
 * Caches the "persistence-unit" MetaData information when encountered, in JavaSE mode.
 */
public class JPAEntityManagerFactory implements EntityManagerFactory, PersistenceUnitUtil, JPAEntityGraphRegistrationListener, Serializable, AutoCloseable
{
    static
    {
        Localiser.registerBundle("org.datanucleus.api.jpa.Localisation", JPAEntityManagerFactory.class.getClassLoader());
    }

    static final long serialVersionUID = -2306972481580259021L;

    /** Logger for enhancing. */
    public static final NucleusLogger LOGGER = NucleusLogger.getLoggerInstance("DataNucleus.JPA");

    /** Cache of EMF keyed by the name. Only used when having single-EMF property enabled. */
    private static ConcurrentHashMap<String, JPAEntityManagerFactory> emfByName = null;

    /** Cache of persistence-unit information for JavaSE. */
    private static volatile Map<String, PersistenceUnitMetaData> unitMetaDataCache = null;

    private String name = null;

    private PersistenceUnitMetaData unitMetaData = null;

    private transient PersistenceNucleusContext nucleusCtx = null;

    private PersistenceContextType persistenceContextType = PersistenceContextType.EXTENDED;

    private boolean closed = false;

    private transient Cache datastoreCache = null;

    private transient JPAQueryCache queryCache = null;

    private transient MetamodelImpl metamodel = null;

    private transient Map<String, JPAEntityGraph> entityGraphsByName = null;

    private transient JPAClassTransformer transformer = null;

    /** Flag for whether this EMF is managed by a container (whether it was created via JavaEE constructor). */
    private boolean containerManaged = false;

    /**
     * Constructor when working in a JavaEE environment.
     * @param unitInfo The "persistent-unit" info
     * @param overridingProps factory properties overriding those in the "persistence-unit"
     */
    public JPAEntityManagerFactory(PersistenceUnitInfo unitInfo, Map overridingProps)
    {
        containerManaged = true;
        Properties props = unitInfo.getProperties();

        // Set persistence context type (default to TRANSACTION unless overridden)
        persistenceContextType = PersistenceContextType.TRANSACTION;
        setPersistenceContextTypeFromProperties(props, overridingProps);

        // Strictly speaking this is only required for the other constructor since the J2EE container should check
        // before calling us but we check anyway
        boolean validProvider = false;
        if (unitInfo.getPersistenceProviderClassName() == null ||
            unitInfo.getPersistenceProviderClassName().equals(PersistenceProviderImpl.class.getName()) ||
            (overridingProps != null && PersistenceProviderImpl.class.getName().equals(overridingProps.get("javax.persistence.provider"))))
        {
            validProvider = true;
        }
        if (!validProvider)
        {
            // Not a valid provider
            throw new NotProviderException(Localiser.msg("EMF.NotProviderForPersistenceUnit", unitInfo.getPersistenceUnitName()));
        }

        // Create a PersistenceUnitMetaData
        URI rootURI = null;
        try
        {
            rootURI = unitInfo.getPersistenceUnitRootUrl().toURI();
        }
        catch (URISyntaxException e1)
        {
        }
        name = unitInfo.getPersistenceUnitName();
        if (unitInfo.getTransactionType() == PersistenceUnitTransactionType.JTA)
        {
            unitMetaData = new PersistenceUnitMetaData(unitInfo.getPersistenceUnitName(), TransactionType.JTA.toString(), rootURI);
        }
        else if (unitInfo.getTransactionType() == PersistenceUnitTransactionType.RESOURCE_LOCAL)
        {
            unitMetaData = new PersistenceUnitMetaData(unitInfo.getPersistenceUnitName(), TransactionType.RESOURCE_LOCAL.toString(), rootURI);
        }
        
        // Classes
        List<String> classNames = unitInfo.getManagedClassNames();
        Iterator<String> classIter = classNames.iterator();
        while (classIter.hasNext())
        {
            unitMetaData.addClassName(classIter.next());
        }

        // Mapping files
        List<String> mappingFileNames = unitInfo.getMappingFileNames();
        Iterator<String> mappingFileIter = mappingFileNames.iterator();
        while (mappingFileIter.hasNext())
        {
            unitMetaData.addMappingFile(mappingFileIter.next());
        }

        // Jars
        List<URL> jarUrls = unitInfo.getJarFileUrls();
        Iterator<URL> jarUrlIter = jarUrls.iterator();
        while (jarUrlIter.hasNext())
        {
            unitMetaData.addJarFile(jarUrlIter.next());
        }

        // Properties
        if (props != null)
        {
            for (Enumeration e = props.propertyNames(); e.hasMoreElements();)
            {
                String prop = (String) e.nextElement();
                unitMetaData.addProperty(prop, props.getProperty(prop));
            }
        }

        // Exclude unlisted classes
        if (unitInfo.excludeUnlistedClasses())
        {
            unitMetaData.setExcludeUnlistedClasses();
        }

        // Provider
        unitMetaData.setProvider(unitInfo.getPersistenceProviderClassName());

        if (overridingProps == null)
        {
            overridingProps = new HashMap();
        }
        else
        {
            //create a new hashmap, because we cannot modify the user map 
            overridingProps = new HashMap(overridingProps);
        }

        // PersistenceUnitInfo will give us javax.sql.DataSource instance(s), so we give that to context
        PersistenceUnitTransactionType type = unitInfo.getTransactionType();
        if (type == PersistenceUnitTransactionType.RESOURCE_LOCAL)
        {
            // Assumed to have non-jta datasource for connections
            if (unitInfo.getNonJtaDataSource() != null)
            {
                overridingProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY, unitInfo.getNonJtaDataSource());
                overridingProps.put(ConnectionFactory.DATANUCLEUS_CONNECTION_RESOURCE_TYPE, ConnectionResourceType.RESOURCE_LOCAL.toString());
            }
            if (unitInfo.getJtaDataSource() != null)
            {
                LOGGER.warn(Localiser.msg("EMF.ContainerLocalWithJTADataSource"));
            }
        }
        else
        {
            // Assumed to have JTA datasource for primary connections
            if (unitInfo.getJtaDataSource() != null)
            {
                overridingProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY, unitInfo.getJtaDataSource());
                overridingProps.put(ConnectionFactory.DATANUCLEUS_CONNECTION_RESOURCE_TYPE, ConnectionResourceType.JTA.toString());
            }
            if (unitInfo.getNonJtaDataSource() != null)
            {
                // Use non-jta for secondary connections
                overridingProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY2, unitInfo.getNonJtaDataSource());
                overridingProps.put(ConnectionFactory.DATANUCLEUS_CONNECTION2_RESOURCE_TYPE, ConnectionResourceType.RESOURCE_LOCAL.toString());
            }
            else
            {
                LOGGER.warn(Localiser.msg("EMF.ContainerJTAWithNoNonJTADataSource"));
            }
        }

        if (unitInfo.getClassLoader() != null)
        {
            overridingProps.put(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY, unitInfo.getClassLoader());
        }

        // ClassTransformer - defaults to disabled but user can enable it via properties
        boolean addClassTransformer = false;
        if (unitMetaData.getProperties() != null)
        {
            Object addCTVal = unitMetaData.getProperties().get(JPAPropertyNames.PROPERTY_JPA_ADD_CLASS_TRANSFORMER);
            if (addCTVal != null && ((String)addCTVal).equalsIgnoreCase("true"))
            {
                addClassTransformer = true;
            }
        }

        Object addCTVal = overridingProps.get(JPAPropertyNames.PROPERTY_JPA_ADD_CLASS_TRANSFORMER);
        if (addCTVal != null && ((String)addCTVal).equalsIgnoreCase("true"))
        {
            addClassTransformer = true;
        }
        if (addClassTransformer)
        {
            try
            {
                LOGGER.debug("Adding ClassTransformer for enhancing classes at runtime");
                transformer = new JPAClassTransformer(overridingProps);
                unitInfo.addTransformer(transformer);
            }
            catch (IllegalStateException ise)
            {
                // Spring probably threw its toys out so log it
                LOGGER.warn("Exception was caught when adding the class transformer. Ignoring it.", ise);
            }
        }

        // Initialise the NucleusContext
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug("Container EntityManagerFactory with persistence-unit defined as follows : \n" + unitMetaData.toString("", "    "));
            Iterator<Map.Entry<String, Object>> propsIter = overridingProps.entrySet().iterator();
            while (propsIter.hasNext())
            {
                Map.Entry<String, Object> entry = propsIter.next();
                NucleusLogger.PERSISTENCE.debug("Container EntityManagerFactory overriding property : name=" + entry.getKey() + " value=" + entry.getValue());
            }
        }
        nucleusCtx = initialiseNucleusContext(unitMetaData, overridingProps, null);

        if (entityGraphsToRegister != null)
        {
            for (JPAEntityGraph eg : entityGraphsToRegister)
            {
                registerEntityGraph(eg, eg.getName());
            }
        }

        assertSingleton(unitMetaData.getName(), this);

        // Turn off loading of metadata from here if required
        boolean allowMetadataLoad = nucleusCtx.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_LOAD_AT_RUNTIME);
        if (!allowMetadataLoad)
        {
            nucleusCtx.getMetaDataManager().setAllowMetaDataLoad(false);
        }

        // Initialise metamodel
        getMetamodel();
    }

    private void setPersistenceContextTypeFromProperties(Properties props, Map overridingProps)
    {
        String persistenceContextTypeProp = null;
        if (props != null && props.containsKey(JPAPropertyNames.PROPERTY_JPA_PERSISTENCE_CONTEXT_TYPE))
        {
            persistenceContextTypeProp = (String)props.get(JPAPropertyNames.PROPERTY_JPA_PERSISTENCE_CONTEXT_TYPE);
        }
        if (overridingProps != null && overridingProps.containsKey(JPAPropertyNames.PROPERTY_JPA_PERSISTENCE_CONTEXT_TYPE))
        {
            persistenceContextTypeProp = (String)overridingProps.get(JPAPropertyNames.PROPERTY_JPA_PERSISTENCE_CONTEXT_TYPE);
        }

        if ("extended".equals(persistenceContextTypeProp))
        {
            persistenceContextType = PersistenceContextType.EXTENDED;
        }
        else if ("transaction".equals(persistenceContextTypeProp))
        {
            persistenceContextType = PersistenceContextType.TRANSACTION;
        }
    }

    /**
     * Convenience constructor to allow for dynamic persistence-unit creation in JavaSE.
     * @param pumd Persistence unit metadata
     * @param overridingProps Properties overriding those defined for this unit
     */
    public JPAEntityManagerFactory(PersistenceUnitMetaData pumd, Map overridingProps)
    {
        name = pumd.getName();
        if (unitMetaDataCache == null)
        {
            // Create our cache so we save on lookups
            unitMetaDataCache = new ConcurrentHashMap<String, PersistenceUnitMetaData>();
        }
        unitMetaDataCache.put(name, pumd);
        Properties props = pumd.getProperties();

        // Set persistence context type (default to EXTENDED unless overridden)
        persistenceContextType = PersistenceContextType.EXTENDED;
        setPersistenceContextTypeFromProperties(props, overridingProps);

        initialise(pumd, overridingProps, null);

        // Initialise metamodel
        getMetamodel();
    }

    /**
     * Constructor when working in a JavaSE environment.
     * @param unitName Name of the "persistent-unit" to use
     * @param overridingProps factory properties overriding those in the "persistence-unit"
     */
    public JPAEntityManagerFactory(String unitName, Map overridingProps)
    {
        name = unitName;
        if (unitMetaDataCache == null)
        {
            // Create our cache so we save on lookups
            unitMetaDataCache = new ConcurrentHashMap<String, PersistenceUnitMetaData>();
        }

        // Find the "persistence-unit" with this name
        PluginManager pluginMgr = null;
        unitMetaData = unitMetaDataCache.get(unitName);
        if (unitMetaData == null)
        {
            // Find all "META-INF/persistence.xml" files in the current thread loader CLASSPATH and parse them
            pluginMgr = PluginManager.createPluginManager(overridingProps, this.getClass().getClassLoader());
            unitMetaData = getPersistenceUnitMetaDataForName(unitName, pluginMgr, overridingProps);
            if (unitMetaData == null)
            {
                throw new NoPersistenceUnitException("No persistence unit found with name " + unitName + ". Check that your persistence.xml is in META-INF from the root of the CLASSPATH");
            }
        }

        // Set persistence context type (default to EXTENDED unless overridden)
        persistenceContextType = PersistenceContextType.EXTENDED;
        Properties props = unitMetaData.getProperties();
        setPersistenceContextTypeFromProperties(props, overridingProps);

        initialise(unitMetaData, overridingProps, pluginMgr);

        // Initialise metamodel
        getMetamodel();
    }

    public NucleusContext getNucleusContext()
    {
        return nucleusCtx;
    }

    /**
     * Method to initialise this EMF for the specified persistence-unit and overriding properties.
     * @param pumd Persistence unit definition
     * @param overridingProps Any overriding properties
     * @throws NotProviderException If this provider is not valid for the passed persistence-unit
     */
    private void initialise(PersistenceUnitMetaData pumd, Map overridingProps, PluginManager pluginMgr)
    {
        if (pumd == null)
        {
            throw new IllegalArgumentException("Persistence-unit supplied to initialise was null!");
        }

        // Check the provider is ok for our use
        boolean validProvider = false;
        if (pumd.getProvider() == null || pumd.getProvider().equals(PersistenceProviderImpl.class.getName()))
        {
            validProvider = true;
        }
        else if (overridingProps != null && PersistenceProviderImpl.class.getName().equals(overridingProps.get("javax.persistence.provider")))
        {
            validProvider = true;
        }
        if (!validProvider)
        {
            // Not a valid provider
            throw new NotProviderException(Localiser.msg("EMF.NotProviderForPersistenceUnit", pumd.getName()));
        }

        // Cache the unit definition
        unitMetaData = pumd;

        // Convert any jta-data-source and non-jta-data-source into the requisite internal persistent property
        if (unitMetaData.getTransactionType() == TransactionType.RESOURCE_LOCAL)
        {
            // Assumed to have non-jta datasource for connections
            if (unitMetaData.getNonJtaDataSource() != null)
            {
                overridingProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME, unitMetaData.getNonJtaDataSource());
                overridingProps.put(ConnectionFactory.DATANUCLEUS_CONNECTION_RESOURCE_TYPE, ConnectionResourceType.RESOURCE_LOCAL.toString());
            }
            if (unitMetaData.getJtaDataSource() != null)
            {
                // TODO Update the message
                LOGGER.warn(Localiser.msg("EMF.ContainerLocalWithJTADataSource"));
            }
        }
        else if (unitMetaData.getTransactionType() == TransactionType.JTA)
        {
            // Assumed to have non-JTA datasource for primary connections
            if (unitMetaData.getJtaDataSource() != null)
            {
                overridingProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME, unitMetaData.getJtaDataSource());
                overridingProps.put(ConnectionFactory.DATANUCLEUS_CONNECTION_RESOURCE_TYPE, ConnectionResourceType.JTA.toString());
            }
            if (unitMetaData.getNonJtaDataSource() != null)
            {
                // Use non-jta for secondary connections
                overridingProps.put(PropertyNames.PROPERTY_CONNECTION_FACTORY2_NAME, unitMetaData.getNonJtaDataSource());
                overridingProps.put(ConnectionFactory.DATANUCLEUS_CONNECTION2_RESOURCE_TYPE, ConnectionResourceType.RESOURCE_LOCAL.toString());
            }
            else
            {
                // TODO Update the message
                LOGGER.warn(Localiser.msg("EMF.ContainerJTAWithNoNonJTADataSource"));
            }
        }

        // Initialise the context (even if unitMetaData is null)
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug("Application EntityManagerFactory with persistence-unit defined as follows : \n" + unitMetaData.toString("", "    "));
            Iterator<Map.Entry<String, Object>> propsIter = overridingProps.entrySet().iterator();
            while (propsIter.hasNext())
            {
                Map.Entry<String, Object> entry = propsIter.next();
                NucleusLogger.PERSISTENCE.debug("Application EntityManagerFactory overriding property : name=" + entry.getKey() + " value=" + entry.getValue());
            }
        }

        nucleusCtx = initialiseNucleusContext(pumd, overridingProps, pluginMgr);

        if (entityGraphsToRegister != null)
        {
            for (JPAEntityGraph eg : entityGraphsToRegister)
            {
                registerEntityGraph(eg, eg.getName());
            }
            entityGraphsToRegister = null;
        }

        assertSingleton(pumd.getName(), this);

        // Turn off loading of metadata from here if required
        boolean allowMetadataLoad = nucleusCtx.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_LOAD_AT_RUNTIME);
        if (!allowMetadataLoad)
        {
            nucleusCtx.getMetaDataManager().setAllowMetaDataLoad(false);
        }
    }

    /**
     * Accessor for whether the EMF is managed by a container (JavaEE).
     * @return Whether managed by a container
     */
    public boolean isContainerManaged()
    {
        return containerManaged;
    }

    public String getName()
    {
        return name;
    }

    /**
     * Method to close the factory.
     */
    public synchronized void close()
    {
        assertIsClosed();

        if (emfByName != null && name != null)
        {
            // Closing so clean out from singleton pattern handler
            emfByName.remove(name);
        }
        if (queryCache != null)
        {
            queryCache.evictAll();
            queryCache = null;
        }
        if (datastoreCache != null)
        {
            datastoreCache.evictAll();
            datastoreCache = null;
        }
        if (metamodel != null)
        {
            metamodel = null;
        }
        if (unitMetaData != null)
        {
            unitMetaData = null;
        }
        if (entityGraphsByName != null)
        {
            entityGraphsByName.clear();
            entityGraphsByName = null;
        }
        if (entityGraphsToRegister != null)
        {
            entityGraphsToRegister.clear();
            entityGraphsToRegister = null;
        }

        nucleusCtx.close();
        nucleusCtx = null;

        closed = true;
    }

    /**
     * Accessor for whether the factory is open
     * @return Whether it is open
     */
    public boolean isOpen()
    {
        return !closed;
    }

    /**
     * Accessor for the query results cache.
     * @return Query results cache
     */
    public JPAQueryCache getQueryCache()
    {
        assertIsClosed();

        if (queryCache == null)
        {
            queryCache = new JPAQueryCache(nucleusCtx.getStoreManager().getQueryManager().getQueryResultsCache());
        }
        return queryCache;
    }

    /**
     * Accessor for the query generic compilation cache.
     * @return Query generic compilation cache
     */
    public QueryCompilationCache getQueryGenericCompilationCache()
    {
        return nucleusCtx.getStoreManager().getQueryManager().getQueryCompilationCache();
    }

    /**
     * Accessor for the query datastore compilation cache.
     * @return Query datastore compilation cache
     */
    public QueryDatastoreCompilationCache getQueryDatastoreCompilationCache()
    {
        return nucleusCtx.getStoreManager().getQueryManager().getQueryDatastoreCompilationCache();
    }

    /**
     * Method to save the specified query under the provided name, so it can be used as a named query.
     * If there is already a named query of this name it is overwritten.
     * @param query The query whose definition we save as named
     */
    public void addNamedQuery(String name, Query query)
    {
        assertIsClosed();

        if (query == null)
        {
            return;
        }

        org.datanucleus.store.query.Query intQuery = ((JPAQuery)query).getInternalQuery();
        QueryMetaData qmd = new QueryMetaData(name);
        qmd.setLanguage(QueryLanguage.JPQL.toString());
        qmd.setQuery(intQuery.toString());
        qmd.setResultClass(intQuery.getResultClassName());
        qmd.setUnique(intQuery.isUnique());
        Map<String, Object> queryExts = intQuery.getExtensions();
        if (queryExts != null && !queryExts.isEmpty())
        {
            Iterator<Map.Entry<String, Object>> queryExtsIter = queryExts.entrySet().iterator();
            while (queryExtsIter.hasNext())
            {
                Map.Entry<String, Object> queryExtEntry = queryExtsIter.next();
                qmd.addExtension(queryExtEntry.getKey(), "" + queryExtEntry.getValue());
            }
        }

        // Register the query under this name, ignoring any parameters
        nucleusCtx.getMetaDataManager().registerNamedQuery(qmd);
    }

    /**
     * Method to create an (application-managed) entity manager.
     * @return The Entity Manager
     */
    public EntityManager createEntityManager()
    {
        assertIsClosed();

        return newEntityManager(nucleusCtx, persistenceContextType, SynchronizationType.SYNCHRONIZED);
    }

    /**
     * Method to create an (application-managed) entity manager with the specified properties.
     * This creates a new underlying context since each EMF is locked when created to stop config changes.
     * @param overridingProps Properties to use for this manager
     * @return The Entity Manager
     */
    public EntityManager createEntityManager(Map overridingProps)
    {
        assertIsClosed();

        // Create a NucleusContext to do the actual persistence, using the original persistence-unit, plus these properties
        return newEntityManager(initialiseNucleusContext(unitMetaData, overridingProps, null), persistenceContextType, SynchronizationType.SYNCHRONIZED);
    }

    /**
     * Create a new JTA application-managed EntityManager with the specified synchronization type.
     * This method returns a new EntityManager instance each time it is invoked.
     * The isOpen method will return true on the returned instance.
     * @param syncType how and when the entity manager should be synchronized with the current JTA transaction
     * @return entity manager instance
     * @throws IllegalStateException if the entity manager factory has been configured for 
     *     resource-local entity managers or has been closed
     * @since JPA2.1
     */
    public EntityManager createEntityManager(SynchronizationType syncType)
    {
        assertIsClosed();
        if (nucleusCtx.getConfiguration().getStringProperty(PropertyNames.PROPERTY_TRANSACTION_TYPE).equalsIgnoreCase(TransactionType.RESOURCE_LOCAL.toString()))
        {
            throw new IllegalStateException("EntityManagerFactory is configured for RESOURCE_LOCAL");
        }

        return newEntityManager(nucleusCtx, persistenceContextType, syncType);
    }

    /**
     * Create a new JTA application-managed EntityManager with the specified synchronization type 
     * and Map of properties. This method returns a new EntityManager instance each time it is invoked.
     * The isOpen method will return true on the returned instance.
     * @param syncType how and when the entity manager should be synchronized with the current JTA transaction
     * @param overridingProps properties for entity manager; may be null
     * @return entity manager instance
     * @throws IllegalStateException if the entity manager factory has been configured for resource-local 
     *     entity managers or has been closed
     * @since JPA2.1
     */
    public EntityManager createEntityManager(SynchronizationType syncType, Map overridingProps)
    {
        assertIsClosed();
        if (nucleusCtx.getConfiguration().getStringProperty(PropertyNames.PROPERTY_TRANSACTION_TYPE).equalsIgnoreCase(TransactionType.RESOURCE_LOCAL.toString()))
        {
            throw new IllegalStateException("EntityManagerFactory is configured for RESOURCE_LOCAL");
        }

        // Create a NucleusContext to do the actual persistence, using the original persistence-unit, plus these properties
        return newEntityManager(initialiseNucleusContext(unitMetaData, overridingProps, null), persistenceContextType, syncType);
    }

    /**
     * Creates an {@link EntityManager}.
     * Override if you want to return a different type that implements this interface.
     * @param nucleusCtx Nucleus Context
     * @param contextType The persistence context type
     * @param syncType Synchronization type
     * @return The EntityManager
     */
    protected EntityManager newEntityManager(PersistenceNucleusContext nucleusCtx, PersistenceContextType contextType, SynchronizationType syncType)
    {
        return new JPAEntityManager(this, nucleusCtx, contextType, syncType);
    }

    /**
     * Method to initialise a PersistenceManagerFactory that will control the persistence.
     * If the unitMetaData is null will simply create a default context without initialising any MetaData etc.
     * If there is a unitMetaData then all metadata for that unit will be loaded/initialised.
     * @param unitMetaData The "persistence-unit" metadata (if any)
     * @param overridingProps Properties to override all others
     * @param pluginMgr Plugin Manager
     * @return The PersistenceManagerFactory
     */
    protected PersistenceNucleusContext initialiseNucleusContext(PersistenceUnitMetaData unitMetaData, Map overridingProps, PluginManager pluginMgr)
    {
        // Build map of properties for the NucleusContext, with all properties in lower-case
        // We use lower-case so we can detect presence of some properties, hence allowing case-insensitivity
        Map<String, Object> props = new HashMap();

        if (unitMetaData.getJtaDataSource() != null)
        {
            props.put(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME.toLowerCase(Locale.ENGLISH), unitMetaData.getJtaDataSource());
        }
        if (unitMetaData.getNonJtaDataSource() != null)
        {
            props.put(PropertyNames.PROPERTY_CONNECTION_FACTORY2_NAME.toLowerCase(Locale.ENGLISH), unitMetaData.getNonJtaDataSource());
        }
        if (unitMetaData.getTransactionType() != null)
        {
            props.put(PropertyNames.PROPERTY_TRANSACTION_TYPE.toLowerCase(Locale.ENGLISH), unitMetaData.getTransactionType().toString());
        }

        if (unitMetaData.getCaching().equalsIgnoreCase("NONE"))
        {
            props.put(PropertyNames.PROPERTY_CACHE_L2_TYPE, "none");
        }
        else if (!unitMetaData.getCaching().equalsIgnoreCase("UNSPECIFIED"))
        {
            props.put(PropertyNames.PROPERTY_CACHE_L2_MODE, unitMetaData.getCaching());
        }

        Properties unitProps = unitMetaData.getProperties();
        if (unitProps != null)
        {
            // Props for this "persistence-unit"
            for (Object key : unitProps.keySet())
            {
                String propName = (String)key;
                props.put(propName.toLowerCase(Locale.ENGLISH), unitProps.getProperty(propName));
            }
        }

        // Set properties appropriate for persistence context
        if (persistenceContextType == PersistenceContextType.TRANSACTION)
        {
            // Need to detach instances at transaction commit
            if (!props.containsKey(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT.toLowerCase(Locale.ENGLISH)))
            {
                props.put(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT.toLowerCase(Locale.ENGLISH), "true");
            }
            if (!props.containsKey(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK.toLowerCase(Locale.ENGLISH)))
            {
                props.put(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK.toLowerCase(Locale.ENGLISH), "true");
            }
            if (!props.containsKey(PropertyNames.PROPERTY_DETACH_ON_CLOSE.toLowerCase(Locale.ENGLISH)))
            {
                props.put(PropertyNames.PROPERTY_DETACH_ON_CLOSE.toLowerCase(Locale.ENGLISH), "false");
            }
        }
        else if (persistenceContextType == PersistenceContextType.EXTENDED)
        {
            // Need to keep instances active until close of EntityManager and then detach
            if (!props.containsKey(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT.toLowerCase(Locale.ENGLISH)))
            {
                props.put(PropertyNames.PROPERTY_DETACH_ALL_ON_COMMIT.toLowerCase(Locale.ENGLISH), "false");
            }
            if (!props.containsKey(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK.toLowerCase(Locale.ENGLISH)))
            {
                props.put(PropertyNames.PROPERTY_DETACH_ALL_ON_ROLLBACK.toLowerCase(Locale.ENGLISH), "false");
            }
            if (!props.containsKey(PropertyNames.PROPERTY_DETACH_ON_CLOSE.toLowerCase(Locale.ENGLISH)))
            {
                props.put(PropertyNames.PROPERTY_DETACH_ON_CLOSE.toLowerCase(Locale.ENGLISH), "true");
            }
        }

        if (overridingProps != null)
        {
            if (overridingProps.containsKey(PropertyNames.PROPERTY_CONNECTION_URL))
            {
                // User providing connectionURL overriding persistence unit so remove any JNDI
                props.remove(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME.toLowerCase(Locale.ENGLISH));
            }
            else if (overridingProps.containsKey(JPAPropertyNames.PROPERTY_JPA_STANDARD_JDBC_URL))
            {
                // User providing connectionURL overriding persistence unit so remove any JNDI
                props.remove(PropertyNames.PROPERTY_CONNECTION_FACTORY_NAME.toLowerCase(Locale.ENGLISH));
            }

            // Apply the overriding properties
            props.putAll(overridingProps);
        }

        props.put(PropertyNames.PROPERTY_AUTOSTART_MECHANISM.toLowerCase(Locale.ENGLISH), "None"); // Don't allow autostart with JPA
        props.put(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME.toLowerCase(Locale.ENGLISH), unitMetaData.getName()); // Make sure we register the name
        if (unitMetaData.getValidationMode() != null)
        {
            // Set validation mode if set on persistence-unit
            props.put(PropertyNames.PROPERTY_VALIDATION_MODE.toLowerCase(Locale.ENGLISH), unitMetaData.getValidationMode());
        }
        props.remove(JPAPropertyNames.PROPERTY_JPA_PERSISTENCE_CONTEXT_TYPE); // Processed above
        if (!props.containsKey(PropertyNames.PROPERTY_TRANSACTION_TYPE.toLowerCase(Locale.ENGLISH)))
        {
            // Default to RESOURCE_LOCAL txns
            props.put(PropertyNames.PROPERTY_TRANSACTION_TYPE.toLowerCase(Locale.ENGLISH), TransactionType.RESOURCE_LOCAL.toString());
        }
        else
        {
            // let TransactionType.JTA imply ResourceType.JTA
            String transactionType = (String)props.get(PropertyNames.PROPERTY_TRANSACTION_TYPE.toLowerCase(Locale.ENGLISH));
            if (TransactionType.JTA.toString().equalsIgnoreCase(transactionType))
            {
                props.put(ConnectionFactory.DATANUCLEUS_CONNECTION_RESOURCE_TYPE.toLowerCase(Locale.ENGLISH), ConnectionResourceType.JTA.toString());
                props.put(ConnectionFactory.DATANUCLEUS_CONNECTION2_RESOURCE_TYPE.toLowerCase(Locale.ENGLISH), ConnectionResourceType.JTA.toString());
            }
        }

        // Extract any properties that affect NucleusContext startup
        Map startupProps = null;
        for (String startupPropName : AbstractNucleusContext.STARTUP_PROPERTIES)
        {
            Iterator<Map.Entry<String, Object>> propsEntryIter = props.entrySet().iterator();
            while (propsEntryIter.hasNext())
            {
                Map.Entry<String, Object> propsEntry = propsEntryIter.next();
                if (propsEntry.getKey().equalsIgnoreCase(startupPropName))
                {
                    if (startupProps == null)
                    {
                        startupProps = new HashMap();
                    }
                    startupProps.put(startupPropName, propsEntry.getValue());
                }
            }
        }

        // Initialise the context for JPA
        PersistenceNucleusContext nucCtx = (pluginMgr != null ? new PersistenceNucleusContextImpl("JPA", startupProps, pluginMgr) :
            new PersistenceNucleusContextImpl("JPA", startupProps));

        // Apply remaining persistence properties
        Configuration propConfig = nucCtx.getConfiguration();
        propConfig.setPersistenceProperties(props);
        JPAMetaDataManager mmgr = (JPAMetaDataManager)nucCtx.getMetaDataManager();

        // Initialise metadata manager, and load up the MetaData implied by this "persistence-unit"
        mmgr.setAllowXML(propConfig.getBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_XML));
        mmgr.setAllowAnnotations(propConfig.getBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_ANNOTATIONS));
        mmgr.setValidate(propConfig.getBooleanProperty(PropertyNames.PROPERTY_METADATA_XML_VALIDATE));
        mmgr.setDefaultNullable(propConfig.getBooleanProperty(PropertyNames.PROPERTY_METADATA_DEFAULT_NULLABLE));
        mmgr.registerEntityGraphListener(this);
        nucCtx.getMetaDataManager().loadPersistenceUnit(unitMetaData, null);

        // Initialise the context, creating the StoreManager
        nucCtx.initialise();

        return nucCtx;
    }

    /**
     * Get the properties and associated values that are in effect for the entity manager factory. 
     * Changing the contents of the map does not change the configuration in effect.
     * @return properties
     */
    public Map<String, Object> getProperties()
    {
        return nucleusCtx.getConfiguration().getPersistenceProperties();
    }

    /**
     * Get the names of the properties that are supported for use with the entity manager factory. 
     * These correspond to properties that may be passed to the methods of the EntityManagerFactory 
     * interface that take a properties argument. These include all standard properties as well as
     * vendor-specific properties supported by the provider. These properties may or may not currently 
     * be in effect.
     * @return properties and hints
     */
    public Set<String> getSupportedProperties()
    {
        assertIsClosed();

        return nucleusCtx.getConfiguration().getSupportedProperties();
    }

    protected void assertIsClosed()
    {
        if (closed)
        {
            throw new IllegalStateException("EntityManagerFactory is already closed");
        }
    }

    /**
     * Accessor for the second level cache.
     * @return Level 2 cache
     */
    public Cache getCache()
    {
        assertIsClosed();

        if (datastoreCache == null && nucleusCtx.hasLevel2Cache())
        {
            // Initialise the L2 cache (if used)
            datastoreCache = new JPADataStoreCache(nucleusCtx, nucleusCtx.getLevel2Cache());
        }
        return datastoreCache;
    }

    public Metamodel getMetamodel()
    {
        assertIsClosed();

        if (metamodel == null)
        {
            metamodel = new MetamodelImpl(nucleusCtx.getMetaDataManager());
        }
        return metamodel;
    }

    public CriteriaBuilder getCriteriaBuilder()
    {
        assertIsClosed();

        return new CriteriaBuilderImpl(this);
    }

    /* (non-Javadoc)
     * @see javax.persistence.EntityManagerFactory#getPersistenceUnitUtil()
     */
    public PersistenceUnitUtil getPersistenceUnitUtil()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.PersistenceUnitUtil#getIdentifier(java.lang.Object)
     */
    public Object getIdentifier(Object entity)
    {
        assertIsClosed();

        return nucleusCtx.getApiAdapter().getIdForObject(entity);
    }

    /* (non-Javadoc)
     * @see javax.persistence.PersistenceUnitUtil#isLoaded(java.lang.Object, java.lang.String)
     */
    public boolean isLoaded(Object entity, String attrName)
    {
        assertIsClosed();

        ExecutionContext ec = nucleusCtx.getApiAdapter().getExecutionContext(entity);
        if (ec == null)
        {
            return false;
        }
        ObjectProvider op = ec.findObjectProvider(entity);
        if (op == null)
        {
            // Not managed
            return false;
        }
        AbstractClassMetaData cmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(entity.getClass(), nucleusCtx.getClassLoaderResolver(entity.getClass().getClassLoader()));
        if (cmd == null)
        {
            // No metadata
            return false;
        }
        return op.isLoaded(cmd.getAbsolutePositionOfMember(attrName));
    }

    /* (non-Javadoc)
     * @see javax.persistence.PersistenceUnitUtil#isLoaded(java.lang.Object)
     */
    public boolean isLoaded(Object entity)
    {
        assertIsClosed();

        if (nucleusCtx.getApiAdapter().getObjectState(entity).equals("hollow"))
        {
            return false;
        }
        return true;
    }

    /**
     * Return an object of the specified type to allow access to the provider-specific API.
     * If the provider's EntityManagerFactory implementation does not support the specified class, the
     * PersistenceException is thrown.
     * @param cls the class of the object to be returned. This is normally either the underlying 
     * EntityManagerFactory implementation class or an interface that it implements.
     * @return an instance of the specified class
     * @throws PersistenceException if the provider does not support the call.
     */
    public <T> T unwrap(Class<T> cls)
    {
        assertIsClosed();

        if (ClassConstants.NUCLEUS_CONTEXT.isAssignableFrom(cls))
        {
            return (T) nucleusCtx;
        }
        if (ClassConstants.STORE_MANAGER.isAssignableFrom(cls))
        {
            return (T) nucleusCtx.getStoreManager();
        }
        if (ClassConstants.METADATA_MANAGER.isAssignableFrom(cls))
        {
            return (T) nucleusCtx.getMetaDataManager();
        }

        throw new PersistenceException("Not yet supported unwrapping of " + cls.getName());
    }

    public EntityGraph getNamedEntityGraph(String graphName)
    {
        if (entityGraphsByName != null)
        {
            return entityGraphsByName.get(graphName);
        }
        return null;
    }

    public Set<String> getEntityGraphNames()
    {
        if (entityGraphsByName == null)
        {
            return null;
        }
        return entityGraphsByName.keySet();
    }

    public <T> List<EntityGraph<? super T>> getEntityGraphsByType(Class<T> entityClass)
    {
        if (entityGraphsByName == null)
        {
            return null;
        }

        final List<EntityGraph<? super T>> results = new ArrayList<EntityGraph<? super T>>();
        for (JPAEntityGraph eg : entityGraphsByName.values())
        {
            if (eg.getClassType().isAssignableFrom(entityClass))
            {
                results.add(eg);
            }
        }
        return results;
    }

    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> graph)
    {
        assertIsClosed();

        if (graph == null)
        {
            // Maybe better to throw an exception but the JPA API says nothing
            return;
        }
        AbstractClassMetaData cmd = nucleusCtx.getMetaDataManager().getMetaDataForClass(((JPAEntityGraph)graph).getClassType(), nucleusCtx.getClassLoaderResolver(null));
        if (cmd == null)
        {
            throw new IllegalStateException("Attempt to add graph " + graph + " for type=" + ((JPAEntityGraph)graph).getClassType() + " but is not a known Entity");
        }

        String myGraphName = (graphName != null ? graphName : cmd.getEntityName());
        ((JPAEntityGraph)graph).setName(myGraphName);

        if (entityGraphsByName == null)
        {
            entityGraphsByName = new HashMap<String, JPAEntityGraph>();
        }
        entityGraphsByName.put(myGraphName, (JPAEntityGraph) graph);

        // Register graph as FetchGroupMetaData for the class
        registerEntityGraph((JPAEntityGraph) graph, graph.getName());
    }

    public void registerEntityGraph(JPAEntityGraph eg, String graphName)
    {
        assertIsClosed();

        FetchGroup fg = new FetchGroup(nucleusCtx, graphName, eg.getClassType());
        if (eg.getIncludeAllAttributes())
        {
            fg.addCategory(FetchGroup.ALL);
        }
        else
        {
            List<AttributeNode> graphNodes = eg.getAttributeNodes();
            if (graphNodes != null)
            {
                for (AttributeNode node : graphNodes)
                {
                    fg.addMember(node.getAttributeName());
                    Map<Class, Subgraph> subgraphByTypeForNode = node.getSubgraphs();
                    if (!subgraphByTypeForNode.isEmpty())
                    {
                        // Add FetchGroup for the subgraph type under the same fetch group name as this graph uses
                        for (Map.Entry<Class, Subgraph> subgraphEntry : subgraphByTypeForNode.entrySet())
                        {
                            FetchGroup subFG = new FetchGroup(nucleusCtx, graphName, subgraphEntry.getKey());
                            List<AttributeNode> subgraphNodes = subgraphEntry.getValue().getAttributeNodes();
                            if (subgraphNodes != null)
                            {
                                for (AttributeNode subNode : subgraphNodes)
                                {
                                    subFG.addMember(subNode.getAttributeName());
                                }
                            }
                            nucleusCtx.getFetchGroupManager().addFetchGroup(subFG);
                        }
                    }
                }
            }
            Map<Class, Subgraph> subclassSubgraphs = eg.getSubclassSubgraphs();
            if (subclassSubgraphs != null && !subclassSubgraphs.isEmpty())
            {
                // Add FetchGroup for the subclass type under the same fetch group name as this graph uses
                for (Map.Entry<Class, Subgraph> subclassEntry : subclassSubgraphs.entrySet())
                {
                    FetchGroup subFG = new FetchGroup(nucleusCtx, graphName, subclassEntry.getKey());
                    List<AttributeNode> subgraphNodes = subclassEntry.getValue().getAttributeNodes();
                    if (subgraphNodes != null)
                    {
                        for (AttributeNode subNode : subgraphNodes)
                        {
                            subFG.addMember(subNode.getAttributeName());
                        }
                    }
                    nucleusCtx.getFetchGroupManager().addFetchGroup(subFG);
                }
            }
        }
        nucleusCtx.getFetchGroupManager().addFetchGroup(fg);
    }

    public void deregisterEntityGraph(String graphName)
    {
        assertIsClosed();

        Set<FetchGroup> fgs = nucleusCtx.getFetchGroupManager().getFetchGroupsWithName(graphName);
        if (fgs != null)
        {
            for (FetchGroup fg : fgs)
            {
                nucleusCtx.getFetchGroupManager().removeFetchGroup(fg);
            }
        }
    }

    Random random = new Random();

    public String getDefinedEntityGraphName()
    {
        return "DN_ENTITY_GRAPH" + random.nextLong();
    }

    List<JPAEntityGraph> entityGraphsToRegister = null;

    public void entityGraphRegistered(JPAEntityGraph eg)
    {
        // Add to internal map
        if (entityGraphsByName == null)
        {
            entityGraphsByName = new HashMap<String, JPAEntityGraph>();
        }
        entityGraphsByName.put(eg.getName(), eg);

        if (nucleusCtx != null)
        {
            // Register graph as FetchGroupMetaData for the class
            registerEntityGraph(eg, eg.getName());
        }
        else
        {
            // Save the EntityGraph for later registration when the MetaDataManager/NucleusContext is available
            if (entityGraphsToRegister == null)
            {
                entityGraphsToRegister = new ArrayList();
            }
            entityGraphsToRegister.add(eg);
        }
    }

    private static PersistenceUnitMetaData getPersistenceUnitMetaDataForName(String unitName, PluginManager pluginMgr, Map overridingProps)
    {
        PersistenceUnitMetaData pumd = null;

        // Find all "META-INF/persistence.xml" files in the current thread loader CLASSPATH and parse them
        String persistenceFileName = null;
        boolean validate = false;
        if (overridingProps != null)
        {
            if (overridingProps.containsKey(PropertyNames.PROPERTY_PERSISTENCE_XML_FILENAME))
            {
                persistenceFileName = (String)overridingProps.get(PropertyNames.PROPERTY_PERSISTENCE_XML_FILENAME);
            }
            if (overridingProps.containsKey(PropertyNames.PROPERTY_METADATA_XML_VALIDATE))
            {
                validate = Boolean.getBoolean((String)overridingProps.get(PropertyNames.PROPERTY_METADATA_XML_VALIDATE));
            }
        }
        PersistenceFileMetaData[] files = MetaDataUtils.parsePersistenceFiles(pluginMgr, persistenceFileName, 
            validate, new ClassLoaderResolverImpl());
        if (files == null)
        {
            // No "persistence.xml" files found
            LOGGER.warn(Localiser.msg("EMF.NoPersistenceXML"));
        }
        else
        {
            for (int i=0;i<files.length;i++)
            {
                PersistenceUnitMetaData[] unitmds = files[i].getPersistenceUnits();
                for (int j=0;j<unitmds.length;j++)
                {
                    // Cache the "persistence-unit" for future reference
                    if (!unitMetaDataCache.containsKey(unitmds[j].getName()))
                    {
                        unitMetaDataCache.put(unitmds[j].getName(), unitmds[j]);
                    }
                    else
                    {
                        LOGGER.warn("Found persistence-unit with name \"" + unitmds[j].getName() + "\" at " + unitmds[j].getRootURI() +
                            " but already found one with same name at " + JPAEntityManagerFactory.unitMetaDataCache.get(unitmds[j].getName()).getRootURI());
                    }

                    if (unitmds[j].getName().equals(unitName) && pumd == null)
                    {
                        pumd = unitmds[j];
                        pumd.clearJarFiles(); // Jar files not applicable to J2SE [JPA 6.3]
                    }
                }
            }
        }

        if (pumd == null)
        {
            // No "persistence-unit" of the same name as requested so nothing to manage the persistence of
            LOGGER.warn(Localiser.msg("EMF.PersistenceUnitNotFound", unitName));
        }
        else
        {
            unitMetaDataCache.put(unitName, pumd);
        }

        return pumd;
    }

    /**
     * Check on serialisation of the EMF.
     * @param oos The output stream to serialise to
     * @throws IOException Exception thrown if error
     */
    private void writeObject(ObjectOutputStream oos) throws IOException 
    {
        if (name == null) 
        {
            throw new InvalidObjectException("Could not serialize EntityManagerFactory with null name");
        }
        oos.defaultWriteObject();
        oos.writeObject(nucleusCtx.getConfiguration().getPersistenceProperties());
    }

    private Map<String, Object> deserialisationProps = null;
    private void readObject(java.io.ObjectInputStream ois) throws IOException, ClassNotFoundException 
    {
        ois.defaultReadObject();
        deserialisationProps = (Map<String, Object>) ois.readObject();
    }

    /**
     * Control deserialisation of the EMF where we have a singleton (in emfByName).
     * @return The EMF
     * @throws InvalidObjectException Thrown if an error occurs
     */
    private Object readResolve() throws InvalidObjectException 
    {
        JPAEntityManagerFactory emf = null;
        if (emfByName != null)
        {
            // Return singleton if present to save reinitialisation
            emf = emfByName.get(name);
            if (emf != null)
            {
                return emf;
            }
        }

        // Use deserialised object, so need to initialise it
        PersistenceUnitMetaData pumd = (unitMetaDataCache != null ? unitMetaDataCache.get(name) : null);
        PluginManager pluginMgr = null;
        if (pumd == null)
        {
            // Metadata not cached for persistence-unit, so try to read it
            pluginMgr = PluginManager.createPluginManager(deserialisationProps, this.getClass().getClassLoader());
            pumd = getPersistenceUnitMetaDataForName(name, pluginMgr, deserialisationProps);
        }
        initialise(pumd, deserialisationProps, pluginMgr);
        this.deserialisationProps = null;
        return this;
    }

    private static synchronized void assertSingleton(String name, JPAEntityManagerFactory emf)
    {
        Boolean singleton = emf.getNucleusContext().getConfiguration().getBooleanObjectProperty(JPAPropertyNames.PROPERTY_JPA_SINGLETON_EMF_FOR_NAME);
        if (singleton != null && singleton)
        {
            // Check on singleton pattern
            if (emfByName == null)
            {
                emfByName = new ConcurrentHashMap<String, JPAEntityManagerFactory>();
            }
            if (emfByName.containsKey(name))
            {
                JPAEntityManagerFactory singletonEMF = emfByName.get(name);
                emf.close();
                NucleusLogger.PERSISTENCE.warn("Requested EMF of name \"" + name + 
                    "\" but already exists and using singleton pattern, so returning existing EMF");
                throw new SingletonEMFException("Requested EMF that already exists", singletonEMF);
            }
            emfByName.putIfAbsent(name, emf);
        }
    }
}