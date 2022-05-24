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
import java.util.Iterator;
import java.util.Set;

import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.QueryLanguage;
import org.datanucleus.store.query.compiler.JPQLSymbolResolver;
import org.datanucleus.store.query.compiler.PropertySymbol;
import org.datanucleus.store.query.compiler.QueryCompilation;
import org.datanucleus.store.query.compiler.SymbolTable;
import org.datanucleus.store.query.expression.ClassExpression;

/**
 * Implementation of a Criteria Delete query.
 */
public class CriteriaDeleteImpl<T> implements CriteriaDelete<T>, Serializable
{
    static final long serialVersionUID = -4037442478473468674L;

    private CriteriaBuilderImpl cb;

    private RootImpl<T> from;
    private PredicateImpl filter;

    /** The JPQL single-string delete query that this equates to (cached). */
    String jpqlString = null;

    /** The generic query compilation that this equates to (cached). */
    QueryCompilation compilation = null;

    public CriteriaDeleteImpl(CriteriaBuilderImpl cb)
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
        throw new UnsupportedOperationException("CriteriaDelete.subquery not yet supported");
        // TODO Support this
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaDelete#from(java.lang.Class)
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
     * @see javax.persistence.criteria.CriteriaDelete#from(javax.persistence.metamodel.EntityType)
     */
    public Root<T> from(EntityType<T> type)
    {
        discardCompiled();
        from = new RootImpl<T>(cb, type);
        return from;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaDelete#getRoot()
     */
    public Root<T> getRoot()
    {
        return from;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaDelete#where(javax.persistence.criteria.Expression)
     */
    public CriteriaDelete<T> where(Expression<Boolean> expr)
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
     * @see javax.persistence.criteria.CriteriaDelete#where(javax.persistence.criteria.Predicate[])
     */
    public CriteriaDelete<T> where(Predicate... exprs)
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

            org.datanucleus.store.query.expression.Expression[] fromExprs = new org.datanucleus.store.query.expression.Expression[1];
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

            org.datanucleus.store.query.expression.Expression filterExpr = null;
            if (filter != null)
            {
                filterExpr = filter.getQueryExpression();
                if (filterExpr != null)
                {
                    filterExpr.bind(symtbl);
                }
            }

            compilation = new QueryCompilation(candidateClass, candidateAlias, symtbl, null,
                fromExprs, filterExpr, null, null, null, null);
            compilation.setQueryLanguage(QueryLanguage.JPQL.name());
        }

        // TODO Handle subqueries

        return compilation;
    }

    /**
     * Method to return a single-string representation of the criteria delete query in JPQL.
     * @return The single-string form
     */
    public String toString()
    {
        if (jpqlString == null)
        {
            // Generate the query string
            StringBuilder str = new StringBuilder();
            str.append("DELETE ");

            // FROM clause
            str.append("FROM ");
            str.append(from.toString(true));
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