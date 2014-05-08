/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.Converts;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Enumerated;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.MapKeyTemporal;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.Version;

import javax.persistence.Convert;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;

import org.datanucleus.api.jpa.annotations.DatastoreIdentity;
import org.datanucleus.api.jpa.annotations.Extension;
import org.datanucleus.api.jpa.annotations.Extensions;
import org.datanucleus.api.jpa.annotations.PersistenceAware;
import org.datanucleus.api.jpa.annotations.SurrogateVersion;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.IdentityStrategy;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.annotations.Member;
import org.datanucleus.util.StringUtils;

/**
 * Series of utility methods for converting between JPA annotations and metadata.
 */
public class JPAAnnotationUtils
{
    public static final String ENTITY = Entity.class.getName();

    public static final String MAPPED_SUPERCLASS = MappedSuperclass.class.getName();

    public static final String EMBEDDABLE = Embeddable.class.getName();

    public static final String EMBEDDED = Embedded.class.getName();

    public static final String TABLE = Table.class.getName();

    public static final String COLUMN = Column.class.getName();

    public static final String ID_CLASS = IdClass.class.getName();

    public static final String ID = Id.class.getName();

    public static final String BASIC = Basic.class.getName();

    public static final String TRANSIENT = Transient.class.getName();

    public static final String ENUMERATED = Enumerated.class.getName();

    public static final String TEMPORAL = Temporal.class.getName();

    public static final String LOB = Lob.class.getName();

    public static final String VERSION = Version.class.getName();

    public static final String EMBEDDED_ID = EmbeddedId.class.getName();

    public static final String GENERATED_VALUE = GeneratedValue.class.getName();

    public static final String INHERITANCE = Inheritance.class.getName();

    public static final String DISCRIMINATOR_COLUMN = DiscriminatorColumn.class.getName();

    public static final String DISCRIMINATOR_VALUE = DiscriminatorValue.class.getName();

    public static final String ENTITY_LISTENERS = EntityListeners.class.getName();

    public static final String EXCLUDE_SUPERCLASS_LISTENERS = ExcludeSuperclassListeners.class.getName();

    public static final String EXCLUDE_DEFAULT_LISTENERS = ExcludeDefaultListeners.class.getName();

    public static final String SEQUENCE_GENERATOR = SequenceGenerator.class.getName();

    public static final String TABLE_GENERATOR = TableGenerator.class.getName();

    public static final String PRIMARY_KEY_JOIN_COLUMNS = PrimaryKeyJoinColumns.class.getName();

    public static final String PRIMARY_KEY_JOIN_COLUMN = PrimaryKeyJoinColumn.class.getName();

    public static final String ATTRIBUTE_OVERRIDES = AttributeOverrides.class.getName();

    public static final String ATTRIBUTE_OVERRIDE = AttributeOverride.class.getName();

    public static final String ASSOCIATION_OVERRIDES = AssociationOverrides.class.getName();

    public static final String ASSOCIATION_OVERRIDE = AssociationOverride.class.getName();

    public static final String NAMED_QUERIES = NamedQueries.class.getName();

    public static final String NAMED_QUERY = NamedQuery.class.getName();

    public static final String NAMED_NATIVE_QUERIES = NamedNativeQueries.class.getName();

    public static final String NAMED_NATIVE_QUERY = NamedNativeQuery.class.getName();

    public static final String NAMED_STOREDPROC_QUERIES = NamedStoredProcedureQueries.class.getName();

    public static final String NAMED_STOREDPROC_QUERY = NamedStoredProcedureQuery.class.getName();

    public static final String SQL_RESULTSET_MAPPINGS = SqlResultSetMappings.class.getName();

    public static final String SQL_RESULTSET_MAPPING = SqlResultSetMapping.class.getName();

    public static final String SECONDARY_TABLES = SecondaryTables.class.getName();

    public static final String SECONDARY_TABLE = SecondaryTable.class.getName();

    public static final String JOIN_TABLE = JoinTable.class.getName();

    public static final String MAP_KEY = MapKey.class.getName();

    public static final String MAP_KEY_COLUMN = MapKeyColumn.class.getName();
    
    public static final String MAP_KEY_JOIN_COLUMN = MapKeyJoinColumn.class.getName();
    
    public static final String MAP_KEY_JOIN_COLUMNS = MapKeyJoinColumns.class.getName();

    public static final String MAP_KEY_CLASS = MapKeyClass.class.getName();
    
    public static final String MAP_KEY_ENUMERATED = MapKeyEnumerated.class.getName();

    public static final String MAP_KEY_TEMPORAL = MapKeyTemporal.class.getName();

    public static final String ORDER_BY = OrderBy.class.getName();

    public static final String ONE_TO_ONE = OneToOne.class.getName();

    public static final String ONE_TO_MANY = OneToMany.class.getName();

