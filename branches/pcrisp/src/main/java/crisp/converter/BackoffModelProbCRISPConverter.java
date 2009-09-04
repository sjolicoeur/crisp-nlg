package crisp.converter;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.DurativeAction;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.effect.Effect;
import crisp.planningproblem.goal.Goal;

import crisp.tools.Pair;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.grammar.LexiconEntry;

import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;

import de.saar.penguin.tag.grammar.ElementaryTree;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.HashSet;

/**
 *
 * @author dbauer
 */
public class BackoffModelProbCRISPConverter extends TreeModelProbCRISPConverter {

    /**
     * Computes the domain of the PDDL planning problem.  In particular, this method
     * generates the actions.
     *
     * @param grammar The grammar from which actions are generated
     */
    @Override
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

                    Double treeProb = substProbs.get(childTree);
                    PairIterator<LexiconEntry> pairIter = new PairIterator<LexiconEntry>(parentEntries, childEntries);
                    while (pairIter.hasNext()) {
                        Pair<LexiconEntry, LexiconEntry> pair = pairIter.next();
                        LexiconEntry parentEntry = pair.getFirst();
                        LexiconEntry childEntry = pair.getSecond();

                        Double prob = (grammar.hasSubstitutionProbability(parentEntry, node, childEntry) ? grammar.getSubstitutionProbability(parentEntry, node, childEntry): treeProb);
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

                    Double treeProb = adjProbs.get(childTree);
                    PairIterator<LexiconEntry> pairIter = new PairIterator<LexiconEntry>(parentEntries, childEntries);
                    while (pairIter.hasNext()) {
                        Pair<LexiconEntry,LexiconEntry> pair = pairIter.next();
                        LexiconEntry parentEntry = pair.getFirst();
                        LexiconEntry childEntry = pair.getSecond();
                        
                        Double prob = (grammar.hasAdjunctionProbability(parentEntry, node, childEntry) ? grammar.getAdjunctionProbability(parentEntry, node, childEntry) : treeProb);
                    Collection<Action> adjActions =
                       PCrispActionCreator.createActions(grammar, parentEntry, node, childEntry, TagActionType.ADJUNCTION, prob, plansize, roles);
                        
                       addActionsToDomain(adjActions, domain);
                    }

                }

                //noadjoin
                if (grammar.hasTreeNoadjoinProbability(tree, node)) {
                    Double noadjoinProbTree = grammar.getTreeNoadjoinProbability(tree, node);
                    for (LexiconEntry entry : treesToEntries.get(tree)) {
                       Double prob = (grammar.hasNoadjoinProbability(entry, node) ? grammar.getNoadjoinProbability(entry, node) : noadjoinProbTree);
                       Action noadjoinAction = PCrispActionCreator.createNoAdjoinAction(entry, node, prob, plansize);
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
}
