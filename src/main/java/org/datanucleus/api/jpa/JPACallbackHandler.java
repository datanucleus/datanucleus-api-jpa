/**********************************************************************
Copyright (c) 2006 Erik Bengtson and others. All rights reserved.
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
2007 Andy Jefferson - added prePersist
2008 Andy Jefferson - fixed postRefresh
    ...
**********************************************************************/
package org.datanucleus.api.jpa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.NucleusContext;
import org.datanucleus.api.jpa.metadata.JPAMetaDataManager;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.EventListenerMetaData;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.state.ObjectProvider;

/**
 * CallbackHandler implementation for JPA.
 */
public class JPACallbackHandler implements CallbackHandler
{
    NucleusContext nucleusCtx;

    CallbackHandler beanValidationHandler;

    public JPACallbackHandler(NucleusContext nucleusCtx)
    {
        this.nucleusCtx = nucleusCtx;
    }

    public void setValidationListener(CallbackHandler handler)
    {
        beanValidationHandler = handler;
    }

    /**
     * Callback after the object has been created.
     * @param pc The Object
     */
    public void postCreate(Object pc)
    {
        // Not supported by JPA
    }

    /**
     * Callback before the object is persisted (just before the lifecycle state change).
     * @param pc The Object
     */
    public void prePersist(Object pc)
    {
        if (nucleusCtx.getApiAdapter().isNew(pc))
        {
            invokeCallback(pc, PrePersist.class);
        }
        if (beanValidationHandler != null)
        {
            beanValidationHandler.prePersist(pc);
        }
    }

    /**
     * Callback before the object is stored.
     * @param pc The Object
     */
    public void preStore(Object pc)
    {
        if (!nucleusCtx.getApiAdapter().isNew(pc))
        {
            invokeCallback(pc, PreUpdate.class);
        }
        if (beanValidationHandler != null)
        {
            ObjectProvider op = nucleusCtx.getApiAdapter().getExecutionContext(pc).findObjectProvider(pc);
            if (!op.getLifecycleState().isNew())
            {
                // Don't fire this when persisting new since we will have done prePersist
                beanValidationHandler.preStore(pc);
            }
        }
    }

    /**
     * Callback after the object is stored.
     * @param pc The Object
     */
    public void postStore(Object pc)
    {
        if (nucleusCtx.getApiAdapter().isNew(pc))
        {
            invokeCallback(pc, PostPersist.class);
        }
        else
        {
            invokeCallback(pc, PostUpdate.class);
        }
    }

    /**
     * Callback before the fields of the object are cleared.
     * @param pc The Object
     */
    public void preClear(Object pc)
    {
        // Not supported by JPA
    }

    /**
     * Callback after the fields of the object are cleared.
     * @param pc The Object
     */
    public void postClear(Object pc)
    {
        // Not supported by JPA
    }

    /**
     * Callback before the object is deleted (after calling remove() but before the lifecycle state change).
     * @param pc The Object
     */
    public void preDelete(Object pc)
    {
        invokeCallback(pc, PreRemove.class);
        if (beanValidationHandler != null)
        {
            beanValidationHandler.preDelete(pc);
        }
    }

    /**
     * Callback after the object is deleted.
     * @param pc The Object
     */
    public void postDelete(Object pc)
    {
        invokeCallback(pc, PostRemove.class);
    }

    /**
     * Callback before the object is made dirty.
     * @param pc The Object
     */
    public void preDirty(Object pc)
    {
        // Not supported by JPA
    }

    /**
     * Callback after the object is made dirty.
     * @param pc The Object
     */
    public void postDirty(Object pc)
    {
        // Not supported by JPA
    }

    /**
     * Callback after the fields of the object are loaded.
     * @param pc The Object
     */
    public void postLoad(Object pc)
    {
        invokeCallback(pc, PostLoad.class);
    }

    /**
     * Callback after the fields of the object are refreshed.
     * @param pc The Object
     */
    public void postRefresh(Object pc)
    {
        // Not supported by JPA
    }

    /**
     * Callback before the object is detached.
     * @param pc The Object
     */
    public void preDetach(Object pc)
    {
        // Not supported by JPA
    }

    /**
     * Callback after the object is detached.
     * @param pc The Object
     * @param detachedPC The detached object
     */
    public void postDetach(Object pc, Object detachedPC)
    {
        // Not supported by JPA
    }

