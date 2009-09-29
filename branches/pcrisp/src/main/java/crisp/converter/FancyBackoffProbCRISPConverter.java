package crisp.converter;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.formula.Conjunction;
import crisp.planningproblem.formula.Formula;
import crisp.planningproblem.formula.Literal;

import de.saar.chorus.term.parser.TermParser;
import crisp.tools.Pair;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.grammar.LexiconEntry;
import de.saar.penguin.tag.grammar.filter.GrammarFilterer;
import de.saar.penguin.tag.grammar.filter.LexiconEntryFilter;

import de.saar.chorus.term.Term;

import de.saar.penguin.tag.grammar.ElementaryTree;
import de.saar.penguin.tag.grammar.ElementaryTreeType;
import de.saar.penguin.tag.grammar.LinearInterpolationProbabilisticGrammar;
import de.saar.penguin.tag.grammar.NodeType;
import de.saar.penguin.tag.grammar.filter.SemanticsPredicateListFilter;
import java.io.FileReader;
import java.io.Reader;
import java.io.File;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author dbauer
 */
public class FancyBackoffProbCRISPConverter extends ProbCRISPConverter {


    double init_cutoff;
    double adj_cutoff;
    double subst_cutoff;
    double noadj_cutoff;

    public FancyBackoffProbCRISPConverter(){
        this.init_cutoff = 0;
        this.adj_cutoff = 1E-3;
        this.subst_cutoff = 1E-3;
        this.noadj_cutoff = 1E-4;
    }

    /**
     * Iterator that enumerates all possible ordered pairs from two list.
     */
    protected class PairIterator<E> implements Iterator<Pair<E,E>>{

        Collection<E> list1;
        Collection<E> list2;
        Iterator<E> iter1;
        Iterator<E> iter2;
        E currentInList1;

        PairIterator(Collection<E> list1, Collection<E> list2){
            this.list1 = list1;
            this.list2 = list2;
            this.iter1 = this.list1.iterator();
            this.iter2 = this.list2.iterator();
            currentInList1 = iter1.next();
        }

        public Pair<E,E> next(){
            if (!iter2.hasNext()){
                if (!iter1.hasNext()){
                    throw new NoSuchElementException();
                }
                currentInList1 = iter1.next();
                iter2 = this.list2.iterator(); // reinitialize iterator for 2nd collection
            }
            return new Pair(currentInList1, iter2.next());
        }


        public boolean hasNext() {
            return (iter2.hasNext() || iter1.hasNext());
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

   }

