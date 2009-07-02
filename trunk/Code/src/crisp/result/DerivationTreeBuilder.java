package crisp.result;

import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.LexiconEntry;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Action;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.TypedVariableList;
import crisp.planningproblem.goal.Goal;
import crisp.planningproblem.goal.Literal;
import crisp.planningproblem.goal.Conjunction;
import crisp.planningproblem.effect.Effect;


import crisp.converter.TagActionType;

import de.saar.chorus.term.Term;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Variable;
import de.saar.chorus.term.Substitution;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


/**
 * Build @{de.saar.penguin.tag.derivation.DerivationTree}s from
 * a plan. 
 *
 * @author Daniel Bauer
 */
public class DerivationTreeBuilder{
       
    
    private Grammar<Term> grammar;
    
    private Map<String,String> syntaxnodeToTreeId; // Store a mapping from tree#word#syntaxnode -> derivation 
                                                       // tree tree ID.   
    private DerivationTree currentDerivation;
    
    public DerivationTreeBuilder(Grammar<Term> grammar) {
        this.grammar = grammar;
    }   
            
    /** 
     * Retrieve the list of positive preconditions with a certain label from an instantiated action.
     */
    private List<Compound> getPreconditionByLabel(Action action, String label) {
        Conjunction preconds = (Conjunction) action.getPrecondition();        
        List<Compound> result = new ArrayList<Compound>();
        for (Goal conjunct : preconds.getConjuncts()) {
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

    private List<Compound> getSubstPreconditions(Action action) {
        return this.getPreconditionByLabel(action, "subst");
    }
    
    private List<Compound> getAdjPreconditions(Action action) {
        return this.getPreconditionByLabel(action, "canadjoin");
    }


    /** 
     * Retrieve the list of positive effects with a certain label from an instantiated action.
     */
    private List<Compound> getEffectsByLabel(Action action, String label) {
        crisp.planningproblem.effect.Conjunction effect = (crisp.planningproblem.effect.Conjunction) action.getEffect();        
        List<Compound> result = new ArrayList<Compound>();
        for (Effect conjunct : effect.getConjuncts()) {
            if (conjunct instanceof crisp.planningproblem.effect.Literal){
                crisp.planningproblem.effect.Literal literal = (crisp.planningproblem.effect.Literal) conjunct;
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

    private List<Compound> getSubstEffects(Action action) {
        return this.getEffectsByLabel(action, "subst");
    }
    
    private List<Compound> getAdjEffects(Action action) {
        return this.getEffectsByLabel(action, "canadjoin");
    }    
    
    private void processPlanStep(Action action) {

            TypedVariableList t = action.getPredicate().getVariables();            
        
            Compound pred = (Compound) action.getPredicate().toTerm();
            String[] predicateParts = pred.getLabel().split("-");                        
            TagActionType operation = decodeActionType(predicateParts[0]);
            String childTree = predicateParts[1];
            String word = predicateParts[2];
                
            String parentTree = null;
            String parentWord = null;
            String parentNode = null;            
            int step;                                  
            
            String syntaxnode = pred.getSubterms().get(0).toString();
            
            
            if (operation == TagActionType.NO_ADJUNCTION) { // this is a null adjunction
               // Ignore no adjunction operations for now
            } else {
                                                
            
                String treeId;
                String snode;
                String node;                
                
                if (operation == TagActionType.INIT ) { // this is an init operation                                
                    step = Integer.valueOf(predicateParts[3]).intValue();                    
                    LexiconEntry entry = grammar.getLexiconEntry(word,childTree);
                    // Really add the derivation
                    treeId = currentDerivation.addNode(null, null, childTree, entry);                                                            
                                                                                
                } else {// This is an adjunction or substitution
                    parentTree = predicateParts[3];
                    parentWord = predicateParts[4];
                    parentNode = predicateParts[5];
                    step = Integer.valueOf(predicateParts[6]).intValue();                    
                    String key = parentTree+"#"+parentWord+"#"+syntaxnode;                
                    String targetTreeId = syntaxnodeToTreeId.get(key);
                    LexiconEntry entry = grammar.getLexiconEntry(word,childTree);
                    // Really add the derivation
                    treeId = currentDerivation.addNode(targetTreeId, parentNode, childTree, entry);
                }
                                               
                // Store the treeId for all 
                for (Compound substterm : getSubstEffects(action)) {
                    node = substterm.getSubterms().get(1).toString();
                    snode = substterm.getSubterms().get(2).toString();
                    String key = childTree+"#"+word+"#"+snode;
                    syntaxnodeToTreeId.put(key,treeId);
                }
                
                for (Compound adjterm : getAdjEffects(action)) {
                    node = adjterm.getSubterms().get(1).toString();
                    snode = adjterm.getSubterms().get(2).toString();
                    String key = childTree+"#"+word+"#"+snode;
                    syntaxnodeToTreeId.put(key,treeId);
                }
                
            }                                              
    }
    
    private TagActionType decodeActionType(String actionType) {
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
    
    public DerivationTree buildDerivationTreeFromPlan(List<Term> plan, Domain domain){
        currentDerivation = new DerivationTree();                		            
            
        syntaxnodeToTreeId = new HashMap<String, String>();
        
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
            processPlanStep(instantiatedAction);                                    
            
        }
        return currentDerivation;
    }        
    
}

