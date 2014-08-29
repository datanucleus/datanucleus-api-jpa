/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.query.compiler.JPQLSymbolResolver;
import org.datanucleus.query.compiler.QueryCompilation;
import org.datanucleus.query.expression.ClassExpression;
import org.datanucleus.query.expression.CreatorExpression;
import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.OrderExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.SubqueryExpression;
import org.datanucleus.query.expression.VariableExpression;
import org.datanucleus.query.symbol.PropertySymbol;
import org.datanucleus.query.symbol.SymbolTable;

/**
 * Implementation of JPA2 Criteria "Query".
 * When the user calls getCompilation() or getParameters(), or toString() then that part of the query is compiled.
 * Any subsequent update will require the compilation to be redone.
 */
public class CriteriaQueryImpl<T> implements CriteriaQuery<T>, Serializable
{
    static final long serialVersionUID = -7894275881256415495L;

    private CriteriaBuilderImpl cb;

    private boolean distinct;
    private Class<T> resultClass;

    private List<Selection<?>> result;
    private List<RootImpl<?>> from;
    private PredicateImpl filter;
    private List<Expression<?>> grouping;
    private PredicateImpl having;
    private List<Order> ordering;

    private List<SubqueryImpl<?>> subqueries;

    /** The JPQL single-string query that this equates to (cached). */
    String jpqlString = null;

    /** The generic query compilation that this equates to (cached). */
    QueryCompilation compilation = null;

    /** Parameter definition for the compilation. */
    Set<ParameterExpression<?>> params = null;

    /**
     * Constructor for a criteria query for the supplied model and result class.
     * @param cb Criteria Builder
     * @param resultClass Result class (if any)
     */
    public CriteriaQueryImpl(CriteriaBuilderImpl cb, Class<T> resultClass)
    {
        this.cb = cb;
        this.resultClass = resultClass;
    }

