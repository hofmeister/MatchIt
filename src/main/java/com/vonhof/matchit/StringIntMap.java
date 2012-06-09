package com.vonhof.matchit;

import java.util.HashMap;

/**
 *
 * @author Henrik Hofmeister <@vonhofdk>
 */
public class StringIntMap<T> extends HashMap<Object,T> {

    public StringIntMap(int size) {
        super(size);
    }

    public StringIntMap() {
        super();
    }

    public boolean containsKey(String o) {
        return super.containsKey(o);
    }
    
    public boolean containsKey(int o) {
        return super.containsKey(o);
    }
    
    public T get(String o) {
        return super.get(o);
    }
    public T get(int o) {
        return super.get(o);
    }
    
    public T put(String k, T v) {
        return super.put(k, v);
    }
    public T put(Integer k, T v) {
        return super.put(k, v);
    }

    public T remove(String o) {
        return super.remove(o);
    }
    
    public T remove(int o) {
        return super.remove(o);
    }

}
