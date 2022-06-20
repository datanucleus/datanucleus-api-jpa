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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.jpa.JPAEntityGraph;
import org.datanucleus.api.jpa.AbstractJPAGraph;
import org.datanucleus.api.jpa.JPASubgraph;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.ColumnMetaData;
import org.datanucleus.metadata.DiscriminatorMetaData;
import org.datanucleus.metadata.DiscriminatorStrategy;
import org.datanucleus.metadata.ElementMetaData;
import org.datanucleus.metadata.EmbeddedMetaData;
import org.datanucleus.metadata.EventListenerMetaData;
import org.datanucleus.metadata.FieldMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.metadata.FileMetaData;
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
import org.datanucleus.metadata.MetaDataFileType;
import org.datanucleus.metadata.MultitenancyMetaData;
import org.datanucleus.metadata.NullValue;
import org.datanucleus.metadata.OrderMetaData;
import org.datanucleus.metadata.PackageMetaData;
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
import org.datanucleus.metadata.xml.AbstractXmlMetaDataHandler;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Parser handler for JPA XML MetaData.
 * Implements DefaultHandler and handles the extracting of MetaData for JPA from the XML elements/attributes. 
 * This class simply constructs the MetaData representation mirroring what is in the XML MetaData file. 
 * It has no knowledge of the class(es) that it represents, simply the information in the XML MetaData file. 
 * The knowledge of the classes is imposed on the representation at a later stage where necessary.
 * <P>
 * Operates the parse process using a Stack. XML MetaData components are added to the stack as they are encountered and created. 
 * They are then popped off the stack when the end element is encountered.
 * </P>
 */
public class JPAXmlMetaDataHandler extends AbstractXmlMetaDataHandler
{
    /** Default package name for the entity-mappings (if any). */
    String defaultPackageName = null;

    /** Flag for whether the MetaData in this file is "metadata-complete" */
    boolean metaDataComplete = false;

    /** Default value for whether to cascade-persist. */
    boolean defaultCascadePersist = false;

    /** Whether this file is using property access. */
    boolean propertyAccess = false;

    /** Current query result entity (persistent class), during parse process. */
    String queryResultEntityName = null;

    /** Work-in-progress result constructor type mapping, during parse process. */
    String ctrTypeClassName = null;

    /** Work-in-progress result constructor columns, during parse process. */
    List<ConstructorTypeColumn> ctrTypeColumns = null;

    private class GraphHolder
    {
        AbstractJPAGraph graph;
        Map<String, String> attributeNameBySubgroupName = new HashMap<String, String>();
    }
    Deque<GraphHolder> graphHolderStack = new ArrayDeque<GraphHolder>();

    /**
     * Constructor. Protected to prevent instantiation.
     * @param mgr the metadata manager
     * @param filename The name of the file to parse
     * @param resolver Entity Resolver to use (null if not available)
     */
    public JPAXmlMetaDataHandler(MetaDataManager mgr, String filename, EntityResolver resolver)
    {
        super(mgr, filename, resolver);
        metadata = new FileMetaData(filename);
        pushStack(metadata); // Start with FileMetaData on the stack
    }

    /**
     * Utility to create a new class component.
     * @param pmd The parent PackageMetaData
     * @param attrs The attributes
     * @param embeddedOnly Whether this class is embedded-only
     * @return The ClassMetaData
     */
    protected ClassMetaData newClassObject(PackageMetaData pmd, Attributes attrs, boolean embeddedOnly)
    {
        String className = getAttr(attrs, "class");
        if (className.indexOf('.') > 0)
        {
            // Strip off any package info
            className = className.substring(className.lastIndexOf('.')+1);
        }

        if (StringUtils.isWhitespace(className))
        {
            throw new InvalidClassMetaDataException("044061", pmd.getName());
        }
        ClassMetaData cmd = new ClassMetaData(pmd, className);
        cmd.setEntityName(getAttr(attrs, "name"));
        cmd.setRequiresExtent(true);
        cmd.setDetachable(true);
        cmd.setPersistenceModifier(ClassPersistenceModifier.PERSISTENCE_CAPABLE);
        cmd.setEmbeddedOnly(embeddedOnly);
        cmd.setIdentityType(embeddedOnly ? IdentityType.NONDURABLE : IdentityType.APPLICATION);
        cmd.setCacheable(getAttr(attrs, "cacheable"));

        String classMetaDataComplete = getAttr(attrs, "metadata-complete");
        if (metaDataComplete || (classMetaDataComplete != null && classMetaDataComplete.equalsIgnoreCase("true")))
        {
            // Ignore any annotations since either the class or the whole file is "metadata-complete"
            cmd.setMetaDataComplete();
        }

        return cmd;
    }

    /**
     * Utility to create a new field/property component and add it to the class as required.
     * If the field/property already exists
     * @param acmd The parent class MetaData
     * @param attrs The attributes
     * @param dfgDefault The default for DFG for this field if not specified
     * @return The FieldMetaData/PropertyMetaData
     */
    protected AbstractMemberMetaData newFieldObject(AbstractClassMetaData acmd, Attributes attrs, String dfgDefault)
    {
        boolean dfg = dfgDefault.equalsIgnoreCase("EAGER") ? true : false;
        String fetch = getAttr(attrs, "fetch");
        if (fetch != null)
        {
            if (fetch.equalsIgnoreCase("LAZY"))
            {
                dfg = false;
            }
            else if (fetch.equalsIgnoreCase("EAGER"))
            {
                dfg = true;
            }
        }

        AbstractMemberMetaData mmd = null;
        mmd = acmd.getMetaDataForMember(getAttr(attrs, "name"));
        if (mmd != null)
        {
            // Member exists, so add all attributes required
            mmd.setDefaultFetchGroup(dfg);
            String depString = getAttr(attrs,"dependent");
            if (!StringUtils.isWhitespace(depString))
            {
                mmd.setDependent((depString.trim().equalsIgnoreCase("true") ? true : false));
            }

            mmd.setMappedBy(getAttr(attrs,"mapped-by"));

            String loadFg = getAttr(attrs,"load-fetch-group");
            if (!StringUtils.isWhitespace(loadFg))
            {
                mmd.setLoadFetchGroup(loadFg);
            }
        }
        else
        {
            mmd = propertyAccess ? new PropertyMetaData(acmd, getAttr(attrs,"name")) : new FieldMetaData(acmd, getAttr(attrs,"name"));
            mmd.setPersistenceModifier(FieldPersistenceModifier.PERSISTENT);
            mmd.setDefaultFetchGroup(dfg);
            mmd.setMappedBy(getAttr(attrs,"mapped-by"));
            mmd.setLoadFetchGroup(getAttr(attrs,"load-fetch-group"));
            acmd.addMember(mmd);
        }
        if (defaultCascadePersist)
        {
            // This file has <persistence-unit-defaults> set to cascade-persist all fields
            mmd.setCascadePersist(true);
        }

        String optionalStr = getAttr(attrs, "optional");
        if (optionalStr != null && optionalStr.equalsIgnoreCase("false"))
        {
            mmd.setNullValue(NullValue.getNullValue(optionalStr));
        }

        String orphanRemovalStr = getAttr(attrs, "orphan-removal");
        if (orphanRemovalStr != null && orphanRemovalStr.equalsIgnoreCase("true"))
        {
            mmd.setCascadeRemoveOrphans(true);
        }
        return mmd;
    }

    /**
     * Utility to create a new primary key field/property component.
     * @param acmd The parent class MetaData
     * @param attrs Attributes of the "id" element
     * @return The FieldMetaData/PropertyMetaData
     */
    protected AbstractMemberMetaData newPKFieldObject(AbstractClassMetaData acmd, Attributes attrs)
    {
        AbstractMemberMetaData mmd = null;

        mmd = acmd.getMetaDataForMember(getAttr(attrs, "name"));
        if (mmd != null)
        {
            // Member exists, so mark as PK
            mmd.setPrimaryKey(true);
        }
        else
        {
            // Create new property/field
            mmd = propertyAccess ? new PropertyMetaData(acmd, getAttr(attrs, "name")) : new FieldMetaData(acmd, getAttr(attrs, "name"));
            mmd.setPersistenceModifier(FieldPersistenceModifier.PERSISTENT);
            mmd.setPrimaryKey(true);
            if (defaultCascadePersist)
            {
                // This file has <persistence-unit-defaults> set to cascade-persist all fields
                mmd.setCascadePersist(true);
            }
            acmd.addMember(mmd);
        }
        return mmd;
    }

    /**
     * Utility to create a new transient field/property component.
     * @param md The parent MetaData
     * @param name Name of the transient field
     * @return The FieldMetaData/PropertyMetaData
     */
    protected AbstractMemberMetaData newTransientFieldObject(MetaData md, String name)
    {
        AbstractMemberMetaData mmd = propertyAccess ? new PropertyMetaData(md, name) : new FieldMetaData(md, name);
        mmd.setNotPersistent();
        if (defaultCascadePersist)
        {
            // This file has <persistence-unit-defaults> set to cascade-persist all fields
            mmd.setCascadePersist(true);
        }
        return mmd;
    }

    /**
     * Utility to create a new embedded field/property component.
     * @param md The parent MetaData
     * @param name Name of the embedded field
     * @return The FieldMetaData/PropertyMetaData
     */
    protected AbstractMemberMetaData newEmbeddedFieldObject(MetaData md, String name)
    {
        AbstractMemberMetaData mmd = propertyAccess ? new PropertyMetaData(md, name) : new FieldMetaData(md, name);
        mmd.setEmbedded(true);
        if (defaultCascadePersist)
        {
            // This file has <persistence-unit-defaults> set to cascade-persist all fields
            mmd.setCascadePersist(true);
        }
        return mmd;
    }

    /**
     * Utility to create a new field entry for a field/property in a superclass.
     * @param md The parent MetaData
     * @param attrs Attributes of the element
     * @return The FieldMetaData/PropertyMetaData
     */
    protected AbstractMemberMetaData newOverriddenFieldObject(MetaData md, Attributes attrs)
    {
        AbstractMemberMetaData mmd = propertyAccess ? new PropertyMetaData(md, "#UNKNOWN." + getAttr(attrs, "name")) : new FieldMetaData(md, "#UNKNOWN." + getAttr(attrs, "name"));
        String colName = getAttr(attrs, "column");
        if (colName != null)
        {
            mmd.setColumn(colName);
        }
        mmd.setPersistenceModifier(FieldPersistenceModifier.PERSISTENT);
        if (defaultCascadePersist)
        {
            // This file has <persistence-unit-defaults> set to cascade-persist all fields
            mmd.setCascadePersist(true);
        }
        return mmd;
    }

    /**
     * Utility to create a new field entry for a field/property in a superclass.
     * @param embmd The parent MetaData
     * @param attrs Attributes of the "id" element
     * @return The FieldMetaData/PropertyMetaData
     */
    protected AbstractMemberMetaData newOverriddenEmbeddedFieldObject(EmbeddedMetaData embmd, Attributes attrs)
    {
        return newOverriddenEmbeddedFieldObject(embmd, getAttr(attrs, "name"), getAttr(attrs, "column"));
    }

    /** Temporary variable for when processing an xxx-override so we remember the field it actually refers to */
    AbstractMemberMetaData overrideMmd = null;