    /**
     * Callback before the object is attached.
     * @param pc The Object
     */
    public void preAttach(Object pc)
    {
        // Not supported by JPA
    }

    /**
     * Callback after the object is attached.
     * @param pc The attached Object
     * @param detachedPC The detached object
     */
    public void postAttach(Object pc, Object detachedPC)
    {
        // Not supported by JPA
    }

    /**
     * Adds a new listener to this handler. 
     * @param listener the listener instance.
     * @param classes this parameter is ignored in this implementation  
     */
    public void addListener(Object listener, Class[] classes)
    {
    }

    /**
     * Remove a listener for this handler.
     * @param listener the listener instance
     */
    public void removeListener(Object listener)
    {
    }

    /**
     * Clear any objects to release resources.
     */
    public void close()
    {
    }

    /**
     * Method to invoke all listeners for a particular callback.
     * @param pc The PC object causing the event
     * @param callbackClass The callback type to call
     */
    private void invokeCallback(final Object pc, final Class callbackClass)
    {
        final ExecutionContext ec = nucleusCtx.getApiAdapter().getExecutionContext(pc);
        final ClassLoaderResolver clr = ec.getClassLoaderResolver();

        AbstractClassMetaData acmd = ec.getMetaDataManager().getMetaDataForClass(pc.getClass(), clr);
        try
        {
            if (!acmd.isExcludeDefaultListeners())
            {
                // Global listeners for all classes
                List<EventListenerMetaData> listenerMetaData = ((JPAMetaDataManager)ec.getMetaDataManager()).getEventListeners();
                if (listenerMetaData != null && !listenerMetaData.isEmpty())
                {
                    // Files have listeners so go through them in the same order
                    Iterator<EventListenerMetaData> listenerIter = listenerMetaData.iterator();
                    while (listenerIter.hasNext())
                    {
                        EventListenerMetaData elmd = listenerIter.next();
                        Class listenerClass = clr.classForName(elmd.getClassName());
                        String methodName = elmd.getMethodNameForCallbackClass(callbackClass.getName());
                        if (methodName != null)
                        {
                            // Separate listener class taking the PC object as input
                            Object listener = listenerClass.newInstance();
                            invokeCallbackMethod(listener, methodName, pc, clr);
                        }
                    }
                }
            }

            // Class listeners for this class
            List<String> entityMethodsToInvoke = null;
            while (acmd != null)
            {
                List<EventListenerMetaData> listenerMetaData = acmd.getListeners();
                if (listenerMetaData != null && !listenerMetaData.isEmpty())
                {
                    // Class has listeners so go through them in the same order
                    Iterator<EventListenerMetaData> listenerIter = listenerMetaData.iterator();
                    while (listenerIter.hasNext())
                    {
                        EventListenerMetaData elmd = listenerIter.next();
                        Class listenerClass = clr.classForName(elmd.getClassName());
                        String methodName = elmd.getMethodNameForCallbackClass(callbackClass.getName());
                        if (methodName != null)
                        {
                            if (elmd.getClassName().equals(acmd.getFullClassName()))
                            {
                                // Class itself is the listener
                                if (entityMethodsToInvoke == null)
                                {
                                    entityMethodsToInvoke = new ArrayList<String>();
                                }
                                if (!entityMethodsToInvoke.contains(methodName))
                                {
                                    // Only add the method if is not already present (allows for inherited listener methods)
                                    entityMethodsToInvoke.add(methodName);
                                }
                            }
                            else
                            {
                                // Separate listener class taking the PC object as input
                                Object listener = listenerClass.newInstance();
                                invokeCallbackMethod(listener, methodName, pc, clr);
                            }
                        }
                    }
                    if (acmd.isExcludeSuperClassListeners())
                    {
                        break;
                    }
                }

                // Move up to superclass
                acmd = acmd.getSuperAbstractClassMetaData();
            }

            if (entityMethodsToInvoke != null && !entityMethodsToInvoke.isEmpty())
            {
                // Invoke all listener methods on the entity
                for (int i=0;i<entityMethodsToInvoke.size();i++)
                {
                    String methodName = entityMethodsToInvoke.get(i);
                    invokeCallbackMethod(pc, methodName, clr);
                }
            }
        }
        catch (SecurityException e)
        {
            JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
        }
        catch (IllegalArgumentException e)
        {
            JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
        }
        catch (IllegalAccessException e)
        {
            JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
        }
        catch (InstantiationException e)
        {
            JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
        }
    }

