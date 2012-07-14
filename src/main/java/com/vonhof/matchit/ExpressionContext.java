package com.vonhof.matchit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Used to define and store expressions. The context must include all sub expressions and expression functions that
 * you'll use in subsequent expressions.
 * 
 * 
 * @author Henrik Hofmeister <@vonhofdk>
 */
public class ExpressionContext {
    private final Map<String,Set<Expression>> expressions = new HashMap<String, Set<Expression>>();
    private final Map<String,ExpressionFunction> functions = new HashMap<String, ExpressionFunction>();
   
    /**
     * Adds sub expressions with id. See Expression for syntax
     * @param id
     * @param expression
     * @return 
     */
    public Expression add(String id,String expression) {
        Expression out = new Expression(this,expression);
        if (!expressions.containsKey(id))
            expressions.put(id,new HashSet<Expression>());
        expressions.get(id).add(out);
        return out;
    }
    
    /**
     * Add expressions function with id. See Expression for syntax
     * @param id
     * @param function 
     */
    public void add(String id,ExpressionFunction function) {
        functions.put(id, function);
    }
    
    public Set<Expression> get(String id) {
        return expressions.get(id);
    }
    
    protected ExpressionFunction getFunction(String id) {
        return functions.get(id);
    }
    
    public Expression compile(String expression) {
        return new Expression(this,expression);
    }
}