    /**
     * Recursive method to process embedded member overrides.
     * @param embmd The parent MetaData
     * @param memberName The member name being overridden
     * @param columnName Column name to override it with
     * @return The FieldMetaData/PropertyMetaData
     */
    protected AbstractMemberMetaData newOverriddenEmbeddedFieldObject(EmbeddedMetaData embmd, String memberName, String columnName)
    {
        if (memberName.indexOf('.') > 0)
        {
            int position = memberName.indexOf('.');
            String baseMemberName = memberName.substring(0, position);
            String nestedMemberName = memberName.substring(position+1);

            AbstractMemberMetaData mmd = propertyAccess ? new PropertyMetaData(embmd, baseMemberName) : new FieldMetaData(embmd, baseMemberName);

            EmbeddedMetaData nestedEmbmd = new EmbeddedMetaData();
            nestedEmbmd.setParent(mmd);
            mmd.setEmbeddedMetaData(nestedEmbmd);

            AbstractMemberMetaData nestedEmbMmd = newOverriddenEmbeddedFieldObject(nestedEmbmd, nestedMemberName, columnName);
            nestedEmbmd.addMember(nestedEmbMmd);
            overrideMmd = nestedEmbMmd;
            return mmd;
        }

        AbstractMemberMetaData mmd = propertyAccess ? new PropertyMetaData(embmd, memberName) : new FieldMetaData(embmd, memberName);
        mmd.setParent(embmd);

        if (columnName != null)
        {
            mmd.setColumn(columnName);
        }
        mmd.setPersistenceModifier(FieldPersistenceModifier.PERSISTENT);
        if (defaultCascadePersist)
        {
            // This file has <persistence-unit-defaults> set to cascade-persist all fields
            mmd.setCascadePersist(true);
        }
        overrideMmd = mmd;
        return mmd;
    }

