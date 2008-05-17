/*
 * @(#)Substitution.java created 11.05.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package de.saar.chorus.term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


public class Substitution implements Cloneable {
    // subst maps variables to terms. INVARIANT: Variables on the LHS
    // of any mapping never occur on the RHS of any mapping.
    private final Map<Variable,Term> subst;

    private boolean valid;

    public Substitution() {
        subst = new HashMap<Variable,Term>();
        valid = true;
    }

    public Substitution(Variable v, Term t) {
        subst = new HashMap<Variable,Term>();
        valid = true;
        subst.put(v,t);
    }

    public boolean isValid() {
        return valid;
    }

    public void addSubstitution(Variable v, Term t) {
        copy(concatenate(new Substitution(v,t)), this);
    }

    /**
     * Sets the substitution for v to t.  Use this method with care:
     * It has the potential for destroying the internal invariant that
     * variables on the LHS of substitutions don't occur on the RHS.  However,
     * it is correct for ground substitutions, and much more efficient than
     * addSubstitution in this case.
     *
     * @param v
     * @param t
     */
    public void setSubstitution(Variable v, Term t) {
        subst.put(v, t);
    }


    public Term apply(Term t) {
        switch(t.getType()) {
        case VARIABLE:
            Variable v = (Variable) t;
            if( subst.containsKey(v) ) {
                return subst.get(v);
            } else {
                return t;
            }

        case CONSTANT:
            return t;

        case COMPOUND:
            Compound com = (Compound) t;
            List<Term> newSubterms = new ArrayList<Term>(com.getSubterms().size());

            for( Term subterm : com.getSubterms() ) {
                newSubterms.add(apply(subterm));
            }

            return new Compound(com.getLabel(), newSubterms);
        }

        // unreachable
        return null;
    }

    public List<Term> apply(List<Term> terms) {
        List<Term> ret = new ArrayList<Term>(terms.size());

        for( Term term : terms ) {
            ret.add(apply(term));
        }

        return ret;
    }


    public Substitution concatenate(Substitution other) {
        Substitution ret = (Substitution) clone();
        Queue<Map.Entry<Variable,Term>> addQueue = new LinkedList<Map.Entry<Variable,Term>>(other.subst.entrySet());

        // concatenation inherits invalidity from both sides
        if( !isValid() || !other.isValid() ) {
            ret.valid = false;
            return ret;
        }

        while( !addQueue.isEmpty() ) {
            Map.Entry<Variable,Term> el = addQueue.remove();
            Variable v = el.getKey();
            Term t = el.getValue();

            // 1. apply substitution to t to eliminate variables that occur on
            // the LHS of the substitution
            t = ret.apply(t);

            if( t.hasSubterm(v)) {
                ret.valid = false;
                break;
            } else {
                if( ret.subst.containsKey(v)) {
                    // 2a. If the LHS occurs in the substitution, then unify
                    // the RHSs. It isn't necessary to apply (v -> unified) to
                    // any other mapping in the substitution because v can't
                    // occur both on the LHS and the RHS.
                    Substitution unifier = t.getUnifier(ret.subst.get(v));

                    if( unifier != null ) {
                        Term unified = unifier.apply(t);

                        if( !unified.hasSubterm(v) ) {
                            ret.subst.put(v, unified);

                            for( Map.Entry<Variable,Term> pair : unifier.subst.entrySet() ) {
                                addQueue.offer(pair);
                            }
                        } else {
                            ret.valid = false;
                            break;
                        }
                    } else {
                        ret.valid = false;
                        break;
                    }
                } else {
                    // 2b. If the LHS doesn't occur in the substitution, then
                    // apply (v -> t) to all RHSs (to eliminate v) and add
                    // (v -> t) to the substitution.
                    Substitution newSubst = new Substitution(v,t);

                    for( Map.Entry<Variable,Term> entry : ret.subst.entrySet() ) {
                        ret.subst.put(entry.getKey(), newSubst.apply(entry.getValue()));
                    }

                    ret.subst.put(v,t);
                }
            }

       }

        return ret;
    }

    @Override
    public String toString() {
        if( valid ) {
            return subst.toString();
        } else {
            return "(INVALID)";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Substitution) {
            Substitution substi = (Substitution) o;

            return (substi.valid == valid)
            && substi.subst.equals(subst);
        } else {
            return false;
        }
    }

    @Override
    public Object clone()  {
        Substitution ret = new Substitution();

        copy(this,ret);

        return ret;
    }

    private static void copy(Substitution from, Substitution to) {
        to.valid = from.valid;

        to.subst.clear();
        to.subst.putAll(from.subst);
    }


    public void remove(Variable v) {
        subst.remove(v);
    }

    public boolean appliesTo(Variable v) {
        return subst.containsKey(v);
    }




}
