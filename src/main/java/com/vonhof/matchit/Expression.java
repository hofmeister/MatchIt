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
public final class Expression {

    private final Pattern EXPRESSION = Pattern.compile("(?uis)\\$\\{([A-Z][A-Z0-9_]*)(?:\\(([^\\)]*)\\))?\\}");
    private final Pattern GROUP_START = Pattern.compile("(?uis)(?<!\\\\)\\((?:\\?\\<([A-Z][A-Z0-9_]+)\\>|(?!\\?))");
    
    /**
     * The original string expression that this expression was created from.
     */
    private final String expression;
    
    /**
     * Maps groups to group names.
     */
    private final Map<Integer,String> groupNames = new LinkedHashMap<Integer,String>();
    
    /**
     * Map of all sub expressions in the expression (recursivly).
     */
    private final Map<String,Set<Expression>> subExpressions = new LinkedHashMap<String, Set<Expression>>();
    
    /**
     * Maps group numbers to sub expression ids.
     */
    private final Map<Integer,String> groupSubExpression = new LinkedHashMap<Integer, String>();
    
    /**
     * The expression context - contains all other expressions and functions 
     * that should be available to this expression.
     */
    private final ExpressionContext ctxt;
    
    /**
     * The total amount of capturing groups found in this expression and its sub expressions.
     */
    private int groupCount = 0;
    
    /**
     * The compiled pattern result from this expression.
     */
    private Pattern compiled;
    

    protected Expression(ExpressionContext ctxt,String expression) {
        this.expression = expression;
        this.ctxt = ctxt;
    }

    /**
     * Compile the expression. Must be done before using it (which is done automatically *when* using it)
     * @return 
     */
    protected Expression compile() {
        if (compiled == null) {
            String expr = compileSubExpressions(expression);
            expr = prepareNamedGroups(expr);
            
            compiled = Pattern.compile("(?uis)"+expr);
        }
        return this;
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
    
    
    
    
    public static class ExpressionMatch {
        protected final Expression expression;
        protected String[] groups;
        protected String[] groupNames;
        protected String[] subExpressionIds;
        protected int[] groupStart;
        protected int[] groupEnd;

        public ExpressionMatch(Expression baseExpression) {
            this.expression = baseExpression;
        }
        
        public int start() {
            return start(0);
        }
        
        public int start(int group) {
            return groupStart[group];
        }
        public int start(String group) {
            int i = groupIndex(group);
            return start(i);
        }
        
        
        public int end() {
            return end(0);
        }
        
        public int end(int group) {
            return groupEnd[group];
        }
        
        public int end(String group) {
            int i = groupIndex(group);
            return end(i);
        }
        
        public String group(String group) {
            int i = groupIndex(group);
            if (i < 0)
                return null;
            return group(i);
        }
        
        public String group() {
            return group(0);
        }
        
        public String group(int group) {
            if (group < 0 || group >= groups.length)
                return null;
            return groups[group];
        }
        
        public ExpressionMatch subMatch(String expressionId) {
            int offset = subExpressionIndex(expressionId);
            //If not found of group where its located is empty - return null
            if (offset < 0 || group(offset) == null)
                return null;
            
            Set<Expression> expressions = expression.subExpressions.get(expressionId);
            
            //Find the sub expression that did match something (they are placed in a (<expr1>|<expr2>) )
            Expression subExpression = null;
            
            if (expressions.size() > 1) {
                offset++;
                for(Expression expr:expressions) {
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
            if (subExpression == null) return null;
            
            ExpressionMatch out = new ExpressionMatch(subExpression);
            
            //Copy the values from this match into the sub match
            int size = subExpression.groupCount;
            int limit = offset+size;
            out.groups = new String[size];
            out.groupNames = new String[size];
            out.subExpressionIds = new String[size];
            out.groupStart = new int[size];
            out.groupEnd = new int[size];
            
            for(int i = offset; i < limit;i++) {
                out.groups[i-offset] = group(i);
                out.groupStart[i-offset] = start(i);
                out.groupEnd[i-offset] = end(i);
                out.groupNames[i-offset] = groupName(i);
                out.subExpressionIds[i-offset] = subExpression.groupSubExpression.get(i-offset);
            }
            
            return out;
        }
        
        public int groupCount() {
            return groups.length;
        }
        
        /**
         * Get a map of all group names and group indices - along with the value they represent.
         * @return 
         */
        public StringIntMap<String> groups() {
            return groups(0, groupCount());
        }
        
        /**
         * Get a map of all group names and group indices - along with the value they represent - between start and 
         * limit
         * @param start
         * @param limit
         * @return 
         */
        public StringIntMap<String> groups(int start,int limit) {
            StringIntMap<String> out = new StringIntMap<String>();
            
            for(int i = start; i < limit;i++) {
                String key = groupName(i);
                if (key != null) {
                    out.put(key,group(i));
                }
                out.put(i-start,group(i));
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
            for(int i = 0; i < array.length;i++) {
                if (array[i] != null)
                    out++;
                    
            }
            return out;
        }
        
        private int arrayLookup(String key,String[] array) {
            int first = -1;
            for(int i = 0; i < array.length;i++) {
                if (array[i] != null 
                        && array[i].equals(key)) {
                    if (first == -1)
                        first = 1;
                    if (array[i] == null)
                        continue;
                    return i;
                }
                    
            }
            return first;
        }

        public Expression expression() {
            return expression;
        }
    }
    
    public static final class ExpressionMatcher extends ExpressionMatch {
        private final Matcher m;
        
        public ExpressionMatcher(Matcher matcher,Expression baseExpression) {
            super(baseExpression);
            this.m = matcher;
        }    
        
        public boolean find() {
            if (m.find()) {
                readMatch(m,expression,expression.groupCount);
                return true;
            }
            return false;
        }
        
        protected void readMatch(Matcher m,Expression expr,int limit) {
            int size = limit;
            groups = new String[size];
            groupNames = new String[size];
            subExpressionIds = new String[size];
            groupStart = new int[size];
            groupEnd = new int[size];

            for(int i = 0; i < limit;i++) {
                groups[i] = m.group(i);
                groupStart[i] = m.start(i);
                groupEnd[i] = m.end(i);
                groupNames[i] = expr.groupNames.get(i);
                subExpressionIds[i] = expr.groupSubExpression.get(i);
            }
        }
        
        public boolean find(int offset) {
            return m.find(offset);
        }
    }
}
