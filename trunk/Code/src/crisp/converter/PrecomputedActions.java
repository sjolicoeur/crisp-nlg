package crisp.converter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Set;

import crisp.planningproblem.Action;
import crisp.planningproblem.DurativeAction;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.TypedVariableList;
import crisp.planningproblem.effect.Effect;
import crisp.planningproblem.goal.Goal;

import crisp.termparser.TermParser;

import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

import crisp.converter.grammar.TAGrammar;
import crisp.converter.grammar.TAGTree;
import crisp.converter.grammar.TAGNode;
import crisp.converter.grammar.TAGLeaf;
import crisp.converter.grammar.TAGLexEntry;

import java.lang.Math;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;


/**
* This class generates and represents a set of actions precomputed from the 
* grammar and methods to generate these actions.
* At runtime we only have to select and include to the domain actions with  
* preconditions that can actually become true.
* Efficient selection of these actions should get around the runtime bottleneck
* of computing the planning domain because we only have to generate actions once
* for the grammar. We could even serialize this object and just reload it every 
* time we run the program.
*
* @author Daniel Bauer 
*/

public class PrecomputedActions {  
    
    public static int ACTION_TYPE_SUBST = 0;
    public static int ACTION_TYPE_ADJOIN = 1;
    public static int ACTION_TYPE_INIT = 2;
    
    
    private TAGrammar grammar;
    
    private Set<Term> trueAtoms;
    
    // Store the actions so we can access them by their semantic content.
    private HashMap<String, ArrayList<DurativeAction>> actionsBySemContent; 
    private ArrayList<DurativeAction> emptyActions; 
    
    private Map<String,List<String>> roles = new HashMap<String, List<String>>();
    private Map<String,TAGTree> trees = new HashMap<String, TAGTree>();
    
    private int plansize = 5;
    private int maximumArity = 0;
    
    /*********************** Constructors *****************/
    
    /**
    * Create a new empty set of precomputed Actions.
    */
    public PrecomputedActions() {
        actionsBySemContent = new HashMap<String, ArrayList<DurativeAction>>();
        emptyActions = new ArrayList<DurativeAction>();
    }
    
    
    /**
    * Create a new set of precomputed Actions from a grammar file.
    */
    public PrecomputedActions(File grammarFile) throws ParserConfigurationException, SAXParseException, SAXException, IOException{   
        this();
        TAGrammar grammar = GrammarParser.parseGrammar(grammarFile);
        actionsFromGrammar(grammar);
    }
    
    
    /**
    * Create a new set of precomputed Actions from a TAGrammar object.
    */
    public PrecomputedActions(TAGrammar grammar){
        this();
        actionsFromGrammar(grammar);
    }
    
    
    /***********************Set and get methods ***********/
    
    /**
    * @return the grammar used to create the set of actions
    */
    public TAGrammar getGrammar(){
        return grammar;
    }
    
    
    /**
    * Retrieve a set of actions to include in the domain for the 
    * current planning problem. This method should return a minimal
    * set of actions that can have true preconditions during planning
    * for the given problem.
    * @param problem The problem for which to select appropriate actions
    * @return A list of actions to generate a domain for a given planning problem. 
    */
    public ArrayList<DurativeAction> retrieveActions(Collection<String> items) {
        ArrayList<DurativeAction> ret = new ArrayList<DurativeAction>();
        for (String key : items) {
            ArrayList<DurativeAction> actions = actionsBySemContent.get(key);
            if (actions != null)
                ret.addAll(actions);
        }
        ret.addAll(emptyActions); // Add actions with empty semantics
        return ret;
    }
    
    public ArrayList<DurativeAction> getAllActions(){
        return retrieveActions(actionsBySemContent.keySet());
    }
    
    /******************** Methods to create actions ***************/
    
