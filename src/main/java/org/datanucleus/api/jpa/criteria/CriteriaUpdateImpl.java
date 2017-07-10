/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.api.jpa.criteria;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.query.compiler.JPQLSymbolResolver;
import org.datanucleus.query.compiler.PropertySymbol;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.compiler.SymbolTable;
import org.datanucleus.query.expression.ClassExpression;
import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.store.query.Query;

/**
 * Implementation of a Criteria Update query.
 */
public class CriteriaUpdateImpl<T> implements CriteriaUpdate<T>, Serializable
{
    static final long serialVersionUID = 5874782279554441662L;

    private CriteriaBuilderImpl cb;

    private RootImpl<T> from;
    private Map sets = new HashMap();
    private PredicateImpl filter;

    /** The JPQL single-string delete query that this equates to (cached). */
    String jpqlString = null;

    /** The generic query compilation that this equates to (cached). */
    QueryCompilation compilation = null;

    public CriteriaUpdateImpl(CriteriaBuilderImpl cb)
    {
        this.cb = cb;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CommonAbstractCriteria#getRestriction()
     */
    public Predicate getRestriction()
    {
        return filter;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CommonAbstractCriteria#subquery(java.lang.Class)
     */
    public <U> Subquery<U> subquery(Class<U> cls)
    {
        throw new UnsupportedOperationException("CriteriaUpdate.subquery not yet supported");
        // TODO Support this
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#from(java.lang.Class)
     */
    public Root<T> from(Class<T> cls)
    {
        discardCompiled();
        EntityType<T> entity = cb.getEntityManagerFactory().getMetamodel().entity(cls);
        if (entity == null)
        {
            throw new IllegalArgumentException("The specified class (" + cls.getName() + ") is not an entity");
        }

        return from(entity);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#from(javax.persistence.metamodel.EntityType)
     */
    public Root<T> from(EntityType<T> type)
    {
        discardCompiled();
        from = new RootImpl<T>(cb, type);
        return from;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#getRoot()
     */
    public Root<T> getRoot()
    {
        return from;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#set(javax.persistence.criteria.Path, javax.persistence.criteria.Expression)
     */
    public <Y> CriteriaUpdate<T> set(Path<Y> path, Expression<? extends Y> expr)
    {
        sets.put(path, expr);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#set(javax.persistence.criteria.Path, java.lang.Object)
     */
    public <Y, X extends Y> CriteriaUpdate<T> set(Path<Y> path, X val)
    {
        sets.put(path, val);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#set(javax.persistence.metamodel.SingularAttribute, javax.persistence.criteria.Expression)
     */
    public <Y> CriteriaUpdate<T> set(SingularAttribute<? super T, Y> attr, Expression<? extends Y> expr)
    {
        return set(from.get(attr), expr);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#set(javax.persistence.metamodel.SingularAttribute, java.lang.Object)
     */
    public <Y, X extends Y> CriteriaUpdate<T> set(SingularAttribute<? super T, Y> attr, X val)
    {
        return set(from.get(attr), val);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#set(java.lang.String, java.lang.Object)
     */
    public CriteriaUpdate<T> set(String name, Object val)
    {
        throw new UnsupportedOperationException("CriteriaUpdate.set(String,Object) not yet supported");
        // TODO Support this
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#where(javax.persistence.criteria.Expression)
     */
    public CriteriaUpdate<T> where(Expression<Boolean> expr)
    {
        discardCompiled();
        if (expr == null)
        {
            filter = null;
            return this;
        }
        filter = (PredicateImpl)expr;
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaUpdate#where(javax.persistence.criteria.Predicate[])
     */
    public CriteriaUpdate<T> where(Predicate... exprs)
    {
        discardCompiled();
        if (exprs == null || exprs.length == 0)
        {
            filter = null;
            return this;
        }

        if (filter == null)
        {
            filter = new PredicateImpl(cb);
        }
        for (int i=0;i<exprs.length;i++)
        {
            filter = filter.append(exprs[i]);
        }

        return this;
    }

    protected void discardCompiled()
    {
        jpqlString = null;
        compilation = null;
    }

    /**
     * Accessor for the generic compilation that this criteria query equates to.
     * @param mmgr MetaData manager
     * @param clr ClassLoader resolver
     * @return The generic compilation
     */
    public QueryCompilation getCompilation(MetaDataManager mmgr, ClassLoaderResolver clr)
    {
        return getCompilation(mmgr, clr, null);
    }

    /**
     * Accessor for the generic compilation that this criteria query equates to.
     * @param mmgr Metadata manager
     * @param clr ClassLoader resolver
     * @param parentSymtbl Parent symbol table (when this is a subquery)
     * @return The generic compilation
     */
    public QueryCompilation getCompilation(MetaDataManager mmgr, ClassLoaderResolver clr, SymbolTable parentSymtbl)
    {
        if (compilation == null)
        {
            // Not yet compiled, so compile it
            Class candidateClass = from.getJavaType();
            String candidateAlias = from.getAlias();
            if (candidateAlias == null)
            {
                candidateAlias = "DN_THIS";
                from.alias(candidateAlias);
            }

            SymbolTable symtbl = new SymbolTable();
            symtbl.setSymbolResolver(new JPQLSymbolResolver(mmgr, clr, symtbl, candidateClass, candidateAlias));
            symtbl.addSymbol(new PropertySymbol(candidateAlias, candidateClass));
            if (parentSymtbl != null)
            {
                symtbl.setParentSymbolTable(parentSymtbl);
            }

            org.datanucleus.query.expression.Expression[] updateExprs = new org.datanucleus.query.expression.Expression[sets.size()];
            Iterator<Map.Entry> updateEntryIter = sets.entrySet().iterator();
            int i = 0;
            while (updateEntryIter.hasNext())
            {
                Map.Entry entry = updateEntryIter.next();
                org.datanucleus.query.expression.Expression keyQueryExpr = ((ExpressionImpl) entry.getKey()).getQueryExpression();
                Object val = entry.getValue();
                org.datanucleus.query.expression.Expression valQueryExpr = null;
                if (val instanceof ExpressionImpl)
                {
                    valQueryExpr = ((ExpressionImpl)val).getQueryExpression();
                    valQueryExpr.bind(symtbl);
                }
                else
                {
                    valQueryExpr = new Literal(val);
                }
                updateExprs[i++] = new DyadicExpression(keyQueryExpr, org.datanucleus.query.expression.Expression.OP_EQ, valQueryExpr);
            }

            org.datanucleus.query.expression.Expression[] fromExprs = new org.datanucleus.query.expression.Expression[1];
            Set<Join<T, ?>> frmJoins = from.getJoins();
            if (frmJoins != null && !frmJoins.isEmpty())
            {
                Iterator<Join<T, ?>> frmJoinIter = frmJoins.iterator();
                while (frmJoinIter.hasNext())
                {
                    Join<T, ?> frmJoin = frmJoinIter.next();
                    if (frmJoin.getAlias() != null)
                    {
                        Class frmJoinCls = ((JoinImpl)frmJoin).getType().getJavaType();
                        symtbl.addSymbol(new PropertySymbol(frmJoin.getAlias(), frmJoinCls));
                    }
                }
            }
            ClassExpression clsExpr = (ClassExpression)from.getQueryExpression(true);
            clsExpr.bind(symtbl);
            fromExprs[0] = clsExpr;

            org.datanucleus.query.expression.Expression filterExpr = null;
            if (filter != null)
            {
                filterExpr = filter.getQueryExpression();
                if (filterExpr != null)
                {
                    filterExpr.bind(symtbl);
                }
            }

            compilation = new QueryCompilation(candidateClass, candidateAlias, symtbl, null,
                fromExprs, filterExpr, null, null, null, updateExprs);
            compilation.setQueryLanguage(Query.LANGUAGE_JPQL);
        }

        // TODO Handle subqueries

        return compilation;
    }

    /**
     * Method to return a single-string representation of the criteria update query in JPQL.
     * @return The single-string form
     */
    public String toString()
    {
        if (jpqlString == null)
        {
            // Generate the query string
            StringBuilder str = new StringBuilder();
            str.append("UPDATE ");
            str.append(from.toString(true));
            str.append(" SET ");

            Iterator<Map.Entry> setEntryIter = sets.entrySet().iterator();
            while (setEntryIter.hasNext())
            {
                Map.Entry entry = setEntryIter.next();
                Object key = entry.getKey();
                Object val = entry.getValue();
                str.append(key.toString());
                str.append(" = ");
                if (val instanceof String || val instanceof Character)
                {
                    str.append("'").append(val.toString()).append("'");
                }
                else
                {
                    str.append(val.toString());
                }
                if (setEntryIter.hasNext())
                {
                    str.append(",");
                }
            }
            str.append(" ");

            if (filter != null)
            {
                // WHERE clause
                str.append("WHERE ").append(filter.toString()).append(" ");
            }

            jpqlString = str.toString().trim();
        }
        return jpqlString;
    }
}