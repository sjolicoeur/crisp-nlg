package crisp.converter;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;

import crisp.planningproblem.Action;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.TypedVariableList;

import crisp.planningproblem.formula.Formula;
import crisp.planningproblem.formula.Conjunction;
import crisp.planningproblem.formula.Literal;
import crisp.planningproblem.formula.Negation;
import crisp.planningproblem.formula.Universal;
import crisp.planningproblem.formula.Conditional;

import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.grammar.LexiconEntry;
import de.saar.penguin.tag.grammar.ElementaryTree;
import de.saar.penguin.tag.grammar.NodeType;
import de.saar.penguin.tag.grammar.Constraint;






public class PCrispActionCreator {
    
    /******************** Methods to create actions ***************/    
    
    private static int maximumArity = 0; // the maximum arity of any predicate in the problem file
    
    private static String getTreeName(LexiconEntry entry){
        String actionName = entry.tree + "-" + entry.word;
        for (String aux : entry.auxLexicalItems.keySet()) {
            actionName += "-" + aux;
        }
        return actionName;
    }
    
    public static Action createInitAction(ProbabilisticGrammar grammar, LexiconEntry entry, double prob, Map<String,HashSet<String>> roles){               
        
        // Get the tree for this lexical entry from the hash map
        String treeRef = entry.tree;
        ElementaryTree<Term> tree = grammar.getTree(entry.tree);
        Collection<String> allNodes = tree.getAllNodes();
        
        // Get lists of nodes that are open for substitution and adjunction
        List<String> substNodes = new ArrayList<String>();
        List<String> adjNodes = new ArrayList<String>();                
        
        Map<String,String> constants = new HashMap<String,String>();
        
        for (String node : allNodes) {
            String cat = tree.getNodeLabel(node);
            if (cat==null || cat.equals("")) {
                constants.put(cat, "category");
            }
            if (tree.getNodeType(node) == NodeType.SUBSTITUTION) {
                substNodes.add(node);
            } else {
                if (tree.getNodeConstraint(node) != Constraint.NO_ADJUNCTION && (tree.getNodeDecoration(node) != null)) {
                    adjNodes.add(node);            
                }
            }        
        }
        
        
        // compute action name
        String treeName = getTreeName(entry);
        String actionName = "init-" + treeName; 
        
        
        List<Term> semantics = entry.semantics;        
        
        String rootCategory = tree.getNodeLabel(tree.getRoot());                      
        
        // compute n and I as in the paper
        Map<String, String> n = new HashMap<String, String>();
        Map<String, String> I = new HashMap<String, String>();
        int roleno = 1;
        
        int i = 1;
        List<String> rolelist = new ArrayList<String>(roles.get(entry.tree));
        Collections.sort(rolelist);
        rolelist.remove("self");
        rolelist.add(0,"self");

        for( String role : rolelist) {
            if (role.equals("self") )    
                n.put("self","?u");
            else 
                n.put(role, role + "-" + i);
            I.put(n.get(role), "?x" + (roleno++));                       
        }                       
        
        
        constants.put("step0","stepindex");
        constants.put("step1","stepindex");
        
        Map<Compound, List<String>> predicates = new HashMap<Compound, List<String>>();
        constants.put(treeName,"treename");
        constants.put(rootCategory,"category");                
        
        
        ArrayList<Formula> preconds = new ArrayList<Formula>();
        ArrayList<Formula> effects = new ArrayList<Formula>();
        
        // Compute the predicate
        String label = actionName + "-0";
        List<Term> variables = new ArrayList<Term>();
        List<String> types = new ArrayList<String>();
        variables.add(new Variable("?u"));
        types.add("syntaxnode");
        
        for (String role : rolelist) {
            variables.add(new Variable(I.get(n.get(role))));
            types.add("individual");
        }

        Compound pred = new Compound(label, variables);

        // Syntaxnode must be referent for first individual
        preconds.add(makeLiteral(true, "referent","?u",I.get(n.get("self"))));
        
        // Count the step
        
        preconds.add(makeLiteral(true, "step", "step0"));
        effects.add(makeLiteral(false, "step","step0"));
        effects.add(makeLiteral(true, "step","step1"));
        
        constants.put("init", "syntaxnode");
        
        preconds.add(makeLiteral(true, "subst","root","none","?u"));
        effects.add(makeLiteral(false, "subst","root", "none", "?u"));
        constants.put("none", "nodetype");           
        
        List<Term> contentWithVariables = new ArrayList<Term>();        
        
        // semantic content must be satisfied
        for (Term semContTerm : entry.semantics) {
            
            Compound semContCompound = ((Compound) semContTerm);                                                                
            Compound termWithVariables = (Compound) substituteVariablesForRoles(semContCompound, n, I);
            

            Compound semPredicate = makeSemanticPredicate(semContCompound);
            List<String> semPredicateTypes = new ArrayList<String>();
            for (int j=0; j < semPredicate.getSubterms().size(); j++) {
                semPredicateTypes.add("individual");
            }
            predicates.put(semPredicate, semPredicateTypes);
            preconds.add(new Literal(termWithVariables, true));
            
            contentWithVariables.add(termWithVariables);
            
            effects.add(new Literal((Compound) flattenTerm(termWithVariables, "needtoexpress"),false));
            
            if ( semContCompound.getSubterms().size() > maximumArity ) { 
                maximumArity = semContCompound.getSubterms().size();
            }
            
            constants.put(renamePredicate(semContCompound.getLabel()), "predicate");
            
            // remove distractors
            /*
            Variable distractorVar = new Variable("?y");
            Substitution distractorSubst = new Substitution(new Variable("?x1"), distractorVar);
            
            List<Term> distractorQuantifierVars = new ArrayList<Term>();
            List<String> distractorQuantifierTypes= new ArrayList<String>();
            distractorQuantifierVars.add(distractorVar);
            distractorQuantifierTypes.add("individual");
            
            List<Formula> literals = new ArrayList<Formula>();
            for ( Term t: contentWithVariables ) 
                literals.add(new Literal((Compound) distractorSubst.apply(t), true));
            
            Formula distractorPrecondition =
            new Negation(new Conjunction(literals));
            
            effects.add(new Universal(distractorQuantifierVars, distractorQuantifierTypes,
            new Conditional(distractorPrecondition,
            new Literal("distractor(?u,?y)", false))));
            */
            
        }
        
        
        /* pragmatic requirements must be satisfied             
        //for( String pragCond : entry.getPragConds() ) {
            //    Compound pragTerm = (Compound) TermParser.parse(pragCond);
            //    predicates.add(makeSemanticPredicate(pragTerm));
            //    preconds.add(new crisp.planningproblem.goal.Literal(substituteVariablesForRoles(pragTerm, n, I), true));
        //}
        
        
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
        */
        
        // effects for the substitution nodes
        for (String substNode : substNodes) {
            String role = tree.getNodeDecoration(substNode).toString();                       
            String roleN = n.get(role);
            
            // Node without role: just assign "self". This should never happen!
            if (roleN == null) {
                roleN = "?u";
            }
            
            String cat = tree.getNodeLabel(substNode);                        
            if (cat.equals(""))                             
                cat = "NONE";            
            
                               
            effects.add(makeLiteral(true, "subst",treeName, substNode, roleN ));
            
            constants.put(substNode,"nodetype");
                        
            if (!role.equals("self") ) 
                constants.put(roleN, "syntaxnode");
            
            //referent
            effects.add(makeLiteral(true, "referent", roleN, I.get(roleN)));
            
            /*
            //distractors
            Variable distractorVar = new Variable("?y");
            Substitution distractorSubst = new Substitution(new Variable(I.get(roleN)), distractorVar);
            
            List<Term> distractorQuantifierVars = new ArrayList<Term>();
            List<String> distractorQuantifierTypes = new ArrayList<String>();
            distractorQuantifierVars.add(distractorVar);
            distractorQuantifierTypes.add("individual");
            
            
            // TODO - it's a bit of a hack that we use the same semantic requirement
            // (modulo substitution) for each substitution node, even if it is irrelevant
            // for the distractors of this substitution node.  But it seems to be ok.
            List<Formula> distractorPreconditions = new ArrayList<Formula>();
            distractorPreconditions.add(new Literal("**equals**(?y," +
            I.get(roleN) + ")", false));
            
            /*for( String sr : entry.getSemReqs() ) {
                Term semReqTerm = distractorSubst.apply(substituteVariablesForRoles(TermParser.parse(sr), n, I));
                predicates.add(makeSemanticPredicate(semReqTerm));
                distractorPreconditions.add(new crisp.planningproblem.goal.Literal(semReqTerm, true));
            } */
            /*
            Formula distractorPrecondition = new Conjunction(distractorPreconditions);
            
            effects.add(new Universal(distractorQuantifierVars, distractorQuantifierTypes,
            new Conditional(distractorPrecondition, new Literal("distractor(" + roleN + ",?y)", true))));
             */
            
        }
        
        // internal nodes: allow adjunction
        for (String adjNode : adjNodes) {
            
            String role = tree.getNodeDecoration(adjNode).toString();             
            String roleN = n.get(role);
            if (roleN == null) {
                roleN = "?u";
            }
            
            String cat = tree.getNodeLabel(adjNode);                   
            if (cat.equals("")){                             
                cat = "NONE";
            }
            
            // canadjoin            
            //effects.add(new Literal("canadjoin(" + treeName + ", " + adjNode + ", "+ roleN+ ")", true));
            // Allways put a mustadjoin constraint
            effects.add(makeLiteral(true, "mustadjoin", treeName, adjNode, roleN));
            
            constants.put(adjNode, "nodetype");
            
            // don't need to add constant to the constant list because we ASSUME that every role
            // except for "self" decorates some substitution node (and hence is added there)                    
        }
        
        // Assemble action
        Action newAction = new Action(pred, types,
        new Conjunction(preconds), new Conjunction(effects),
        probabilityToDuration(prob), constants, predicates);
        
        return newAction;       
    }
    
