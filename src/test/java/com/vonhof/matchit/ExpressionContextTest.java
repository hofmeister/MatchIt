package com.vonhof.matchit;

import junit.framework.TestCase;

/**
 *
 * @author Henrik Hofmeister <@vonhofdk>
 */
public class ExpressionContextTest extends TestCase {
    
    public ExpressionContextTest(String testName) {
        super(testName);
    }
    
    public void test_can_compile_subexpressions() {
        ExpressionContext ctxt = new ExpressionContext();
        ctxt.add("letters","[A-Z]+");
        ctxt.add("numbers","[0-9]+");
        
        String pattern = ctxt.compile("${letters}_${numbers}").pattern();
        
        assertEquals("(?uis)([A-Z]+)_([0-9]+)", pattern);
    }
    
    public void test_can_compile_functions() {
        ExpressionContext ctxt = new ExpressionContext();
        ctxt.add("range",new ExpressionFunction.Range());
        ctxt.add("letters","[A-Z]+");
        ctxt.add("numbers","[0-9]+");
        
        String pattern = ctxt.compile("${letters}_${numbers}_${range(9,12,2)}").pattern();
        
        assertEquals("(?uis)([A-Z]+)_([0-9]+)_(?:09|10|11|12)", pattern);
    }
    
    public void test_can_match_groups() {
        ExpressionContext ctxt = new ExpressionContext();
        ctxt.add("range",new ExpressionFunction.Range());
        ctxt.add("letters","[A-Za-z]+");
        ctxt.add("numbers","[0-9]+");
        ctxt.add("year","${range(2000,2012)}");
        
        Expression expr = ctxt.compile("(year) (?<year>${year})");
        ExpressionMatcher matcher = expr.matcher("In the year 2010");
        assertTrue(matcher.find());
        assertEquals("2010", matcher.group("year"));
        assertEquals("year", matcher.group(1));
    }
    
    public void test_can_extract_nested_groups_serial() {
        ExpressionContext ctxt = new ExpressionContext();
        ctxt.add("a","(A)");
        ctxt.add("a","(a)");
        ctxt.add("b","(B)");
        ctxt.add("b","(b)");
        ctxt.add("ab","${a}${b}");
        
        
        Expression expr = ctxt.compile("${ab}");
        ExpressionMatcher matcher = expr.matcher("aB");
        assertTrue(matcher.find());
        
        ExpressionMatch abMatch = matcher.subMatch("ab");
        
        assertEquals("aB", abMatch.group(0));
        
        ExpressionMatch bMatch = abMatch.subMatch("b");
        
        assertEquals("B", bMatch.group(0));
    }
    
    public void test_can_extract_nested_groups() {
        ExpressionContext ctxt = new ExpressionContext();
        ctxt.add("a","A");
        ctxt.add("b","B");
        ctxt.add("c","C");
        ctxt.add("d","D");
        ctxt.add("ab","${a}${b}");
        ctxt.add("cd","${c}${d}");
        ctxt.add("abcd","${ab}${cd}");
        
        
        Expression expr = ctxt.compile("(d(?:o) (s((?:o)m)(e))) ${abcd} (p(l)e(?:as)e)");
        ExpressionMatcher matcher = expr.matcher("do some ABCD please");
        assertTrue(matcher.find());
        ExpressionMatch abcdGroups = matcher.subMatch("abcd");
        
        assertEquals("ABCD", abcdGroups.group(0));
        assertEquals("AB", abcdGroups.group(1));
        assertEquals("CD", abcdGroups.group(4));
    }
    
    public void test_can_extract_nested_named_groups() {
        ExpressionContext ctxt = new ExpressionContext();
        ctxt.add("a","(?<ANAME>A)");
        ctxt.add("b","(?<BNAME>B)");
        ctxt.add("c","(?<CNAME>C)");
        ctxt.add("d","(?<DNAME>D)");
        ctxt.add("ab","(?<ABNAME>${a}${b})");
        ctxt.add("cd","(?<CDNAME>${c}${d})");
        ctxt.add("abcd","${ab}${cd}");
        
        
        Expression expr = ctxt.compile("(d(?:o) (s((?:o)m)(e))) (?<ABCDNAME>${abcd}) (p(l)e(?:as)e)");
        ExpressionMatcher matcher = expr.matcher("do some ABCD please");
        assertTrue(matcher.find());
        
        ExpressionMatch abcdGroups = matcher.subMatch("abcd");
        
        assertEquals("ABCD", matcher.group("ABCDNAME"));
        assertEquals("AB", abcdGroups.group("ABNAME"));
    }
}
