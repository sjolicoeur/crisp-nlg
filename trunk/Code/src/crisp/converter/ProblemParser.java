package crisp.converter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
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

import crisp.converter.grammar.TAGrammar;
import crisp.converter.grammar.TAGTree;
import crisp.converter.grammar.TAGNode;
import crisp.converter.grammar.TAGLeaf;
import crisp.converter.grammar.TAGLexEntry;

/**
* This class provides a fast converter from XML CRISP problem descriptions to
* PDDL domains and problems.
* This parser is implemented as a plain SAX parser, thereby improving
* processing speed and decreasing memory requirements over the old xpath based
* parser, which is particularly important for parsing large grammar 
* description files.
*/

public class ProblemParser extends DefaultHandler {  // Default Handler already does a lot of work like
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
    
    private TAGrammar grammar;
    
    private Set<Term> trueAtoms;
    
    
    /************************ Methods for the content handler *****************/
    
    
    // All of these methods are specified in the ContentHandler interface.    
    
    public void startDocument() throws SAXException {
        characterBuffer = new StringWriter();
    }
    
    public void startElement(String namespaceURI, String localName, String qName, 
    Attributes atts) throws SAXException {
        
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
            
            // Domain is now precomputed.
            /*try {
                grammar = GrammarParser.parseGrammar(convertGrammarPath(atts.getValue("grammar")));
            } catch (ParserConfigurationException e) {
                throw new SAXParseException("Couldn't initialize grammar parse.",null); 
            } catch (IOException e) {
                throw new SAXParseException("Couldn't open grammar file.",null);
            }*/
            
            // add Index 
            domain.addConstant(atts.getValue("index"),"individual");            
            
            // This was in computeInitialState(Domain domain, Problem problem)
            problem.addToInitialState(TermParser.parse("subst(root,none, init)"));
            problem.addToInitialState(TermParser.parse("referent(init, " + 
            atts.getValue("index") + ")"));
            // TODO: maybe there is a better place for this
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
            throw new SAXParseException("Cannot close "+qName+
        " here. Expected "+lastElement+".",null);
        
        if (qName.equals("world")){ // Term definition ends here 
            Term term = TermParser.parse(characterBuffer.toString()); // parse the Term
            
            // This was in computeInitialState(Domain domain, Problem problem)
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
                problem.registerComgoalArity(arity);
                
                domain.addConstant(renamePredicate(c.getLabel()), "predicate");                               
                
                problem.addToInitialState(flattenTerm(c, "needtoexpress"));
            }
            
        }
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
        Goal noMustAdj= new crisp.planningproblem.goal.Universal(tlCatNode,
        new crisp.planningproblem.goal.Literal("mustadjoin(?a,?u)", false));
        
        finalStateGoals.add(noSubst);
        finalStateGoals.add(noDistractors);
        finalStateGoals.add(noMustAdj);
        
        // no positive needtoexpress-* literals, for any arity
        for( int i = 1; i <= maximumArity; i++ ) {
            TypedVariableList tlPredicate = new TypedVariableList();
            tlPredicate.addItem(new Variable("?P"), "predicate");
            
            Predicate predNTE = new Predicate();
            predNTE.setLabel("needtoexpress-" + i);
            predNTE.addVariable("?P", "predicate");
            
            List<Term> subterms = new ArrayList<Term>();
            subterms.add(new Variable("?P"));
            
            for( int j = 1; j <= i; j++ ) {
                tlPredicate.addItem(new Variable("?x" + j), "individual");
                subterms.add(new Variable("?x" + j));
                
                predNTE.addVariable("?x" + j, "individual");
            }
            
            finalStateGoals.add(new crisp.planningproblem.goal.Universal(tlPredicate,
            new crisp.planningproblem.goal.Literal(new Compound("needtoexpress-" + i, subterms), false)));
            
            domain.addPredicate(predNTE);
        }
        
        
        problem.setGoal(new crisp.planningproblem.goal.Conjunction(finalStateGoals));
    }
    
    
    
    /******** input, output, main program **********/
    
    public ProblemParser(Domain domain, Problem problem){
        this.domain = domain;
        this.problem = problem;
    };
    
   /**
    * Parses the XML document given in problemfilename and creates 
    * a problem over the given planning domain.
    *
    * @param problemfile The file to parse.
    * @throws ParserConfigurationException
    * @throws SAXException
    * @throws IOException
    */
    public static Problem parseProblem(File problemfile, Domain domain) throws ParserConfigurationException, SAXException, IOException { 
        
        // get the pathname where problem and grammar files are stored
        problempath = problemfile.getAbsoluteFile().getParent();
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        
        Problem newproblem = new Problem();
        newproblem.clear();
        
        ProblemParser handler = new ProblemParser(domain, newproblem);
        try{
            SAXParser parser = factory.newSAXParser();
            parser.parse(problemfile, handler);
        } catch (ParserConfigurationException e){
            throw new SAXException("Parser misconfigured: "+e);    
        }
        
        return newproblem;        
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
    */
    private static String normalizeTreename(String treename) {
        return treename.replace("i.", "init-").replace("a.", "aux-");
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