    /**
    * Create the set of i actions of a specific type (substitution, adjunction) 
    * involving a certain lexical entry, a target tree and a node label and add
    * it to the object's action list.
    */
    public static Collection<Action> createActions(ProbabilisticGrammar grammar, LexiconEntry parentEntry, String nodeID, LexiconEntry childEntry, TagActionType actionType, Double prob, int plansize, Map<String,HashSet<String>> roles){                        
        
        String parentTreeRef = parentEntry.tree;
        String parentWord = parentEntry.word;
        String parentTreeName = getTreeName(parentEntry);                
        String childTreeRef = childEntry.tree;
        String childWord = childEntry.word;
        String childTreeName = getTreeName(childEntry);                        
        
        // compute action name
        StringWriter actionNameBuf = new StringWriter();
        if (actionType == TagActionType.SUBSTITUTION) 
            actionNameBuf.write("subst-");
        else if (actionType == TagActionType.ADJUNCTION)
            actionNameBuf.write("adj-");
        
        actionNameBuf.write(childTreeName);
        actionNameBuf.write("-");  
        actionNameBuf.write(parentTreeName);                        
        actionNameBuf.write("-");        
        actionNameBuf.write(nodeID);
        
        String actionName = actionNameBuf.toString();
        
        List<Term> semantics = childEntry.semantics;        
        
        ElementaryTree parentTree = grammar.getTree(parentTreeRef);
        ElementaryTree childTree =  grammar.getTree(childTreeRef);                               
        
        Collection<String> allNodes = childTree.getAllNodes();                                
        // Get lists of nodes that are open for substitution and adjunction
        ArrayList<String> substNodes = new ArrayList<String>();
        ArrayList<String> adjNodes = new ArrayList<String>();
        
        for (String node : allNodes) {
            if (childTree.getNodeType(node) == NodeType.SUBSTITUTION) {
                substNodes.add(node);
            } else {
                if (childTree.getNodeConstraint(node) != Constraint.NO_ADJUNCTION && (childTree.getNodeDecoration(node) != null)) {
                    adjNodes.add(node);            
                }
            }        
        }
        
        
        
        ArrayList<Action> retList = new ArrayList<Action>();
        
        for (int i = 2; i <= plansize; i++) {
                                    
            // compute n and I as in the paper
            Map<String, String> n = new HashMap<String, String>();
            Map<String, String> I = new HashMap<String, String>();
            int roleno = 1;

           
            List<String> rolelist = new ArrayList<String>(roles.get(childTreeRef));
            Collections.sort(rolelist);
            rolelist.remove("self");
            rolelist.add(0,"self");


            for ( String role : rolelist) {
                if (role != null) {
                    if ( role.equals("self") )   
                        n.put("self","?u");
                    else 
                        n.put(role, role + "-"+i);
                
                    I.put(n.get(role), "?x" + (roleno++));
                }
            }
                                    
                       
            
            HashMap<String,String> constants = new HashMap<String,String>();
            constants.put(parentTreeName,"treename");
            constants.put(childTreeName,"treename");
            
            constants.put("step"+(i-1),"stepindex");
            constants.put("step"+i,"stepindex");
            
            Map<Compound, List<String>> predicates = new HashMap<Compound, List<String>>();
            
            //constants.put(rootCategory,"category");                       
            
            
            // Compute the predicate                        
            ArrayList<Formula> preconds = new ArrayList<Formula>();
            ArrayList<Formula> effects = new ArrayList<Formula>();
            
            String label = (actionName + "-"+  (i-1));

            List<Term> variables = new ArrayList<Term>();
            List<String> types = new ArrayList<String>();
            variables.add(new Variable("?u"));
            types.add("syntaxnode");
            for (String role : rolelist) {
                variables.add(new Variable(I.get(n.get(role))));
                types.add("individual");
            }
            
            Compound pred = new Compound(label, variables);

            // Syntaxnode must be referent for first individual
            preconds.add(makeLiteral(true, "referent","?u","?x1"));
            
            // Count the step
            preconds.add(makeLiteral(true, "step", "step"+(i-1)));
            effects.add(makeLiteral(false, "step", "step"+(i-1)));
            effects.add(makeLiteral(true, "step", "step"+i));
            
            constants.put(nodeID,"nodetype");
            
            // Satisfy open substitution or adjunction
            if (actionType == TagActionType.SUBSTITUTION) {                
                preconds.add(makeLiteral(true, "subst", parentTreeName, nodeID , "?u"));
                effects.add(makeLiteral(false, "subst", parentTreeName, nodeID , "?u"));
            } else if (actionType == TagActionType.ADJUNCTION) {                
                preconds.add(makeLiteral(true,"mustadjoin",parentTreeName,nodeID, "?u"));
                effects.add(makeLiteral(false,"mustadjoin", parentTreeName,nodeID, "?u"));
                //effects.add(new Literal("canadjoin(" + parentTreeName+ ","+ nodeID + ", ?u)", false));
            } 



            
            // semantic content must be satisfied
            List<Term> contentWithVariables = new ArrayList<Term>();
            for (Term semContTerm : semantics) {

                                        Compound semContCompound = ((Compound) semContTerm);
            Compound termWithVariables = (Compound) substituteVariablesForRoles(semContCompound, n, I);


            Compound semPredicate = makeSemanticPredicate(semContCompound);
            List<String> semPredicateTypes = new ArrayList<String>();
            for (int j=0; j < semPredicate.getSubterms().size(); j++) {
                semPredicateTypes.add("individual");
            }
            predicates.put(semPredicate, semPredicateTypes);
            preconds.add(new Literal(termWithVariables, true));

            contentWithVariables.add(termWithVariables);

            effects.add(new Literal((Compound) flattenTerm(termWithVariables, "needtoexpress"),false));

            if ( semContCompound.getSubterms().size() > maximumArity ) {
                maximumArity = semContCompound.getSubterms().size();
            }

            constants.put(renamePredicate(semContCompound.getLabel()), "predicate");

                // remove distractors
                /*
                Variable distractorVar = new Variable("?y");
                Substitution distractorSubst = new Substitution(new Variable("?x1"), distractorVar);

                List<Term> distractorQuantifierVars = new ArrayList<Term>();
                List<String> distractorQuantifierTypes = new ArrayList<String>();
                distractorQuantifierVars.add(distractorVar);
                distractorQuantifierTypes.add("individual");
                
                List<Formula> literals = new ArrayList<Formula>();
                for ( Term t: contentWithVariables ) 
                    literals.add(new Literal((Compound) distractorSubst.apply(t), true));
                
                Formula distractorPrecondition =
                new Negation(new Conjunction(literals));
                
                effects.add(new Universal(distractorQuantifierVars, distractorQuantifierTypes,
                new Conditional(distractorPrecondition,
                new Literal("distractor(?u,?y)", false))));
                 */
            }            
            
            
            /* pragmatic requirements must be satisfied             
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
            
            */
            
            // effects for the substitution nodes
            for (String substNode : substNodes) {
                String role = childTree.getNodeDecoration(substNode).toString();                       
                String roleN = n.get(role);
                
                // Node without role: just assign "self". This should never happen!
                if (roleN == null) {
                    roleN = "?u";
                }
                
                String cat = childTree.getNodeLabel(substNode);                        
                if (cat.equals(""))                             
                    cat = "NONE";            
                
                                       
                effects.add(makeLiteral(true, "subst", childTreeName, substNode, roleN));
                
                constants.put(substNode,"nodetype");
                
                if (!role.equals("self") ) 
                    constants.put(roleN, "syntaxnode");
                
                //referent
                effects.add(makeLiteral(true, "referent",roleN, I.get(roleN)));
                

                /*
                //distractors
                Variable distractorVar = new Variable("?y");
                Substitution distractorSubst = new Substitution(new Variable(I.get(roleN)), distractorVar);
                
                List<Term> distractorQuantifierVars = new ArrayList<Term>();
                List<String> distractorQuantifierTypes = new ArrayList<String>();
                distractorQuantifierVars.add(distractorVar);
                distractorQuantifierTypes.add("individual");
                
                
                // TODO - it's a bit of a hack that we use the same semantic requirement
                // (modulo substitution) for each substitution node, even if it is irrelevant
                // for the distractors of this substitution node.  But it seems to be ok.
                List<Formula> distractorPreconditions = new ArrayList<Formula>();
                distractorPreconditions.add(new Literal("**equals**(?y," + I.get(roleN) + ")" , false));
                
                /*for( String sr : entry.getSemReqs() ) {
                    Term semReqTerm = distractorSubst.apply(substituteVariablesForRoles(TermParser.parse(sr), n, I));
                    predicates.add(makeSemanticPredicate(semReqTerm));
                    distractorPreconditions.add(new crisp.planningproblem.goal.Literal(semReqTerm, true));
                } 
                
                Formula distractorPrecondition =
                new Conjunction(distractorPreconditions);
                
                effects.add(new Universal(distractorQuantifierVars, distractorQuantifierTypes,
                new Conditional(distractorPrecondition,
                new Literal("distractor(" + roleN + ",?y)", true))));
                */
            }
            
            // internal nodes: allow adjunction
            for ( String adjNode : adjNodes) {
                
                String role = childTree.getNodeDecoration(adjNode).toString();             
                String roleN = n.get(role);
                if (roleN == null) {
                    roleN = "?u";
                }
                
                String cat = childTree.getNodeLabel(adjNode);                   
                if (cat.equals("")){                             
                    cat = "NONE";
                }
                
                // canadjoin                
                //effects.add(new Literal("canadjoin(" +
                //childTreeName + ", " + adjNode + ", "+ roleN+ ")", true));
                // Allways put a mustadjoin constraint
                effects.add(makeLiteral(true, "mustadjoin", childTreeName, adjNode,roleN));
                
                constants.put(adjNode, "nodetype");
                
                // don't need to add constant to the constant list because we ASSUME that every role
                // except for "self" decorates some substitution node (and hence is added there)                    
            }
            
            // Assemble action
            Action newAction = new Action(pred, types,
            new Conjunction(preconds), 
            new Conjunction(effects),  probabilityToDuration(prob),
            constants, predicates);
            
            retList.add(newAction);
        }
        return retList;
    }
    
