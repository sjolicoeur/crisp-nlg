package crisp.converter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import crisp.planningproblem.Action;
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
import de.saar.penguin.tag.grammar.Constraint;
import de.saar.penguin.tag.grammar.ElementaryTree;
import de.saar.penguin.tag.grammar.ElementaryTreeType;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.LexiconEntry;
import de.saar.penguin.tag.grammar.NodeType;
import de.saar.penguin.tag.grammar.filter.GrammarFilterer;
import de.saar.penguin.tag.grammar.filter.SemanticsPredicateListFilter;

/**
* This class provides a fast converter from XML CRISP problem descriptions to
* planning domains and problems. This class only processes non-probabilistic grammar descriptions.
* The parser is implemented as a plain SAX parser, thereby improving
* processing speed and decreasing memory requirements over the old xpath based
* parser in {@link CRISPConverter}, which is particularly important for parsing large grammar 
* description files.
*/

public class FastCRISPConverter extends DefaultHandler {  // Default Handler already does a lot of work like
    // parse error handling and registering the handler
    // with a parser.
    
    private static String problempath;   // Absolute pathname for the directory that stores problem and grammar
    
    
    
    private static int plansize;         // the maximum plan size as specified in the problem file
    private static int maximumArity = 0; // the maximum arity of any predicate in the problem file
    
    private static String problemname;   // the name for the problem as specified in the problem file 
    
    private static String mainCat;       // main category for the problem in the problem file      
    
    
    // Member variables for instances of the Content Handler
    private Stack<String> elementStack = new Stack<String>(); 
    private StringWriter characterBuffer;
    
    private Problem problem;
    private Domain domain;
    
    private Set<Term> trueAtoms;
    
    private Map<String,Set<Integer>> predicatesInWorld;
    
    
    /************************ Methods for the content handler *****************/
    
    public FastCRISPConverter(Domain aDomain, Problem aProblem) {
        problem = aProblem;
        domain = aDomain;
        predicatesInWorld = new HashMap<String, Set<Integer>>();
    }
    
    
    // All of these methods are specified in the ContentHandler interface.    
    public void startDocument() throws SAXException {
        characterBuffer = new StringWriter();
    }
    
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        
        elementStack.push(qName);
        
        // Namespace is ingored for now, use prefixed name and assume prefixes are empty.
        if (qName.equals("crispproblem")){
            
            // Retrieve and set name for the problem
            problemname = atts.getValue("name");  
            domain.setName(problemname);
            problem.setName(problemname);
            problem.setDomain(problemname);
            
            try {
                plansize = Integer.parseInt(atts.getValue("plansize"));
            } catch (NumberFormatException e){
                throw new SAXParseException("Expecting integer number in plansize attribute.",null);
            }
            
           /* Grammar is not parsed from here any more// Open and parse the grammar file
            * try {
            *    grammar = GrammarParser.parseGrammar(convertGrammarPath(atts.getValue("grammar")));
            *} catch (ParserConfigurationException e) {
            *    throw new SAXParseException("Couldn't initialize grammar parse.",null); 
            *} catch (IOException e) {
            *    throw new SAXParseException("Couldn't open grammar file.",null);
            *}
            */
            
            // add Index TODO: what does this attribute do?
            domain.addConstant(atts.getValue("index"),"individual");
            
            mainCat = atts.getValue("cat"); // TODO: do we really need this as a member variable?
            
            // This was in computeInitialState(Domain domain, Problem problem)
            problem.addToInitialState(TermParser.parse("subst(" + mainCat+ ", root)"));
            problem.addToInitialState(TermParser.parse("referent(root, " + 
            atts.getValue("index") + ")"));
            // TODO: maybe there is a better place for this
            problem.addToInitialState(TermParser.parse("step(step1)")); 
            
            // Reinitialize the set of true atoms for each new problem
            trueAtoms = new HashSet<Term>();
            
        }
        
        if (qName.equals("world")){
            characterBuffer = new StringWriter();
        }
        
