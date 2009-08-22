package crisp.converter;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.DurativeAction;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.effect.Effect;
import crisp.planningproblem.goal.Goal;
import crisp.termparser.TermParser;
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
public class TreeModelProbCRISPConverter extends ProbCRISPConverter {

    /**
     * Iterator that enumerates all possible ordered pairs from two list.
     */
    private class PairIterator<E> implements Iterator<Pair<E,E>>{

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
    protected void computeDomain(Domain domain, Problem problem, ProbabilisticGrammar<Term> grammar) {
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


        // Create a mapping from trees to lexical entries with this tree
        Map<String, List<LexiconEntry>> treesToEntries = new HashMap<String, List<LexiconEntry>>();
        for (String word : grammar.getAllWords()) {
            Collection<LexiconEntry> entries = grammar.getLexiconEntries(word);
            for (LexiconEntry entry : entries) {
                if (!treesToEntries.containsKey(entry.tree)) {
                    treesToEntries.put(entry.tree, new ArrayList<LexiconEntry>());                    
                }
                treesToEntries.get(entry.tree).add(entry);
            }
        }


        for (String tree : treesToEntries.keySet()) {
            

            // add init actions for all entries with this tree
            if (grammar.hasTreeInitProbability(tree)) {
                for (LexiconEntry entry : treesToEntries.get(tree)) {                    
                    Action initAction = PCrispActionCreator.createInitAction(grammar, entry, grammar.getTreeInitProbability(tree), roles);
                    addActionToDomain(initAction, domain);                    
                }

            }

            for (String node : grammar.getTree(tree).getAllNodes()) {

                Map<String, Double> substProbs =
                        grammar.getTreeSubstitutionProbabilities(tree, node);
                for (String childTree : substProbs.keySet()) {

                    Collection<LexiconEntry> parentEntries = treesToEntries.get(tree);
                    Collection<LexiconEntry> childEntries = treesToEntries.get(childTree);

                    Double prob = substProbs.get(childTree);
                    PairIterator<LexiconEntry> pairIter = new PairIterator<LexiconEntry>(parentEntries, childEntries);
                    while (pairIter.hasNext()) {
                        Pair<LexiconEntry, LexiconEntry> pair = pairIter.next();
                        LexiconEntry parentEntry = pair.getFirst();
                        LexiconEntry childEntry = pair.getSecond();
                        Collection<Action> substActions =
                                PCrispActionCreator.createActions(grammar, parentEntry, node, childEntry, TagActionType.SUBSTITUTION, prob, plansize, roles);
                        addActionsToDomain(substActions, domain);
                    }

                }


                Map<String, Double> adjProbs =
                        grammar.getTreeAdjunctionProbabilities(tree, node);
                for (String childTree : adjProbs.keySet()) {
                    
                    Collection<LexiconEntry> parentEntries = treesToEntries.get(tree);
                    Collection<LexiconEntry> childEntries = treesToEntries.get(childTree);                    

                    Double prob = adjProbs.get(childTree);

                    PairIterator<LexiconEntry> pairIter = new PairIterator<LexiconEntry>(parentEntries, childEntries);
                    while (pairIter.hasNext()) {
                        Pair<LexiconEntry,LexiconEntry> pair = pairIter.next();                        
                        LexiconEntry parentEntry = pair.getFirst();
                        LexiconEntry childEntry = pair.getSecond();                        
                        Collection<Action> adjActions =
                                PCrispActionCreator.createActions(grammar, parentEntry, node, childEntry, TagActionType.ADJUNCTION, prob, plansize, roles);
                        addActionsToDomain(adjActions, domain);
                    }

                }

                if (grammar.hasTreeNoadjoinProbability(tree, node)) {
                    Double noadjoinProb = grammar.getTreeNoadjoinProbability(tree, node);
                    for (LexiconEntry entry : treesToEntries.get(tree)) {
                        Action noadjoinAction = PCrispActionCreator.createNoAdjoinAction(entry, node, noadjoinProb, plansize);
                        addActionToDomain(noadjoinAction, domain);
                    }
                }


            }
        }


        // Add dummy action, needed to sidestep a LAMA bug
        ArrayList<Goal> preconds = new ArrayList<Goal>();
        preconds.add(new crisp.planningproblem.goal.Literal("step(step0)", true));
        ArrayList<Effect> effects = new ArrayList<Effect>();
        HashMap<String, String> constants = new HashMap<String, String>();
        List<Predicate> predicates = new ArrayList<Predicate>();

        domain.addConstant("dummyindiv", "individual");
        domain.addConstant("dummypred", "predicate");
        domain.addConstant("dummynodetype", "nodetype");
        domain.addConstant("dummysyntaxnode", "syntaxnode");
        domain.addConstant("dummytree", "treename");

        effects.add(new crisp.planningproblem.effect.Literal("referent(dummysyntaxnode, dummyindiv)", false));
        effects.add(new crisp.planningproblem.effect.Literal("distractor(dummysyntaxnode, dummyindiv)", false));
        effects.add(new crisp.planningproblem.effect.Literal("subst(dummytree, dummynodetype, dummysyntaxnode)", false));
        effects.add(new crisp.planningproblem.effect.Literal("canadjoin(dummytree, dummynodetype, dummysyntaxnode)", false));
        effects.add(new crisp.planningproblem.effect.Literal("mustadjoin(dummytree, dummynodetype, dummysyntaxnode)", false));
        for (int i = 1; i <= maximumArity; i++) {
            List<Term> subterms = new ArrayList<Term>();
            subterms.add(new Constant("dummypred"));
            for (int j = 1; j <= i; j++) {
                subterms.add(new Constant("dummyindiv"));
            }
            Compound c = new Compound("needtoexpress-" + i, subterms);
            effects.add(new crisp.planningproblem.effect.Literal(c, false));
        }


        DurativeAction dummyAction = new DurativeAction(new Predicate("dummy"),
                new crisp.planningproblem.goal.Conjunction(preconds),
                new crisp.planningproblem.effect.Conjunction(effects),
                constants, predicates, 0);
        domain.addAction(dummyAction);

        problem.addToInitialState(TermParser.parse("referent(dummysyntaxnode, dummyindiv)"));
        problem.addToInitialState(TermParser.parse("distractor(dummysyntaxnode, dummyindiv)"));
        problem.addToInitialState(TermParser.parse("subst(dummytree, dummynodetype, dummysyntaxnode)"));
        problem.addToInitialState(TermParser.parse("canadjoin(dummytree, dummynodetype, dummysyntaxnode)"));
        problem.addToInitialState(TermParser.parse("mustadjoin(dummytree, dummynodetype, dummysyntaxnode)"));

        for (int i = 1; i <= maximumArity; i++) {
            List<Term> subterms = new ArrayList<Term>();
            subterms.add(new Constant("dummypred"));
            for (int j = 1; j <= i; j++) {
                subterms.add(new Constant("dummyindiv"));
            }
            Compound c = new Compound("needtoexpress-" + i, subterms);
            problem.addToInitialState(c);
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



        for (String word : filteredGrammar.getAllWords()) {
            for (LexiconEntry entry : filteredGrammar.getLexiconEntries(word)) {
                filteredEntries.add(entry);
                ret.addLexiconEntry(entry.word, entry.tree, entry.auxLexicalItems, entry.semantics);
            }
        }


        // Copy probabilities
        Set<String> treeSet = new HashSet<String>();
        for (LexiconEntry entry : filteredEntries) {
            treeSet.add(entry.tree);
        }

        for (String tree : treeSet) {
            if (grammar.hasTreeInitProbability(tree)) {
                ret.setTreeInitProbability(tree, grammar.getTreeInitProbability(tree));
            }

            for (String node : grammar.getTree(tree).getAllNodes()) {
                if (grammar.hasTreeNoadjoinProbability(tree, node)) {
                    ret.setTreeNoadjoinProbability(tree, node, grammar.getTreeNoadjoinProbability(tree, node));
                }

                Map<String, Double> substProbs = grammar.getTreeSubstitutionProbabilities(tree, node);
                for (String child : substProbs.keySet()) {
                    if (treeSet.contains(child)) {
                        ret.setTreeSubstitutionProbability(tree, node, child, substProbs.get(child));
                    }
                }

                Map<String, Double> adjoinProbs = grammar.getTreeAdjunctionProbabilities(tree, node);
                for (String child : adjoinProbs.keySet()) {
                    if (treeSet.contains(child)) {
                        ret.setTreeAdjunctionProbability(tree, node, child, adjoinProbs.get(child));
                    }
                }
            }
        }

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
                SemanticsPredicateListFilter(handler.getPredicatesInWorld()));
            computeDomain(domain, problem, filteredGrammar);
            computeGoal(domain, problem);

        } catch (ParserConfigurationException e){
            throw new SAXException("Parser misconfigured: "+e);
        }

    }

    public void convert(Grammar<Term> grammar, File problemfile, Domain domain, Problem problem) throws Exception {
        convert(grammar, new FileReader(problemfile), domain, problem);

    }

}