    public static Action createNoAdjoinAction(LexiconEntry entry, String nodeID, Double prob, int plansize) {
        
        // Get the tree for this lexical entry from the hash map
        String treeRef = entry.tree;        
        String treeName = getTreeName(entry);                
        String actionName = "noadj-"+treeName+"-"+nodeID;        
        
        HashMap<String, String> constants = new HashMap<String, String>();
        constants.put(treeName,"treename");        
        
        // Compute the predicate           



        ArrayList<Formula> preconds = new ArrayList<Formula>();
        ArrayList<Formula> effects = new ArrayList<Formula>();
                
        List<Term> variables = new ArrayList<Term>();
        List<String> types = new ArrayList<String>();
        variables.add(new Variable("?u"));
        types.add("syntaxnode");
        
        Compound pred = new Compound(actionName, variables);

        // Don't need to count the step for NoAdjoin, because we can only apply it once for a given node anyways
        // and it blocks all further operations involving this node.
        
        constants.put(nodeID,"nodetype");            
        preconds.add(makeLiteral(true, "mustadjoin",treeName,nodeID ,"?u"));
        effects.add(makeLiteral(false, "mustadjoin",treeName,nodeID ,"?u"));
        //effects.add(new Literal("canadjoin(" + treeName+ ","+ nodeID + ", ?u)", false));
        
        Map<Compound, List<String>> predicates = new HashMap<Compound, List<String>>();
        // Assemble and store action          
        Action newAction = new Action(pred, types, new Conjunction(preconds), new Conjunction(effects),  probabilityToDuration(prob), constants, predicates);
        return newAction;             
    }
    