    /**
     * Computes the domain of the PDDL planning problem.  In particular, this method
     * generates the actions.
     *
     * @param grammar The grammar from which actions are generated
     */
    protected void computeDomain(Domain domain, Problem problem, LinearInterpolationProbabilisticGrammar<Term> probGrammar, Grammar<Term> grammar) {
        Map<String, HashSet<String>> roles = new HashMap<String, HashSet<String>>();


        // for all trees in the gramar store all roles
        for (String treeName : grammar.getAllTreeNames()) {

            domain.addConstant(treeName, "treename");

            // Get all nodes in the tree
            ElementaryTree<Term> tree = grammar.getTree(treeName);
            Collection<String> allNodeIds = tree.getAllNodes();

            // store list of roles in each tree in a map by name
            HashSet<String> localRoles = new HashSet<String>();
            for (String node : allNodeIds) {
                if (tree.getNodeDecoration(node) != null) {
                    String role = tree.getNodeDecoration(node).toString();
                    if (role != null) {
                        localRoles.add(tree.getNodeDecoration(node).toString());
                    }
                }
            }
            roles.put(treeName, localRoles);
        }


        List<LexiconEntry> initEntries = new LinkedList<LexiconEntry>();
        List<LexiconEntry> auxEntries = new LinkedList<LexiconEntry>();
        for(String word : grammar.getAllWords()) {
            Collection<LexiconEntry> entries = grammar.getLexiconEntries(word);
            for (LexiconEntry entry : entries ){
                if (grammar.getTree(entry.tree).getType() == ElementaryTreeType.INITIAL) {
                    initEntries.add(entry);
                } else {
                    auxEntries.add(entry);
                }
            }
        }


        for (LexiconEntry parentEntry : initEntries) {
            //System.out.println(parentEntry);
            ElementaryTree<Term> parentTree = grammar.getTree(parentEntry.tree);

            double initProb = probGrammar.getSmoothedInitProbability(parentEntry);
            if (initProb > init_cutoff) {
                Action initAction = PCrispActionCreator.createInitAction(probGrammar, parentEntry, initProb, roles);
                addActionToDomain(initAction, domain);
            } 

            for (String node : parentTree.getAllNodes()) {

                if (parentTree.getNodeType(node) == NodeType.SUBSTITUTION)  {
                    for (LexiconEntry childEntry : initEntries) {
                        double substProbability = probGrammar.getSmoothedSubstitutionProbability(parentEntry, node, childEntry);
                        
                        if (substProbability > subst_cutoff) {                            
                            Collection<Action> substActions = PCrispActionCreator.createActions(probGrammar, parentEntry, node, childEntry, TagActionType.SUBSTITUTION, substProbability, plansize, roles);
                            addActionsToDomain(substActions, domain);
                        }
                    }
                } else {

                    for (LexiconEntry childEntry : auxEntries) {
                        double adjProbability = probGrammar.getSmoothedAdjunctionProbability(parentEntry, node, childEntry);
                        
                        if (adjProbability > adj_cutoff) {
                            //System.out.println("adj "+parentEntry.tree+" "+parentEntry.word+ " : "+childEntry.tree + " "+childEntry.word+" at "+node+" : "+adjProbability );
                            Collection<Action> adjActions = PCrispActionCreator.createActions(probGrammar, parentEntry, node, childEntry, TagActionType.ADJUNCTION, adjProbability, plansize, roles);
                            addActionsToDomain(adjActions, domain);
                        }
                    }

                    double noAdjProbability = probGrammar.getSmoothedNoAdjunctionProbability(parentEntry, node);
                    if (noAdjProbability > noadj_cutoff) {
                        Action noAdjoinAction = PCrispActionCreator.createNoAdjoinAction(parentEntry, node, noAdjProbability, plansize);
                        addActionToDomain(noAdjoinAction, domain);
                    }
                }

            }
        }

        //System.out.println("Auxiliary trees...");
        for (LexiconEntry parentEntry : auxEntries) {
            ElementaryTree<Term> parentTree = grammar.getTree(parentEntry.tree);

            for (String node : parentTree.getAllNodes()) {

                if (parentTree.getNodeType(node) == NodeType.SUBSTITUTION)  {
                    for (LexiconEntry childEntry : initEntries) {
                        double substProbability = probGrammar.getSmoothedSubstitutionProbability(parentEntry, node, childEntry);
                        if (substProbability > subst_cutoff) {
                            Collection<Action> substActions = PCrispActionCreator.createActions(probGrammar, parentEntry, node, childEntry, TagActionType.SUBSTITUTION, substProbability, plansize, roles);
                            addActionsToDomain(substActions, domain);
                        }
                    }
                } else {

                    for (LexiconEntry childEntry : auxEntries) {
                        double adjProbability = probGrammar.getSmoothedAdjunctionProbability(parentEntry, node, childEntry);
                        if (adjProbability > adj_cutoff) {
                            Collection<Action> adjActions = PCrispActionCreator.createActions(probGrammar, parentEntry, node, childEntry, TagActionType.ADJUNCTION, adjProbability, plansize, roles);
                            addActionsToDomain(adjActions, domain);
                        }
                    }

                    double noAdjProbability = probGrammar.getSmoothedNoAdjunctionProbability(parentEntry, node);                    
                    if (noAdjProbability > noadj_cutoff) {
                        Action noAdjoinAction = PCrispActionCreator.createNoAdjoinAction(parentEntry, node, noAdjProbability, plansize);
                        addActionToDomain(noAdjoinAction, domain);
                    }

                }

            }
        }




        // Add dummy action, needed to sidestep a LAMA bug
        ArrayList<Formula> preconds = new ArrayList<Formula>();
        preconds.add(new Literal("step(step0)",true));
        ArrayList<Formula> effects = new ArrayList<Formula>();
        HashMap<String,String> constants = new HashMap<String,String>();
        Map<Compound, List<String>> predicates = new HashMap<Compound, List<String>>();

        domain.addConstant("dummyindiv", "individual");
        domain.addConstant("dummypred", "predicate");
        domain.addConstant("dummynodetype", "nodetype");
        domain.addConstant("dummysyntaxnode", "syntaxnode");
        domain.addConstant("dummytree", "treename");

        effects.add(new Literal("referent(dummysyntaxnode, dummyindiv)",false));
        effects.add(new Literal("distractor(dummysyntaxnode, dummyindiv)",false));
        effects.add(new Literal("subst(dummytree, dummynodetype, dummysyntaxnode)",false));
        //effects.add(new Literal("canadjoin(dummytree, dummynodetype, dummysyntaxnode)",false));
        effects.add(new Literal("mustadjoin(dummytree, dummynodetype, dummysyntaxnode)",false));
        for(int i=1; i <= maximumArity; i++ ) {
            List<Term> subterms = new ArrayList<Term>();
            subterms.add(new Constant("dummypred"));
            for (int j=1; j<=i; j++){
                subterms.add(new Constant("dummyindiv"));
            }
            Compound c = new Compound("needtoexpress-"+i, subterms);
            effects.add(new Literal(c,false));
        }


        Action dummyAction = new Action(new Compound("dummy", new ArrayList<Term>()), new ArrayList<String>(),
                                            new Conjunction(preconds),
                                            new Conjunction(effects),
                                            1.0, constants, predicates);
        domain.addAction(dummyAction);

        problem.addToInitialState(TermParser.parse("referent(dummysyntaxnode, dummyindiv)"));
        problem.addToInitialState(TermParser.parse("distractor(dummysyntaxnode, dummyindiv)"));
        problem.addToInitialState(TermParser.parse("subst(dummytree, dummynodetype, dummysyntaxnode)"));
        //problem.addToInitialState(TermParser.parse("canadjoin(dummytree, dummynodetype, dummysyntaxnode)"));
        problem.addToInitialState(TermParser.parse("mustadjoin(dummytree, dummynodetype, dummysyntaxnode)"));

        for(int i=1; i <= maximumArity; i++ ) {
            List<Term> subterms = new ArrayList<Term>();
            subterms.add(new Constant("dummypred"));
            for (int j=1; j<=i; j++){
                subterms.add(new Constant("dummyindiv"));
            }
            Compound c = new Compound("needtoexpress-"+i, subterms);
            problem.addToInitialState(c);
        }

    }

    
    public LinearInterpolationProbabilisticGrammar<Term> filterProbabilisticGrammar(ProbabilisticGrammar<Term> grammar, SemanticsPredicateListFilter filter) {
       Grammar<Term> filteredGrammar = new GrammarFilterer<Term>().filter(grammar, filter);

       if (! filter.allPredicatesSatisfied()) {
           throw new RuntimeException("Some semantic predicate cannot be realized by the grammar.");
       }

       LinearInterpolationProbabilisticGrammar<Term> ret = new LinearInterpolationProbabilisticGrammar(1,1,1000);

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

            ret.setAdjChildPrior(entry, grammar.getAdjChildPrior(entry));
            ret.setSubstChildPrior(entry, grammar.getSubstChildPrior(entry));

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
                
                ret.setAdjParentPrior(entry, node, grammar.getAdjParentPrior(entry, node));
                ret.setSubstParentPrior(entry, node, grammar.getSubstParentPrior(entry, node));

            }
        }

