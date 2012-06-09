This library wraps JAVA Pattern's in an Expression class that is able to use sub expressions, function expressions and named groups.

It handles infinite nesting of everything.

#### Syntax:
```
${subExpressionId}         - Allows the reuse of common expressions like numbers, dates etc. These sub expressions must 
                             first be registered within an expression context
 
${functionId(args...)}     - Allows the use of special ExpressionFunction instances that can be used to generate large 
                             sets of data or complex patterns. Functions need also be registered within an expression
                             context. 
 
(capital_letters:[A-Z])    - Named groups within your regular expressions.
```

#### Example:
```java
//Initialize an expression context.
ExpressionContext ctxt = new ExpressionContext(); 

//Add Range() ExpressionFunction with an id of "range". Function takes 2-3 arguments: (int from,int to,int width)
ctxt.add("range",new ExpressionFunction.Range()); 

//Add sub expression with id "letters"
ctxt.add("letters","[A-Z]+"); 

//Add sub expression with id "numbers"
ctxt.add("numbers","[0-9]+"); 

//Compile the expression you need
Expression expr = ctxt.compile("${letters}_(num:${numbers})_${range(9,12,2)}");

//Create matcher for text
ExpressionMatcher matcher = expr.matcher("Does it match ABC_123_09 and XYZ_321_10 ?");

//Start matching
while(matcher.find()) {
    //Get contents of named group
    String number = matcher.group("num"); //123 and 321
    
    //Get entire match
    String match = matcher.group(); //ABC_123_09 and XYZ_321_10
}
```
