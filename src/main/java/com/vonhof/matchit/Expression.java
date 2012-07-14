package com.vonhof.matchit;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expressions are regular expressions on steroids. 
 * 
 * In addition to the normal regexp syntax you get the following:
 * 
 * ${subExpression}         - Allows the reuse of common expressions like numbers, dates etc. These sub expressions must 
 *                            first be registered within an expression context
 * 
 * ${function(args...)}     - Allows the use of special ExpressionFunction instances that can be used to generate large 
 *                            sets of data or complex patterns. Functions need also be registered within an expression
 *                            context. 
 * 
 * (capital_letters:[A-Z])  - Named groups within your regular expressions.
 * 
 * For usage see the ExpressionContext class. (This class is equivalent to Pattern - without the public static 
 * methods in std. JAVA regex.)
 * 
 * @author Henrik Hofmeister <@vonhofdk>
 */
public class Expression {

    private static final Pattern EXPRESSION = Pattern.compile("(?uis)\\$\\{([A-Z][A-Z0-9_]*)(?:\\(([^\\)]*)\\))?\\}");
    private static final Pattern GROUP_START = Pattern.compile("(?uis)(?<!\\\\)\\((?:\\?\\<([A-Z][A-Z0-9_]+)\\>|(?!\\?))");
    
    /**
     * The original string expression that this expression was created from.
     */
    protected final String expression;
    
    /**
     * Maps groups to group names.
     */
    protected final Map<Integer,String> groupNames = new LinkedHashMap<Integer,String>();
    
    /**
     * Map of all sub expressions in the expression (recursivly).
     */
    protected final Map<String,Set<Expression>> subExpressions = new LinkedHashMap<String, Set<Expression>>();
    
    /**
     * Maps group numbers to sub expression ids.
     */
    protected final Map<Integer,String> groupSubExpression = new LinkedHashMap<Integer, String>();
    
    /**
     * The expression context - contains all other expressions and functions 
     * that should be available to this expression.
     */
    protected final ExpressionContext ctxt;
    
    /**
     * The total amount of capturing groups found in this expression and its sub expressions.
     */
    protected int groupCount = 0;
    
    /**
     * The compiled pattern result from this expression.
     */
    protected Pattern compiled;
    

    protected Expression(ExpressionContext ctxt,String expression) {
        this.expression = expression;
        this.ctxt = ctxt;
    }

    /**
     * Compile the expression. Must be done before using it (which is done automatically *when* using it)
     * @return 
     */
    private Expression compile() {
        if (compiled == null) {
            try {
                String expr = compileSubExpressions(expression);
            
                expr = prepareNamedGroups(expr);
            
                compiled = Pattern.compile("(?uis)"+expr);
            } catch(StackOverflowError ex) {
                //System.err.print("Failed to compile: "+expression);
            }
        }
        return this;
    }
    
    /**
     * Return context-less expression (only able to use named groups)
     * @param text
     * @return 
     */
    public static Expression compile(String text) {
        return new Expression(new ExpressionContext(),text);
    }

    /**
     * Returns the original string expression that this expression was created from.
     * @return 
     */
    public String expression() {
        return expression;
    }

    /**
     * Finds and sets up named group matching for this expression.
     * @param textExpression
     * @return 
     */
    private String prepareNamedGroups(String textExpression) {
        StringBuilder sb = new StringBuilder();
        
        int offset = 0;
        int group = 1;
        Matcher matcher = GROUP_START.matcher(textExpression);
        while (matcher.find()) {
            sb.append(textExpression.substring(offset, matcher.start()));
            String id = matcher.group(1);
            if (id != null) {
                groupNames.put(group,id);
            }
            group++;
            sb.append("(");
            offset = matcher.end();
        }
        
        groupCount = group;

        sb.append(textExpression.substring(offset));
        
        return sb.toString();
    }

    /**
     * Compile and preparse sub expression
     * @param textExpression
     * @return 
     */
    private String compileSubExpressions(String textExpression) {
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        Matcher matcher = EXPRESSION.matcher(textExpression);
        while (matcher.find()) {
            sb.append(textExpression.substring(offset, matcher.start()));
            String id = matcher.group(1);
            String args = matcher.group(2);
            if (args != null) {
                ExpressionFunction function = ctxt.getFunction(id);
                if (function == null) {
                    throw new ExpressionException(String.format("Missing function: %s", id));
                }
                
                String[] parts = args.split(",");
                sb.append(function.execute(parts));

            } else {
                Set<Expression> subExprs = ctxt.get(id);
                if (subExprs == null) {
                    throw new ExpressionException(String.format("Missing expression: %s", id));
                }
                if (!subExpressions.containsKey(id)) {
                    subExpressions.put(id, new HashSet<Expression>());
                }
                subExpressions.get(id).addAll(subExprs);
                
                String tmp = sb.toString();
                Matcher groupM = GROUP_START.matcher(tmp);
                int groupOffset = 1;//One for the one we're adding...
                while(groupM.find()) {
                    groupOffset++;
                    
                }
                
                groupSubExpression.put(groupOffset, id);
                
                boolean first = true;
                if (subExprs.size() > 1) {
                    sb.append("((");
                    groupOffset++;
                    for(Expression expr:subExprs) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(")|(");
                        }
                        //Substring it to avoid (?uis) in the beginning... A bit of a hack ...
                        sb.append(expr.compile().pattern().substring(6));
                        
                        for(Entry<Integer,String> entry:expr.groupNames.entrySet()) {
                            int subNameOffset = entry.getKey()+groupOffset;
                            groupNames.put(subNameOffset,entry.getValue());
                        }
                        
                        groupOffset += expr.groupCount;
                    }
                    sb.append("))");
                } else {
                    sb.append("(");
                    for(Expression expr:subExprs) {
                        //Substring it to avoid (?uis) in the beginning... A bit of a hack ...
                        sb.append(expr.compile().pattern().substring(6));
                        
                        for(Entry<Integer,String> entry:expr.groupNames.entrySet()) {
                            int subNameOffset = entry.getKey()+groupOffset;
                            groupNames.put(subNameOffset,entry.getValue());
                        }
                    }
                    sb.append(")");
                }
                
            }
            offset = matcher.end();
        }

        sb.append(textExpression.substring(offset));
        return sb.toString();
    }
    /**
     * Get the sub expressions found in this expression (recursivly)
     * @return 
     */
    public Map<String, Set<Expression>> getSubExpressions() {
        return subExpressions;
    }

    /**
     * Get the resulting regular expression pattern
     * @return 
     */
    protected String pattern() {
        compile();
        return compiled.pattern();
    }
    
    /**
     * Create a new matcher to look for this expression in the supplied text.
     * @param text
     * @return 
     */
    public ExpressionMatcher matcher(String text) {
        compile();
        Matcher m = compiled.matcher(text);
        return new ExpressionMatcher(m, this);
    }

    @Override
    public String toString() {
        return expression;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Expression other = (Expression) obj;
        if ((this.expression == null) ? (other.expression != null) : !this.expression.equals(other.expression)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + (this.expression != null ? this.expression.hashCode() : 0);
        return hash;
    }
}
