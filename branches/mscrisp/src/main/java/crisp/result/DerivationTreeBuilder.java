package crisp.result;

import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.grammar.Grammar;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Action;
import crisp.planningproblem.formula.Formula;
import crisp.planningproblem.formula.Literal;
import crisp.planningproblem.formula.Conjunction;


import crisp.converter.TagActionType;

import de.saar.chorus.term.Term;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Variable;
import de.saar.chorus.term.Substitution;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;


/**
 * Build @{de.saar.penguin.tag.derivation.DerivationTree}s from
 * a plan. 
 *
 * @author Daniel Bauer
 */
public abstract class DerivationTreeBuilder{
       
    
    protected Grammar<Term> grammar;
    
    protected Map<String,String> syntaxnodeToTreeId; // Store a mapping from tree#word#syntaxnode -> derivation 
                                                       // tree tree ID.   
    protected DerivationTree currentDerivation;
    
    public DerivationTreeBuilder(Grammar<Term> grammar) {
        this.grammar = grammar;
    }   
            
    /** 
     * Retrieve the list of positive preconditions with a certain label from an instantiated action.
     */
    protected List<Compound> getPreconditionByLabel(Action action, String label) {
        Conjunction preconds = (Conjunction) action.getPrecondition();        
        List<Compound> result = new ArrayList<Compound>();
        for (Formula conjunct : preconds.getConjuncts()) {
            if (conjunct instanceof Literal){
                Literal literal = (Literal) conjunct;
                if (literal.getAtom() instanceof Compound){                
                    // Take posisitive subst terms
                    Compound comp = (Compound) literal.getAtom();
                    if ((label.equals(comp.getLabel())) && literal.getPolarity()) {
                        result.add(comp);
                    }
                }
            }
        }
        return result;
    }

    protected List<Compound> getSubstPreconditions(Action action) {
        return this.getPreconditionByLabel(action, "subst");
    }
    
    protected List<Compound> getAdjPreconditions(Action action) {
        return this.getPreconditionByLabel(action, "canadjoin");
    }


    /** 
     * Retrieve the list of positive effects with a certain label from an instantiated action.
     */
    protected List<Compound> getEffectsByLabel(Action action, String label) {
        Conjunction effect = (Conjunction) action.getEffect();        
        List<Compound> result = new ArrayList<Compound>();
        for (Formula conjunct : effect.getConjuncts()) {
            if (conjunct instanceof Literal){
                Literal literal = (Literal) conjunct;
                if (literal.getAtom() instanceof Compound){                
                    // Take posisitive subst terms
                    Compound comp = (Compound) literal.getAtom();
                    if ((label.equals(comp.getLabel())) && literal.getPolarity()) {
                        result.add(comp);
                    }
                }
            }
        }
        return result;
    }

    protected List<Compound> getSubstEffects(Action action) {
        return this.getEffectsByLabel(action, "subst");
    }
    
    protected List<Compound> getAdjEffects(Action action) {
        return this.getEffectsByLabel(action, "canadjoin");
    }    
    
    
    public Action computeInstantiatedAction(Term term, Domain domain) {
        Compound compound = (Compound) term;
        String label = compound.getLabel();            
        List<Term> arguments = compound.getSubterms();
        Action action = domain.findAction(label);

        if( action == null ) {
            throw new RuntimeException("Couldn't find action: " + label);
        }

        List<Term> variables = action.getPredicate().getSubterms();
        
        // Create a substitution (individuals for variables) for the original action to instantiate it. 
        int i = 0;
        Substitution subst = new Substitution();
        for (Term v : variables) {            
            subst.setSubstitution((Variable) v, arguments.get(i)); // Ok, because arguments.get(i) is an individual
            i++;
        }
                    
        return action.instantiate(subst);
    }
    
    public abstract void processPlanStep(Action action);
    
    
    protected TagActionType decodeActionType(String actionType) {
        if (actionType.equals("init")) {
            return TagActionType.INIT;
        } else if (actionType.equals("subst")) {
            return TagActionType.SUBSTITUTION;
        } else if (actionType.equals("adj")) {
            return TagActionType.ADJUNCTION;        
        } else if (actionType.equals("noadj")) {
            return TagActionType.NO_ADJUNCTION;
        } else {
            return TagActionType.UNKNOWN;
        }            
    }
    
    public abstract DerivationTree buildDerivationTreeFromPlan(List<Term> plan, Domain domain);
            
    
}

