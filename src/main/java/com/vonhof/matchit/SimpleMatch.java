package com.vonhof.matchit;

import com.vonhof.matchit.StringIntMap;

/**
 *
 * @author Henrik Hofmeister <@vonhofdk>
 */
public class SimpleMatch {
    
    protected String[] groups;
    protected int[] groupStart;
    protected int[] groupEnd;

    public SimpleMatch() {
        
    }
    
    public SimpleMatch(java.util.regex.Matcher m) {
        int size = m.groupCount();
        groups = new String[size];
        groupStart = new int[size];
        groupEnd = new int[size];

        for(int i = 0; i < size;i++) {
            groups[i] = m.group(i);
            groupStart[i] = m.start(i);
            groupEnd[i] = m.end(i);
        }
    }

    
    public int start() {
        return start(0);
    }

    public int start(int group) {
        return groupStart[group];
    }

    public int end() {
        return end(0);
    }

    public int end(int group) {
        return groupEnd[group];
    }

    public String group() {
        return group(0);
    }

    public String group(int group) {
        if (group < 0 || group >= groups.length)
            return null;
        return groups[group];
    }

    public int groupCount() {
        return groups.length;
    }
}
