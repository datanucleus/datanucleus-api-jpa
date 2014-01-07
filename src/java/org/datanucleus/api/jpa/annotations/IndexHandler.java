/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.api.jpa.annotations;

import java.util.HashMap;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IndexMetaData;
import org.datanucleus.metadata.annotations.AnnotationObject;
import org.datanucleus.metadata.annotations.MemberAnnotationHandler;
import org.datanucleus.util.StringUtils;

/**
 * Handler for the {@link Index} annotation when applied to a field/property of a persistable class.
 */
public class IndexHandler implements MemberAnnotationHandler
{
    public void processMemberAnnotation(AnnotationObject ann, AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        HashMap<String, Object> annotationValues = ann.getNameValueMap();
        String name = (String)annotationValues.get("name");
        Boolean unique = (Boolean)annotationValues.get("name");
        IndexMetaData idxmd = mmd.getIndexMetaData();
        if (idxmd == null)
        {
            idxmd = mmd.newIndexMetaData();
            if (!StringUtils.isWhitespace(name))
            {
                idxmd.setName(name);
            }
            idxmd.setUnique(unique);
        }
    }
}