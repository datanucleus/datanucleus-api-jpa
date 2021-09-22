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
import org.datanucleus.state.LifeCycleState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.transaction.Transaction;
import org.datanucleus.util.Localiser;

/**
 * Class representing the life cycle state of PersistentDeleted.
 */
class PersistentDeleted extends LifeCycleState
{
    /** Protected Constructor to prevent external instantiation. */
    protected PersistentDeleted()
    {
		isPersistent = true;    	
        isDirty = true;
        isNew = false;
        isDeleted = true;
        isTransactional = true;

        stateType =  P_DELETED;
    }

    @Override
    public LifeCycleState transitionMakeNontransactional(ObjectProvider op)
    {
        throw new NucleusUserException(Localiser.msg("027007"),op.getInternalObjectId());
    }

    @Override
    public LifeCycleState transitionMakeTransient(ObjectProvider op, boolean useFetchPlan, boolean detachAllOnCommit)
    {
        throw new NucleusUserException(Localiser.msg("027008"),op.getInternalObjectId());
    }

    @Override
    public LifeCycleState transitionMakePersistent(ObjectProvider op)
    {
        return changeState(op, P_CLEAN);
    }

    @Override
    public LifeCycleState transitionCommit(ObjectProvider op, Transaction tx)
    {
        if (!tx.getRetainValues())
        {
            op.clearFields();
        }
        return changeState(op, TRANSIENT);
    }

    @Override
    public LifeCycleState transitionRollback(ObjectProvider op, Transaction tx)
    {
        if (tx.getRetainValues())
        {
            if (tx.getRestoreValues())
            {
                op.restoreFields();
            }

            return changeState(op, P_NONTRANS);
        }

        op.clearNonPrimaryKeyFields();
        op.clearSavedFields();
        return changeState(op, HOLLOW);
    }

    @Override
    public LifeCycleState transitionWriteField(ObjectProvider op)
    {
        throw new NucleusUserException(Localiser.msg("027010"),op.getInternalObjectId());
    }

    /**
     * Method to return a string version of this object.
     * @return The string "P_DELETED".
     **/
    public String toString()
    {
        return "P_DELETED";
    }
}