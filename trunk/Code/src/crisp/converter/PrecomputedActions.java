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


import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;
import de.saar.chorus.term.parser.TermParser;

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
    private ArrayList<DurativeAction> initActions; 
    
    private Map<String,List<String>> roles = new HashMap<String, List<String>>();
    private Map<String,TAGTree> trees = new HashMap<String, TAGTree>();
    
    private int plansize = 10;
    private int maximumArity = 0;
    
    /*********************** Constructors *****************/
    
    /**
    * Create a new empty set of precomputed Actions.
    */
    public PrecomputedActions() {
        actionsBySemContent = new HashMap<String, ArrayList<DurativeAction>>();
        emptyActions = new ArrayList<DurativeAction>();
        initActions = new ArrayList<DurativeAction>();
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
    
      
    // TODO actionsBySemContent now requires a collection of terms, not strings
    //public ArrayList<DurativeAction> getAllActions(){
    //    return retrieveActions(actionsBySemContent.keySet());
    //}
    

    
   /**
    * Retrieve a set of actions to include in the domain for the 
    * current planning problem. This method should return a minimal
    * set of actions that can have true preconditions during planning
    * for the given problem.
    * @param items A list of terms that is true in the initial state of a given problem.
    * @return A list of actions to generate a domain for a given planning problem. 
    */
    public ArrayList<DurativeAction> retrieveActions(Collection<Term> initialStateTerms) {
        ArrayList<DurativeAction> selectedActions = new ArrayList<DurativeAction>();
        
        
        
        Problem dynamicDomain = new Problem();                              
        
        for (Term term : initialStateTerms) {
            if (term.isCompound()) {
                Compound comp = (Compound) term;
                String key = comp.getLabel() + "-" + comp.getSubterms().size();
                ArrayList<DurativeAction> actions = actionsBySemContent.get(key);
                if (actions != null)
                    selectedActions.addAll(actions);
            }
        }
        selectedActions.addAll(emptyActions); // Add actions with empty semantics
        selectedActions.addAll(initActions); // Add actions with empty semantics
                
        
        return selectedActions;
    }
       
    
    /******************** Methods to create actions ***************/
    
    
    private void createInitAction(TAGLexEntry entry, float prob){
        
        // compute action name
        String actionName = "init-" + entry.getTreeName();
        
        String semContent = entry.getSemContent();
        
        String treeRef = entry.getTreeRef();
        TAGTree tree = trees.get(entry.getTreeRef());
        String rootCategory = tree.getRootNode().getCat();                      
        
        Map<String, String> n = new HashMap<String, String>();
        Map<String, String> I = new HashMap<String, String>();
        int roleno = 1;
        
        
        // compute n and I as in the paper
        for ( String role : roles.get(treeRef)) {                
            if ( role.equals("self") || role == null)  
                n.put(role,"?u");
            else 
                n.put(role, role + "-0");
            I.put(n.get(role), "?x" + (roleno++));
        }
        
        HashMap<String,String> constants = new HashMap<String,String>();
        
        constants.put("step0","stepindex");
        constants.put("step1","stepindex");
        
        ArrayList<Predicate> predicates = new ArrayList<Predicate>();
        constants.put(entry.getTreeName(),"treename");
        constants.put(rootCategory,"category");                
        
        Predicate pred = new Predicate();
        ArrayList<Goal> preconds = new ArrayList<Goal>();
        ArrayList<Effect> effects = new ArrayList<Effect>();
        
        // Compute the predicate
        pred.setLabel(actionName + "-0");
        pred.addVariable("?u", "syntaxnode");
        for (String role : roles.get(treeRef))
            pred.addVariable(I.get(n.get(role)), "individual");
        
        // Syntaxnode must be referent for first individual
        preconds.add(new crisp.planningproblem.goal.Literal("referent(?u,?x1)", true));
        
        // Count the step
        preconds.add(new crisp.planningproblem.goal.Literal("step(step0)", true));
        effects.add(new crisp.planningproblem.effect.Literal("step(step0)", false));
        effects.add(new crisp.planningproblem.effect.Literal("step(step1)", true));
        
        
        constants.put("init", "syntaxnode");
        
        
        preconds.add(new crisp.planningproblem.goal.Literal("subst(root, none, ?u)", true));
        effects.add(new crisp.planningproblem.effect.Literal("subst(root, none, ?u)", false));
        constants.put("none", "nodetype");           
        
         
        List<Term> contentWithVariables = new ArrayList<Term>();
        boolean hasContent = false;
        
        Compound term = null;
        
        // semantic content must be satisfied        
        if (semContent != null) {     
            //System.out.println(entry.getSemContent());
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
            
            // remove distractors
            Variable distractorVar = new Variable("?y");
            Substitution distractorSubst = new Substitution(new Variable("?x1"), distractorVar);
            TypedVariableList distractorQuantifierVars = new TypedVariableList();
            distractorQuantifierVars.addItem(distractorVar, "individual");
            
            List<crisp.planningproblem.goal.Goal> literals = new ArrayList<crisp.planningproblem.goal.Goal>();
            for ( Term t: contentWithVariables ) 
                literals.add(new crisp.planningproblem.goal.Literal(distractorSubst.apply(t), true));
            
            Goal distractorPrecondition = 
                new crisp.planningproblem.goal.Negation(new crisp.planningproblem.goal.Conjunction(literals));
            
            effects.add(new crisp.planningproblem.effect.Universal(distractorQuantifierVars,
                new crisp.planningproblem.effect.Conditional(distractorPrecondition,
                    new crisp.planningproblem.effect.Literal("distractor(?u,?y)", false))));
                        
        }
                                
        // TODO: semantic requirements must also be satisfied
        
        // pragmatic requirements must be satisfied             
        for( String pragCond : entry.getPragConds() ) {
            Compound pragTerm = (Compound) TermParser.parse(pragCond);
            predicates.add(makeSemanticPredicate(pragTerm));
            preconds.add(new crisp.planningproblem.goal.Literal(substituteVariablesForRoles(pragTerm, n, I), true));
        }
        
        
        // pragmatic effects
        for( String pragEffect : entry.getPragEffects() ) {
            Compound effect = (Compound) TermParser.parse(pragEffect);
            
            if ( "uniqueref".equals(effect.getLabel())) {
                String roleN = n.get(effect.getSubterms().get(0).toString());
                TypedVariableList vars = new TypedVariableList();
                vars.addItem(new Variable("?y"), "individual");
                
                effects.add(new crisp.planningproblem.effect.Universal(vars,
                new crisp.planningproblem.effect.Literal("distractor(" + roleN + ",?y)", false)));
                break;
            }
        }
        
        // effects for the substitution nodes
        for (TAGNode substNode : tree.getSubstNodes()) {
            String role = substNode.getSem();                       
            String roleN = n.get(role);
            if (roleN == null) {
                roleN = "?u";
            }
            
            String cat = substNode.getCat();                        
            
            constants.put(cat,"category");
            
            //System.out.println(treeIdent+":"+cat+":"+role+":"+roleN);       
            effects.add(new crisp.planningproblem.effect.Literal("subst(" + entry.getTreeName() 
                +", " + substNode.getIndex()  +", "+roleN + ")", true));
            
            constants.put(substNode.getIndex(),"nodetype");
            //if (role==null)
            //System.out.println(tree.getID());
            if (!role.equals("self") ) 
                constants.put(roleN, "syntaxnode");
            
            //referent
            effects.add(new crisp.planningproblem.effect.Literal("referent(" + roleN + ", " + 
                I.get(roleN) + ")", true));
                        
            
            //distractors
            Variable distractorVar = new Variable("?y");
            Substitution distractorSubst = new Substitution(new Variable(I.get(roleN)), distractorVar);
            TypedVariableList distractorQuantifierVars = new TypedVariableList();
            distractorQuantifierVars.addItem(distractorVar, "individual");
            
            
            // TODO - it's a bit of a hack that we use the same semantic requirement
            // (modulo substitution) for each substitution node, even if it is irrelevant
            // for the distractors of this substitution node.  But it seems to be ok.
            List<Goal> distractorPreconditions = new ArrayList<Goal>();
            distractorPreconditions.add(new crisp.planningproblem.goal.Literal("**equals**(?y," +
                I.get(roleN) + ")", false));
            
            for( String sr : entry.getSemReqs() ) {
                Term semReqTerm = distractorSubst.apply(substituteVariablesForRoles(TermParser.parse(sr), n, I));
                predicates.add(makeSemanticPredicate(semReqTerm));
                distractorPreconditions.add(new crisp.planningproblem.goal.Literal(semReqTerm, true));
            }
            
            Goal distractorPrecondition = 
                new crisp.planningproblem.goal.Conjunction(distractorPreconditions);
            
            effects.add(new crisp.planningproblem.effect.Universal(distractorQuantifierVars,
            new crisp.planningproblem.effect.Conditional(distractorPrecondition,
            new crisp.planningproblem.effect.Literal("distractor(" + roleN + ",?y)", true))));
            
            
        }
        
        // internal nodes: allow adjunction
        for ( TAGNode adjNode : tree.getNonSubstNodes()) {
            
            String role = adjNode.getSem();
            //String roleN = (((role == null) || (role.equals("self"))) ? targetNode : 
            String roleN = n.get(role);
            if (roleN == null) {
                roleN = "?u";
            }
            
            String cat = adjNode.getCat();       
            
            
            // canadjoin
            //System.out.println(treeIdent+"#"+cat+"#"+role+"#"+roleN);
            effects.add(new crisp.planningproblem.effect.Literal("canadjoin(" + 
                entry.getTreeName() + ", " + adjNode.getIndex() + ", "+ roleN+ ")", true));
            effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" +
                entry.getTreeName() + ", " + adjNode.getIndex() + ", "+ roleN+ ")", true));
            
            constants.put(adjNode.getIndex(), "nodetype");
            
            // mustadjoin         
            String constraint = adjNode.getConstraint();
            if( constraint != null && constraint.equals("oa") ){
                effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" + 
                    entry.getTreeName() + ", " + adjNode.getIndex() + ", "+ roleN+ ")", true)); 
            }
            // don't need to add constant to the constant list because we ASSUME that every role
            // except for "self" decorates some substitution node (and hence is added there)                    
        }
        
        // Assemble action
        DurativeAction newAction = new DurativeAction(pred, 
            new crisp.planningproblem.goal.Conjunction(preconds), 
                new crisp.planningproblem.effect.Conjunction(effects), 
                    constants, predicates, probabilityToDuration(prob));
        
        initActions.add(newAction);               
        
        
    }
    
    /**
    * Create the set of i actions of a specific type (substitution, adjunction) 
    * involving a certain lexical entry, a target tree and a node label and add
    * it to the object's action list.
    */
    private void createActions(TAGLexEntry target, String nodeID, TAGLexEntry plugger, int actionType, float prob){
                
        
        
        String targetTreeRef = target.getTreeRef();
        String targetLex = target.getWord();
        String targetTreeName = target.getTreeName();                
        String pluggerTreeRef = plugger.getTreeRef();
        String pluggerLex = plugger.getWord();
        String pluggerTreeName = plugger.getTreeName();                        
        
        // compute action name
        StringWriter actionNameBuf = new StringWriter();
        if (actionType == ACTION_TYPE_SUBST) 
            actionNameBuf.write("subst-");
        else if (actionType == ACTION_TYPE_ADJOIN)
            actionNameBuf.write("adj-");
        
        actionNameBuf.write(pluggerTreeName);
        actionNameBuf.write("-");  
        actionNameBuf.write(targetTreeName);        
        actionNameBuf.write("-");        
        actionNameBuf.write(nodeID);
                                     
        String actionName = actionNameBuf.toString();
                
        String semContent = plugger.getSemContent();                        
        
        TAGTree pluggerTree = trees.get(pluggerTreeRef);        
        TAGTree targetTree = trees.get(targetTreeRef);        
        
        
        
        //String rootCategory = targetTree.getRootNode().getCat();
        
        
        for (int i = 2; i <= plansize; i++) {
            
            Map<String, String> n = new HashMap<String, String>();
            Map<String, String> I = new HashMap<String, String>();
            int roleno = 1;
            
            
            // compute n and I as in the paper
            for ( String role : roles.get(pluggerTreeRef)) {                
                if ( role.equals("self"))  
                    n.put(role,"?u");
                else 
                    n.put(role, role +"-"+ i);
                I.put(n.get(role), "?x" + (roleno++));
            }

            
            
            HashMap<String,String> constants = new HashMap<String,String>();
            constants.put(targetTreeName,"treename");
            constants.put(pluggerTreeName,"treename");
            
            constants.put("step"+(i-1),"stepindex");
            constants.put("step"+i,"stepindex");
            
            ArrayList<Predicate> predicates = new ArrayList<Predicate>();
            
            //constants.put(rootCategory,"category");                       


            // Compute the predicate            
            Predicate pred = new Predicate();
            ArrayList<Goal> preconds = new ArrayList<Goal>();
            ArrayList<Effect> effects = new ArrayList<Effect>();
                                   
            pred.setLabel(actionName + "-"+  (i-1));
            pred.addVariable("?u","syntaxnode");
            for (String role : roles.get(pluggerTreeRef))
                pred.addVariable(I.get(n.get(role)), "individual");
            
            
            // Syntaxnode must be referent for first individual
            preconds.add(new crisp.planningproblem.goal.Literal("referent(?u,?x1)", true));
            
            // Count the step
            preconds.add(new crisp.planningproblem.goal.Literal("step(step"+(i-1)+")",true));
            effects.add(new crisp.planningproblem.effect.Literal("step(step"+(i-1)+")",false));
            effects.add(new crisp.planningproblem.effect.Literal("step(step"+i+")",true));            
            
            constants.put(nodeID,"nodetype");
            
            // Satisfy open substitution or adjunction
            if (actionType == ACTION_TYPE_SUBST) {
                //System.out.println(nodeID);
                preconds.add(new crisp.planningproblem.goal.Literal("subst(" + targetTreeName+ ", " + nodeID + ", ?u)", true));
                effects.add(new crisp.planningproblem.effect.Literal("subst(" + targetTreeName+ ", " + nodeID + ", ?u)", false));                
            } else if (actionType == ACTION_TYPE_ADJOIN) {
                //System.out.println(" -- "+targetTree+", "+nodeID);
                preconds.add(new crisp.planningproblem.goal.Literal("canadjoin(" + targetTreeName+ ","+ nodeID + ", ?u)", true));
                effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" + targetTreeName+ ","+ nodeID + ", ?u)", false));                
            } 
            
            // semantic content must be satisfied 
            List<Term> contentWithVariables = new ArrayList<Term>();
            boolean hasContent = false;
            
            
            
            Compound term = null;
            if (semContent != null) {
                //System.out.println(entry.getSemContent());
                term = (Compound) TermParser.parse(plugger.getSemContent());                
                Compound termWithVariables = (Compound) substituteVariablesForRoles(term, n, I);
                
                hasContent = true;
                
                predicates.add(makeSemanticPredicate(term));
                preconds.add(new crisp.planningproblem.goal.Literal(termWithVariables, true));
                
                contentWithVariables.add(termWithVariables);
                
                effects.add(new crisp.planningproblem.effect.Literal(flattenTerm(termWithVariables, "needtoexpress"), false));
                
                if ( term.getSubterms().size() > maximumArity ) 
                    maximumArity = term.getSubterms().size();
                
                constants.put(renamePredicate(term.getLabel()), "predicate");
                
                // remove distractors
                Variable distractorVar = new Variable("?y");
                Substitution distractorSubst = new Substitution(new Variable("?x1"), distractorVar);
                TypedVariableList distractorQuantifierVars = new TypedVariableList();
                distractorQuantifierVars.addItem(distractorVar, "individual");
                
                List<crisp.planningproblem.goal.Goal> literals = new ArrayList<crisp.planningproblem.goal.Goal>();
                for ( Term t: contentWithVariables ) 
                    literals.add(new crisp.planningproblem.goal.Literal(distractorSubst.apply(t), true));
                
                Goal distractorPrecondition = 
                    new crisp.planningproblem.goal.Negation(new crisp.planningproblem.goal.Conjunction(literals));
                
                effects.add(new crisp.planningproblem.effect.Universal(distractorQuantifierVars,
                    new crisp.planningproblem.effect.Conditional(distractorPrecondition,
                        new crisp.planningproblem.effect.Literal("distractor(?u,?y)", false))));
            
                
            }
            
            // TODO: semantic requirements must also be satisfied
            
            // pragmatic requirements must be satisfied             
            for( String pragCond : plugger.getPragConds() ) {
                Compound pragTerm = (Compound) TermParser.parse(pragCond);
                predicates.add(makeSemanticPredicate(pragTerm));
                preconds.add(new crisp.planningproblem.goal.Literal(substituteVariablesForRoles(pragTerm, n, I), true));
            }
                                                        
            
            
            // pragmatic effects
            for( String pragEffect : plugger.getPragEffects() ) {
                Compound effect = (Compound) TermParser.parse(pragEffect);
                
                if ( "uniqueref".equals(effect.getLabel())) {
                    String roleN = n.get(effect.getSubterms().get(0).toString());
                    TypedVariableList vars = new TypedVariableList();
                    vars.addItem(new Variable("?y"), "individual");
                    
                    effects.add(new crisp.planningproblem.effect.Universal(vars,
                    new crisp.planningproblem.effect.Literal("distractor(" + roleN + ",?y)", false)));
                    break;
                }
            }
            
            
            
            // effects for the substitution nodes
            for (TAGNode substNode : pluggerTree.getSubstNodes()) {
                String role = substNode.getSem();                       
                String roleN = n.get(role);
                if (roleN == null) {
                    roleN = "?u";
                }
                
                String cat = substNode.getCat();
                
                constants.put(cat,"category");                                
                
                //System.out.println(treeIdent+":"+cat+":"+role+":"+roleN);       
                effects.add(new crisp.planningproblem.effect.Literal("subst(" + pluggerTreeName +", " + substNode.getIndex()  +", "+roleN + ")", true));
                
                constants.put(substNode.getIndex(),"nodetype");
                //if (role==null)
                //System.out.println(tree.getID());
                if (!role.equals("self") ) 
                    constants.put(roleN, "syntaxnode");
                
                //referent
                effects.add(new crisp.planningproblem.effect.Literal("referent(" + roleN + ", " + I.get(roleN) + ")", true));
                
                //distractors
                Variable distractorVar = new Variable("?y");
                Substitution distractorSubst = new Substitution(new Variable(I.get(roleN)), distractorVar);
                TypedVariableList distractorQuantifierVars = new TypedVariableList();
                distractorQuantifierVars.addItem(distractorVar, "individual");
                
                
                // TODO - it's a bit of a hack that we use the same semantic requirement
                // (modulo substitution) for each substitution node, even if it is irrelevant
                // for the distractors of this substitution node.  But it seems to be ok.
                List<Goal> distractorPreconditions = new ArrayList<Goal>();
                distractorPreconditions.add(new crisp.planningproblem.goal.Literal("**equals**(?y," + I.get(roleN) + ")", false));
                
                for( String sr : plugger.getSemReqs() ) {
                    Term semReqTerm = distractorSubst.apply(substituteVariablesForRoles(TermParser.parse(sr), n, I));
                    predicates.add(makeSemanticPredicate(semReqTerm));
                    distractorPreconditions.add(new crisp.planningproblem.goal.Literal(semReqTerm, true));
                }
                
                Goal distractorPrecondition = new crisp.planningproblem.goal.Conjunction(distractorPreconditions);
                
                effects.add(new crisp.planningproblem.effect.Universal(distractorQuantifierVars,
                    new crisp.planningproblem.effect.Conditional(distractorPrecondition,
                        new crisp.planningproblem.effect.Literal("distractor(" + roleN + ",?y)", true))));            
                
                
            }
            
            // internal nodes: allow adjunction
            for ( TAGNode adjNode : pluggerTree.getNonSubstNodes()) {
                
                String role = adjNode.getSem();
                
                String roleN = n.get(role);
                if (roleN == null) {
                    roleN = "?u";
                }
                
                
                String cat = adjNode.getCat();       
                
                
                // canadjoin
                //System.out.println(treeIdent+"#"+cat+"#"+role+"#"+roleN);
                effects.add(new crisp.planningproblem.effect.Literal("canadjoin(" +pluggerTreeName + ", " + adjNode.getIndex() + ", " + roleN + ")", true));
                effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" +pluggerTreeName + ", " + adjNode.getIndex() + ", " + roleN + ")", true));
                
                constants.put(adjNode.getIndex(),"nodetype");
                
                // mustadjoin         
                String constraint = adjNode.getConstraint();
                if( constraint!=null && constraint.equals("oa")){
                    effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" +pluggerTreeName + ", " + adjNode.getIndex() + ", " + roleN + ")", true)); 
                }
                // don't need to add constant to the constant list because we ASSUME that every role
                // except for "self" decorates some substitution node (and hence is added there)                    
            }
            
            // Assemble action
            DurativeAction newAction = new DurativeAction(pred, new crisp.planningproblem.goal.Conjunction(preconds), new crisp.planningproblem.effect.Conjunction(effects), constants, predicates, probabilityToDuration(prob));
            
            if (term != null) {
                String key = term.getLabel()+"-"+term.getSubterms().size(); // Use label and arity as keys
                if (!actionsBySemContent.containsKey(key))
                    actionsBySemContent.put(key, new ArrayList<DurativeAction>());
                actionsBySemContent.get(key).add(newAction); // Sort new actions in a HashMap by semantic content
            } else 
            emptyActions.add(newAction); // for actions that don't have semantic content
        }
    }
    
    private void createNoAdjoinAction(TAGLexEntry entry, String nodeID, float prob) {
        
        String treeName = entry.getTreeName();
                
        String actionName = "noadj-"+treeName+"-"+nodeID;
        
        HashMap<String, String> constants = new HashMap<String, String>();      
           
        constants.put(treeName,"treename");        

        // Compute the predicate           
        Predicate pred = new Predicate();
        ArrayList<Goal> preconds = new ArrayList<Goal>();
        ArrayList<Effect> effects = new ArrayList<Effect>();
                                    
        pred.setLabel(actionName);
        pred.addVariable("?u","syntaxnode");
                       
        // Don't need to count the step for NoAdjoin, because                     
         
        constants.put(nodeID,"nodetype");            
        preconds.add(new crisp.planningproblem.goal.Literal("canadjoin(" + treeName+ ","+ nodeID + ", ?u)", true));
        effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" + treeName+ ","+ nodeID + ", ?u)", false));
        effects.add(new crisp.planningproblem.effect.Literal("canadjoin(" + treeName+ ","+ nodeID + ", ?u)", false));
                             
        ArrayList<Predicate> predicates = new ArrayList<Predicate>();           
        // Assemble and store action          
        DurativeAction newAction = new DurativeAction(pred, new crisp.planningproblem.goal.Conjunction(preconds), new crisp.planningproblem.effect.Conjunction(effects), constants, predicates, probabilityToDuration(prob));
        emptyActions.add(newAction);
     
        
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
            try {
                createInitAction(entry, initProb);
            } catch (de.saar.chorus.term.parser.TokenMgrError e ) {
                System.err.println("Warning: Couldn't create initial action for "+
                entry.getTreeName()+". Failed to parse a term: "+e);               
            } catch (NullPointerException e) {
                System.err.println("Warning: Couldn't create initial action for "+
                entry.getTreeName()+". Tree not found: "+e);               
            }
                 
            
            // Create actions for no-adjoin on every node of this tree
            HashMap<String, Float> noAdjProbs = entry.getNoAdjProbs();                       
            for (String node: noAdjProbs.keySet()){                
                createNoAdjoinAction(entry, node, noAdjProbs.get(node));
            }
            
            /* Create one action for substitution of every tree into 
            every other tree if there is a probability >0.*/
            HashMap<String, HashMap<String, Float>> substProbs = entry.getSubstProbs();
            for (String key: substProbs.keySet()){
                // Restore treeID and anchor word for the target tree
                String[] keyElements = key.split("-",2);                
                String targetTree = keyElements[0];
                String targetLex = keyElements[1];
                                                
                
                HashMap<String, Float> substLabelProbs = substProbs.get(key);
                
                // for each possible node in the target tree
                for (String node : substLabelProbs.keySet()){
                    //System.out.println(label);
                    float substProb = substLabelProbs.get(node);
                    //System.out.println(targetTree+" : "+targetLex);
                    try {
                        createActions(entry, node, grammar.getEntry(targetTree, targetLex),  ACTION_TYPE_SUBST, substProb);
                    } catch (de.saar.chorus.term.parser.TokenMgrError e ) {
                        System.err.println("Warning: Couldn't create action for substituting "+
                        entry.getTreeName() + " into node "+node+" of "+targetTree+"-"+targetLex+". Failed to parse a term: "+e);
                    } catch (NullPointerException e) {
                        System.err.println("Warning: Couldn't create action for substituting "+
                        entry.getTreeName() + " into node "+node+" of "+targetTree+"-"+targetLex+". Tree not found: "+e);
                    }
                }
            }
            
            /* Create one action for adjunction of every tree into 
            every other tree if there is a probability >0.*/
            HashMap<String, HashMap<String, Float>> adjProbs = entry.getAdjProbs();
            for (String key: adjProbs.keySet()){
                // Restore treeID and anchor word for the target tree
                //ystem.out.println(key);
                String[] keyElements = key.split("-");
                String targetTree = keyElements[0];
                String targetLex = keyElements[1];
                                
                HashMap<String, Float> adjLabelProbs = adjProbs.get(key);
                // for each possible node in the target tree           
                for (String node : adjLabelProbs.keySet()){ 
                    float adjProb = adjLabelProbs.get(node);
                    try {                        
                        createActions(entry, node, grammar.getEntry(targetTree, targetLex), ACTION_TYPE_ADJOIN, adjProb);
                    } catch (de.saar.chorus.term.parser.TokenMgrError e) {
                        System.err.println("Warning: Couldn't create action for adjoining "+
                        entry.getTreeName() + " at node "+node+" of "+targetTree+"-"+targetLex);                    
                    } catch (NullPointerException e) {
                        System.err.println("Warning: Couldn't create action for adjoining "+
                        entry.getTreeName() + " at node "+node+" of "+targetTree+"-"+targetLex+": "+e);
                    }
                    
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
        return ((prob==1.0) ? 0: Math.round(-Math.log(prob)*1000000));
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
