/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.QueryTimeoutException;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.compiler.Symbol;
import org.datanucleus.query.compiler.SymbolTable;
import org.datanucleus.store.query.AbstractJavaQuery;
import org.datanucleus.store.query.Query;
import org.datanucleus.store.query.QueryInvalidParametersException;
import org.datanucleus.store.query.NoQueryResultsException;
import org.datanucleus.store.query.QueryNotUniqueException;
import org.datanucleus.store.query.Query.QueryType;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Basic implementation of a JPA Query.
 * Wraps an internal query.
 * @param <X> Type of the candidate of the query
 */
public class JPAQuery<X> implements TypedQuery<X>
{
    public static final String QUERY_HINT_TIMEOUT = "javax.persistence.query.timeout";

    /** Underlying EntityManager handling persistence. */
    JPAEntityManager em;

    /** Query language. */
    String language;

    /** Underlying query providing the querying capability. */
    org.datanucleus.store.query.Query query;

    /** Flush mode for the query. */
    FlushModeType flushMode = FlushModeType.AUTO;

    /** Lock mode for the query. */
    LockModeType lockMode = null;

    /** The current start position. */
    private int startPosition = 0;

    /** The current max number of results. */
    private int maxResults = -1;

    boolean parametersLoaded = false;

    Set<Parameter<?>> parameters = null;

    JPAFetchPlan fetchPlan;

    /**
     * Constructor for a query used by JPA.
     * @param em Entity Manager
     * @param query Underlying query
     * @param language Query language
     */
    public JPAQuery(JPAEntityManager em, org.datanucleus.store.query.Query query, String language)
    {
        this.em = em;
        this.query = query;
        this.language = language;
        this.flushMode = em.getFlushMode(); // Default to flush mode of EntityManager
        this.query.setCacheResults(false);
        this.fetchPlan = new JPAFetchPlan(query.getFetchPlan());

        // Enable closure of results at ExecutionContext close (i.e EntityManager close). User can turn it off by adding this hint as "false"
        this.query.addExtension(Query.EXTENSION_CLOSE_RESULTS_AT_EC_CLOSE, "true");
    }

    public JPAFetchPlan getFetchPlan()
    {
        assertIsOpen();
        return fetchPlan;
    }