    /********************** Auxiliary functions ***********************/
    
    /**
    * Convert substitution/adjunction probabilities to action durations.
    * @param prob the probability to convert
    * @return the corresponding duration -log(p) for the action.
    */ 
    private static double probabilityToDuration(double prob){
        return ((prob==1.0) ? 1: 1 - Math.log(prob));
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
    public static Compound makeSemanticPredicate(Term term) {
       
        Compound t = (Compound) term;

        List<Term> subterms = new ArrayList<Term>();
        for( int i = 1; i <= t.getSubterms().size(); i++ ) 
            subterms.add(new Variable("?y" + i));
        
        return new Compound(t.getLabel(),subterms);
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


    /**
     * Construct a Literal from a label name and an array of Strings describing constants or variables.
     * This should be much faster then using the TermParser to construct Literals.
     * @param label label for the new literal
     * @param subterms an array containing the names of constants and variables
     * @param polarity polarity of the literal
     * @return the literal
     */
    private static Literal makeLiteral(boolean polarity, String label, String...constants){
        Term[] subterms = new Term[constants.length];
        int i = 0;
        for (String constant : constants){
        if (constant.startsWith("?")) {
                subterms[i] = new Variable(constant);
            } else {
                subterms[i] = new Constant(constant);
            }
            i++;
        }
        return new Literal(new Compound(label, subterms), polarity);
    }

}
