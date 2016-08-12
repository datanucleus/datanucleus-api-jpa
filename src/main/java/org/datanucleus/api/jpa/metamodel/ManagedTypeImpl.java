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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;

/**
 * Implementation of JPA2 Metamodel "ManagedType".
 * Provides a wrapper to AbstractClassMetaData.
 */
public class ManagedTypeImpl<X> extends TypeImpl<X> implements ManagedType<X>
{
    /** DataNucleus metadata for this class. */
    protected AbstractClassMetaData cmd;

    /** The metamodel being used, where we need to look up related classes. */
    protected MetamodelImpl model;

    protected Map<String, Attribute<X, ?>> attributes = new HashMap<String, Attribute<X, ?>>();

    /**
     * Constructor for a managed type.
     * @param cls The class
     * @param cmd Metadata for the class
     * @param model The model being used
     */
    protected ManagedTypeImpl(Class<X> cls, AbstractClassMetaData cmd, MetamodelImpl model)
    {
        super(cls);
        this.model = model;
        this.cmd = cmd;

        AbstractMemberMetaData[] mmds = cmd.getManagedMembers();
        ClassLoaderResolver clr = model.getClassLoaderResolver();
        for (int i=0;i<mmds.length;i++)
        {
            RelationType relationType = mmds[i].getRelationType(clr);
            Attribute attr = null;
            if (relationType == RelationType.ONE_TO_MANY_UNI || relationType == RelationType.ONE_TO_MANY_BI)
            {
                if (mmds[i].hasCollection())
                {
                    if (List.class.isAssignableFrom(mmds[i].getType()))
                    {
                        attr = new ListAttributeImpl(mmds[i], this);
                    }
                    else if (Set.class.isAssignableFrom(mmds[i].getType()))
                    {
                        attr = new SetAttributeImpl(mmds[i], this);
                    }
                    else
                    {
                        attr = new CollectionAttributeImpl(mmds[i], this);
                    }
                }
                else if (mmds[i].hasMap())
                {
                    attr = new MapAttributeImpl(mmds[i], this);
                }
                else if (mmds[i].hasArray())
                {
                    // Include arrays as SingularAttributeImpl! since JPA doesn't provide an alternative
                    attr = new SingularAttributeImpl(mmds[i], this);
                }
            }
            else if (RelationType.isRelationSingleValued(relationType))
            {
                attr = new SingularAttributeImpl(mmds[i], this);
            }
            else if (relationType == RelationType.MANY_TO_MANY_BI)
            {
                attr = new CollectionAttributeImpl(mmds[i], this);
            }
            else
            {
                if (List.class.isAssignableFrom(mmds[i].getType()))
                {
                    attr = new ListAttributeImpl(mmds[i], this);
                }
                else if (Set.class.isAssignableFrom(mmds[i].getType()))
                {
                    attr = new SetAttributeImpl(mmds[i], this);
                }
                else if (Collection.class.isAssignableFrom(mmds[i].getType()))
                {
                    attr = new CollectionAttributeImpl(mmds[i], this);
                }
                else if (Map.class.isAssignableFrom(mmds[i].getType()))
                {
                    attr = new MapAttributeImpl(mmds[i], this);
                }
                else
                {
                    // Include arrays as SingularAttributeImpl! since JPA doesn't provide an alternative
                    attr = new SingularAttributeImpl(mmds[i], this);
                }
            }

            attributes.put(mmds[i].getName(), attr);
        }
    }

    /**
     * Convenience accessor for the metadata underlying this class.
     * @return The metadata
     */
    public AbstractClassMetaData getMetadata()
    {
        return cmd;
    }

