package crisp.converter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

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
import de.saar.penguin.tag.grammar.Constraint;
import de.saar.penguin.tag.grammar.ElementaryTree;
import de.saar.penguin.tag.grammar.ElementaryTreeType;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.grammar.LexiconEntry;
import de.saar.penguin.tag.grammar.NodeType;
import de.saar.penguin.tag.grammar.filter.GrammarFilterer;
import de.saar.penguin.tag.grammar.filter.SemanticsPredicateListFilter;
import de.saar.penguin.tag.grammar.filter.LexiconEntryFilter;


/**
* This class provides a fast converter from XML CRISP problem descriptions to
* planning domains with costs and problems using a probabilistic TAG grammar. 
*/

public class ProbCRISPConverter implements ProblemConverter {
                                                          // Default Handler already does a lot of work like
                                                          // parse error handling and registering the handler
                                                          // with a parser.                
    
    private int plansize;         // the maximum plan size as specified in the problem file
    private int maximumArity = 0; // the maximum arity of any predicate in the problem file
    
    private String problemname;   // the name for the problem as specified in the problem file 
    
    private String mainCat;       // main category for the problem in the problem file      
    
    
    private class ProblemfileHandler extends DefaultHandler {
        // Member variables for instances of the Content Handler
        private Stack<String> elementStack = new Stack<String>(); 
        private StringWriter characterBuffer;
        
        private Problem problem;
        private Domain domain;
        
        private Set<Term> trueAtoms;
        
        private Map<String,Set<Integer>> predicatesInWorld;
        
        
        /************************ Methods for the content handler *****************/    
        public ProblemfileHandler(Domain aDomain, Problem aProblem) {
            problem = aProblem;
            domain = aDomain;
            predicatesInWorld = new HashMap<String, Set<Integer>>();
        }
        
        
        // All of these methods are specified in the ContentHandler interface.    
        @Override
        public void startDocument() throws SAXException {
            characterBuffer = new StringWriter();
        }
        
        
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            
            elementStack.push(qName);
            
            // Namespace is ingored for now, use prefixed name and assume prefixes are empty.
            if (qName.equals("crispproblem")){
                
                // Retrieve and set name for the problem
                problemname = atts.getValue("name");  
                domain.setName(problemname);
                problem.setName(problemname);
                problem.setDomain(domain);
                
                try {
                    plansize = Integer.parseInt(atts.getValue("plansize"));
                } catch (NumberFormatException e){
                    throw new SAXParseException("Expecting integer number in plansize attribute.",null);
                }
                
                domain.addConstant(atts.getValue("index"),"individual");
                
                mainCat = atts.getValue("cat"); // TODO: do we really need this as a member variable?
                domain.addConstant(mainCat,"category");
                
                // This was in computeInitialState(Domain domain, Problem problem)
                problem.addToInitialState(TermParser.parse("subst(root, none, init)"));
                problem.addToInitialState(TermParser.parse("referent(init, " + atts.getValue("index") + ")"));                        
                problem.addToInitialState(TermParser.parse("step(step0)"));                                     
                
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
                throw new SAXParseException("Cannot close " + qName + " here. Expected "+lastElement+".",null);
            
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
        
        
        public void characters(char[] ch, int start, int length) {                        
            
            characterBuffer.write(ch, start, length);
            
        } 
    }
    
    /*********************** Compute goal and domain  *****************/
    
