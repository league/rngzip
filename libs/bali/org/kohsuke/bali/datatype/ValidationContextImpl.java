package org.kohsuke.bali.datatype;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.relaxng.datatype.ValidationContext;

/**
 * {@link ValidationContext} implementation that monitors queries
 * from the datatype.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class ValidationContextImpl implements ValidationContext {
    ValidationContextImpl( ValidationContext _core ) {
        this.core = _core;
    }
    
    private final ValidationContext core;

    /** Prefixes and namespace URIs that are queried by the datatype. */
    private final HashMap queriedNamespaces = new HashMap();
    
    /** Resets the accumulated monitored values. */
    public void reset() {
        queriedNamespaces.clear();
    }
    
    /** Gets all the queried prefix/namespace URIs in a single array. */
    public String[] getQueriedNamespaces() {
        String[] result = new String[queriedNamespaces.size()*2];

        int i=0;        
        for( Iterator itr = queriedNamespaces.entrySet().iterator(); itr.hasNext() ;) {
            Map.Entry e = (Map.Entry)itr.next();
            result[i+0] = (String)e.getKey();
            result[i+1] = (String)e.getValue();
        }
        
        return result;
    }
    
    public String resolveNamespacePrefix(String prefix) {
        String uri = core.resolveNamespacePrefix(prefix);
        queriedNamespaces.put(prefix,uri);    
        return uri;
    }

    public String getBaseUri() {
        return null;
    }

    public boolean isUnparsedEntity(String name) {
        return true;
    }

    public boolean isNotation(String name) {
        return true;
    }

}
