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
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.transaction.Transaction;
import org.datanucleus.util.Localiser;

/**
 * Class representing the life cycle state of PersistentDirty.
 */
class PersistentDirty extends LifeCycleState
{
    protected PersistentDirty()
    {
        isPersistent = true;        
        isDirty = true;
        isNew = false;
        isDeleted = false;
        isTransactional = true;

        stateType =  P_DIRTY;
    }

    @Override
    public LifeCycleState transitionDeletePersistent(ObjectProvider op)
    {
        return changeState(op, P_DELETED);
    }

    @Override
    public LifeCycleState transitionMakeNontransactional(ObjectProvider op)
    {
        throw new NucleusUserException(Localiser.msg("027011"), op.getInternalObjectId());
    }

    @Override
    public LifeCycleState transitionMakeTransient(ObjectProvider op, boolean useFetchPlan, boolean detachAllOnCommit)
    {
        if (detachAllOnCommit)
        {
            return changeState(op, TRANSIENT);
        }
        throw new NucleusUserException(Localiser.msg("027012"),op.getInternalObjectId());
    }

    @Override
    public LifeCycleState transitionCommit(ObjectProvider op, Transaction tx)
    {
        op.clearSavedFields();

        if (tx.getRetainValues())
        {
            return changeState(op, P_NONTRANS);
        }

        if (op.getClassMetaData().getIdentityType() != IdentityType.NONDURABLE)
        {
            op.clearNonPrimaryKeyFields();
        }
        return changeState(op, HOLLOW);
    }

    @Override
    public LifeCycleState transitionRollback(ObjectProvider op, Transaction tx)
    {
        if (tx.getRestoreValues())
        {
            op.restoreFields();
            return changeState(op, P_NONTRANS);
        }

        if (op.getClassMetaData().getIdentityType() != IdentityType.NONDURABLE)
        {
            op.clearNonPrimaryKeyFields();
        }
        op.clearSavedFields();
        return changeState(op, HOLLOW);
    }

    @Override
    public LifeCycleState transitionRefresh(ObjectProvider op)
    {
        op.clearSavedFields();

        // Refresh the FetchPlan fields and unload all others
        op.refreshFieldsInFetchPlan();
        op.unloadNonFetchPlanFields();

        Transaction tx = op.getExecutionContext().getTransaction();
        if (tx.isActive() && !tx.getOptimistic())
        {
            return changeState(op,P_CLEAN);
        }
        return changeState(op,P_NONTRANS);      
    }

    @Override
    public LifeCycleState transitionDetach(ObjectProvider op)
    {
        return changeState(op, DETACHED_CLEAN);
    }

    /**
     * Method to return a string version of this object.
     * @return The string "P_DIRTY".
     **/
    public String toString()
    {
        return "P_DIRTY";
    }
}