    /**
    * Create the set of i actions of a specific type (substitution, adjunction) 
    * involving a certain lexical entry, a target tree and a node label and add
    * it to the object's action list.
    */
    private void createActions(TAGLexEntry entry, String targetTree, String label, int actionType, float prob){
        
        // compute action name
        StringWriter actionNameBuf = new StringWriter();
        if (actionType == ACTION_TYPE_SUBST || actionType== ACTION_TYPE_INIT) 
            actionNameBuf.write("subst-");
        else if (actionType == ACTION_TYPE_ADJOIN)
            actionNameBuf.write("adj-");
        
        actionNameBuf.write(normalizeTreename(entry.getTreeRef()));
        actionNameBuf.write("-");
        actionNameBuf.write(entry.getWord());
        actionNameBuf.write("-");
        if (actionType == ACTION_TYPE_INIT)
            actionNameBuf.write("init");
        else{
            actionNameBuf.write(normalizeTreename(targetTree));
            actionNameBuf.write("-");
            actionNameBuf.write(label);
        }
        String actionName = actionNameBuf.toString();
        
        String semContent = entry.getSemContent();
        
        String treeRef = entry.getTreeRef();
        TAGTree tree = trees.get(entry.getTreeRef());
        String rootCategory = tree.getRootNode().getCat();
        
        int maxPlanLength = ((actionType == ACTION_TYPE_INIT) ? 1 : plansize);
        for (int i = 1; i <= maxPlanLength; i++) {
            
            Map<String, String> n = new HashMap<String, String>();
            Map<String, String> I = new HashMap<String, String>();
            int roleno = 1;
            
            
            // compute n and I as in the paper
            for ( String role : roles.get(treeRef)) {
                if ( role.equals("self") )  
                    n.put(role,"?u");
                else 
                n.put(role, role + i);
                I.put(n.get(role), "?x" + (roleno++));
            }
            
            HashMap<String,String> constants = new HashMap<String,String>();
            
            constants.put("step"+(i-1),"stepindex");
            constants.put("step"+i,"stepindex");
            
            ArrayList<Predicate> predicates = new ArrayList<Predicate>();
            constants.put(normalizeTreename(entry.getTreeRef()+"-"+entry.getWord()),"treename");
            constants.put(rootCategory,"category");
            
            
            Predicate pred = new Predicate();
            ArrayList<Goal> preconds = new ArrayList<Goal>();
            ArrayList<Effect> effects = new ArrayList<Effect>();
            
            // Compute the predicate
            pred.setLabel(actionName +  (i-1));
            //pred.addVariable("?u","syntaxnode");
            for (String role : roles.get(treeRef))
                pred.addVariable(I.get(n.get(role)), "individual");
            
            // Count the step
            preconds.add(new crisp.planningproblem.goal.Literal("step(step"+(i-1)+")",true));
            effects.add(new crisp.planningproblem.effect.Literal("step(step"+(i-1)+")",false));
            effects.add(new crisp.planningproblem.effect.Literal("step(step"+i+")",true));
            
            String targetLabel = label+(i-1);
            constants.put(targetLabel,"syntaxnode");
            // Satisfy open substitution or adjunction
            if (actionType == ACTION_TYPE_SUBST) {
                preconds.add(new crisp.planningproblem.goal.Literal("subst(" + normalizeTreename(targetTree)+ ", "+ rootCategory + ","+targetLabel+")", true));
                effects.add(new crisp.planningproblem.effect.Literal("subst(" + normalizeTreename(targetTree)+ ", "+rootCategory + ","+targetLabel+")", false));
            } else if (actionType == ACTION_TYPE_ADJOIN) {
                preconds.add(new crisp.planningproblem.goal.Literal("canadjoin(" + rootCategory + ", "+targetLabel+")", true));
                effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" + rootCategory + ","+targetLabel+")", false)); 
            } else if (actionType == ACTION_TYPE_INIT) {
                preconds.add(new crisp.planningproblem.goal.Literal("subst(root, S, init)", true));
                effects.add(new crisp.planningproblem.effect.Literal("subst(root, S, init)", false));
                targetLabel = "init";

            }
            
            // semantic content must be satisfied 
            List<Term> contentWithVariables = new ArrayList<Term>();
            boolean hasContent = false;
            
            Compound term = null;
            if (semContent != null) {
                term = (Compound) TermParser.parse(entry.getSemContent());
                Compound termWithVariables = (Compound) substituteVariablesForRoles(term, n, I);
                
                hasContent = true;
                
                predicates.add(makeSemanticPredicate(term));
                preconds.add(new crisp.planningproblem.goal.Literal(termWithVariables, true));
                
                contentWithVariables.add(termWithVariables);
                
                effects.add(new crisp.planningproblem.effect.Literal(flattenTerm(termWithVariables, "needtoexpress"), false));
                
                if ( term.getSubterms().size() > maximumArity ) 
                    maximumArity = term.getSubterms().size();
                
                constants.put(renamePredicate(term.getLabel()), "predicate");
            }
            
            // TODO: semantic requirements must also be satisfied
            
            // effects for the substitution nodes
            for (TAGNode substNode : tree.getSubstNodes()) {
                String role = substNode.getSem();                       
                String roleN = n.get(role);
                String cat = substNode.getCat();
                
                constants.put(cat,"category");
                
                effects.add(new crisp.planningproblem.effect.Literal("subst(" + normalizeTreename(entry.getTreeRef())+"-"+entry.getWord()+", "+ cat +", "+roleN + ")", true));
                
                if (!role.equals("self") ) 
                    constants.put(roleN, "syntaxnode");
                
                //referent
                effects.add(new crisp.planningproblem.effect.Literal("referent(" + roleN + ", " + I.get(roleN) + ")", true));
                
            }
            
            // internal nodes: allow adjunction
            for ( TAGNode adjNode : tree.getNonSubstNodes()) {
                
                String role = adjNode.getSem();
                String roleN = (((role == null) || (role.equals("self"))) ? targetLabel : n.get(role));
                
                String cat = adjNode.getCat();
                
                constants.put(cat,"category");
                
                // canadjoin
                effects.add(new crisp.planningproblem.effect.Literal("canadjoin(" + cat + ", " + roleN + ")", true));
                
                // mustadjoin         
                String constraint = adjNode.getConstraint();
                if( constraint!=null && constraint.equals("oa")){
                    effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" + cat + ", " + roleN + ")", true));
                    
                }
                // don't need to add constant to the constant list because we ASSUME that every role
                // except for "self" decorates some substitution node (and hence is added there)                    
            }
            
