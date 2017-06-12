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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.query.expression.CaseExpression;
import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.SubqueryExpression;
import org.datanucleus.query.expression.VariableExpression;
import org.datanucleus.query.expression.CaseExpression.ExpressionPair;

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
            StringBuilder str = new StringBuilder();

            if (dyExpr.getOperator() == Expression.OP_CAST)
            {
                str.append("TREAT(");
                str.append(JPQLHelper.getJPQLForExpression(left));
                str.append(" AS ");
                if (right == null)
                {
                    throw new NucleusUserException("Attempt to CAST but right argument is null");
                }
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
            return ":" + paramExpr.getId();
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
                StringBuilder str = new StringBuilder("LENGTH(");
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
                StringBuilder str = new StringBuilder();
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                str.append(" IS EMPTY");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("indexOf"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform LOCATE without any arguments");
                }
                StringBuilder str = new StringBuilder("LOCATE(");
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
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform SUBSTRING without any arguments");
                }
                StringBuilder str = new StringBuilder("SUBSTRING(");
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
                StringBuilder str = new StringBuilder("TRIM(BOTH ");

                str.append(JPQLHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
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
                StringBuilder str = new StringBuilder("TRIM(LEADING ");

                str.append(JPQLHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
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
                StringBuilder str = new StringBuilder("TRIM(TRAILING ");

                str.append(JPQLHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
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
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform LIKE without any arguments");
                }
                StringBuilder str = new StringBuilder();
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
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform MEMBER OF without any arguments");
                }
                StringBuilder str = new StringBuilder();
                Expression firstExpr = args.get(0);
                str.append(JPQLHelper.getJPQLForExpression(firstExpr));
                str.append(" MEMBER OF ");
                str.append(JPQLHelper.getJPQLForExpression(invoked));
                return str.toString();
            }
            else if (method.equalsIgnoreCase("COUNT"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform COUNT without any arguments");
                }
                Expression argExpr = args.get(0);
                if (argExpr instanceof DyadicExpression && ((DyadicExpression)argExpr).getOperator() == Expression.OP_DISTINCT)
                {
                    DyadicExpression dyExpr = (DyadicExpression)argExpr;
                    return "COUNT(DISTINCT " + JPQLHelper.getJPQLForExpression(dyExpr.getLeft()) + ")";
                }

                return "COUNT(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("COALESCE"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform COALESCE without any arguments");
                }
                StringBuilder str = new StringBuilder("COALESCE(");
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
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform NULLIF without any arguments");
                }
                StringBuilder str = new StringBuilder("NULLIF(");
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
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform ABS without any arguments");
                }
                Expression argExpr = args.get(0);
                return "ABS(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("AVG"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform AVG without any arguments");
                }
                Expression argExpr = args.get(0);
                return "AVG(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("MAX"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform MAX without any arguments");
                }
                Expression argExpr = args.get(0);
                return "MAX(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("MIN"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform MIN without any arguments");
                }
                Expression argExpr = args.get(0);
                return "MIN(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SQRT"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform SQRT without any arguments");
                }
                Expression argExpr = args.get(0);
                return "SQRT(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SUM"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform SUM without any arguments");
                }
                Expression argExpr = args.get(0);
                return "SUM(" + JPQLHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SQL_function"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform FUNCTION without any arguments");
                }
                StringBuilder str = new StringBuilder("FUNCTION(");
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
            else if (value instanceof Time)
            {
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                String timeStr = formatter.format((Time)value);
                return "{t '" + timeStr + "'}";
            }
            else if (value instanceof Date)
            {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String dateStr = formatter.format((Date)value);
                return "{d '" + dateStr + "'}";
            }
            else if (value instanceof Timestamp)
            {
                return "{ts '" + value.toString() + "'}";
            }
            else if (value instanceof java.util.Date)
            {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String datetimeStr = formatter.format((java.util.Date)value);
                return "{ts '" + datetimeStr + "'}";
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
        else if (expr instanceof CaseExpression)
        {
            CaseExpression caseExpr = (CaseExpression)expr;
            List<ExpressionPair> conds = caseExpr.getConditions();
            Expression elseExpr = caseExpr.getElseExpression();
            StringBuilder str = new StringBuilder("CASE ");
            if (conds != null)
            {
                for (ExpressionPair cond : conds)
                {
                    Expression whenExpr = cond.getWhenExpression();
                    Expression actionExpr = cond.getActionExpression();
                    str.append("WHEN ");
                    str.append(JPQLHelper.getJPQLForExpression(whenExpr));
                    str.append(" THEN ");
                    str.append(JPQLHelper.getJPQLForExpression(actionExpr));
                    str.append(" ");
                }
            }
            if (elseExpr != null)
            {
                str.append("ELSE ");
                str.append(JPQLHelper.getJPQLForExpression(elseExpr));
                str.append(" ");
            }
            str.append("END");
            return str.toString();
        }
        else
        {
            throw new UnsupportedOperationException("Dont currently support " + expr.getClass().getName() + " in JPQLHelper");
        }
    }
}