    /**
    * Compute the goal specification for the given CRISP problem.  This becomes the
    * "goal" clause in the PDDL problem.
    *
    * @param domain
    * @param problem
    */
    private void computeGoal(Domain domain, Problem problem) {

      TypedVariableList tlNodeIndiv = new TypedVariableList();
      tlNodeIndiv.addItem(new Variable("?u"), "syntaxnode");
      tlNodeIndiv.addItem(new Variable("?x"), "individual");

      TypedVariableList tlCatNode = new TypedVariableList();
      tlCatNode.addItem(new Variable("?t"), "treename");
      tlCatNode.addItem(new Variable("?n"), "nodetype");
      tlCatNode.addItem(new Variable("?u"), "syntaxnode");

        // collect all goals in this list
        List<Goal> finalStateGoals = new ArrayList<Goal>();

        // no positive "subst" literals in the goal state
        Goal noSubst = new crisp.planningproblem.goal.Universal(tlCatNode,
                new crisp.planningproblem.goal.Literal("subst(?t,?n,?u)", false));

                
        
        // no positive "distractor" literals in the goal state
        Goal noDistractors = new crisp.planningproblem.goal.Universal(tlNodeIndiv,
                new crisp.planningproblem.goal.Literal("distractor(?u,?x)", false));

        // TODO
        // no positive "mustadjoin" literals in the goal state
        //   this is only added if there is an action that creates a mustadjoin constraint
        //   because otherwise the LAMA planner cannot handle universal preconditions 
        //   involving this predicate
        //if (domain.sawMustadjoin()){ 
            Goal noMustAdj= new crisp.planningproblem.goal.Universal(tlCatNode,
                new crisp.planningproblem.goal.Literal("mustadjoin(?t, ?n, ?u)", false));
            finalStateGoals.add(noMustAdj);
        //}

        finalStateGoals.add(noSubst);
        finalStateGoals.add(noDistractors);
        
        
        // no positive needtoexpress-* literals, for any arity used in the communicative 
        // goals. If we would just do this for all arities the LAMA planner cannot handle 
        // the universal precondition involving needtoexpress predicates that do not occur        
        // elsewhere as an effect        
        for( Integer i : problem.getComgoalArities()) {
            //System.out.println("Found needtoexpress for arity "+i.toString());
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
    * domain information cannot be set here (e.g. constants, predicates, 
    * maximum plan length etc.)
    *
    * @param domain
    * @param problem
    */
    private void setupDomain(Domain domain, Problem problem) {
        domain.clear();
        problem.clear();

        domain.addRequirement(":strips");
        domain.addRequirement(":equality");
        domain.addRequirement(":typing");
        domain.addRequirement(":conditional-effects");
        domain.addRequirement(":quantified-preconditions");        


        domain.addSubtype("individual", "object");
        domain.addSubtype("category", "object");
        domain.addSubtype("syntaxnode", "object");
        domain.addSubtype("stepindex", "object");
        domain.addSubtype("predicate", "object");
        domain.addSubtype("rolename", "object");
        domain.addSubtype("treename", "object");
        domain.addSubtype("nodetype", "object"); // Identifiers for nodes in trees (tree types)

        Predicate predSubst = new Predicate(); predSubst.setLabel("subst");
        predSubst.addVariable("?t", "treename");
        predSubst.addVariable("?x", "nodetype"); predSubst.addVariable("?y", "syntaxnode");
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
        predCanadjoin.addVariable("?t", "treename"); predCanadjoin.addVariable("?x","nodetype"); 
        predCanadjoin.addVariable("?y", "syntaxnode");
        domain.addPredicate(predCanadjoin);

        Predicate predMustadjoin = new Predicate(); predMustadjoin.setLabel("mustadjoin");
        predMustadjoin.addVariable("?t", "treename"); predMustadjoin.addVariable("?x","nodetype"); 
        predMustadjoin.addVariable("?y", "syntaxnode");
        domain.addPredicate(predMustadjoin);
        
        domain.addConstant("S","category");
        domain.addConstant("init","syntaxnode");
        domain.addConstant("root","treename");
    }
    
    
    /**
    * Computes the domain of the PDDL planning problem.  In particular, this method
    * generates the actions.
    *
    * @param grammar The grammar from which actions are generated
    */
    private void computeDomain(Domain domain, Problem problem, ProbabilisticGrammar<Term> grammar) {
        Map<String,HashSet<String>> roles = new HashMap<String, HashSet<String>>();
        
        // for all trees in the gramar store all roles 
        for(String treeName : grammar.getAllTreeNames() ) {
            
            domain.addConstant(treeName, "treename");
            
            // Get all nodes in the tree
            ElementaryTree<Term> tree = grammar.getTree(treeName);
            Collection<String> allNodeIds = tree.getAllNodes();            
            
            // store list of roles in each tree in a map by name
            HashSet<String> localRoles = new HashSet<String>();            
            for (String node : allNodeIds) {
            	if( tree.getNodeDecoration(node) != null ) {
                    String role = tree.getNodeDecoration(node).toString();
                    if (role!=null) { 
                        localRoles.add(tree.getNodeDecoration(node).toString());
                    }
            	}
            }                        
            roles.put(treeName, localRoles);                                    
        }
        
        for(String word : grammar.getAllWords()) {
            Collection<LexiconEntry> entries = grammar.getLexiconEntries(word);            
            
            for (LexiconEntry parentEntry : entries) {
                ElementaryTree<Term> parentTree = grammar.getTree(parentEntry.tree);                
                        
                                
                // Add init action for this entry
                if (grammar.hasInitProbability(parentEntry)) {  
                    Action initAction = PCrispActionCreator.createInitAction(grammar, parentEntry, grammar.getInitProbability(parentEntry), roles);
                    addActionToDomain(initAction, domain);
                }                                  
                
                for (String node : parentTree.getAllNodes()) {
                    
                    //Compute actions for substitution in parentTree at this node
                    Map<LexiconEntry,Double> substProbs =            
                        grammar.getSubstitutionProbabilities(parentEntry,node);                           
                        
                    for (LexiconEntry childEntry : substProbs.keySet()) {
                        Collection<Action> substActions = 
                            PCrispActionCreator.createActions(grammar, parentEntry, node, childEntry, TagActionType.SUBSTITUTION, substProbs.get(childEntry), plansize, roles);
                        addActionsToDomain(substActions, domain);  
                    }
                    
                    //Compute actions for adjoining in parentTree at this node
                    Map<LexiconEntry, Double> adjoinProbs = grammar.getAdjunctionProbabilities(parentEntry,node);                     
                    
                    for (LexiconEntry childEntry : adjoinProbs.keySet()) {
                        Collection<Action> adjActions = 
                            PCrispActionCreator.createActions(grammar, parentEntry, node, childEntry, TagActionType.ADJUNCTION, adjoinProbs.get(childEntry), plansize, roles);
                        addActionsToDomain(adjActions, domain);
                    }
                    
                    //Compute noadjoin action for this node
                    if (grammar.hasNoadjoinProbability(parentEntry,node)) {
                        double noadjoinProb = grammar.getNoadjoinProbability(parentEntry, node);
                        Action noadjoinAction = PCrispActionCreator.createNoAdjoinAction(parentEntry, node, grammar.getNoadjoinProbability(parentEntry,node), plansize);
                        addActionToDomain(noadjoinAction, domain);
                    }
                }                                    
            }            
        }

        // Add dummy action, needed to sidestep a LAMA bug
        ArrayList<Goal> preconds = new ArrayList<Goal>();
        preconds.add(new crisp.planningproblem.goal.Literal("step(step0)",true));
        ArrayList<Effect> effects = new ArrayList<Effect>();
        HashMap<String,String> constants = new HashMap<String,String>();
        List<Predicate> predicates = new ArrayList<Predicate>();

        domain.addConstant("dummyindiv", "individual");
        domain.addConstant("dummypred", "predicate");
        domain.addConstant("dummynodetype", "nodetype");
        domain.addConstant("dummysyntaxnode", "syntaxnode");
        domain.addConstant("dummytree", "treename");
        
        effects.add(new crisp.planningproblem.effect.Literal("referent(dummysyntaxnode, dummyindiv)",false));
        effects.add(new crisp.planningproblem.effect.Literal("distractor(dummysyntaxnode, dummyindiv)",false));
        effects.add(new crisp.planningproblem.effect.Literal("subst(dummytreename, dummynodetype, dummysyntaxnode)",false));
        effects.add(new crisp.planningproblem.effect.Literal("canadjoin(dummytreename, dummynodetype, dummysyntaxnode)",false));
        effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(dummytreename, dummynodetype, dummysyntaxnode)",false));
        for(int i=1; i <= maximumArity; i++ ) {
            List<Term> subterms = new ArrayList<Term>();
            for (int j=1; j<=i; j++){
                subterms.add(new Constant("dummyindiv"));
            }
            Compound c = new Compound("needtoexpress_"+i, subterms);
            effects.add(new crisp.planningproblem.effect.Literal(c,false));
        }


        DurativeAction dummyAction = new DurativeAction(new Predicate("dummy"), 
                                            new crisp.planningproblem.goal.Conjunction(preconds),
                                            new crisp.planningproblem.effect.Conjunction(effects),
                                            constants, predicates, 0);
        domain.addAction(dummyAction);

        problem.addToInitialState(TermParser.parse("referent(dummysyntaxnode, dummyindiv)"));
        problem.addToInitialState(TermParser.parse("distractor(dummysyntaxnode, dummyindiv)"));
        problem.addToInitialState(TermParser.parse("subst(dummytreename, dummynodetype, dummysyntaxnode)"));
        problem.addToInitialState(TermParser.parse("canadjoin(dummytreename, dummynodetype, dummysyntaxnode)"));
        problem.addToInitialState(TermParser.parse("mustadjoin(dummytreename, dummynodetype, dummysyntaxnode)"));

    }
    
    
    /**
     * Add a collection of actions to a planning domain and register all constants and predicates
     * they use.
     */
    private void addActionsToDomain(Collection<Action> actions, Domain domain) {        
        for (Action action : actions) {
            addActionToDomain(action,domain);
        }
    }
    
    /**
     * Add a new action to a planning domain and register all constants and predicates it uses.
     */
    private void addActionToDomain(Action action, Domain domain){
        Map<String,String> constants = action.getDomainConstants();
        
        domain.addAction(action);
        
        // register constants used by this action
        if (constants!=null){
            for (String key : constants.keySet()) {
                domain.addConstant(key,constants.get(key));
            }
        }
        
        // register predicates used by this action
        List<Predicate> predicates = action.getDomainPredicates();
        if (predicates!=null) {
            for (Predicate pred : predicates) { 
               domain.addPredicate(pred);
            }
        }
    }
    
    
    
    
    
    public ProbabilisticGrammar<Term> filterProbabilisticGrammar(ProbabilisticGrammar<Term> grammar, LexiconEntryFilter filter) {
        Grammar<Term> filteredGrammar = new GrammarFilterer<Term>().filter(grammar, filter);                
        ProbabilisticGrammar<Term> ret = new ProbabilisticGrammar();
                
        Set<LexiconEntry> filteredEntries = new HashSet<LexiconEntry>();
                
        
        // Copy all treeNames and all lexicon entries
        for (String treeName : filteredGrammar.getAllTreeNames()) {
            ret.addTree(treeName, filteredGrammar.getTree(treeName));
        }
        
        
        
        for (String word: filteredGrammar.getAllWords()){
            for (LexiconEntry entry : filteredGrammar.getLexiconEntries(word)) {
                filteredEntries.add(entry);
                ret.addLexiconEntry(entry.word, entry.tree, entry.auxLexicalItems, entry.semantics);                
            }
        }
                
        
                
        for (LexiconEntry entry : filteredEntries) {                        
                        
            if (grammar.hasInitProbability(entry)) {                
                ret.setInitProbability(entry, grammar.getInitProbability(entry));                
            }
                
            for (String node : grammar.getTree(entry.tree).getAllNodes()) {
                if (grammar.hasNoadjoinProbability(entry,node)) {                    
                    ret.setNoadjoinProbability(entry, node, grammar.getNoadjoinProbability(entry,node));
                }
                
                Map<LexiconEntry, Double> substProbs = grammar.getSubstitutionProbabilities(entry, node);                                                    
                for (LexiconEntry child : substProbs.keySet()) {
                    if (filteredEntries.contains(child)) {
                        ret.setSubstitutionProbability(entry,node,child,substProbs.get(child)); 
                    }
                }
                
                Map<LexiconEntry, Double> adjoinProbs = grammar.getAdjunctionProbabilities(entry, node);
                for (LexiconEntry child : adjoinProbs.keySet()) {
                    if (filteredEntries.contains(child)) {
                        ret.setAdjunctionProbability(entry,node,child,adjoinProbs.get(child)); 
                    }
                }
            }                        
        }
        
        return ret;
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
    * @param problemfile a reader from which to read the problem file
    * @param domain reference to an empty planning domain, will be completed by convert.
    * @param problem reference to an empty planning problem, will be completed by convert
    */
    public void convert(Grammar<Term> grammar, Reader problemfile, Domain domain, Problem problem) throws Exception {                 
        
        //initialize domain
        setupDomain(domain, problem);
                                
        // get the pathname where problem and grammar files are stored
        //problempath = problemfile.getAbsoluteFile().getParent();
        
        SAXParserFactory factory = SAXParserFactory.newInstance();        
        
        ProblemfileHandler handler = new ProblemfileHandler(domain,problem);                                        
        
        try{
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(problemfile), handler);            
            ProbabilisticGrammar<Term> filteredGrammar = filterProbabilisticGrammar((ProbabilisticGrammar<Term>) grammar, new
                SemanticsPredicateListFilter(handler.predicatesInWorld));                                   
            computeDomain(domain, problem, filteredGrammar);            
            computeGoal(domain, problem);
            
        } catch (ParserConfigurationException e){
            throw new SAXException("Parser misconfigured: "+e);    
        }
        
    }        
    
    public void convert(Grammar<Term> grammar, File problemfile, Domain domain, Problem problem) throws Exception { 
        convert(grammar, new FileReader(problemfile), domain, problem);
        
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
    private Term substituteVariablesForRoles(Term term, Map<String, String> n, Map<String, String> I) {
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
    private Term flattenTerm(Compound t, String newLabel) {
        List<Term> subterms = new ArrayList<Term>();
        
        subterms.add(new Constant(renamePredicate(t.getLabel())));
        subterms.addAll(t.getSubterms());
        
        return new Compound(newLabel + "-" + t.getSubterms().size(), subterms);
    }
    
    private String renamePredicate(String predicate) {
        return "pred-"+predicate;
    }
    
    
    /**
    * Adds all constants that occur as arguments of the term to the domain.
    *
    * @param term
    * @param domain
    */
    private void addIndividualConstants(Term term, Domain domain) {
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
    private Predicate makeSemanticPredicate(Term term) {
        Predicate ret = new Predicate();
        Compound t = (Compound) term;
        
        ret.setLabel(t.getLabel());
        for( int i = 1; i <= t.getSubterms().size(); i++ ) {
            ret.addVariable("?y" + i, "individual");
        }
        
        return ret;
    }
    
    /*
    public static File convertGrammarPath(String filename){
        return new File(problempath,filename);
    }
    */
    
}