    public static final String MANY_TO_ONE = ManyToOne.class.getName();

    public static final String MANY_TO_MANY = ManyToMany.class.getName();

    public static final String JOIN_COLUMNS = JoinColumns.class.getName();

    public static final String JOIN_COLUMN = JoinColumn.class.getName();

    public static final String PERSISTENCE_AWARE = PersistenceAware.class.getName();

    public static final String DATASTORE_IDENTITY = DatastoreIdentity.class.getName();

    public static final String SURROGATE_VERSION = SurrogateVersion.class.getName();

    public static final String EXTENSIONS = Extensions.class.getName();

    public static final String EXTENSION = Extension.class.getName();

    public static final String ELEMENT_COLLECTION = ElementCollection.class.getName();

    public static final String COLLECTION_TABLE = CollectionTable.class.getName();

    public static final String ORDER_COLUMN = OrderColumn.class.getName();

    public static final String CACHEABLE = Cacheable.class.getName();

    public static final String CONVERT = Convert.class.getName();

    public static final String CONVERTS = Converts.class.getName();

    public static final String CONVERTER = Converter.class.getName();

    public static final String NAMED_ENTITY_GRAPHS = NamedEntityGraphs.class.getName();

    public static final String NAMED_ENTITY_GRAPH = NamedEntityGraph.class.getName();

    public static final String NAMED_ATTRIBUTE_NODE = NamedAttributeNode.class.getName();

    /**
     * Convenience accessor for the string name of a id generator strategy (from JPA annotations)
     * @param type Generation type (strategy)
     * @return The name
     */
    public static String getIdentityStrategyString(GenerationType type)
    {
        if (type == GenerationType.AUTO)
        {
            return IdentityStrategy.NATIVE.toString();
        }
        else if (type == GenerationType.IDENTITY)
        {
            return IdentityStrategy.IDENTITY.toString();
        }
        else if (type == GenerationType.SEQUENCE)
        {
            return IdentityStrategy.SEQUENCE.toString();
        }
        else if (type == GenerationType.TABLE)
        {
            return IdentityStrategy.INCREMENT.toString();
        }
        else
        {
            return null;
        }
    }

    /**
     * Whether the given type is temporal for JPA.
     * @param type the type
     * @return true if the type is temporal as per JPA spec
     */
    public static boolean isTemporalType(Class type)
    {
        if (type == Date.class || type == java.sql.Date.class || type == Time.class || type == Timestamp.class ||
            type == Calendar.class)
        {
            return true;
        }
        return false;
    }

    /**
     * Method to create a ColumnMetaData based on the supplied Column annotation.
     * @param parent Parent MetaData object
     * @param field The field/property
     * @param column The Column annotation
     * @return MetaData for the column
     */
    static ColumnMetaData getColumnMetaDataForColumnAnnotation(MetaData parent, Member field, Column column)
    {
        String columnName = column.name();
        String target = null;
        String targetField = null;
        String jdbcType = null;
        String sqlType = null;
        String length = null;
        String scale = null;
        String allowsNull = null;
        String defaultValue = null;
        String insertValue = null;
        String insertable = null;
        String updateable = null;
        String unique = null;
        String table = null;

        if (field.getType().isPrimitive())
        {
            length = "" + column.precision();
            if (length.equals(""))
            {
                length = null;
            }
            scale = "" + column.scale();
            if (scale.equals(""))
            {
                scale = null;
            }
            if ((length == null || length.equals("0")) && char.class.isAssignableFrom(field.getType()))
            {
                // in the TCK, char is stored by default in a CHAR column with 1 length
                // if nothing defined, then default to this
                length = "1";
            }
            if (field.getType() == boolean.class)
            {
                jdbcType = "SMALLINT";
            }
        }
        else if (String.class.isAssignableFrom(field.getType()))
        {
            length = "" + column.length();
            if (length.equals(""))
            {
                length = null;
            }
        }
        else if (Number.class.isAssignableFrom(field.getType()))
        {
            length = "" + column.precision();
            if (length.equals(""))
            {
                length = null;
            }
            scale = "" + column.scale();
            if (scale.equals(""))
            {
                scale = null;
            }
        }
        allowsNull = Boolean.valueOf(column.nullable()).toString();
        insertable = Boolean.valueOf(column.insertable()).toString();
        updateable = Boolean.valueOf(column.updatable()).toString(); // Note : "updatable" is spelt incorrectly in the JPA spec.
        unique = Boolean.valueOf(column.unique()).toString();
        table = column.table();
        if (!StringUtils.isWhitespace(table))
        {
            // Column in secondary-table
            // TODO use this value
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
        if (!StringUtils.isWhitespace(column.columnDefinition()))
        {
            colmd.setColumnDdl(column.columnDefinition());
        }
        return colmd;
    }
}