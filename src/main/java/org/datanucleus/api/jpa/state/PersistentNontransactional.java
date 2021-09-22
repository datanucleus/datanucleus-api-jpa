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
2008 Andy Jefferson - fixed transitionReadField, transitionWriteField exceptions
    ...
**********************************************************************/
package org.datanucleus.api.jpa.state;

import org.datanucleus.FetchPlan;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.state.IllegalStateTransitionException;
import org.datanucleus.state.LifeCycleState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.transaction.Transaction;
import org.datanucleus.util.Localiser;

/**
 * Class representing the life cycle state of PersistentNontransactional.
 */
class PersistentNontransactional extends LifeCycleState
{
    /** Protected Constructor to prevent external instantiation. */
    protected PersistentNontransactional()
    {
		isPersistent = true;    	
        isDirty = false;
        isNew = false;
        isDeleted = false;
        isTransactional = false;

        stateType = P_NONTRANS;
    }

    @Override
    public LifeCycleState transitionDeletePersistent(ObjectProvider sm)
    {
        return changeState(sm, P_DELETED);
    }

    @Override
    public LifeCycleState transitionMakeTransactional(ObjectProvider sm, boolean refreshFields)
    {
        if (refreshFields)
        {
            sm.refreshLoadedFields();
        }
        return changeState(sm, P_CLEAN);
    }

    @Override
    public LifeCycleState transitionMakeTransient(ObjectProvider sm, boolean useFetchPlan, boolean detachAllOnCommit)
    {
        if (useFetchPlan)
        {
            sm.loadUnloadedFieldsInFetchPlan();
        }
        return changeState(sm, TRANSIENT);
    }

    @Override
    public LifeCycleState transitionCommit(ObjectProvider sm, Transaction tx)
    {
        throw new IllegalStateTransitionException(this, "commit", sm);
    }

    @Override
    public LifeCycleState transitionRollback(ObjectProvider sm, Transaction tx)
    {
        throw new IllegalStateTransitionException(this, "rollback", sm);
    }

    @Override
    public LifeCycleState transitionRefresh(ObjectProvider sm)
    {
        // Refresh the FetchPlan fields and unload all others
        sm.refreshFieldsInFetchPlan();
        sm.unloadNonFetchPlanFields();

        return this;
    }

    @Override
    public LifeCycleState transitionEvict(ObjectProvider sm)
    {
        sm.clearNonPrimaryKeyFields();
        sm.clearSavedFields();
        return changeState(sm, HOLLOW);
    }

    @Override
    public LifeCycleState transitionReadField(ObjectProvider sm, boolean isLoaded)
    {
        Transaction tx = sm.getExecutionContext().getTransaction();
		if (!tx.isActive() && !tx.getNontransactionalRead())
		{
	        throw new NucleusUserException(Localiser.msg("027002"),sm.getInternalObjectId());
		}

        if (tx.isActive() && ! tx.getOptimistic())
        {
            // Save the fields for rollback.
            sm.saveFields();
            sm.refreshLoadedFields();
            return changeState(sm, P_CLEAN);
        }

        return this;
    }

    @Override
    public LifeCycleState transitionWriteField(ObjectProvider sm)
    {
        Transaction tx = sm.getExecutionContext().getTransaction();
        if (!tx.isActive() && !tx.getNontransactionalWrite())
        {
            throw new NucleusUserException(Localiser.msg("027001"), sm.getInternalObjectId());
        }
        if (tx.isActive())
        {
            // Save the fields for rollback.
            sm.saveFields();

            return changeState(sm, P_DIRTY);
        }

        return this;
    }

    @Override
    public LifeCycleState transitionRetrieve(ObjectProvider sm, boolean fgOnly)
    {
        Transaction tx = sm.getExecutionContext().getTransaction();
        if (tx.isActive() && !tx.getOptimistic())
        {
            // Save the fields for rollback.
            sm.saveFields();
    		if (fgOnly)
            {
                sm.loadUnloadedFieldsInFetchPlan();
            }
    		else
            {
    			sm.loadUnloadedFields();
            }             
            return changeState(sm, P_CLEAN);
        }
        else if (tx.isActive() && tx.getOptimistic())
        {
            // Save the fields for rollback.
            sm.saveFields();
    		if (fgOnly)
            {
                sm.loadUnloadedFieldsInFetchPlan();
            }
    		else
            {
    			sm.loadUnloadedFields();
            }
    		return this;
        }
        else
        {
    		if (fgOnly)
            {
                sm.loadUnloadedFieldsInFetchPlan();
            }
    		else
            {
    			sm.loadUnloadedFields();
            }
    		return this;
        }
    }

    @Override
    public LifeCycleState transitionRetrieve(ObjectProvider sm, FetchPlan fetchPlan)
    {
        Transaction tx = sm.getExecutionContext().getTransaction();
        if (tx.isActive() && !tx.getOptimistic())
        {
            // Save the fields for rollback.
            sm.saveFields();
            sm.loadUnloadedFieldsOfClassInFetchPlan(fetchPlan);
            return changeState(sm, P_CLEAN);
        }
        else if (tx.isActive() && tx.getOptimistic())
        {
            // Save the fields for rollback.
            sm.saveFields();
            sm.loadUnloadedFieldsOfClassInFetchPlan(fetchPlan);
            return this;
        }
        else
        {
            sm.loadUnloadedFieldsOfClassInFetchPlan(fetchPlan);
            return this;
        }
    }

    @Override
    public LifeCycleState transitionSerialize(ObjectProvider sm)
    {
        Transaction tx = sm.getExecutionContext().getTransaction();
        if (tx.isActive() && !tx.getOptimistic())
        {
            return changeState(sm, P_CLEAN);
        }
        return this;
    }

    @Override
    public LifeCycleState transitionDetach(ObjectProvider sm)
    {
        return changeState(sm, DETACHED_CLEAN);
    }

    /**
     * Method to return a string version of this object.
     * @return The string "P_NONTRANS".
     **/
    public String toString()
    {
        return "P_NONTRANS";
    }
}