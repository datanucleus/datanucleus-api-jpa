/**********************************************************************
Copyright (c) 2002 Kelly Grizzle and others. All rights reserved.
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
2002 Mike Martin - unknown changes
2003 Andy Jefferson - commented
    ...
**********************************************************************/
package org.datanucleus.api.jpa.state;

import org.datanucleus.FetchPlan;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.state.LifeCycleState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.transaction.Transaction;

/**
 * Class representing the life cycle state of PersistentClean.
 */
class PersistentClean extends LifeCycleState
{
    /** Protected Constructor to prevent external instantiation. */
    protected PersistentClean()
    {
		isPersistent = true;        	
        isDirty = false;
        isNew = false;
        isDeleted = false;
        isTransactional = true;
        
        stateType = P_CLEAN;
    }

    @Override
    public LifeCycleState transitionDeletePersistent(ObjectProvider sm)
    {
        return changeState(sm, P_DELETED);
    }

    @Override
    public LifeCycleState transitionMakeNontransactional(ObjectProvider sm)
    {
        sm.clearSavedFields();
        return changeState(sm, P_NONTRANS);
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
        sm.clearSavedFields();

        if (tx.getRetainValues())
        {
            return changeState(sm, P_NONTRANS);
        }

        if (sm.getClassMetaData().getIdentityType() != IdentityType.NONDURABLE)
        {
            sm.clearNonPrimaryKeyFields();
        }
        return changeState(sm, HOLLOW);
    }

    @Override
    public LifeCycleState transitionRollback(ObjectProvider sm, Transaction tx)
    {
        if (tx.getRestoreValues())
        {
            sm.restoreFields();
            return changeState(sm, P_NONTRANS);
        }

        if (sm.getClassMetaData().getIdentityType() != IdentityType.NONDURABLE)
        {
            sm.clearNonPrimaryKeyFields();
        }
        sm.clearSavedFields();
        return changeState(sm, HOLLOW);
    }

    @Override
    public LifeCycleState transitionEvict(ObjectProvider sm)
    {
        sm.clearNonPrimaryKeyFields();
        sm.clearSavedFields();
        return changeState(sm, HOLLOW);
    }

    @Override
    public LifeCycleState transitionWriteField(ObjectProvider sm)
    {
        Transaction tx = sm.getExecutionContext().getTransaction();
        if (tx.getRestoreValues())
        {
            sm.saveFields();
        }

        return changeState(sm, P_DIRTY);
    }

    @Override
	public LifeCycleState transitionRefresh(ObjectProvider sm)
	{
		sm.clearSavedFields();

        // Refresh the FetchPlan fields and unload all others
        sm.refreshFieldsInFetchPlan();
        sm.unloadNonFetchPlanFields();

        Transaction tx = sm.getExecutionContext().getTransaction();
		if (tx.isActive())
		{
			return changeState(sm, P_CLEAN);
		}
		return changeState(sm, P_NONTRANS);      
	}

    @Override
    public LifeCycleState transitionRetrieve(ObjectProvider sm, boolean fgOnly)
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

    @Override
    public LifeCycleState transitionRetrieve(ObjectProvider sm, FetchPlan fetchPlan)
    {
        sm.loadUnloadedFieldsOfClassInFetchPlan(fetchPlan);
        return this;
    }

    @Override
    public LifeCycleState transitionDetach(ObjectProvider sm)
    {
        return changeState(sm, DETACHED_CLEAN);
    }

    /**
     * Method to return a string version of this object.
     * @return The string "P_CLEAN".
     **/
    public String toString()
    {
        return "P_CLEAN";
    }
}