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

public class PCrispDerivationTreeBuilder extends DerivationTreeBuilder {

    public PCrispDerivationTreeBuilder(Grammar<Term> grammar) {
        super(grammar);
    }   
    
  
    @Override 
    public void processPlanStep(Action action) {

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
    
    @Override
    public DerivationTree buildDerivationTreeFromPlan(List<Term> plan, Domain domain){
        currentDerivation = new DerivationTree();                		            
            
        syntaxnodeToTreeId = new HashMap<String, String>();
        
        for (Term term : plan){
            
            Action instantiatedAction = computeInstantiatedAction(term, domain);                        
            processPlanStep(instantiatedAction);                                    
            
        }
        return currentDerivation;
    }
    
}