    /**
     * Accessor for the model used by this query.
     * @return The model
     */
    public Metamodel getMetamodel()
    {
        return cb.getEntityManagerFactory().getMetamodel();
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#distinct(boolean)
     */
    public CriteriaQuery<T> distinct(boolean flag)
    {
        discardCompiled();
        this.distinct = flag;
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#isDistinct()
     */
    public boolean isDistinct()
    {
        return this.distinct;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#select(javax.persistence.criteria.Selection)
     */
    public CriteriaQuery<T> select(Selection<? extends T> select)
    {
        discardCompiled();
        result = new ArrayList<Selection<?>>();
        result.add(select);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#multiselect(java.util.List)
     */
    public CriteriaQuery<T> multiselect(List<Selection<?>> selects)
    {
        discardCompiled();
        if (selects == null || selects.size() == 0)
        {
            result = null;
            return this;
        }
        result = new ArrayList<Selection<?>>(selects);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#multiselect(javax.persistence.criteria.Selection<?>[])
     */
    public CriteriaQuery<T> multiselect(Selection<?>... selects)
    {
        discardCompiled();
        if (selects == null || selects.length == 0)
        {
            result = null;
            return this;
        }
        result = new ArrayList<Selection<?>>();
        for (int i=0;i<selects.length;i++)
        {
            result.add(selects[i]);
        }
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getSelection()
     */
    public Selection<T> getSelection()
    {
        return (result != null ? (Selection<T>) result.get(0) : null);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getResultType()
     */
    public Class<T> getResultType()
    {
        return resultClass;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#from(java.lang.Class)
     */
    public <X> Root<X> from(Class<X> cls)
    {
        discardCompiled();
        EntityType<X> entity = cb.getEntityManagerFactory().getMetamodel().entity(cls);
        if (entity == null)
        {
            throw new IllegalArgumentException("The specified class (" + cls.getName() + ") is not an entity");
        }

        return from(entity);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#from(javax.persistence.metamodel.EntityType)
     */
    public <X> Root<X> from(EntityType<X> type)
    {
        discardCompiled();
        if (from == null)
        {
            from = new ArrayList<RootImpl<?>>();
        }
        RootImpl root = new RootImpl<X>(cb, type);
        from.add(root);
        return root;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getRoots()
     */
    public Set<Root<?>> getRoots()
    {
        if (from == null)
        {
            return null;
        }
        return new HashSet<Root<?>>(from);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#where(javax.persistence.criteria.Expression)
     */
    public CriteriaQuery<T> where(Expression<Boolean> expr)
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
     * @see javax.persistence.criteria.CriteriaQuery#where(javax.persistence.criteria.Predicate[])
     */
    public CriteriaQuery<T> where(Predicate... exprs)
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

    public CriteriaQuery<T> where(List<Predicate> preds)
    {
        discardCompiled();
        if (preds == null || preds.size() == 0)
        {
            filter = null;
            return this;
        }

        if (filter == null)
        {
            filter = new PredicateImpl(cb);
        }
        for (Predicate pred : preds)
        {
            filter = filter.append(pred);
        }

        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getRestriction()
     */
    public Predicate getRestriction()
    {
        return filter;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#groupBy(javax.persistence.criteria.Expression<?>[])
     */
    public CriteriaQuery<T> groupBy(Expression<?>... exprs)
    {
        discardCompiled();
        if (exprs == null || exprs.length == 0)
        {
            grouping = null;
            return this;
        }
        grouping = new ArrayList<Expression<?>>();
        for (int i=0;i<exprs.length;i++)
        {
            grouping.add(exprs[i]);
        }
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#groupBy(java.util.List)
     */
    public CriteriaQuery<T> groupBy(List<Expression<?>> exprs)
    {
        discardCompiled();
        if (exprs == null || exprs.size() == 0)
        {
            grouping = null;
            return this;
        }
        grouping = new ArrayList<Expression<?>>();
        grouping.addAll(exprs);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getGroupList()
     */
    public List<Expression<?>> getGroupList()
    {
        if (grouping == null)
        {
            return null;
        }
        return new ArrayList<Expression<?>>(grouping);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#getGroupRestriction()
     */
    public Predicate getGroupRestriction()
    {
        return having;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#having(javax.persistence.criteria.Expression)
     */
    public CriteriaQuery<T> having(Expression<Boolean> expr)
    {
        discardCompiled();
        having = (PredicateImpl)expr;
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#having(javax.persistence.criteria.Predicate[])
     */
    public CriteriaQuery<T> having(Predicate... exprs)
    {
        discardCompiled();
        if (exprs == null)
        {
            having = null;
            return this;
        }
        if (having == null)
        {
            having = new PredicateImpl(cb);
        }

        for (int i=0;i<exprs.length;i++)
        {
            having = (PredicateImpl)exprs[i];
        }

        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#getOrderList()
     */
    public List<Order> getOrderList()
    {
        if (ordering == null)
        {
            return null;
        }
        return new ArrayList<Order>(ordering);
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#orderBy(java.util.List)
     */
    public CriteriaQuery<T> orderBy(List<Order> orders)
    {
        discardCompiled();
        if (orders == null || orders.size() == 0)
        {
            ordering = null;
            return this;
        }

        ordering = new ArrayList<Order>();
        ordering.addAll(orders);
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#orderBy(javax.persistence.criteria.Order[])
     */
    public CriteriaQuery<T> orderBy(Order... orders)
    {
        discardCompiled();
        if (orders == null || orders.length == 0)
        {
            ordering = null;
            return this;
        }

        ordering = new ArrayList<Order>();
        for (int i=0;i<orders.length;i++)
        {
            ordering.add(orders[i]);
        }
        return this;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.AbstractQuery#subquery(java.lang.Class)
     */
    public <U> Subquery<U> subquery(Class<U> type)
    {
        discardCompiled();
        if (subqueries == null)
        {
            subqueries = new ArrayList<SubqueryImpl<?>>();
        }
        SubqueryImpl<U> subquery = new SubqueryImpl<U>(cb, type, this);
        subqueries.add(subquery);
        return subquery;
    }

    protected void discardCompiled()
    {
        jpqlString = null;
        compilation = null;
        params = null;
    }

    /* (non-Javadoc)
     * @see javax.persistence.criteria.CriteriaQuery#getParameters()
     */
    public Set<ParameterExpression<?>> getParameters()
    {
        if (params != null)
        {
            return params;
        }

        List<org.datanucleus.query.expression.ParameterExpression> paramExprs = new ArrayList();
        if (result != null)
        {
            Iterator<Selection<?>> iter = result.iterator();
            while (iter.hasNext())
            {
                org.datanucleus.query.expression.Expression expr = ((ExpressionImpl)iter.next()).getQueryExpression();
                getParametersForQueryExpression(expr, paramExprs);
            }
        }
        if (filter != null)
        {
            getParametersForQueryExpression(filter.getQueryExpression(), paramExprs);
        }
        if (grouping != null)
        {
            Iterator<Expression<?>> iter = grouping.iterator();
            while (iter.hasNext())
            {
                org.datanucleus.query.expression.Expression expr = ((ExpressionImpl)iter.next()).getQueryExpression();
                getParametersForQueryExpression(expr, paramExprs);
            }
        }
        if (having != null)
        {
            getParametersForQueryExpression(having.getQueryExpression(), paramExprs);
        }

        if (paramExprs.isEmpty())
        {
            params = Collections.EMPTY_SET;
        }
        else
        {
            params = new HashSet<ParameterExpression<?>>();
            Iterator<org.datanucleus.query.expression.ParameterExpression> iter = paramExprs.iterator();
            while (iter.hasNext())
            {
                org.datanucleus.query.expression.ParameterExpression paramExpr = iter.next();
                ParameterExpressionImpl param = new ParameterExpressionImpl(cb, paramExpr.getType(), paramExpr.getId());
                params.add(param);
            }
        }

        return params;
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
            Class candidateClass = from.get(0).getJavaType();
            String candidateAlias = from.get(0).getAlias();
            if (candidateAlias == null)
            {
                candidateAlias = "DN_THIS";
                from.get(0).alias(candidateAlias);
            }

            SymbolTable symtbl = new SymbolTable();
            symtbl.setSymbolResolver(new JPQLSymbolResolver(mmgr, clr, symtbl, candidateClass, candidateAlias));
            symtbl.addSymbol(new PropertySymbol(candidateAlias, candidateClass));
            if (parentSymtbl != null)
            {
                symtbl.setParentSymbolTable(parentSymtbl);
            }

            org.datanucleus.query.expression.Expression[] resultExprs = null;
            if (result != null && !result.isEmpty())
            {
                resultExprs = new org.datanucleus.query.expression.Expression[result.size()];
                Iterator iter = result.iterator();
                int i=0;
                while (iter.hasNext())
                {
                    ExpressionImpl result = (ExpressionImpl)iter.next();
                    org.datanucleus.query.expression.Expression resultExpr = result.getQueryExpression();
                    resultExpr.bind(symtbl);
                    resultExprs[i++] = resultExpr;
                }

                if (resultExprs.length == 1 && resultExprs[0] instanceof PrimaryExpression)
                {
                    // Check for special case of "Object(p)" in result, which means no special result
                    String resultExprId = ((PrimaryExpression)resultExprs[0]).getId();
                    if (resultExprId.equalsIgnoreCase(candidateAlias))
                    {
                        resultExprs = null;
                    }
                }
            }

            org.datanucleus.query.expression.Expression[] fromExprs = 
                new org.datanucleus.query.expression.Expression[from.size()];
            Iterator iter = from.iterator();
            int i=0;
            while (iter.hasNext())
            {
                FromImpl frm = (FromImpl)iter.next();
                Set<JoinImpl> frmJoins = frm.getJoins();
                if (frmJoins != null && !frmJoins.isEmpty())
                {
                    Iterator<JoinImpl> frmJoinIter = frmJoins.iterator();
                    while (frmJoinIter.hasNext())
                    {
                        JoinImpl frmJoin = frmJoinIter.next();
                        if (frmJoin.getAlias() != null)
                        {
                            Class frmJoinCls = frmJoin.getType().getJavaType();
                            symtbl.addSymbol(new PropertySymbol(frmJoin.getAlias(), frmJoinCls));
                        }
                    }
                }

                ClassExpression clsExpr = (ClassExpression)frm.getQueryExpression(true);
                clsExpr.bind(symtbl);
                fromExprs[i++] = clsExpr;
            }

            org.datanucleus.query.expression.Expression filterExpr = null;
            if (filter != null)
            {
                filterExpr = filter.getQueryExpression();
                if (filterExpr != null)
                {
                    filterExpr.bind(symtbl);
                }
            }

            org.datanucleus.query.expression.Expression[] groupingExprs = null;
            if (grouping != null && !grouping.isEmpty())
            {
                groupingExprs = new org.datanucleus.query.expression.Expression[grouping.size()];
                Iterator grpIter = grouping.iterator();
                i=0;
                while (grpIter.hasNext())
                {
                    ExpressionImpl grp = (ExpressionImpl)grpIter.next();
                    org.datanucleus.query.expression.Expression groupingExpr = grp.getQueryExpression();
                    groupingExpr.bind(symtbl);
                    groupingExprs[i++] = groupingExpr;
                }
            }

            org.datanucleus.query.expression.Expression havingExpr = null;
            if (having != null)
            {
                havingExpr = having.getQueryExpression();
                havingExpr.bind(symtbl);
            }

            org.datanucleus.query.expression.Expression[] orderExprs = null;
            if (ordering != null && !ordering.isEmpty())
            {
                orderExprs = new org.datanucleus.query.expression.Expression[ordering.size()];
                Iterator<Order> orderIter = ordering.iterator();
                i=0;
                while (orderIter.hasNext())
                {
                    OrderImpl order = (OrderImpl)orderIter.next();
                    OrderExpression orderExpr = order.getQueryExpression();
                    orderExpr.bind(symtbl);
                    orderExprs[i++] = orderExpr;
                }
            }

            compilation = new QueryCompilation(candidateClass, candidateAlias, symtbl, resultExprs,
                fromExprs, filterExpr, groupingExprs, havingExpr, orderExprs, null);
            if (distinct)
            {
                compilation.setResultDistinct();
            }
            compilation.setQueryLanguage("JPQL");
        }

        if (subqueries != null && !subqueries.isEmpty())
        {
            Iterator<SubqueryImpl<?>> subqueryIter = subqueries.iterator();
            while (subqueryIter.hasNext())
            {
                SubqueryImpl sub = subqueryIter.next();
                org.datanucleus.query.expression.Expression subExpr = sub.getQueryExpression();
                if (subExpr instanceof SubqueryExpression)
                {
                    SubqueryExpression subqueryExpr = (SubqueryExpression) sub.getQueryExpression();
                    VariableExpression subqueryVar = (VariableExpression) subqueryExpr.getRight();
                    CriteriaQueryImpl<T> subDelegate = (CriteriaQueryImpl<T>) sub.getDelegate();
                    QueryCompilation subCompilation = subDelegate.getCompilation(mmgr, clr, compilation.getSymbolTable());
                    subCompilation.setQueryLanguage("JPQL");
                    compilation.addSubqueryCompilation(subqueryVar.getId(), subCompilation);
                }
                else if (subExpr instanceof VariableExpression)
                {
                    VariableExpression subVarExpr = (VariableExpression)subExpr;
                    CriteriaQueryImpl<T> subDelegate = (CriteriaQueryImpl<T>) sub.getDelegate();
                    QueryCompilation subCompilation = subDelegate.getCompilation(mmgr, clr, compilation.getSymbolTable());
                    subCompilation.setQueryLanguage("JPQL");
                    compilation.addSubqueryCompilation(subVarExpr.getId(), subCompilation);
                }
            }
        }

        return compilation;
    }

    /**
     * Method to return a single-string representation of the criteria query in JPQL.
     * @return The single-string form
     */
    public String toString()
    {
        if (jpqlString == null)
        {
            // Generate the query string
            StringBuilder str = new StringBuilder();
            str.append("SELECT ");
            if (distinct)
            {
                str.append("DISTINCT ");
            }

            if (result != null)
            {
                Iterator<Selection<?>> iter = result.iterator();
                while (iter.hasNext())
                {
                    Selection<?> select = iter.next();
                    str.append(select.toString());
                    if (iter.hasNext())
                    {
                        str.append(",");
                    }
                }
                str.append(" ");
            }

            // FROM clause
            str.append("FROM ");
            Iterator<RootImpl<?>> fromIter = from.iterator();
            while (fromIter.hasNext())
            {
                RootImpl<?> root = fromIter.next();
                str.append(root.toString(true));
                if (fromIter.hasNext())
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

            if (grouping != null && !grouping.isEmpty())
            {
                // GROUP BY clause
                str.append("GROUP BY ");
                Iterator<Expression<?>> iter = grouping.iterator();
                while (iter.hasNext())
                {
                    Expression<?> groupExpr = iter.next();
                    str.append(groupExpr.toString());
                    if (iter.hasNext())
                    {
                        str.append(",");
                    }
                }
                str.append(" ");
            }

            if (having != null)
            {
                // HAVING clause
                str.append("HAVING ");
                str.append(having.toString()).append(" ");
            }

            if (ordering != null && !ordering.isEmpty())
            {
                // ORDER BY clause
                str.append("ORDER BY ");
                Iterator<Order> iter = ordering.iterator();
                while (iter.hasNext())
                {
                    Order order = iter.next();
                    str.append(order.toString());
                    if (iter.hasNext())
                    {
                        str.append(",");
                    }
                }
                str.append(" ");
            }

            jpqlString = str.toString().trim();
        }
        return jpqlString;
    }

    protected void getParametersForQueryExpression(org.datanucleus.query.expression.Expression expr, List params)
    {
        if (expr == null)
        {
            return;
        }

        if (expr instanceof DyadicExpression)
        {
            getParametersForQueryExpression(expr.getLeft(), params);
            getParametersForQueryExpression(expr.getRight(), params);
        }
        else if (expr instanceof InvokeExpression)
        {
            InvokeExpression invokeExpr = (InvokeExpression)expr;
            getParametersForQueryExpression(invokeExpr.getLeft(), params);
            List<org.datanucleus.query.expression.Expression> args = invokeExpr.getArguments();
            if (args != null && !args.isEmpty())
            {
                Iterator<org.datanucleus.query.expression.Expression> iter = args.iterator();
                while (iter.hasNext())
                {
                    getParametersForQueryExpression(iter.next(), params);
                }
            }
        }
        else if (expr instanceof PrimaryExpression)
        {
            if (expr.getLeft() != null)
            {
                getParametersForQueryExpression(expr.getLeft(), params);
            }
        }
        else if (expr instanceof org.datanucleus.query.expression.ParameterExpression)
        {
            params.add(expr);
        }
        else if (expr instanceof CreatorExpression)
        {
            CreatorExpression createExpr = (CreatorExpression)expr;
            List<org.datanucleus.query.expression.Expression> args = createExpr.getArguments();
            if (args != null && !args.isEmpty())
            {
                Iterator<org.datanucleus.query.expression.Expression> iter = args.iterator();
                while (iter.hasNext())
                {
                    getParametersForQueryExpression(iter.next(), params);
                }
            }
        }
        else if (expr instanceof VariableExpression)
        {
        }
        else if (expr instanceof Literal)
        {
        }
    }
}