    /**
     * Handler method called at the start of an element.
     * @param uri URI of the tag
     * @param localName Local name
     * @param qName Element name
     * @param attrs Attributes for this element 
     * @throws SAXException in parsing errors
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
    throws SAXException
    {
        if (localName.length() < 1)
        {
            localName = qName;
        }
        try
        {
            if (localName.equals("entity-mappings"))
            {
                FileMetaData filemd = (FileMetaData)getStack();
                filemd.setType(MetaDataFileType.JPA_MAPPING_FILE);
            }
            else if (localName.equals("description"))
            {
                // Of no practical use so ignored
            }
            else if (localName.equals("persistence-unit-metadata"))
            {
                // Nothing to do - we use subelements
            }
            else if (localName.equals("xml-mapping-metadata-complete"))
            {
                // All classes in the file are complete without any annotations
                metaDataComplete = true;
            }
            else if (localName.equals("persistence-unit-defaults"))
            {
                // Nothing to do - we use subelements
            }
            else if (localName.equals("package"))
            {
                // Processed in endElement()
            }
            else if (localName.equals("schema"))
            {
                // Processed in endElement()
            }
            else if (localName.equals("catalog"))
            {
                // Processed in endElement()
            }
            else if (localName.equals("access"))
            {
                // Processed in endElement()
            }
            else if (localName.equals("column-name"))
            {
                // Processed in endElement()
            }
            else if (localName.equals("sequence-generator"))
            {
                // Find the package for this sequence
                PackageMetaData pmd = null;
                FileMetaData filemd = (FileMetaData)metadata;
                if (defaultPackageName != null)
                {
                    pmd = filemd.getPackage(defaultPackageName);
                }
                else
                {
                    if (filemd.getNoOfPackages() > 0)
                    {
                        pmd = filemd.getPackage(0);
                    }
                    else
                    {
                        // Add a dummy (root) package to hold our sequences since no default package name set
                        pmd = filemd.newPackageMetaData("");
                    }
                }
                String initValue = getAttr(attrs, "initial-value");
                if (StringUtils.isWhitespace(initValue))
                {
                    initValue = "1"; // JPA default
                }
                String allocSize = getAttr(attrs, "allocation-size");
                if (StringUtils.isWhitespace(allocSize))
                {
                    allocSize = "50"; // JPA default
                }
                SequenceMetaData seqmd = pmd.newSequenceMetaData(getAttr(attrs, "name"), null);
                String datastoreSeqName = getAttr(attrs, "sequence-name");
                if (StringUtils.isWhitespace(datastoreSeqName))
                {
                    datastoreSeqName = seqmd.getName();
                }
                seqmd.setDatastoreSequence(datastoreSeqName);
                seqmd.setInitialValue(initValue);
                seqmd.setAllocationSize(allocSize);
                String catalogName = getAttr(attrs, "catalog");
                if (StringUtils.isWhitespace(catalogName))
                {
                    seqmd.setCatalogName(catalogName);
                }
                String schemaName = getAttr(attrs, "schema");
                if (StringUtils.isWhitespace(schemaName))
                {
                    seqmd.setSchemaName(schemaName);
                }
            }
            else if (localName.equals("table-generator"))
            {
                // Find the package for this table generator
                PackageMetaData pmd = null;
                FileMetaData filemd = (FileMetaData)metadata;
                if (defaultPackageName != null)
                {
                    pmd = filemd.getPackage(defaultPackageName);
                }
                else
                {
                    if (filemd.getNoOfPackages() > 0)
                    {
                        pmd = filemd.getPackage(0);
                    }
                    else
                    {
                        // Add a dummy (root) package to hold our sequences since no default package name set
                        pmd = filemd.newPackageMetaData("");
                    }
                }
                TableGeneratorMetaData tgmd = pmd.newTableGeneratorMetaData(getAttr(attrs, "name"));
                tgmd.setTableName(getAttr(attrs, "table"));
                tgmd.setCatalogName(getAttr(attrs, "catalog"));
                tgmd.setSchemaName(getAttr(attrs, "schema"));
                tgmd.setPKColumnName(getAttr(attrs, "pk-column-name"));
                tgmd.setPKColumnValue(getAttr(attrs, "pk-column-value"));
                tgmd.setValueColumnName(getAttr(attrs, "value-column-name"));
                tgmd.setInitialValue(getAttr(attrs, "initial-value"));
                tgmd.setAllocationSize(getAttr(attrs, "allocation-size"));
            }
            else if (localName.equals("named-query"))
            {
                // Named JPQL query
                MetaData md = getStack();
                if (md instanceof FileMetaData)
                {
                    FileMetaData filemd = (FileMetaData)md;
                    QueryMetaData qmd = filemd.newQueryMetaData(getAttr(attrs, "name"));
                    qmd.setLanguage(QueryLanguage.JPQL.name());
                    pushStack(qmd);
                }
                else if (md instanceof ClassMetaData)
                {
                    ClassMetaData cmd = (ClassMetaData)md;
                    String name = getAttr(attrs, "name");
                    if (StringUtils.isWhitespace(name))
                    {
                        throw new InvalidClassMetaDataException("044154", cmd.getFullClassName());
                    }
                    QueryMetaData qmd = new QueryMetaData(name);
                    qmd.setLanguage(QueryLanguage.JPQL.name());
                    cmd.addQuery(qmd);
                    pushStack(qmd);
                }
            }
            else if (localName.equals("named-native-query"))
            {
                // Named SQL query
                MetaData md = getStack();
                if (md instanceof FileMetaData)
                {
                    FileMetaData filemd = (FileMetaData)md;
                    QueryMetaData qmd = filemd.newQueryMetaData(getAttr(attrs, "name"));
                    qmd.setLanguage(QueryLanguage.SQL.name());
                    qmd.setResultClass(getAttr(attrs, "result-class"));
                    qmd.setResultMetaDataName(getAttr(attrs, "result-set-mapping"));
                    pushStack(qmd);
                }
                else if (md instanceof ClassMetaData)
                {
                    ClassMetaData cmd = (ClassMetaData)md;
                    String name = getAttr(attrs, "name");
                    if (StringUtils.isWhitespace(name))
                    {
                        throw new InvalidClassMetaDataException("044154", cmd.getFullClassName());
                    }
                    QueryMetaData qmd = new QueryMetaData(name);
                    qmd.setLanguage(QueryLanguage.SQL.name());
                    qmd.setResultClass(getAttr(attrs, "result-class"));
                    qmd.setResultMetaDataName(getAttr(attrs, "result-set-mapping"));
                    cmd.addQuery(qmd);
                    pushStack(qmd);
                }
            }
            else if (localName.equals("named-stored-procedure-query"))
            {
                // Named Stored Procedure query
                MetaData md = getStack();
                if (md instanceof FileMetaData)
                {
                    FileMetaData filemd = (FileMetaData)md;
                    StoredProcQueryMetaData spqmd = filemd.newStoredProcQueryMetaData(getAttr(attrs, "name"));
                    spqmd.setProcedureName(getAttr(attrs, "procedure-name"));
                    pushStack(spqmd);
                }
                else if (md instanceof ClassMetaData)
                {
                    ClassMetaData cmd = (ClassMetaData)md;
                    String name = getAttr(attrs, "name");
                    if (StringUtils.isWhitespace(name))
                    {
                        throw new InvalidClassMetaDataException("044154", cmd.getFullClassName());
                    }
                    StoredProcQueryMetaData spqmd = new StoredProcQueryMetaData(name);
                    spqmd.setProcedureName(getAttr(attrs, "procedure-name"));
                    cmd.addStoredProcQuery(spqmd);
                    pushStack(spqmd);
                }
            }
            else if (localName.equals("stored-procedure-parameter"))
            {
                // Stored Procedure parameter
                MetaData md = getStack();
                if (md instanceof StoredProcQueryMetaData)
                {
                    StoredProcQueryMetaData spqmd = (StoredProcQueryMetaData)md;
                    StoredProcQueryParameterMetaData parammd = new StoredProcQueryParameterMetaData();
                    parammd.setName(getAttr(attrs, "name"));
                    parammd.setType(getAttr(attrs, "class"));
                    String mode = getAttr(attrs, "mode");
                    if (mode != null)
                    {
                        if (mode.equalsIgnoreCase("IN"))
                        {
                            parammd.setMode(StoredProcQueryParameterMode.IN);
                        }
                        else if (mode.equalsIgnoreCase("OUT"))
                        {
                            parammd.setMode(StoredProcQueryParameterMode.OUT);
                        }
                        else if (mode.equalsIgnoreCase("INOUT"))
                        {
                            parammd.setMode(StoredProcQueryParameterMode.INOUT);
                        }
                        else if (mode.equalsIgnoreCase("REF_CURSOR"))
                        {
                            parammd.setMode(StoredProcQueryParameterMode.REF_CURSOR);
                        }
                    }
                    spqmd.addParameter(parammd);
                }
            }
            else if (localName.equals("result-class"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("result-set-mapping"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("sql-result-set-mapping"))
            {
                MetaData md = getStack();
                if (md instanceof FileMetaData)
                {
                    FileMetaData filemd = (FileMetaData)md;
                    QueryResultMetaData qrmd = filemd.newQueryResultMetaData(getAttr(attrs, "name"));
                    pushStack(qrmd);
                }
                else if (md instanceof ClassMetaData)
                {
                    ClassMetaData cmd = (ClassMetaData)md;
                    QueryResultMetaData qrmd = new QueryResultMetaData(getAttr(attrs, "name"));
                    cmd.addQueryResultMetaData(qrmd);
                    pushStack(qrmd);
                }
            }
            else if (localName.equals("entity-result"))
            {
                // Add an entity (persistent class) mapping
                QueryResultMetaData qrmd = (QueryResultMetaData)getStack();
                queryResultEntityName = getAttr(attrs, "entity-class"); // Save this for any field-result that arrives
                qrmd.addPersistentTypeMapping(queryResultEntityName, null, // No field-column mappings info at this point
                    getAttr(attrs, "discriminator-column"));
            }
            else if (localName.equals("field-result"))
            {
                // Add a field-column mapping for the entity (persistent class)
                QueryResultMetaData qrmd = (QueryResultMetaData)getStack();
                qrmd.addMappingForPersistentTypeMapping(queryResultEntityName, getAttr(attrs, "name"), getAttr(attrs, "column"));
            }
            else if (localName.equals("column-result"))
            {
                if (ctrTypeClassName == null)
                {
                    // Add a scalar column mapping
                    QueryResultMetaData qrmd = (QueryResultMetaData)getStack();
                    qrmd.addScalarColumn(getAttr(attrs, "name"));
                }
                else
                {
                    // Add column to current work-in-progress constructor mapping
                    if (ctrTypeColumns == null)
                    {
                        ctrTypeColumns = new ArrayList<ConstructorTypeColumn>();
                    }
                    String colClsName = getAttr(attrs, "class");
                    Class ctrColCls = colClsName!=null ? clr.classForName(colClsName) : null;
                    ctrTypeColumns.add(new ConstructorTypeColumn(getAttr(attrs, "name"), ctrColCls));
                }
            }
            else if (localName.equals("constructor-result"))
            {
                ctrTypeClassName = getAttr(attrs, "target-class");
            }
            else if (localName.equals("mapped-superclass"))
            {
                // New entity for this package
                FileMetaData filemd = (FileMetaData)getStack();
                String className = getAttr(attrs, "class");
                String packageName = null;
                if (className.indexOf('.') > 0)
                {
                    // Fully-qualified so use package name from class
                    packageName = className.substring(0, className.lastIndexOf('.'));
                }
                PackageMetaData pmd = null;
                if (packageName != null)
                {
                    pmd = filemd.getPackage(packageName);
                }
                if (pmd == null)
                {
                    if (packageName != null)
                    {
                        // Class fully qualified so add its package
                        pmd = filemd.newPackageMetaData(packageName);
                    }
                    else if (defaultPackageName != null)
                    {
                        // Use default package for entity-mappings
                        pmd = filemd.getPackage(defaultPackageName);
                    }
                    else
                    {
                        // Add root package
                        pmd = filemd.newPackageMetaData("");
                    }
                }

                ClassMetaData cmd = newClassObject(pmd, attrs, false);
                pmd.addClass(cmd);
                cmd.setMappedSuperclass(true);

                // Set to use "subclass-table" since all subclasses inherit these fields
                InheritanceMetaData inhmd = new InheritanceMetaData();
                inhmd.setStrategy(InheritanceStrategy.SUBCLASS_TABLE);
                cmd.setInheritanceMetaData(inhmd);

                pushStack(cmd);
            }
            else if (localName.equals("query"))
            {
                // Processed in endElement()
            }
            else if (localName.equals("entity"))
            {
                // New entity for this package
                FileMetaData filemd = (FileMetaData)getStack();
                String className = getAttr(attrs, "class");
                String packageName = null;
                if (className.indexOf('.') > 0)
                {
                    // Fully-qualified so use package name from class
                    packageName = className.substring(0, className.lastIndexOf('.'));
                }
                PackageMetaData pmd = null;
                if (packageName != null)
                {
                    pmd = filemd.getPackage(packageName);
                }
                if (pmd == null)
                {
                    if (packageName != null)
                    {
                        // Class fully qualified so add its package
                        pmd = filemd.newPackageMetaData(packageName);
                    }
                    else if (defaultPackageName != null)
                    {
                        // Use default package for entity-mappings
                        pmd = filemd.getPackage(defaultPackageName);
                    }
                    else
                    {
                        // Add root package
                        pmd = filemd.newPackageMetaData("");
                    }
                }

                ClassMetaData cmd = newClassObject(pmd, attrs, false);
                pmd.addClass(cmd);

                pushStack(cmd);
            }
            else if (localName.equals("embeddable"))
            {
                // New embedded-only entity for this package
                FileMetaData filemd = (FileMetaData)getStack();
                String className = getAttr(attrs, "class");
                String packageName = null;
                if (className.indexOf('.') > 0)
                {
                    // Fully-qualified so use package name from class
                    packageName = className.substring(0, className.lastIndexOf('.'));
                }
                PackageMetaData pmd = null;
                if (packageName != null)
                {
                    pmd = filemd.getPackage(packageName);
                }
                if (pmd == null)
                {
                    if (packageName != null)
                    {
                        // Class fully qualified so add its package
                        pmd = filemd.newPackageMetaData(packageName);
                    }
                    else if (defaultPackageName != null)
                    {
                        // Use default package for entity-mappings
                        pmd = filemd.getPackage(defaultPackageName);
                    }
                    else
                    {
                        // Add root package
                        pmd = filemd.newPackageMetaData("");
                    }
                }

                ClassMetaData cmd = newClassObject(pmd, attrs, true);
                pmd.addClass(cmd);

                pushStack(cmd);
            }
            else if (localName.equals("attributes"))
            {
                // Nothing to do since is just a holder of other elements
            }
            else if (localName.equals("embeddable-attributes"))
            {
                // Nothing to do since is just a holder of other elements
            }
            else if (localName.equals("id-class"))
            {
                // Identity class
                ClassMetaData cmd = (ClassMetaData)getStack();
                cmd.setObjectIdClass(getAttr(attrs, "class"));
            }
            else if (localName.equals("datastore-id"))
            {
                // DataNucleus Extension : Datastore Identity
                ClassMetaData cmd = (ClassMetaData)getStack();
                DatastoreIdentityMetaData idmd = cmd.newDatastoreIdentityMetaData();
                String dsidColName = getAttr(attrs, "column");
                if (!StringUtils.isWhitespace(dsidColName))
                {
                    idmd.setColumnName(dsidColName);
                }
                cmd.setIdentityType(IdentityType.DATASTORE);
                pushStack(idmd);
            }
            else if (localName.equals("nondurable-id"))
            {
                // DataNucleus Extension : NonDurable Identity
                ClassMetaData cmd = (ClassMetaData)getStack();
                cmd.setIdentityType(IdentityType.NONDURABLE);
            }
            else if (localName.equals("surrogate-version"))
            {
                // DataNucleus Extension : Surrogate Version
                ClassMetaData cmd = (ClassMetaData)getStack();
                VersionMetaData vermd = cmd.newVersionMetaData();
                String verColName = getAttr(attrs, "column");
                if (!StringUtils.isWhitespace(verColName))
                {
                    vermd.setColumnName(verColName);
                }
                vermd.setStrategy(VersionStrategy.VERSION_NUMBER);
                String indexedStr = getAttr(attrs, "indexed");
                if (!StringUtils.isWhitespace(indexedStr) && indexedStr.equals("true"))
                {
                    vermd.setIndexed(IndexedValue.TRUE);
                }
                pushStack(vermd);
            }
            else if (localName.equals("shared-relation"))
            {
                // DataNucleus Extension : Shared Relation
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)getStack();
                String shColName = getAttr(attrs, "column");
                mmd.addExtension(MetaData.EXTENSION_MEMBER_RELATION_DISCRIM_COLUMN, shColName);
                String shColValue = getAttr(attrs, "value");
                mmd.addExtension(MetaData.EXTENSION_MEMBER_RELATION_DISCRIM_VALUE, shColValue);
                String shColPK = getAttr(attrs, "primary-key");
                mmd.addExtension(MetaData.EXTENSION_MEMBER_RELATION_DISCRIM_PK, shColPK);
            }
            else if (localName.equals("inheritance"))
            {
                // Inheritance - only for root class
                ClassMetaData cmd = (ClassMetaData)getStack();
                String strategy = getAttr(attrs, "strategy");
                String strategyType = null;
                if (strategy != null)
                {
                    if (strategy.equalsIgnoreCase("JOINED"))
                    {
                        strategyType = InheritanceStrategy.NEW_TABLE.toString();
                    }
                    else if (strategy.equalsIgnoreCase("TABLE_PER_CLASS"))
                    {
                        strategyType = InheritanceStrategy.COMPLETE_TABLE.toString();
                    }
                }
                else
                {
                    // SINGLE_TABLE (default), so implies nothing needs setting since thats the default
                }
                InheritanceMetaData inhmd = new InheritanceMetaData();
                inhmd.setStrategy(strategyType);
                if (strategy != null)
                {
                    inhmd.setStrategyForTree(strategy.toUpperCase());
                }
                cmd.setInheritanceMetaData(inhmd);
            }
            else if (localName.equals("table"))
            {
                // Table for this entity
                ClassMetaData cmd = (ClassMetaData)getStack();
                cmd.setCatalog(getAttr(attrs, "catalog"));
                cmd.setSchema(getAttr(attrs, "schema"));
                cmd.setTable(getAttr(attrs, "name"));
            }
            else if (localName.equals("secondary-table"))
            {
                // Join for this entity
                ClassMetaData cmd = (ClassMetaData)getStack();
                JoinMetaData joinmd = new JoinMetaData();
                joinmd.setTable(getAttr(attrs, "name"));
                joinmd.setCatalog(getAttr(attrs, "catalog"));
                joinmd.setSchema(getAttr(attrs, "schema"));
                cmd.addJoin(joinmd);
                pushStack(joinmd);
            }
            else if (localName.equals("primary-key-join-column"))
            {
                MetaData md = getStack();
                if (md instanceof ClassMetaData)
                {
                    // Join columns between PK of subclass table and PK of base class table
                    ClassMetaData cmd = (ClassMetaData)md;
                    ColumnMetaData colmd = new ColumnMetaData();
                    colmd.setName(getAttr(attrs, "name"));
                    colmd.setTarget(getAttr(attrs, "referenced-column-name"));
                    String columnDdl = getAttr(attrs, "column-definition");
                    if (columnDdl != null)
                    {
                        colmd.setColumnDdl(columnDdl);
                    }
                    InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
                    if (inhmd == null)
                    {
                        inhmd = cmd.newInheritanceMetaData();
                    }
                    JoinMetaData inhJoinmd = inhmd.getJoinMetaData();
                    if (inhJoinmd == null)
                    {
                        inhJoinmd = inhmd.newJoinMetaData();
                    }
                    inhJoinmd.addColumn(colmd);
                }
                else if (md instanceof JoinMetaData)
                {
                    // Join columns between PK of secondary table and PK of primary table
                    JoinMetaData joinmd = (JoinMetaData)md;
                    ColumnMetaData colmd = new ColumnMetaData();
                    colmd.setName(getAttr(attrs, "name"));
                    colmd.setTarget(getAttr(attrs, "referenced-column-name"));
                    joinmd.addColumn(colmd);
                }
            }
            else if (localName.equals("id"))
            {
                // Identity field
                ClassMetaData cmd = (ClassMetaData)getStack();
                AbstractMemberMetaData mmd = newPKFieldObject(cmd, attrs);

                pushStack(mmd);
            }
            else if (localName.equals("embedded-id"))
            {
                // Embedded identity field
                ClassMetaData cmd = (ClassMetaData)getStack();
                AbstractMemberMetaData mmd = newPKFieldObject(cmd, attrs);

                pushStack(mmd);
            }
            else if (localName.equals("basic"))
            {
                // Basic field
                AbstractClassMetaData cmd = (AbstractClassMetaData)getStack();
                AbstractMemberMetaData mmd = newFieldObject(cmd, attrs, "EAGER");

                pushStack(mmd);
            }
            else if (localName.equals("convert"))
            {
                // Basic field
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)getStack();

                if (isPersistenceContext()) // Not required whilst enhancing
                {
                    String converterClassName = getAttr(attrs, "converter");
                    String convAttrName = getAttr(attrs, "attribute-name");
                    Boolean disableConversion = Boolean.valueOf(getAttr(attrs, "disable-conversion"));
                    if (disableConversion)
                    {
                        mmd.setTypeConverterDisabled();
                    }
                    else if (!StringUtils.isWhitespace(converterClassName))
                    {
                        if (StringUtils.isWhitespace(convAttrName))
                        {
                            if (mmd.hasCollection()) // TODO What if <collection> not yet added?
                            {
                                ElementMetaData elemmd = mmd.getElementMetaData();
                                if (elemmd == null)
                                {
                                    elemmd = new ElementMetaData();
                                    mmd.setElementMetaData(elemmd);
                                }
                                elemmd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterClassName);
                            }
                            else
                            {
                                mmd.setTypeConverterName(converterClassName);
                            }
                        }
                        else
                        {
                            if ("key".equals(convAttrName))
                            {
                                KeyMetaData keymd = mmd.getKeyMetaData();
                                if (keymd == null)
                                {
                                    keymd = new KeyMetaData();
                                    mmd.setKeyMetaData(keymd);
                                }
                                keymd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterClassName);
                            }
                            else if ("value".equals(convAttrName))
                            {
                                ValueMetaData valmd = mmd.getValueMetaData();
                                if (valmd == null)
                                {
                                    valmd = new ValueMetaData();
                                    mmd.setValueMetaData(valmd);
                                }
                                valmd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterClassName);
                            }
                            else
                            {
                                // TODO Support attributeName to convert field of embedded object, or field of key/value
                                NucleusLogger.METADATA.warn("Field " + mmd.getFullFieldName() + 
                                    " has <convert> for attribute " + convAttrName + " but this is not yet fully supported. Ignored");
                            }
                        }

                        TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();
                        if (typeMgr.getTypeConverterForName(converterClassName) == null)
                        {
                            // Not yet cached an instance of this converter so create one
                            // TODO Support injectable AttributeConverters
                            Class entityConvCls = clr.classForName(converterClassName);
                            AttributeConverter entityConv = JPATypeConverterUtils.createAttributeConverterInstance(mmgr.getNucleusContext(), entityConvCls);

                            // Extract attribute and datastore types for this converter
                            Class attrType = JPATypeConverterUtils.getAttributeTypeForAttributeConverter(entityConv.getClass(), null);
                            Class dbType = JPATypeConverterUtils.getDatabaseTypeForAttributeConverter(entityConv.getClass(), attrType, null);

                            // Register the TypeConverter under the name of the AttributeConverter class
                            TypeConverter conv = new JPATypeConverter(entityConv);
                            typeMgr.registerConverter(converterClassName, conv, attrType, dbType, false, null);
                        }
                    }
                }

                pushStack(mmd);
            }
            else if (localName.equals("converter"))
            {
                if (isPersistenceContext()) // Not required whilst enhancing
                {
                    String converterClassName = getAttr(attrs, "class");
                    Boolean autoApply = Boolean.valueOf(getAttr(attrs, "auto-apply"));

                    TypeManager typeMgr = mmgr.getNucleusContext().getTypeManager();
                    Class entityConvCls = clr.classForName(converterClassName);
                    Class attrType = JPATypeConverterUtils.getAttributeTypeForAttributeConverter(entityConvCls, null);
                    Class dbType = JPATypeConverterUtils.getDatabaseTypeForAttributeConverter(entityConvCls, attrType, null);

                    if (attrType != null)
                    {
                        // Register the TypeConverter under the name of the AttributeConverter class
                        TypeConverter typeConv = typeMgr.getTypeConverterForName(converterClassName);
                        if (typeConv == null)
                        {
                            // Not yet cached an instance of this converter so create one
                            typeConv = new JPATypeConverter(JPATypeConverterUtils.createAttributeConverterInstance(mmgr.getNucleusContext(), entityConvCls));
                            typeMgr.registerConverter(converterClassName, typeConv, attrType, dbType, autoApply, attrType.getName());
                        }
                        else
                        {
                            // Update the "autoApply" in case we simply registered this converter for a member
                            typeMgr.registerConverter(converterClassName, typeConv, attrType, dbType, autoApply, attrType.getName());
                        }
                    }
                }
            }
            else if (localName.equals("lob"))
            {
                AbstractMemberMetaData fmd = (AbstractMemberMetaData)getStack();
                fmd.setStoreInLob(); // Just mark it as to be stored in a "lob" and let the MetaData sort it out
            }
            else if (localName.equals("enumerated"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("temporal"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("transient"))
            {
                // Transient field
                ClassMetaData cmd = (ClassMetaData)getStack();
                AbstractMemberMetaData mmd = newTransientFieldObject(cmd, getAttr(attrs, "name"));
                cmd.addMember(mmd);

                pushStack(mmd);
            }
            else if (localName.equals("embedded"))
            {
                // Embedded field (Entity, or Collection of Entity)
                AbstractClassMetaData cmd = (AbstractClassMetaData)getStack();
                AbstractMemberMetaData mmd = newEmbeddedFieldObject(cmd, getAttr(attrs, "name"));
                cmd.addMember(mmd);
                mmd.setEmbedded(true);
                mmd.setCascadePersist(true);
                mmd.setCascadeDelete(true);
                mmd.setCascadeAttach(true);

                pushStack(mmd);
            }
            else if (localName.equals("one-to-many"))
            {
                // 1-N field
                ClassMetaData cmd = (ClassMetaData)getStack();
                AbstractMemberMetaData mmd = newFieldObject(cmd, attrs, "LAZY");
                String targetEntityName = getAttr(attrs, "target-entity");
                if (!StringUtils.isWhitespace(targetEntityName))
                {
                    mmd.setTargetClassName(targetEntityName);
                }
                mmd.setOrdered(); // Mark as ordered so we know we're using JPA
                if (mmd.getMappedBy() == null && mmd.getJoinMetaData() == null)
                {
                    if (mmd.getColumnMetaData() != null)
                    {
                        // 1-N FK UNI since JoinColumn specified and no JoinTable
                    }
                    else
                    {
                        // JPA1 : 1-N uni with no join specified (yet) so add one (see JPA1 spec [9.1.24])
                        mmd.setJoinMetaData(new JoinMetaData());
                    }
                }
                mmd.setRelationTypeString("OneToMany");

                pushStack(mmd);
            }
            else if (localName.equals("one-to-one"))
            {
                // 1-1 field
                ClassMetaData cmd = (ClassMetaData)getStack();
                AbstractMemberMetaData mmd = newFieldObject(cmd, attrs, "EAGER");
                String targetEntityName = getAttr(attrs, "target-entity");
                if (!StringUtils.isWhitespace(targetEntityName))
                {
                    mmd.setTargetClassName(targetEntityName);
                }
                String mapsId = getAttr(attrs, "maps-id");
                if (mapsId != null)
                {
                    mmd.setMapsIdAttribute(mapsId);
                }
                mmd.setRelationTypeString("OneToOne");

                pushStack(mmd);
            }
            else if (localName.equals("many-to-one"))
            {
                // N-1 field
                ClassMetaData cmd = (ClassMetaData)getStack();
                AbstractMemberMetaData mmd = newFieldObject(cmd, attrs, "EAGER");
                String targetEntityName = getAttr(attrs, "target-entity");
                if (!StringUtils.isWhitespace(targetEntityName))
                {
                    mmd.setTargetClassName(targetEntityName);
                }
                String mapsId = getAttr(attrs, "maps-id");
                if (mapsId != null)
                {
                    mmd.setMapsIdAttribute(mapsId);
                }
                mmd.setRelationTypeString("ManyToOne");

                pushStack(mmd);
            }
            else if (localName.equals("many-to-many"))
            {
                // M-N field
                ClassMetaData cmd = (ClassMetaData)getStack();
                AbstractMemberMetaData mmd = newFieldObject(cmd, attrs, "LAZY");
                String targetEntityName = getAttr(attrs, "target-entity");
                if (!StringUtils.isWhitespace(targetEntityName))
                {
                    mmd.setTargetClassName(targetEntityName);
                }
                mmd.setOrdered(); // Mark as ordered so we know we're using JPA
                if (mmd.getMappedBy() == null && mmd.getJoinMetaData() == null)
                {
                    // M-N and no join specified (yet) so add one
                    mmd.setJoinMetaData(new JoinMetaData());
                }
                mmd.setRelationTypeString("ManyToMany");

                pushStack(mmd);
            }
            else if (localName.equals("element-collection"))
            {
                // Element collection for this field (1-N using non-PC elements)
                ClassMetaData cmd = (ClassMetaData)getStack();
                AbstractMemberMetaData mmd = newFieldObject(cmd, attrs, "LAZY");
                JoinMetaData joinmd = new JoinMetaData();
                mmd.setJoinMetaData(joinmd);
                mmd.setCascadePersist(true);
                mmd.setCascadeAttach(true);
                mmd.setCascadeDelete(true);
                mmd.setCascadeDetach(true);
                mmd.setCascadeRefresh(true);
                pushStack(joinmd); // Use join so we can distinguish "element-collection"
            }
            else if (localName.equals("collection-table"))
            {
                // Collection table for this field (1-N using non-PC elements)
                JoinMetaData joinmd = (JoinMetaData)getStack();
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)joinmd.getParent();
                String nameStr = getAttr(attrs, "name");
                if (!StringUtils.isWhitespace(nameStr))
                {
                    mmd.setTable(nameStr);
                }
                String catStr = getAttr(attrs, "catalog");
                if (!StringUtils.isWhitespace(catStr))
                {
                    mmd.setCatalog(catStr);
                }
                String schStr = getAttr(attrs, "schema");
                if (!StringUtils.isWhitespace(schStr))
                {
                    mmd.setSchema(schStr);
                }
            }
            else if (localName.equals("map-key"))
            {
                // Key of a Map (field/property of the value class)
                MetaData md = getStack();
                if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData fmd = (AbstractMemberMetaData)getStack();
                    String mappedByFieldName = getAttr(attrs, "name");
                    if (StringUtils.isWhitespace(mappedByFieldName))
                    {
                        mappedByFieldName = "#PK"; // Special value understood by MapMetaData.populate()
                    }
                    KeyMetaData keymd = fmd.getKeyMetaData();
                    if (keymd == null)
                    {
                        keymd = new KeyMetaData();
                        fmd.setKeyMetaData(keymd);
                    }
                    keymd.setMappedBy(mappedByFieldName);
                }
                else if (md instanceof JoinMetaData)
                {
                    // Map<NonPC, NonPC> defining the key
                    JoinMetaData joinmd = (JoinMetaData)md;
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)joinmd.getParent();
                    String mappedByFieldName = getAttr(attrs, "name");
                    if (StringUtils.isWhitespace(mappedByFieldName))
                    {
                        mappedByFieldName = "#PK"; // Special value understood by MapMetaData.populate()
                    }
                    KeyMetaData keymd = mmd.getKeyMetaData();
                    if (keymd == null)
                    {
                        keymd = new KeyMetaData();
                        mmd.setKeyMetaData(keymd);
                    }
                    keymd.setMappedBy(mappedByFieldName);
                }
            }
            else if (localName.equals("map-key-temporal"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("map-key-enumerated"))
            {
                // Processed elsewhere
            }
            else if (localName.equals("map-key-attribute-override"))
            {
                // TODO Support this
                NucleusLogger.METADATA.info(">> Dont currently support map-key-attribute-override element");
            }
            else if (localName.equals("map-key-class"))
            {
                MetaData md = getStack();
                String clsName = getAttr(attrs, "class");
                if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    MapMetaData mapmd = mmd.getMap();
                    if (mapmd == null)
                    {
                        mapmd = mmd.newMapMetaData();
                    }
                    mapmd.setKeyType(clsName);
                }
            }
            else if (localName.equals("map-key-convert"))
            {
                MetaData md = getStack();
                if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;

                    KeyMetaData keymd = mmd.getKeyMetaData();
                    if (keymd == null)
                    {
                        keymd = new KeyMetaData();
                        mmd.setKeyMetaData(keymd);
                    }

                    String converterClassName = getAttr(attrs, "converter");
                    Boolean disableConversion = Boolean.valueOf(getAttr(attrs, "disable-conversion"));
                    if (disableConversion)
                    {
                        mmd.setTypeConverterDisabled();
                    }
                    else if (!StringUtils.isWhitespace(converterClassName))
                    {
                        keymd.addExtension(MetaData.EXTENSION_MEMBER_TYPE_CONVERTER_NAME, converterClassName);
                    }
                }
            }
            else if (localName.equals("index"))
            {
                String idxName = getAttr(attrs, "name");
                String idxColStr = getAttr(attrs, "column-list");
                String idxUnique = getAttr(attrs, "unique");

                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    if (!StringUtils.isWhitespace(idxColStr))
                    {
                        IndexMetaData idxmd = cmd.newIndexMetaData();
                        String[] colNames = StringUtils.split(idxColStr, ",");
                        for (int i=0;i<colNames.length;i++)
                        {
                            idxmd.addColumn(colNames[i]);
                        }
                        if (!StringUtils.isWhitespace(idxName))
                        {
                            idxmd.setName(idxName);
                        }
                        if (!StringUtils.isWhitespace(idxUnique))
                        {
                            idxmd.setUnique(Boolean.valueOf(idxUnique));
                        }
                    }
                }
                else if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    if (!StringUtils.isWhitespace(idxColStr))
                    {
                        IndexMetaData idxmd = mmd.newIndexMetaData();
                        String[] colNames = StringUtils.split(idxColStr, ",");
                        for (int i=0;i<colNames.length;i++)
                        {
                            idxmd.addColumn(colNames[i]);
                        }
                        if (!StringUtils.isWhitespace(idxName))
                        {
                            idxmd.setName(idxName);
                        }
                        if (!StringUtils.isWhitespace(idxUnique))
                        {
                            idxmd.setUnique(Boolean.valueOf(idxUnique));
                        }
                    }
                }
                else if (md instanceof JoinMetaData)
                {
                    // TODO Support indexes here
                    NucleusLogger.METADATA.info(">> Dont currently support index element with parent=" + md);
                }
                else
                {
                    NucleusLogger.METADATA.info(">> Dont currently support index element with parent " + md);
                }
            }
            else if (localName.equals("foreign-key"))
            {
                String fkName = getAttr(attrs, "name");
                String fkDefinition = getAttr(attrs, "foreign-key-definition");
                String fkConstraintMode = getAttr(attrs, "constraint-mode");
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    ForeignKeyMetaData fkmd = cmd.newForeignKeyMetaData();
                    fkmd.setName(fkName);
                    fkmd.setFkDefinition(fkDefinition);
                    if (fkConstraintMode != null)
                    {
                        if (fkConstraintMode.equalsIgnoreCase("no_constraint"))
                        {
                            fkmd.setFkDefinitionApplies(false);
                        }
                    }
                }
                else if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    ForeignKeyMetaData fkmd = mmd.newForeignKeyMetaData();
                    fkmd.setName(fkName);
                    fkmd.setFkDefinition(fkDefinition);
                    if (fkConstraintMode != null)
                    {
                        if (fkConstraintMode.equalsIgnoreCase("no_constraint"))
                        {
                            fkmd.setFkDefinitionApplies(false);
                        }
                    }
                }
                else if (md instanceof JoinMetaData)
                {
                    JoinMetaData joinmd = (JoinMetaData)md;
                    ForeignKeyMetaData joinFkmd = joinmd.getForeignKeyMetaData();
                    if (joinFkmd == null)
                    {
                        joinFkmd = joinmd.newForeignKeyMetaData();
                    }
                    joinFkmd.setName(fkName);
                    joinFkmd.setFkDefinition(fkDefinition);
                    if (fkConstraintMode != null)
                    {
                        if (fkConstraintMode.equalsIgnoreCase("no_constraint"))
                        {
                            joinFkmd.setFkDefinitionApplies(false);
                        }
                    }
                }
                else
                {
                    // TODO Support foreign-key element in other parts of the XML. What are they?
                    NucleusLogger.METADATA.warn(">> Dont currently support foreign-key element with parent " + md.getClass().getName());
                }
            }
            else if (localName.equals("inverse-foreign-key"))
            {
                String fkName = getAttr(attrs, "name");
                String fkDefinition = getAttr(attrs, "foreign-key-definition");
                String fkConstraintMode = getAttr(attrs, "constraint-mode");
                MetaData md = getStack();
                if (md instanceof JoinMetaData)
                {
                    JoinMetaData joinmd = (JoinMetaData)md;
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData) joinmd.getParent();
                    ForeignKeyMetaData fkmd = mmd.getForeignKeyMetaData();
                    if (fkmd == null)
                    {
                        fkmd = mmd.newForeignKeyMetaData();
                    }
                    fkmd.setName(fkName);
                    fkmd.setFkDefinition(fkDefinition);
                    if (fkConstraintMode != null)
                    {
                        if (fkConstraintMode.equalsIgnoreCase("no_constraint"))
                        {
                            fkmd.setFkDefinitionApplies(false);
                        }
                    }
                }
                else
                {
                    NucleusLogger.METADATA.warn("inverse-foreign-key not yet supported with parent=" + getStack());
                }
            }
            else if (localName.equals("primary-key-foreign-key"))
            {
                String fkName = getAttr(attrs, "name");
                String fkDefinition = getAttr(attrs, "foreign-key-definition");
                String fkConstraintMode = getAttr(attrs, "constraint-mode");
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
                    if (inhmd == null)
                    {
                        inhmd = cmd.newInheritanceMetaData();
                    }
                    JoinMetaData joinmd = inhmd.getJoinMetaData();
                    if (joinmd == null)
                    {
                        joinmd = inhmd.newJoinMetaData();
                    }
                    ForeignKeyMetaData fkmd = joinmd.newForeignKeyMetaData();
                    fkmd.setName(fkName);
                    fkmd.setFkDefinition(fkDefinition);
                    if (fkConstraintMode != null)
                    {
                        if (fkConstraintMode.equalsIgnoreCase("no_constraint"))
                        {
                            fkmd.setFkDefinitionApplies(false);
                        }
                    }
                }
                else
                {
                    NucleusLogger.METADATA.warn("primary-key-foreign-key not yet supported with parent=" + getStack());
                }
            }
            else if (localName.equals("map-key-foreign-key"))
            {
                // TODO Implement this
                NucleusLogger.METADATA.warn("map-key-foreign-key not yet supported md=" + getStack());
            }
            else if (localName.equals("order-by"))
            {
                // Processed in endElement()
            }
            else if (localName.equals("order-column"))
            {
                String columnName = getAttr(attrs, "name");
                OrderMetaData ordermd = new OrderMetaData();
                ordermd.setColumnName(columnName);

                ColumnMetaData colmd = new ColumnMetaData();
                colmd.setName(getAttr(attrs, "name"));
                colmd.setAllowsNull(getAttr(attrs, "nullable"));
                colmd.setInsertable(getAttr(attrs, "insertable"));
                colmd.setUpdateable(getAttr(attrs, "updatable"));
                String columnDdl = getAttr(attrs, "column-definition");
                if (columnDdl != null)
                {
                    colmd.setColumnDdl(columnDdl);
                }
                ordermd.addColumn(colmd);

                MetaData md = getStack();
                if (md instanceof JoinMetaData)
                {
                    JoinMetaData joinmd = (JoinMetaData)md;
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData) joinmd.getParent();
                    mmd.setOrderMetaData(ordermd);
                }
                else
                {
                    AbstractMemberMetaData fmd = (AbstractMemberMetaData)getStack();
                    fmd.setOrderMetaData(ordermd);
                }
            }
            else if (localName.equals("cascade"))
            {
                // Do nothing
            }
            else if (localName.equals("cascade-type"))
            {
                // Handled in elements below
            }
            else if (localName.equals("cascade-all"))
            {
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)getStack();
                mmd.setCascadePersist(true);
                mmd.setCascadeAttach(true);
                mmd.setCascadeDelete(true);
                mmd.setCascadeDetach(true);
                mmd.setCascadeRefresh(true);
            }
            else if (localName.equals("cascade-persist"))
            {
                MetaData md = getStack();
                if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    mmd.setCascadePersist(true);
                }
                else if (md instanceof FileMetaData)
                {
                    // Specified at <persistence-unit-defaults>
                    defaultCascadePersist = true;
                }
            }
            else if (localName.equals("cascade-merge"))
            {
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)getStack();
                mmd.setCascadeAttach(true);
            }
            else if (localName.equals("cascade-remove"))
            {
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)getStack();
                mmd.setCascadeDelete(true);
            }
            else if (localName.equals("cascade-refresh"))
            {
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)getStack();
                mmd.setCascadeRefresh(true);
            }
            else if (localName.equals("cascade-detach"))
            {
                AbstractMemberMetaData mmd = (AbstractMemberMetaData)getStack();
                mmd.setCascadeDetach(true);
            }
            else if (localName.equals("version"))
            {
                if (getStack() instanceof ClassMetaData)
                {
                    // Version field
                    ClassMetaData cmd = (ClassMetaData)getStack();
                    AbstractMemberMetaData mmd = newFieldObject(cmd, attrs, "EAGER");

                    // Tag this field as the version field
                    VersionMetaData vermd = cmd.newVersionMetaData();
                    vermd.setStrategy(VersionStrategy.VERSION_NUMBER).setMemberName(mmd.getName());

                    pushStack(mmd);
                }
            }
            else if (localName.equals("discriminator-value"))
            {
                // Processed in endElement()
            }
            else if (localName.equals("discriminator-column"))
            {
                ClassMetaData cmd = (ClassMetaData)getStack();
                InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
                if (inhmd == null)
                {
                    // Add an empty inheritance specification
                    inhmd = new InheritanceMetaData();
                    cmd.setInheritanceMetaData(inhmd);
                }

                DiscriminatorMetaData dismd = inhmd.getDiscriminatorMetaData();
                if (dismd == null)
                {
                    // User hasn't specified discriminator value so use "provider-specific function" (JPA 9.1.3.1)
                    if (mmgr.getNucleusContext().getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_USE_DISCRIMINATOR_DEFAULT_CLASS_NAME))
                    {
                        // Legacy handling, DN <= 5.0.2
                        dismd = inhmd.newDiscriminatorMetaData();
                        dismd.setStrategy(DiscriminatorStrategy.VALUE_MAP);
                        dismd.setValue(cmd.getFullClassName()); // Default to class name as value unless set
                        dismd.setIndexed("true");
                    }
                    else
                    {
                        dismd = inhmd.newDiscriminatorMetaData();
                        dismd.setStrategy(DiscriminatorStrategy.VALUE_MAP_ENTITY_NAME);
                        dismd.setIndexed("true");
                    }
                }

                String jdbcType = null;
                String discType = getAttr(attrs, "discriminator-type");
                if (discType != null)
                {
                    if (discType.equalsIgnoreCase("STRING"))
                    {
                        jdbcType = "VARCHAR";
                    }
                    else if (discType.equalsIgnoreCase("CHAR"))
                    {
                        jdbcType = "CHAR";
                    }
                    else if (discType.equalsIgnoreCase("INTEGER"))
                    {
                        jdbcType = "INTEGER";
                    }
                }
                ColumnMetaData colmd = new ColumnMetaData();
                colmd.setName(getAttr(attrs, "name"));
                colmd.setJdbcType(jdbcType);
                colmd.setLength(getAttr(attrs, "length"));
                String columnDdl = getAttr(attrs, "column-definition");
                if (columnDdl != null)
                {
                    colmd.setColumnDdl(columnDdl);
                }
                dismd.setColumnMetaData(colmd);
            }
            else if (localName.equals("generated-value"))
            {
                // generated value for this field
                MetaData md = getStack();
                if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData fmd = (AbstractMemberMetaData)getStack();
                    String strategy = getAttr(attrs, "strategy");
                    if (strategy != null)
                    {
                        if (strategy.equalsIgnoreCase("auto"))
                        {
                            fmd.setValueStrategy(ValueGenerationStrategy.NATIVE);
                        }
                        else if (strategy.equalsIgnoreCase("table"))
                        {
                            fmd.setValueStrategy(ValueGenerationStrategy.INCREMENT);
                        }
                        else if (strategy.equalsIgnoreCase("sequence"))
                        {
                            fmd.setValueStrategy(ValueGenerationStrategy.SEQUENCE);
                        }
                        else if (strategy.equalsIgnoreCase("identity"))
                        {
                            fmd.setValueStrategy(ValueGenerationStrategy.IDENTITY);
                        }
                    }
                    fmd.setValueGeneratorName(getAttr(attrs, "generator"));
                }
                else if (md instanceof DatastoreIdentityMetaData)
                {
                    // DataNucleus extension
                    DatastoreIdentityMetaData idmd = (DatastoreIdentityMetaData)md;
                    String strategy = getAttr(attrs, "strategy");
                    if (strategy != null)
                    {
                        if (strategy.equalsIgnoreCase("auto"))
                        {
                            idmd.setValueStrategy(ValueGenerationStrategy.NATIVE);
                        }
                        else if (strategy.equalsIgnoreCase("table"))
                        {
                            idmd.setValueStrategy(ValueGenerationStrategy.INCREMENT);
                        }
                        else if (strategy.equalsIgnoreCase("sequence"))
                        {
                            idmd.setValueStrategy(ValueGenerationStrategy.SEQUENCE);
                        }
                        else if (strategy.equalsIgnoreCase("identity"))
                        {
                            idmd.setValueStrategy(ValueGenerationStrategy.IDENTITY);
                        }
                        else if (strategy.equalsIgnoreCase("uuid"))
                        {
                            idmd.setValueStrategy(ValueGenerationStrategy.UUID);
                        }
                    }
                    idmd.setValueGeneratorName(getAttr(attrs, "generator"));
                }
            }
            else if (localName.equals("join-table"))
            {
                // Join table for this field
                AbstractMemberMetaData fmd = (AbstractMemberMetaData)getStack();
                JoinMetaData joinmd = new JoinMetaData();
                String tableName = getAttr(attrs, "name");
                String schemaName = getAttr(attrs, "schema");
                String catalogName = getAttr(attrs, "catalog");

                fmd.setJoinMetaData(joinmd);
                if (!StringUtils.isWhitespace(tableName))
                {
                    fmd.setTable(tableName);
                }
                if (!StringUtils.isWhitespace(schemaName))
                {
                    fmd.setSchema(schemaName);
                }
                if (!StringUtils.isWhitespace(catalogName))
                {
                    fmd.setSchema(catalogName);
                }
                pushStack(joinmd);
            }
            else if (localName.equals("column"))
            {
                // Column for the current field
                MetaData md = getStack();
                ColumnMetaData colmd = new ColumnMetaData();
                colmd.setName(getAttr(attrs, "name"));
                colmd.setLength(getAttr(attrs, "length"));
                colmd.setScale(getAttr(attrs, "scale"));
                colmd.setAllowsNull(getAttr(attrs, "nullable"));
                colmd.setInsertable(getAttr(attrs, "insertable"));
                colmd.setUpdateable(getAttr(attrs, "updatable"));
                colmd.setUnique(getAttr(attrs, "unique"));
                String jdbcType = getAttr(attrs, "jdbc-type"); // DN extension
                if (jdbcType != null)
                {
                    colmd.setJdbcType(jdbcType);
                }
                String sqlType = getAttr(attrs, "sql-type"); // DN extension
                if (sqlType != null)
                {
                    colmd.setSqlType(sqlType);
                }
                String position = getAttr(attrs, "position"); // DN extension
                if (position != null)
                {
                    colmd.setPosition(position);
                }

                String columnDdl = getAttr(attrs, "column-definition");
                if (columnDdl != null)
                {
                    colmd.setColumnDdl(columnDdl);
                }
                if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData fmd = (AbstractMemberMetaData)md;
                    if (overrideMmd != null)
                    {
                        // Actually the column of an [attribute/association]-override
                        fmd = overrideMmd;
                    }

                    fmd.addColumn(colmd);
                    String table = getAttr(attrs, "table");
                    if (!StringUtils.isWhitespace(table))
                    {
                        // Using secondary table
                        fmd.setTable(table);
                    }
                }
                else if (md instanceof JoinMetaData)
                {
                    // <element-collection>
                    //     <column .../>
                    // </element-collection>
                    JoinMetaData joinmd = (JoinMetaData)md;
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)joinmd.getParent();

                    // We don't know if this field is a collection or map so just put this as the column on the member and let it's population sort it out
                    mmd.addColumn(colmd);
                }
                else if (md instanceof DatastoreIdentityMetaData)
                {
                    DatastoreIdentityMetaData idmd = (DatastoreIdentityMetaData)md;
                    idmd.setColumnMetaData(colmd);
                }
                else if (md instanceof VersionMetaData)
                {
                    // DataNucleus extension
                    VersionMetaData vermd = (VersionMetaData)md;
                    vermd.setColumnMetaData(colmd);
                }
            }
            else if (localName.equals("map-key-column"))
            {
                // Column for the current field
                MetaData md = getStack();
                
                ColumnMetaData colmd = new ColumnMetaData();
                colmd.setName(getAttr(attrs, "name"));
                if (getAttr(attrs, "precision")!=null)
                {
                    colmd.setLength(getAttr(attrs, "precision"));
                }
                else
                {
                    colmd.setLength(getAttr(attrs, "length"));
                }
                colmd.setScale(getAttr(attrs, "scale"));
                colmd.setAllowsNull(getAttr(attrs, "nullable"));
                colmd.setInsertable(getAttr(attrs, "insertable"));
                colmd.setUpdateable(getAttr(attrs, "updatable"));
                colmd.setUnique(getAttr(attrs, "unique"));
                String columnDdl = getAttr(attrs, "column-definition");
                if (columnDdl != null)
                {
                    colmd.setColumnDdl(columnDdl);
                }

                if (md instanceof JoinMetaData)
                {
                    JoinMetaData joinmd = (JoinMetaData)md;
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)joinmd.getParent();
                    KeyMetaData keymd = mmd.getKeyMetaData();
                    if (keymd == null)
                    {
                        keymd = new KeyMetaData();
                        mmd.setKeyMetaData(keymd);
                    }
                    keymd.addColumn(colmd);
                }
                else if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    KeyMetaData keymd = mmd.getKeyMetaData();
                    if (keymd == null)
                    {
                        keymd = new KeyMetaData();
                        mmd.setKeyMetaData(keymd);
                    }
                    keymd.addColumn(colmd);
                }
            }
            else if (localName.equals("map-key-join-column"))
            {
                // Column for the current field
                MetaData md = getStack();
                
                ColumnMetaData colmd = new ColumnMetaData();
                colmd.setName(getAttr(attrs, "name"));
                if (getAttr(attrs, "precision")!=null)
                {
                    colmd.setLength(getAttr(attrs, "precision"));
                }
                else
                {
                    colmd.setLength(getAttr(attrs, "length"));
                }
                colmd.setScale(getAttr(attrs, "scale"));
                colmd.setAllowsNull(getAttr(attrs, "nullable"));
                colmd.setInsertable(getAttr(attrs, "insertable"));
                colmd.setUpdateable(getAttr(attrs, "updatable"));
                colmd.setUnique(getAttr(attrs, "unique"));
                String columnDdl = getAttr(attrs, "column-definition");
                if (columnDdl != null)
                {
                    colmd.setColumnDdl(columnDdl);
                }

                if (md instanceof JoinMetaData)
                {
                    JoinMetaData joinmd = (JoinMetaData)md;
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)joinmd.getParent();
                    KeyMetaData keymd = mmd.getKeyMetaData();
                    if (keymd == null)
                    {
                        keymd = new KeyMetaData();
                        mmd.setKeyMetaData(keymd);
                    }
                    keymd.addColumn(colmd);
                }
                else if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    KeyMetaData keymd = mmd.getKeyMetaData();
                    if (keymd == null)
                    {
                        keymd = new KeyMetaData();
                        mmd.setKeyMetaData(keymd);
                    }
                    keymd.addColumn(colmd);
                }
            }
            else if (localName.equals("join-column"))
            {
                MetaData md = getStack();
                if (md instanceof JoinMetaData)
                {
                    JoinMetaData joinmd = (JoinMetaData)md;
                    ColumnMetaData colmd = new ColumnMetaData();
                    colmd.setName(getAttr(attrs, "name"));
                    colmd.setTarget(getAttr(attrs, "referenced-column-name"));
                    colmd.setAllowsNull(getAttr(attrs, "nullable"));
                    colmd.setInsertable(getAttr(attrs, "insertable"));
                    colmd.setUpdateable(getAttr(attrs, "updatable"));
                    colmd.setUnique(getAttr(attrs, "unique"));
                    String columnDdl = getAttr(attrs, "column-definition");
                    if (columnDdl != null)
                    {
                        colmd.setColumnDdl(columnDdl);
                    }
                    String indexedStr = getAttr(attrs, "indexed"); // DN extension
                    if (!StringUtils.isWhitespace(indexedStr))
                    {
                        joinmd.setIndexed(IndexedValue.getIndexedValue(indexedStr));
                    }
                    joinmd.addColumn(colmd);
                }
                else if (md instanceof AbstractMemberMetaData)
                {
                    // N-1, 1-1, 1-N (FK). Just set <column> for the field. 
                    // TODO Cater for 1-N FK ?
                    // TODO Make use of "table"
                    AbstractMemberMetaData fmd = (AbstractMemberMetaData)md;
                    ColumnMetaData colmd = new ColumnMetaData();
                    colmd.setName(getAttr(attrs, "name"));
                    colmd.setTarget(getAttr(attrs, "referenced-column-name"));
                    colmd.setAllowsNull(getAttr(attrs, "nullable"));
                    colmd.setInsertable(getAttr(attrs, "insertable"));
                    colmd.setUpdateable(getAttr(attrs, "updatable"));
                    colmd.setUnique(getAttr(attrs, "unique"));
                    fmd.addColumn(colmd);
                }
            }
            else if (localName.equals("inverse-join-column"))
            {
                MetaData md = getStack();
                if (md instanceof JoinMetaData)
                {
                    // Join table column that is FK to the element table
                    JoinMetaData joinmd = (JoinMetaData)md;
                    ElementMetaData elemmd = null;
                    AbstractMemberMetaData fmd = (AbstractMemberMetaData)joinmd.getParent();
                    if (fmd.getElementMetaData() != null)
                    {
                        elemmd = fmd.getElementMetaData();
                    }
                    else
                    {
                        elemmd = new ElementMetaData();
                        fmd.setElementMetaData(elemmd);
                    }
                    ColumnMetaData colmd = new ColumnMetaData();
                    colmd.setName(getAttr(attrs, "name"));
                    colmd.setTarget(getAttr(attrs, "referenced-column-name"));
                    colmd.setAllowsNull(getAttr(attrs, "nullable"));
                    colmd.setInsertable(getAttr(attrs, "insertable"));
                    colmd.setUpdateable(getAttr(attrs, "updatable"));
                    colmd.setUnique(getAttr(attrs, "unique"));
                    elemmd.addColumn(colmd);
                }
            }
            else if (localName.equals("unique-constraint"))
            {
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Unique constraint on primary table
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    UniqueMetaData unimd = new UniqueMetaData();
                    unimd.setTable(cmd.getTable()); // Columns are in subelement
                    cmd.addUniqueConstraint(unimd);
                    pushStack(unimd);
                }
                else if (md instanceof JoinMetaData)
                {
                    // Unique constraint on secondary table or join table
                    JoinMetaData joinmd = (JoinMetaData)md;
                    UniqueMetaData unimd = new UniqueMetaData();
                    joinmd.setUniqueMetaData(unimd);
                    pushStack(unimd);
                }
                else
                {
                    NucleusLogger.METADATA.info(">> Dont currently support unique-constraint element with parent " + md.getClass().getName());
                }
            }
            else if (localName.equals("named-entity-graph"))
            {
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    JPAEntityGraph entityGraph = new JPAEntityGraph(mmgr, getAttr(attrs, "name"), clr.classForName(cmd.getFullClassName()));
                    GraphHolder graphHolder = new GraphHolder();
                    graphHolder.graph = entityGraph;
                    graphHolderStack.push(graphHolder);
                    Boolean includeAll = Boolean.valueOf(getAttr(attrs, "include-all-attributes"));
                    if (includeAll)
                    {
                        entityGraph.setIncludeAll();
                    }
                }
                else
                {
                    throw new RuntimeException("Dont support named-entity-graph with parent of " + md);
                }
            }
            else if (localName.equals("named-attribute-node"))
            {
                GraphHolder graphHolder = graphHolderStack.peek();
                if (graphHolder == null)
                {
                    throw new RuntimeException("named-attribute-node has no named-entity-graph element");
                }
                String attributeName = getAttr(attrs, "name");
                String subgroupName = getAttr(attrs, "subgroup-name");
                if (!StringUtils.isWhitespace(subgroupName))
                {
                    graphHolder.attributeNameBySubgroupName.put(subgroupName, attributeName);
                }
                graphHolder.graph.addAttributeNodes(attributeName);
            }
            else if (localName.equals("subgraph"))
            {
                GraphHolder parentGraphHolder = graphHolderStack.peek();
                AbstractJPAGraph parentGraph = parentGraphHolder.graph;
                String subgraphName = getAttr(attrs, "name");
                String attributeName = (parentGraphHolder.attributeNameBySubgroupName != null ? parentGraphHolder.attributeNameBySubgroupName.get(subgraphName) : null);
                if (attributeName == null)
                {
                    throw new RuntimeException("subgraph specified with name=" + subgraphName + " but no attribute was marked as having that subgroup name");
                }

                String subgraphClassName = getAttr(attrs, "class");
                Class cls = null;
                if (!StringUtils.isWhitespace(subgraphClassName))
                {
                    cls = clr.classForName(subgraphClassName);
                }
                JPASubgraph subgraph = (cls == null ? (JPASubgraph) parentGraph.addSubgraph(attributeName) : (JPASubgraph)parentGraph.addSubgraph(attributeName, cls));
                GraphHolder graphHolder = new GraphHolder();
                graphHolder.graph = subgraph;
                graphHolderStack.push(graphHolder);
            }
            else if (localName.equals("subclass-subgraph"))
            {
                GraphHolder parentGraphHolder = graphHolderStack.peek();
                JPAEntityGraph parentGraph = (JPAEntityGraph) parentGraphHolder.graph;
                String subgraphClassName = getAttr(attrs, "class");
                Class subclass = clr.classForName(subgraphClassName);
                JPASubgraph subgraph = (JPASubgraph) parentGraph.addSubclassSubgraph(subclass);
                GraphHolder graphHolder = new GraphHolder();
                graphHolder.graph = subgraph;
                graphHolderStack.push(graphHolder);
            }
            else if (localName.equals("entity-listeners"))
            {
                // Nothing to add at this point
            }
            else if (localName.equals("entity-listener"))
            {
                MetaData md = getStack();
                EventListenerMetaData elmd = new EventListenerMetaData(getAttr(attrs, "class"));
                if (md instanceof AbstractClassMetaData)
                {
                    // Specified at <entity> or <mapped-superclass>
                    ((AbstractClassMetaData)md).addListener(elmd);
                }
                else if (md instanceof FileMetaData)
                {
                    // Specified at <persistence-unit-defaults>
                    ((FileMetaData)md).addListener(elmd);
                }

                pushStack(elmd);
            }
            else if (localName.equals("pre-persist"))
            {
                // Pre-create callback
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Specified at <entity> or <mapped-superclass>
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    EventListenerMetaData elmd = cmd.getListenerForClass(cmd.getFullClassName());
                    if (elmd == null)
                    {
                        elmd = new EventListenerMetaData(cmd.getFullClassName());
                        cmd.addListener(elmd);
                    }
                    elmd.addCallback("javax.persistence.PrePersist", getAttr(attrs, "method-name"));
                }
                else
                {
                    // Specified at <entity-listener>
                    EventListenerMetaData elmd = (EventListenerMetaData)md;
                    elmd.addCallback("javax.persistence.PrePersist", getAttr(attrs, "method-name"));
                }
            }
            else if (localName.equals("post-persist"))
            {
                // Post-create callback
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Specified at <entity> or <mapped-superclass>
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    EventListenerMetaData elmd = cmd.getListenerForClass(cmd.getFullClassName());
                    if (elmd == null)
                    {
                        elmd = new EventListenerMetaData(cmd.getFullClassName());
                        cmd.addListener(elmd);
                    }
                    elmd.addCallback("javax.persistence.PostPersist", getAttr(attrs, "method-name"));
                }
                else
                {
                    // Specified at <entity-listener>
                    EventListenerMetaData elmd = (EventListenerMetaData)md;
                    elmd.addCallback("javax.persistence.PostPersist", getAttr(attrs, "method-name"));
                }
            }
            else if (localName.equals("pre-remove"))
            {
                // Pre-delete callback
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Specified at <entity> or <mapped-superclass>
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    EventListenerMetaData elmd = cmd.getListenerForClass(cmd.getFullClassName());
                    if (elmd == null)
                    {
                        elmd = new EventListenerMetaData(cmd.getFullClassName());
                        cmd.addListener(elmd);
                    }
                    elmd.addCallback("javax.persistence.PreRemove", getAttr(attrs, "method-name"));
                }
                else
                {
                    // Specified at <entity-listener>
                    EventListenerMetaData elmd = (EventListenerMetaData)md;
                    elmd.addCallback("javax.persistence.PreRemove", getAttr(attrs, "method-name"));
                }
            }
            else if (localName.equals("post-remove"))
            {
                // Post-delete callback
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Specified at <entity> or <mapped-superclass>
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    EventListenerMetaData elmd = cmd.getListenerForClass(cmd.getFullClassName());
                    if (elmd == null)
                    {
                        elmd = new EventListenerMetaData(cmd.getFullClassName());
                        cmd.addListener(elmd);
                    }
                    elmd.addCallback("javax.persistence.PostRemove", getAttr(attrs, "method-name"));
                }
                else
                {
                    // Specified at <entity-listener>
                    EventListenerMetaData elmd = (EventListenerMetaData)md;
                    elmd.addCallback("javax.persistence.PostRemove", getAttr(attrs, "method-name"));
                }
            }
            else if (localName.equals("pre-update"))
            {
                // Pre-store callback
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Specified at <entity> or <mapped-superclass>
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    EventListenerMetaData elmd = cmd.getListenerForClass(cmd.getFullClassName());
                    if (elmd == null)
                    {
                        elmd = new EventListenerMetaData(cmd.getFullClassName());
                        cmd.addListener(elmd);
                    }
                    elmd.addCallback("javax.persistence.PreUpdate", getAttr(attrs, "method-name"));
                }
                else
                {
                    // Specified at <entity-listener>
                    EventListenerMetaData elmd = (EventListenerMetaData)md;
                    elmd.addCallback("javax.persistence.PreUpdate", getAttr(attrs, "method-name"));
                }
            }
            else if (localName.equals("post-update"))
            {
                // Post-store callback
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Specified at <entity> or <mapped-superclass>
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    EventListenerMetaData elmd = cmd.getListenerForClass(cmd.getFullClassName());
                    if (elmd == null)
                    {
                        elmd = new EventListenerMetaData(cmd.getFullClassName());
                        cmd.addListener(elmd);
                    }
                    elmd.addCallback("javax.persistence.PostUpdate", getAttr(attrs, "method-name"));
                }
                else
                {
                    // Specified at <entity-listener>
                    EventListenerMetaData elmd = (EventListenerMetaData)md;
                    elmd.addCallback("javax.persistence.PostUpdate", getAttr(attrs, "method-name"));
                }
            }
            else if (localName.equals("post-load"))
            {
                // Post-load callback
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Specified at <entity> or <mapped-superclass>
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    EventListenerMetaData elmd = cmd.getListenerForClass(cmd.getFullClassName());
                    if (elmd == null)
                    {
                        elmd = new EventListenerMetaData(cmd.getFullClassName());
                        cmd.addListener(elmd);
                    }
                    elmd.addCallback("javax.persistence.PostLoad", getAttr(attrs, "method-name"));
                }
                else
                {
                    // Specified at <entity-listener>
                    EventListenerMetaData elmd = (EventListenerMetaData)md;
                    elmd.addCallback("javax.persistence.PostLoad", getAttr(attrs, "method-name"));
                }
            }
            else if (localName.equals("attribute-override"))
            {
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Override columns for a superclass field
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    AbstractMemberMetaData fmd = newOverriddenFieldObject(cmd, attrs);
                    cmd.addMember(fmd);
                    pushStack(fmd);
                }
                else
                {
                    // Override mappings for embedded field (1-1)
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    if (mmd.hasCollection())
                    {
                        ElementMetaData elemmd = mmd.getElementMetaData();
                        if (elemmd == null)
                        {
                            elemmd = new ElementMetaData();
                            mmd.setElementMetaData(elemmd);
                        }
                        EmbeddedMetaData embmd = elemmd.getEmbeddedMetaData();
                        if (embmd == null)
                        {
                            embmd = elemmd.newEmbeddedMetaData();
                        }

                        AbstractMemberMetaData embMmd = newOverriddenEmbeddedFieldObject(embmd, attrs);
                        embmd.addMember(embMmd);
                        pushStack(mmd);
                    }
                    else if (mmd.hasMap())
                    {
                        String memberNameOverride = getAttr(attrs, "name");
                        EmbeddedMetaData embmd = null;
                        String memberName = memberNameOverride;
                        if (memberNameOverride.startsWith("key."))
                        {
                            memberName = memberNameOverride.substring(4);
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
                        else if (memberNameOverride.startsWith("value."))
                        {
                            memberName = memberNameOverride.substring(6);
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
                            throw new RuntimeException("<attribute-override> specified for map field with name of " + memberName + " yet should start with 'key.' or 'value.'!");
                        }

                        AbstractMemberMetaData embMmd = newOverriddenEmbeddedFieldObject(embmd, memberName, getAttr(attrs, "column"));
                        embmd.addMember(embMmd);
                        pushStack(mmd);
                    }
                    else
                    {
                        EmbeddedMetaData embmd = mmd.getEmbeddedMetaData();
                        if (embmd == null)
                        {
                            embmd = new EmbeddedMetaData();
                            embmd.setParent(mmd);
                            mmd.setEmbeddedMetaData(embmd);
                        }

                        AbstractMemberMetaData embMmd = newOverriddenEmbeddedFieldObject(embmd, attrs);
                        embmd.addMember(embMmd);
                        pushStack(mmd);
                    }
                }
            }
            else if (localName.equals("association-override"))
            {
                MetaData md = getStack();
                if (md instanceof AbstractClassMetaData)
                {
                    // Override columns for a superclass field
                    AbstractClassMetaData cmd = (AbstractClassMetaData)getStack();
                    AbstractMemberMetaData fmd = newOverriddenFieldObject(cmd, attrs);
                    cmd.addMember(fmd);
                    pushStack(fmd);
                }
                else
                {
                    // Override mappings for embedded field
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    EmbeddedMetaData embmd = mmd.getEmbeddedMetaData();
                    if (embmd == null)
                    {
                        embmd = new EmbeddedMetaData();
                        embmd.setParent(mmd);
                        mmd.setEmbeddedMetaData(embmd);
                    }
                    AbstractMemberMetaData embMmd = newOverriddenEmbeddedFieldObject(embmd, attrs);
                    embmd.addMember(embMmd);
                    pushStack(mmd);
                }
            }
            else if (localName.equals("exclude-default-listeners"))
            {
                AbstractClassMetaData cmd = (AbstractClassMetaData)getStack();
                cmd.excludeDefaultListeners();
            }
            else if (localName.equals("exclude-superclass-listeners"))
            {
                AbstractClassMetaData cmd = (AbstractClassMetaData)getStack();
                cmd.excludeSuperClassListeners();
            }
            else if (localName.equals("extension"))
            {
                MetaData md = getStack();
                String extKey = getAttr(attrs, "key");
                if (extKey.equals(MetaData.EXTENSION_CLASS_MULTITENANT) || extKey.equals(MetaData.EXTENSION_CLASS_MULTITENANCY_COLUMN_NAME) ||
                    extKey.equals(MetaData.EXTENSION_CLASS_MULTITENANCY_JDBC_TYPE) || extKey.equals(MetaData.EXTENSION_CLASS_MULTITENANCY_COLUMN_LENGTH))
                {
                    AbstractClassMetaData cmd = (AbstractClassMetaData)md;
                    MultitenancyMetaData mtmd = cmd.getMultitenancyMetaData();
                    if (mtmd == null)
                    {
                        mtmd = cmd.newMultitenancyMetaData();
                    }
                    if (extKey.equals(MetaData.EXTENSION_CLASS_MULTITENANCY_COLUMN_NAME))
                    {
                        mtmd.setColumnName(getAttr(attrs, "value"));
                    }
                    else if (extKey.equals(MetaData.EXTENSION_CLASS_MULTITENANCY_COLUMN_LENGTH))
                    {
                        ColumnMetaData colmd = mtmd.getColumnMetaData();
                        if (colmd == null)
                        {
                            colmd = mtmd.newColumnMetaData();
                        }
                        colmd.setLength(getAttr(attrs, "value"));
                    }
                }
                else
                {
                    md.addExtension(extKey, getAttr(attrs, "value"));
                }
            }
            else if (localName.equals("query-hint"))
            {
                MetaData md = getStack();
                if (md instanceof QueryMetaData)
                {
                    md.addExtension(getAttr(attrs, "name"), getAttr(attrs, "value"));
                }
                else if (md instanceof StoredProcQueryMetaData)
                {
                    md.addExtension(getAttr(attrs, "name"), getAttr(attrs, "value"));
                }
            }
            else
            {
                String message = Localiser.msg("044037", qName);
                NucleusLogger.METADATA.error(message);
                throw new RuntimeException(message);
            }
        }
        catch (RuntimeException ex)
        {
            NucleusLogger.METADATA.error(Localiser.msg("044042", qName, getStack(), uri), ex);
            throw ex;
        }
    }

    /**
     * Handler method called at the end of an element.
     * @param uri URI of the tag
     * @param localName local name
     * @param qName Name of element just ending
     * @throws SAXException in parsing errors
     */
    public void endElement(String uri, String localName, String qName)
    throws SAXException
    {
        if (localName.length()<1)
        {
            localName = qName;
        }
        // Save the current string for elements that have a body value
        String currentString = getString().trim();
        if (currentString.length() > 0)
        {
            MetaData md = getStack();
            if (localName.equals("schema"))
            {
                if (md instanceof FileMetaData)
                {
                    // Specified at <entity-mappings> or <persistence-unit-defaults>
                    ((FileMetaData)md).setSchema(currentString);
                }
            }
            else if (localName.equals("catalog"))
            {
                if (md instanceof FileMetaData)
                {
                    // Specified at <entity-mappings> or <persistence-unit-defaults>
                    ((FileMetaData)md).setCatalog(currentString);
                }
            }
            else if (localName.equals("delimited-identifiers"))
            {
                if (md instanceof FileMetaData)
                {
                    // TODO Support this
                }
            }
            else if (localName.equals("access"))
            {
                if (md instanceof FileMetaData)
                {
                    // Specified at <entity-mappings> or <persistence-unit-defaults>
                    if (currentString.equalsIgnoreCase("PROPERTY"))
                    {
                        // Use property access
                        propertyAccess = true;
                    }
                }
            }
            else if (localName.equals("package"))
            {
                if (md instanceof FileMetaData)
                {
                    // Add the default package
                    FileMetaData filemd = (FileMetaData)md;
                    filemd.newPackageMetaData(currentString);
                    defaultPackageName = currentString;
                }
            }
            else if (localName.equals("discriminator-value"))
            {
                if (md instanceof ClassMetaData)
                {
                    // Add the discriminator value
                    ClassMetaData cmd = (ClassMetaData)md;
                    InheritanceMetaData inhmd = cmd.getInheritanceMetaData();
                    if (inhmd == null)
                    {
                        // Add an empty inheritance specification
                        inhmd = new InheritanceMetaData();
                        cmd.setInheritanceMetaData(inhmd);
                    }
                    String discrimValue = currentString;
                    DiscriminatorMetaData dismd = inhmd.getDiscriminatorMetaData();
                    if (dismd == null)
                    {
                        dismd = inhmd.newDiscriminatorMetaData();
                    }
                    dismd.setValue(discrimValue);
                    dismd.setStrategy(DiscriminatorStrategy.VALUE_MAP);
                }
            }
            else if (localName.equals("column-name"))
            {
                if (md instanceof UniqueMetaData)
                {
                    // Column for a unique constraint
                    ((UniqueMetaData)md).addColumn(currentString);
                }
            }
            else if (localName.equals("order-by"))
            {
                if (md instanceof AbstractMemberMetaData)
                {
                    // "Ordered List" so add its ordering constraint
                    AbstractMemberMetaData fmd = (AbstractMemberMetaData)md;
                    OrderMetaData ordmd = new OrderMetaData();
                    ordmd.setOrdering(currentString);
                    fmd.setOrderMetaData(ordmd);
                }
            }
            else if (localName.equals("result-class"))
            {
                if (md instanceof StoredProcQueryMetaData)
                {
                    ((StoredProcQueryMetaData)md).addResultClass(currentString);
                }
            }
            else if (localName.equals("result-set-mapping"))
            {
                if (md instanceof StoredProcQueryMetaData)
                {
                    ((StoredProcQueryMetaData)md).addResultSetMapping(currentString);
                }
            }
            else if (localName.equals("query"))
            {
                if (md instanceof QueryMetaData)
                {
                    // Named query, so set the query string
                    ((QueryMetaData)md).setQuery(currentString);
                }
            }
            else if (localName.equals("enumerated"))
            {
                if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    String enumerationType = currentString;
                    String jdbcType = "INTEGER";
                    if (enumerationType.equalsIgnoreCase("STRING"))
                    {
                        jdbcType = "VARCHAR";
                    }
                    if (mmd.getColumnMetaData() == null)
                    {
                        ColumnMetaData colmd = new ColumnMetaData();
                        colmd.setJdbcType(jdbcType);
                        mmd.addColumn(colmd);
                    }
                    else
                    {
                        mmd.getColumnMetaData()[0].setJdbcType(jdbcType);
                    }
                }
            }
            else if (localName.equals("temporal"))
            {
                if (md instanceof AbstractMemberMetaData)
                {
                    AbstractMemberMetaData mmd = (AbstractMemberMetaData)md;
                    String enumerationType = currentString;
                    String jdbcType = null;
                    if (enumerationType.equalsIgnoreCase("DATE"))
                    {
                        jdbcType = "DATE";
                    }
                    else if (enumerationType.equalsIgnoreCase("TIME"))
                    {
                        jdbcType = "TIME";
                    }
                    else if (enumerationType.equalsIgnoreCase("TIMESTAMP"))
                    {
                        jdbcType = "TIMESTAMP";
                    }
                    if (mmd.getColumnMetaData() == null)
                    {
                        ColumnMetaData colmd = new ColumnMetaData();
                        colmd.setJdbcType(jdbcType);
                        mmd.addColumn(colmd);
                    }
                    else
                    {
                        mmd.getColumnMetaData()[0].setJdbcType(jdbcType);
                    }
                }
            }
            else if (localName.equals("map-key-temporal"))
            {
                String enumerationType = currentString;
                String jdbcType = null;
                if (enumerationType.equalsIgnoreCase("DATE"))
                {
                    jdbcType = "DATE";
                }
                else if (enumerationType.equalsIgnoreCase("TIME"))
                {
                    jdbcType = "TIME";
                }
                else if (enumerationType.equalsIgnoreCase("TIMESTAMP"))
                {
                    jdbcType = "TIMESTAMP";
                }
                if (jdbcType != null)
                {
                    AbstractMemberMetaData mmd = null;
                    if (md instanceof JoinMetaData)
                    {
                        JoinMetaData joinmd = (JoinMetaData)md;
                        mmd = (AbstractMemberMetaData)joinmd.getParent();
                    }
                    else if (md instanceof AbstractMemberMetaData)
                    {
                        mmd = (AbstractMemberMetaData)getStack();
                    }
                    if (mmd != null)
                    {
                        KeyMetaData keymd = mmd.getKeyMetaData();
                        if (keymd == null)
                        {
                            keymd = new KeyMetaData();
                            mmd.setKeyMetaData(keymd);
                        }
                        ColumnMetaData colmd = null;
                        if (keymd.getColumnMetaData() != null && keymd.getColumnMetaData().length == 1)
                        {
                            colmd = keymd.getColumnMetaData()[0];
                        }
                        else
                        {
                            colmd = keymd.newColumnMetaData();
                        }
                        colmd.setJdbcType(jdbcType);
                    }
                }
            }
            else if (localName.equals("map-key-enumerated"))
            {
                String enumerationType = currentString;
                String jdbcType = "INTEGER";
                if (enumerationType.equalsIgnoreCase("STRING"))
                {
                    jdbcType = "VARCHAR";
                }
                AbstractMemberMetaData mmd = null;
                if (md instanceof JoinMetaData)
                {
                    JoinMetaData joinmd = (JoinMetaData)md;
                    mmd = (AbstractMemberMetaData)joinmd.getParent();
                }
                else if (md instanceof AbstractMemberMetaData)
                {
                    mmd = (AbstractMemberMetaData)getStack();
                }
                if (mmd != null)
                {
                    KeyMetaData keymd = mmd.getKeyMetaData();
                    if (keymd == null)
                    {
                        keymd = new KeyMetaData();
                        mmd.setKeyMetaData(keymd);
                    }
                    ColumnMetaData colmd = null;
                    if (keymd.getColumnMetaData() != null && keymd.getColumnMetaData().length == 1)
                    {
                        colmd = keymd.getColumnMetaData()[0];
                    }
                    else
                    {
                        colmd = keymd.newColumnMetaData();
                    }
                    colmd.setJdbcType(jdbcType);
                }
            }
            else if (localName.equals("subgraph"))
            {
                // Pop the current subgraph
                graphHolderStack.pop();
            }
            else if (localName.equals("subclass-subgraph"))
            {
                // Pop the current subclass-subgraph
                graphHolderStack.pop();
            }
            else if (localName.equals("named-entity-graph"))
            {
                // Register the EntityGraph
                GraphHolder graphHolder = graphHolderStack.pop();
                JPAEntityGraph entityGraph = (JPAEntityGraph) graphHolder.graph;
                ((JPAMetaDataManager)mmgr).registerEntityGraph(entityGraph);
                graphHolderStack.clear();
            }
        }

