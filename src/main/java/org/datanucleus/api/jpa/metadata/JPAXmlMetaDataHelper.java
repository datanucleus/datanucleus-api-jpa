/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.util.StringUtils;

/**
 * Helper class that can convert internal metadata into (JPA) orm.xml metadata.
 * TODO Make this complete. Much is lacking currently
 */
public class JPAXmlMetaDataHelper
{
    public JPAXmlMetaDataHelper()
    {
    }

    /**
     * Method to convert an internal class/interface metadata into the associated JPA XML metadata.
     * @param cmd Metadata for the class/interface
     * @param prefix Prefix for the XML (e.g "    ")
     * @param indent Indent for each block of XML (e.g "    ")
     * @return The XML
     */
    public String getXMLForMetaData(AbstractClassMetaData cmd, String prefix, String indent)
    {
        StringBuilder str = new StringBuilder();

        if (cmd.isMappedSuperclass())
        {
            str.append(prefix).append("<mapped-superclass class=\"" + cmd.getFullClassName() + "\"");
        }
        else if (cmd.isEmbeddedOnly())
        {
            str.append(prefix).append("<embeddable class=\"" + cmd.getFullClassName() + "\"");
        }
        else
        {
            str.append(prefix).append("<entity name=\"" + cmd.getName() + "\"");
        }
        if (cmd.isMetaDataComplete())
        {
            str.append(" metadata-complete=\"true\"");
        }
        str.append(">\n");

        if (cmd.getTable() != null || cmd.getCatalog() != null || cmd.getSchema() != null)
        {
            str.append(prefix).append(indent).append("<table");
            if (cmd.getTable() != null)
            {
                str.append(" name=\"").append(cmd.getTable()).append("\"");
            }
            if (cmd.getCatalog() != null)
            {
                str.append(" catalog=\"").append(cmd.getCatalog()).append("\"");
            }
            if (cmd.getSchema() != null)
            {
                str.append(" schema=\"").append(cmd.getSchema()).append("\"");
            }
            // TODO Support <unique-constraint>, <index>
            str.append("/>\n");
        }
        if (!StringUtils.isWhitespace(cmd.getObjectidClass()) && !cmd.getObjectidClass().startsWith("org.datanucleus.identity"))
        {
            str.append(prefix).append(indent).append("<id-class class=\"").append(cmd.getObjectidClass()).append("</id-class>\n");
        }
        if (cmd.getIdentityType() != null && cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            str.append(prefix).append(indent).append("<datastore-id");
            if (cmd.getIdentityMetaData().getColumnName() != null)
            {
                str.append(" column=\"").append(cmd.getIdentityMetaData().getColumnName()).append("\"");
            }
            // TODO Add generated-value
            str.append("/>");
        }

        // Inheritance
        if (cmd.getInheritanceMetaData() != null)
        {
            // TODO Convert internal strategy to JPA strategy
            str.append(prefix).append(indent).append("<inheritance strategy=\"").append(cmd.getInheritanceMetaData().getStrategy()).append("\"/>\n");
        }
        // TODO Add discriminator-value, discriminator-column

        // Add members
        str.append(prefix).append(indent).append("<attributes>\n");
        int numMembers = cmd.getNoOfMembers();
        for (int i=0;i<numMembers;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForMemberAtRelativePosition(i);
            str.append(getXMLForMetaData(mmd, prefix + indent,indent));
        }
        str.append(prefix).append(indent).append("</attributes>\n");

        // Add extensions
        processExtensions(cmd.getExtensions(), str, prefix, indent);

        if (cmd.isMappedSuperclass())
        {
            str.append(prefix).append("</mapped-superclass>");
        }
        else if (cmd.isEmbeddedOnly())
        {
            str.append(prefix).append("</embeddable>");
        }
        else
        {
            str.append(prefix).append("</entity>");
        }

        return str.toString();
    }

    public String getXMLForMetaData(AbstractMemberMetaData mmd, String prefix, String indent)
    {
        if (mmd.isStatic() || mmd.isFinal())
        {
            // If this field is static or final, don't bother with MetaData since we will ignore it anyway.
            return "";
        }

        // Field needs outputting so generate metadata
        StringBuilder str = new StringBuilder();
        ClassLoaderResolver clr = mmd.getMetaDataManager().getNucleusContext().getClassLoaderResolver(null);
        RelationType relType = mmd.getRelationType(clr);
        if (relType == RelationType.ONE_TO_ONE_UNI || relType == RelationType.ONE_TO_ONE_BI)
        {
            str.append(prefix).append("<one-to-one name=\"").append(mmd.getName()).append("\" fetch=").append(mmd.isDefaultFetchGroup() ? "\"EAGER\"" : "\"LAZY\"");
            // TODO Support optional, mapped-by, orphan-removal, id, maps-id, access etc
        }
        else if (relType == RelationType.ONE_TO_MANY_UNI || relType == RelationType.ONE_TO_MANY_BI)
        {
            str.append(prefix).append("<one-to-many");
        }
        else if (relType == RelationType.MANY_TO_ONE_UNI || relType == RelationType.MANY_TO_ONE_BI)
        {
            str.append(prefix).append("<many-to-one");
        }
        else if (relType == RelationType.MANY_TO_MANY_BI)
        {
            str.append(prefix).append("<many-to-many");
        }
        else
        {
            // TODO Cater for <id>, <embedded-id>, <basic>, <lob>, <temporal> etc
        }

        return str.toString();
    }

    protected void processExtensions(Map<String, String> extensions, StringBuilder str, String prefix, String indent)
    {
        if (extensions != null)
        {
            Iterator<Entry<String, String>> entryIter = extensions.entrySet().iterator();
            while (entryIter.hasNext())
            {
                Entry<String, String> entry = entryIter.next();
                str.append(getXMLForMetaData(entry.getKey(), entry.getValue(), prefix+indent, indent)).append("\n");
            }
        }
    }

    public String getXMLForMetaData(String key, String value, String prefix, String indent)
    {
        // DN extension for orm.xml
        StringBuilder str = new StringBuilder();
        str.append(prefix).append("<extension ")
            .append("key=\"").append(key).append("\" ")
            .append("value=\"").append(value).append("\"/>");
        return str.toString();
    }
}
