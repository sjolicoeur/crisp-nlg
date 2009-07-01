package crisp.result;

import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.grammar.Grammar;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Action;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.TypedVariableList;


import de.saar.chorus.term.Term;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Variable;
import de.saar.chorus.term.Substitution;

import java.util.List;

/**
 * Build @{de.saar.penguin.tag.derivation.DerivationTree}s from
 * a plan. 
 */
public class DerivationTreeBuilder{

    Grammar<Term> grammar;
    
    public DerivationTreeBuilder(Grammar<Term> grammar) {
        this.grammar = grammar;
    }   

    
    public DerivationTree buildDerivationTreeFromPlan(List<Term> plan, Domain domain){
        DerivationTree result = new DerivationTree();                		            
            
        for (Term term : plan){
            // Create an action instance for the planning action
            Compound compound = (Compound) term;
            String label = compound.getLabel();
            List<Term> arguments = compound.getSubterms();
            Action action = domain.getAction(label);
            
            TypedVariableList variables = action.getPredicate().getVariables();
            
            // Create a substitution (individuals for variables) for the original action to instantiate it. 
            int i = 0;
            Substitution subst = new Substitution();
            for (Variable v : variables) {                
                subst.setSubstitution(v,arguments.get(i)); // Ok, because arguments.get(i) is an individual
                i++;
            }
           
            Action instantiatedAction = action.instantiate(subst);
            
            
            
            
        }
        return result;
    }
}

