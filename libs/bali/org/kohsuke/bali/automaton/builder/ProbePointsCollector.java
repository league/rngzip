package org.kohsuke.bali.automaton.builder;

import java.util.HashSet;
import java.util.Set;

import com.sun.msv.grammar.AnyNameClass;
import com.sun.msv.grammar.ChoiceNameClass;
import com.sun.msv.grammar.DifferenceNameClass;
import com.sun.msv.grammar.NameClassVisitor;
import com.sun.msv.grammar.NamespaceNameClass;
import com.sun.msv.grammar.NotNameClass;
import com.sun.msv.grammar.SimpleNameClass;
import com.sun.msv.util.StringPair;

/**
 * Accumulates probe points from name classes.
 * 
 * <p>
 * A probe point is a pair of (namespaceUri,localName) that is
 * recognized by the grammar.
 * 
 * <p>
 * To collect probe points from a name class <code>nc</code>,
 * do <code>nc.visit(aProbePointCollectorObject)</code>.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ProbePointsCollector implements NameClassVisitor {
    
    /** all collected probe points. */
    public final Set probePoints = new HashSet();

    public Object onChoice(ChoiceNameClass nc) {
        nc.nc1.visit(this);
        nc.nc2.visit(this);
        return null;
    }

    public Object onAnyName(AnyNameClass nc) {
        probePoints.add( new StringPair( NameClassEncoder.IMPOSSIBLE, NameClassEncoder.IMPOSSIBLE ) );
        return null;
    }

    public Object onSimple(SimpleNameClass nc) {
        probePoints.add( new StringPair( nc.namespaceURI, nc.localName ) );
        return null;
    }

    public Object onNsName(NamespaceNameClass nc) {
        probePoints.add( new StringPair( nc.namespaceURI, NameClassEncoder.IMPOSSIBLE ) );
        return null;
    }

    public Object onNot(NotNameClass nc) {
        probePoints.add( new StringPair( NameClassEncoder.IMPOSSIBLE, NameClassEncoder.IMPOSSIBLE ) );
        return nc.child.visit(this);
    }

    public Object onDifference(DifferenceNameClass nc) {
        nc.nc1.visit(this);
        nc.nc2.visit(this);
        return null;
    }

}
