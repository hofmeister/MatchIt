package com.vonhof.matchit;

/**
 *
 * @author Henrik Hofmeister <@vonhofdk>
 */
public interface ExpressionFunction {
    public String execute(String[] args);
    
    
    public static class Range implements ExpressionFunction {

        public String execute(String[] args) {
            if (args.length < 2)
                throw new ExpressionException("Range function requires atleast 2 arguments");
            int from = Integer.valueOf(args[0]);
            int to = Integer.valueOf(args[1]);
            int width = 1;
            if (args.length > 2) {
                width = Integer.valueOf(args[2]);
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("(?:");
            for(int i = from; i <= to;i++) {
                String num = String.valueOf(i);
                for(int x = 0; x < width-num.length();x++) {
                    sb.append("0");
                }
                sb.append(num);
                if (i != to)
                    sb.append("|");
            }
            
            sb.append(")");
            return sb.toString();
        }
    
    }
}