            /* Generate action */
            
            DurativeAction newAction = new DurativeAction(pred, new crisp.planningproblem.goal.Conjunction(preconds), new crisp.planningproblem.effect.Conjunction(effects), constants, predicates, probabilityToDuration(prob));
            
            if (term != null) {
                String key = term.getLabel()+"-"+term.getSubterms().size(); // Use label and arity as keys
                if (!actionsBySemContent.containsKey(key))
                    actionsBySemContent.put(key, new ArrayList<DurativeAction>());
                actionsBySemContent.get(key).add(newAction); // Sort new actions in a HashMap by semantic content
            } else 
            emptyActions.add(newAction); // For actions that don't satisfy semantic requirements
        }
    }
    
    
    /**
    * Create actions from a TAGrammar object.
    */
    public void actionsFromGrammar(TAGrammar grammar) {        
        
        // get trees from grammar and store them in a hashmap by name.
        for(TAGTree tree : grammar.getTrees() ) {
            
            String treeName = tree.getID();
            trees.put(treeName, tree);
            
            // store list of roles in each tree in a map by name
            roles.put(treeName, tree.getRoles());
        }
        
        for (TAGLexEntry entry : grammar.getLexicon()) {
            
            String treeRef = entry.getTreeRef();
            TAGTree tree = trees.get(entry.getTreeRef());
            
            
            // Create action to use this tree as initial tree, if there is any 
            // chance to do so.
            float initProb = entry.getInitProb();
            if (initProb>0) 
                createActions(entry, null, null, ACTION_TYPE_INIT, initProb);
            
            
            
            /* Create one action for substitution of every tree into 
            every other tree if there is a probability >0.*/
            HashMap<String, HashMap<String, Float>> substProbs = entry.getSubstProbs();
            for (String targetTree: substProbs.keySet()){
                
                HashMap<String, Float> substLabelProbs = substProbs.get(targetTree);
                // for each possible node in the target tree
                for (String label : substLabelProbs.keySet()){
                    float substProb = substLabelProbs.get(label);
                    createActions(entry, targetTree, label, ACTION_TYPE_SUBST, substProb);
                }
            }
            
            /* Create one action for adjunction of every tree into 
            every other tree if there is a probability >0.*/
            HashMap<String, HashMap<String, Float>> adjProbs = entry.getAdjProbs();
            for (String targetTree: adjProbs.keySet()){
                
                HashMap<String, Float> adjLabelProbs = adjProbs.get(targetTree);
                // for each possible node in the target tree           
                for (String label : adjLabelProbs.keySet()){ 
                    float adjProb = adjLabelProbs.get(label);
                    createActions(entry, targetTree, label, ACTION_TYPE_ADJOIN, adjProb);
                }
            }
        }
    }
    
    /********************** Auxiliary functions ***********************/
    
    /**
    * Convert substitution/adjunction probabilities to action durations.
    * @param prob the probability to convert
    * @return the corresponding duration -log(p) for the action.
    */ 
    private double probabilityToDuration(double prob){
        return -Math.log(prob);
    }
    
    
    /**
    * Translates XTAG style tree names into tree names that PDDL will accept.
    *
    * @param treename
    * @return
    */
    private static String normalizeTreename(String treename) {
        return treename.replace("i.", "init_").replace("a.", "aux_");
    }
    
    /**
    * Translates a Term into a Predicate.  This method assumes that the argument
    * is really an object of class Compound.
    *
    * @param term
    * @return
    */
    private static Predicate makeSemanticPredicate(Term term) {
        Predicate ret = new Predicate();
        Compound t = (Compound) term;
       
        ret.setLabel(t.getLabel());
        for( int i = 1; i <= t.getSubterms().size(); i++ ) 
            ret.addVariable("?y" + i, "individual");
       
        return ret;
    }
    
    
    /**
    * Replaces all occurrences of semantic roles in the given term by the
    * variables that correspond to them.  That is, each occurrence of a role
    * r is replaced by I(n(r)) as defined in the paper.
    *
    * @param term
    * @param n a mapping of role names to node identities
    * @param I a mapping of node identities to variables
    * @return
    */
    private static Term substituteVariablesForRoles(Term term, Map<String, String> n, Map<String, String> I) {
        if( term instanceof Compound ) {
            Compound t = (Compound) term;
            List<Term> newChildren = new ArrayList<Term>();
            
            for( Term sub : t.getSubterms()) {
                newChildren.add(substituteVariablesForRoles(sub, n, I));
            }
            
            return new Compound(t.getLabel(), newChildren);
        } else if( term instanceof Constant ) {
            Constant t = (Constant) term;
            if( n.containsKey(t.getName()) ) {
                return new Variable(I.get(n.get(t.getName())));
            } else {
                return t;
            }
        } else {
            return term;
        }
    }
    
    
    /**
    * Translates a term into one in which the predicate symbol of the original
    * term becomes the first argument.  The call flattenTerm(f(a,b), "foo") will
    * return the term foo-2(f,a,b); the 2 is the arity of the original term.
    *
    * @param t
    * @param newLabel
    * @return
    */
    private static Term flattenTerm(Compound t, String newLabel) {
        List<Term> subterms = new ArrayList<Term>();
        
        subterms.add(new Constant(renamePredicate(t.getLabel())));
        subterms.addAll(t.getSubterms());
        
        return new Compound(newLabel + "-" + t.getSubterms().size(), subterms);
    }
    
    private static String renamePredicate(String predicate) {
        return "pred-"+predicate;
    }
    
    
}
