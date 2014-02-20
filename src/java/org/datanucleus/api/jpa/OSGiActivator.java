/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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

import java.util.Hashtable;

import javax.persistence.spi.PersistenceProvider;
import javax.transaction.TransactionManager;

import org.datanucleus.exceptions.NucleusException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Activator used to register/deregister JPA services in an OSGi environment.
 */
public class OSGiActivator implements BundleActivator 
{
    public static final String PERSISTENCE_PROVIDER_ARIES = "javax.persistence.provider";
    public static final String PERSISTENCE_PROVIDER = PersistenceProvider.class.getName();
    public static final String OSGI_PERSISTENCE_PROVIDER = PersistenceProviderImpl.class.getName();

    /** The JPA registered service (or null if not currently registered). */
    private static ServiceRegistration jpaService = null;

    @SuppressWarnings("unused")
    private static TransactionManager jtaTxnManager;
    private static ServiceReference jtaServiceRef;

    private static ServiceListener jtaListener;

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext ctx) throws Exception 
    {
        PersistenceProvider provider = new PersistenceProviderImpl();
        Hashtable<String, String> props = new Hashtable<String, String>();

        // Register JPA service with alternative properties used by various OSGi frameworks
        props.put(PERSISTENCE_PROVIDER_ARIES, OSGI_PERSISTENCE_PROVIDER);
        props.put(PERSISTENCE_PROVIDER, OSGI_PERSISTENCE_PROVIDER);
        jpaService = ctx.registerService(PERSISTENCE_PROVIDER, provider, props);

        // Add listener to any JTA txn manager
        if (jtaListener != null)
        {
            throw new NucleusException("Another OSGi service listener has already been registered.");
        }
        jtaListener = new Listener(ctx);

        ctx.addServiceListener(jtaListener, "(" + Constants.OBJECTCLASS + "=javax.transaction.TransactionManager)");
        jtaServiceRef = ctx.getServiceReference("javax.transaction.TransactionManager");
        if (jtaServiceRef != null) 
        {
            jtaTxnManager = (TransactionManager)ctx.getService(jtaServiceRef);
        }
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext ctx) throws Exception 
    {
        // Deregister service listener
        try 
        {
            if (jtaServiceRef != null) 
            {
                ctx.ungetService(jtaServiceRef);
                jtaTxnManager = null;
                jtaServiceRef = null;
            }
        }
        finally 
        {
            if (jtaListener != null) 
            {
                ctx.removeServiceListener(jtaListener);
                jtaListener = null;
            }
        }

        if (jpaService != null) 
        {
            jpaService.unregister();
            jpaService = null;
        }
    }

    private static final class Listener implements ServiceListener 
    {
        final BundleContext bundleContext;

        /** Reference to this class to avoid garbage collection. */
        final Class<OSGiActivator> clazzRef;

        public Listener(BundleContext bundleContext) 
        {
            this.bundleContext = bundleContext;
            this.clazzRef = OSGiActivator.class;
        }

        public void serviceChanged(ServiceEvent event) 
        {
            synchronized (this.clazzRef) 
            {
                switch (event.getType()) 
                {
                    case ServiceEvent.REGISTERED:
                        OSGiActivator.jtaServiceRef = event.getServiceReference();
                        OSGiActivator.jtaTxnManager =
                            (TransactionManager) this.bundleContext.getService(OSGiActivator.jtaServiceRef);
                        break;

                    case ServiceEvent.UNREGISTERING:
                        OSGiActivator.jtaTxnManager = null;
                        OSGiActivator.jtaServiceRef = null;
                        this.bundleContext.ungetService(event.getServiceReference());
                        break;
                }
            }
        }
    }
}