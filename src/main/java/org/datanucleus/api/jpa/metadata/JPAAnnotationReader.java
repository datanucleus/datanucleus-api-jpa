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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.datanucleus.metadata.IdentityMetaData;
import org.datanucleus.metadata.IdentityStrategy;
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

        // We support JPA and DataNucleus annotations in this reader
        setSupportedAnnotationPackages(new String[] {"javax.persistence", "org.datanucleus"});
    }

    /**
     * Method to process the "class" level annotations and create the outline ClassMetaData object
     * @param pmd Parent PackageMetaData
     * @param cls The class
     * @param annotations Annotations for this class
     * @param clr ClassLoader resolver
     * @return The ClassMetaData (or null if no annotations)
     */
    protected AbstractClassMetaData processClassAnnotations(PackageMetaData pmd, Class cls, 
            AnnotationObject[] annotations, ClassLoaderResolver clr)
    {
        this.clr = clr;
        ClassMetaData cmd = null;

        if (annotations != null && annotations.length > 0)
        {
            if (isClassPersistable(cls))
            {
                cmd = pmd.newClassMetadata(ClassUtils.getClassNameForClass(cls));
                cmd.setPersistenceModifier(ClassPersistenceModifier.PERSISTENCE_CAPABLE);
            }
            else if (isClassPersistenceAware(cls))
            {
                cmd = pmd.newClassMetadata(ClassUtils.getClassNameForClass(cls));
                cmd.setPersistenceModifier(ClassPersistenceModifier.PERSISTENCE_AWARE);
            }
            else if (doesClassHaveNamedQueries(cls))
            {
                cmd = pmd.newClassMetadata(ClassUtils.getClassNameForClass(cls));
                cmd.setPersistenceModifier(ClassPersistenceModifier.NON_PERSISTENT);
            }
            else if (doesClassHaveConverter(cls))
            {
                // Converter has now been processed so just return
                return null;
            }
            else
            {
                return null;
            }

            processNamedQueries(cmd, annotations);
            if (cmd.getPersistenceModifier() != ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                // Class is either not persistent, or just hasnamed queries
                return cmd;
            }

            IdentityType identityType = IdentityType.APPLICATION;
            String identityColumn = null;
            String identityStrategy = null;
            String identityGenerator = null;

            String cacheable = "true";
            String requiresExtent = "true";
            String detachable = "true"; // In JPA default is true.
            String embeddedOnly = "false";
            String idClassName = null;
            String catalog = null;
            String schema = null;
            String table = null;
            String inheritanceStrategyForTree = null;
            String inheritanceStrategy = null;
            String discriminatorColumnName = null;
            String discriminatorColumnType = null;
            Integer discriminatorColumnLength = null;
            String discriminatorColumnDdl = null;
            String discriminatorValue = null;
            String entityName = null;
            Class[] entityListeners = null;
            boolean excludeSuperClassListeners = false;
            boolean excludeDefaultListeners = false;
            ColumnMetaData[] pkColumnMetaData = null;
            Set<UniqueMetaData> uniques = null;
            Set<IndexMetaData> indexes = null;
            Set<AbstractMemberMetaData> overriddenFields = null;
            List<QueryResultMetaData> resultMappings = null;
            Map<String,String> extensions = null;

            for (int i=0;i<annotations.length;i++)
            {
                Map<String, Object> annotationValues = annotations[i].getNameValueMap();
                String annName = annotations[i].getName();
                if (annName.equals(JPAAnnotationUtils.ENTITY))
                {
                    entityName = (String) annotationValues.get("name");
                    if (entityName == null || entityName.length() == 0)
                    {
                        entityName = ClassUtils.getClassNameForClass(cls);
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.MAPPED_SUPERCLASS))
                {
                    cmd.setMappedSuperclass(true);
                    if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
                    {
                        inheritanceStrategy = InheritanceStrategy.SUBCLASS_TABLE.toString();
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.DATASTORE_IDENTITY))
                {
                    // extension to allow datastore-identity
                    identityType = IdentityType.DATASTORE;
                    identityColumn = (String)annotationValues.get("column");
                    GenerationType type = (GenerationType) annotationValues.get("generationType");
                    identityStrategy = JPAAnnotationUtils.getIdentityStrategyString(type);
                    identityGenerator = (String) annotationValues.get("generator");
                }
                else if (annName.equals(JPAAnnotationUtils.NONDURABLE_IDENTITY))
                {
                    // extension to allow nondurable-identity
                    identityType = IdentityType.NONDURABLE;
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
                    table = (String)annotationValues.get("name");
                    catalog = (String)annotationValues.get("catalog");
                    schema = (String)annotationValues.get("schema");
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
                            if (uniques == null)
                            {
                                uniques = new HashSet<UniqueMetaData>();
                            }
                            uniques.add(unimd);
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
                                // TODO Support ASC|DESC that can be placed after a column name
                                for (int k=0;k<cols.length;k++)
                                {
                                    idxmd.addColumn(cols[k]);
                                }
                            }
                            if (indexConstrs[j].unique())
                            {
                                idxmd.setUnique(true);
                            }
                            if (indexes == null)
                            {
                                indexes = new HashSet<IndexMetaData>();
                            }
                            indexes.add(idxmd);
                        }
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.ID_CLASS))
                {
                    idClassName = ((Class)annotationValues.get("value")).getName();
                }
                else if (annName.equals(JPAAnnotationUtils.INHERITANCE))
                {
                    // Only valid in the root class
                    InheritanceType inhType = (InheritanceType)annotationValues.get("strategy");
                    inheritanceStrategyForTree = inhType.toString();
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
                }
                else if (annName.equals(JPAAnnotationUtils.DISCRIMINATOR_COLUMN))
                {
                    discriminatorColumnName = (String)annotationValues.get("name");
                    DiscriminatorType type = (DiscriminatorType)annotationValues.get("discriminatorType");
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
                    discriminatorColumnLength = (Integer)annotationValues.get("length");
                    String tmp = (String)annotationValues.get("columnDefinition");
                    if (!StringUtils.isWhitespace(tmp))
                    {
                        discriminatorColumnDdl = tmp;
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.DISCRIMINATOR_VALUE))
                {
                    discriminatorValue = (String)annotationValues.get("value");
                }
                else if (annName.equals(JPAAnnotationUtils.EMBEDDABLE))
                {
                    embeddedOnly = "true";
                    identityType = IdentityType.NONDURABLE;
                }
                else if (annName.equals(JPAAnnotationUtils.CACHEABLE))
                {
                    Boolean cacheableVal = (Boolean)annotationValues.get("value");
                    if (cacheableVal == Boolean.FALSE)
                    {
                        cacheable = "false";
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.ENTITY_LISTENERS))
                {
                    entityListeners = (Class[])annotationValues.get("value");
                }
                else if (annName.equals(JPAAnnotationUtils.EXCLUDE_SUPERCLASS_LISTENERS))
                {
                    excludeSuperClassListeners = true;
                }
                else if (annName.equals(JPAAnnotationUtils.EXCLUDE_DEFAULT_LISTENERS))
                {
                    excludeDefaultListeners = true;
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
                    pkColumnMetaData = new ColumnMetaData[1];
                    pkColumnMetaData[0] = new ColumnMetaData();
                    pkColumnMetaData[0].setName((String)annotationValues.get("name"));
                    pkColumnMetaData[0].setTarget((String)annotationValues.get("referencedColumnName"));
                    ForeignKey fk = (ForeignKey) annotationValues.get("foreignKey");
                    if (fk != null && fk.value() != ConstraintMode.PROVIDER_DEFAULT)
                    {
                        ForeignKeyMetaData fkmd = cmd.newForeignKeyMetadata();
                        fkmd.setName(fk.name());
                        fkmd.setFkDefinition(fk.foreignKeyDefinition());
                        fkmd.addColumn(pkColumnMetaData[0]);
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
                    pkColumnMetaData = new ColumnMetaData[values.length];
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
                    for (int j=0;j<values.length;j++)
                    {
                        pkColumnMetaData[j] = new ColumnMetaData();
                        pkColumnMetaData[j].setName(values[j].name());
                        pkColumnMetaData[j].setTarget(values[j].referencedColumnName());
                        if (!StringUtils.isWhitespace(values[j].columnDefinition()))
                        {
                            pkColumnMetaData[j].setColumnDdl(values[j].columnDefinition());
                        }
                        if (fkmd != null)
                        {
                            fkmd.addColumn(pkColumnMetaData[j]);
                        }
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.ATTRIBUTE_OVERRIDES))
                {
                    AttributeOverride[] overrides = (AttributeOverride[])annotationValues.get("value");
                    if (overrides != null)
                    {
                        if (overriddenFields == null)
                        {
                            overriddenFields = new HashSet<AbstractMemberMetaData>();
                        }

                        for (int j=0;j<overrides.length;j++)
                        {
                            AbstractMemberMetaData fmd = new FieldMetaData(cmd, "#UNKNOWN." + overrides[j].name());
                            fmd.setPersistenceModifier(FieldPersistenceModifier.PERSISTENT);
                            Column col = overrides[j].column();
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
                            overriddenFields.add(fmd);
                        }
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.ATTRIBUTE_OVERRIDE))
                {
                    if (overriddenFields == null)
                    {
                        overriddenFields = new HashSet<AbstractMemberMetaData>();
                    }

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
                    overriddenFields.add(fmd);
                }
                else if (annName.equals(JPAAnnotationUtils.ASSOCIATION_OVERRIDES))
                {
                    AssociationOverride[] overrides = (AssociationOverride[])annotationValues.get("value");
                    if (overrides != null)
                    {
                        if (overriddenFields == null)
                        {
                            overriddenFields = new HashSet<AbstractMemberMetaData>();
                        }

                        for (int j=0;j<overrides.length;j++)
                        {
                            AbstractMemberMetaData fmd = new FieldMetaData(cmd, "#UNKNOWN." + overrides[j].name());
                            JoinColumn[] cols = overrides[j].joinColumns();
                            for (int k=0;k<cols.length;k++)
                            {
                                ColumnMetaData colmd = new ColumnMetaData();
                                colmd.setName(cols[k].name());
                                colmd.setTarget(cols[k].referencedColumnName());
                                colmd.setAllowsNull(cols[k].nullable());
                                colmd.setInsertable(cols[k].insertable());
                                colmd.setUpdateable(cols[k].updatable());
                                colmd.setUnique(cols[k].unique());
                                fmd.addColumn(colmd);
                            }
                            overriddenFields.add(fmd);
                        }
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.ASSOCIATION_OVERRIDE))
                {
                    if (overriddenFields == null)
                    {
                        overriddenFields = new HashSet<AbstractMemberMetaData>();
                    }

                    AbstractMemberMetaData fmd = new FieldMetaData(cmd, "#UNKNOWN." + (String)annotationValues.get("name"));
                    JoinColumn[] cols = (JoinColumn[])annotationValues.get("joinColumns");
                    for (int k=0;k<cols.length;k++)
                    {
                        ColumnMetaData colmd = new ColumnMetaData();
                        colmd.setName(cols[k].name());
                        colmd.setTarget(cols[k].referencedColumnName());
                        colmd.setAllowsNull(cols[k].nullable());
                        colmd.setInsertable(cols[k].insertable());
                        colmd.setUpdateable(cols[k].updatable());
                        colmd.setUnique(cols[k].unique());
                        fmd.addColumn(colmd);
                    }
                    overriddenFields.add(fmd);
                }
                else if (annName.equals(JPAAnnotationUtils.SQL_RESULTSET_MAPPINGS))
                {
                    SqlResultSetMapping[] mappings = (SqlResultSetMapping[])annotationValues.get("value");
                    if (resultMappings == null)
                    {
                        resultMappings = new ArrayList<QueryResultMetaData>();
                    }

                    for (int j=0;j<mappings.length;j++)
                    {
                        QueryResultMetaData qrmd = new QueryResultMetaData(mappings[j].name());
                        EntityResult[] entityResults = mappings[j].entities();
                        if (entityResults != null && entityResults.length > 0)
                        {
                            for (int k=0;k<entityResults.length;k++)
                            {
                                String entityClassName = entityResults[k].entityClass().getName();
                                qrmd.addPersistentTypeMapping(entityClassName, null, entityResults[k].discriminatorColumn());
                                FieldResult[] fields = entityResults[k].fields();
                                if (fields != null)
                                {
                                    for (int l=0;l<fields.length;l++)
                                    {
                                        qrmd.addMappingForPersistentTypeMapping(entityClassName, fields[l].name(), fields[l].column());
                                    }
                                }
                            }
                        }
                        ColumnResult[] colResults = mappings[j].columns();
                        if (colResults != null && colResults.length > 0)
                        {
                            for (int k=0;k<colResults.length;k++)
                            {
                                qrmd.addScalarColumn(colResults[k].name());
                            }
                        }

                        ConstructorResult[] ctrResults = mappings[j].classes();
                        if (ctrResults != null && ctrResults.length > 0)
                        {
                            for (int k=0;k<ctrResults.length;k++)
                            {
                                String ctrClassName = ctrResults[k].targetClass().getName();
                                List<ConstructorTypeColumn> ctrCols = null;
                                ColumnResult[] cols = ctrResults[k].columns();
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

                        resultMappings.add(qrmd);
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.SQL_RESULTSET_MAPPING))
                {
                    if (resultMappings == null)
                    {
                        resultMappings = new ArrayList<QueryResultMetaData>();
                    }

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

                    resultMappings.add(qrmd);
                }
                else if (annName.equals(JPAAnnotationUtils.SECONDARY_TABLES))
                {
                    // processed below in newJoinMetaData
                }
                else if (annName.equals(JPAAnnotationUtils.SECONDARY_TABLE))
                {
                    // processed below in newJoinMetaData
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
                            graphName = entityName;
                        }
                        JPAEntityGraph eg = new JPAEntityGraph(mgr, graphName, cls);
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
                        ((JPAMetaDataManager)mgr).registerEntityGraph(eg);
                    }
                }
                else if (annName.equals(JPAAnnotationUtils.NAMED_ENTITY_GRAPH))
                {
                    String graphName = (String)annotationValues.get("name");
                    if (StringUtils.isWhitespace(graphName))
                    {
                        graphName = entityName;
                    }
                    JPAEntityGraph eg = new JPAEntityGraph(mgr, graphName, cls);
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
                    ((JPAMetaDataManager)mgr).registerEntityGraph(eg);
                }
                else if (annName.equals(JPAAnnotationUtils.EXTENSION))
                {
                    // extension
                    if (extensions == null)
                    {
                        extensions = new HashMap<String,String>(1);
                    }
                    extensions.put((String)annotationValues.get("key"), (String)annotationValues.get("value"));
                }
                else if (annName.equals(JPAAnnotationUtils.EXTENSIONS))
                {
                    // extension
                    Extension[] values = (Extension[])annotationValues.get("value");
                    if (values != null && values.length > 0)
                    {
                        if (extensions == null)
                        {
                            extensions = new HashMap<String,String>(values.length);
                        }
                        for (int j=0;j<values.length;j++)
                        {
                            extensions.put(values[j].key().toString(), values[j].value().toString());
                        }
                    }
                }
                else
                {
                    NucleusLogger.METADATA.debug(Localiser.msg("044203", cls.getName(), annotations[i].getName()));
                }
            }

            if (entityName == null || entityName.length() == 0)
            {
                entityName = ClassUtils.getClassNameForClass(cls);
            }

            NucleusLogger.METADATA.debug(Localiser.msg("044200", cls.getName(), "JPA"));

            cmd.setTable(table);
            cmd.setCatalog(catalog);
            cmd.setSchema(schema);
            cmd.setEntityName(entityName);
            cmd.setDetachable(detachable);
            cmd.setRequiresExtent(requiresExtent);
            cmd.setObjectIdClass(idClassName);
            cmd.setEmbeddedOnly(embeddedOnly);
            cmd.setCacheable(cacheable);
            cmd.setIdentityType(identityType);
            if (excludeSuperClassListeners)
            {
                cmd.excludeSuperClassListeners();
            }
            if (excludeDefaultListeners)
            {
                cmd.excludeDefaultListeners();
            }
            if (entityListeners != null)
            {
                for (int i=0; i<entityListeners.length; i++)
                {
                    // Any EventListener will not have their callback methods registered at this point
                    EventListenerMetaData elmd = new EventListenerMetaData(entityListeners[i].getName());
                    cmd.addListener(elmd);
                }
            }

            // Inheritance
            InheritanceMetaData inhmd = null;
            if (inheritanceStrategy != null)
            {
                // Strategy specified so create inheritance data
                inhmd = cmd.newInheritanceMetadata().setStrategy(inheritanceStrategy);
                inhmd.setStrategyForTree(inheritanceStrategyForTree);
            }
            else if (inheritanceStrategyForTree != null)
            {
                // Strategy for tree defined, so add inheritance metadata to store this info
                inhmd = cmd.newInheritanceMetadata();
                inhmd.setStrategyForTree(inheritanceStrategyForTree);
            }

            if (discriminatorValue != null || discriminatorColumnName != null || discriminatorColumnLength != null || discriminatorColumnType != null)
            {
                if (inhmd == null)
                {
                    // No inheritance specified, but discriminator is, so add empty inheritance metadata to store discriminator info
                    inhmd = cmd.newInheritanceMetadata();
                }

                // Add discriminator information to the inheritance of this class
                DiscriminatorMetaData dismd = inhmd.newDiscriminatorMetadata();
                if (discriminatorValue != null)
                {
                    // Value specified so assumed to be value-map
                    dismd.setColumnName(discriminatorColumnName);
                    dismd.setValue(discriminatorValue).setStrategy(DiscriminatorStrategy.VALUE_MAP).setIndexed("true");
                }
                else
                {
                    if (mgr.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_USE_DISCRIMINATOR_DEFAULT_CLASS_NAME))
                    {
                        // Legacy handling, DN <= 5.0.2
                        if (!Modifier.isAbstract(cls.getModifiers()))
                        {
                            // No value and concrete class so use class-name
                            discriminatorValue = cls.getName();
                            dismd.setValue(discriminatorValue);
                        }
                        dismd.setColumnName(discriminatorColumnName);
                        dismd.setStrategy(DiscriminatorStrategy.CLASS_NAME).setIndexed("true");
                    }
                    else
                    {
                        dismd.setColumnName(discriminatorColumnName);
                        dismd.setStrategy(DiscriminatorStrategy.ENTITY_NAME).setIndexed("true");
                    }
                }

                ColumnMetaData discolmd = null;
                if (discriminatorColumnLength != null || discriminatorColumnName != null || discriminatorColumnType != null)
                {
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

            if (identityType == IdentityType.DATASTORE)
            {
                // extension - datastore-identity
                IdentityMetaData idmd = cmd.newIdentityMetadata();
                idmd.setColumnName(identityColumn);
                idmd.setValueStrategy(IdentityStrategy.getIdentityStrategy(identityStrategy));
                if (identityGenerator != null)
                {
                    idmd.setSequence(identityGenerator);
                    idmd.setValueGeneratorName(identityGenerator);
                }
            }

            if (pkColumnMetaData != null)
            {
                // PK columns overriding those in the root class
                PrimaryKeyMetaData pkmd = cmd.newPrimaryKeyMetadata();
                for (int i=0;i<pkColumnMetaData.length;i++)
                {
                    pkmd.addColumn(pkColumnMetaData[i]);
                }
            }
            if (uniques != null && !uniques.isEmpty())
            {
                // Unique constraints for the primary/secondary tables
                Iterator<UniqueMetaData> uniquesIter = uniques.iterator();
                while (uniquesIter.hasNext())
                {
                    cmd.addUniqueConstraint(uniquesIter.next());
                }
            }
            if (indexes != null && !indexes.isEmpty())
            {
                Iterator<IndexMetaData> indexesIter = indexes.iterator();
                while (indexesIter.hasNext())
                {
                    cmd.addIndex(indexesIter.next());
                }
            }

            if (overriddenFields != null)
            {
                // Fields overridden from superclasses
                Iterator<AbstractMemberMetaData> iter = overriddenFields.iterator();
                while (iter.hasNext())
                {
                    cmd.addMember(iter.next());
                }
            }
            if (resultMappings != null)
            {
                Iterator<QueryResultMetaData> iter = resultMappings.iterator();
                while (iter.hasNext())
                {
                    cmd.addQueryResultMetaData(iter.next());
                }
            }
            if (extensions != null)
            {
                cmd.addExtensions(extensions);
            }

            // Process any secondary tables
            newJoinMetaDataForClass(cmd, annotations);
        }

        return cmd;
    }

    /**
     * Convenience method to process NamedQuery, NamedQueries, NamedNativeQuery, NamedNativeQueries,
     * NamedStoredProcedureQueries, NamedStoredProcedureQuery annotations.
     * @param cmd Metadata for the class
     * @param annotations Annotations specified on the class
     */
    protected void processNamedQueries(AbstractClassMetaData cmd, AnnotationObject[] annotations)
    {
        Set<QueryMetaData> namedQueries = null;
        Set<StoredProcQueryMetaData> namedStoredProcQueries = null;

        for (int i=0;i<annotations.length;i++)
        {
            Map<String, Object> annotationValues = annotations[i].getNameValueMap();
            String annName = annotations[i].getName();
            if (annName.equals(JPAAnnotationUtils.NAMED_QUERIES))
            {
                NamedQuery[] queries = (NamedQuery[])annotationValues.get("value");
                if (namedQueries == null)
                {
                    namedQueries = new HashSet<QueryMetaData>();
                }
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
                    namedQueries.add(qmd);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_QUERY))
            {
                if (namedQueries == null)
                {
                    namedQueries = new HashSet<QueryMetaData>();
                }
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
                namedQueries.add(qmd);
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_NATIVE_QUERIES))
            {
                NamedNativeQuery[] queries = (NamedNativeQuery[])annotationValues.get("value");
                if (namedQueries == null)
                {
                    namedQueries = new HashSet<QueryMetaData>();
                }
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
                    namedQueries.add(qmd);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_NATIVE_QUERY))
            {
                if (namedQueries == null)
                {
                    namedQueries = new HashSet<QueryMetaData>();
                }

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
                namedQueries.add(qmd);
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_STOREDPROC_QUERIES))
            {
                NamedStoredProcedureQuery[] procs = (NamedStoredProcedureQuery[])annotationValues.get("value");
                if (namedStoredProcQueries == null)
                {
                    namedStoredProcQueries = new HashSet<StoredProcQueryMetaData>();
                }
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

                    namedStoredProcQueries.add(spqmd);
                }
            }
            else if (annName.equals(JPAAnnotationUtils.NAMED_STOREDPROC_QUERY))
            {
                if (namedStoredProcQueries == null)
                {
                    namedStoredProcQueries = new HashSet<StoredProcQueryMetaData>();
                }

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
                namedStoredProcQueries.add(spqmd);
            }
        }

        if (namedQueries != null)
        {
            // Add any named queries that we found
            Iterator<QueryMetaData> iter = namedQueries.iterator();
            while (iter.hasNext())
            {
                cmd.addQuery(iter.next());
            }
        }

        if (namedStoredProcQueries != null)
        {
            // Add any named stored procedures that we found
            Iterator<StoredProcQueryMetaData> iter = namedStoredProcQueries.iterator();
            while (iter.hasNext())
            {
                cmd.addStoredProcQuery(iter.next());
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
     * @param propertyAccessor if has persistent properties
     * @return The FieldMetaData/PropertyMetaData that was added (if any)
     */
    protected AbstractMemberMetaData processMemberAnnotations(AbstractClassMetaData cmd, Member member, AnnotationObject[] annotations, boolean propertyAccessor)
    {
        if (Modifier.isTransient(member.getModifiers()))
        {
            // Field is transient so nothing to persist
            return null;
        }

        if (member.getName().startsWith(mgr.getEnhancedMethodNamePrefix()))
        {
            // ignore enhanced fields/methods added during enhancement
            return null;
        }

        if ((annotations != null && annotations.length > 0) || mgr.getApiAdapter().isMemberDefaultPersistent(member.getType()))
        {
        	// TODO Move this logic to the calling code (AbstractAnnotationReader)
            if (!member.isProperty() && (annotations == null || annotations.length == 0) && propertyAccessor)
            {
                return null;
            }
            if (member.isProperty() && (annotations == null || annotations.length == 0) && !propertyAccessor)
            {
                // field accessor will ignore all methods not annotated
                return null;
            }

            // Create the Field/Property MetaData so we have something to add to
            AbstractMemberMetaData mmd = newMetaDataForMember(cmd, member, annotations);

            // Check if this is marked as an element collection
            boolean elementCollection = false;
            if (annotations != null)
            {
                for (int i=0;i<annotations.length;i++)
                {
                    String annName = annotations[i].getName();
                    if (annName.equals(JPAAnnotationUtils.ELEMENT_COLLECTION))
                    {
                        elementCollection = true;
                        break;
                    }
                }
            }

            // Process other annotations
            ColumnMetaData[] columnMetaData = null;
            String columnTable = null;
            JoinMetaData joinmd = null;
            ElementMetaData elemmd = null;
            KeyMetaData keymd = null;
            ValueMetaData valmd = null;
            boolean oneToMany = false;
            boolean manyToMany = false;
            for (int i=0;annotations != null && i<annotations.length;i++)
            {
                String annName = annotations[i].getName();
                Map<String, Object> annotationValues = annotations[i].getNameValueMap();
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
                                // TODO Support ASC|DESC that can be placed after a column name
                                for (int k=0;k<cols.length;k++)
                                {
                                    idxmd.addColumn(cols[k]);
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
                                // TODO Support ASC|DESC that can be placed after a column name
                                for (int k=0;k<cols.length;k++)
                                {
                                    idxmd.addColumn(cols[k]);
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
                    // TODO Support this
                    NucleusLogger.METADATA.debug("We do not currently support @MapKeyJoinColumns");
                }
                else if (annName.equals(JPAAnnotationUtils.MAP_KEY_JOIN_COLUMN))// TODO Also support MapKeyJoinColumns
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
                        throw new NucleusException("@OrderBy found yet field=" + cmd.getFullClassName() + "." + member.getName() +
                            " already has ordering information!");
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
                        throw new NucleusException("@OrderColumn found yet field=" + cmd.getFullClassName() + "." + member.getName() +
                            " already has ordering information!");
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
                    // Multiple @Convert annotations (for embedded field)
                    Convert[] converts = (Convert[])annotationValues.get("value");
                    if (converts == null || converts.length == 0)
                    {
                        // Do nothing
                    }
                    else if (converts.length > 1)
                    {
                        NucleusLogger.METADATA.warn("Dont currently support @Converts annotation for embedded fields");
                    }
                    else if (converts.length == 1)
                    {
                        Class converterCls = converts[0].converter();
                        String convAttrName = converts[0].attributeName();
                        Boolean disable = converts[0].disableConversion();
                        if (disable == Boolean.TRUE)
                        {
                            mmd.setTypeConverterDisabled();
                        }
                        else
                        {
                            TypeManager typeMgr = mgr.getNucleusContext().getTypeManager();
                            if (typeMgr.getTypeConverterForName(converterCls.getName()) == null)
                            {
                                // Not yet cached an instance of this converter so create one
                                // TODO Support injectable AttributeConverters
                                AttributeConverter entityConv = JPATypeConverterUtils.createAttributeConverterInstance(converterCls);

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
                else if (annName.equals(JPAAnnotationUtils.CONVERT))
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
                        TypeManager typeMgr = mgr.getNucleusContext().getTypeManager();
                        TypeConverter conv = typeMgr.getTypeConverterForName(converterCls.getName());
                        if (typeMgr.getTypeConverterForName(converterCls.getName()) == null)
                        {
                            // Not yet cached an instance of this converter so create one
                            // TODO Support injectable AttributeConverters
                            AttributeConverter entityConv = JPATypeConverterUtils.createAttributeConverterInstance(converterCls);

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
            for (int i=0; i<annotations.length; i++)
            {
                String annotationTypeName = annotations[i].annotationType().getName();
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
        FieldPersistenceModifier modifier = null;
        Boolean dfg = null;
        Boolean embedded = null;
        Boolean pk = null;
        String version = null;
        String nullValue = null;
        String mappedBy = null;
        boolean mapsId = false;
        String mapsIdAttribute = null;
        boolean orphanRemoval = false;
        CascadeType[] cascades = null;
        Map<String, String> extensions = null;
        String valueStrategy = null;
        String valueGenerator = null;
        boolean storeInLob = false;
        Class targetEntity = null;
        boolean addJoin = false;
        boolean unique = false;
        String relationTypeString = null;

        for (int i=0;annotations != null && i<annotations.length;i++)
        {
            String annName = annotations[i].getName();
            Map<String, Object> annotationValues = annotations[i].getNameValueMap();
            if (annName.equals(JPAAnnotationUtils.EMBEDDED))
            {
                embedded = Boolean.TRUE;
                cascades = new CascadeType[] {CascadeType.ALL};
            }
            else if (annName.equals(JPAAnnotationUtils.ID))
            {
                pk = Boolean.TRUE;
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
                version = "true";
                if (modifier == null)
                {
                    modifier = FieldPersistenceModifier.PERSISTENT;
                }
            }
            else if (annName.equals(JPAAnnotationUtils.EMBEDDED_ID))
            {
                pk = Boolean.TRUE;
                embedded = Boolean.TRUE;
                if (modifier == null)
                {
                    modifier = FieldPersistenceModifier.PERSISTENT;
                }
            }
            else if (annName.equals(JPAAnnotationUtils.BASIC))
            {
                FetchType fetch = (FetchType)annotationValues.get("fetch");
                dfg = (fetch == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);
                modifier = FieldPersistenceModifier.PERSISTENT;
                if (!field.getType().isPrimitive())
                {
                    boolean optional = (Boolean)annotationValues.get("optional");
                    if (!optional)
                    {
                        nullValue = "exception";
                    }
                    else
                    {
                        nullValue = "none";
                    }
                }
            }
            else if (annName.equals(JPAAnnotationUtils.ONE_TO_ONE))
            {
                // 1-1 relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                mappedBy = (String)annotationValues.get("mappedBy");
                cascades = (CascadeType[])annotationValues.get("cascade");
                targetEntity = (Class)annotationValues.get("targetEntity");
                FetchType fetch = (FetchType)annotationValues.get("fetch");
                dfg = (fetch == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);
                boolean optional = (Boolean)annotationValues.get("optional");
                if (!optional)
                {
                    nullValue = "exception";
                }
                else
                {
                    nullValue = "none";
                }
                orphanRemoval = (Boolean)annotationValues.get("orphanRemoval");
                if (StringUtils.isWhitespace(mappedBy))
                {
                    // Default to UNIQUE constraint on the FK
                    unique = true;
                }
                relationTypeString = "OneToOne";
            }
            else if (annName.equals(JPAAnnotationUtils.ONE_TO_MANY))
            {
                // 1-N relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                mappedBy = (String)annotationValues.get("mappedBy");
                cascades = (CascadeType[])annotationValues.get("cascade");
                targetEntity = (Class)annotationValues.get("targetEntity");
                FetchType fetch = (FetchType)annotationValues.get("fetch");
                dfg = (fetch == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);
                orphanRemoval = (Boolean)annotationValues.get("orphanRemoval");
                relationTypeString = "OneToMany";
            }
            else if (annName.equals(JPAAnnotationUtils.MANY_TO_MANY))
            {
                // M-N relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                mappedBy = (String)annotationValues.get("mappedBy");
                cascades = (CascadeType[])annotationValues.get("cascade");
                targetEntity = (Class)annotationValues.get("targetEntity");
                FetchType fetch = (FetchType)annotationValues.get("fetch");
                dfg = (fetch == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);
                relationTypeString = "ManyToMany";
            }
            else if (annName.equals(JPAAnnotationUtils.MANY_TO_ONE))
            {
                // N-1 relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                mappedBy = (String)annotationValues.get("mappedBy");
                cascades = (CascadeType[])annotationValues.get("cascade");
                targetEntity = (Class)annotationValues.get("targetEntity");
                FetchType fetch = (FetchType)annotationValues.get("fetch");
                dfg = (fetch == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);
                boolean optional = (Boolean)annotationValues.get("optional");
                if (!optional)
                {
                    nullValue = "exception";
                }
                else
                {
                    nullValue = "none";
                }
                relationTypeString = "ManyToOne";
            }
            else if (annName.equals(JPAAnnotationUtils.MAPS_ID))
            {
                mapsIdAttribute = (String)annotationValues.get("value");
                mapsId = true;
            }
            else if (annName.equals(JPAAnnotationUtils.ELEMENT_COLLECTION))
            {
                // 1-N NonPC relation
                modifier = FieldPersistenceModifier.PERSISTENT;
                targetEntity = (Class)annotationValues.get("targetClass");
                addJoin = true;
                FetchType fetch = (FetchType)annotationValues.get("fetch");
                dfg = (fetch == FetchType.LAZY ? Boolean.FALSE : Boolean.TRUE);
                cascades = new CascadeType[1];
                cascades[0] = CascadeType.ALL;
            }
            else if (annName.equals(JPAAnnotationUtils.GENERATED_VALUE))
            {
                GenerationType type = (GenerationType) annotationValues.get("strategy");
                valueStrategy = JPAAnnotationUtils.getIdentityStrategyString(type);
                valueGenerator = (String) annotationValues.get("generator");
            }
            else if (annName.equals(JPAAnnotationUtils.LOB))
            {
                storeInLob = true;
                modifier = FieldPersistenceModifier.PERSISTENT;
            }
            else if (annName.equals(JPAAnnotationUtils.EXTENSION))
            {
                if (extensions == null)
                {
                    extensions = new HashMap<String,String>(1);
                }
                extensions.put((String)annotationValues.get("key"), (String)annotationValues.get("value"));
            }
            else if (annName.equals(JPAAnnotationUtils.EXTENSIONS))
            {
                Extension[] values = (Extension[])annotationValues.get("value");
                if (values != null && values.length > 0)
                {
                    if (extensions == null)
                    {
                        extensions = new HashMap<String,String>(values.length);
                    }
                    for (int j=0;j<values.length;j++)
                    {
                        extensions.put(values[j].key().toString(), values[j].value().toString());
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

        if (mgr.getApiAdapter().isMemberDefaultPersistent(field.getType()) && modifier == null)
        {
            modifier = FieldPersistenceModifier.PERSISTENT;
        }

        // Create the field
        AbstractMemberMetaData mmd;
        if (field.isProperty())
        {
            mmd = new PropertyMetaData(cmd, field.getName());
        }
        else
        {
            mmd = new FieldMetaData(cmd, field.getName());
        }
        if (relationTypeString != null)
        {
            mmd.setRelationTypeString(relationTypeString);
        }
        if (modifier != null)
        {
            mmd.setPersistenceModifier(modifier);
        }
        if (pk != null)
        {
            mmd.setPrimaryKey(pk);
        }
        if (dfg != null)
        {
            mmd.setDefaultFetchGroup(dfg);
        }
        if (embedded != null)
        {
            mmd.setEmbedded(embedded);
        }
        mmd.setNullValue(NullValue.getNullValue(nullValue));
        mmd.setMappedBy(mappedBy);

        if (version != null)
        {
            // Tag this field as the version field
            VersionMetaData vermd = cmd.newVersionMetadata();
            vermd.setStrategy(VersionStrategy.VERSION_NUMBER).setFieldName(mmd.getName());
        }

        cmd.addMember(mmd);

        if (orphanRemoval)
        {
            mmd.setCascadeRemoveOrphans(true);
        }
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

        // Value generation
        if (valueStrategy != null && valueGenerator != null)
        {
            mmd.setSequence(valueGenerator);
            mmd.setValueGeneratorName(valueGenerator);
        }
        if (valueStrategy != null)
        {
            mmd.setValueStrategy(valueStrategy);
        }

        // Type storage
        if (storeInLob)
        {
            mmd.setStoreInLob();
        }
        if (unique)
        {
            mmd.setUnique(unique);
        }
        
        TypeManager typeManager = mgr.getNucleusContext().getTypeManager();
        ContainerHandler containerHandler = typeManager.getContainerHandler(field.getType());
        
        // If the field is a container then add its container element
        ContainerMetaData contmd = null;
        
        if ( containerHandler != null )
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
            // No annotation for collections so cant specify the element type, dependent, embedded, serialized

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

            // No annotation for maps so cant specify the key/value type, dependent, embedded, serialized
            contmd = new MapMetaData();
            MapMetaData mapmd = (MapMetaData)contmd;
            mapmd.setKeyType(keyType);
            mapmd.setValueType(valueType);
        }
        
        if (contmd != null)
        {
            mmd.setContainer(contmd);
        }
        if (mapsId)
        {
            mmd.setMapsIdAttribute(mapsIdAttribute);
        }

        if (addJoin)
        {
            if (mmd.getJoinMetaData() == null)
            {
                JoinMetaData joinmd = new JoinMetaData();
                mmd.setJoinMetaData(joinmd);
            }
        }

        // Extensions
        if (extensions != null)
        {
            mmd.addExtensions(extensions);
        }

        return mmd;
    }

    /**
     * Method to create a new ColumnMetaData.
     * TODO !!!! the fieldType logic, like setting a length based on the type, should be done only after loading all metadata, 
     * otherwise it can cause a different behavior based on the loading order of the annotations !!!!
     * @param parent The parent MetaData object
     * @param field The field/property
     * @param annotations Annotations on this field/property
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
        typeLength = "" + annotationValues.get("length");
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
            if (mgr.getNucleusContext().getConfiguration().getBooleanProperty("datanucleus.jpa.legacy.mapBooleanToSmallint", false))
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
            if (String.class.isAssignableFrom(fieldType) || fieldType == Character.class || fieldType == char.class)
            {
                length = typeLength;
            }
            else
            {
                length = (typePrecision != null ? typePrecision : null);
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
     * TODO !!!! the fieldType logic, like setting a length based on the type, should be done only after loading all metadata, 
     * otherwise it can cause a different behavior based on the loading order of the annotations !!!!
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
                typeLength = "" + annotationValues.get("length");
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
                    if (mgr.getNucleusContext().getConfiguration().getBooleanProperty("datanucleus.jpa.legacy.mapBooleanToSmallint", false))
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
            if (String.class.isAssignableFrom(fieldType) || fieldType == Character.class || fieldType == char.class)
            {
                length = typeLength;
            }
            else
            {
                length = (typePrecision != null ? typePrecision : null);
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
     * Method to create a new JoinMetaData for a secondary table.
     * @param cmd MetaData for the class
     * @param annotations Annotations on the class
     * @return The join metadata
     */
    private JoinMetaData[] newJoinMetaDataForClass(AbstractClassMetaData cmd, AnnotationObject[] annotations)
    {
        Set<JoinMetaData> joins = new HashSet<JoinMetaData>();
        for (int i=0;annotations != null && i<annotations.length;i++)
        {
            String annName = annotations[i].getName();
            Map<String, Object> annotationValues = annotations[i].getNameValueMap();
            if (annName.equals(JPAAnnotationUtils.SECONDARY_TABLES))
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
                        joins.add(joinmd);
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
                                    // TODO Support ASC|DESC that can be placed after a column name
                                    for (int l=0;l<cols.length;l++)
                                    {
                                        idxmd.addColumn(cols[l]);
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
                joins.add(joinmd);
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
                            // TODO Support ASC|DESC that can be placed after a column name
                            for (int k=0;k<cols.length;k++)
                            {
                                idxmd.addColumn(cols[k]);
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
        }
        return joins.toArray(new JoinMetaData[joins.size()]);
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
     * Check if class is persistable, by looking at annotations
     * @param cls the Class
     * @return true if the class has Entity annotation 
     */
    protected boolean isClassPersistable(Class cls)
    {
        AnnotationObject[] annotations = getClassAnnotationsForClass(cls);
        for (int i = 0; i < annotations.length; i++)
        {
            String annClassName = annotations[i].getName();
            if (annClassName.equals(JPAAnnotationUtils.ENTITY))
            {
                return true;
            }
            else if (annClassName.equals(JPAAnnotationUtils.EMBEDDABLE))
            {
                return true;
            }
            else if (annClassName.equals(JPAAnnotationUtils.MAPPED_SUPERCLASS))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if class is persistence aware, by looking at annotations
     * @param cls the Class
     * @return true if the class has @PersistenceAware
     */
    protected boolean isClassPersistenceAware(Class cls)
    {
        AnnotationObject[] annotations = getClassAnnotationsForClass(cls);
        for (int i = 0; i < annotations.length; i++)
        {
            String annName = annotations[i].getName();
            if (annName.equals(JPAAnnotationUtils.PERSISTENCE_AWARE))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if class has NamedXXXQuery annotations (for classes that are not persistable but provide
     * named query definitions.
     * @param cls the Class
     * @return true if the class has Named query annotations
     */
    protected boolean doesClassHaveNamedQueries(Class cls)
    {
        AnnotationObject[] annotations = getClassAnnotationsForClass(cls);
        for (int i = 0; i < annotations.length; i++)
        {
            String annClassName = annotations[i].getName();
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
     * Check if class has Converter annotation.
     * @param cls the Class
     * @return true if the class has Converter annotations
     */
    protected boolean doesClassHaveConverter(Class cls)
    {
        AnnotationObject[] annotations = getClassAnnotationsForClass(cls);
        for (int i = 0; i < annotations.length; i++)
        {
            String annClassName = annotations[i].getName();
            if (annClassName.equals(JPAAnnotationUtils.CONVERTER))
            {
                Map<String, Object> annotationValues = annotations[i].getNameValueMap();
                boolean autoApply = (Boolean) annotationValues.get("autoApply");
                TypeManager typeMgr = mgr.getNucleusContext().getTypeManager();
                if (typeMgr.getTypeConverterForName(cls.getName()) == null)
                {
                    // Not yet cached an instance of this converter so create one
                    AttributeConverter entityConv = 
                        (AttributeConverter) ClassUtils.newInstance(cls, null, null);

                    // Extract field and datastore types for this converter
                    Class attrType = JPATypeConverterUtils.getAttributeTypeForAttributeConverter(entityConv.getClass(), null);
                    Class dbType = JPATypeConverterUtils.getDatabaseTypeForAttributeConverter(entityConv.getClass(), attrType, null);

                    // Register the TypeConverter under the name of the AttributeConverter class
                    if (attrType != null)
                    {
                        TypeConverter conv = new JPATypeConverter(entityConv);
                        typeMgr.registerConverter(cls.getName(), conv, attrType, dbType, autoApply, attrType.getName());
                    }
                }
                return true;
            }
        }
        return false;
    }
}