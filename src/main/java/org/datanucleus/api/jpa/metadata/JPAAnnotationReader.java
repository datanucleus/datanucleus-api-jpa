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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AccessType;
import javax.persistence.AssociationOverride;
import javax.persistence.AttributeConverter;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstraintMode;
import javax.persistence.ConstructorResult;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorType;
import javax.persistence.EntityResult;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.FieldResult;
import javax.persistence.ForeignKey;
import javax.persistence.GenerationType;
import javax.persistence.Index;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.NamedSubgraph;
import javax.persistence.ParameterMode;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.QueryHint;
import javax.persistence.SecondaryTable;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.jpa.JPAEntityGraph;
import org.datanucleus.api.jpa.JPASubgraph;
import org.datanucleus.api.jpa.annotations.Extension;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractElementMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ArrayMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.CollectionMetaData;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.ContainerMetaData;
import org.datanucleus.metadata.DiscriminatorMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.metadata.ElementMetaData;
import org.datanucleus.metadata.EmbeddedMetaData;
import org.datanucleus.metadata.EventListenerMetaData;
import org.datanucleus.metadata.FieldMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.ForeignKeyMetaData;
import org.datanucleus.metadata.DatastoreIdentityMetaData;
import org.datanucleus.metadata.ValueGenerationStrategy;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.IndexMetaData;
import org.datanucleus.metadata.IndexedValue;
import org.datanucleus.metadata.InheritanceMetaData;
import org.datanucleus.metadata.InheritanceStrategy;
import org.datanucleus.metadata.InvalidClassMetaDataException;
import org.datanucleus.metadata.JoinMetaData;
import org.datanucleus.metadata.KeyMetaData;
import org.datanucleus.metadata.MapMetaData;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.NullValue;
import org.datanucleus.metadata.OrderMetaData;
import org.datanucleus.metadata.PackageMetaData;
import org.datanucleus.metadata.PrimaryKeyMetaData;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.metadata.QueryLanguage;
import org.datanucleus.metadata.QueryMetaData;
import org.datanucleus.metadata.QueryResultMetaData;
import org.datanucleus.metadata.QueryResultMetaData.ConstructorTypeColumn;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.metadata.StoredProcQueryMetaData;
import org.datanucleus.metadata.StoredProcQueryParameterMetaData;
import org.datanucleus.metadata.StoredProcQueryParameterMode;
import org.datanucleus.metadata.TableGeneratorMetaData;
import org.datanucleus.metadata.UniqueMetaData;
import org.datanucleus.metadata.ValueMetaData;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.metadata.VersionStrategy;
import org.datanucleus.metadata.annotations.AbstractAnnotationReader;
import org.datanucleus.metadata.annotations.AnnotationObject;
import org.datanucleus.metadata.annotations.Member;
import org.datanucleus.store.types.ContainerHandler;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Implementation for Annotation Reader for Java annotations using JPA's definition.
 * This reader also accepts certain DataNucleus extensions where the JPA annotations don't provide 
 * full definition of the data required.
 */
public class JPAAnnotationReader extends AbstractAnnotationReader
{
    ClassLoaderResolver clr = null;

    /**
     * Constructor.
     * @param mgr MetaData manager
     */
    public JPAAnnotationReader(MetaDataManager mgr)
    {
        super(mgr);

        // We support "JPA" and "DataNucleus JPA extension" annotations in this reader
        setSupportedAnnotationPackages(new String[] {"javax.persistence", "org.datanucleus.api.jpa.annotations"});
    }

    /**
     * Method to process the "class" level annotations and create the outline ClassMetaData object
     * @param pmd Parent PackageMetaData
     * @param cls The class
     * @param annotations Annotations for this class
     * @param clr ClassLoader resolver
     * @return The ClassMetaData (or null if no annotations)
     */
    protected AbstractClassMetaData processClassAnnotations(PackageMetaData pmd, Class cls, AnnotationObject[] annotations, ClassLoaderResolver clr)
    {
        if (annotations == null || annotations.length == 0)
        {
            return null;
        }

        this.clr = clr;
        ClassMetaData cmd = null;

        AnnotationObject persistableAnnotation = isClassPersistable(annotations);
        if (persistableAnnotation != null)
        {
            cmd = pmd.newClassMetadata(ClassUtils.getClassNameForClass(cls));
            cmd.setPersistenceModifier(ClassPersistenceModifier.PERSISTENCE_CAPABLE);

            // Set entity name
            String entityName = ClassUtils.getClassNameForClass(cls);
            Map<String, Object> annotationValues = persistableAnnotation.getNameValueMap();
            if (persistableAnnotation.getName().equals(JPAAnnotationUtils.ENTITY))
            {
                String entName = (String) annotationValues.get("name");
                if (!StringUtils.isWhitespace(entName))
                {
                    entityName = entName;
                }
            }
            cmd.setEntityName(entityName);
        }
        else if (isClassPersistenceAware(annotations))
        {
            cmd = pmd.newClassMetadata(ClassUtils.getClassNameForClass(cls));
            cmd.setPersistenceModifier(ClassPersistenceModifier.PERSISTENCE_AWARE);
        }
        else if (doesClassHaveNamedQueries(annotations))
        {
            cmd = pmd.newClassMetadata(ClassUtils.getClassNameForClass(cls));
            cmd.setPersistenceModifier(ClassPersistenceModifier.NON_PERSISTENT);
        }
        else if (doesClassHaveConverter(cls, annotations))
        {
            // Converter has now been processed so just return
            return null;
        }
        else
        {
            // Not involved in the persistence process
            return null;
        }

        // Cater for named queries being specified on a persistence aware, or other class
        processNamedQueries(cmd, annotations);

        if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE)
        {
            // Class is either not persistent, or just has named queries
            return cmd;
        }

        cmd.setDetachable(true);
        cmd.setRequiresExtent(true);
        cmd.setEmbeddedOnly(false);
        cmd.setIdentityType(IdentityType.APPLICATION);

