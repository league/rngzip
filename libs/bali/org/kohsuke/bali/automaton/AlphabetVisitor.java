package org.kohsuke.bali.automaton;

/**
 * Visitor design pattern for Alphabets.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface AlphabetVisitor {
    Object attribute( AttributeAlphabet alpha );
    Object nonExistentAttribute( NonExistentAttributeAlphabet alpha );
    Object element( ElementAlphabet alpha );
    Object interleave( InterleaveAlphabet alpha );
    Object list( ListAlphabet alpha );
    Object data( DataAlphabet alpha );
    Object value( ValueAlphabet alpha );
}
