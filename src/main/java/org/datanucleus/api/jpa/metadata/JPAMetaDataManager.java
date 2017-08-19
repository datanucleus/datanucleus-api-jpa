/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.api.jpa.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.api.jpa.JPAEntityGraph;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.DiscriminatorMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.metadata.EventListenerMetaData;
import org.datanucleus.metadata.FileMetaData;
import org.datanucleus.metadata.MetaDataManagerImpl;
import org.datanucleus.metadata.MetadataFileType;
import org.datanucleus.metadata.PackageMetaData;
import org.datanucleus.metadata.xml.MetaDataParser;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager of JPA MetaData information in DataNucleus.
 * Manages the MetaData for a particular "persistence-unit".
 */
public class JPAMetaDataManager extends MetaDataManagerImpl
{
    private static final long serialVersionUID = 3591790314245592821L;

    /** Parser for XML MetaData. */
    protected MetaDataParser metaDataParser = null;

    /** EventListeners. Use a list to preserve ordering. */
    protected List eventListeners = new ArrayList();

    /** Listeners for notification of when an EntityGraph is registered. */
    protected List<JPAEntityGraphRegistrationListener> entityGraphListeners = new ArrayList<JPAEntityGraphRegistrationListener>();

    /**
     * Constructor.
     * @param ctxt NucleusContext that this metadata manager operates in
     */
    public JPAMetaDataManager(NucleusContext ctxt)
    {
        super(ctxt);

        // log the configuration
        if (NucleusLogger.METADATA.isDebugEnabled())
        {
            if (allowXML && allowAnnotations)
            {
                NucleusLogger.METADATA.debug("MetaDataManager : Input=(XML,Annotations), XML-Validation=" + validateXML);
            }
            else if (allowXML && !allowAnnotations)
            {
                NucleusLogger.METADATA.debug("MetaDataManager : Input=(XML), XML-Validation=" + validateXML);
            }
            else if (!allowXML && allowAnnotations)
            {
                NucleusLogger.METADATA.debug("MetaDataManager : Input=(Annotations)");
            }
            else
            {
                NucleusLogger.METADATA.debug("MetaDataManager : Input=(NONE)");
            }
        }
    }

    public synchronized void registerEntityGraphListener(JPAEntityGraphRegistrationListener listener)
    {
        entityGraphListeners.add(listener);
    }

    public synchronized void registerEntityGraph(JPAEntityGraph eg)
    {
        for (JPAEntityGraphRegistrationListener listener : entityGraphListeners)
        {
            listener.entityGraphRegistered(eg);
        }
    }

    /**
     * Get the event listeners
     * @return the event listeners
     */
    public List getEventListeners()
    {
        return eventListeners;
    }

    /**
     * Utility to parse a file, using the "jpa" MetaData handler.
     * @param fileURL URL of the file
     * @return The FileMetaData for this file
     */
    protected FileMetaData parseFile(URL fileURL)
    {
        if (metaDataParser == null)
        {
            metaDataParser = new MetaDataParser(this, nucleusContext.getPluginManager(), validateXML, supportXMLNamespaces);
        }
        return (FileMetaData)metaDataParser.parseMetaDataURL(fileURL, "jpa");
    }

    /**
     * Method that will perform any necessary post-processing on metadata.
     * In the case of JPA we need to populate all event listener methods against the listener.
     * @param cmd Metadata for the class
     * @param clr ClassLoader resolver
     */
    protected void postProcessClassMetaData(AbstractClassMetaData cmd, ClassLoaderResolver clr)
    {
        if (cmd.getListeners() != null)
        {
            List<EventListenerMetaData> classListeners = cmd.getListeners();

            for (EventListenerMetaData elmd : classListeners)
            {
                // Load up all listener methods of the listener
                // If the metadata had defined some methods then the annotated method definition will be ignored
                populateListenerMethodsForEventListener(elmd, clr);
            }
        }
    }

