/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.api.jpa.metamodel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MappedSuperclassType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Type;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.FileMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.PackageMetaData;

/**
 * Implementation of JPA2 Criteria "Metamodel".
 */
public class MetamodelImpl implements Metamodel
{
    ClassLoaderResolver clr;

    /** The entity types handled here. */
    Map<String, EntityType<?>> entityTypes = new HashMap<String, EntityType<?>>();

    /** The MappedSuperclass types handled here. */
    Map<String, MappedSuperclassTypeImpl<?>> mappedSuperTypes = new HashMap<String, MappedSuperclassTypeImpl<?>>();

    /** The Embeddable types handled here. */
    Map<String, EmbeddableType<?>> embeddedTypes = new HashMap<String, EmbeddableType<?>>();

    /** Basic types, can act as a cache. */
    Map<String, Type<?>> basicTypes = new HashMap<String, Type<?>>();

    public MetamodelImpl(MetaDataManager mmgr)
    {
        this.clr = mmgr.getNucleusContext().getClassLoaderResolver(null);

        // Load up known types
        FileMetaData[] filemds = mmgr.getFileMetaData();
        ClassLoaderResolver clr = getClassLoaderResolver();
        for (int i=0;i<filemds.length;i++)
        {
            for (int j=0;j<filemds[i].getNoOfPackages();j++)
            {
                PackageMetaData pmd = filemds[i].getPackage(j);
                for (int k=0;k<pmd.getNoOfClasses();k++)
                {
                    ClassMetaData cmd = pmd.getClass(k);
                    Class cls = clr.classForName(cmd.getFullClassName());
                    if (cmd.isEmbeddedOnly())
                    {
                        EmbeddableTypeImpl type = new EmbeddableTypeImpl(cls, cmd, this);
                        embeddedTypes.put(cmd.getFullClassName(), type);
                    }
                    else if (cmd.isMappedSuperclass())
                    {
                        MappedSuperclassTypeImpl type = new MappedSuperclassTypeImpl(cls, cmd, this);
                        mappedSuperTypes.put(cmd.getFullClassName(), type);
                    }
                    else
                    {
                        EntityTypeImpl type = new EntityTypeImpl(cls, cmd, this);
                        entityTypes.put(cmd.getFullClassName(), type);
                    }
                }
            }
        }

        for (int i=0;i<filemds.length;i++)
        {
            for (int j=0;j<filemds[i].getNoOfPackages();j++)
            {
                PackageMetaData pmd = filemds[i].getPackage(j);
                for (int k=0;k<pmd.getNoOfClasses();k++)
                {
                    ClassMetaData cmd = pmd.getClass(k);
                    ManagedType managedType = null;
                    if (cmd.isEmbeddedOnly())
                    {
                        managedType = embeddedTypes.get(cmd.getFullClassName());
                    }
                    else if (cmd.isMappedSuperclass())
                    {
                        managedType = mappedSuperTypes.get(cmd.getFullClassName());
                    }
                    else
                    {
                        managedType = entityTypes.get(cmd.getFullClassName());
                    }

                    // Initialise metamodel class
                    Class metaCls = null;
                    try
                    {
                        metaCls = clr.classForName(cmd.getFullClassName() + "_");
                        AbstractMemberMetaData[] mmds = cmd.getManagedMembers();
                        for (int l=0;l<mmds.length;l++)
                        {
                            try
                            {
                                Field metaField = metaCls.getField(mmds[l].getName());
                                if (mmds[l].getPersistenceModifier() == FieldPersistenceModifier.PERSISTENT)
                                {
                                    Attribute attr = managedType.getAttribute(mmds[l].getName());
                                    try
                                    {
                                        metaField.set(null, attr);
                                    }
                                    catch (Exception e)
                                    {
                                    }
                                }
                            }
                            catch (NoSuchFieldException nsfe)
                            {
                            }
                        }
                    }
                    catch (ClassNotResolvedException cnre)
                    {
                    }
                }
            }
        }
    }

    public ClassLoaderResolver getClassLoaderResolver()
    {
        return clr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Metamodel#embeddable(java.lang.Class)
     */
    public <X> EmbeddableType<X> embeddable(Class<X> cls)
    {
        EmbeddableType<X> type = (EmbeddableType<X>)embeddedTypes.get(cls.getName());
        if (type != null)
        {
            return type;
        }
        throw new IllegalArgumentException("Type " + cls.getName() + " is not a known embeddable type");
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Metamodel#getEmbeddables()
     */
    public Set<EmbeddableType<?>> getEmbeddables()
    {
        Set<EmbeddableType<?>> results = new HashSet<EmbeddableType<?>>();
        results.addAll(embeddedTypes.values());
        return results;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Metamodel#entity(java.lang.Class)
     */
    public <X> EntityType<X> entity(Class<X> cls)
    {
        EntityType<X> type = (EntityType<X>)entityTypes.get(cls.getName());
        if (type != null)
        {
            return type;
        }
        throw new IllegalArgumentException("Type " + cls.getName() + " is not a known entity type");
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Metamodel#getEntities()
     */
    public Set<EntityType<?>> getEntities()
    {
        Set<EntityType<?>> results = new HashSet<EntityType<?>>();
        results.addAll(entityTypes.values());
        return results;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Metamodel#managedType(java.lang.Class)
     */
    public <X> ManagedType<X> managedType(Class<X> cls)
    {
        ManagedType<X> type = (EntityType<X>)entityTypes.get(cls.getName());
        if (type != null)
        {
            return type;
        }
        type = (EmbeddableType<X>)embeddedTypes.get(cls.getName());
        if (type != null)
        {
            return type;
        }
        type = (MappedSuperclassType<X>)mappedSuperTypes.get(cls.getName());
        if (type != null)
        {
            return type;
        }

        throw new IllegalArgumentException("Type " + cls.getName() + " is not a known managed type");
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.Metamodel#getManagedTypes()
     */
    public Set<ManagedType<?>> getManagedTypes()
    {
        Set<ManagedType<?>> results = new HashSet<ManagedType<?>>();
        results.addAll(entityTypes.values());
        results.addAll(embeddedTypes.values());
        results.addAll(mappedSuperTypes.values());
        return results;
    }

    /**
     * Convenience method to look up the Type for a class.
     * @param cls The class
     * @param <X> The type
     * @return The Type for this class
     */
    public <X> Type<X> getType(Class<X> cls)
    {
        try
        {
            return managedType(cls);
        }
        catch (IllegalArgumentException ex)
        {
            // Not persistable type
            if (basicTypes.containsKey(cls.getName()))
            {
                return (Type<X>)basicTypes.get(cls.getName());
            }

            Type<X> basic = new TypeImpl<X>(cls);
            basicTypes.put(cls.getName(), basic);
            return basic;
        }
    }
}