    /**
     * Method to invoke a method of a listener where the Entity is the listener.
     * Means that the method invoked takes no arguments as input.
     * @param listener Listener object
     * @param methodName The method name
     */
    private void invokeCallbackMethod(final Object listener, final String methodName, ClassLoaderResolver clr)
    {
        final String callbackClassName = methodName.substring(0, methodName.lastIndexOf('.'));
        final String callbackMethodName = methodName.substring(methodName.lastIndexOf('.')+1);
        final Class callbackClass;
        if (callbackClassName.equals(listener.getClass().getName()))
        {
            callbackClass = listener.getClass();
        }
        else
        {
            callbackClass = clr.classForName(callbackClassName);
        }

        // Need to have priveleges to perform invoke on private methods
        AccessController.doPrivileged(
            new PrivilegedAction<Object>()
            {
                public Object run()
                {
                    try
                    {
                        Method m = callbackClass.getDeclaredMethod(callbackMethodName, (Class[])null);
                        if (!m.isAccessible())
                        {
                            m.setAccessible(true);
                        }
                        m.invoke(listener, (Object[])null);
                    }
                    catch (NoSuchMethodException e)
                    {
                        JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
                    }
                    catch (IllegalArgumentException e)
                    {
                        JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
                    }
                    catch (IllegalAccessException e)
                    {
                        JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
                    }
                    catch (InvocationTargetException e)
                    {
                        if (e.getTargetException() instanceof RuntimeException)
                        {
                            throw (RuntimeException) e.getTargetException();
                        }
                        throw new RuntimeException(e.getTargetException());
                    }
                    return null;
                }
            });
    }

    /**
     * Method to invoke a method of a listener where listener is a separate EntityListener (not an Entity).
     * Means that the method invoked takes the object causing the event as input.
     * @param listener Listener object
     * @param methodName The method name
     * @param obj The object causing the callback
     */
    private void invokeCallbackMethod(final Object listener, final String methodName, final Object obj, ClassLoaderResolver clr)
    {
        final String callbackClassName = methodName.substring(0, methodName.lastIndexOf('.'));
        final String callbackMethodName = methodName.substring(methodName.lastIndexOf('.')+1);
        final Class callbackClass;
        if (callbackClassName.equals(listener.getClass().getName()))
        {
            callbackClass = listener.getClass();
        }
        else
        {
            callbackClass = clr.classForName(callbackClassName);
        }

        // Need to have priveleges to perform invoke on private methods
        AccessController.doPrivileged(
            new PrivilegedAction<Object>()
            {
                public Object run()
                {
                    try
                    {
                        try
                        {
                            Class[] argTypes = new Class[]{Object.class};
                            Object[] args = new Object[]{obj};
                            Method m = callbackClass.getDeclaredMethod(callbackMethodName, argTypes);
                            if (!m.isAccessible())
                            {
                                m.setAccessible(true);
                            }
                            m.invoke(listener, args);
                        }
                        catch (NoSuchMethodException ex)
                        {
                            //either this method does not exist with the arguments, or is an interface type
                            //if interface type, the following block will solve it
                            Object[] args = new Object[]{obj};
                            Method[] methods = callbackClass.getDeclaredMethods();
                            for (int i=0; i<methods.length; i++)
                            {
                                Method m = methods[i];
                                if (m.getName().equals(callbackMethodName) && m.getParameterTypes().length == 1 && 
                                    m.getParameterTypes()[0].isAssignableFrom(obj.getClass()))
                                {
                                    if (!m.isAccessible())
                                    {
                                        m.setAccessible(true);
                                    }
                                    m.invoke(listener, args);
                                    break;
                                }
                            }
                        }
                    }
                    catch (IllegalArgumentException e)
                    {
                        JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
                    }
                    catch (IllegalAccessException e)
                    {
                        JPAEntityManagerFactory.LOGGER.debug("Exception in JPACallbackHandler", e);
                    }
                    catch (InvocationTargetException e)
                    {
                        if (e.getTargetException() instanceof RuntimeException)
                        {
                            throw (RuntimeException) e.getTargetException();
                        }
                        throw new RuntimeException(e.getTargetException());
                    }
                    return null;
                }
            });
    }
}