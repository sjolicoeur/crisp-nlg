package crisp.result;

import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.LexiconEntry;
import de.saar.penguin.tag.grammar.ElementaryTree;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Action;


import de.saar.chorus.term.Term;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Compound;
import de.saar.penguin.tag.grammar.Constraint;
import de.saar.penguin.tag.grammar.NodeType;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrispDerivationTreeBuilder extends DerivationTreeBuilder {

    public static final String DEFAULT_ROOT_CATEGORY = "s";

    private class Site {

        public String derivationNode;
        //public String tree;
        //public String word;
        public String treeNode;
        public String cat;

        public Site(String derivationNode, String treeNode, String cat) {
            this.derivationNode = derivationNode;
            //this.tree = tree;
            //this.word = word;
            this.treeNode = treeNode;
            this.cat = cat;
        }

        @Override
        public String toString() {
            return "Site{" + "derivationNode=" + derivationNode + "treeNode=" + treeNode + "cat=" + cat + '}';
        }
    }
    private Map<String, Site> substitutionSites;
    private Map<String, Site> adjunctionSites;

    public CrispDerivationTreeBuilder(Grammar<Term> grammar) {
        super(grammar);

    }

    private String findNodeWithCat(String cat, ElementaryTree t) {
        for (String node : t.getAllNodes()) {
            if (t.getNodeLabel(node).equalsIgnoreCase(cat)) {
                return node;
            }
        }
        return null;

    }

    private void addSubstitutionSites(ElementaryTree tree, String derivationNode, List<String> plannerNodenames, Map<String, String> categoryForPlannerNodename) {
        int positionInNodeList = 0;

        for (String nodeInTree : tree.getAllNodesInDfsOrder()) {
            if (tree.getChildren(nodeInTree).isEmpty()) {
                // found a leaf
                if (tree.getNodeType(nodeInTree) == NodeType.SUBSTITUTION) {
                    String cat = categoryForPlannerNodename.get(plannerNodenames.get(positionInNodeList));
                    assert tree.getNodeLabel(nodeInTree).equalsIgnoreCase(cat) : "node " + nodeInTree + " has label " + tree.getNodeLabel(nodeInTree) + ", but expected " + cat + " in elementary tree: " + tree;

                    String key = cat + ":" + plannerNodenames.get(positionInNodeList);
                    positionInNodeList++;

                    if (!substitutionSites.containsKey(key)) {
                        Site substSite = new Site(derivationNode, nodeInTree, cat);
                        substitutionSites.put(key, substSite);
                    }
                }
            }
        }
    }
    
    private void addNewSubstAndAdjSites(Action action, ElementaryTree tree, String derivationNode) {
        Map<String, String> categoryForPlannerNodename = new HashMap<String, String>();

        // decode substitution effects
        List<Compound> substEffects = getSubstEffects(action);

        for (Compound c : substEffects) {
            List<Term> subterms = c.getSubterms();
            String cat = ((Constant) subterms.get(0)).getName();
            String syntaxnode = ((Constant) subterms.get(1)).getName();

            categoryForPlannerNodename.put(syntaxnode, cat);
        }


        List<String> plannerNodenames = new ArrayList<String>();

        for (Term arg : action.getPredicate().getSubterms()) {
            String sArg = ((Constant) arg).getName();
            plannerNodenames.add(sArg);
        }

        plannerNodenames.retainAll(categoryForPlannerNodename.keySet());
        addSubstitutionSites(tree, derivationNode, plannerNodenames, categoryForPlannerNodename);

        // decode adjunction effects
        List<Compound> adjEffects = getAdjEffects(action);


        for (Compound c : adjEffects) {
            List<Term> subterms = c.getSubterms();
            String cat = ((Constant) subterms.get(0)).getName();
            String syntaxnode = ((Constant) subterms.get(1)).getName();
            String key = cat + ":" + syntaxnode;

            if (!adjunctionSites.containsKey(key)) {
                Site adjSite = new Site(derivationNode, findNodeWithCat(cat, tree), cat);
                adjunctionSites.put(key, adjSite);
            }
        }
    }

    @Override
    public void processPlanStep(Action action) {
        Compound pred = (Compound) action.getPredicate();
        String predicateName = action.getPredicate().getLabel();
        Pattern pattern = Pattern.compile("([^-]+)-(.*)-([^-]+)");
        Matcher m = pattern.matcher(predicateName);

        if (!m.matches()) {
            throw new RuntimeException("Operator name does not encode lexicalized tree: " + action);
        }

        String type = m.group(1);
        String typelesstreename = m.group(2);
        String word = m.group(3);

        String treename = null;
        if (type.equals("aux")) {
            treename = "a." + typelesstreename;
        } else {
            treename = "i." + typelesstreename;
        }

        ElementaryTree childTree = grammar.getTree(treename);
        LexiconEntry childEntry = grammar.getLexiconEntry(word, treename);

        // Won't be doing subst and adj. Only check for adj if subst not found.
        boolean operationFound = false;

        for (Compound comp : getSubstPreconditions(action)) {
            operationFound = true;
            Constant catTerm = (Constant) comp.getSubterms().get(0);
            Constant syntaxnodeTerm = (Constant) comp.getSubterms().get(1);
            String syntaxnode = syntaxnodeTerm.getName();
            String cat = catTerm.getName();

            Site substSite = substitutionSites.get(cat + ":" + syntaxnode);

            if (substSite == null) {
                throw new RuntimeException("No suitable substitution site found for " + action + ".");
            }

            String substituted = null;
            if (cat.equals(substSite.cat)) { // Found a suitable substitution site
                String newDerivNode = currentDerivation.addNode(substSite.derivationNode, substSite.treeNode, treename, childEntry);
                substituted = substSite.cat + ":" + syntaxnode;
                addNewSubstAndAdjSites(action, childTree, newDerivNode);
            }

            if (substituted != null) {
                substitutionSites.remove(substituted);
            } else {
                throw new RuntimeException("No substitution was performed for action " + action);
            }
            break;
        }

        //if (!operationFound){ // No Subst operation, do adj instead

        for (Compound comp : getAdjPreconditions(action)) {
            //System.out.println(action + "is an adj action.");
            operationFound = true;
            Constant catTerm = (Constant) comp.getSubterms().get(0);
            Constant syntaxnodeTerm = (Constant) comp.getSubterms().get(1);
            String syntaxnode = syntaxnodeTerm.getName();
            String cat = catTerm.getName();

            Site adjSite = adjunctionSites.get(cat + ":" + syntaxnode);

            if (adjSite == null) {
                //System.out.println(adjunctionSites);
                throw new RuntimeException("No suitable adjunction site found for " + action + ".");
            }

            String adjoined = null;
            if (cat.equals(adjSite.cat)) { // Found a suitable substitution site
                String newDerivNode = currentDerivation.addNode(adjSite.derivationNode, adjSite.treeNode, treename, childEntry);
                adjoined = adjSite.cat + ":" + syntaxnode;
                addNewSubstAndAdjSites(action, childTree, newDerivNode);
            }

            if (adjoined != null) {
                substitutionSites.remove(adjoined);
            } else {
                throw new RuntimeException("No substitution was performed for action " + action);
            }
            break;
        }


        //}
    }

    public DerivationTree buildDerivationTreeFromPlan(List<Term> plan, Domain domain, String root_category) {
        currentDerivation = new DerivationTree();
        substitutionSites = new HashMap<String, Site>();
        adjunctionSites = new HashMap<String, Site>();

        // Create initial substitution site for the root of the derivation

        Site rootSite = new Site(null, null, root_category);
        substitutionSites.put(root_category + ":root", rootSite);

        for (Term term : plan) {
            Action instantiatedAction = computeInstantiatedAction(term, domain);
            processPlanStep(instantiatedAction);
        }

        // at this point, the contents of currentDerivation.getChildren(*) could be manipulated to rearrange
        // the order of auxiliary trees that are adjoined to the same node

        return currentDerivation;
    }

    @Override
    public DerivationTree buildDerivationTreeFromPlan(List<Term> plan, Domain domain) {
        return buildDerivationTreeFromPlan(plan, domain, DEFAULT_ROOT_CATEGORY);
    }
}