    public MetamodelImpl getModel()
    {
        return model;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getAttribute(java.lang.String)
     */
    public Attribute<? super X, ?> getAttribute(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getAttribute(attr);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class " + cmd.getFullClassName());
        }
        return theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getAttributes()
     */
    public Set<Attribute<? super X, ?>> getAttributes()
    {
        Set<Attribute<? super X, ?>> set = new HashSet<Attribute<? super X,?>>();
        set.addAll(attributes.values());
        if (cmd.getSuperAbstractClassMetaData() != null)
        {
            // Relay to the supertype
            Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
            ManagedType supertype = model.managedType(supercls);
            Set<Attribute<? super X, ?>> superattrs = supertype.getAttributes();
            set.addAll(superattrs);
        }
        return set;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredAttribute(java.lang.String)
     */
    public Attribute<X, ?> getDeclaredAttribute(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredAttributes()
     */
    public Set<Attribute<X, ?>> getDeclaredAttributes()
    {
        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());

        Set<Attribute<X, ?>> attrs = new HashSet<Attribute<X,?>>();
        Iterator iter = attributes.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, Attribute<X, ?>> entry = (Entry<String, Attribute<X, ?>>) iter.next();
            AttributeImpl<X, ?> attr = (AttributeImpl<X, ?>) entry.getValue();
            AbstractMemberMetaData mmd = attr.getMetadata();

            if (mmd.getMemberRepresented().getDeclaringClass() == thisCls)
            {
                attrs.add(attr);
            }
        }

        return attrs;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getCollection(java.lang.String, java.lang.Class)
     */
    public <E> CollectionAttribute<? super X, E> getCollection(String attr, Class<E> elementType)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getCollection(attr, elementType);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Collection.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a collection");
        }
        Class elementCls = model.getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
        if (!elementType.isAssignableFrom(elementCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a collection with element of type " + elementType.getName());
        }
        return (CollectionAttribute<? super X, E>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getCollection(java.lang.String)
     */
    public CollectionAttribute<? super X, ?> getCollection(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getCollection(attr);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Collection.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a collection");
        }
        return (CollectionAttribute<X, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredCollection(java.lang.String, java.lang.Class)
     */
    public <E> CollectionAttribute<X, E> getDeclaredCollection(String attr, Class<E> elementType)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Collection.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a collection");
        }
        Class elementCls = model.getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
        if (!elementType.isAssignableFrom(elementCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a collection with element of type " + elementType.getName());
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (CollectionAttribute<X, E>) theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredCollection(java.lang.String)
     */
    public CollectionAttribute<X, ?> getDeclaredCollection(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Collection.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a collection");
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (CollectionAttribute<X, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getList(java.lang.String, java.lang.Class)
     */
    public <E> ListAttribute<? super X, E> getList(String attr, Class<E> elementType)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getList(attr, elementType);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!List.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a List");
        }
        Class elementCls = model.getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
        if (!elementType.isAssignableFrom(elementCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a List with element of type " + elementType.getName());
        }
        return (ListAttribute<? super X, E>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getList(java.lang.String)
     */
    public ListAttribute<? super X, ?> getList(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getList(attr);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!List.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a List");
        }
        return (ListAttribute<X, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredList(java.lang.String, java.lang.Class)
     */
    public <E> ListAttribute<X, E> getDeclaredList(String attr, Class<E> elementType)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!List.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a List");
        }
        Class elementCls = model.getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
        if (!elementType.isAssignableFrom(elementCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a List with element of type " + elementType.getName());
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (ListAttribute<X, E>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredList(java.lang.String)
     */
    public ListAttribute<X, ?> getDeclaredList(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!List.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a List");
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (ListAttribute<X, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getMap(java.lang.String, java.lang.Class, java.lang.Class)
     */
    public <K, V> MapAttribute<? super X, K, V> getMap(String attr, Class<K> keyType, Class<V> valueType)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getMap(attr, keyType, valueType);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Map.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a Map");
        }

        Class keyCls = model.getClassLoaderResolver().classForName(mmd.getMap().getKeyType());
        if (!keyType.isAssignableFrom(keyCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a Map with Key of type " + keyType.getName());
        }

        Class valueCls = model.getClassLoaderResolver().classForName(mmd.getMap().getValueType());
        if (!valueType.isAssignableFrom(valueCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a Map with Value of type " + valueType.getName());
        }

        return (MapAttribute<? super X, K, V>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getMap(java.lang.String)
     */
    public MapAttribute<? super X, ?, ?> getMap(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getMap(attr);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Map.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a Map");
        }
        return (MapAttribute<X, ?, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredMap(java.lang.String, java.lang.Class, java.lang.Class)
     */
    public <K, V> MapAttribute<X, K, V> getDeclaredMap(String attr, Class<K> keyType, Class<V> valueType)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Map.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a Map");
        }

        Class keyCls = model.getClassLoaderResolver().classForName(mmd.getMap().getKeyType());
        if (!keyType.isAssignableFrom(keyCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a Map with Key of type " + keyType.getName());
        }

        Class valueCls = model.getClassLoaderResolver().classForName(mmd.getMap().getValueType());
        if (!valueType.isAssignableFrom(valueCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a Map with Value of type " + valueType.getName());
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (MapAttribute<X, K, V>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredMap(java.lang.String)
     */
    public MapAttribute<X, ?, ?> getDeclaredMap(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Map.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a Map");
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (MapAttribute<X, ?, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getSet(java.lang.String, java.lang.Class)
     */
    public <E> SetAttribute<? super X, E> getSet(String attr, Class<E> elementType)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getSet(attr, elementType);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Set.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a Set");
        }
        Class elementCls = model.getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
        if (!elementType.isAssignableFrom(elementCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a Set with element of type " + elementType.getName());
        }
        return (SetAttribute<? super X, E>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getSet(java.lang.String)
     */
    public SetAttribute<? super X, ?> getSet(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getSet(attr);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Set.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a Set");
        }
        return (SetAttribute<X, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredSet(java.lang.String, java.lang.Class)
     */
    public <E> SetAttribute<X, E> getDeclaredSet(String attr, Class<E> elementType)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Set.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a Set");
        }
        Class elementCls = model.getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
        if (!elementType.isAssignableFrom(elementCls))
        {
            throw new IllegalArgumentException("Attribute " + attr + " doesn't have a Set with element of type " + elementType.getName());
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (SetAttribute<X, E>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredSet(java.lang.String)
     */
    public SetAttribute<X, ?> getDeclaredSet(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!Set.class.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt a Set");
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (SetAttribute<X, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getSingularAttribute(java.lang.String, java.lang.Class)
     */
    public <Y> SingularAttribute<? super X, Y> getSingularAttribute(String attr, Class<Y> type)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getSingularAttribute(attr, type);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!type.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt of type " + type.getName());
        }
        return (SingularAttribute<X, Y>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getSingularAttribute(java.lang.String)
     */
    public SingularAttribute<? super X, ?> getSingularAttribute(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            if (cmd.getSuperAbstractClassMetaData() != null)
            {
                // Relay to the supertype
                Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
                ManagedType supertype = model.managedType(supercls);
                return supertype.getSingularAttribute(attr);
            }
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        return (SingularAttribute<X, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getSingularAttributes()
     */
    public Set<SingularAttribute<? super X, ?>> getSingularAttributes()
    {
        Set<SingularAttribute<? super X, ?>> attrs = new HashSet<SingularAttribute<? super X,?>>();
        Iterator iter = attributes.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, Attribute<X, ?>> entry = (Entry<String, Attribute<X, ?>>) iter.next();
            AttributeImpl<X, ?> attr = (AttributeImpl<X, ?>) entry.getValue();
            if (attr instanceof SingularAttribute)
            {
                attrs.add((SingularAttribute<? super X, ?>) attr);
            }
        }
        if (cmd.getSuperAbstractClassMetaData() != null)
        {
            // Relay to the supertype
            Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
            ManagedType supertype = model.managedType(supercls);
            Set superattrs = supertype.getSingularAttributes();
            attrs.addAll(superattrs);
        }

        return attrs;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredSingularAttribute(java.lang.String, java.lang.Class)
     */
    public <Y> SingularAttribute<X, Y> getDeclaredSingularAttribute(String attr, Class<Y> type)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();
        if (!type.isAssignableFrom(mmd.getType()))
        {
            throw new IllegalArgumentException("Attribute " + attr + " isnt of type " + type.getName());
        }

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (SingularAttribute<X, Y>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredSingularAttribute(java.lang.String)
     */
    public SingularAttribute<X, ?> getDeclaredSingularAttribute(String attr)
    {
        AttributeImpl<X, ?> theAttr = (AttributeImpl<X, ?>) attributes.get(attr);
        if (theAttr == null)
        {
            throw new IllegalArgumentException("Attribute " + attr + " was not found in class");
        }
        AbstractMemberMetaData mmd = theAttr.getMetadata();

        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        if (mmd.getMemberRepresented().getDeclaringClass() != thisCls)
        {
            throw new IllegalArgumentException("Attribute " + attr + " isn't defined in " + cmd.getFullClassName());
        }

        return (SingularAttribute<X, ?>)theAttr;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredSingularAttributes()
     */
    public Set<SingularAttribute<X, ?>> getDeclaredSingularAttributes()
    {
        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        Set<SingularAttribute<X, ?>> attrs = new HashSet<SingularAttribute<X,?>>();
        Iterator iter = attributes.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, Attribute<X, ?>> entry = (Entry<String, Attribute<X, ?>>) iter.next();
            AttributeImpl<X, ?> attr = (AttributeImpl<X, ?>) entry.getValue();
            if (attr instanceof SingularAttribute)
            {
                AbstractMemberMetaData mmd = attr.getMetadata();
                if (mmd.getMemberRepresented().getDeclaringClass() == thisCls)
                {
                    attrs.add((SingularAttribute<X, ?>) attr);
                }
            }
        }

        return attrs;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getPluralAttributes()
     */
    public Set<PluralAttribute<? super X, ?, ?>> getPluralAttributes()
    {
        Set<PluralAttribute<? super X, ?, ?>> attrs = new HashSet<PluralAttribute<? super X, ?, ?>>();
        Iterator iter = attributes.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, Attribute<X, ?>> entry = (Entry<String, Attribute<X, ?>>) iter.next();
            AttributeImpl<X, ?> attr = (AttributeImpl<X, ?>) entry.getValue();
            if (attr instanceof PluralAttribute)
            {
                attrs.add((PluralAttribute<X, ?, ?>) attr);
            }
        }
        if (cmd.getSuperAbstractClassMetaData() != null)
        {
            // Relay to the supertype
            Class supercls = model.getClassLoaderResolver().classForName(cmd.getSuperAbstractClassMetaData().getFullClassName());
            ManagedType supertype = model.managedType(supercls);
            Set superattrs = supertype.getPluralAttributes();
            attrs.addAll(superattrs);
        }

        return attrs;
    }

    /* (non-Javadoc)
     * @see javax.persistence.metamodel.ManagedType#getDeclaredPluralAttributes()
     */
    public Set<PluralAttribute<X, ?, ?>> getDeclaredPluralAttributes()
    {
        Class thisCls = model.getClassLoaderResolver().classForName(cmd.getFullClassName());
        Set<PluralAttribute<X, ?, ?>> attrs = new HashSet<PluralAttribute<X, ?, ?>>();
        Iterator iter = attributes.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry<String, Attribute<X, ?>> entry = (Entry<String, Attribute<X, ?>>) iter.next();
            AttributeImpl<X, ?> attr = (AttributeImpl<X, ?>) entry.getValue();
            if (attr instanceof PluralAttribute)
            {
                AbstractMemberMetaData mmd = attr.getMetadata();
                if (mmd.getMemberRepresented().getDeclaringClass() == thisCls)
                {
                    attrs.add((PluralAttribute<X, ?, ?>) attr);
                }
            }
        }

        return attrs;
    }
}