/*
 * @(#)Predicate.java created 30.09.2006
 *
 * Copyright (c) 2006 Alexander Koller
 *
 */

package crisp.planningproblem;

import java.util.ArrayList;
import java.util.List;

import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

/*
 * TODO:
 * Ich glaube, dass der einzige Unterschied zwischen Termen und Praedikaten ist,
 * dass die Parameter von Praedikaten typisiert sind. Man koennte Predicate durch
 * eine neue Klasse "TypedTerm" ersetzen, die von Compound abgeleitet ist und
 * eine Liste "ParameterTypes" enthaelt.
 */

public class Predicate {
    private String label;
    private final TypedVariableList variables;

    public Predicate() {
        variables = new TypedVariableList();
    }

    public String getLabel() {
        return label;
    }

    public TypedVariableList getVariables() {
        return variables;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void addVariable(Variable var, String type) {
        variables.addItem(var, type);
    }

    public void addVariable(String var, String type) {
        variables.addItem(new Variable(var), type);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(label + "(");
        for( Variable var : variables.getItems() ) {
            buf.append(var + ":" + variables.getType(var) + " ");
        }
        buf.append(")");
        return buf.toString();
    }

    public Term toTerm() {
        List<Term> tvariables = new ArrayList<Term>();
        for( Variable var : variables.getItems() ) {
            tvariables.add(var);
        }

        return new Compound(label, tvariables);
    }

    public Predicate instantiate(Substitution subst) {
        Predicate ret = new Predicate();
        ret.setLabel(subst.apply(toTerm()).toString());

        return ret;
    }

    @Override
    public boolean equals(Object o) {
    	if (o instanceof Predicate) {
			Predicate pred = (Predicate) o;
			List<Variable> items1 = variables.getItems(), items2 = pred.variables.getItems();

			if( !label.equals(pred.label)) {
				return false;
			}

			if( ! items1.equals(items2) ) {
				return false;
			}

			for( Variable item : items1 ) {
				if( ! variables.getType(item).equals(pred.variables.getType(item)) ) {
					return false;
				}
			}

			return true;


		} else {
			return false;
		}
    }

	@Override
	public int hashCode() {
		return toString().hashCode();
	}


}
