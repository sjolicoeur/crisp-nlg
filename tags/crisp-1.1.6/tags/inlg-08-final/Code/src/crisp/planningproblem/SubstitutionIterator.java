/*
 * @(#)SubstitutionIterator.java created 01.10.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;


public class SubstitutionIterator implements Iterator<Substitution> {
    //private Map<String,String> universe;
    //private TypeHierarchy types;
    private final TypedVariableList variables;
    private final List<List<String>> valuesForVariables;
    private final int[] index; // each in 0 .. vfv[i].size()-1
    private final int length;
    private boolean finished;

    public SubstitutionIterator(TypedVariableList variables, Map<String, String> universe, TypeHierarchy types) {
        this.variables = variables;
        //this.types = types;
        //this.universe = universe;

        length = variables.size();

        valuesForVariables = new ArrayList<List<String>>(length);
        for( int i = 0; i < length; i++ ) {
            List<String> thisUniverse = new ArrayList<String>();

            for( String individual : universe.keySet() ) {
                if( types.isSubtypeOf(universe.get(individual), variables.getType(variables.get(i))) ) {
                    thisUniverse.add(individual);
                }
            }

            valuesForVariables.add(thisUniverse);
        }

        index = new int[length];
        // all initialised with 0

        finished = false;
    }

    public boolean hasNext() {
        return !finished;
    }

    public Substitution next() {
        Substitution ret = new Substitution();
        int carry = 0;

        for( int i = length - 1; i >= 0; i-- ) {
            ret.addSubstitution(variables.get(i),
                    new Constant(valuesForVariables.get(i).get(index[i])));

            if( i == length - 1 ) {
                index[i]++;
            } else {
                index[i] += carry;
            }

            if( index[i] > valuesForVariables.get(i).size() - 1 ) {
                index[i] = 0;
                carry = 1;
            } else {
                carry = 0;
            }
        }

        if( carry == 1 ) {
            finished = true;
        }

        return ret;
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
