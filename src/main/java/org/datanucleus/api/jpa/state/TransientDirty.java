/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.api.jpa.state;

import org.datanucleus.state.LifeCycleState;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.transaction.Transaction;

/**
 * Class representing the life cycle state of TransientDirty.
 */
class TransientDirty extends LifeCycleState
{
    /**
     * Constructor
     **/
    TransientDirty()
    {
        // these flags are set only in the constructor 
        // and shouldn't be changed afterwards
        // (cannot make them final since they are declared in superclass 
        // but their values are specific to subclasses)
        isPersistent = false;
        isTransactional = true;
        isDirty = true;
        isNew = false;
        isDeleted = false;

        stateType =  T_DIRTY;
    }

    @Override
    public LifeCycleState transitionMakeTransient(DNStateManager sm, boolean useFetchPlan, boolean detachAllOnCommit)
    {
        return this;
    }

    @Override
    public LifeCycleState transitionMakePersistent(DNStateManager sm)
    {
        sm.registerTransactional();
        return changeState(sm,P_NEW);
    }

    @Override
    public LifeCycleState transitionCommit(DNStateManager sm, Transaction tx)
    {
    	sm.clearSavedFields();
        return changeTransientState(sm, T_CLEAN);
    }

    @Override
    public LifeCycleState transitionRollback(DNStateManager sm, Transaction tx)
    {
        if (tx.getRestoreValues() || sm.isRestoreValues())
        {
            sm.restoreFields();
        } // else do nothing.
        return changeTransientState(sm, T_CLEAN); 
    }

    /**
     * Method to return a string version of this object.
     * @return The string "T_DIRTY".
     **/
    public String toString()
    {
        return "T_DIRTY";
    }
}