        for (AnnotationObject annotation : annotations)
        {
            Map<String, Object> annotationValues = annotation.getNameValueMap();
            String annName = annotation.getName();

            if (annName.equals(JPAAnnotationUtils.ENTITY))
            {
                // Entity name processed above
            }
            else if (annName.equals(JPAAnnotationUtils.MAPPED_SUPERCLASS))
            {
                cmd.setMappedSuperclass(true);
                if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
                {
                    InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
                    if (inhmd == null)
                    {
                        inhmd = cmd.newInheritanceMetadata();
                    }
                    inhmd.setStrategy(InheritanceStrategy.SUBCLASS_TABLE);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.DATASTORE_IDENTITY))
            {
                // extension to allow datastore-identity
                cmd.setIdentityType(IdentityType.DATASTORE);
                DatastoreIdentityMetaData idmd = cmd.newDatastoreIdentityMetadata();
                idmd.setColumnName((String)annotationValues.get("column"));

                Column[] columns = (Column[]) annotationValues.get("columns");
                if (columns != null && columns.length > 0)
                {
                    ColumnMetaData colmd = new ColumnMetaData();
                    colmd.setName(columns[0].name());
                    colmd.setLength(columns[0].length());
                    colmd.setScale(columns[0].scale());
                    if (columns[0].unique())
                    {
                        colmd.setUnique(true);
                    }
                    idmd.setColumnMetaData(colmd);
                }

                String identityStrategy = JPAAnnotationUtils.getValueGenerationStrategyString((GenerationType) annotationValues.get("generationType"));
                idmd.setValueStrategy(ValueGenerationStrategy.getIdentityStrategy(identityStrategy));
                String identityGenerator = (String) annotationValues.get("generator");
                if (identityGenerator != null)
                {
                    idmd.setSequence(identityGenerator);
                    idmd.setValueGeneratorName(identityGenerator);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.NONDURABLE_IDENTITY))
            {
                // extension to allow nondurable-identity
                cmd.setIdentityType(IdentityType.NONDURABLE);
            }
            else if (annName.equals(JPAAnnotationUtils.SURROGATE_VERSION))
            {
                // extension to allow surrogate version
                VersionMetaData vermd = cmd.newVersionMetadata();
                vermd.setStrategy(VersionStrategy.VERSION_NUMBER);
                String colName = (String)annotationValues.get("columnName");
                if (!StringUtils.isWhitespace(colName))
                {
                    vermd.setColumnName(colName);
                }
                String indexed = (String) annotationValues.get("indexed");
                if (!StringUtils.isWhitespace(indexed))
                {
                    vermd.setIndexed(IndexedValue.getIndexedValue(indexed));
                }
            }
            else if (annName.equals(JPAAnnotationUtils.ACCESS))
            {
                AccessType access = (AccessType) annotationValues.get("value");
                cmd.setAccessViaField(access == AccessType.FIELD);
            }
            else if (annName.equals(JPAAnnotationUtils.TABLE))
            {
                cmd.setTable((String)annotationValues.get("name"));
                cmd.setCatalog((String)annotationValues.get("catalog"));
                cmd.setSchema((String)annotationValues.get("schema"));

                UniqueConstraint[] constrs = (UniqueConstraint[])annotationValues.get("uniqueConstraints");
                if (constrs != null && constrs.length > 0)
                {
                    for (UniqueConstraint constr : constrs)
                    {
                        UniqueMetaData unimd = new UniqueMetaData();
                        String uniName = constr.name();
                        if (!StringUtils.isWhitespace(uniName))
                        {
                            unimd.setName(uniName);
                        }
                        for (int k=0;k<constr.columnNames().length;k++)
                        {
                            unimd.addColumn(constr.columnNames()[k]);
                        }

                        cmd.addUniqueConstraint(unimd);
                        unimd.setParent(cmd);
                    }
                }

                Index[] indexConstrs = (Index[]) annotationValues.get("indexes");
                if (indexConstrs != null && indexConstrs.length > 0)
                {
                    for (Index indexConstr : indexConstrs)
                    {
                        IndexMetaData idxmd = new IndexMetaData();
                        String idxName = indexConstr.name();
                        if (!StringUtils.isWhitespace(idxName))
                        {
                            idxmd.setName(idxName);
                        }
                        String colStr = indexConstr.columnList();
                        String[] cols = StringUtils.split(colStr, ",");
                        if (cols != null)
                        {
                            StringBuilder colOrderings = new StringBuilder();
                            boolean orderingSpecified = false;
                            for (int k=0;k<cols.length;k++)
                            {
                                String colName = cols[k].trim();
                                String colOrder = "ASC";
                                int spacePos = colName.indexOf(' ');
                                if (spacePos > 0)
                                {
                                    // Assumes we have "colName [ASC|DESC]". In principle we could also have "NULLS [FIRST|LAST]" but not supported
                                    colOrder = colName.substring(spacePos+1);
                                    colName = colName.substring(0, spacePos);
                                    orderingSpecified = true;
                                }

                                idxmd.addColumn(colName);
                                if (k != 0)
                                {
                                    colOrderings.append(",");
                                }
                                colOrderings.append(colOrder);
                            }
                            if (orderingSpecified)
                            {
                                idxmd.addExtension(MetaData.EXTENSION_INDEX_COLUMN_ORDERING, colOrderings.toString());
                            }
                        }
                        if (indexConstr.unique())
                        {
                            idxmd.setUnique(true);
                        }

                        cmd.addIndex(idxmd);
                        idxmd.setParent(cmd);
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.ID_CLASS))
            {
                cmd.setObjectIdClass(((Class)annotationValues.get("value")).getName());
            }
            else if (annName.equals(JPAAnnotationUtils.INHERITANCE))
            {
                // Add any InheritanceMetaData
                InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
                if (inhmd == null)
                {
                    inhmd = cmd.newInheritanceMetadata();
                }

                // Only valid in the root class
                InheritanceType inhType = (InheritanceType)annotationValues.get("strategy");
                String inheritanceStrategy = null;
                if (inhType == InheritanceType.JOINED)
                {
                    inheritanceStrategy = InheritanceStrategy.NEW_TABLE.toString();
                }
                else if (inhType == InheritanceType.TABLE_PER_CLASS)
                {
                    inheritanceStrategy = InheritanceStrategy.COMPLETE_TABLE.toString();
                }
                else if (inhType == InheritanceType.SINGLE_TABLE)
                {
                    // Translated to root class as "new-table" and children as "superclass-table"
                    // and @Inheritance should only be specified on root class so defaults to internal default
                }
                inhmd.setStrategy(inheritanceStrategy);
                inhmd.setStrategyForTree(inhType.toString());
            }
            else if (annName.equals(JPAAnnotationUtils.DISCRIMINATOR_COLUMN))
            {
                // Add any InheritanceMetaData with nested DiscriminatorMetaData
                InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
                if (inhmd == null)
                {
                    inhmd = cmd.newInheritanceMetadata();
                }
                DiscriminatorMetaData dismd = inhmd.getDiscriminatorMetaData();
                if (dismd == null)
                {
                    dismd = inhmd.newDiscriminatorMetadata();
                }

                String discriminatorColumnName = (String)annotationValues.get("name");
                dismd.setColumnName(discriminatorColumnName);

                DiscriminatorType type = (DiscriminatorType)annotationValues.get("discriminatorType");
                String discriminatorColumnType = null;
                if (type == DiscriminatorType.CHAR)
                {
                    discriminatorColumnType = "CHAR";
                }
                else if (type == DiscriminatorType.INTEGER)
                {
                    discriminatorColumnType = "INTEGER";
                }
                else if (type == DiscriminatorType.STRING)
                {
                    discriminatorColumnType = "VARCHAR";
                }
                Integer discriminatorColumnLength = (Integer)annotationValues.get("length");

                String discriminatorColumnDdl = null;
                String tmp = (String)annotationValues.get("columnDefinition");
                if (!StringUtils.isWhitespace(tmp))
                {
                    discriminatorColumnDdl = tmp;
                }

                ColumnMetaData discolmd = null;
                if (discriminatorColumnLength != null || discriminatorColumnType != null || discriminatorColumnDdl != null)
                {
                    // Add required column details for discriminator
                    discolmd = new ColumnMetaData();
                    discolmd.setName(discriminatorColumnName);
                    if (discriminatorColumnType != null)
                    {
                        discolmd.setJdbcType(discriminatorColumnType);
                    }
                    if (discriminatorColumnLength != null)
                    {
                        discolmd.setLength(discriminatorColumnLength);
                    }
                    dismd.setColumnMetaData(discolmd);
                    if (discriminatorColumnDdl != null)
                    {
                        discolmd.setColumnDdl(discriminatorColumnDdl);
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.DISCRIMINATOR_VALUE))
            {
                // Add any InheritanceMetaData with nested DiscriminatorMetaData
                InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
                if (inhmd == null)
                {
                    inhmd = cmd.newInheritanceMetadata();
                }
                DiscriminatorMetaData dismd = inhmd.getDiscriminatorMetaData();
                if (dismd == null)
                {
                    dismd = inhmd.newDiscriminatorMetadata();
                }

                // Using value-map strategy, with the specified value
                dismd.setValue((String)annotationValues.get("value"));
                dismd.setStrategy(DiscriminatorStrategy.VALUE_MAP);
                dismd.setIndexed("true");
            }
            else if (annName.equals(JPAAnnotationUtils.EMBEDDABLE))
            {
                cmd.setEmbeddedOnly(true);
                cmd.setIdentityType(IdentityType.NONDURABLE);
            }
            else if (annName.equals(JPAAnnotationUtils.CACHEABLE))
            {
                Boolean cacheableVal = (Boolean)annotationValues.get("value");
                cmd.setCacheable(cacheableVal != null ? cacheableVal : true);
            }
            else if (annName.equals(JPAAnnotationUtils.ENTITY_LISTENERS))
            {
                Class[] entityListeners = (Class[])annotationValues.get("value");
                if (entityListeners != null)
                {
                    for (Class entityListener : entityListeners)
                    {
                        // Any EventListener will not have their callback methods registered at this point
                        EventListenerMetaData elmd = new EventListenerMetaData(entityListener.getName());
                        cmd.addListener(elmd);
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.EXCLUDE_SUPERCLASS_LISTENERS))
            {
                cmd.excludeSuperClassListeners();
            }
            else if (annName.equals(JPAAnnotationUtils.EXCLUDE_DEFAULT_LISTENERS))
            {
                cmd.excludeDefaultListeners();
            }
            else if (annName.equals(JPAAnnotationUtils.SEQUENCE_GENERATOR))
            {
                processSequenceGeneratorAnnotation(pmd, annotationValues);
            }
            else if (annName.equals(JPAAnnotationUtils.TABLE_GENERATOR))
            {
                processTableGeneratorAnnotation(pmd, annotationValues);
            }
            else if (annName.equals(JPAAnnotationUtils.PRIMARY_KEY_JOIN_COLUMN))
            {
                // Override the PK column name when we have a persistent superclass
                PrimaryKeyMetaData pkmd = cmd.newPrimaryKeyMetadata();

                ColumnMetaData pkcolMd = new ColumnMetaData();
                pkcolMd.setName((String)annotationValues.get("name"));
                pkcolMd.setTarget((String)annotationValues.get("referencedColumnName"));
                pkmd.addColumn(pkcolMd);

                ForeignKey fk = (ForeignKey) annotationValues.get("foreignKey");
                if (fk != null && fk.value() != ConstraintMode.PROVIDER_DEFAULT)
                {
                    ForeignKeyMetaData fkmd = cmd.newForeignKeyMetadata();
                    fkmd.setName(fk.name());
                    fkmd.setFkDefinition(fk.foreignKeyDefinition());
                    fkmd.addColumn(pkcolMd);
                    if (fk.value() == ConstraintMode.NO_CONSTRAINT)
                    {
                        fkmd.setFkDefinitionApplies(false);
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.PRIMARY_KEY_JOIN_COLUMNS))
            {
                // Override the PK column names when we have a persistent superclass
                PrimaryKeyJoinColumn[] values = (PrimaryKeyJoinColumn[])annotationValues.get("value");

                PrimaryKeyMetaData pkmd = cmd.newPrimaryKeyMetadata();

                ForeignKey fk = (ForeignKey) annotationValues.get("foreignKey");
                ForeignKeyMetaData fkmd = null;
                if (fk != null && fk.value() != ConstraintMode.PROVIDER_DEFAULT)
                {
                    fkmd = cmd.newForeignKeyMetadata();
                    fkmd.setName(fk.name());
                    fkmd.setFkDefinition(fk.foreignKeyDefinition());
                    if (fk.value() == ConstraintMode.NO_CONSTRAINT)
                    {
                        fkmd.setFkDefinitionApplies(false);
                    }
                }

                for (PrimaryKeyJoinColumn pkJoinCol : values)
                {
                    ColumnMetaData pkcolMD = new ColumnMetaData();
                    pkcolMD.setName(pkJoinCol.name());
                    pkcolMD.setTarget(pkJoinCol.referencedColumnName());
                    if (!StringUtils.isWhitespace(pkJoinCol.columnDefinition()))
                    {
                        pkcolMD.setColumnDdl(pkJoinCol.columnDefinition());
                    }
                    pkmd.addColumn(pkcolMD);

                    if (fkmd != null)
                    {
                        fkmd.addColumn(pkcolMD);
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.ATTRIBUTE_OVERRIDES))
            {
                AttributeOverride[] overrides = (AttributeOverride[])annotationValues.get("value");
                if (overrides != null)
                {
                    for (AttributeOverride override : overrides)
                    {
                        AbstractMemberMetaData fmd = new FieldMetaData(cmd, "#UNKNOWN." + override.name());
                        fmd.setPersistenceModifier(FieldPersistenceModifier.PERSISTENT);
                        Column col = override.column();
                        // TODO Make inferrals about jdbctype, length etc if the field is 1 char etc
                        ColumnMetaData colmd = new ColumnMetaData();
                        colmd.setName(col.name());
                        colmd.setLength(col.length());
                        colmd.setScale(col.scale());
                        colmd.setAllowsNull(col.nullable());
                        colmd.setInsertable(col.insertable());
                        colmd.setUpdateable(col.updatable());
                        colmd.setUnique(col.unique());
                        fmd.addColumn(colmd);

                        cmd.addMember(fmd);
                        fmd.setParent(cmd);
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.ATTRIBUTE_OVERRIDE))
            {
                AbstractMemberMetaData fmd = new FieldMetaData(cmd, "#UNKNOWN." + (String)annotationValues.get("name"));
                Column col = (Column)annotationValues.get("column");
                // TODO Make inferrals about jdbctype, length etc if the field is 1 char etc
                ColumnMetaData colmd = new ColumnMetaData();
                colmd.setName(col.name());
                colmd.setLength(col.length());
                colmd.setScale(col.scale());
                colmd.setAllowsNull(col.nullable());
                colmd.setInsertable(col.insertable());
                colmd.setUpdateable(col.updatable());
                colmd.setUnique(col.unique());
                fmd.addColumn(colmd);

                cmd.addMember(fmd);
                fmd.setParent(cmd);
            }
            else if (annName.equals(JPAAnnotationUtils.ASSOCIATION_OVERRIDES))
            {
                AssociationOverride[] overrides = (AssociationOverride[])annotationValues.get("value");
                if (overrides != null)
                {
                    for (AssociationOverride override : overrides)
                    {
                        AbstractMemberMetaData fmd = new FieldMetaData(cmd, "#UNKNOWN." + override.name());
                        JoinColumn[] cols = override.joinColumns();
                        for (JoinColumn col : cols)
                        {
                            ColumnMetaData colmd = new ColumnMetaData();
                            colmd.setName(col.name());
                            colmd.setTarget(col.referencedColumnName());
                            colmd.setAllowsNull(col.nullable());
                            colmd.setInsertable(col.insertable());
                            colmd.setUpdateable(col.updatable());
                            colmd.setUnique(col.unique());
                            fmd.addColumn(colmd);
                        }

                        cmd.addMember(fmd);
                        fmd.setParent(cmd);
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.ASSOCIATION_OVERRIDE))
            {
                AbstractMemberMetaData fmd = new FieldMetaData(cmd, "#UNKNOWN." + (String)annotationValues.get("name"));
                JoinColumn[] cols = (JoinColumn[])annotationValues.get("joinColumns");
                for (JoinColumn col : cols)
                {
                    ColumnMetaData colmd = new ColumnMetaData();
                    colmd.setName(col.name());
                    colmd.setTarget(col.referencedColumnName());
                    colmd.setAllowsNull(col.nullable());
                    colmd.setInsertable(col.insertable());
                    colmd.setUpdateable(col.updatable());
                    colmd.setUnique(col.unique());
                    fmd.addColumn(colmd);
                }

                cmd.addMember(fmd);
                fmd.setParent(cmd);
            }
            else if (annName.equals(JPAAnnotationUtils.SQL_RESULTSET_MAPPINGS))
            {
                SqlResultSetMapping[] mappings = (SqlResultSetMapping[])annotationValues.get("value");
                for (SqlResultSetMapping mapping : mappings)
                {
                    QueryResultMetaData qrmd = new QueryResultMetaData(mapping.name());
                    EntityResult[] entityResults = mapping.entities();
                    if (entityResults != null && entityResults.length > 0)
                    {
                        for (EntityResult entityResult : entityResults)
                        {
                            String entityClassName = entityResult.entityClass().getName();
                            qrmd.addPersistentTypeMapping(entityClassName, null, entityResult.discriminatorColumn());
                            FieldResult[] fields = entityResult.fields();
                            if (fields != null)
                            {
                                for (int l=0;l<fields.length;l++)
                                {
                                    qrmd.addMappingForPersistentTypeMapping(entityClassName, fields[l].name(), fields[l].column());
                                }
                            }
                        }
                    }
                    ColumnResult[] colResults = mapping.columns();
                    if (colResults != null && colResults.length > 0)
                    {
                        for (ColumnResult colResult : colResults)
                        {
                            qrmd.addScalarColumn(colResult.name());
                        }
                    }

                    ConstructorResult[] ctrResults = mapping.classes();
                    if (ctrResults != null && ctrResults.length > 0)
                    {
                        for (ConstructorResult ctrResult : ctrResults)
                        {
                            String ctrClassName = ctrResult.targetClass().getName();
                            List<ConstructorTypeColumn> ctrCols = null;
                            ColumnResult[] cols = ctrResult.columns();
                            if (cols != null && cols.length > 0)
                            {
                                ctrCols = new ArrayList<ConstructorTypeColumn>();
                                for (int l=0;l<cols.length;l++)
                                {
                                    ctrCols.add(new ConstructorTypeColumn(cols[l].name(), cols[l].type()));
                                }
                            }
                            qrmd.addConstructorTypeMapping(ctrClassName, ctrCols);
                        }
                    }

                    cmd.addQueryResultMetaData(qrmd);
                    qrmd.setParent(cmd);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.SQL_RESULTSET_MAPPING))
            {
                QueryResultMetaData qrmd = new QueryResultMetaData((String)annotationValues.get("name"));
                EntityResult[] entityResults = (EntityResult[])annotationValues.get("entities");
                if (entityResults != null && entityResults.length > 0)
                {
                    for (int j=0;j<entityResults.length;j++)
                    {
                        String entityClassName = entityResults[j].entityClass().getName();
                        qrmd.addPersistentTypeMapping(entityClassName, null, entityResults[j].discriminatorColumn());
                        FieldResult[] fields = entityResults[j].fields();
                        if (fields != null)
                        {
                            for (int k=0;k<fields.length;k++)
                            {
                                qrmd.addMappingForPersistentTypeMapping(entityClassName, fields[k].name(), fields[k].column());
                            }
                        }
                    }
                }
                ColumnResult[] colResults = (ColumnResult[])annotationValues.get("columns");
                if (colResults != null && colResults.length > 0)
                {
                    for (int j=0;j<colResults.length;j++)
                    {
                        qrmd.addScalarColumn(colResults[j].name());
                    }
                }
                ConstructorResult[] ctrResults = (ConstructorResult[]) annotationValues.get("classes");
                if (ctrResults != null && ctrResults.length > 0)
                {
                    for (int j=0;j<ctrResults.length;j++)
                    {
                        String ctrClassName = ctrResults[j].targetClass().getName();
                        List<ConstructorTypeColumn> ctrCols = null;
                        ColumnResult[] cols = ctrResults[j].columns();
                        if (cols != null && cols.length > 0)
                        {
                            ctrCols = new ArrayList<ConstructorTypeColumn>();
                            for (int k=0;k<cols.length;k++)
                            {
                                ctrCols.add(new ConstructorTypeColumn(cols[k].name(), cols[k].type()));
                            }
                        }
                        qrmd.addConstructorTypeMapping(ctrClassName, ctrCols);
                    }
                }

                cmd.addQueryResultMetaData(qrmd);
                qrmd.setParent(cmd);
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_ENTITY_GRAPHS))
            {
                NamedEntityGraph[] graphs = (NamedEntityGraph[])annotationValues.get("value");
                for (int j=0;j<graphs.length;j++)
                {
                    String graphName = graphs[j].name();
                    if (StringUtils.isWhitespace(graphName))
                    {
                        // Fallback to entity name; TODO What if more than 1 graph?
                        graphName = cmd.getEntityName();
                    }
                    JPAEntityGraph eg = new JPAEntityGraph(mmgr, graphName, cls);
                    boolean includeAll = graphs[j].includeAllAttributes();
                    if (includeAll)
                    {
                        eg.setIncludeAll();
                    }

                    // Add nodes for graph
                    Map<String, String> attributeNameBySubgraphName = new HashMap();
                    NamedAttributeNode[] nodes = graphs[j].attributeNodes();
                    if (nodes != null && nodes.length > 0)
                    {
                        for (int k=0;k<nodes.length;k++)
                        {
                            String attributeName = nodes[k].value();
                            String subgraphName = nodes[k].subgraph();
                            eg.addAttributeNodes(attributeName);
                            // TODO Use keySubgraph
                            if (!StringUtils.isWhitespace(subgraphName))
                            {
                                attributeNameBySubgraphName.put(subgraphName, attributeName);
                            }
                        }
                    }

                    // Add subgraphs for graph
                    NamedSubgraph[] subgraphs = graphs[j].subgraphs();
                    if (subgraphs != null && subgraphs.length > 0)
                    {
                        for (int k=0;k<subgraphs.length;k++)
                        {
                            String subgraphName = subgraphs[k].name();
                            String attributeName = attributeNameBySubgraphName.get(subgraphName);
                            JPASubgraph subgraph = (JPASubgraph) eg.addSubgraph(attributeName, subgraphs[k].type());
                            NamedAttributeNode[] subnodes = subgraphs[k].attributeNodes();
                            if (subnodes != null && subnodes.length > 0)
                            {
                                for (int l=0;l<subnodes.length;l++)
                                {
                                    subgraph.addAttributeNodes(subnodes[l].value());
                                }
                            }
                        }
                    }
                    NamedSubgraph[] subclassSubgraphs = graphs[j].subclassSubgraphs();
                    if (subclassSubgraphs != null && subclassSubgraphs.length > 0)
                    {
                        if (subgraphs != null)
                        {
                            for (int k=0;k<subgraphs.length;k++)
                            {
                                JPASubgraph subgraph = (JPASubgraph) eg.addSubclassSubgraph(subgraphs[k].type());
                                NamedAttributeNode[] subnodes = subgraphs[k].attributeNodes();
                                if (subnodes != null && subnodes.length > 0)
                                {
                                    for (int l=0;l<subnodes.length;l++)
                                    {
                                        subgraph.addAttributeNodes(subnodes[l].value());
                                    }
                                }
                            }
                        }
                    }
                    ((JPAMetaDataManager)mmgr).registerEntityGraph(eg);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_ENTITY_GRAPH))
            {
                String graphName = (String)annotationValues.get("name");
                if (StringUtils.isWhitespace(graphName))
                {
                    graphName = cmd.getEntityName();
                }
                JPAEntityGraph eg = new JPAEntityGraph(mmgr, graphName, cls);
                boolean includeAll = (Boolean) annotationValues.get("includeAllAttributes");
                if (includeAll)
                {
                    eg.setIncludeAll();
                }
                Map<String, String> attributeNameBySubgraphName = new HashMap();
                NamedAttributeNode[] nodes = (NamedAttributeNode[]) annotationValues.get("attributeNodes");
                if (nodes != null && nodes.length > 0)
                {
                    for (int k=0;k<nodes.length;k++)
                    {
                        String attributeName = nodes[k].value();
                        String subgraphName = nodes[k].subgraph();
                        eg.addAttributeNodes(attributeName);
                        // TODO Use keySubgraph
                        if (!StringUtils.isWhitespace(subgraphName))
                        {
                            attributeNameBySubgraphName.put(subgraphName, attributeName);
                        }
                    }
                }
                NamedSubgraph[] subgraphs = (NamedSubgraph[])annotationValues.get("subgraphs");
                if (subgraphs != null && subgraphs.length > 0)
                {
                    for (int k=0;k<subgraphs.length;k++)
                    {
                        String subgraphName = subgraphs[k].name();
                        String attributeName = attributeNameBySubgraphName.get(subgraphName);
                        JPASubgraph subgraph = (JPASubgraph) eg.addSubgraph(attributeName, subgraphs[k].type());
                        NamedAttributeNode[] subnodes = subgraphs[k].attributeNodes();
                        if (subnodes != null && subnodes.length > 0)
                        {
                            for (int l=0;l<subnodes.length;l++)
                            {
                                subgraph.addAttributeNodes(subnodes[l].value());
                            }
                        }
                    }
                }
                NamedSubgraph[] subclassSubgraphs = (NamedSubgraph[])annotationValues.get("subclassSubgraphs");
                if (subclassSubgraphs != null && subclassSubgraphs.length > 0)
                {
                    for (int k=0;k<subclassSubgraphs.length;k++)
                    {
                        JPASubgraph subgraph = (JPASubgraph) eg.addSubclassSubgraph(subclassSubgraphs[k].type());
                        NamedAttributeNode[] subnodes = subclassSubgraphs[k].attributeNodes();
                        if (subnodes != null && subnodes.length > 0)
                        {
                            for (int l=0;l<subnodes.length;l++)
                            {
                                subgraph.addAttributeNodes(subnodes[l].value());
                            }
                        }
                    }
                }
                ((JPAMetaDataManager)mmgr).registerEntityGraph(eg);
            }
            else if (annName.equals(JPAAnnotationUtils.SECONDARY_TABLES))
            {
                SecondaryTable[] secTableAnns = (SecondaryTable[])annotationValues.get("value");
                if (secTableAnns != null)
                {
                    for (int j=0;j<secTableAnns.length;j++)
                    {
                        JoinMetaData joinmd = new JoinMetaData();
                        joinmd.setTable(secTableAnns[j].name());
                        joinmd.setCatalog(secTableAnns[j].catalog());
                        joinmd.setSchema(secTableAnns[j].schema());
                        PrimaryKeyJoinColumn[] pkJoinCols = secTableAnns[j].pkJoinColumns();
                        if (pkJoinCols != null)
                        {
                            for (int k = 0; k < pkJoinCols.length; k++)
                            {
                                ColumnMetaData colmd = new ColumnMetaData();
                                colmd.setName(pkJoinCols[k].name());
                                colmd.setTarget(pkJoinCols[k].referencedColumnName());
                                joinmd.addColumn(colmd);
                            }
                        }
                        cmd.addJoin(joinmd);

                        UniqueConstraint[] constrs = secTableAnns[j].uniqueConstraints();
                        if (constrs != null && constrs.length > 0)
                        {
                            for (int k=0;k<constrs.length;k++)
                            {
                                UniqueMetaData unimd = new UniqueMetaData();
                                String uniName = constrs[k].name();
                                if (!StringUtils.isWhitespace(uniName))
                                {
                                    unimd.setName(uniName);
                                }
                                for (int l=0;l<constrs[k].columnNames().length;l++)
                                {
                                    unimd.addColumn(constrs[k].columnNames()[l]);
                                }
                                joinmd.setUniqueMetaData(unimd); // JDO only allows one unique
                            }
                        }

                        Index[] indexConstrs = secTableAnns[j].indexes();
                        if (indexConstrs != null && indexConstrs.length > 0)
                        {
                            for (int k=0;k<indexConstrs.length;k++)
                            {
                                IndexMetaData idxmd = new IndexMetaData();
                                String idxName = indexConstrs[k].name();
                                if (!StringUtils.isWhitespace(idxName))
                                {
                                    idxmd.setName(idxName);
                                }
                                String colStr = indexConstrs[k].columnList();
                                String[] cols = StringUtils.split(colStr, ",");
                                if (cols != null)
                                {
                                    StringBuilder colOrderings = new StringBuilder();
                                    boolean orderingSpecified = false;
                                    for (int l=0;l<cols.length;l++)
                                    {
                                        String colName = cols[l].trim();
                                        String colOrder = "ASC";
                                        int spacePos = colName.indexOf(' ');
                                        if (spacePos > 0)
                                        {
                                            // Assumes we have "colName [ASC|DESC]". In principle we could also have "NULLS [FIRST|LAST]" but not supported
                                            colOrder = colName.substring(spacePos+1);
                                            colName = colName.substring(0, spacePos);
                                            orderingSpecified = true;
                                        }

                                        idxmd.addColumn(colName);
                                        if (k != 0)
                                        {
                                            colOrderings.append(",");
                                        }
                                        colOrderings.append(colOrder);
                                    }
                                    if (orderingSpecified)
                                    {
                                        idxmd.addExtension(MetaData.EXTENSION_INDEX_COLUMN_ORDERING, colOrderings.toString());
                                    }
                                }
                                if (indexConstrs[k].unique())
                                {
                                    idxmd.setUnique(true);
                                }
                                joinmd.setIndexMetaData(idxmd); // JDO only allows one unique
                            }
                        }
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.SECONDARY_TABLE))
            {
                JoinMetaData joinmd = new JoinMetaData();
                joinmd.setTable((String)annotationValues.get("name"));
                joinmd.setCatalog((String)annotationValues.get("catalog"));
                joinmd.setSchema((String)annotationValues.get("schema"));
                if (annotationValues.get("pkJoinColumns") != null)
                {
                    PrimaryKeyJoinColumn[] joinCols = (PrimaryKeyJoinColumn[])annotationValues.get("pkJoinColumns");
                    for (int j = 0; j < joinCols.length; j++)
                    {
                        ColumnMetaData colmd = new ColumnMetaData();
                        colmd.setName(joinCols[j].name());
                        colmd.setTarget(joinCols[j].referencedColumnName());
                        joinmd.addColumn(colmd);
                    }
                }
                cmd.addJoin(joinmd);
                if (annotationValues.containsKey("foreignKey"))
                {
                    ForeignKey fk = (ForeignKey) annotationValues.get("foreignKey");
                    if (fk.value() == ConstraintMode.CONSTRAINT)
                    {
                        ForeignKeyMetaData fkmd = joinmd.newForeignKeyMetaData();
                        fkmd.setName(fk.name());
                        fkmd.setFkDefinition(fk.foreignKeyDefinition());
                    }
                }

                UniqueConstraint[] constrs = (UniqueConstraint[])annotationValues.get("uniqueConstraints");
                if (constrs != null && constrs.length > 0)
                {
                    for (int j=0;j<constrs.length;j++)
                    {
                        UniqueMetaData unimd = new UniqueMetaData();
                        String uniName = constrs[j].name();
                        if (!StringUtils.isWhitespace(uniName))
                        {
                            unimd.setName(uniName);
                        }
                        for (int k=0;k<constrs[j].columnNames().length;k++)
                        {
                            unimd.addColumn(constrs[j].columnNames()[k]);
                        }
                        joinmd.setUniqueMetaData(unimd); // JDO only allows one unique
                    }
                }

                Index[] indexConstrs = (Index[]) annotationValues.get("indexes");
                if (indexConstrs != null && indexConstrs.length > 0)
                {
                    for (int j=0;j<indexConstrs.length;j++)
                    {
                        IndexMetaData idxmd = new IndexMetaData();
                        String idxName = indexConstrs[j].name();
                        if (!StringUtils.isWhitespace(idxName))
                        {
                            idxmd.setName(idxName);
                        }
                        String colStr = indexConstrs[j].columnList();
                        String[] cols = StringUtils.split(colStr, ",");
                        if (cols != null)
                        {
                            StringBuilder colOrderings = new StringBuilder();
                            boolean orderingSpecified = false;
                            for (int k=0;k<cols.length;k++)
                            {
                                String colName = cols[k].trim();
                                String colOrder = "ASC";
                                int spacePos = colName.indexOf(' ');
                                if (spacePos > 0)
                                {
                                    // Assumes we have "colName [ASC|DESC]". In principle we could also have "NULLS [FIRST|LAST]" but not supported
                                    colOrder = colName.substring(spacePos+1);
                                    colName = colName.substring(0, spacePos);
                                    orderingSpecified = true;
                                }

                                idxmd.addColumn(colName);
                                if (k != 0)
                                {
                                    colOrderings.append(",");
                                }
                                colOrderings.append(colOrder);
                            }
                            if (orderingSpecified)
                            {
                                idxmd.addExtension(MetaData.EXTENSION_INDEX_COLUMN_ORDERING, colOrderings.toString());
                            }
                        }
                        if (indexConstrs[j].unique())
                        {
                            idxmd.setUnique(true);
                        }
                        joinmd.setIndexMetaData(idxmd); // JDO only allows one unique
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.EXTENSION))
            {
                cmd.addExtension((String)annotationValues.get("key"), (String)annotationValues.get("value"));
            }
            else if (annName.equals(JPAAnnotationUtils.EXTENSIONS))
            {
                // extension
                Extension[] values = (Extension[])annotationValues.get("value");
                if (values != null && values.length > 0)
                {
                    for (int j=0;j<values.length;j++)
                    {
                        cmd.addExtension(values[j].key().toString(), values[j].value().toString());
                    }
                }
            }
            else
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044203", cls.getName(), annotation.getName()));
            }
        }

        NucleusLogger.METADATA.debug(Localiser.msg("044200", cls.getName(), "JPA"));

        // Add fallback info for discriminator when not explicitly specified
        InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
        if (inhmd != null)
        {
            DiscriminatorMetaData dismd = inhmd.getDiscriminatorMetaData();
            if (dismd != null)
            {
                // Discriminator defined, but no value, so must be using class/entity strategy
                String discriminatorValue = dismd.getValue();
                if (discriminatorValue == null)
                {
                    if (mmgr.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_USE_DISCRIMINATOR_DEFAULT_CLASS_NAME))
                    {
                        // Legacy handling, DN <= 5.0.2
                        if (!Modifier.isAbstract(cls.getModifiers()))
                        {
                            // No value and concrete class so use class-name
                            discriminatorValue = cls.getName();
                            dismd.setValue(discriminatorValue);
                        }

                        dismd.setStrategy(DiscriminatorStrategy.CLASS_NAME);
                    }
                    else
                    {
                        dismd.setStrategy(DiscriminatorStrategy.VALUE_MAP_ENTITY_NAME);
                    }
                    dismd.setIndexed("true");
                }
            }
        }

        return cmd;
    }

    /**
     * Convenience method to process NamedQuery, NamedQueries, NamedNativeQuery, NamedNativeQueries, NamedStoredProcedureQueries, NamedStoredProcedureQuery annotations.
     * @param cmd Metadata for the class
     * @param annotations Annotations specified on the class
     */
    protected void processNamedQueries(AbstractClassMetaData cmd, AnnotationObject[] annotations)
    {
        for (AnnotationObject annotation : annotations)
        {
            Map<String, Object> annotationValues = annotation.getNameValueMap();
            String annName = annotation.getName();

            if (annName.equals(JPAAnnotationUtils.NAMED_QUERIES))
            {
                NamedQuery[] queries = (NamedQuery[])annotationValues.get("value");
                for (int j=0;j<queries.length;j++)
                {
                    if (StringUtils.isWhitespace(queries[j].name()))
                    {
                        throw new InvalidClassMetaDataException("044154", cmd.getFullClassName());
                    }
                    QueryMetaData qmd = new QueryMetaData(queries[j].name());
                    qmd.setLanguage(QueryLanguage.JPQL.toString());
                    qmd.setUnmodifiable(true);
                    qmd.setQuery(queries[j].query());
                    QueryHint[] hints = queries[j].hints();
                    if (hints != null)
                    {
                        for (int k=0;k<hints.length;k++)
                        {
                            qmd.addExtension(hints[k].name(), hints[k].value());
                        }
                    }

                    cmd.addQuery(qmd);
                    qmd.setParent(cmd);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_QUERY))
            {
                if (StringUtils.isWhitespace((String)annotationValues.get("name")))
                {
                    throw new InvalidClassMetaDataException("044154", cmd.getFullClassName());
                }
                QueryMetaData qmd = new QueryMetaData((String)annotationValues.get("name"));
                qmd.setLanguage(QueryLanguage.JPQL.toString());
                qmd.setUnmodifiable(true);
                qmd.setQuery((String)annotationValues.get("query"));
                QueryHint[] hints = (QueryHint[]) annotationValues.get("hints");
                if (hints != null)
                {
                    for (int j=0;j<hints.length;j++)
                    {
                        qmd.addExtension(hints[j].name(), hints[j].value());
                    }
                }

                cmd.addQuery(qmd);
                qmd.setParent(cmd);
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_NATIVE_QUERIES))
            {
                NamedNativeQuery[] queries = (NamedNativeQuery[])annotationValues.get("value");
                for (int j=0;j<queries.length;j++)
                {
                    String resultClassName = null;
                    if (queries[j].resultClass() != null && queries[j].resultClass() != void.class)
                    {
                        resultClassName = queries[j].resultClass().getName();
                    }
                    String resultMappingName = null;
                    if (queries[j].resultSetMapping() != null)
                    {
                        resultMappingName = queries[j].resultSetMapping();
                    }
                    QueryMetaData qmd = new QueryMetaData(queries[j].name());
                    qmd.setLanguage(QueryLanguage.SQL.toString());
                    qmd.setUnmodifiable(true);
                    qmd.setResultClass(resultClassName);
                    qmd.setResultMetaDataName(resultMappingName);
                    qmd.setQuery(queries[j].query());
                    QueryHint[] hints = queries[j].hints();
                    if (hints != null)
                    {
                        for (int k=0;k<hints.length;k++)
                        {
                            qmd.addExtension(hints[k].name(), hints[k].value());
                        }
                    }

                    cmd.addQuery(qmd);
                    qmd.setParent(cmd);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_NATIVE_QUERY))
            {
                Class resultClass = (Class)annotationValues.get("resultClass");
                String resultClassName = null;
                if (resultClass != null && resultClass != void.class)
                {
                    resultClassName = resultClass.getName();
                }
                String resultMappingName = (String)annotationValues.get("resultSetMapping");
                if (StringUtils.isWhitespace(resultMappingName))
                {
                    resultMappingName = null;
                }
                QueryMetaData qmd = new QueryMetaData((String)annotationValues.get("name"));
                qmd.setLanguage(QueryLanguage.SQL.toString());
                qmd.setUnmodifiable(true);
                qmd.setResultClass(resultClassName);
                qmd.setResultMetaDataName(resultMappingName);
                qmd.setQuery((String)annotationValues.get("query"));
                QueryHint[] hints = (QueryHint[]) annotationValues.get("hints");
                if (hints != null)
                {
                    for (int j=0;j<hints.length;j++)
                    {
                        qmd.addExtension(hints[j].name(), hints[j].value());
                    }
                }

                cmd.addQuery(qmd);
                qmd.setParent(cmd);
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_STOREDPROC_QUERIES))
            {
                NamedStoredProcedureQuery[] procs = (NamedStoredProcedureQuery[])annotationValues.get("value");
                for (int j=0;j<procs.length;j++)
                {
                    if (StringUtils.isWhitespace(procs[j].name()))
                    {
                        throw new InvalidClassMetaDataException("044154", cmd.getFullClassName());
                    }
                    StoredProcQueryMetaData spqmd = new StoredProcQueryMetaData(procs[j].name());
                    spqmd.setProcedureName(procs[j].procedureName());
                    if (procs[j].resultClasses() != null && procs[j].resultClasses().length > 0)
                    {
                        Class[] resultClasses = procs[j].resultClasses();
                        for (int k=0;k<resultClasses.length;k++)
                        {
                            spqmd.addResultClass(resultClasses[k].getName());
                        }
                    }
                    if (procs[j].resultSetMappings() != null && procs[j].resultSetMappings().length > 0)
                    {
                        String[] resultSetMappings = procs[j].resultSetMappings();
                        for (int k=0;k<resultSetMappings.length;k++)
                        {
                            spqmd.addResultSetMapping(resultSetMappings[k]);
                        }
                    }
                    if (procs[j].parameters() != null && procs[j].parameters().length > 0)
                    {
                        StoredProcedureParameter[] params = procs[j].parameters();
                        for (int k=0;k<params.length;k++)
                        {
                            StoredProcQueryParameterMetaData parammd = getMetaDataForStoredProcParameter(params[k]);
                            spqmd.addParameter(parammd);
                        }
                    }

                    cmd.addStoredProcQuery(spqmd);
                    spqmd.setParent(cmd);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_STOREDPROC_QUERY))
            {
                if (StringUtils.isWhitespace((String)annotationValues.get("name")))
                {
                    throw new InvalidClassMetaDataException("044154", cmd.getFullClassName());
                }
                StoredProcQueryMetaData spqmd = new StoredProcQueryMetaData((String)annotationValues.get("name"));
                spqmd.setProcedureName((String)annotationValues.get("procedureName"));

                Class[] resultClasses = (Class[])annotationValues.get("resultClasses");
                if (resultClasses != null)
                {
                    for (int j=0;j<resultClasses.length;j++)
                    {
                        spqmd.addResultClass(resultClasses[j].getName());
                    }
                }
                String[] resultSetMappings = (String[])annotationValues.get("resultSetMappings");
                if (resultSetMappings != null)
                {
                    for (int j=0;j<resultSetMappings.length;j++)
                    {
                        spqmd.addResultSetMapping(resultSetMappings[j]);
                    }
                }
                StoredProcedureParameter[] params = (StoredProcedureParameter[])annotationValues.get("parameters");
                if (params != null)
                {
                    for (int j=0;j<params.length;j++)
                    {
                        StoredProcQueryParameterMetaData parammd = getMetaDataForStoredProcParameter(params[j]);
                        spqmd.addParameter(parammd);
                    }
                }

                cmd.addStoredProcQuery(spqmd);
                spqmd.setParent(cmd);
            }
        }
    }

    protected StoredProcQueryParameterMetaData getMetaDataForStoredProcParameter(StoredProcedureParameter param)
    {
        StoredProcQueryParameterMetaData pmd = new StoredProcQueryParameterMetaData();
        pmd.setName(param.name());
        pmd.setType(param.type().getName());
        if (param.mode() == ParameterMode.IN)
        {
            pmd.setMode(StoredProcQueryParameterMode.IN);
        }
        else if (param.mode() == ParameterMode.OUT)
        {
            pmd.setMode(StoredProcQueryParameterMode.OUT);
        }
        else if (param.mode() == ParameterMode.INOUT)
        {
            pmd.setMode(StoredProcQueryParameterMode.INOUT);
        }
        else if (param.mode() == ParameterMode.REF_CURSOR)
        {
            pmd.setMode(StoredProcQueryParameterMode.REF_CURSOR);
        }
        return pmd;
    }

    /**
     * Convenience method to process the annotations for a field/property.
     * The passed annotations may have been specified on the field or on a getter method.
     * @param cmd The ClassMetaData to update
     * @param member The field/property
     * @param annotations The annotations for the field/property
     * @return The FieldMetaData/PropertyMetaData that was added (if any)
     */
    protected AbstractMemberMetaData processMemberAnnotations(AbstractClassMetaData cmd, Member member, AnnotationObject[] annotations)
    {
        if (Modifier.isTransient(member.getModifiers()))
        {
            // Field is Java transient so nothing to persist
            return null;
        }
        if (member.getName().startsWith(mmgr.getEnhancedMethodNamePrefix()))
        {
            // ignore enhanced fields/methods added during enhancement
            return null;
        }

        // TODO Maybe remove the isMemberDefaultPersistent check since we add missing members in ClassMetaData
        if ((annotations != null && annotations.length > 0) || mmgr.getApiAdapter().isMemberDefaultPersistent(member.getType()))
        {
            // Create the Field/Property MetaData so we have something to add to
            AbstractMemberMetaData mmd = newMetaDataForMember(cmd, member, annotations);

            if (annotations != null && annotations.length > 0)
            {
                // Process member annotations
                ColumnMetaData[] columnMetaData = null;
                String columnTable = null;
                JoinMetaData joinmd = null;
                ElementMetaData elemmd = null;
                KeyMetaData keymd = null;
                ValueMetaData valmd = null;
                boolean oneToMany = false;
                boolean manyToMany = false;

                for (AnnotationObject annotation : annotations)
                {
                    String annName = annotation.getName();
                    Map<String, Object> annotationValues = annotation.getNameValueMap();

                    if (annName.equals(JPAAnnotationUtils.JOIN_COLUMNS))
                    {
                        // 1-1 FK columns, or 1-N FK columns, or N-1 FK columns
                        JoinColumn[] cols = (JoinColumn[])annotationValues.get("value");
                        ForeignKey fk = (ForeignKey) annotationValues.get("foreignKey");
                        ForeignKeyMetaData fkmd = null;
                        if (fk != null && fk.value() != ConstraintMode.PROVIDER_DEFAULT)
                        {
                            fkmd = mmd.newForeignKeyMetaData();
                            fkmd.setName(fk.name());
                            fkmd.setFkDefinition(fk.foreignKeyDefinition());
                            if (fk.value() == ConstraintMode.NO_CONSTRAINT)
                            {
                                fkmd.setFkDefinitionApplies(false);
                            }
                        }
                        if (cols != null)
                        {
                            columnMetaData = new ColumnMetaData[cols.length];
                            for (int j=0;j<cols.length;j++)
                            {
                                if (annotationValues.get("table") != null)
                                {
                                    // Only currently using one table value
                                    columnTable = cols[j].table();
                                }
                                columnMetaData[j] = new ColumnMetaData();
                                columnMetaData[j].setName(cols[j].name());
                                columnMetaData[j].setTarget(cols[j].referencedColumnName());
                                columnMetaData[j].setAllowsNull(cols[j].nullable());
                                columnMetaData[j].setInsertable(cols[j].insertable());
                                columnMetaData[j].setUpdateable(cols[j].updatable());
                                columnMetaData[j].setUnique(cols[j].unique());
                                if (fkmd != null)
                                {
                                    fkmd.addColumn(columnMetaData[j]);
                                }
                            }
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.JOIN_COLUMN))
                    {
                        // 1-1 FK column, or 1-N FK column, or N-1 FK column
                        columnMetaData = new ColumnMetaData[1];
                        String colNullable = null;
                        String colInsertable = null;
                        String colUpdateable = null;
                        String colUnique = null;
                        if (annotationValues.get("table") != null)
                        {
                            columnTable = annotationValues.get("table").toString();
                        }
                        if (annotationValues.get("nullable") != null)
                        {
                            colNullable = annotationValues.get("nullable").toString();
                        }
                        if (annotationValues.get("insertable") != null)
                        {
                            colInsertable = annotationValues.get("insertable").toString();
                        }
                        if (annotationValues.get("updatable") != null)
                        {
                            // Note : "updatable" is spelt incorrectly in the JPA spec.
                            colUpdateable = annotationValues.get("updatable").toString();
                        }
                        if (annotationValues.get("unique") != null)
                        {
                            colUnique = annotationValues.get("unique").toString();
                        }
                        columnMetaData[0] = new ColumnMetaData();
                        columnMetaData[0].setName((String)annotationValues.get("name"));
                        columnMetaData[0].setTarget((String)annotationValues.get("referencedColumnName"));
                        columnMetaData[0].setAllowsNull(colNullable);
                        columnMetaData[0].setInsertable(colInsertable);
                        columnMetaData[0].setUpdateable(colUpdateable);
                        columnMetaData[0].setUnique(colUnique);
                        ForeignKey fk = (ForeignKey)annotationValues.get("foreignKey");
                        ForeignKeyMetaData fkmd = null;
                        if (fk != null && fk.value() != ConstraintMode.PROVIDER_DEFAULT)
                        {
                            fkmd = mmd.newForeignKeyMetaData();
                            fkmd.setName(fk.name());
                            fkmd.setFkDefinition(fk.foreignKeyDefinition());
                            if (fk.value() == ConstraintMode.NO_CONSTRAINT)
                            {
                                fkmd.setFkDefinitionApplies(false);
                            }
                            fkmd.addColumn(columnMetaData[0]);
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.PRIMARY_KEY_JOIN_COLUMNS))
                    {
                        // 1-1 FK columns
                        PrimaryKeyJoinColumn[] cols = (PrimaryKeyJoinColumn[])annotationValues.get("value");
                        if (cols != null)
                        {
                            ForeignKey fk = (ForeignKey) annotationValues.get("foreignKey");
                            ForeignKeyMetaData fkmd = null;
                            if (fk != null && fk.value() != ConstraintMode.PROVIDER_DEFAULT)
                            {
                                fkmd = mmd.newForeignKeyMetaData();
                                fkmd.setName(fk.name());
                                fkmd.setFkDefinition(fk.foreignKeyDefinition());
                                if (fk.value() == ConstraintMode.NO_CONSTRAINT)
                                {
                                    fkmd.setFkDefinitionApplies(false);
                                }
                            }
                            columnMetaData = new ColumnMetaData[cols.length];
                            for (int j=0;j<cols.length;j++)
                            {
                                columnMetaData[j] = new ColumnMetaData();
                                columnMetaData[j].setName(cols[j].name());
                                columnMetaData[j].setTarget(cols[j].referencedColumnName());
                                if (fkmd != null)
                                {
                                    fkmd.addColumn(columnMetaData[j]);
                                }
                            }
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.PRIMARY_KEY_JOIN_COLUMN))
                    {
                        // 1-1 FK column
                        columnMetaData = new ColumnMetaData[1];
                        columnMetaData[0] = new ColumnMetaData();
                        columnMetaData[0].setName((String)annotationValues.get("name"));
                        columnMetaData[0].setTarget((String)annotationValues.get("referencedColumnName"));
                        ForeignKey fk = (ForeignKey)annotationValues.get("foreignKey");
                        ForeignKeyMetaData fkmd = null;
                        if (fk != null && fk.value() != ConstraintMode.PROVIDER_DEFAULT)
                        {
                            fkmd = mmd.newForeignKeyMetaData();
                            fkmd.setName(fk.name());
                            fkmd.setFkDefinition(fk.foreignKeyDefinition());
                            if (fk.value() == ConstraintMode.NO_CONSTRAINT)
                            {
                                fkmd.setFkDefinitionApplies(false);
                            }
                            fkmd.addColumn(columnMetaData[0]);
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.ATTRIBUTE_OVERRIDES))
                    {
                        // AttributeOverrides on a field/property so assumed to be override of embedded (basic) field
                        mmd.setEmbedded(true);

                        // Embedded field overrides
                        AttributeOverride[] attributeOverride = (AttributeOverride[])annotationValues.get("value");
                        for (int j=0; j<attributeOverride.length; j++)
                        {
                            processEmbeddedAttributeOverride(mmd, attributeOverride[j].name(), member.getType(), attributeOverride[j].column());
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.ATTRIBUTE_OVERRIDE))
                    {
                        // AttributeOverride on a field/property so assumed to be override of embedded (basic) field
                        mmd.setEmbedded(true);

                        // Embedded field override
                        processEmbeddedAttributeOverride(mmd, (String)annotationValues.get("name"), member.getType(), (Column)annotationValues.get("column"));
                    }
                    else if (annName.equals(JPAAnnotationUtils.ASSOCIATION_OVERRIDES))
                    {
                        // TODO Not yet processed
                    }
                    else if (annName.equals(JPAAnnotationUtils.ASSOCIATION_OVERRIDE))
                    {
                        // TODO Not yet processed
                    }
                    else if (annName.equals(JPAAnnotationUtils.JOIN_TABLE))
                    {
                        // Process @JoinTable to generate JoinMetaData
                        String tableName = (String)annotationValues.get("name");
                        if (!StringUtils.isWhitespace(tableName))
                        {
                            mmd.setTable(tableName);
                        }
                        String catalogName = (String)annotationValues.get("catalog");
                        if (!StringUtils.isWhitespace(catalogName))
                        {
                            mmd.setCatalog(catalogName);
                        }
                        String schemaName = (String)annotationValues.get("schema");
                        if (!StringUtils.isWhitespace(schemaName))
                        {
                            mmd.setSchema(schemaName);
                        }

                        joinmd = new JoinMetaData();
                        mmd.setJoinMetaData(joinmd);

                        if (annotationValues.get("joinColumns") != null)
                        {
                            ArrayList<JoinColumn> joinColumns = new ArrayList<JoinColumn>();
                            joinColumns.addAll(Arrays.asList((JoinColumn[])annotationValues.get("joinColumns")));
                            for (int j = 0; j < joinColumns.size(); j++)
                            {
                                ColumnMetaData colmd = new ColumnMetaData();
                                colmd.setName(joinColumns.get(j).name());
                                colmd.setTarget(joinColumns.get(j).referencedColumnName());
                                colmd.setAllowsNull(joinColumns.get(j).nullable());
                                joinmd.addColumn(colmd);
                            }
                        }
                        if (annotationValues.containsKey("foreignKey"))
                        {
                            ForeignKey fk = (ForeignKey) annotationValues.get("foreignKey");
                            if (fk.value() == ConstraintMode.CONSTRAINT)
                            {
                                // User definition of FK from owner table to join table
                                ForeignKeyMetaData fkmd = joinmd.newForeignKeyMetaData();
                                fkmd.setName(fk.name());
                                fkmd.setFkDefinition(fk.foreignKeyDefinition());
                            }
                        }
                        if (annotationValues.get("inverseJoinColumns") != null)
                        {
                            ArrayList<JoinColumn> elementColumns = new ArrayList<JoinColumn>();
                            elementColumns.addAll(Arrays.asList((JoinColumn[])annotationValues.get("inverseJoinColumns")));
                            AbstractElementMetaData aemd = null;
                            if (Map.class.isAssignableFrom(member.getType()))
                            {
                                aemd = new ValueMetaData();
                                mmd.setValueMetaData((ValueMetaData)aemd);
                            }
                            else
                            {
                                aemd = new ElementMetaData();
                                mmd.setElementMetaData((ElementMetaData)aemd);
                            }
                            for (int j = 0; j < elementColumns.size(); j++)
                            {
                                ColumnMetaData colmd = new ColumnMetaData();
                                colmd.setName(elementColumns.get(j).name());
                                colmd.setTarget(elementColumns.get(j).referencedColumnName());
                                colmd.setAllowsNull(elementColumns.get(j).nullable());
                                aemd.addColumn(colmd);
                            }
                        }
                        if (annotationValues.containsKey("inverseForeignKey"))
                        {
                            ForeignKey fk = (ForeignKey) annotationValues.get("inverseForeignKey");
                            if (fk.value() == ConstraintMode.CONSTRAINT)
                            {
                                // User definition of FK from join table to element table
                                ForeignKeyMetaData fkmd = mmd.newForeignKeyMetaData();
                                fkmd.setName(fk.name());
                                fkmd.setFkDefinition(fk.foreignKeyDefinition());
                            }
                        }
                        UniqueConstraint[] joinUniqueConstraints = (UniqueConstraint[])annotationValues.get("uniqueConstraints");
                        if (joinUniqueConstraints != null && joinUniqueConstraints.length > 0)
                        {
                            // Unique constraints on the join table
                            for (int j=0;j<joinUniqueConstraints.length;j++)
                            {
                                UniqueMetaData unimd = new UniqueMetaData();
                                String uniName = joinUniqueConstraints[j].name();
                                if (!StringUtils.isWhitespace(uniName))
                                {
                                    unimd.setName(uniName);
                                }
                                for (int k=0;k<joinUniqueConstraints[j].columnNames().length;k++)
                                {
                                    unimd.addColumn(joinUniqueConstraints[j].columnNames()[k]);
                                }
                                joinmd.setUniqueMetaData(unimd); // JDO only supports a single unique constraint on a join table
                            }
                        }

                        Index[] joinIndexConstrs = (Index[]) annotationValues.get("indexes");
                        if (joinIndexConstrs != null && joinIndexConstrs.length > 0)
                        {
                            for (int j=0;j<joinIndexConstrs.length;j++)
                            {
                                IndexMetaData idxmd = new IndexMetaData();
                                String idxName = joinIndexConstrs[j].name();
                                if (!StringUtils.isWhitespace(idxName))
                                {
                                    idxmd.setName(idxName);
                                }
                                String colStr = joinIndexConstrs[j].columnList();
                                String[] cols = StringUtils.split(colStr, ",");
                                if (cols != null)
                                {
                                    StringBuilder colOrderings = new StringBuilder();
                                    boolean orderingSpecified = false;
                                    for (int k=0;k<cols.length;k++)
                                    {
                                        String colName = cols[k].trim();
                                        String colOrder = "ASC";
                                        int spacePos = colName.indexOf(' ');
                                        if (spacePos > 0)
                                        {
                                            // Assumes we have "colName [ASC|DESC]". In principle we could also have "NULLS [FIRST|LAST]" but not supported
                                            colOrder = colName.substring(spacePos+1);
                                            colName = colName.substring(0, spacePos);
                                            orderingSpecified = true;
                                        }

                                        idxmd.addColumn(colName);
                                        if (k != 0)
                                        {
                                            colOrderings.append(",");
                                        }
                                        colOrderings.append(colOrder);
                                    }
                                    if (orderingSpecified)
                                    {
                                        idxmd.addExtension(MetaData.EXTENSION_INDEX_COLUMN_ORDERING, colOrderings.toString());
                                    }
                                }
                                if (joinIndexConstrs[j].unique())
                                {
                                    idxmd.setUnique(true);
                                }
                                joinmd.setIndexMetaData(idxmd); // JDO only supports a single index on a join table
                            }
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.COLLECTION_TABLE))
                    {
                        // Process @CollectionTable to generate JoinMetaData
                        String tableName = (String)annotationValues.get("name");
                        if (!StringUtils.isWhitespace(tableName))
                        {
                            mmd.setTable(tableName);
                        }
                        String catalogName = (String)annotationValues.get("catalog");
                        if (!StringUtils.isWhitespace(catalogName))
                        {
                            mmd.setCatalog(catalogName);
                        }
                        String schemaName = (String)annotationValues.get("schema");
                        if (!StringUtils.isWhitespace(schemaName))
                        {
                            mmd.setSchema(schemaName);
                        }

                        joinmd = mmd.getJoinMetaData();
                        if (joinmd == null)
                        {
                            // Should always be set by @ElementCollection but add just in case
                            joinmd = new JoinMetaData();
                            mmd.setJoinMetaData(joinmd);
                        }

                        if (annotationValues.get("joinColumns") != null)
                        {
                            ArrayList<JoinColumn> joinColumns = new ArrayList<JoinColumn>();
                            joinColumns.addAll(Arrays.asList((JoinColumn[])annotationValues.get("joinColumns")));
                            for (int j = 0; j < joinColumns.size(); j++)
                            {
                                ColumnMetaData colmd = new ColumnMetaData();
                                colmd.setName(joinColumns.get(j).name());
                                colmd.setTarget(joinColumns.get(j).referencedColumnName());
                                colmd.setAllowsNull(joinColumns.get(j).nullable());
                                joinmd.addColumn(colmd);
                            }
                        }
                        if (annotationValues.containsKey("foreignKey"))
                        {
                            ForeignKey fk = (ForeignKey) annotationValues.get("foreignKey");
                            if (fk.value() == ConstraintMode.CONSTRAINT)
                            {
                                ForeignKeyMetaData fkmd = joinmd.newForeignKeyMetaData();
                                fkmd.setName(fk.name());
                                fkmd.setFkDefinition(fk.foreignKeyDefinition());
                            }
                        }
                        UniqueConstraint[] joinUniqueConstraints = (UniqueConstraint[])annotationValues.get("uniqueConstraints");
                        if (joinUniqueConstraints != null && joinUniqueConstraints.length > 0)
                        {
                            // Unique constraints on the join table
                            for (int j=0;j<joinUniqueConstraints.length;j++)
                            {
                                UniqueMetaData unimd = new UniqueMetaData();
                                String uniName = joinUniqueConstraints[j].name();
                                if (!StringUtils.isWhitespace(uniName))
                                {
                                    unimd.setName(uniName);
                                }
                                for (int k=0;k<joinUniqueConstraints[j].columnNames().length;k++)
                                {
                                    unimd.addColumn(joinUniqueConstraints[j].columnNames()[k]);
                                }
                                joinmd.setUniqueMetaData(unimd); // JDO only supports a single unique constraint on a join table
                            }
                        }

                        Index[] joinIndexConstrs = (Index[]) annotationValues.get("indexes");
                        if (joinIndexConstrs != null && joinIndexConstrs.length > 0)
                        {
                            for (int j=0;j<joinIndexConstrs.length;j++)
                            {
                                IndexMetaData idxmd = new IndexMetaData();
                                String idxName = joinIndexConstrs[j].name();
                                if (!StringUtils.isWhitespace(idxName))
                                {
                                    idxmd.setName(idxName);
                                }
                                String colStr = joinIndexConstrs[j].columnList();
                                String[] cols = StringUtils.split(colStr, ",");
                                if (cols != null)
                                {
                                    StringBuilder colOrderings = new StringBuilder();
                                    boolean orderingSpecified = false;
                                    for (int k=0;k<cols.length;k++)
                                    {
                                        String colName = cols[k].trim();
                                        String colOrder = "ASC";
                                        int spacePos = colName.indexOf(' ');
                                        if (spacePos > 0)
                                        {
                                            // Assumes we have "colName [ASC|DESC]". In principle we could also have "NULLS [FIRST|LAST]" but not supported
                                            colOrder = colName.substring(spacePos+1);
                                            colName = colName.substring(0, spacePos);
                                            orderingSpecified = true;
                                        }

                                        idxmd.addColumn(colName);
                                        if (k != 0)
                                        {
                                            colOrderings.append(",");
                                        }
                                        colOrderings.append(colOrder);
                                    }
                                    if (orderingSpecified)
                                    {
                                        idxmd.addExtension(MetaData.EXTENSION_INDEX_COLUMN_ORDERING, colOrderings.toString());
                                    }
                                }
                                if (joinIndexConstrs[j].unique())
                                {
                                    idxmd.setUnique(true);
                                }
                                joinmd.setIndexMetaData(idxmd); // JDO only supports a single index on a join table
                            }
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.MAP_KEY_CLASS))
                    {
                        MapMetaData mapmd = mmd.getMap();
                        if (mmd.getMap() == null)
                        {
                            mapmd = mmd.newMapMetaData();
                        }
                        mapmd.setKeyType(((Class) annotationValues.get("value")).getName());
                    }
                    else if (annName.equals(JPAAnnotationUtils.MAP_KEY_COLUMN))
                    {
                        if (keymd == null)
                        {
                            keymd = new KeyMetaData();
                            mmd.setKeyMetaData(keymd);
                        }

                        Class keyType = mmd.getMap() != null && mmd.getMap().getKeyType() != null ? clr.classForName(mmd.getMap().getKeyType()) : Object.class;
                        keymd.addColumn(newColumnMetaDataForAnnotation(keymd, keyType, annotationValues));
                    }
                    else if (annName.equals(JPAAnnotationUtils.MAP_KEY_JOIN_COLUMNS))
                    {
                        MapKeyJoinColumn[] mapKeyJoinCols = (MapKeyJoinColumn[])annotationValues.get("value");
                        if (mapKeyJoinCols == null || mapKeyJoinCols.length == 0)
                        {
                            // Do nothing
                        }
                        else
                        {
                            if (keymd == null)
                            {
                                keymd = new KeyMetaData();
                                mmd.setKeyMetaData(keymd);
                            }

                            Class keyType = mmd.getMap() != null && mmd.getMap().getKeyType() != null ? clr.classForName(mmd.getMap().getKeyType()) : Object.class;
                            for (int i=0;i<mapKeyJoinCols.length;i++)
                            {
                                Map<String,Object> columnAnnValues = new HashMap<>();
                                columnAnnValues.put("name", mapKeyJoinCols[i].name());
                                columnAnnValues.put("nullable", mapKeyJoinCols[i].nullable());
                                columnAnnValues.put("unique", mapKeyJoinCols[i].unique());
                                columnAnnValues.put("insertable", mapKeyJoinCols[i].insertable());
                                columnAnnValues.put("updatable", mapKeyJoinCols[i].updatable());
                                columnAnnValues.put("table", mapKeyJoinCols[i].table());
                                columnAnnValues.put("columnDefinition", mapKeyJoinCols[i].columnDefinition());
                                columnAnnValues.put("referencedColumnName", mapKeyJoinCols[i].referencedColumnName());
                                columnAnnValues.put("foreignKey", mapKeyJoinCols[i].foreignKey());
                                keymd.addColumn(newColumnMetaDataForAnnotation(keymd, keyType, columnAnnValues));
                            }
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.MAP_KEY_JOIN_COLUMN))
                    {
                        if (keymd == null)
                        {
                            keymd = new KeyMetaData();
                            mmd.setKeyMetaData(keymd);
                        }

                        Class keyType = mmd.getMap() != null && mmd.getMap().getKeyType() != null ? clr.classForName(mmd.getMap().getKeyType()) : Object.class;
                        keymd.addColumn(newColumnMetaDataForAnnotation(keymd, keyType, annotationValues));
                    }
                    else if (annName.equals(JPAAnnotationUtils.MAP_KEY))
                    {
                        String keyMappedBy = (String)annotationValues.get("name");
                        if (keyMappedBy != null)
                        {
                            if (keymd == null)
                            {
                                keymd = new KeyMetaData();
                                mmd.setKeyMetaData(keymd);
                            }
                            keymd.setMappedBy(keyMappedBy);
                            if (mmd.getMap() != null &&
                                    (mmd.getMap().getKeyType() == null || mmd.getMap().getKeyType().equals(Object.class.getName())))
                            {
                                // Set keyType based on mapped-by field of value class
                                String valueType = mmd.getMap().getValueType();
                                try
                                {
                                    Class cls = clr.classForName(valueType);
                                    try
                                    {
                                        Field fld = cls.getDeclaredField(keyMappedBy);
                                        mmd.getMap().setKeyType(fld.getType().getName());
                                    }
                                    catch (NoSuchFieldException nsfe)
                                    {
                                        try
                                        {
                                            String getterName = ClassUtils.getJavaBeanGetterName(keyMappedBy, false);
                                            Method mthd = cls.getDeclaredMethod(getterName, (Class[])null);
                                            mmd.getMap().setKeyType(mthd.getReturnType().getName());
                                        }
                                        catch (NoSuchMethodException nsme)
                                        {
                                        }
                                    }
                                }
                                catch (Exception e)
                                {
                                }
                            }
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.MAP_KEY_ENUMERATED))
                    {
                        EnumType type = (EnumType)annotationValues.get("value");
                        if (keymd == null)
                        {
                            keymd = new KeyMetaData();
                            mmd.setKeyMetaData(keymd);
                        }
                        // TODO Merge with any @MapKey specification of the column
                        ColumnMetaData colmd = keymd.newColumnMetaData();
                        colmd.setJdbcType(type == EnumType.STRING ? "VARCHAR" : "INTEGER");
                    }
                    else if (annName.equals(JPAAnnotationUtils.MAP_KEY_TEMPORAL))
                    {
                        TemporalType type = (TemporalType)annotationValues.get("value");
                        String jdbcType = null;
                        if (type == TemporalType.DATE)
                        {
                            jdbcType = "DATE";
                        }
                        else if (type == TemporalType.TIME)
                        {
                            jdbcType = "TIME";
                        }
                        else if (type == TemporalType.TIMESTAMP)
                        {
                            jdbcType = "TIMESTAMP";
                        }
                        if (keymd == null)
                        {
                            keymd = new KeyMetaData();
                            mmd.setKeyMetaData(keymd);
                        }
                        // TODO Merge with any @MapKey specification of the column
                        ColumnMetaData colmd = keymd.newColumnMetaData();
                        colmd.setJdbcType(jdbcType);
                    }
                    else if (annName.equals(JPAAnnotationUtils.ORDER_BY))
                    {
                        if (mmd.getOrderMetaData() != null)
                        {
                            throw new NucleusException("@OrderBy found yet field=" + cmd.getFullClassName() + "." + member.getName() + " already has ordering information!");
                        }

                        String orderBy = (String)annotationValues.get("value");
                        if (orderBy != null)
                        {
                            // "Ordered List"
                            OrderMetaData ordmd = new OrderMetaData();
                            ordmd.setOrdering(orderBy);
                            mmd.setOrderMetaData(ordmd);
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.ORDER_COLUMN))
                    {
                        if (mmd.getOrderMetaData() != null)
                        {
                            throw new NucleusException("@OrderColumn found yet field=" + cmd.getFullClassName() + "." + member.getName() +" already has ordering information!");
                        }

                        String columnName = (String)annotationValues.get("name");
                        OrderMetaData ordermd = new OrderMetaData();
                        ordermd.setColumnName(columnName);

                        String colNullable = null;
                        String colInsertable = null;
                        String colUpdateable = null;
                        if (annotationValues.get("nullable") != null)
                        {
                            colNullable = annotationValues.get("nullable").toString();
                        }
                        if (annotationValues.get("insertable") != null)
                        {
                            colInsertable = annotationValues.get("insertable").toString();
                        }
                        if (annotationValues.get("updatable") != null)
                        {
                            // Note : "updatable" is spelt incorrectly in the JPA spec.
                            colUpdateable = annotationValues.get("updatable").toString();
                        }
                        ColumnMetaData colmd = new ColumnMetaData();
                        colmd.setName(columnName);
                        colmd.setAllowsNull(colNullable);
                        colmd.setInsertable(colInsertable);
                        colmd.setUpdateable(colUpdateable);
                        String tmp = (String)annotationValues.get("columnDefinition");
                        if (!StringUtils.isWhitespace(tmp))
                        {
                            colmd.setColumnDdl(tmp);
                        }
                        ordermd.addColumn(colmd);
                        mmd.setOrderMetaData(ordermd);
                    }
                    else if (annName.equals(JPAAnnotationUtils.ONE_TO_MANY))
                    {
                        // 1-N relation
                        oneToMany = true;
                    }
                    else if (annName.equals(JPAAnnotationUtils.MANY_TO_MANY))
                    {
                        // M-N relation
                        manyToMany = true;
                    }
                    else if (annName.equals(JPAAnnotationUtils.ACCESS))
                    {
                        // TODO Support this
                    }
                    else if (annName.equals(JPAAnnotationUtils.CONVERTS))
                    {
                        if (isPersistenceContext()) // Don't process this when enhancing since not needed
                        {
                            // Multiple @Convert annotations (for embedded field)
                            Convert[] converts = (Convert[])annotationValues.get("value");
                            if (converts == null || converts.length == 0)
                            {
                                // Do nothing
                            }
                            else if (converts.length > 1)
                            {
                                // TODO Support this. If we have a "key" and "value" then this would come through here
                                NucleusLogger.METADATA.warn("Dont currently support @Converts annotation for embedded fields");
                            }
                            else if (converts.length == 1)
                            {
                                Class converterCls = converts[0].converter();
                                String convAttrName = converts[0].attributeName();
                                boolean disable = converts[0].disableConversion();
                                if (disable)
                                {
                                    mmd.setTypeConverterDisabled();
                                }
                                else
                                {
                                    TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();
                                    if (typeMgr.getTypeConverterForName(converterCls.getName()) == null)
                                    {
                                        // Not yet cached an instance of this converter so create one
                                        // TODO Support injectable AttributeConverters
                                        AttributeConverter entityConv = JPATypeConverterUtils.createAttributeConverterInstance(mmgr.getNucleusContext(), converterCls);

                                        // Extract field and datastore types for this converter
                                        Class attrType = member.getType();
                                        if ("key".equals(convAttrName))
                                        {
                                            attrType = ClassUtils.getMapKeyType(member.getType(), member.getGenericType());
                                        }
                                        else if ("value".equals(convAttrName))
                                        {
                                            attrType = ClassUtils.getMapValueType(member.getType(), member.getGenericType());
                                        }
                                        else if (!StringUtils.isWhitespace(convAttrName) && Collection.class.isAssignableFrom(member.getType()))
                                        {
                                            attrType = ClassUtils.getCollectionElementType(member.getType(), member.getGenericType());
                                        }
                                        Class dbType = JPATypeConverterUtils.getDatabaseTypeForAttributeConverter(converterCls, attrType, null);

                                        // Register the TypeConverter under the name of the AttributeConverter class
                                        TypeConverter conv = new JPATypeConverter(entityConv);
                                        typeMgr.registerConverter(converterCls.getName(), conv, attrType, dbType, false, null);
                                    }

                                    if (StringUtils.isWhitespace(convAttrName))
                                    {
                                        if (Collection.class.isAssignableFrom(member.getType()))
                                        {
                                            if (elemmd == null)
                                            {
                                                elemmd = new ElementMetaData();
                                                mmd.setElementMetaData(elemmd);
                                            }
                                            elemmd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterCls.getName());
                                        }
                                        else
                                        {
                                            mmd.setTypeConverterName(converterCls.getName());
                                        }
                                    }
                                    else
                                    {
                                        if ("key".equals(convAttrName))
                                        {
                                            if (keymd == null)
                                            {
                                                keymd = new KeyMetaData();
                                                mmd.setKeyMetaData(keymd);
                                            }
                                            keymd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterCls.getName());
                                        }
                                        else if ("value".equals(convAttrName))
                                        {
                                            if (valmd == null)
                                            {
                                                valmd = new ValueMetaData();
                                                mmd.setValueMetaData(valmd);
                                            }
                                            valmd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterCls.getName());
                                        }
                                        else
                                        {
                                            // TODO Support attributeName to convert field of embedded object, or field of key/value
                                            NucleusLogger.METADATA.warn("Field " + mmd.getFullFieldName() + 
                                                " has @Convert annotation for attribute " + convAttrName + " but this is not yet fully supported. Ignored");
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (annName.equals(JPAAnnotationUtils.CONVERT))
                    {
                        if (isPersistenceContext()) // Don't process this when enhancing since not needed
                        {
                            // JPA2.1 : Field needs to be converted for persistence/retrieval
                            Class converterCls = (Class)annotationValues.get("converter");
                            String convAttrName = (String)annotationValues.get("attributeName");
                            Boolean disable = (Boolean)annotationValues.get("disableConversion");
                            Class attrType = null;
                            Class dbType = null;
                            // TODO Support disable to override autoApply
                            if (disable != Boolean.TRUE)
                            {
                                TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();
                                TypeConverter conv = typeMgr.getTypeConverterForName(converterCls.getName());
                                if (typeMgr.getTypeConverterForName(converterCls.getName()) == null)
                                {
                                    // Not yet cached an instance of this converter so create one
                                    // TODO Support injectable AttributeConverters
                                    AttributeConverter entityConv = JPATypeConverterUtils.createAttributeConverterInstance(mmgr.getNucleusContext(), converterCls);

                                    // Extract attribute and datastore types for this converter
                                    attrType = member.getType();
                                    if (Map.class.isAssignableFrom(member.getType()))
                                    {
                                        if ("key".equals(convAttrName))
                                        {
                                            attrType = ClassUtils.getMapKeyType(member.getType(), member.getGenericType());
                                        }
                                        else if ("value".equals(convAttrName))
                                        {
                                            attrType = ClassUtils.getMapValueType(member.getType(), member.getGenericType());
                                        }
                                    }
                                    else if (Collection.class.isAssignableFrom(member.getType()))
                                    {
                                        // Assume it is for the element
                                        attrType = ClassUtils.getCollectionElementType(member.getType(), member.getGenericType());
                                    }
                                    dbType = JPATypeConverterUtils.getDatabaseTypeForAttributeConverter(converterCls, attrType, null);

                                    if (dbType == null)
                                    {
                                        if (Collection.class.isAssignableFrom(member.getType()))
                                        {
                                            // Assume the converter is for the whole field
                                            attrType = member.getType();
                                            dbType = JPATypeConverterUtils.getDatabaseTypeForAttributeConverter(converterCls, attrType, null);
                                        }
                                    }

                                    // Register the TypeConverter under the name of the AttributeConverter class
                                    conv = new JPATypeConverter(entityConv);
                                    typeMgr.registerConverter(converterCls.getName(), conv, attrType, dbType, false, null);
                                }
                                else
                                {
                                    attrType = typeMgr.getMemberTypeForTypeConverter(conv, dbType);
                                    dbType = typeMgr.getDatastoreTypeForTypeConverter(conv, attrType);
                                }

                                if (StringUtils.isWhitespace(convAttrName))
                                {
                                    if (Collection.class.isAssignableFrom(member.getType()) && !Collection.class.isAssignableFrom(attrType))
                                    {
                                        // Converter for the element
                                        if (elemmd == null)
                                        {
                                            elemmd = new ElementMetaData();
                                            mmd.setElementMetaData(elemmd);
                                        }
                                        elemmd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterCls.getName());
                                    }
                                    else
                                    {
                                        mmd.setTypeConverterName(converterCls.getName());
                                    }
                                }
                                else
                                {
                                    if ("key".equals(convAttrName))
                                    {
                                        if (keymd == null)
                                        {
                                            keymd = new KeyMetaData();
                                            mmd.setKeyMetaData(keymd);
                                        }
                                        keymd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterCls.getName());
                                    }
                                    else if ("value".equals(convAttrName))
                                    {
                                        if (valmd == null)
                                        {
                                            valmd = new ValueMetaData();
                                            mmd.setValueMetaData(valmd);
                                        }
                                        valmd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterCls.getName());
                                    }
                                    else
                                    {
                                        // TODO Support attributeName to convert field of embedded object, or field of key/value
                                        NucleusLogger.METADATA.warn("Field " + mmd.getFullFieldName() + 
                                            " has @Convert annotation for attribute " + convAttrName + " but this is not yet fully supported. Ignored");
                                    }
                                }
                            }
                        }
                    }
                }

                // Post-processing to apply JPA rules for field relationships etc
                if (oneToMany && mmd.getJoinMetaData() == null && mmd.getMappedBy() == null)
                {
                    if (columnMetaData != null)
                    {
                        // 1-N FK UNI since JoinColumn specified and no JoinTable
                    }
                    else
                    {
                        // 1-N with no join specified and unidirectional so JPA says it has to be via join (no 1-N uni FKs)
                        joinmd = new JoinMetaData();
                        mmd.setJoinMetaData(joinmd);
                    }
                }
                if (manyToMany && mmd.getJoinMetaData() == null && mmd.getMappedBy() == null)
                {
                    // M-N with no join specified and unidir so add the join for them
                    joinmd = new JoinMetaData();
                    mmd.setJoinMetaData(joinmd);
                }

                if (mmd.getOrderMetaData() == null && Collection.class.isAssignableFrom(member.getType()))
                {
                    // @OrderBy not specified but is a Collection so use ordering of element using PK field(s)
                    OrderMetaData ordmd = new OrderMetaData();
                    ordmd.setOrdering("#PK"); // Special value recognised by OrderMetaData
                    mmd.setOrderMetaData(ordmd);
                }

                if (columnMetaData == null)
                {
                    // Column specified (at least in part) via @Column/@Lob/@Enumerated/@Temporal
                    ColumnMetaData colmd = newColumnMetaData(mmd, member, annotations);
                    if (colmd != null)
                    {
                        columnMetaData = new ColumnMetaData[1];
                        columnMetaData[0] = colmd;
                    }
                }

                if (columnMetaData != null)
                {
                    // Check if this is marked as an element collection
                    boolean elementCollection = false;
                    for (AnnotationObject annotation : annotations)
                    {
                        if (annotation.getName().equals(JPAAnnotationUtils.ELEMENT_COLLECTION) && mmd.getTypeConverterName() == null)
                        {
                            // Not being converted, so treat as column info for the element (of collection) or value (of map)
                            elementCollection = true;
                            break;
                        }
                    }

                    // Column definition provided so apply to the respective place
                    if ((mmd.hasCollection() || mmd.hasArray()) && joinmd == null)
                    {
                        // Column is for the FK of the element of the collection/array
                        elemmd = mmd.getElementMetaData();
                        if (elemmd == null)
                        {
                            elemmd = new ElementMetaData();
                            mmd.setElementMetaData(elemmd);
                        }
                        if (columnTable != null)
                        {
                            elemmd.setTable(columnTable);
                        }
                        for (int i=0;i<columnMetaData.length;i++)
                        {
                            elemmd.addColumn(columnMetaData[i]);
                        }
                    }
                    else if (mmd.hasMap() && joinmd == null)
                    {
                        // Column is for the FK value of the map
                        valmd = mmd.getValueMetaData();
                        if (valmd == null)
                        {
                            valmd = new ValueMetaData();
                            mmd.setValueMetaData(valmd);
                        }
                        if (columnTable != null)
                        {
                            valmd.setTable(columnTable);
                        }
                        for (int i=0;i<columnMetaData.length;i++)
                        {
                            valmd.addColumn(columnMetaData[i]);
                        }
                    }
                    else if (elementCollection)
                    {
                        // Column is for element/value column(s) of join table of 1-N of non-PCs
                        if (mmd.hasCollection() || mmd.hasArray())
                        {
                            elemmd = mmd.getElementMetaData();
                            if (elemmd == null)
                            {
                                elemmd = new ElementMetaData();
                                mmd.setElementMetaData(elemmd);
                            }
                            for (int i=0;i<columnMetaData.length;i++)
                            {
                                elemmd.addColumn(columnMetaData[i]);
                            }
                        }
                        else if (mmd.hasMap())
                        {
                            valmd = mmd.getValueMetaData();
                            if (valmd == null)
                            {
                                valmd = new ValueMetaData();
                                mmd.setValueMetaData(valmd);
                            }
                            for (int i=0;i<columnMetaData.length;i++)
                            {
                                valmd.addColumn(columnMetaData[i]);
                            }
                        }
                    }
                    else
                    {
                        // Column is for the member
                        for (int i=0;i<columnMetaData.length;i++)
                        {
                            mmd.addColumn(columnMetaData[i]);
                        }
                    }
                }
            }
            return mmd;
        }

        return null;
    }

    /**
     * Method to process the override of embedded members.
     * Can recurse if the overriddenName uses "dot" syntax.
     * @param mmd Metadata for this member
     * @param overriddenName The overridden member within this embedded object
     * @param type Type of this member
     * @param column The column details to override it with
     */
    protected void processEmbeddedAttributeOverride(AbstractMemberMetaData mmd, String overriddenName, Class type, Column column)
    {
        String overrideName = overriddenName;

        // Get the EmbeddedMetaData, creating as necessary
        EmbeddedMetaData embmd = null;
        if (mmd.hasCollection())
        {
            // Embedded 1-N
            type = clr.classForName(mmd.getCollection().getElementType()); // Update to the element type
            ElementMetaData elemmd = mmd.getElementMetaData();
            if (elemmd == null)
            {
                elemmd = new ElementMetaData();
                mmd.setElementMetaData(elemmd);
            }
            embmd = elemmd.getEmbeddedMetaData();
            if (embmd == null)
            {
                embmd = elemmd.newEmbeddedMetaData();
            }
        }
        else if (mmd.hasMap())
        {
            if (overrideName.startsWith("key."))
            {
                overrideName = overrideName.substring(4);
                type = clr.classForName(mmd.getMap().getKeyType()); // Update to the key type
                KeyMetaData keymd = mmd.getKeyMetaData();
                if (keymd == null)
                {
                    keymd = new KeyMetaData();
                    mmd.setKeyMetaData(keymd);
                }
                embmd = keymd.getEmbeddedMetaData();
                if (embmd == null)
                {
                    embmd = keymd.newEmbeddedMetaData();
                }
            }
            else if (overrideName.startsWith("value."))
            {
                overrideName = overrideName.substring(6);
                type = clr.classForName(mmd.getMap().getValueType()); // Update to the value type
                ValueMetaData valmd = mmd.getValueMetaData();
                if (valmd == null)
                {
                    valmd = new ValueMetaData();
                    mmd.setValueMetaData(valmd);
                }
                embmd = valmd.getEmbeddedMetaData();
                if (embmd == null)
                {
                    embmd = valmd.newEmbeddedMetaData();
                }
            }
            else
            {
                NucleusLogger.METADATA.warn("Attempt to override '" + overriddenName + "' on Map field. Should prefix key field(s) with 'key.' and value fields with 'value.'");
                return;
            }
        }
        else
        {
            // Embedded 1-1
            embmd = mmd.getEmbeddedMetaData();
            if (embmd == null)
            {
                mmd.setEmbedded(true);
                embmd = new EmbeddedMetaData();
                embmd.setParent(mmd);
            }
            mmd.setEmbeddedMetaData(embmd);
        }

        if (overrideName.indexOf('.') > 0)
        {
            int position = overrideName.indexOf('.');
            String baseMemberName = overrideName.substring(0, position);
            String nestedMemberName = overrideName.substring(position+1);
            AbstractMemberMetaData ammd = null;

            // Try as field
            try
            {
                Field overrideMember = type.getDeclaredField(baseMemberName);
                ammd = new FieldMetaData(embmd, baseMemberName);
                type = overrideMember.getType();
            }
            catch (Exception e)
            {
            }
            if (ammd == null)
            {
                // Try as property
                try
                {
                    Method overrideMember = type.getDeclaredMethod(ClassUtils.getJavaBeanGetterName(baseMemberName, false));
                    ammd = new FieldMetaData(embmd, baseMemberName);
                    type = overrideMember.getReturnType();
                }
                catch (Exception e)
                {
                }
            }
            if (ammd == null)
            {
                throw new NucleusException("Cannot obtain override field/property "+
                        overrideName + " of class " + type + " for persistent class " + mmd.getClassName(true));
            }

            embmd.addMember(ammd);
            ammd.setParent(embmd);

            // Recurse to nested field type
            processEmbeddedAttributeOverride(ammd, nestedMemberName, type, column);
        }
        else
        {
            Member overriddenMember = null;
            java.lang.reflect.Member overrideMember = null;
            AbstractMemberMetaData ammd = null;

            // Try as field
            try
            {
                overrideMember = type.getDeclaredField(overrideName);
                overriddenMember = new Member((Field)overrideMember);
                ammd = new FieldMetaData(embmd, overrideName);
            }
            catch (Exception e)
            {
            }

            if (ammd == null)
            {
                // Try as property
                try
                {
                    overrideMember = type.getDeclaredMethod(ClassUtils.getJavaBeanGetterName(overrideName, false));
                    overriddenMember = new Member((Method)overrideMember);
                    ammd = new PropertyMetaData(embmd, overrideName);
                }
                catch (Exception e)
                {
                }
            }

            if (ammd == null)
            {
                throw new NucleusException("Cannot obtain override field/property "+
                        overrideName + " of class " + type + " for persistent class " + mmd.getClassName(true));
            }

            embmd.addMember(ammd);
            ammd.addColumn(JPAAnnotationUtils.getColumnMetaDataForColumnAnnotation(ammd, overriddenMember, column));
        }
    }

    /**
     * Method to take the passed in outline ClassMetaData and process the annotations for
     * method adding any necessary MetaData to the ClassMetaData.
     * @param cmd The ClassMetaData (to be updated)
     * @param method The method
     */
    protected void processMethodAnnotations(AbstractClassMetaData cmd, Method method)
    {
        Annotation[] annotations = method.getAnnotations();
        if (annotations != null && annotations.length > 0)
        {
            EventListenerMetaData elmd = cmd.getListenerForClass(cmd.getFullClassName());
            for (Annotation annotation : annotations)
            {
                String annotationTypeName = annotation.annotationType().getName();
                if (annotationTypeName.equals(PrePersist.class.getName()) ||
                    annotationTypeName.equals(PostPersist.class.getName()) ||
                    annotationTypeName.equals(PreRemove.class.getName()) ||
                    annotationTypeName.equals(PostRemove.class.getName()) ||
                    annotationTypeName.equals(PreUpdate.class.getName()) ||
                    annotationTypeName.equals(PostUpdate.class.getName()) ||
                    annotationTypeName.equals(PostLoad.class.getName()))
                {
                    if (elmd == null)
                    {
                        elmd = new EventListenerMetaData(cmd.getFullClassName());
                        cmd.addListener(elmd);
                    }
                    elmd.addCallback(annotationTypeName, method.getDeclaringClass().getName(), method.getName());
                }
            }
        }
    }

    /**
     * Method to create a new field/property MetaData for the supplied annotations.
     * @param cmd MetaData for the class
     * @param field The field/method
     * @param annotations Annotations for the field/property
     * @return The MetaData for the field/property
     */
    private AbstractMemberMetaData newMetaDataForMember(AbstractClassMetaData cmd, Member field, AnnotationObject[] annotations)
    {
        // Create the member meta-data
        AbstractMemberMetaData mmd = (field.isProperty()) ? new PropertyMetaData(cmd, field.getName()) : new FieldMetaData(cmd, field.getName());

        FieldPersistenceModifier modifier = null;
        Class targetEntity = null;

        for (AnnotationObject annotation : annotations)
        {
            String annName = annotation.getName();
            Map<String, Object> annotationValues = annotation.getNameValueMap();

            if (annName.equals(JPAAnnotationUtils.EMBEDDED))
            {
                mmd.setEmbedded(true);
                setCascadesOnMember(mmd, new CascadeType[]{CascadeType.ALL});
            }
            else if (annName.equals(JPAAnnotationUtils.ID))
            {
                mmd.setPrimaryKey(true);

                if (modifier == null)
                {
                    modifier = FieldPersistenceModifier.PERSISTENT;
                }
            }
            else if (annName.equals(JPAAnnotationUtils.TRANSIENT))
            {
                modifier = FieldPersistenceModifier.NONE;
            }
            else if (annName.equals(JPAAnnotationUtils.ENUMERATED))
            {
                if (modifier == null)
                {
                    modifier = FieldPersistenceModifier.PERSISTENT;
                }
            }
            else if (annName.equals(JPAAnnotationUtils.VERSION))
            {
                // Tag this field as the version field
                VersionMetaData vermd = cmd.newVersionMetadata();
                vermd.setStrategy(VersionStrategy.VERSION_NUMBER).setFieldName(mmd.getName());

                if (modifier == null)
                {
                    modifier = FieldPersistenceModifier.PERSISTENT;
                }
            }
            else if (annName.equals(JPAAnnotationUtils.EMBEDDED_ID))
            {
                mmd.setPrimaryKey(true);
                mmd.setEmbedded(true);
                if (modifier == null)
                {
                    modifier = FieldPersistenceModifier.PERSISTENT;
                }
            }
            else if (annName.equals(JPAAnnotationUtils.BASIC))
            {
                FetchType fetch = (FetchType)annotationValues.get("fetch");
                mmd.setDefaultFetchGroup(fetch == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);

                modifier = FieldPersistenceModifier.PERSISTENT;
                if (!field.getType().isPrimitive())
                {
                    boolean optional = (Boolean)annotationValues.get("optional");
                    String nullValue = optional ? "none" : "exception";
                    mmd.setNullValue(NullValue.getNullValue(nullValue));
                }
            }
            else if (annName.equals(JPAAnnotationUtils.ONE_TO_ONE))
            {
                // 1-1 relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                mmd.setMappedBy((String)annotationValues.get("mappedBy"));
                mmd.setRelationTypeString("OneToOne");
                mmd.setCascadeRemoveOrphans((Boolean)annotationValues.get("orphanRemoval"));
                setCascadesOnMember(mmd, (CascadeType[])annotationValues.get("cascade"));
                mmd.setDefaultFetchGroup(annotationValues.get("fetch") == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);
                mmd.setNullValue(NullValue.getNullValue((Boolean)annotationValues.get("optional") ? "none" : "exception"));

                if (StringUtils.isWhitespace(mmd.getMappedBy()))
                {
                    // Default to UNIQUE constraint on the FK
                    mmd.setUnique(true);
                }

                targetEntity = (Class)annotationValues.get("targetEntity");
            }
            else if (annName.equals(JPAAnnotationUtils.ONE_TO_MANY))
            {
                // 1-N relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                mmd.setMappedBy((String)annotationValues.get("mappedBy"));
                mmd.setRelationTypeString("OneToMany");
                mmd.setCascadeRemoveOrphans((Boolean)annotationValues.get("orphanRemoval"));
                setCascadesOnMember(mmd, (CascadeType[])annotationValues.get("cascade"));
                mmd.setDefaultFetchGroup(annotationValues.get("fetch") == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);

                targetEntity = (Class)annotationValues.get("targetEntity");
            }
            else if (annName.equals(JPAAnnotationUtils.MANY_TO_MANY))
            {
                // M-N relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                mmd.setMappedBy((String)annotationValues.get("mappedBy"));
                mmd.setRelationTypeString("ManyToMany");
                setCascadesOnMember(mmd, (CascadeType[])annotationValues.get("cascade"));
                mmd.setDefaultFetchGroup(annotationValues.get("fetch") == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);

                targetEntity = (Class)annotationValues.get("targetEntity");
            }
            else if (annName.equals(JPAAnnotationUtils.MANY_TO_ONE))
            {
                // N-1 relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                mmd.setMappedBy((String)annotationValues.get("mappedBy"));
                mmd.setRelationTypeString("ManyToOne");
                setCascadesOnMember(mmd, (CascadeType[])annotationValues.get("cascade"));
                mmd.setDefaultFetchGroup(annotationValues.get("fetch") == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);
                mmd.setNullValue(NullValue.getNullValue((Boolean)annotationValues.get("optional") ? "none" : "exception"));

                targetEntity = (Class)annotationValues.get("targetEntity");
            }
            else if (annName.equals(JPAAnnotationUtils.MAPS_ID))
            {
                mmd.setMapsIdAttribute((String)annotationValues.get("value"));
            }
            else if (annName.equals(JPAAnnotationUtils.ELEMENT_COLLECTION))
            {
                // 1-N NonPC relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                setCascadesOnMember(mmd, new CascadeType[]{CascadeType.ALL});
                mmd.setDefaultFetchGroup(annotationValues.get("fetch") == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);

                if (mmd.getJoinMetaData() == null)
                {
                    JoinMetaData joinmd = new JoinMetaData();
                    mmd.setJoinMetaData(joinmd);
                }

                targetEntity = (Class)annotationValues.get("targetClass");
            }
            else if (annName.equals(JPAAnnotationUtils.GENERATED_VALUE))
            {
                GenerationType type = (GenerationType) annotationValues.get("strategy");
                String valueStrategy = JPAAnnotationUtils.getValueGenerationStrategyString(type);
                String valueGenerator = (String) annotationValues.get("generator");
                if (valueStrategy != null)
                {
                    mmd.setValueStrategy(valueStrategy);
                    if (valueGenerator != null)
                    {
                        mmd.setSequence(valueGenerator);
                        mmd.setValueGeneratorName(valueGenerator);
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.LOB))
            {
                mmd.setStoreInLob();
                modifier = FieldPersistenceModifier.PERSISTENT;
            }
            else if (annName.equals(JPAAnnotationUtils.EXTENSION))
            {
                mmd.addExtension((String)annotationValues.get("key"), (String)annotationValues.get("value"));
            }
            else if (annName.equals(JPAAnnotationUtils.EXTENSIONS))
            {
                Extension[] values = (Extension[])annotationValues.get("value");
                if (values != null && values.length > 0)
                {
                    for (Extension ext : values)
                    {
                        mmd.addExtension(ext.key().toString(), ext.value().toString());
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.SEQUENCE_GENERATOR))
            {
                // Sequence generator, so store it against the package that we are under
                processSequenceGeneratorAnnotation(cmd.getPackageMetaData(), annotationValues);
            }
            else if (annName.equals(JPAAnnotationUtils.TABLE_GENERATOR))
            {
                // Table generator, so store it against the package that we are under
                processTableGeneratorAnnotation(cmd.getPackageMetaData(), annotationValues);
            }
        }

        if (mmgr.getApiAdapter().isMemberDefaultPersistent(field.getType()) && modifier == null)
        {
            modifier = FieldPersistenceModifier.PERSISTENT;
        }
        if (modifier != null)
        {
            mmd.setPersistenceModifier(modifier);
        }

        cmd.addMember(mmd);

        // If the field is a container then add its container element
        ContainerHandler containerHandler = mmgr.getNucleusContext().getTypeManager().getContainerHandler(field.getType());
        ContainerMetaData contmd = null;
        if (containerHandler != null)
        {
            contmd = containerHandler.newMetaData();
        }

        if (contmd instanceof CollectionMetaData)
        {
            String elementType = null;
            if (targetEntity != null && targetEntity != void.class)
            {
                elementType = targetEntity.getName();
            }
            if (elementType == null)
            {
                Class elType = ClassUtils.getCollectionElementType(field.getType(), field.getGenericType());
                elementType = (elType != null ? elType.getName() : null);
            }
            // No annotation for collections so can't specify the element type, dependent, embedded, serialized

            ((CollectionMetaData)contmd).setElementType(elementType);
        }
        else if (contmd instanceof MapMetaData)
        {
            Class keyCls = ClassUtils.getMapKeyType(field.getType(), field.getGenericType());
            String keyType = (keyCls != null ? keyCls.getName() : null);
            String valueType = null;
            if (targetEntity != null && targetEntity != void.class)
            {
                valueType = targetEntity.getName();
            }
            if (valueType == null)
            {
                Class valueCls = ClassUtils.getMapValueType(field.getType(), field.getGenericType());
                valueType = (valueCls != null ? valueCls.getName() : null);
            }

            // No annotation for maps so can't specify the key/value type, dependent, embedded, serialized
            contmd = new MapMetaData();
            MapMetaData mapmd = (MapMetaData)contmd;
            mapmd.setKeyType(keyType);
            mapmd.setValueType(valueType);
        }
        else if (contmd instanceof ArrayMetaData)
        {
            String elementType = null;
            if (targetEntity != null && targetEntity != void.class)
            {
                elementType = targetEntity.getName();
            }
            if (elementType == null)
            {
                // TODO Support generics?
                elementType = field.getType().getComponentType().getName();
            }
            // No annotation for arrays so can't specify the element type, dependent, embedded, serialized

            ((ArrayMetaData)contmd).setElementType(elementType);
        }

        if (contmd != null)
        {
            mmd.setContainer(contmd);
        }

        return mmd;
    }

    private void setCascadesOnMember(AbstractMemberMetaData mmd, CascadeType[] cascades)
    {
        if (cascades != null)
        {
            for (int i = 0; i < cascades.length; i++)
            {
                if (cascades[i] == CascadeType.ALL)
                {
                    mmd.setCascadePersist(true);
                    mmd.setCascadeUpdate(true);
                    mmd.setCascadeDelete(true);
                    mmd.setCascadeDetach(true);
                    mmd.setCascadeRefresh(true);
                }
                else if (cascades[i] == CascadeType.PERSIST)
                {
                    mmd.setCascadePersist(true);
                }
                else if (cascades[i] == CascadeType.MERGE)
                {
                    mmd.setCascadeUpdate(true);
                }
                else if (cascades[i] == CascadeType.REMOVE)
                {
                    mmd.setCascadeDelete(true);
                }
                else if (cascades[i] == CascadeType.REFRESH)
                {
                    mmd.setCascadeRefresh(true);
                }
                else if (cascades[i] == CascadeType.DETACH)
                {
                    mmd.setCascadeDetach(true);
                }
            }
        }
    }

    /**
     * Method to create a new ColumnMetaData.
     * TODO the fieldType logic, like setting a length based on the type, should be done only after loading all metadata, 
     * otherwise it can cause a different behaviour based on the loading order of the annotations !!!!
     * @param parent The parent MetaData object
     * @param fieldType The field/property type
     * @param annotationValues Annotations on this field/property
     * @return MetaData for the column
     */
    private ColumnMetaData newColumnMetaDataForAnnotation(MetaData parent, Class fieldType, Map<String, Object> annotationValues)
    {
        String columnName = null;
        String target = null;
        String targetField = null;
        String jdbcType = null;
        String sqlType = null;
        String typePrecision = null;
        String typeLength = null;
        String typeScale = null;
        String allowsNull = null;
        String defaultValue = null;
        String insertValue = null;
        String insertable = null;
        String updateable = null;
        String unique = null;
        String table = null;
        String columnDdl = null;

        columnName = (String)annotationValues.get("name");

        // Take values of length, precision, scale when not set to JPA annotation default
        if (annotationValues.get("length") != null)
        {
            int lengthValue = ((Integer)annotationValues.get("length")).intValue();
            if (lengthValue != 255)
            {
                typeLength = "" + lengthValue;
            }
        }
        if (annotationValues.get("precision") != null)
        {
            int precisionValue = ((Integer)annotationValues.get("precision")).intValue();
            if (precisionValue != 0)
            {
                typePrecision = "" + precisionValue;
            }
        }
        if (annotationValues.get("scale") != null)
        {
            int scaleValue = ((Integer)annotationValues.get("scale")).intValue();
            if (scaleValue != 0)
            {
                typeScale = "" + scaleValue;
            }
        }

        if (fieldType == char.class || fieldType == Character.class)
        {
            // Char field needs to have length of 1 (JPA TCK)
            jdbcType = "CHAR";
            typeLength = "1";
        }
        else if (fieldType == boolean.class || fieldType == Boolean.class)
        {
            if (mmgr.getNucleusContext().getConfiguration().getBooleanProperty("datanucleus.jpa.legacy.mapBooleanToSmallint", false))
            {
                // NOTE : This was present for up to DN 4.0 but now only available via property since not found a reason for it
                String memberName = "unknown";
                if (parent instanceof AbstractMemberMetaData)
                {
                    memberName = ((AbstractMemberMetaData)parent).getFullFieldName();
                }
                NucleusLogger.METADATA.info("Member " + memberName + " has column of type " + fieldType.getName() + 
                        " and being mapped to SMALLINT JDBC type. This is deprecated and could be removed in the future. Use @JdbcType instead");
                jdbcType = "SMALLINT";
            }
        }

        if (annotationValues.get("nullable") != null)
        {
            allowsNull = annotationValues.get("nullable").toString();
        }
        if (annotationValues.get("insertable") != null)
        {
            insertable = annotationValues.get("insertable").toString();
        }
        if (annotationValues.get("updatable") != null)
        {
            // Note : "updatable" is spelt incorrectly in the JPA spec.
            updateable = annotationValues.get("updatable").toString();
        }
        if (annotationValues.get("unique") != null)
        {
            unique = annotationValues.get("unique").toString();
        }
        if (annotationValues.get("table") != null)
        {
            // Column in secondary-table
            String columnTable = (String)annotationValues.get("table");
            if (!StringUtils.isWhitespace(columnTable))
            {
                table = columnTable;
            }
        }

        String tmp = (String)annotationValues.get("columnDefinition");
        if (!StringUtils.isWhitespace(tmp))
        {
            columnDdl = tmp;
        }

        // Set length/scale based on the field type
        String length = null;
        String scale = null;
        if (Enum.class.isAssignableFrom(fieldType))
        {
            // Ignore scale on Enum
            if (jdbcType != null && jdbcType.equals("VARCHAR"))
            {
                length = typeLength;
            }
            else if (typePrecision != null)
            {
                length = typePrecision;
            }
        }
        else
        {
            // TODO Arguably we also should allow specification of length when using a converter to a String
            if (fieldType == String.class || fieldType == Character.class || fieldType == char.class || fieldType == StringBuilder.class || fieldType == StringBuffer.class)
            {
                length = typeLength;
            }
            else if (fieldType == Float.class || fieldType == float.class || fieldType == Double.class || fieldType == double.class || fieldType == BigDecimal.class)
            {
                // Floating point types use precision/scale
                length = typePrecision;
            }
            else
            {
                // Other types. Note that some may not want a length
                length = (typePrecision != null ? typePrecision : typeLength);
            }

            scale = typeScale;
        }

        if (columnName == null && length == null && scale == null && insertable == null && updateable == null && allowsNull == null && unique == null && jdbcType == null)
        {
            // Nothing specified so don't provide ColumnMetaData and default to what we get
            return null;
        }

        ColumnMetaData colmd = new ColumnMetaData();
        colmd.setName(columnName);
        colmd.setTarget(target);
        colmd.setTargetMember(targetField);
        colmd.setJdbcType(jdbcType);
        colmd.setSqlType(sqlType);
        colmd.setLength(length);
        colmd.setScale(scale);
        colmd.setAllowsNull(allowsNull);
        colmd.setDefaultValue(defaultValue);
        colmd.setInsertValue(insertValue);
        colmd.setInsertable(insertable);
        colmd.setUpdateable(updateable);
        colmd.setUnique(unique);
        if (columnDdl != null)
        {
            // TODO Extract length etc out of DDL?
            colmd.setColumnDdl(columnDdl);
        }
        if (parent instanceof AbstractMemberMetaData)
        {
            AbstractMemberMetaData apmd = (AbstractMemberMetaData) parent;
            if (!StringUtils.isWhitespace(table))
            {
                apmd.setTable(table);
            }
            // apmd.addColumn(colmd);
            // update column settings if primary key, cannot be null
            if (apmd.isPrimaryKey())
            {
                colmd.setAllowsNull(false);
            }
        }
        else if (parent instanceof KeyMetaData)
        {
            KeyMetaData keymd = (KeyMetaData) parent;
            AbstractMemberMetaData mmd = (AbstractMemberMetaData) keymd.getParent();
            if (!StringUtils.isWhitespace(table))
            {
                mmd.setTable(table);
            }
//            colmd.setAllowsNull(colmd.isAllowsNull());
        }
        return colmd;
    }

    /**
     * Method to create a new ColumnMetaData for a member.
     * TODO the fieldType logic, like setting a length based on the type, should be done only after loading all metadata, 
     * otherwise it can cause a different behaviour based on the loading order of the annotations !!!!
     * @param mmd The member MetaData
     * @param member The field/property
     * @param annotations Annotations on this field/property
     * @return MetaData for the column
     */
    private ColumnMetaData newColumnMetaData(AbstractMemberMetaData mmd, Member member, AnnotationObject[] annotations)
    {
        Class fieldType = member.getType();
        if (mmd.getJoinMetaData() != null && !mmd.isSerialized())
        {
            if (mmd.hasCollection() && Collection.class.isAssignableFrom(fieldType))
            {
                // Column for a Collection<NonPC> ?
                fieldType = ClassUtils.getCollectionElementType(fieldType, member.getGenericType());
                if (fieldType == null)
                {
                    fieldType = clr.classForName(mmd.getCollection().getElementType());
                }
            }
            else if (mmd.hasMap() && Map.class.isAssignableFrom(fieldType))
            {
                // Column for a Map<?,NonPC> ?
                fieldType = ClassUtils.getMapValueType(fieldType, member.getGenericType());
                if (fieldType == null)
                {
                    fieldType = clr.classForName(mmd.getMap().getValueType());
                }
            }
            else if (mmd.hasArray() && fieldType.isArray())
            {
                // Column for an array of NonPC using join table
                fieldType = fieldType.getComponentType();
                if (fieldType == null)
                {
                    fieldType = clr.classForName(mmd.getArray().getElementType());
                }
            }
        }

        String columnName = null;
        String target = null;
        String targetField = null;
        String jdbcType = null;
        String sqlType = null;
        String typePrecision = null;
        String typeLength = null;
        String typeScale = null;
        String allowsNull = null;
        String defaultValue = null;
        String insertValue = null;
        String insertable = null;
        String updateable = null;
        String unique = null;
        String table = null;
        String columnDdl = null;

        for (int i=0;annotations != null && i<annotations.length;i++)
        {
            String annName = annotations[i].getName();
            Map<String, Object> annotationValues = annotations[i].getNameValueMap();
            if (annName.equals(JPAAnnotationUtils.COLUMN))
            {
                columnName = (String)annotationValues.get("name");

                // Take values of length, precision, scale when not set to JPA annotation default
                if (annotationValues.get("length") != null)
                {
                    int lengthValue = ((Integer)annotationValues.get("length")).intValue();
                    if (lengthValue != 255)
                    {
                        typeLength = "" + lengthValue;
                    }
                }
                if (annotationValues.get("precision") != null)
                {
                    int precisionValue = ((Integer)annotationValues.get("precision")).intValue();
                    if (precisionValue != 0)
                    {
                        typePrecision = "" + precisionValue;
                    }
                }
                if (annotationValues.get("scale") != null)
                {
                    int scaleValue = ((Integer)annotationValues.get("scale")).intValue();
                    if (scaleValue != 0)
                    {
                        typeScale = "" + scaleValue;
                    }
                }

                if (fieldType == char.class || fieldType == Character.class)
                {
                    // Char field needs to have length of 1 (JPA TCK)
                    jdbcType = "CHAR";
                    typeLength = "1";
                }
                else if (fieldType == boolean.class || fieldType == Boolean.class)
                {
                    if (mmgr.getNucleusContext().getConfiguration().getBooleanProperty("datanucleus.jpa.legacy.mapBooleanToSmallint", false))
                    {
                        // NOTE : This was present for up to DN 4.0 but now only available via property since not found a reason for it
                        String memberName = mmd.getFullFieldName();
                        NucleusLogger.METADATA.info("Member " + memberName + " has column of type " + fieldType.getName() + 
                            " and being mapped to SMALLINT JDBC type. This is deprecated and could be removed in the future. Use @JdbcType instead");
                        jdbcType = "SMALLINT";
                    }
                }

                if (annotationValues.get("nullable") != null)
                {
                    allowsNull = annotationValues.get("nullable").toString();
                }
                if (annotationValues.get("insertable") != null)
                {
                    insertable = annotationValues.get("insertable").toString();
                }
                if (annotationValues.get("updatable") != null)
                {
                    // Note : "updatable" is spelt incorrectly in the JPA spec.
                    updateable = annotationValues.get("updatable").toString();
                }
                if (annotationValues.get("unique") != null)
                {
                    unique = annotationValues.get("unique").toString();
                }
                if (annotationValues.get("table") != null)
                {
                    // Column in secondary-table
                    String columnTable = (String)annotationValues.get("table");
                    if (!StringUtils.isWhitespace(columnTable))
                    {
                        table = columnTable;
                    }
                }

                String tmp = (String)annotationValues.get("columnDefinition");
                if (!StringUtils.isWhitespace(tmp))
                {
                    columnDdl = tmp;
                }
            }
            else if (Enum.class.isAssignableFrom(fieldType) && annName.equals(JPAAnnotationUtils.ENUMERATED))
            {
                EnumType type = (EnumType)annotationValues.get("value");
                jdbcType = (type == EnumType.STRING ? "VARCHAR" : "INTEGER");
            }
            else if (JPAAnnotationUtils.isTemporalType(fieldType) && annName.equals(JPAAnnotationUtils.TEMPORAL))
            {
                TemporalType type = (TemporalType)annotationValues.get("value");
                if (type == TemporalType.DATE)
                {
                    jdbcType = "DATE";
                }
                else if (type == TemporalType.TIME)
                {
                    jdbcType = "TIME";
                }
                else if (type == TemporalType.TIMESTAMP)
                {
                    jdbcType = "TIMESTAMP";
                }
            }
        }

        // Set length/scale based on the field type
        String length = null;
        String scale = null;
        if (Enum.class.isAssignableFrom(fieldType))
        {
            // Ignore scale on Enum
            if (jdbcType != null && jdbcType.equals("VARCHAR"))
            {
                length = typeLength;
            }
            else if (typePrecision != null)
            {
                length = typePrecision;
            }
        }
        else
        {
            // TODO Arguably we also should allow specification of length when using a converter to a String
            if (fieldType == String.class || fieldType == Character.class || fieldType == char.class || fieldType == StringBuilder.class || fieldType == StringBuffer.class)
            {
                length = typeLength;
            }
            else if (fieldType == Float.class || fieldType == float.class || fieldType == Double.class || fieldType == double.class || fieldType == BigDecimal.class)
            {
                // Floating point types use precision
                length = typePrecision;
            }
            else
            {
                // Other types. Note that some may not want a length
                length = (typePrecision != null ? typePrecision : typeLength);
            }

            scale = typeScale;
        }

        if (columnName == null && length == null && scale == null && insertable == null && updateable == null && allowsNull == null && unique == null && jdbcType == null)
        {
            // Nothing specified so don't provide ColumnMetaData and default to what we get
            return null;
        }

        ColumnMetaData colmd = new ColumnMetaData();
        colmd.setName(columnName);
        colmd.setTarget(target);
        colmd.setTargetMember(targetField);
        colmd.setJdbcType(jdbcType);
        colmd.setSqlType(sqlType);
        colmd.setLength(length);
        colmd.setScale(scale);
        colmd.setAllowsNull(allowsNull);
        colmd.setDefaultValue(defaultValue);
        colmd.setInsertValue(insertValue);
        colmd.setInsertable(insertable);
        colmd.setUpdateable(updateable);
        colmd.setUnique(unique);
        if (columnDdl != null)
        {
            // TODO Extract length etc out of DDL?
            colmd.setColumnDdl(columnDdl);
        }

        if (!StringUtils.isWhitespace(table))
        {
            mmd.setTable(table);
        }
        // mmd.addColumn(colmd);
        // update column settings if primary key, cannot be null
        if (mmd.isPrimaryKey())
        {
            colmd.setAllowsNull(false);
        }

        return colmd;
    }

    /**
     * Process a @SequenceGenerator annotation.
     * @param pmd Package MetaData to add the sequence to
     * @param annotationValues The annotation info
     */
    private void processSequenceGeneratorAnnotation(PackageMetaData pmd, Map<String, Object> annotationValues)
    {
        // Sequence generator, so store it against the package that we are under
        String name = (String)annotationValues.get("name");
        String seqName = (String)annotationValues.get("sequenceName");
        // Don't apply default for sequenceName if not provided. This is done by the individual sequence generators
        Integer initialValue = (Integer)annotationValues.get("initialValue");
        if (initialValue == null)
        {
            initialValue = Integer.valueOf(1); // JPA default
        }
        Integer allocationSize = (Integer)annotationValues.get("allocationSize");
        if (allocationSize == null)
        {
            allocationSize = Integer.valueOf(50); // JPA default
        }
        SequenceMetaData seqmd = pmd.newSequenceMetadata(name, null);
        seqmd.setDatastoreSequence(seqName);
        seqmd.setInitialValue(initialValue.intValue());
        seqmd.setAllocationSize(allocationSize.intValue());

        String catalogName = (String)annotationValues.get("catalog");
        if (!StringUtils.isWhitespace(catalogName))
        {
            seqmd.setCatalogName(catalogName);
        }
        String schemaName = (String)annotationValues.get("schema");
        if (!StringUtils.isWhitespace(schemaName))
        {
            seqmd.setSchemaName(schemaName);
        }
    }

    /**
     * Process a @TableGenerator annotation and add it to the specified package MetaData.
     * @param pmd Package MetaData to add the table generator to
     * @param annotationValues The annotation info
     */
    private void processTableGeneratorAnnotation(PackageMetaData pmd, Map<String, Object> annotationValues)
    {
        TableGeneratorMetaData tgmd = pmd.newTableGeneratorMetadata((String)annotationValues.get("name"));
        tgmd.setTableName((String)annotationValues.get("table"));
        tgmd.setCatalogName((String)annotationValues.get("catalog"));
        tgmd.setSchemaName((String)annotationValues.get("schema"));
        tgmd.setPKColumnName((String)annotationValues.get("pkColumnName"));
        tgmd.setPKColumnValue((String)annotationValues.get("pkColumnValue"));
        tgmd.setValueColumnName((String)annotationValues.get("valueColumnName"));
        tgmd.setInitialValue((Integer)annotationValues.get("initialValue"));
        tgmd.setAllocationSize((Integer)annotationValues.get("allocationSize"));
        // TODO Support uniqueConstraints
    }

    /**
     * Check if a class is persistable, by looking at its annotations.
     * @param annotations Annotations for the class
     * @return The annotation marking this class as an entity (or null if none present)
     */
    protected AnnotationObject isClassPersistable(AnnotationObject[] annotations)
    {
        for (AnnotationObject annotation : annotations)
        {
            if (JPAAnnotationUtils.ENTITY.equals(annotation.getName()))
            {
                return annotation;
            }
            else if (JPAAnnotationUtils.EMBEDDABLE.equals(annotation.getName()))
            {
                return annotation;
            }
            else if (JPAAnnotationUtils.MAPPED_SUPERCLASS.equals(annotation.getName()))
            {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Check if class is persistence aware, by looking at its annotations.
     * @param annotations Annotations for the class
     * @return true if the class has @PersistenceAware
     */
    protected boolean isClassPersistenceAware(AnnotationObject[] annotations)
    {
        for (AnnotationObject annotation : annotations)
        {
            if (JPAAnnotationUtils.PERSISTENCE_AWARE.equals(annotation.getName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if class has NamedXXXQuery annotations (for classes that are not persistable but provide named query definitions.
     * @param annotations Annotations for the class
     * @return true if the class has Named query annotations
     */
    protected boolean doesClassHaveNamedQueries(AnnotationObject[] annotations)
    {
        for (AnnotationObject annotation : annotations)
        {
            String annClassName = annotation.getName();
            if (annClassName.equals(JPAAnnotationUtils.NAMED_QUERIES) ||
                annClassName.equals(JPAAnnotationUtils.NAMED_QUERY) || 
                annClassName.equals(JPAAnnotationUtils.NAMED_NATIVE_QUERIES) ||
                annClassName.equals(JPAAnnotationUtils.NAMED_NATIVE_QUERY) ||
                annClassName.equals(JPAAnnotationUtils.NAMED_STOREDPROC_QUERIES) ||
                annClassName.equals(JPAAnnotationUtils.NAMED_STOREDPROC_QUERY))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if class has Converter annotation by inspecting its annotations.
     * @param cls The class
     * @param annotations Annotations for the class
     * @return true if the class has Converter annotations
     */
    protected boolean doesClassHaveConverter(Class cls, AnnotationObject[] annotations)
    {
        for (AnnotationObject annotation : annotations)
        {
            String annClassName = annotation.getName();
            if (annClassName.equals(JPAAnnotationUtils.CONVERTER))
            {
                Map<String, Object> annotationValues = annotation.getNameValueMap();
                boolean autoApply = (Boolean) annotationValues.get("autoApply");

                TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();
                Class attrType = JPATypeConverterUtils.getAttributeTypeForAttributeConverter(cls, null);
                Class dbType = JPATypeConverterUtils.getDatabaseTypeForAttributeConverter(cls, attrType, null);
                if (attrType != null)
                {
                    // Register the TypeConverter under the name of the AttributeConverter class
                    TypeConverter typeConv = typeMgr.getTypeConverterForName(cls.getName());
                    if (typeConv == null)
                    {
                        // Not yet cached an instance of this converter so create one
                        typeConv = new JPATypeConverter(JPATypeConverterUtils.createAttributeConverterInstance(mmgr.getNucleusContext(), cls));
                        typeMgr.registerConverter(cls.getName(), typeConv, attrType, dbType, autoApply, attrType.getName());
                    }
                    else
                    {
                        // Update the "autoApply" in case we simply registered the converter for a member
                        typeMgr.registerConverter(cls.getName(), typeConv, attrType, dbType, autoApply, attrType.getName());
                    }
                    if (NucleusLogger.METADATA.isDebugEnabled())
                    {
                        NucleusLogger.METADATA.debug("Registering AttributeConverter for java=" + attrType.getName() + " db=" + dbType.getName() + " autoApply=" + autoApply);
                    }
                }

                return true;
            }
        }
        return false;
    }
}