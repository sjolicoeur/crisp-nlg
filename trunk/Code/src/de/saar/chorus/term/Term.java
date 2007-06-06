/*
 * @(#)Term.java created 11.05.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */

package de.saar.chorus.term;

import java.io.StringReader;
import java.util.Set;

import de.saar.chorus.term.parser.ParseException;
import de.saar.chorus.term.parser.TermParser;

public abstract class Term {
    public static Term parse(String string) {
        try {
            TermParser p = new TermParser(new StringReader(string));
            return p.term();
        } catch(ParseException e) {
            System.err.println(e);
            return null;
        }
    }
    
    public boolean isVariable() {
        return getType() == Type.VARIABLE;
    }
    
    public boolean isConstant() {
        return getType() == Type.CONSTANT;
    }
    
    public boolean isCompound() {
        return getType() == Type.COMPOUND;
    }
    
    public abstract Type getType();
    
    public abstract boolean hasSubterm(Term other);
    
    public abstract Substitution getUnifier(Term other);
    
    public boolean isUnifiableWith(Term other) {
        Substitution subst = getUnifier(other);
        
        return (subst != null) && subst.isValid();
    }
    
    public abstract Set<Variable> getVariables();
    
    public Term unify(Term other) {
        Substitution mgu = getUnifier(other);
        
        if( mgu == null ) {
            return null;
        } else {
            return mgu.apply(this);
        }
    }

    // TODO this is a hack
    public int hashCode() {
        return toString().hashCode();
    }
    
    public Substitution substFor(Variable x) {
        return new Substitution(x, this);
    }
    
    public Substitution substFor(String varname) {
        return substFor(new Variable(varname));
    }
    
    public abstract String toLispString();
    
    
    
    
    
    
    

}
