/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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

import java.util.List;

import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.SubqueryExpression;
import org.datanucleus.query.expression.VariableExpression;

/**
 * Helper class that assists in generating JPQL from "org.datanucleus.query.expression" expressions.
 * TODO Drop this and use JPQLQueryHelper from "core".
 */
public class JPQLHelper
{
    public static String getJPQLForExpression(Expression expr)
    {
        if (expr instanceof DyadicExpression)
        {
            DyadicExpression dyExpr = (DyadicExpression)expr;
            Expression left = dyExpr.getLeft();
            Expression right = dyExpr.getRight();
            StringBuffer str = new StringBuffer();

            if (dyExpr.getOperator() == Expression.OP_CAST)
            {
                str.append("TREAT(");
                str.append(JPQLHelper.getJPQLForExpression(left));
                str.append(" AS ");
                str.append(((Literal)right).getLiteral());
                str.append(")");
                return str.toString();
            }

            str.append("(");
            if (left != null)
            {
                str.append(JPQLHelper.getJPQLForExpression(left));
            }

            // Special cases
            if (right != null && right instanceof Literal && ((Literal)right).getLiteral() == null &&
                (dyExpr.getOperator() == Expression.OP_EQ || dyExpr.getOperator() == Expression.OP_NOTEQ))
            {
                str.append(dyExpr.getOperator() == Expression.OP_EQ ? " IS NULL" : " IS NOT NULL");
            }
            else
            {
                if (dyExpr.getOperator() == Expression.OP_AND)
                {
                    str.append(" AND ");
                }
                else if (dyExpr.getOperator() == Expression.OP_OR)
                {
                    str.append(" OR ");
                }
                else if (dyExpr.getOperator() == Expression.OP_ADD)
                {
                    str.append(" + ");
                }
                else if (dyExpr.getOperator() == Expression.OP_SUB)
                {
                    str.append(" - ");
                }
                else if (dyExpr.getOperator() == Expression.OP_MUL)
                {
                    str.append(" * ");
                }
                else if (dyExpr.getOperator() == Expression.OP_DIV)
                {
                    str.append(" / ");
                }
                else if (dyExpr.getOperator() == Expression.OP_EQ)
                {
                    str.append(" = ");
                }
                else if (dyExpr.getOperator() == Expression.OP_GT)
                {
                    str.append(" > ");
                }
                else if (dyExpr.getOperator() == Expression.OP_LT)
                {
                    str.append(" < ");
                }
                else if (dyExpr.getOperator() == Expression.OP_GTEQ)
                {
                    str.append(" >= ");
                }
                else if (dyExpr.getOperator() == Expression.OP_LTEQ)
                {
                    str.append(" <= ");
                }
                else if (dyExpr.getOperator() == Expression.OP_NOTEQ)
                {
                    str.append(" <> ");
                }
                else
                {
                    // TODO Support other operators
                    throw new UnsupportedOperationException("Dont currently support operator " + dyExpr.getOperator() + " in JPQL conversion");
                }

                if (right != null)
                {
                    str.append(JPQLHelper.getJPQLForExpression(right));
                }
            }
            str.append(")");
            return str.toString();
        }
        else if (expr instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)expr;
            Expression exprLeft = expr.getLeft();
            if (exprLeft != null)
            {
                return JPQLHelper.getJPQLForExpression(exprLeft) + "." + primExpr.getId();
            }
            return primExpr.getId();
        }
        else if (expr instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)expr;
            if (paramExpr.getPosition() >= 0)
            {
                return "?" + paramExpr.getPosition();
            }
            else
            {
                return ":" + paramExpr.getId();
            }
        }
        else if (expr instanceof InvokeExpression)
        {
            InvokeExpression invExpr = (InvokeExpression)expr;
            Expression invoked = invExpr.getLeft();
            List<Expression> args = invExpr.getArguments();
            String method = invExpr.getOperation();
            if (method.equalsIgnoreCase("CURRENT_DATE"))
            {
                return "CURRENT_DATE";
            }
            else if (method.equalsIgnoreCase("CURRENT_TIME"))
            {
                return "CURRENT_TIME";
            }
            else if (method.equalsIgnoreCase("CURRENT_TIMESTAMP"))
            {
                return "CURRENT_TIMESTAMP";
            }
            else if (method.equalsIgnoreCase("length"))
            {
                StringBuffer str = new StringBuffer("LENGTH(");
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
                {
                    Expression firstExpr = args.get(0);
                    str.append(",").append(JPQLHelper.getJPQLForExpression(firstExpr));
                    if (args.size() == 2)
                    {
                        Expression secondExpr = args.get(1);
                        str.append(",").append(JPQLHelper.getJPQLForExpression(secondExpr));
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equals("toLowerCase"))
            {
                return "LOWER(" + JPQLHelper.getJPQLForExpression(invoked) + ")";
            }
            else if (method.equals("toUpperCase"))
            {
                return "UPPER(" + JPQLHelper.getJPQLForExpression(invoked) + ")";
            }
            else if (method.equalsIgnoreCase("isEmpty"))
            {
                StringBuffer str = new StringBuffer();
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                str.append(" IS EMPTY");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("indexOf"))
            {
                StringBuffer str = new StringBuffer("LOCATE(");
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                Expression firstExpr = args.get(0);
                str.append(",").append(JPQLHelper.getJPQLForExpression(firstExpr));
                if (args.size() > 1)
                {
                    Expression secondExpr = args.get(1);
                    str.append(",").append(JPQLHelper.getJPQLForExpression(secondExpr));
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("substring"))
            {
                StringBuffer str = new StringBuffer("SUBSTRING(");
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                Expression firstExpr = args.get(0);
                str.append(",").append(JPQLHelper.getJPQLForExpression(firstExpr));
                if (args.size() > 1)
                {
                    Expression secondExpr = args.get(1);
                    str.append(",").append(JPQLHelper.getJPQLForExpression(secondExpr));
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("trim"))
            {
                StringBuffer str = new StringBuffer("TRIM(BOTH ");

                str.append(JPQLHelper.getJPQLForExpression(invoked));
                if (args.size() > 0)
                {
                    Expression trimChrExpr = args.get(0);
                    str.append(JPQLHelper.getJPQLForExpression(trimChrExpr));
                }

                str.append(" FROM ");
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("trimLeft"))
            {
                StringBuffer str = new StringBuffer("TRIM(LEADING ");

                str.append(JPQLHelper.getJPQLForExpression(invoked));
                if (args.size() > 0)
                {
                    Expression trimChrExpr = args.get(0);
                    str.append(JPQLHelper.getJPQLForExpression(trimChrExpr));
                }

                str.append(" FROM ");
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("trimLeft"))
            {
                StringBuffer str = new StringBuffer("TRIM(TRAILING ");

                str.append(JPQLHelper.getJPQLForExpression(invoked));
                if (args.size() > 0)
                {
                    Expression trimChrExpr = args.get(0);
                    str.append(JPQLHelper.getJPQLForExpression(trimChrExpr));
                }

                str.append(" FROM ");
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("matches"))
            {
                StringBuffer str = new StringBuffer();
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                str.append(" LIKE ");
                Expression firstExpr = args.get(0);
                str.append(JPQLHelper.getJPQLForExpression(firstExpr));
                if (args.size() > 1)
                {
                    Expression secondExpr = args.get(1);
                    str.append(" ESCAPE ").append(JPQLHelper.getJPQLForExpression(secondExpr));
                }
                return str.toString();
            }
            else if (method.equalsIgnoreCase("contains"))
            {
                StringBuffer str = new StringBuffer();
                Expression firstExpr = args.get(0);
                str.append(JPQLHelper.getJPQLForExpression(firstExpr));
                str.append(" MEMBER OF ");
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                return str.toString();
            }
            else if (method.equalsIgnoreCase("COUNT"))
            {
                Expression argExpr = args.get(0);
                if (argExpr instanceof DyadicExpression && ((DyadicExpression)argExpr).getOperator() == Expression.OP_DISTINCT)
                {
                    DyadicExpression dyExpr = (DyadicExpression)argExpr;
                    return "COUNT(DISTINCT " + JPQLHelper.getJPQLForExpression(dyExpr.getLeft()) + ")";
                }
                else
                {
                    return "COUNT(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
                }
            }
            else if (method.equalsIgnoreCase("COALESCE"))
            {
                StringBuffer str = new StringBuffer("COALESCE(");
                for (int i=0;i<args.size();i++)
                {
                    Expression argExpr = args.get(i);
                    str.append(JPQLHelper.getJPQLForExpression(argExpr));
                    if (i < args.size()-1)
                    {
                        str.append(",");
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("NULLIF"))
            {
                StringBuffer str = new StringBuffer("NULLIF(");
                for (int i=0;i<args.size();i++)
                {
                    Expression argExpr = args.get(i);
                    str.append(JPQLHelper.getJPQLForExpression(argExpr));
                    if (i < args.size()-1)
                    {
                        str.append(",");
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("ABS"))
            {
                Expression argExpr = args.get(0);
                return "ABS(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("AVG"))
            {
                Expression argExpr = args.get(0);
                return "AVG(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("MAX"))
            {
                Expression argExpr = args.get(0);
                return "MAX(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("MIN"))
            {
                Expression argExpr = args.get(0);
                return "MIN(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SQRT"))
            {
                Expression argExpr = args.get(0);
                return "SQRT(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SUM"))
            {
                Expression argExpr = args.get(0);
                return "SUM(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SQL_function"))
            {
                StringBuffer str = new StringBuffer("FUNCTION(");
                for (int i=0;i<args.size();i++)
                {
                    Expression argExpr = args.get(i);
                    str.append(JPQLHelper.getJPQLForExpression(argExpr));
                    if (i < args.size()-1)
                    {
                        str.append(",");
                    }
                }
                str.append(")");
                return str.toString();
            }
            // TODO Support this
            throw new UnsupportedOperationException("Dont currently support InvokeExpression (" + invExpr + ") conversion into JPQL");
        }
        else if (expr instanceof Literal)
        {
            Literal litExpr = (Literal)expr;
            Object value = litExpr.getLiteral();
            if (value instanceof String || value instanceof Character)
            {
                return "'" + value.toString() + "'";
            }
            else if (value instanceof Boolean)
            {
                return ((Boolean)value ? "TRUE" : "FALSE");
            }
            else
            {
                return litExpr.getLiteral().toString();
            }
        }
        else if (expr instanceof VariableExpression)
        {
            VariableExpression varExpr = (VariableExpression)expr;
            return varExpr.getId();
        }
        else if (expr instanceof SubqueryExpression)
        {
            SubqueryExpression subqExpr = (SubqueryExpression)expr;
            return subqExpr.getKeyword() + " " + JPQLHelper.getJPQLForExpression(subqExpr.getRight());
        }
        else
        {
            throw new UnsupportedOperationException("Dont currently support " + expr.getClass().getName() + " in JPQLHelper");
        }
    }
}