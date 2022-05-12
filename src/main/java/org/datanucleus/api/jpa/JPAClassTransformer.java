/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved. 
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
package org.datanucleus.api.jpa;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Map;

import javax.persistence.spi.ClassTransformer;

import org.datanucleus.util.ClassUtils;

/**
 * ClassTransformer for runtime enhancement of classes to the JPA interface.
 * A persistence provider supplies an instance of this interface to the PersistenceUnitInfo.addTransformer method. 
 * The supplied transformer instance will get called to transform entity class files when they are loaded or redefined. 
 * The transformation occurs before the class is defined by the JVM.
 */
public class JPAClassTransformer implements ClassTransformer
{
    final ClassFileTransformer transformer;

    public JPAClassTransformer(Map contextProps)
    {
        try
        {
            Class cls = Class.forName("org.datanucleus.enhancer.DataNucleusClassFileTransformer");
            transformer = (ClassFileTransformer)ClassUtils.newInstance(cls, new Class[]{String.class, Map.class}, new Object[]{"-api=JPA", contextProps});
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined, 
            ProtectionDomain protectionDomain, byte[] classfileBuffer)
        throws IllegalClassFormatException
    {
        return transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }
}