    /**
     * Method to execute a (UPDATE/DELETE) query returning the number of changed records.
     * @return Number of records updated/deleted with the query.
     * @throws QueryTimeoutException if the query times out
     */
    public int executeUpdate()
    {
        assertIsOpen();
        if (query.getType() == QueryType.SELECT)
        {
            throw new IllegalStateException(Localiser.msg("Query.ExecuteUpdateForSelectInvalid"));
        }

        try
        {
            if (flushMode == FlushModeType.AUTO && em.isTransactionActive())
            {
                em.flush();
            }

            if (lockMode == LockModeType.PESSIMISTIC_READ || lockMode == LockModeType.PESSIMISTIC_WRITE)
            {
                query.setSerializeRead(Boolean.TRUE);
            }

            Object result = query.executeWithMap(null); // Params defined using setParameter() earlier
            if (result != null)
            {
                return ((Long)result).intValue();
            }

            throw new NucleusException("Invalid return from query for an update/delete. Expected Long");
        }
        catch (NoQueryResultsException nqre)
        {
            return 0;
        }
        catch (QueryInvalidParametersException ex)
        {
            throw new IllegalArgumentException(ex.getMessage(),ex);
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

    /**
     * Method to execute a (SELECT) query statement returning multiple results.
     * @return The results
     * @throws QueryTimeoutException if the query times out
     */
    public List getResultList()
    {
        assertIsOpen();
        if (query.getType() != QueryType.SELECT)
        {
            throw new IllegalStateException(Localiser.msg("Query.GetResultForUpdateInvalid"));
        }

        try
        {
            if (flushMode == FlushModeType.AUTO && em.isTransactionActive())
            {
                em.flush();
            }

            if (lockMode == LockModeType.PESSIMISTIC_READ || lockMode == LockModeType.PESSIMISTIC_WRITE)
            {
                query.setSerializeRead(Boolean.TRUE);
            }

            if (QueryUtils.queryReturnsSingleRow(query))
            {
                X res = (X) query.executeWithMap(null); // Params defined using setParameter() earlier
                List l = new ArrayList<X>();
                l.add(res);
                return l;
            }

            return (List)query.executeWithMap(null); // Params defined using setParameter() earlier
        }
        catch (NoQueryResultsException nqre)
        {
            return null;
        }
        catch (QueryInvalidParametersException ex)
        {
            throw new IllegalArgumentException(ex.getMessage(),ex);
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

    /**
     * Method to execute a SELECT statement returning a single result.
     * @return the result
     * @throws QueryTimeoutException if the query times out
     */
    public X getSingleResult()
    {
        assertIsOpen();
        if (query.getType() != QueryType.SELECT)
        {
            throw new IllegalStateException(Localiser.msg("Query.GetResultForUpdateInvalid"));
        }

        try
        {
            if (flushMode == FlushModeType.AUTO && em.isTransactionActive())
            {
                em.flush();
            }

            if (lockMode == LockModeType.PESSIMISTIC_READ || lockMode == LockModeType.PESSIMISTIC_WRITE)
            {
                query.setSerializeRead(Boolean.TRUE);
            }

            query.setUnique(true);

            return (X)query.executeWithMap(null); // Params defined using setParameter() earlier
        }
        catch (NoQueryResultsException nqre)
        {
            throw new NoResultException("No results for query: " + query.toString());
        }
        catch (QueryNotUniqueException ex)
        {
            throw new NonUniqueResultException("Expected a single result for query: " + query.toString() +
                " : " + StringUtils.getStringFromStackTrace(ex));
        }
        catch (QueryInvalidParametersException ex)
        {
            throw new IllegalArgumentException(ex.getMessage(),ex);
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

    /**
     * Method to set the results to start from a particular position.
     * @param startPosition position of first result numbered from 0
     * @return The query
     */
    public TypedQuery<X> setFirstResult(int startPosition)
    {
        assertIsOpen();
        if (startPosition < 0)
        {
            throw new IllegalArgumentException(Localiser.msg("Query.StartPositionInvalid"));
        }

        this.startPosition = startPosition;
        if (this.maxResults == -1)
        {
            query.setRange(this.startPosition, Long.MAX_VALUE);
        }
        else
        {
            query.setRange(this.startPosition, this.startPosition+this.maxResults);
        }
        return this;
    }

    /**
     * Method to set the max number of results to return.
     * @param max Number of results max
     * @return The query
     */
    public TypedQuery<X> setMaxResults(int max)
    {
        assertIsOpen();
        if (max < 0)
        {
            throw new IllegalArgumentException(Localiser.msg("Query.MaxResultsInvalid"));
        }

        this.maxResults = max;
        query.setRange(startPosition, startPosition+max);
        return this;
    }

    /**
     * The maximum number of results the query object was set to retrieve. 
     * Returns Integer.MAX_VALUE if setMaxResults was not applied to the query object.
     * @return maximum number of results
     */
    public int getMaxResults()
    {
        assertIsOpen();
        // This was used datanucleus-api-jpa <= v5.0.6 but would prevent getting correct results when allowing "RANGE" in the JPQL, so commented out.
        /*if (maxResults == -1)
        {
            return Integer.MAX_VALUE;
        }*/
        long queryMin = query.getRangeFromIncl();
        long queryMax = query.getRangeToExcl();
        long max = queryMax - queryMin;
        if (max > Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        return (int)max;
    }

    /**
     * The position of the first result the query object was set to retrieve. 
     * Returns 0 if setFirstResult was not applied to the query object.
     * @return position of first result
     */
    public int getFirstResult()
    {
        assertIsOpen();
        return (int)query.getRangeFromIncl();
    }

    /**
     * Mutator for the flush mode.
     * @param mode Flush mode
     * @return The query
     */
    public TypedQuery<X> setFlushMode(FlushModeType mode)
    {
        assertIsOpen();
        flushMode = mode;
        return this;
    }

    /**
     * The flush mode in effect for the query execution. If a flush mode has not been set for 
     * the query object, returns the flush mode in effect for the entity manager.
     * @return flush mode
     */
    public FlushModeType getFlushMode()
    {
        assertIsOpen();
        return flushMode;
    }

    /**
     * Method to add a vendor extension to the query.
     * If the hint name is not recognized, it is silently ignored.
     * @param hintName Name of the "hint"
     * @param value Value for the "hint"
     * @return the same query instance
     * @throws IllegalArgumentException if the second argument is not valid for the implementation
     */
    public TypedQuery<X> setHint(String hintName, Object value)
    {
        assertIsOpen();
        if (hintName == null)
        {
            return this;
        }
        if (hintName.equalsIgnoreCase(QUERY_HINT_TIMEOUT))
        {
            query.setDatastoreReadTimeoutMillis((Integer)value);
        }
        else if (hintName.equalsIgnoreCase(JPAEntityGraph.FETCHGRAPH_PROPERTY))
        {
            JPAEntityGraph eg = (JPAEntityGraph) value;
            String egName = eg.getName();
            if (eg.getName() == null)
            {
                JPAEntityManagerFactory emf = (JPAEntityManagerFactory)em.getEntityManagerFactory();
                String tmpEntityGraphName = emf.getDefinedEntityGraphName();
                emf.registerEntityGraph(eg, tmpEntityGraphName);
                egName = tmpEntityGraphName;
            }
            query.getFetchPlan().setGroup(egName);
            // TODO Need to deregister any temporary EntityGraph
        }
        else if (hintName.equalsIgnoreCase(JPAEntityGraph.LOADGRAPH_PROPERTY))
        {
            JPAEntityGraph eg = (JPAEntityGraph) value;
            String egName = eg.getName();
            if (eg.getName() == null)
            {
                JPAEntityManagerFactory emf = (JPAEntityManagerFactory)em.getEntityManagerFactory();
                String tmpEntityGraphName = emf.getDefinedEntityGraphName();
                emf.registerEntityGraph(eg, tmpEntityGraphName);
                egName = tmpEntityGraphName;
            }
            query.getFetchPlan().addGroup(egName);
            // TODO Need to deregister any temporary EntityGraph
        }
        else if (hintName.equalsIgnoreCase("datanucleus.query.fetchSize"))
        {
            if (value instanceof Integer)
            {
                query.getFetchPlan().setFetchSize((Integer)value);
            }
            else if (value instanceof Long)
            {
                query.getFetchPlan().setFetchSize(((Long)value).intValue());
            }
        }

        // Just treat a "hint" as an "extension".
        query.addExtension(hintName, value);
        return this;
    }

    /**
     * Get the hints and associated values that are in effect for the query instance.
     * @return query hints
     */
    public Map getHints()
    {
        assertIsOpen();
        Map extensions = query.getExtensions();
        Map map = new HashMap();
        if (extensions != null && !extensions.isEmpty())
        {
            map.putAll(extensions);
        }
        return map;
    }

    /**
     * Get the names of the hints that are supported for query objects.
     * These hints correspond to hints that may be passed to the methods of the Query interface 
     * that take hints as arguments or used with the NamedQuery and NamedNativeQuery annotations.
     * These include all standard query hints as well as vendor-specific hints supported by the 
     * provider. These hints may or may not currently be in effect.
     * @return hints
     */
    public Set<String> getSupportedHints()
    {
        assertIsOpen();
        return query.getSupportedExtensions();
    }

    /**
     * Bind the value of a Parameter object.
     * @param param parameter to be set
     * @param value parameter value
     * @return query instance
     * @throws IllegalArgumentException if parameter does not correspond to a parameter of the query
     */
     public <T> TypedQuery<X> setParameter(Parameter<T> param, T value)
     {
         assertIsOpen();
         if (param == null)
         {
             throw new IllegalArgumentException("Parameter object is null");
         }

         if (param.getName() != null)
         {
             try
             {
                 query.setImplicitParameter(param.getName(), value);
             }
             catch (QueryInvalidParametersException ipe)
             {
                 throw new IllegalArgumentException(ipe.getMessage(), ipe);
             }
             return this;
         }

         try
         {
             if (isNativeQuery())
             {
                 query.setImplicitParameter(param.getPosition(), value);
             }
             else
             {
                 query.setImplicitParameter("" + param.getPosition(), value);
             }
         }
         catch (QueryInvalidParametersException ipe)
         {
             throw new IllegalArgumentException(ipe.getMessage(), ipe);
         }
         return this;
     }

    /**
     * Bind an argument to a named parameter.
     * @param name the parameter name
     * @param value The value for the param
     * @return the same query instance
     * @throws IllegalArgumentException if parameter name does not correspond to parameter in query string or argument is of incorrect type
     */
    public TypedQuery<X> setParameter(String name, Object value)
    {
        assertIsOpen();
        try
        {
            query.setImplicitParameter(name, value);
        }
        catch (QueryInvalidParametersException ipe)
        {
            throw new IllegalArgumentException(ipe.getMessage(), ipe);
        }
        return this;
    }

    /**
     * Bind an argument to a positional parameter.
     * @param position Parameter position
     * @param value The value
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to positional parameter of query 
     *     or argument is of incorrect type
     */
    public TypedQuery<X> setParameter(int position, Object value)
    {
        assertIsOpen();
        try
        {
            if (isNativeQuery())
            {
                query.setImplicitParameter(position, value);
            }
            else
            {
                query.setImplicitParameter("" + position, value);
            }
        }
        catch (QueryInvalidParametersException ipe)
        {
            throw new IllegalArgumentException(ipe.getMessage(), ipe);
        }
        return this;
    }

    /**
     * Bind an instance of java.util.Date to a named parameter.
     * @param name Name of the param
     * @param value Value for the param
     * @param temporalType The temporal type
     * @return the same query instance
     * @throws IllegalArgumentException if parameter name does not correspond to parameter in query string
     */
    public TypedQuery<X> setParameter(String name, Date value, TemporalType temporalType)
    {
        assertIsOpen();
        Object paramValue = value;
        if (temporalType == TemporalType.TIME && !(value instanceof Time))
        {
            paramValue = new Time(value.getTime());
        }
        else if (temporalType == TemporalType.TIMESTAMP && !(value instanceof Timestamp))
        {
            paramValue = new Timestamp(value.getTime());
        }

        try
        {
            query.setImplicitParameter(name, paramValue);
        }
        catch (QueryInvalidParametersException ipe)
        {
            throw new IllegalArgumentException(ipe.getMessage());
        }
        return this;
    }

    /**
     * Bind an instance of java.util.Calendar to a named parameter.
     * @param name name of the param
     * @param value Value for the param
     * @param temporalType The temporal type
     * @return the same query instance
     * @throws IllegalArgumentException if parameter name does not correspond to parameter in query string
     */
    public TypedQuery<X> setParameter(String name, Calendar value, TemporalType temporalType)
    {
        assertIsOpen();
        Object paramValue = value;
        if (value != null)
        {
            if (temporalType == TemporalType.DATE)
            {
                paramValue = value.getTime();
            }
            else if (temporalType == TemporalType.TIME)
            {
                paramValue = new Time(value.getTime().getTime());
            }
            else if (temporalType == TemporalType.TIMESTAMP)
            {
                paramValue = new Timestamp(value.getTime().getTime());
            }
        }

        try
        {
            query.setImplicitParameter(name, paramValue);
        }
        catch (QueryInvalidParametersException ipe)
        {
            throw new IllegalArgumentException(ipe.getMessage());
        }
        return this;
    }

    /**
     * Bind an instance of java.util.Date to a positional parameter.
     * @param position Parameter position
     * @param value Value for the param
     * @param temporalType Temporal Type
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to positional parameter of query
     */
    public TypedQuery<X> setParameter(int position, Date value, TemporalType temporalType)
    {
        assertIsOpen();
        Object paramValue = value;
        if (value != null)
        {
            if (temporalType == TemporalType.TIME && !(value instanceof Time))
            {
                paramValue = new Time(value.getTime());
            }
            else if (temporalType == TemporalType.TIMESTAMP && !(value instanceof Timestamp))
            {
                paramValue = new Timestamp(value.getTime());
            }
        }

        try
        {
            if (isNativeQuery())
            {
                query.setImplicitParameter(position, paramValue);
            }
            else
            {
                query.setImplicitParameter("" + position, paramValue);
            }
        }
        catch (QueryInvalidParametersException ipe)
        {
            throw new IllegalArgumentException(ipe.getMessage());
        }
        return this;
    }

    /**
     * Bind an instance of java.util.Calendar to a positional parameter.
     * @param position Parameter position
     * @param value Value for the param
     * @param temporalType Temporal type
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to positional parameter of query
     */
    public TypedQuery<X> setParameter(int position, Calendar value, TemporalType temporalType)
    {
        assertIsOpen();
        Object paramValue = value;
        if (value != null)
        {
            if (temporalType == TemporalType.DATE)
            {
                paramValue = value.getTime();
            }
            else if (temporalType == TemporalType.TIME)
            {
                paramValue = new Time(value.getTime().getTime());
            }
            else if (temporalType == TemporalType.TIMESTAMP)
            {
                paramValue = new Timestamp(value.getTime().getTime());
            }
        }

        try
        {
            if (isNativeQuery())
            {
                query.setImplicitParameter(position, paramValue);
            }
            else
            {
                query.setImplicitParameter("" + position, paramValue);
            }
        }
        catch (QueryInvalidParametersException ipe)
        {
            throw new IllegalArgumentException(ipe.getMessage());
        }
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#setParameter(javax.persistence.Parameter, java.util.Calendar, javax.persistence.TemporalType)
     */
    public JPAQuery<X> setParameter(Parameter<Calendar> param, Calendar cal, TemporalType type)
    {
        assertIsOpen();
        if (param.getName() != null)
        {
            setParameter(param.getName(), cal, type);
        }
        else
        {
            setParameter(param.getPosition(), cal, type);
        }
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#setParameter(javax.persistence.Parameter, java.util.Date, javax.persistence.TemporalType)
     */
    public TypedQuery<X> setParameter(Parameter<Date> param, Date date, TemporalType type)
    {
        assertIsOpen();
        if (param.getName() != null)
        {
            setParameter(param.getName(), date, type);
        }
        else
        {
            setParameter(param.getPosition(), date, type);
        }
        return this;
    }

    /**
     * Accessor for the internal query.
     * @return Internal query
     */
    public org.datanucleus.store.query.Query getInternalQuery()
    {
        return query;
    }

    /**
     * Return an object of the specified type to allow access to the provider-specific API.
     * If the provider's Query implementation does not support the specified class, the 
     * PersistenceException is thrown.
     * @param cls the class of the object to be returned. This is normally either the underlying 
     * Query implementation class or an interface that it implements.
     * @return an instance of the specified class
     * @throws PersistenceException if the provider does not support the call.
     */
    public <T> T unwrap(Class<T> cls)
    {
        assertIsOpen();
        if (cls == org.datanucleus.store.query.Query.class)
        {
            return (T)query;
        }
        throw new PersistenceException("Not supported unwrapping of query to " + cls.getName());
    }

    /**
     * Accessor for the query language.
     * @return Query language
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * Get the query parameter objects.
     * Returns empty set if the query has no parameters.
     * @return parameter objects
     */
    public Set<Parameter<?>> getParameters()
    {
        assertIsOpen();
        if (isNativeQuery())
        {
            throw new IllegalStateException("Not supported on native query");
        }

        loadParameters();
        if (parameters == null)
        {
            return Collections.EMPTY_SET;
        }

        Set<Parameter<?>> params = new HashSet<Parameter<?>>();
        params.addAll(parameters);
        return params;
    }

    protected void loadParameters()
    {
        if (parametersLoaded)
        {
            return;
        }

        // Load up parameters by (generic) compiling the query
        if (query.getCompilation() == null)
        {
            try
            {
                ((AbstractJavaQuery)query).compileGeneric(null);
            }
            catch (Throwable thr)
            {
            }
        }

        if (query.getCompilation() != null)
        {
            QueryCompilation compilation = query.getCompilation();
            loadParametersForCompilation(compilation);

            // Add on parameters defined in subqueries
            String[] subqueryAliases = compilation.getSubqueryAliases();
            if (subqueryAliases != null && subqueryAliases.length > 0)
            {
                for (int i=0;i<subqueryAliases.length;i++)
                {
                    QueryCompilation subqCompilation = compilation.getCompilationForSubquery(subqueryAliases[i]);
                    loadParametersForCompilation(subqCompilation);
                }
            }
        }
        parametersLoaded = true;
    }

    protected void loadParametersForCompilation(QueryCompilation compilation)
    {
        SymbolTable symTbl = compilation.getSymbolTable();
        for (String symName : symTbl.getSymbolNames())
        {
            Symbol sym = symTbl.getSymbol(symName);
            if (sym.getType() == Symbol.PARAMETER)
            {
                if (parameters == null)
                {
                    parameters = new HashSet<Parameter<?>>();
                }

                Parameter param = null;
                if (query.toString().indexOf("?" + sym.getQualifiedName()) >= 0)
                {
                    // Positional parameters
                    try
                    {
                        param = new JPAQueryParameter(Integer.valueOf(sym.getQualifiedName()), sym.getValueType());
                    }
                    catch (NumberFormatException nfe)
                    {
                    }
                    
                }
                else
                {
                    // Named parameters
                    param = new JPAQueryParameter(sym.getQualifiedName(), sym.getValueType());
                }
                parameters.add(param);
            }
        }
    }

    /**
     * Get the parameter of the given name and type.
     * @return parameter object
     * @throws IllegalArgumentException if the parameter of the specified name and type doesn't exist
     */
    public <T> Parameter<T> getParameter(String name, Class<T> type)
    {
        assertIsOpen();
        if (isNativeQuery())
        {
            throw new IllegalStateException("Not supported on native query");
        }

        loadParameters();
        if (parameters == null)
        {
            throw new IllegalArgumentException("No parameter with name " + name + " and type=" + type.getName());
        }
        for (Parameter param : parameters)
        {
            if (param.getName() != null && param.getName().equals(name))
            {
                return param;
            }
        }
        throw new IllegalArgumentException("No parameter with name " + name + " and type=" + type.getName());
    }

    /**
     * Get the positional parameter with the given position and type.
     * @return parameter object
     * @throws IllegalArgumentException if the parameter with the specified position and type doesn't exist
     */
    public <T> Parameter<T> getParameter(int position, Class<T> type)
    {
        assertIsOpen();
        if (isNativeQuery())
        {
            throw new IllegalStateException("Not supported on native query");
        }

        loadParameters();
        if (parameters == null)
        {
            throw new IllegalArgumentException("No parameter at position=" + position + " and type=" + type.getName());
        }
        for (Parameter param : parameters)
        {
            if (param.getPosition() != null && param.getPosition() == position)
            {
                return param;
            }
        }
        throw new IllegalArgumentException("No parameter at position=" + position + " and type=" + type.getName());
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#getParameter(int)
     */
    public Parameter<?> getParameter(int position)
    {
        assertIsOpen();
        if (isNativeQuery())
        {
            throw new IllegalStateException("Not supported on native query");
        }

        loadParameters();
        if (parameters == null)
        {
            throw new IllegalArgumentException("No parameter at position=" + position);
        }
        for (Parameter param : parameters)
        {
            if (param.getPosition() != null && param.getPosition() == position)
            {
                return param;
            }
        }
        throw new IllegalArgumentException("No parameter at position=" + position);
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#getParameter(java.lang.String)
     */
    public Parameter<?> getParameter(String name)
    {
        assertIsOpen();
        if (isNativeQuery())
        {
            throw new IllegalStateException("Not supported on native query");
        }

        loadParameters();
        if (parameters == null)
        {
            throw new IllegalArgumentException("No parameter with name " + name);
        }
        for (Parameter param : parameters)
        {
            if (param.getName() != null && param.getName().equals(name))
            {
                return param;
            }
        }
        throw new IllegalArgumentException("No parameter with name " + name);
    }

    /**
     * Return the value that has been bound to the parameter.
     * @param param parameter object
     * @return parameter value
     * @throws IllegalStateException if the parameter has not been bound
     */
    public <T> T getParameterValue(Parameter<T> param)
    {
        assertIsOpen();
        if (param.getName() != null)
        {
            if (query.getImplicitParameters() == null)
            {
                throw new IllegalArgumentException("No parameter with name " + param.getName());
            }

            if (query.getImplicitParameters().containsKey(param.getName()))
            {
                return (T)query.getImplicitParameters().get(param.getName());
            }
        }
        else
        {
            if (query.getImplicitParameters() == null)
            {
                throw new IllegalArgumentException("No parameter at position " + param.getPosition());
            }

            if (isNativeQuery())
            {
                if (query.getImplicitParameters().containsKey(param.getPosition()))
                {
                    return (T)query.getImplicitParameters().get(param.getPosition());
                }
            }
            else
            {
                if (query.getImplicitParameters().containsKey("" + param.getPosition()))
                {
                    return (T)query.getImplicitParameters().get("" + param.getPosition());
                }
            }
        }
        throw new IllegalStateException("No parameter matching " + param + " bound to this query");
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#getParameterValue(int)
     */
    public Object getParameterValue(int position)
    {
        assertIsOpen();
        if (query.getImplicitParameters() == null)
        {
            throw new IllegalArgumentException("No parameter at position " + position);
        }

        if (isNativeQuery())
        {
            if (query.getImplicitParameters().containsKey(position))
            {
                return query.getImplicitParameters().get(position);
            }
        }
        else
        {
            if (query.getImplicitParameters().containsKey("" + position))
            {
                return query.getImplicitParameters().get("" + position);
            }
        }
        throw new IllegalArgumentException("No parameter at position " + position);
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#getParameterValue(java.lang.String)
     */
    public Object getParameterValue(String name)
    {
        assertIsOpen();
        if (query.getImplicitParameters() == null)
        {
            throw new IllegalArgumentException("No parameter with name " + name);
        }

        if (query.getImplicitParameters().containsKey(name))
        {
            return query.getImplicitParameters().get(name);
        }
        throw new IllegalArgumentException("No parameter with name " + name);
    }

    /* (non-Javadoc)
     * @see javax.persistence.Query#isBound(javax.persistence.Parameter)
     */
    public boolean isBound(Parameter<?> param)
    {
        assertIsOpen();
        if (parameters == null)
        {
            return false;
        }

        if (param.getName() != null)
        {
            if (query.getImplicitParameters().containsKey(param.getName()))
            {
                return true;
            }
        }
        else
        {
            if (isNativeQuery())
            {
                if (query.getImplicitParameters().containsKey(param.getPosition()))
                {
                    return true;
                }
            }
            else
            {
                if (query.getImplicitParameters().containsKey("" + param.getPosition()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public LockModeType getLockMode()
    {
        assertIsOpen();
        if (query.getType() != QueryType.SELECT || !query.getLanguage().equals("JPQL"))
        {
            throw new IllegalStateException("Query has to be a SELECT JPQL query to allow locking");
        }

        return lockMode;
    }

    public TypedQuery<X> setLockMode(LockModeType lock)
    {
        assertIsOpen();
        if (query.getType() != QueryType.SELECT || !query.getLanguage().equals("JPQL"))
        {
            throw new IllegalStateException("Query has to be a SELECT JPQL query to allow locking");
        }

        this.lockMode = lock;
        return this;
    }

    /**
     * Convenience method to allow setting of the result class of the internal query.
     * @param resultClass The result class
     * @return This query
     */
    TypedQuery<X> setResultClass(Class resultClass)
    {
        if (resultClass == Object[].class)
        {
            // Internal default when we have a result specified so ignore
            query.setResultClass(null);
        }
        else
        {
            query.setResultClass(resultClass);
        }
        return this;
    }

    /**
     * Method to return the single-string form of the query.
     * Note that the JPA spec doesn't define this methods handling and this is an extension.
     * @return The single-string form of the query
     */
    public String toString()
    {
        assertIsOpen();
        return query.toString();
    }

    /**
     * Accessor for the native query invoked by this query (if known at this time and supported by the store plugin).
     * @return The native query (e.g for RDBMS this is the SQL).
     */
    public Object getNativeQuery()
    {
        return query.getNativeQuery();
    }

    /**
     * Save this query as a named query with the specified name.
     * See also EntityManagerFactory.addNamedQuery(String, Query)
     * @param name The name to refer to it under
     */
    public void saveAsNamedQuery(String name)
    {
        em.getEntityManagerFactory().addNamedQuery(name, this);
    }

    protected boolean isNativeQuery()
    {
        return language.equals(em.getExecutionContext().getStoreManager().getNativeQueryLanguage());
    }

    /**
     * Assert if the EntityManager is closed.
     * @throws IllegalStateException When the EntityManaged is closed
     */
    private void assertIsOpen()
    {
        if (!em.isOpen())
        {
            throw new IllegalStateException(Localiser.msg("EM.IsClosed"));
        }
    }
}