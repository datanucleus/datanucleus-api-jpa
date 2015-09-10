/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.QueryTimeoutException;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TemporalType;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.StoredProcQueryParameterMode;
import org.datanucleus.store.query.AbstractStoredProcedureQuery;
import org.datanucleus.store.query.NoQueryResultsException;

/**
 * Implementation of a StoredProcedureQuery.
 * Wraps an internal query.
 */
public class JPAStoredProcedureQuery extends JPAQuery implements StoredProcedureQuery
{
    public JPAStoredProcedureQuery(EntityManager em, org.datanucleus.store.query.Query query)
    {
        super((JPAEntityManager)em, query, "STOREDPROCEDURE");
    }

    private AbstractStoredProcedureQuery getStoredProcQuery()
    {
        return (AbstractStoredProcedureQuery)query;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.JPAQuery#setParameter(javax.persistence.Parameter, java.lang.Object)
     */
    @Override
    public JPAStoredProcedureQuery setParameter(Parameter param, Object value)
    {
        super.setParameter(param, value);
        return this;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.JPAQuery#setParameter(javax.persistence.Parameter, java.util.Calendar, javax.persistence.TemporalType)
     */
    @Override
    public JPAStoredProcedureQuery setParameter(Parameter param, Calendar cal, TemporalType type)
    {
        super.setParameter(param, cal, type);
        return this;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.JPAQuery#setParameter(javax.persistence.Parameter, java.util.Date, javax.persistence.TemporalType)
     */
    @Override
    public JPAStoredProcedureQuery setParameter(Parameter param, Date date, TemporalType type)
    {
        super.setParameter(param, date, type);
        return this;
    }

    public JPAStoredProcedureQuery setParameter(String name, Object value)
    {
        super.setParameter(name, value);
        return this;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#setParameter(java.lang.String, java.util.Calendar, javax.persistence.TemporalType)
     */
    public JPAStoredProcedureQuery setParameter(String name, Calendar value, TemporalType temporalType)
    {
        super.setParameter(name, value, temporalType);
        return this;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#setParameter(java.lang.String, java.util.Date, javax.persistence.TemporalType)
     */
    public JPAStoredProcedureQuery setParameter(String name, Date date, TemporalType type)
    {
        super.setParameter(name, date, type);
        return this;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#setParameter(int, java.lang.Object)
     */
    public JPAStoredProcedureQuery setParameter(int position, Object value)
    {
        super.setParameter(position, value);
        return this;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#setParameter(int, java.util.Calendar, javax.persistence.TemporalType)
     */
    public JPAStoredProcedureQuery setParameter(int position, Calendar value, TemporalType temporalType)
    {
        super.setParameter(position, value, temporalType);
        return this;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#setParameter(int, java.util.Date, javax.persistence.TemporalType)
     */
    public JPAStoredProcedureQuery setParameter(int position, Date value, TemporalType temporalType)
    {
        super.setParameter(position, value, temporalType);
        return this;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#registerStoredProcedureParameter(int, java.lang.Class, jpa_2_1.ParameterMode)
     */
    public StoredProcedureQuery registerStoredProcedureParameter(int position, Class type, ParameterMode mode)
    {
        StoredProcQueryParameterMode paramMode = null;
        if (mode == ParameterMode.IN)
        {
            paramMode = StoredProcQueryParameterMode.IN;
        }
        else if (mode == ParameterMode.OUT)
        {
            paramMode = StoredProcQueryParameterMode.OUT;
        }
        else if (mode == ParameterMode.INOUT)
        {
            paramMode = StoredProcQueryParameterMode.INOUT;
        }
        else if (mode == ParameterMode.REF_CURSOR)
        {
            paramMode = StoredProcQueryParameterMode.REF_CURSOR;
        }
        getStoredProcQuery().registerParameter(position, type, paramMode);
        return this;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#registerStoredProcedureParameter(java.lang.String, java.lang.Class, jpa_2_1.ParameterMode)
     */
    public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode)
    {
        StoredProcQueryParameterMode paramMode = null;
        if (mode == ParameterMode.IN)
        {
            paramMode = StoredProcQueryParameterMode.IN;
        }
        else if (mode == ParameterMode.OUT)
        {
            paramMode = StoredProcQueryParameterMode.OUT;
        }
        else if (mode == ParameterMode.INOUT)
        {
            paramMode = StoredProcQueryParameterMode.INOUT;
        }
        else if (mode == ParameterMode.REF_CURSOR)
        {
            paramMode = StoredProcQueryParameterMode.REF_CURSOR;
        }
        getStoredProcQuery().registerParameter(parameterName, type, paramMode);
        return this;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#getOutputParameterValue(int)
     */
    public Object getOutputParameterValue(int position)
    {
        return getStoredProcQuery().getOutputParameterValue(position);
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#getOutputParameterValue(java.lang.String)
     */
    public Object getOutputParameterValue(String parameterName)
    {
        return getStoredProcQuery().getOutputParameterValue(parameterName);
    }

    boolean executeProcessed = false;

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#execute()
     */
    public boolean execute()
    {
        Object hasResultSet = query.execute();
        executeProcessed = true;
        return (Boolean)hasResultSet;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#hasMoreResults()
     */
    public boolean hasMoreResults()
    {
        if (executeProcessed)
        {
            return getStoredProcQuery().hasMoreResults();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see jpa_2_1.StoredProcedureQuery#getUpdateCount()
     */
    public int getUpdateCount()
    {
        if (executeProcessed)
        {
            return getStoredProcQuery().getUpdateCount();
        }
        return -1;
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#executeUpdate()
     */
    public int executeUpdate()
    {
        try
        {
            if (flushMode == FlushModeType.AUTO && em.getTransaction().isActive())
            {
                em.flush();
            }

            Boolean hasResultSet = (Boolean)query.execute();
            if (hasResultSet)
            {
                throw new IllegalStateException("Stored procedure returned a result set but method requires an update count");
            }
            return getStoredProcQuery().getUpdateCount();
        }
        catch (NoQueryResultsException nqre)
        {
            return 0;
        }
        catch (org.datanucleus.store.query.QueryTimeoutException qte)
        {
            throw new QueryTimeoutException();
        }
        catch (NucleusException jpe)
        {
            throw NucleusJPAHelper.getJPAExceptionForNucleusException(jpe);
        }
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#getResultList()
     */
    public List getResultList()
    {
        if (executeProcessed)
        {
            return (List)getStoredProcQuery().getNextResults();
        }

        try
        {
            if (flushMode == FlushModeType.AUTO && em.getTransaction().isActive())
            {
                em.flush();
            }

            Boolean hasResultSet = (Boolean)query.execute();
            if (!hasResultSet)
            {
                throw new IllegalStateException("Stored proc should have returned result set but didnt");
            }
            return (List)getStoredProcQuery().getNextResults();
        }
        catch (NoQueryResultsException nqre)
        {
            return null;
        }
        catch (org.datanucleus.store.query.QueryTimeoutException qte)
        {
            throw new QueryTimeoutException();
        }
        catch (NucleusException jpe)
        {
            throw NucleusJPAHelper.getJPAExceptionForNucleusException(jpe);
        }
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#getSingleResult()
     */
    public Object getSingleResult()
    {
        if (executeProcessed)
        {
            query.setUnique(true);
            return getStoredProcQuery().getNextResults();
        }

        try
        {
            if (flushMode == FlushModeType.AUTO && em.getTransaction().isActive())
            {
                em.flush();
            }

            query.setUnique(true);
            Boolean hasResultSet = (Boolean)query.execute();
            if (!hasResultSet)
            {
                throw new IllegalStateException("Stored proc should have returned result set but didnt");
            }
            return getStoredProcQuery().getNextResults();
        }
        catch (NoQueryResultsException nqre)
        {
            return null;
        }
        catch (org.datanucleus.store.query.QueryTimeoutException qte)
        {
            throw new QueryTimeoutException();
        }
        catch (NucleusException jpe)
        {
            throw NucleusJPAHelper.getJPAExceptionForNucleusException(jpe);
        }
    }

    public JPAStoredProcedureQuery setFlushMode(FlushModeType mode)
    {
        return (JPAStoredProcedureQuery) super.setFlushMode(mode);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.api.jpa.JPAQuery#setHint(java.lang.String, java.lang.Object)
     */
    @Override
    public JPAStoredProcedureQuery setHint(String hintName, Object value)
    {
        return (JPAStoredProcedureQuery) super.setHint(hintName, value);
    }
}