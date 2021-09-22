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
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.transaction.Transaction;

/**
 * Class representing the life cycle state of TransientClean.
 */
class TransientClean extends LifeCycleState
{
    /**
     * Constructor.
     **/
    TransientClean()
    {
        // these flags are set only in the constructor 
        // and shouldn't be changed afterwards
        // (cannot make them final since they are declared in superclass 
        // but their values are specific to subclasses)
        isPersistent = false;
        isTransactional = true;
        isDirty = false;
        isNew = false;
        isDeleted = false;

        stateType = T_CLEAN;
    }

    @Override
    public LifeCycleState transitionMakeTransient(ObjectProvider op, boolean useFetchPlan, boolean detachAllOnCommit)
    {
        return this;
    }

    /**
     * @param sm StateManager 
     * @see LifeCycleState#transitionMakeNontransactional(ObjectProvider op)
     */
    public LifeCycleState transitionMakeNontransactional(ObjectProvider op)
    {
        try
        {
            return changeTransientState(op,TRANSIENT);
        }
        finally
        {
            op.disconnect();
        }
    }

    @Override
    public LifeCycleState transitionMakePersistent(ObjectProvider op)
    {    
        op.registerTransactional();
        return changeState(op,P_NEW);
    }

    @Override
    public LifeCycleState transitionReadField(ObjectProvider op, boolean isLoaded)
    {
        return this;
    }

    @Override
    public LifeCycleState transitionWriteField(ObjectProvider op)
    {
        Transaction tx = op.getExecutionContext().getTransaction();
        if (tx.isActive())
        {
            op.saveFields();
            return changeTransientState(op,T_DIRTY);
        }

        return this;
    }

    @Override
    public LifeCycleState transitionCommit(ObjectProvider op, org.datanucleus.transaction.Transaction tx)
    {
        return this;
    }

    @Override
    public LifeCycleState transitionRollback(ObjectProvider op, org.datanucleus.transaction.Transaction tx)
    {
        return this;
    }

    /**
     * Method to return a string version of this object.
     * @return The string "T_CLEAN".
     **/
    public String toString()
    {
        return "T_CLEAN";
    }
}