        if (localName.equals("constructor-result"))
        {
            // Add query result constructor (and remove temporary data)
            QueryResultMetaData qrmd = (QueryResultMetaData)getStack();
            qrmd.addConstructorTypeMapping(ctrTypeClassName, ctrTypeColumns);
            ctrTypeClassName = null;
            ctrTypeColumns = null;
        }
        else if (localName.equals("entity-result"))
        {
            queryResultEntityName = null;
        }

        if (localName.equals("attribute-override") || localName.equals("association-override"))
        {
            overrideMmd = null;
        }

        // Pop the tag
        // If startElement pushes an element onto the stack need a remove here for that type
        if (localName.equals("entity") ||
            localName.equals("mapped-superclass") ||
            localName.equals("embeddable") ||
            localName.equals("entity-listener") ||
            localName.equals("attribute-override") ||
            localName.equals("association-override") ||
            localName.equals("id") ||
            localName.equals("embedded-id") ||
            localName.equals("basic") ||
            localName.equals("transient") ||
            localName.equals("embedded") ||
            localName.equals("one-to-one") ||
            localName.equals("one-to-many") ||
            localName.equals("many-to-one") ||
            localName.equals("many-to-many") ||
            localName.equals("element-collection") ||
            localName.equals("version") ||
            localName.equals("datastore-id") ||
            localName.equals("surrogate-version") ||
            localName.equals("secondary-table") ||
            localName.equals("join-table") ||
            localName.equals("unique-constraint") ||
            localName.equals("named-query") ||
            localName.equals("named-native-query") ||
            localName.equals("named-stored-procedure-query") ||
            localName.equals("sql-result-set-mapping"))
        {
            popStack();
        }
    }
}