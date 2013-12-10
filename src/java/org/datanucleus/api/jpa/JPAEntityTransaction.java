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
2006 Andy Jefferson - replaced PersistenceManager with ExecutionContext
2007 Andy Jefferson - added setOption methods
    ...
**********************************************************************/
package org.datanucleus.api.jpa;

import javax.jdo.JDOException;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.datanucleus.ExecutionContext;
import org.datanucleus.TransactionEventListener;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.transaction.NucleusTransactionException;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;

/**
 * EntityTransaction implementation for JPA for ResourceLocal transaction.
 * Utilises the underlying ExecutionContext and its real transaction, providing a JPA layer on top.
 */
public class JPAEntityTransaction implements EntityTransaction
{
    /** Localisation utility for output messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        NucleusJPAHelper.class.getClassLoader());

    /** ExecutionContext managing the persistence and providing the underlying transaction. */
    ExecutionContext ec;

    /**
     * Constructor.
     * @param ec The ExecutionContext providing the transaction.
     */
    public JPAEntityTransaction(ExecutionContext ec)
    {
        this.ec = ec;
    }

    /**
     * Indicate whether a transaction is in progress.
     * @throws PersistenceException if an unexpected error condition is encountered.
     */
    public boolean isActive()
    {
        return ec.getTransaction().isActive();
    }

    /**
     * Start a resource transaction.
     * @throws IllegalStateException if the transaction is active
     */
    public void begin()
    {
        assertNotActive();
        try
        {
            ec.getTransaction().begin();
        }
        catch (NucleusException ne)
        {
            throw NucleusJPAHelper.getJPAExceptionForNucleusException(ne);
        }
    }

    /**
     * Commit the current transaction, writing any unflushed changes to the database.
     * @throws IllegalStateException if isActive() is false.
     * @throws RollbackException if the commit fails.
     */
    public void commit()
    {
        assertActive();

        if (ec.getTransaction().getRollbackOnly())
        {
            // This is thrown by the underlying transaction but we want to have a RollbackException here so intercept it
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug(LOCALISER.msg("015020"));
            }
            throw new RollbackException(LOCALISER.msg("015020"));
        }

        try
        {
            ec.getTransaction().commit();
        }
        catch (NucleusTransactionException nte)
        {
            Throwable cause = nte.getCause();
            Throwable pe = null;
            if (cause instanceof JDOException)
            {
                // TODO Do we still need this? Where is JDOException thrown? bytecode enhancement?
                pe = NucleusJPAHelper.getJPAExceptionForJDOException((JDOException)cause);
            }
            else if (cause instanceof NucleusException)
            {
                pe = NucleusJPAHelper.getJPAExceptionForNucleusException((NucleusException)cause);
            }
            else
            {
                pe = cause;
            }
            throw new RollbackException(LOCALISER.msg("015007"), pe);
        }
        catch (NucleusException ne)
        {
            throw NucleusJPAHelper.getJPAExceptionForNucleusException(ne);
        }
    }

    /**
     * Roll back the current transaction.
     * @throws IllegalStateException if isActive() is false.
     * @throws PersistenceException if an unexpected error condition is encountered.
     */
    public void rollback()
    {
        assertActive();

        try
        {
            ec.getTransaction().rollback();
        }
        catch (NucleusException ne)
        {
            throw NucleusJPAHelper.getJPAExceptionForNucleusException(ne);
        }
    }

    /**
     * Determine whether the current transaction has been marked for rollback.
     * @throws IllegalStateException if isActive() is false.
     */
    public boolean getRollbackOnly()
    {
        assertActive();
        return ec.getTransaction().getRollbackOnly();
    }

    /**
     * Mark the current transaction so that the only possible outcome of the transaction is for the transaction to be rolled back.
     * @throws IllegalStateException Thrown if the transaction is not active
     */
    public void setRollbackOnly()
    {
        assertActive();
        ec.getTransaction().setRollbackOnly();
    }

    /**
     * Convenience accessor for setting a transaction option.
     * @param option option name
     * @param value The value
     */
    public void setOption(String option, int value)
    {
        ec.getTransaction().setOption(option, value);
    }

    /**
     * Convenience accessor for setting a transaction option.
     * @param option option name
     * @param value The value
     */
    public void setOption(String option, boolean value)
    {
        ec.getTransaction().setOption(option, value);
    }

    /**
     * Convenience accessor for setting a transaction option.
     * @param option option name
     * @param value The value
     */
    public void setOption(String option, String value)
    {
        ec.getTransaction().setOption(option, value);
    }

    /**
     * Convenience method to throw an exception if the transaction is not active.
     * @throws IllegalStateException Thrown if the transaction is not active.
     */
    protected void assertActive()
    {
        if (!ec.getTransaction().isActive())
        {
            throw new IllegalStateException(LOCALISER.msg("015040"));
        }
    }

    /**
     * Convenience method to throw an exception if the transaction is active.
     * @throws IllegalStateException Thrown if the transaction is active.
     */
    protected void assertNotActive()
    {
        if (ec.getTransaction().isActive())
        {
            throw new IllegalStateException(LOCALISER.msg("015032"));
        }
    }

    /**
     * Method to register a listener for transaction events.
     * @param listener The listener
     */
    public void registerEventListener(TransactionEventListener listener)
    {
        ec.getTransaction().bindTransactionEventListener(listener);
    }

    /**
     * Method to deregister a listener for transaction events.
     * @param listener The listener to remove
     */
    public void deregisterEventListener(TransactionEventListener listener)
    {
        ec.getTransaction().removeTransactionEventListener(listener);
    }
}