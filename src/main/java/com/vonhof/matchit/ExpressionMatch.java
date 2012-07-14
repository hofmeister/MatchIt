package com.vonhof.matchit;

import java.util.Set;

/**
 *
 * @author Henrik Hofmeister <@vonhofdk>
 */
public class ExpressionMatch extends SimpleMatch {

    protected final Expression expression;
    protected String[] groupNames;
    protected String[] subExpressionIds;

    protected ExpressionMatch(Expression expression) {
        this.expression = expression;
    }

    public int start(String group) {
        int i = groupIndex(group);
        return start(i);
    }

    public int end(String group) {
        int i = groupIndex(group);
        return end(i);
    }

    public String group(String group) {
        int i = groupIndex(group);
        if (i < 0) {
            return null;
        }
        return group(i);
    }

    /**
     * Get a map of all group names and group indices - along with the value they represent.
     *
     * @return
     */
    public StringIntMap<String> groups() {
        return groups(0, groupCount());
    }

    /**
     * Get a map of all group names and group indices - along with the value they represent - between start and limit
     *
     * @param start
     * @param limit
     * @return
     */
    public StringIntMap<String> groups(int start, int limit) {
        StringIntMap<String> out = new StringIntMap<String>();

        for (int i = start; i < limit; i++) {
            String key = groupName(i);
            if (key != null) {
                out.put(key, group(i));
            }
            out.put(i - start, group(i));
        }



        return out;
    }

    public String subExpressionId(int i) {
        return subExpressionIds[i];
    }

    public String[] subExpressionIds() {
        return subExpressionIds;
    }

    public int subExpressionCount() {
        return arrayCount(subExpressionIds);
    }

    public int subExpressionIndex(String group) {
        return arrayLookup(group, subExpressionIds);
    }

    public int groupIndex(String group) {
        return arrayLookup(group, groupNames);
    }

    public String groupName(int i) {
        return groupNames[i];
    }

    public int namedGroupCount() {
        return arrayCount(groupNames);
    }

    private int arrayCount(String[] array) {
        int out = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                out++;
            }

        }
        return out;
    }

    private int arrayLookup(String key, String[] array) {
        int first = -1;
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null
                    && array[i].equals(key)) {
                if (first == -1) {
                    first = 1;
                }
                if (array[i] == null) {
                    continue;
                }
                return i;
            }

        }
        return first;
    }

    public ExpressionMatch subMatch(String expressionId) {
        int offset = subExpressionIndex(expressionId);
        //If not found of group where its located is empty - return null
        if (offset < 0 || group(offset) == null) {
            return null;
        }

        Set<Expression> expressions = expression.subExpressions.get(expressionId);

        //Find the sub expression that did match something (they are placed in a (<expr1>|<expr2>) )
        Expression subExpression = null;

        if (expressions.size() > 1) {
            offset++;
            for (Expression expr : expressions) {
                if (group(offset) != null) {
                    subExpression = expr;
                    break;
                }
                offset += expr.groupCount;
            }
        } else {
            subExpression = expressions.iterator().next();
        }
        //If no match was found - return null
        if (subExpression == null) {
            return null;
        }

        ExpressionMatch out = new ExpressionMatch(subExpression);

        //Copy the values from this match into the sub match
        int size = subExpression.groupCount;
        int limit = offset + size;
        out.groups = new String[size];
        out.groupNames = new String[size];
        out.subExpressionIds = new String[size];
        out.groupStart = new int[size];
        out.groupEnd = new int[size];

        for (int i = offset; i < limit; i++) {
            out.groups[i - offset] = group(i);
            out.groupStart[i - offset] = start(i);
            out.groupEnd[i - offset] = end(i);
            out.groupNames[i - offset] = groupName(i);
            out.subExpressionIds[i - offset] = subExpression.groupSubExpression.get(i - offset);
        }

        return out;
    }

    public Expression expression() {
        return expression;
    }
}
