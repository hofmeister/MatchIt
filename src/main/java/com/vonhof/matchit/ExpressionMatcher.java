package com.vonhof.matchit;

import java.util.regex.Matcher;

/**
 *
 * @author Henrik Hofmeister <@vonhofdk>
 */
public final class ExpressionMatcher extends ExpressionMatch {
    private final Matcher m;

    protected ExpressionMatcher(Matcher matcher, Expression baseExpression) {
        super(baseExpression);
        this.m = matcher;
    }

    public boolean find() {
        if (m.find()) {
            readMatch(m, expression, expression.groupCount);
            return true;
        }
        return false;
    }

    protected void readMatch(Matcher m, Expression expr, int limit) {
        int size = limit;
        groups = new String[size];
        groupNames = new String[size];
        subExpressionIds = new String[size];
        groupStart = new int[size];
        groupEnd = new int[size];
        for (int i = 0; i < limit; i++) {
            groups[i] = m.group(i);
            groupStart[i] = m.start(i);
            groupEnd[i] = m.end(i);
            groupNames[i] = expr.groupNames.get(i);
            subExpressionIds[i] = expr.groupSubExpression.get(i);
        }
    }

    public boolean find(int offset) {
        if (m.find(offset)) {
            readMatch(m, expression, expression.groupCount);
            return true;
        }
        return false;
    }

    public boolean lookingAt() {
        if (m.lookingAt()) {
            readMatch(m, expression, expression.groupCount);
            return true;
        } 
        return false;
    }
}