    /**
     * Method to take the FileMetaData and register the relevant parts of it with the various convenience collections/maps that we use for access.
     * @param fileURLString URL of the metadata file
     * @param filemd The File MetaData
     */
    public void registerFile(String fileURLString, FileMetaData filemd, ClassLoaderResolver clr)
    {
        if (fileURLString == null)
        {
            // Null file
            return;
        }
        if (fileMetaDataByURLString.get(fileURLString) != null)
        {
            // Already registered!
            return;
        }

        fileMetaDataByURLString.put(fileURLString, filemd);

        registerQueriesForFile(filemd);
        registerStoredProcQueriesForFile(filemd);
        registerSequencesForFile(filemd);
        registerTableGeneratorsForFile(filemd);
        registerQueryResultMetaDataForFile(filemd);

        if (filemd.getListeners() != null)
        {
            List<EventListenerMetaData> fileListeners = filemd.getListeners();
            eventListeners.addAll(fileListeners);
            for (EventListenerMetaData elmd : fileListeners)
            {
                // Load up all listener methods of the listener
                // If the metadata had defined some methods then the annotated method definition will be ignored
                populateListenerMethodsForEventListener(elmd, clr);
            }
        }

        // Register the classes and interfaces for later use
        if (filemd.getType() == MetadataFileType.JPA_MAPPING_FILE || filemd.getType() == MetadataFileType.ANNOTATIONS)
        {
            for (int i = 0; i < filemd.getNoOfPackages(); i++)
            {
                PackageMetaData pmd = filemd.getPackage(i);

                // Register all classes into the respective lookup maps
                for (int j = 0; j < pmd.getNoOfClasses(); j++)
                {
                    ClassMetaData cmd = pmd.getClass(j);
                    if (classesWithoutPersistenceInfo.contains(cmd.getFullClassName()))
                    {
                        // Remove from unknown classes now that we have some metadata
                        classesWithoutPersistenceInfo.remove(cmd.getFullClassName());
                    }

                    // Register the metadata under the entity name
                    if (cmd.getEntityName() == null)
                    {
                        // No entity name provided so set to the basic class name
                        cmd.setEntityName(cmd.getName());
                    }
                    classMetaDataByEntityName.put(cmd.getEntityName(), cmd);

                    if (cmd.getInheritanceMetaData() != null)
                    {
                        // Register the metadata under the discriminator name
                        DiscriminatorMetaData dismd = cmd.getInheritanceMetaData().getDiscriminatorMetaData();
                        if (dismd != null)
                        {
                            if (dismd.getStrategy() == DiscriminatorStrategy.CLASS_NAME)
                            {
                                classMetaDataByDiscriminatorName.put(cmd.getFullClassName(), cmd);
                            }
                            else if (dismd.getStrategy() == DiscriminatorStrategy.ENTITY_NAME)
                            {
                                classMetaDataByDiscriminatorName.put(cmd.getEntityName(), cmd);
                            }
                            else if (dismd.getStrategy() == DiscriminatorStrategy.VALUE_MAP && dismd.getValue() != null)
                            {
                                classMetaDataByDiscriminatorName.put(dismd.getValue(), cmd);
                            }
                        }
                    }
                    registerMetaDataForClass(cmd.getFullClassName(), cmd);

                    postProcessClassMetaData(cmd, clr);
                }
            }
        }
    }

    /**
     * Method to populate the methods of the listener class into the EventListenerMetaData.
     * Checks the annotations of the listener class itself and adds them in to the definition that
     * the EventListener uses.
     * @param elmd EventListenerMetaData (updated by this method)
     * @param clr ClassLoader resolver
     */
    private void populateListenerMethodsForEventListener(EventListenerMetaData elmd, ClassLoaderResolver clr)
    {
        Class listenerClass = clr.classForName(elmd.getClassName());
        populateListenerMethodsForClassInEventListener(elmd, listenerClass, clr);
    }

    /**
     * Convenience method to process the specified class for listener callback methods adding them to
     * the EventListener. Proceeds up to the superclass if there is one.
     * @param elmd EventListenerMetaData to add the methods to.
     * @param cls The class to process for callback-annotated methods
     * @param clr ClassLoader resolver
     */
    private void populateListenerMethodsForClassInEventListener(EventListenerMetaData elmd, Class cls, ClassLoaderResolver clr)
    {
        Method[] methods = cls.getDeclaredMethods();
        if (methods != null)
        {
            for (Method method : methods)
            {
                Annotation[] methodAnnots = method.getAnnotations();
                if (methodAnnots != null)
                {
                    for (Annotation methodAnnot : methodAnnots)
                    {
                        if (methodAnnot.annotationType() == PrePersist.class ||
                            methodAnnot.annotationType() == PostPersist.class ||
                            methodAnnot.annotationType() == PreRemove.class ||
                            methodAnnot.annotationType() == PostRemove.class ||
                            methodAnnot.annotationType() == PreUpdate.class ||
                            methodAnnot.annotationType() == PostUpdate.class ||
                            methodAnnot.annotationType() == PostLoad.class)
                        {
                            elmd.addCallback(methodAnnot.annotationType().getName(), method.getDeclaringClass().getName(), method.getName());
                        }
                    }
                }
            }
        }

        // Go up to superclass if there is one
        if (cls.getSuperclass() == null || cls.getSuperclass() == Object.class)
        {
            return;
        }
        populateListenerMethodsForClassInEventListener(elmd, cls.getSuperclass(), clr);
    }

    /**
     * Load the metadata for the specified class (if available).
     * With JPA if a class hasn't been loaded at startup (from the persistence-unit) then we only check for annotations in the class itself.
     * @param c The class
     * @param clr ClassLoader resolver
     * @return The metadata for this class (if found)
     */
    protected AbstractClassMetaData loadMetaDataForClass(Class c, ClassLoaderResolver clr)
    {
        if (!allowMetaDataLoad)
        {
            // Not allowing further metadata load so just return
            return null;
        }

        try
        {
            // TODO What if a different thread starts loading this class just before we do? it will load then we do too
            updateLock.lock();

            if (allowAnnotations)
            {
                // Check for annotations
                FileMetaData annFilemd = loadAnnotationsForClass(c, clr, true, true);
                if (annFilemd != null)
                {
                    // Annotations present so use that
                    return annFilemd.getPackage(0).getClass(0);
                }
            }

            // Not found, so add to known classes/interfaces without MetaData
            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044043", c.getName())); 
            }
            classesWithoutPersistenceInfo.add(c.getName());

            return null;
        }
        finally
        {
            updateLock.unlock();
        }
    }
}