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

import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.datanucleus.ExecutionContext;
import org.datanucleus.TransactionEventListener;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.transaction.NucleusTransactionException;
import org.datanucleus.transaction.TransactionUtils;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;

/**
 * EntityTransaction implementation for JPA for ResourceLocal transaction.
 * Utilises the underlying ExecutionContext and its real transaction, providing a JPA layer on top.
 */
public class JPAEntityTransaction implements EntityTransaction
{
    /** The underlying transaction */
    org.datanucleus.Transaction tx;

    /**
     * Constructor.
     * @param ec The ExecutionContext providing the transaction.
     */
    public JPAEntityTransaction(ExecutionContext ec)
    {
        this.tx = ec.getTransaction();
    }

    /**
     * Indicate whether a transaction is in progress.
     * @throws PersistenceException if an unexpected error condition is encountered.
     */
    public boolean isActive()
    {
        return tx.isActive();
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
            tx.begin();
        }
        catch (NucleusException ne)
        {
            throw DataNucleusHelperJPA.getJPAExceptionForNucleusException(ne);
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

        if (tx.getRollbackOnly())
        {
            // This is thrown by the underlying transaction but we want to have a RollbackException here so intercept it
            if (NucleusLogger.TRANSACTION.isDebugEnabled())
            {
                NucleusLogger.TRANSACTION.debug(Localiser.msg("015020"));
            }
            throw new RollbackException(Localiser.msg("015020"));
        }

        try
        {
            tx.commit();
        }
        catch (NucleusTransactionException nte)
        {
            Throwable cause = nte.getCause();
            Throwable pe = null;
            if (cause instanceof NucleusException)
            {
                pe = DataNucleusHelperJPA.getJPAExceptionForNucleusException((NucleusException)cause);
            }
            else
            {
                pe = cause;
            }
            throw new RollbackException(Localiser.msg("015007"), pe);
        }
        catch (NucleusException ne)
        {
            throw DataNucleusHelperJPA.getJPAExceptionForNucleusException(ne);
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
            tx.rollback();
        }
        catch (NucleusException ne)
        {
            throw DataNucleusHelperJPA.getJPAExceptionForNucleusException(ne);
        }
    }

    /**
     * Determine whether the current transaction has been marked for rollback.
     * @throws IllegalStateException if isActive() is false.
     */
    public boolean getRollbackOnly()
    {
        assertActive();
        return tx.getRollbackOnly();
    }

    /**
     * Mark the current transaction so that the only possible outcome of the transaction is for the transaction to be rolled back.
     * @throws IllegalStateException Thrown if the transaction is not active
     */
    public void setRollbackOnly()
    {
        assertActive();
        tx.setRollbackOnly();
    }

    /**
     * Convenience accessor for setting a transaction option.
     * @param option option name
     * @param value The value
     */
    public void setOption(String option, int value)
    {
        tx.setOption(option, value);
    }

    /**
     * Convenience accessor for setting a transaction option.
     * @param option option name
     * @param value The value
     */
    public void setOption(String option, boolean value)
    {
        tx.setOption(option, value);
    }

    /**
     * Convenience accessor for setting a transaction option.
     * @param option option name
     * @param value The value
     */
    public void setOption(String option, String value)
    {
        if (option.equalsIgnoreCase("transaction.isolation"))
        {
            int isolationLevel = TransactionUtils.getTransactionIsolationLevelForName(value);
            tx.setOption(org.datanucleus.Transaction.TRANSACTION_ISOLATION_OPTION, isolationLevel);
            return;
        }
        tx.setOption(option, value);
    }

    /**
     * Method to mark the current point as a savepoint with the provided name.
     * @param name Name of the savepoint.
     * @throws UnsupportedOperationException if the underlying datastore doesn't support savepoints
     * @throws IllegalStateException if no name is provided
     */
    public void setSavepoint(String name)
    {
        if (name == null)
        {
            throw new IllegalStateException("No savepoint name provided so cannot set savepoint");
        }
        if (tx.isActive())
        {
            tx.setSavepoint(name);
        }
        else
        {
            throw new IllegalStateException("No active transaction so cannot set savepoint");
        }
    }

    /**
     * Method to mark the current point as a savepoint with the provided name.
     * @param name Name of the savepoint.
     * @throws UnsupportedOperationException if the underlying datastore doesn't support savepoints
     * @throws IllegalStateException if no name is provided, or the name doesn't correspond to a known savepoint
     */
    public void releaseSavepoint(String name)
    {
        if (name == null)
        {
            throw new IllegalStateException("No savepoint name provided so cannot release savepoint");
        }
        if (tx.isActive())
        {
            tx.releaseSavepoint(name);
        }
        else
        {
            throw new IllegalStateException("No active transaction so cannot release a savepoint");
        }
    }

    /**
     * Method to mark the current point as a savepoint with the provided name.
     * @param name Name of the savepoint.
     * @throws UnsupportedOperationException if the underlying datastore doesn't support savepoints
     * @throws IllegalStateException if no name is provided, or the name doesn't correspond to a known savepoint
     */
    public void rollbackToSavepoint(String name)
    {
        if (name == null)
        {
            throw new IllegalStateException("No savepoint name provided so cannot rollback to savepoint");
        }
        if (tx.isActive())
        {
            tx.rollbackToSavepoint(name);
        }
        else
        {
            throw new IllegalStateException("No active transaction so cannot rollback to savepoint");
        }
    }

    /**
     * Convenience method to throw an exception if the transaction is not active.
     * @throws IllegalStateException Thrown if the transaction is not active.
     */
    protected void assertActive()
    {
        if (!tx.isActive())
        {
            throw new IllegalStateException(Localiser.msg("015040"));
        }
    }

    /**
     * Convenience method to throw an exception if the transaction is active.
     * @throws IllegalStateException Thrown if the transaction is active.
     */
    protected void assertNotActive()
    {
        if (tx.isActive())
        {
            throw new IllegalStateException(Localiser.msg("015032"));
        }
    }

    /**
     * Method to register a listener for transaction events.
     * @param listener The listener
     */
    public void registerEventListener(TransactionEventListener listener)
    {
        tx.bindTransactionEventListener(listener);
    }

    /**
     * Method to deregister a listener for transaction events.
     * @param listener The listener to remove
     */
    public void deregisterEventListener(TransactionEventListener listener)
    {
        tx.removeTransactionEventListener(listener);
    }
}