        ret.initBackoff();
        return ret;
    }


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
    public void convert_backoff(LinearInterpolationProbabilisticGrammar<Term> grammar, Reader problemfile, Domain domain, Problem problem) throws Exception {

        //initialize domain
        setupDomain(domain, problem);

        // get the pathname where problem and grammar files are stored
        //problempath = problemfile.getAbsoluteFile().getParent();

        SAXParserFactory factory = SAXParserFactory.newInstance();

        ProblemfileHandler handler = new ProblemfileHandler(domain,problem);

        try{
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(problemfile), handler);

            SemanticsPredicateListFilter filter = new SemanticsPredicateListFilter(handler.getPredicatesInWorld());
            Grammar<Term> filteredGrammar = new GrammarFilterer<Term>().filter(grammar, filter);

            if (!filter.allPredicatesSatisfied()) {
                throw new RuntimeException("Some semantic predicates cannot be realized by this grammar.");
            }


            computeDomain(domain, problem, grammar, filteredGrammar);
            computeGoal(domain, problem);

        } catch (ParserConfigurationException e){
            throw new SAXException("Parser misconfigured: "+e);
        }
        problem.setName(domain.getName());
        System.out.println("PROBLEMNAME: "+problem.getName());

    }

    @Override
    public void convert(Grammar<Term> grammar, File problemfile, Domain domain, Problem problem) throws Exception {
        convert(grammar, new FileReader(problemfile), domain, problem);

    }

}