        if (qName.equals("commgoal")){
            characterBuffer = new StringWriter();
        }
        
    }
    
    public void endElement(String namespaceURI, String localName, String qName)
    throws SAXException {
        
        String lastElement = elementStack.pop();
        if (!(lastElement.equals(qName))) 
            throw new SAXParseException("Cannot close "+qName+
        " here. Expected "+lastElement+".",null);
        
        if (qName.equals("world")){ // Term definition ends here 
            Term term = TermParser.parse(characterBuffer.toString()); // parse the Term
            
            // This was in computeInitialState(Domain domain, Problem problem)
            addPredicateInWorld((Compound) term);
            domain.addPredicate(makeSemanticPredicate(term));
            addIndividualConstants(term,domain);
            
            problem.addToInitialState(term);
            trueAtoms.add(term);
        } 
        
        if (qName.equals("commgoal")){ // Communicative goal definition ends here
            Term term = TermParser.parse(characterBuffer.toString());
            
            // This was in computeInitialState(Domain domain, Problem problem)
            
            // keep track of maximum arity
            if( term instanceof Compound ) {
                Compound c = (Compound) term;
                
                int arity = c.getSubterms().size();
                if( arity > maximumArity ) {
                    maximumArity = arity;
                }
                problem.registerComgoalArity(arity);
                
                domain.addConstant(renamePredicate(c.getLabel()), "predicate");
                
                problem.addToInitialState(flattenTerm(c, "needtoexpress"));
            }
            
        }
    }
    
    
    
    private void addPredicateInWorld(Compound term) {
    	Set<Integer> arities = predicatesInWorld.get(term.getLabel());
    	
    	if( arities == null ) {
    		arities = new HashSet<Integer>();
    		predicatesInWorld.put(term.getLabel(), arities);
    	}
    	
    	arities.add(term.getSubterms().size());
	}


	public void characters(char[] ch, int start, int length) 
    throws SAXException {
        
        if (elementStack.empty())
            throw new SAXParseException("Characters out of any Element.",null);
        
        
        characterBuffer.write(ch, start, length);
        
    } 
    
    
    /*********************** Compute goal and domain  *****************/
    
    /**
    * Compute the goal specification for the given CRISP problem.  This becomes the
    * "goal" clause in the PDDL problem.
    *
    * @param domain
    * @param problem
    */
    private static void computeGoal(Domain domain, Problem problem) {
        TypedVariableList tlNodeIndiv = new TypedVariableList();
        tlNodeIndiv.addItem(new Variable("?u"), "syntaxnode");
        tlNodeIndiv.addItem(new Variable("?x"), "individual");
        
        TypedVariableList tlCatNode = new TypedVariableList();
        tlCatNode.addItem(new Variable("?a"), "category");
        tlCatNode.addItem(new Variable("?u"), "syntaxnode");
        
        // collect all goals in this list
        List<Goal> finalStateGoals = new ArrayList<Goal>();
        
        // no positive "subst" literals in the goal state
        Goal noSubst = new crisp.planningproblem.goal.Universal(tlCatNode,
        new crisp.planningproblem.goal.Literal("subst(?a,?u)", false));
        
        // no positive "distractor" literals in the goal state
        Goal noDistractors = new crisp.planningproblem.goal.Universal(tlNodeIndiv,
        new crisp.planningproblem.goal.Literal("distractor(?u,?x)", false));
        
        // no positive "mustadjoin" literals in the goal state
        //   this is only added if there is an action that creates a mustadjoin constraint
        //   because otherwise the LAMA planner cannot handle universal preconditions 
        //   involving this predicate.   
        if (domain.sawMustadjoin()){           
            Goal noMustAdj= new crisp.planningproblem.goal.Universal(tlCatNode,
            new crisp.planningproblem.goal.Literal("mustadjoin(?a,?u)", false));
            finalStateGoals.add(noMustAdj);
        }
        
        finalStateGoals.add(noSubst);
        finalStateGoals.add(noDistractors);
        
        
        // no positive needtoexpress-* literals, for any arity used in the communicative 
        // goals. If we would just do this for all arities the LAMA planner cannot handle 
        // the universal precondition involving needtoexpress predicates that do not occur        
        // elsewhere as an effect
        for( Integer i : problem.getComgoalArities()) {
            TypedVariableList tlPredicate = new TypedVariableList();
            tlPredicate.addItem(new Variable("?P"), "predicate");
            
            List<Term> subterms = new ArrayList<Term>();
            subterms.add(new Variable("?P"));
            
            for( int j = 1; j <= i; j++ ) {
                tlPredicate.addItem(new Variable("?x" + j), "individual");
                subterms.add(new Variable("?x" + j));                
            }
            
            finalStateGoals.add(new crisp.planningproblem.goal.Universal(tlPredicate,
            new crisp.planningproblem.goal.Literal(new Compound("needtoexpress-" + i, subterms), false)));            
        }
        
        // since negated needtoexpress-* literals can also occur with other arity we  
        // need to add predicates for any arity to the domain.        
        for (int i = 1; i <= maximumArity; i++){
            Predicate predNTE = new Predicate();
            predNTE.setLabel("needtoexpress-" + i);
            predNTE.addVariable("?P", "predicate");
            
            for( int j = 1; j <= i; j++ ) 
                predNTE.addVariable("?x" + j, "individual");
            
            domain.addPredicate(predNTE);
        }
        
        
        problem.setGoal(new crisp.planningproblem.goal.Conjunction(finalStateGoals));
    }
    
    
    
    
    /**
    * Sets up the PDDL domain by registering the requirements, types, and a 
    * bunch of constants. This is called before parsing, therefore part of the
    * domain information cannot be set here (e.g. maximum plan length etc.)
    *
    * @param domain
    * @param problem
    */
    private static void setupDomain(Domain domain, Problem problem) {
        domain.clear();
        problem.clear();
        
        domain.addRequirement(":strips");
        domain.addRequirement(":equality");
        domain.addRequirement(":typing");
        domain.addRequirement(":conditional-effects");               
        //domain.addRequirement(":universal-preconditions"); // Not understood by some planners (e.g LAMA) 
        // and redundant because it is subsumed by
        // :quantified-preconditions
        domain.addRequirement(":quantified-preconditions");
        
        domain.addSubtype("individual", "object");
        domain.addSubtype("category", "object");
        domain.addSubtype("syntaxnode", "object");
        domain.addSubtype("stepindex", "object");
        domain.addSubtype("predicate", "object");
        domain.addSubtype("rolename", "object");
        domain.addSubtype("treename", "object");
        
        Predicate predSubst = new Predicate(); predSubst.setLabel("subst");
        predSubst.addVariable("?x", "category"); predSubst.addVariable("?y", "syntaxnode");
        domain.addPredicate(predSubst);
        
        Predicate predStep = new Predicate(); predStep.setLabel("step");
        predStep.addVariable("?i", "stepindex");
        domain.addPredicate(predStep);
        
        Predicate predDistractor = new Predicate(); predDistractor.setLabel("distractor");
        predDistractor.addVariable("?u", "syntaxnode"); predDistractor.addVariable("?x", "individual");
        domain.addPredicate(predDistractor);
        
        Predicate predReferent = new Predicate(); predReferent.setLabel("referent");
        predReferent.addVariable("?u", "syntaxnode"); predReferent.addVariable("?x", "individual");
        domain.addPredicate(predReferent);
        
        Predicate predCanadjoin = new Predicate(); predCanadjoin.setLabel("canadjoin");
        predCanadjoin.addVariable("?x", "category"); predCanadjoin.addVariable("?y", "syntaxnode");
        domain.addPredicate(predCanadjoin);
        
        Predicate predMustadjoin = new Predicate(); predMustadjoin.setLabel("mustadjoin");
        predMustadjoin.addVariable("?x", "category"); predMustadjoin.addVariable("?y", "syntaxnode");
        domain.addPredicate(predMustadjoin);
        
        
        domain.addConstant("root", "syntaxnode");
    }
    
    
    /**
    * Computes the domain of the PDDL planning problem.  In particular, this method
    * generates the actions.
    *
    * @param grammar The grammar from which actions are generated
    */
    private static void computeDomain(Domain domain, Problem problem, Grammar<Term> grammar) {
        Map<String,HashSet<String>> roles = new HashMap<String, HashSet<String>>();
        
        // for each tree in the grammar
        for(String treeName : grammar.getAllTreeNames() ) {
            
            domain.addConstant(treeName, "treename");
            
            // Get all nodes in the tree
            ElementaryTree<Term> tree = grammar.getTree(treeName);
            Collection<String> allNodeIds = tree.getAllNodes();            
            
            // store list of roles in each tree in a map by name
            HashSet<String> localRoles = new HashSet<String>();            
            for (String node : allNodeIds) {
            	if( tree.getNodeDecoration(node) != null ) {
            		localRoles.add(tree.getNodeDecoration(node).toString());
            	}
            }            
            roles.put(treeName, localRoles);
            
            
            
        }
        
        System.out.println(roles);
        
        // compute actions from lexical entries
        for (String word : grammar.getAllWords())  {
            System.out.println(">> "+word);
            Collection<LexiconEntry> entries = grammar.getLexiconEntries(word);
            System.out.println(entries.size());
            for (LexiconEntry entry: entries){
                // Get the tree for this lexical entry from the hash map
                String treeRef = entry.tree;
                ElementaryTree<Term> tree = grammar.getTree(entry.tree);
                
                Collection<String> allNodes = tree.getAllNodes();                                
                
                // Get lists of nodes that are open for substitution and adjunction
                ArrayList<String> substNodes = new ArrayList<String>();
                ArrayList<String> adjNodes = new ArrayList<String>();                                    
                
                for (String node : allNodes) {
                    if (tree.getNodeType(node) == NodeType.SUBSTITUTION) {
                        substNodes.add(node);
                    } else {
                        if (tree.getNodeConstraint(node) != Constraint.NO_ADJUNCTION && (tree.getNodeDecoration(node) != null)) {
                            adjNodes.add(node);            
                        }
                    }        
                }
                
                StringWriter actionNameBuf = new StringWriter();
                actionNameBuf.write(treeRef);
                actionNameBuf.write("-");
                actionNameBuf.write(entry.word);
                
                for (String lex : entry.auxLexicalItems.keySet()){                
                    actionNameBuf.write("-");
                    actionNameBuf.write(entry.auxLexicalItems.get(lex));     
                }
                
                String actionName = actionNameBuf.toString();
                
                System.out.println(actionName);
                
                String rootCategory = tree.getNodeLabel(tree.getRoot());
                
                for ( int i = 1; i <= plansize; i++ ) { 
                    domain.addConstant("step" + i, "stepindex");
                    
                    Predicate pred = new Predicate();
                    List<Goal> goals = new ArrayList<Goal>();
                    List<Effect> effects = new ArrayList<Effect>();
                    
                    // compute n and I as in the paper
                    Map<String, String> n = new HashMap<String, String>();
                    Map<String, String> I = new HashMap<String, String>();
                    int roleno = 1;
                    
                    for ( String role : roles.get(treeRef)) {
                        if ( role.equals("self") )   
                            n.put(role,"?u");
                        else 
                            n.put(role, role + "-" + i);
                        
                        I.put(n.get(role), "?x" + (roleno++));
                    }
                    
                    // compute the predicate
                    pred.setLabel(actionName + "-" + i);
                    pred.addVariable("?u", "syntaxnode");
                    for ( String role : roles.get(treeRef) ) 
                        pred.addVariable(I.get(n.get(role)), "individual");
                    
                    // count the step
                    goals.add(new crisp.planningproblem.goal.Literal("step(step" + i + ")", true));
                    effects.add(new crisp.planningproblem.effect.Literal("step(step" + i + ")", false));
                    effects.add(new crisp.planningproblem.effect.Literal("step(step" + (i+1) + ")", true));
                    
                    // require reference from u to the parameter for role self 
                    goals.add(new crisp.planningproblem.goal.Literal("referent(?u,"+I.get("?u")+")", true));
                    
                    if (grammar.getTree(treeRef).getType() == ElementaryTreeType.INITIAL) {
                        // initial tree: fills substitution node
                        goals.add(new crisp.planningproblem.goal.Literal("subst(" + rootCategory + ", ?u)", true));
                        effects.add(new crisp.planningproblem.effect.Literal("subst(" + rootCategory + ", ?u)", false));
                    } else {
                        // auxiliary tree: adjoin, and satisfies mustadjoin requirements
                        goals.add(new crisp.planningproblem.goal.Literal("canadjoin(" + rootCategory + ", ?u)", true));
                        effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" + rootCategory + ", ?u)", false));
                    }
                    
                    // all semantic contents must be satisfied 
                    List<Term> contentWithVariables = new ArrayList<Term>();
                    boolean hasContent = false;
                    
                    for (Term semContTerm : entry.semantics){
                        Compound semContCompound = ((Compound) semContTerm);                                                                
                        Compound termWithVariables = (Compound) substituteVariablesForRoles(semContCompound, n, I);
                        
                        hasContent = true;
                        
                        domain.addPredicate(makeSemanticPredicate(semContCompound));
                        goals.add(new crisp.planningproblem.goal.Literal(termWithVariables, true));
                        
                        contentWithVariables.add(termWithVariables);
                        
                        effects.add(new crisp.planningproblem.effect.Literal(flattenTerm(termWithVariables, "needtoexpress"), false));
                        
                        
                        if (semContCompound.getSubterms().size() > maximumArity) { 
                            maximumArity = semContCompound.getSubterms().size();
                        }
                        
                        
                        domain.addConstant(renamePredicate(semContCompound.getLabel()), "predicate");
                    }
                    
                    // TODO
                    // pragmatic requirements must be satisfied
                    // (for now, this is handled exactly like the semantic content)
                    // TODO - if that's so, why is the body of this loop different than above?
                    //for( String pragCond : entry.getPragConds() ) {
                        //    Compound term = (Compound) TermParser.parse(pragCond);
                        //    domain.addPredicate(makeSemanticPredicate(term));
                        //    goals.add(new crisp.planningproblem.goal.Literal(substituteVariablesForRoles(term, n, I), true));
                    //}
                    
                    
                    // remove distractors
                    if ( hasContent ) {
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
                    
                    // TODO
                    /* pragmatic effects
                    *for( String pragEffect : entry.getPragEffects() ) {
                        *   Compound effect = (Compound) TermParser.parse(pragEffect);
                        * 
                        *    if ( "uniqueref".equals(effect.getLabel())) {
                            *       String roleN = n.get(effect.getSubterms().get(0).toString());
                            *        TypedVariableList vars = new TypedVariableList();
                            *        vars.addItem(new Variable("?y"), "individual");
                            *
                            *        effects.add(new crisp.planningproblem.effect.Universal(vars,
                            *                new crisp.planningproblem.effect.Literal("distractor(" + roleN + ",?y)", false)));
                            *        break;
                        *    }
                    *}
                    */
                    
                    // effects for the substitution nodes
                    for (String substNode : substNodes) {
                        String role = tree.getNodeDecoration(substNode).toString();                                                       
                        String roleN = n.get(role);
                        System.out.println("    For substNode "+substNode);                            
                        System.out.println(role+" "+roleN);
                        
                        String cat = tree.getNodeLabel(substNode);
                        if (cat.equals(""))                             
                            cat = "NONE";
                        domain.addConstant(cat,"category"); // add constants for categories to the domain
                        
                        //subst 
                        effects.add(new crisp.planningproblem.effect.Literal("subst(" + cat +", "+roleN + ")", true));
                        
                        if (!role.equals("self") ) 
                            domain.addConstant(roleN, "syntaxnode");
                        
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
                        
                        /*
                        *for( String sr : entry.getSemReqs() ) {
                            *    Term term = distractorSubst.apply(substituteVariablesForRoles(TermParser.parse(sr), n, I));
                            *    domain.addPredicate(makeSemanticPredicate(term));
                            *    distractorPreconditions.add(new crisp.planningproblem.goal.Literal(term, true));
                        *}
                        */
                        
                        Goal distractorPrecondition = new crisp.planningproblem.goal.Conjunction(distractorPreconditions);
                        
                        effects.add(new crisp.planningproblem.effect.Universal(distractorQuantifierVars,
                        new crisp.planningproblem.effect.Conditional(distractorPrecondition,
                        new crisp.planningproblem.effect.Literal("distractor(" + roleN + ",?y)", true))));
                        
                    }
                    
                    // internal nodes: allow adjunction
                    for ( String adjNode : adjNodes) {
                        
                        String role = tree.getNodeDecoration(adjNode).toString();
                        String roleN = n.get(role);
                        String cat = tree.getNodeLabel(adjNode);
                        
                        if (cat.equals(""))                             
                            cat = "NONE";
                        domain.addConstant(cat,"category"); // add constants for categories to the domain
                        
                        // canadjoin
                        effects.add(new crisp.planningproblem.effect.Literal("canadjoin(" + cat + ", " + roleN + ")", true));
                        
                        // mustadjoin                    
                        if( tree.getNodeConstraint(adjNode) == Constraint.OBLIGATORY_ADJUNCTION){ 
                            effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(" + cat + ", " + roleN + ")", true));
                            domain.registerMustadjoin(); // set mustadjoin flag
                        }
                        
                        // don't need to add constant to the domain because we ASSUME that every role
                        // except for "self" decorates some substitution node (and hence is added there)                    
                    }
                    
                    
                    HashMap<String,String> constants = null;
                    ArrayList<Predicate> predicates = null;
                    // Finally create action and add it to the domain
                    Action a = new Action(pred, new crisp.planningproblem.goal.Conjunction(goals), new crisp.planningproblem.effect.Conjunction(effects), constants, predicates);
                    domain.addAction(a);
                }
                
                domain.addConstant("step" + (plansize+1), "stepindex");
                
            }
            
        }
        
    }
    
    
    /******** input, output, main program **********/
    
    /**
    * Parses the XML document given in problemfilename, as well as the grammar file
    * referenced from that document.
    *
    * @param problemfilename
    * @throws ParserConfigurationException
    * @throws SAXException
    * @throws IOException
    * 
    * @param grammar the tree adjoining grammar to create the planning operators
    * @param problemfile abstract filename of the problemfile to parse 
    * @param domain reference to an empty planning domain, will be completed by convert.
    * @param problem reference to an empty planning problem, will be completed by convert
    */
    public static void convert(Grammar<Term> grammar, File problemfile, Domain domain, Problem problem) throws ParserConfigurationException, SAXException, IOException { 
        
        //initialize domain
        setupDomain(domain, problem);
                                
        // get the pathname where problem and grammar files are stored
        problempath = problemfile.getAbsoluteFile().getParent();
        
        SAXParserFactory factory = SAXParserFactory.newInstance();        
        
        FastCRISPConverter handler = new FastCRISPConverter(domain,problem);
        try{
            SAXParser parser = factory.newSAXParser();
            parser.parse(problemfile, handler);            
            
            Grammar<Term> filteredGrammar = new GrammarFilterer<Term>().filter(grammar, new SemanticsPredicateListFilter(handler.predicatesInWorld) );
            computeDomain(domain, problem, filteredGrammar);
            computeGoal(domain, problem);
            
        } catch (ParserConfigurationException e){
            throw new SAXException("Parser misconfigured: "+e);    
        }
        
    }
    
    
    /*********** auxiliary functions *************/
    
    
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
    * Translates XTAG style tree names into tree names that PDDL will accept.
    *
    * @param treename
    * @return
    *
    * private static String normalizeTreename(String treename) {
        *   return treename.replace("i.", "init-").replace("a.", "aux-");
    * }
    */
    
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
    * Adds all constants that occur as arguments of the term to the domain.
    *
    * @param term
    * @param domain
    */
    private static void addIndividualConstants(Term term, Domain domain) {
        if( term instanceof Compound ) {
            for( Term sub : ((Compound) term).getSubterms() ) {
                addIndividualConstants(sub, domain);
            }
        } else if( term instanceof Constant ) {
            domain.addConstant(((Constant) term).getName(), "individual");
        }
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
        for( int i = 1; i <= t.getSubterms().size(); i++ ) {
            ret.addVariable("?y" + i, "individual");
        }
        
        return ret;
    }
    
    
    public static File convertGrammarPath(String filename){
        return new File(problempath,filename);
    }
    
}
