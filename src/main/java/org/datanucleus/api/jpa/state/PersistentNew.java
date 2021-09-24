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
2003 Andy Jefferson - added localiser
    ...
**********************************************************************/
package org.datanucleus.api.jpa.state;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.state.LifeCycleState;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.transaction.Transaction;
import org.datanucleus.util.Localiser;

/**
 * Class representing the life cycle state of PersistentNew.
 */
class PersistentNew extends LifeCycleState
{
    /** Protected Constructor to prevent external instantiation. */
    protected PersistentNew()
    {
		isPersistent = true;    	
        isDirty = true;
        isNew = true;
        isDeleted = false;
        isTransactional = true;

        stateType = P_NEW;
    }

    @Override
    public LifeCycleState transitionDeletePersistent(DNStateManager sm)
    {
        return changeState(sm, P_NEW_DELETED);
    }

    @Override
    public LifeCycleState transitionMakeNontransactional(DNStateManager sm)
    {
        throw new NucleusUserException(Localiser.msg("027013"), sm.getInternalObjectId());
    }

    @Override
    public LifeCycleState transitionMakeTransient(DNStateManager sm, boolean useFetchPlan, boolean detachAllOnCommit)
    {
        if (detachAllOnCommit)
        {
            return changeState(sm, TRANSIENT);
        }
        throw new NucleusUserException(Localiser.msg("027014"), sm.getInternalObjectId());
    }

    @Override
    public LifeCycleState transitionCommit(DNStateManager sm, Transaction tx)
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
        //op.restoreFields();
        return changeState(sm, HOLLOW);
    }

    @Override
    public LifeCycleState transitionRollback(DNStateManager sm, Transaction tx)
    {
        if (tx.getRestoreValues())
        {
            sm.restoreFields();
        }

        return changeState(sm, TRANSIENT);
    }

    @Override
    public LifeCycleState transitionDetach(DNStateManager sm)
    {
        return changeState(sm, DETACHED_CLEAN);
    }

    @Override
    public LifeCycleState transitionRefresh(DNStateManager sm)
    {
        sm.clearSavedFields();

        // Refresh the FetchPlan fields and unload all others
        sm.refreshFieldsInFetchPlan();
        sm.unloadNonFetchPlanFields();

        // We leave in the same state to be consistent with JDO section 5.9.1
        return this;
    }

    /**
     * Method to return a string version of this object.
     * @return The string "P_NEW".
     **/
    public String toString()
    {
        return "P_NEW";
    }
}