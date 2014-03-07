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
2006-2012 Andy Jefferson - javadocs, JPA2.0, JPA2.1
    ...
**********************************************************************/
package org.datanucleus.api.jpa;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.persistence.spi.ProviderUtil;

import org.datanucleus.ClassLoaderResolverImpl;
import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.jpa.exceptions.NoPersistenceUnitException;
import org.datanucleus.api.jpa.exceptions.NoPersistenceXmlException;
import org.datanucleus.api.jpa.exceptions.NotProviderException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.PersistenceFileMetaData;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.datanucleus.metadata.TransactionType;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.schema.SchemaTool;
import org.datanucleus.store.schema.SchemaTool.Mode;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;

/**
 * PersistenceProvider implementation.
 * Provides a means of creating EntityManagerFactory objects.
 */
public class PersistenceProviderImpl implements PersistenceProvider, ProviderUtil
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.api.jpa.Localisation",
        JPAEntityManagerFactory.class.getClassLoader());

    /**
     * Constructor.
     */
    public PersistenceProviderImpl()
    {
    }

    /**
     * Method to create an EntityManagerFactory when running in JavaEE.
     * The container will have parsed the persistence.xml files to provide this PersistenceUnitInfo.
     * @param unitInfo The "persistence-unit"
     * @param properties EntityManagerFactory properties to override those in the persistence unit
     * @return The EntityManagerFactory
     */
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo unitInfo, Map properties)
    {
        try
        {
            return new JPAEntityManagerFactory(unitInfo, properties);
        }
        catch (SingletonEMFException se)
        {
            return se.getSingleton();
        }
        catch (NotProviderException npe)
        {
            return null;
        }
        catch (NoPersistenceUnitException npue)
        {
            return null;
        }
        catch (NoPersistenceXmlException npxe)
        {
            return null;
        }
    }

    /**
     * Method to create an EntityManagerFactory when running in JavaSE.
     * @param unitName Name of the "persistence-unit"
     * @param properties EntityManagerFactory properties to override those in the persistence unit
     * @return The EntityManagerFactory
     */
    public EntityManagerFactory createEntityManagerFactory(String unitName, Map properties)
    {
        try
        {
            return new JPAEntityManagerFactory(unitName, properties);
        }
        catch (SingletonEMFException se)
        {
            return se.getSingleton();
        }
        catch (NotProviderException npe)
        {
            return null;
        }
        catch (NoPersistenceUnitException npue)
        {
            return null;
        }
        catch (NoPersistenceXmlException npxe)
        {
            return null;
        }
    }

    /**
     * If the provider determines that the entity has been provided
     * by itself and that the state of the specified attribute has
     * been loaded, this method returns LoadState.LOADED.
     * If the provider determines that the entity has been provided
     * by itself and that either entity attributes with FetchType
     * EAGER have not been loaded or that the state of the specified
     * attribute has not been loaded, this methods returns
     * LoadState.NOT_LOADED.
     * If a provider cannot determine the load state, this method
     * returns LoadState.UNKNOWN.
     * The provider's implementation of this method must not obtain
     * a reference to an attribute value, as this could trigger the
     * loading of entity state if the entity has been provided by a
     * different provider.
     * @param entity The entity
     * @param memberName Name of member whose load status is to be determined
     * @return load status of the attribute
     */
    public LoadState isLoadedWithoutReference(Object entity, String memberName)
    {
        if (entity == null)
        {
            return LoadState.UNKNOWN;
        }

        JPAAdapter adapter = new JPAAdapter();
        ExecutionContext ec = adapter.getExecutionContext(entity);
        if (ec == null)
        {
            try
            {
                // Try to access the enhanced "jdoGet" method
                Method getterMethod = ClassUtils.getMethodWithArgument(entity.getClass(), 
                    "jdoGet" + memberName, entity.getClass());
                getterMethod.invoke(null, new Object[] {entity});
                return LoadState.LOADED;
            }
            catch (IllegalAccessException iae)
            {
                return LoadState.NOT_LOADED;
            }
            catch (Exception e)
            {
            }
            return LoadState.UNKNOWN;
        }
        ObjectProvider op = ec.findObjectProvider(entity);
        if (op == null)
        {
            return LoadState.UNKNOWN;
        }
        else
        {
            String[] loadedFields = op.getLoadedFieldNames();
            if (loadedFields != null)
            {
                for (int i=0;i<loadedFields.length;i++)
                {
                    if (loadedFields[i].equals(memberName))
                    {
                        return LoadState.LOADED;
                    }
                }
            }
            return LoadState.NOT_LOADED;
        }
    }

    /**
     * If the provider determines that the entity has been provided
     * by itself and that the state of the specified attribute has
     * been loaded, this method returns LoadState.LOADED.
     * If a provider determines that the entity has been provided
     * by itself and that either the entity attributes with FetchType
     * EAGER have not been loaded or that the state of the specified
     * attribute has not been loaded, this method returns
     * return LoadState.NOT_LOADED.
     * If the provider cannot determine the load state, this method
     * returns LoadState.UNKNOWN.
     * The provider's implementation of this method is permitted to
     * obtain a reference to the attribute value. (This access is
     * safe because providers which might trigger the loading of the
     * attribute state will have already been determined by
     * isLoadedWithoutReference.)
     * @param entity The entity
     * @param memberName name of member whose load status is to be determined
     * @return load status of the member
     */
    public LoadState isLoadedWithReference(Object entity, String memberName)
    {
        // Just make use of other implementation
        return isLoadedWithoutReference(entity, memberName);
    }

    /**
     * If the provider determines that the entity has been provided
     * by itself and that the state of all attributes for which
     * FetchType EAGER has been specified have been loaded, this
     * method returns LoadState.LOADED.
     * If the provider determines that the entity has been provided
     * by itself and that not all attributes with FetchType EAGER
     * have been loaded, this method returns LoadState.NOT_LOADED.
     * If the provider cannot determine if the entity has been
     * provided by itself, this method returns LoadState.UNKNOWN.
     * The provider's implementation of this method must not obtain
     * a reference to any attribute value, as this could trigger the
     * loading of entity state if the entity has been provided by a different provider.
     * @param entity whose loaded status is to be determined
     * @return load status of the entity
     */
    public LoadState isLoaded(Object entity)
    {
        if (entity == null)
        {
            return LoadState.UNKNOWN;
        }

        JPAAdapter adapter = new JPAAdapter();
        ExecutionContext ec = adapter.getExecutionContext(entity);
        if (ec == null)
        {
            // TODO Handle detached entities
            return LoadState.UNKNOWN;
        }

        ObjectProvider op = ec.findObjectProvider(entity);
        if (op == null)
        {
            return LoadState.UNKNOWN;
        }
        else
        {
            boolean allLoaded = true;
            AbstractClassMetaData cmd =
                ec.getMetaDataManager().getMetaDataForClass(entity.getClass(), ec.getClassLoaderResolver());
            int[] dfgFieldNumbers = cmd.getDFGMemberPositions();
            for (int i=0;i<dfgFieldNumbers.length;i++)
            {
                AbstractMemberMetaData mmd= cmd.getMetaDataForManagedMemberAtAbsolutePosition(dfgFieldNumbers[i]);
                String[] loadedFields = op.getLoadedFieldNames();

                boolean memberLoaded = false;
                if (loadedFields != null)
                {
                    for (int j=0;j<loadedFields.length;j++)
                    {
                        if (loadedFields[j].equals(mmd.getName()))
                        {
                            memberLoaded = true;
                            break;
                        }
                    }
                }
                if (!memberLoaded)
                {
                    allLoaded = false;
                    break;
                }
            }
            return (allLoaded ? LoadState.LOADED : LoadState.NOT_LOADED);
        }
    }

    /* (non-Javadoc)
     * @see javax.persistence.spi.PersistenceProvider#getProviderUtil()
     */
    public ProviderUtil getProviderUtil()
    {
        return this;
    }

    /**
     * Create database schemas and/or tables and/or create DDL scripts as determined by the supplied properties.
     * Called by the container when schema generation is to occur as a separate phase from creation 
     * of the entity manager factory.
     * @param unitInfo metadata for use by the persistence provider
     * @param overridingProps properties for schema generation; these may also include provider-specific properties
     * @throws PersistenceException if insufficient or inconsistent configuration information is provided 
     *     or if schema generation otherwise fails.
     * @since JPA2.1
     */
    public void generateSchema(PersistenceUnitInfo unitInfo, Map overridingProps)
    {
        // TODO Throw exception if this is for a different provider
        // Create a PersistenceUnitMetaData
        PersistenceUnitMetaData pumd = null;
        URI rootURI = null;
        try
        {
            rootURI = unitInfo.getPersistenceUnitRootUrl().toURI();
        }
        catch (URISyntaxException e1)
        {
        }

        if (unitInfo.getTransactionType() == PersistenceUnitTransactionType.JTA)
        {
            pumd = new PersistenceUnitMetaData(unitInfo.getPersistenceUnitName(),
                TransactionType.JTA.toString(), rootURI);
        }
        else
        {
            pumd = new PersistenceUnitMetaData(unitInfo.getPersistenceUnitName(),
                TransactionType.RESOURCE_LOCAL.toString(), rootURI);
        }
        
        // Classes
        List<String> classNames = unitInfo.getManagedClassNames();
        Iterator<String> classIter = classNames.iterator();
        while (classIter.hasNext())
        {
            pumd.addClassName(classIter.next());
        }

        // Mapping files
        List<String> mappingFileNames = unitInfo.getMappingFileNames();
        Iterator<String> mappingFileIter = mappingFileNames.iterator();
        while (mappingFileIter.hasNext())
        {
            pumd.addMappingFile(mappingFileIter.next());
        }

        // Jars
        List<URL> jarUrls = unitInfo.getJarFileUrls();
        Iterator<URL> jarUrlIter = jarUrls.iterator();
        while (jarUrlIter.hasNext())
        {
            pumd.addJarFile(jarUrlIter.next());
        }

        // Properties
        Properties unitProps = unitInfo.getProperties();
        if (unitProps != null)
        {
            for (Enumeration e = unitProps.propertyNames(); e.hasMoreElements();)
            {
                String prop = (String) e.nextElement();
                pumd.addProperty(prop, unitProps.getProperty(prop));
            }
        }

        // Exclude unlisted classes
        if (unitInfo.excludeUnlistedClasses())
        {
            pumd.setExcludeUnlistedClasses();
        }

        // Provider
        pumd.setProvider(unitInfo.getPersistenceProviderClassName());

        // Generate the schema
        generateSchemaForPersistentUnit(pumd, overridingProps);
    }

    /**
     * Create database schemas and/or tables and/or create DDL scripts as determined by the supplied properties.
     * Called by the container when schema generation is to occur as a separate phase from creation 
     * of the entity manager factory.
     * @param unitName Name of the persistence unit
     * @param overridingProps properties for schema generation; these may also include provider-specific properties
     * @throws PersistenceException if insufficient or inconsistent configuration information is provided 
     *     or if schema generation otherwise fails.
     * @since JPA2.1
     */
    public boolean generateSchema(String unitName, Map overridingProps)
    {
        // Find all "META-INF/persistence.xml" files in the current thread loader CLASSPATH and parse them
        PluginManager pluginMgr = PluginManager.createPluginManager(overridingProps, this.getClass().getClassLoader());
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
            throw new PersistenceException(LOCALISER.msg("EMF.NoPersistenceXML"));
        }

        // Extract the persistence unit we require
        PersistenceUnitMetaData pumd = null;
        for (int i=0;i<files.length;i++)
        {
            PersistenceUnitMetaData[] unitmds = files[i].getPersistenceUnits();
            for (int j=0;j<unitmds.length;j++)
            {
                // Cache the "persistence-unit" for future reference
                if (unitmds[j].getName().equals(unitName))
                {
                    pumd = unitmds[j];
                    pumd.clearJarFiles(); // Jar files not applicable to J2SE [JPA 6.3]
                    break;
                }
            }
        }

        if (pumd == null)
        {
            throw new PersistenceException("Persistence unit " + unitName + " not found!");
        }
        // TODO Throw exception if this is for a different provider

        // Generate the schema
        generateSchemaForPersistentUnit(pumd, overridingProps);

        return true;
    }

    protected void generateSchemaForPersistentUnit(PersistenceUnitMetaData pumd, Map overridingProps)
    {
        // Generate schema in database if specified, otherwise as scripts, otherwise nothing
        String modeStr = (String) overridingProps.get(JPAPropertyNames.PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_DATABASE_ACTION);
        if (modeStr == null || modeStr.equalsIgnoreCase("none"))
        {
            // Database not needed, so check if scripts needed
            modeStr = (String) overridingProps.get(JPAPropertyNames.PROPERTY_JPA_STANDARD_GENERATE_SCHEMA_SCRIPTS_ACTION);
        }
        Mode mode = null;
        if (modeStr != null)
        {
            if (modeStr.equalsIgnoreCase("create"))
            {
                mode = Mode.CREATE;
            }
            else if (modeStr.equalsIgnoreCase("drop"))
            {
                mode = Mode.DELETE;
            }
            else if (modeStr.equalsIgnoreCase("drop-and-create"))
            {
                mode = Mode.DELETE_CREATE;
            }
        }
        if (mode == null)
        {
            return;
        }

        // Create NucleusContext, and initialise it (which triggers generate schema)
        SchemaTool.getNucleusContextForMode(mode, "JPA", overridingProps, pumd.getName(), null, true);
    }
}