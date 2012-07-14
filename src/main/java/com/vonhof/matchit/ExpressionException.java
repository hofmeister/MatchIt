package com.vonhof.matchit;

/**
 *
 * @author Henrik Hofmeister <@vonhofdk>
 */
public final class ExpressionException extends RuntimeException {

    public ExpressionException(String msg) {
        super(msg);
    }

    public ExpressionException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }
    
}
