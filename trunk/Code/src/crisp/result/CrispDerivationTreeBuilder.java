package crisp.result;

import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.LexiconEntry;
import de.saar.penguin.tag.grammar.ElementaryTree;
import de.saar.penguin.tag.grammar.NodeType;
import de.saar.penguin.tag.grammar.Constraint;

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
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Variable;
import de.saar.chorus.term.Substitution;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import crisp.tools.Pair;

public class CrispDerivationTreeBuilder extends DerivationTreeBuilder {

    public static final String DEFAULT_ROOT_CATEGORY = "s";
    
    private class Site {
        
        public String derivationNode;
        //public String tree;
        //public String word;
        public String treeNode;
        public String cat;

        public Site(String derivationNode, String treeNode, String cat){
            this.derivationNode = derivationNode;
            //this.tree = tree;
            //this.word = word;
            this.treeNode = treeNode;
            this.cat = cat;
        }                        
    }
    
    private Map<String, ArrayList<Site>> substitutionSites;
    private Map<Pair<String,String>, ArrayList<Site>> adjunctionSites;
    
    public CrispDerivationTreeBuilder(Grammar<Term> grammar) {
        super(grammar);
        
   }       
               
   
   private void addNewSubstAndAdjSites(ElementaryTree tree, Site targetSide, String derivationNode, String selfSem, String step) {
       
       // Get lists of nodes that are open for substitution and adjunction
       ArrayList<String> substNodes = new ArrayList<String>();
       ArrayList<String> adjNodes = new ArrayList<String>();
       
       for (String node : tree.getAllNodes()) {
           if (tree.getNodeType(node) == NodeType.SUBSTITUTION) {
               substNodes.add(node);
           } else {
               if (tree.getNodeConstraint(node) != Constraint.NO_ADJUNCTION && (tree.getNodeDecoration(node) != null)) {
                   adjNodes.add(node);            
               }
           }        
       }
       
       for (String substNode : substNodes) {
           Site substSite = new Site(derivationNode, substNode, tree.getNodeLabel(substNode));
                                 
           String semEffect = ((Constant) tree.getNodeDecoration(substNode)).toString();
           if (semEffect.equals("self")) {
               semEffect = selfSem;
           } else {
               semEffect = semEffect+"-"+step;
           }
           
           ArrayList<Site> effectsSites = substitutionSites.get(semEffect);
                      
           if (effectsSites == null) {
               effectsSites = new ArrayList<Site>();
               substitutionSites.put(semEffect, effectsSites);
           }
           effectsSites.add(substSite);
       }
       
       for (String adjNode : adjNodes) {
           Site adjSite = new Site(derivationNode, adjNode, tree.getNodeLabel(adjNode));
           
           String semEffect = ((Constant) tree.getNodeDecoration(adjNode)).toString();
           if (semEffect.equals("self")) {
               semEffect = selfSem;
           } else {
               semEffect = semEffect+"-"+step;
           }
           
           Pair<String,String> semCat = new Pair<String, String>(semEffect, tree.getNodeLabel(adjNode));
           
           ArrayList<Site> effectsSites = substitutionSites.get(semCat);
                      
           if (effectsSites == null) {
               effectsSites = new ArrayList<Site>();
               adjunctionSites.put(semCat, effectsSites);
           }
           effectsSites.add(adjSite);
       }                     
       
   }
   
    @Override 
    public void processPlanStep(Action action) {

            TypedVariableList t = action.getPredicate().getVariables();                    
            Compound pred = (Compound) action.getPredicate().toTerm();
            String predicateName = action.getPredicate().getLabel();
           
            String[] predicateParts = predicateName.split("-");
            String treename = predicateParts[0];
            String word = predicateParts[1];
            String step = predicateParts[2];
            
            ElementaryTree childTree = grammar.getTree(treename);
            LexiconEntry childEntry = grammar.getLexiconEntry(word, treename);
            
  			// Won't be doing subst and adj. Only check for adj if subst not found.
            boolean operationFound = false;

            for (Compound comp : getSubstPreconditions(action)) {
                operationFound = true;
                Constant catTerm = (Constant) comp.getSubterms().get(0);
                Constant roleTerm = (Constant) comp.getSubterms().get(1);
                String sem = roleTerm.getName();
                String cat = catTerm.getName();
                
                List<Site> substSites = substitutionSites.get(sem);
                
                if (substSites==null) {
                    throw new RuntimeException("No suitable substitution site found for role "+sem);                    
                }                     
                
                Site substituted = null;
                for (Site substSite : substSites) {
                    if (cat.equals(substSite.cat)){ // Found a suitable substitution site
                        
                        String newDerivNode = currentDerivation.addNode(substSite.derivationNode, substSite.treeNode, treename, childEntry);                                                
                        substituted = substSite;
                        addNewSubstAndAdjSites(childTree, substSite, newDerivNode, sem, step);
                                                
                    }
                    break;
                }
                if (substituted!= null) 
                    substSites.remove(substituted);
                else 
                    throw new RuntimeException("No substitution was performed for action "+action);
            }
            
            if (!operationFound){ // No Subst operation, do adj instead
                for (Compound comp : getAdjPreconditions(action)){
                    Constant catTerm = (Constant) comp.getSubterms().get(0);
                    Constant roleTerm = (Constant) comp.getSubterms().get(1);
                    String sem = roleTerm.getName();
                    String cat = catTerm.getName();
                    
                    Pair<String, String> catSem = new Pair<String, String>(sem, cat);
                    List<Site> adjSites = adjunctionSites.get(catSem);
                
                    if (adjSites==null || adjSites.size()==0) {
                        throw new RuntimeException("Adjunction for "+action+" not possible. catSem was " + catSem);
                    } else {
                        Site adjoinTo = adjSites.get(0);
                        String newDerivNode = currentDerivation.addNode(adjoinTo.derivationNode, adjoinTo.treeNode, treename, childEntry);
                        addNewSubstAndAdjSites(childTree, adjoinTo, newDerivNode, sem, step);
                    }
                }
            }
    }
     
        
    public DerivationTree buildDerivationTreeFromPlan(List<Term> plan, Domain domain, String root_category){
        currentDerivation = new DerivationTree();                		            
        substitutionSites = new HashMap<String, ArrayList<Site>>();
        adjunctionSites = new HashMap<Pair<String,String>, ArrayList<Site>>();
        
        // Create initial substitution site for the root of the derivation
        
        Site rootSite = new Site(null, null, root_category);
        ArrayList rootSites = new ArrayList();
        rootSites.add(rootSite);
        substitutionSites.put("root",rootSites);
        
        for (Term term : plan){
                            
            Action instantiatedAction = computeInstantiatedAction(term, domain);
            processPlanStep(instantiatedAction);                                    
            
        }
        return currentDerivation;
    }
    
    
    @Override
    public DerivationTree buildDerivationTreeFromPlan(List<Term> plan, Domain domain ){
        return buildDerivationTreeFromPlan(plan,domain,DEFAULT_ROOT_CATEGORY);        
    }
    
}
