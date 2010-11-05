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
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.formula.Conjunction;
import crisp.planningproblem.formula.Formula;
import crisp.planningproblem.formula.Literal;
import crisp.planningproblem.formula.Negation;
import crisp.planningproblem.formula.Universal;
import crisp.planningproblem.formula.Conditional;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;
import de.saar.chorus.term.parser.TermParser;
import de.saar.penguin.tag.grammar.Constraint;
import de.saar.penguin.tag.grammar.CrispGrammar;
import de.saar.penguin.tag.grammar.CrispLexiconEntry;
import de.saar.penguin.tag.grammar.ElementaryTree;
import de.saar.penguin.tag.grammar.ElementaryTreeType;
import de.saar.penguin.tag.grammar.NodeType;

/**
 * This class provides a fast converter from XML CRISP problem descriptions to
 * planning domains and problems. This class only processes non-probabilistic grammar descriptions.
 * The parser is implemented as a plain SAX parser, thereby improving
 * processing speed and decreasing memory requirements over the old xpath based
 * parser in CRISPConverter, which is particularly important for parsing large grammar
 * description files.
 */
public class CurrentNextCrispConverter {


    private enum ConstantType {
        ID,
        NEXT,
        QUANT,
        NORMAL;
    }

    // Default Handler already does a lot of work like
    // parse error handling and registering the handler
    // with a parser.

    private String problempath;   // Absolute pathname for the directory that stores problem and grammar
    private int referentnum;        // the number of referents for this problem
    private int syntaxnodenum;    // the number of syntaxnodes for this problem
    private int maximumArity = 0; // the maximum arity of any predicate in the problem file
    private String problemname;   // the name for the problem as specified in the problem file
    private String mainCat;       // main category for the problem in the problem file

    private Domain domain;
    private boolean useOldDomain;

    private class ProblemfileHandler extends DefaultHandler {
        // Member variables for instances of the Content Handler

        private Stack<String> elementStack = new Stack<String>();
        private StringWriter characterBuffer;
        private Problem problem;
        private Domain domain;
        private Set<Term> trueAtoms;
        private Map<String, Set<Integer>> predicatesInWorld;
        private String indexIndividual;
        private List<String> currentParamTypes;

        /**
         * ********************* Methods for the content handler ****************
         */
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
            if (qName.equals("crispproblem")) {

                // Retrieve and set name for the problem
                problemname = atts.getValue("name").toLowerCase();
                domain.setName(problemname);
                problem.setName(problemname);
                problem.setDomain(domain);

//                try {
//                    plansize = Integer.parseInt(atts.getValue("plansize"));
//                } catch (NumberFormatException e) {
//                    throw new SAXParseException("Expecting integer number in plansize attribute.", null);
//                }

                try {
                    referentnum = Integer.parseInt(atts.getValue("referents"));
                } catch (NumberFormatException e) {
                    throw new SAXParseException("Expecting integer number in referents attribute.", null);
                }

                try {
                    syntaxnodenum = Integer.parseInt(atts.getValue("syntaxnodes"));
                } catch (NumberFormatException e) {
                    throw new SAXParseException("Expecting integer number in syntaxnodes attribute.", null);
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
                String indexIndividual = atts.getValue("index").toLowerCase();
                problem.addObject(indexIndividual, "individual");
                this.indexIndividual = indexIndividual;

                mainCat = atts.getValue("cat").toLowerCase(); // TODO: do we really need this as a member variable?

                // This was in computeInitialState(Domain domain, Problem problem)
                problem.addToInitialState(TermParser.parse("subst(" + mainCat + ", root)"));
                problem.addToInitialState(TermParser.parse("referent(root, " + atts.getValue("index") + ")"));
                // TODO: maybe there is a better place for this

                // Reinitialize the set of true atoms for each new problem
                trueAtoms = new HashSet<Term>();

                // add initial dummy atoms to sidestep a bug in LAMA
                //problem.addToInitialState(TermParser.parse("referent(dummysyntaxnode, dummyindiv)"));
                //problem.addToInitialState(TermParser.parse("distractor(dummysyntaxnode, dummyindiv)"));
                //problem.addToInitialState(TermParser.parse("subst(dummycategory, dummysyntaxnode)"));
                //problem.addToInitialState(TermParser.parse("canadjoin(dummycategory, dummysyntaxnode)"));
                //problem.addToInitialState(TermParser.parse("mustadjoin(dummycategory, dummysyntaxnode)"));
            }

            if (qName.equals("world")) {
                characterBuffer = new StringWriter();
                String params = atts.getValue("type");
                currentParamTypes = new ArrayList<String>();
                if (params != null) {
                    String[] paramTypes = params.split(" ");
                    for (int i = 0; i < paramTypes.length; i++) {
                        currentParamTypes.add(paramTypes[i]);
                    }
                }
            }

            if (qName.equals("commgoal")) {
                characterBuffer = new StringWriter();
            }

        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName)
                throws SAXException {

            String lastElement = elementStack.pop();
            if (!(lastElement.equals(qName))) {
                throw new SAXParseException("Cannot close " + qName +
                        " here. Expected " + lastElement + ".", null);
            }

            if (qName.equals("world")) { // Term definition ends here
                Term term = TermParser.parse(characterBuffer.toString()); // parse the Term

                // This was in computeInitialState(Domain domain, Problem problem)
                Compound compoundTerm = (Compound) term;
                addPredicateInWorld(compoundTerm);


                List<String> types;
                if (currentParamTypes.isEmpty()) {
                    types = new ArrayList<String>();
                    for (int i = 0; i < compoundTerm.getSubterms().size(); i++) {
                        types.add("individual");
                    }
                } else {
                    types = currentParamTypes;
                }

                domain.addPredicate(compoundTerm.getLabel(), types);
                //addIndividualConstants(term,domain);
                for (int i = 0; i < compoundTerm.getSubterms().size(); i++) {
                    problem.addObject(((Constant) (compoundTerm.getSubterms().get(i))).getName(), types.get(i));
                }


                problem.addToInitialState(term);
                trueAtoms.add(term);
            }

            if (qName.equals("commgoal")) { // Communicative goal definition ends here
                Term term = TermParser.parse(characterBuffer.toString());

                // This was in computeInitialState(Domain domain, Problem problem)

                // keep track of maximum arity
                if (term instanceof Compound) {
                    Compound c = (Compound) term;

                    int arity = c.getSubterms().size();
                    if (arity > maximumArity) {
                        maximumArity = arity;
                    }
                    problem.registerComgoalArity(arity);

                    domain.addConstant(renamePredicate(c.getLabel()), "predicate");

                    problem.addToInitialState(flattenTerm(c, "needtoexpress"));
                }

            }

            if (qName.equals("impgoal")) { // Communicative goal definition ends here
                Term term = TermParser.parse(characterBuffer.toString());

                // This was in computeInitialState(Domain domain, Problem problem)

                // keep track of maximum arity
                if (term instanceof Compound) {
                    Compound c = (Compound) term;

                    int arity = c.getSubterms().size();
                    if (arity > maximumArity) {
                        maximumArity = arity;
                    }
                    problem.registerComgoalArity(arity);

                    domain.addConstant(renameImperative(c.getLabel()), "predicate");

                    problem.addToInitialState(flattenImpTerm(c, "needtoexpress"));
                }

            }

        }

        private void addPredicateInWorld(Compound term) {
            Set<Integer> arities = predicatesInWorld.get(term.getLabel());

            if (arities == null) {
                arities = new HashSet<Integer>();
                predicatesInWorld.put(term.getLabel(), arities);
            }

            arities.add(term.getSubterms().size());
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {

            if (elementStack.empty()) {
                throw new SAXParseException("Characters out of any Element.", null);
            }


            characterBuffer.write(ch, start, length);

        }

        public String getIndexIndividual() {
            return indexIndividual;
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

        List<Term> tlNodeIndiv = new ArrayList<Term>();
        List<String> tlNodeIndivTypes = new ArrayList<String>();
        tlNodeIndiv.add(new Variable("?u"));
        tlNodeIndivTypes.add("syntaxnode");
        tlNodeIndiv.add(new Variable("?x"));
        tlNodeIndivTypes.add("individual");

        List<Term> tlCatNode = new ArrayList<Term>();
        List<String> tlCatNodeTypes = new ArrayList<String>();
        tlCatNode.add(new Variable("?a"));
        tlCatNodeTypes.add("category");
        tlCatNode.add(new Variable("?u"));
        tlCatNodeTypes.add("syntaxnode");

        // collect all goals in this list
        List<Formula> finalStateGoals = new ArrayList<Formula>();

        // no positive "subst" literals in the goal state
        Formula noSubst = new Universal(tlCatNode, tlCatNodeTypes,
                new Literal("subst(?a,?u)", false));

        // no positive "distractor" literals in the goal state
        Formula noDistractors = new Universal(tlNodeIndiv, tlNodeIndivTypes,
                new Literal("distractor(?u,?x)", false));

        // no positive "mustadjoin" literals in the goal state
        //   this is only added if there is an action that creates a mustadjoin constraint
        //   because otherwise the LAMA planner cannot handle universal preconditions
        //   involving this predicate.
        //if (domain.sawMustadjoin()){
        Formula noMustAdj = new Universal(tlCatNode, tlCatNodeTypes, new Literal("mustadjoin(?a,?u)", false));
        finalStateGoals.add(noMustAdj);
        //}

        finalStateGoals.add(noSubst);
        finalStateGoals.add(noDistractors);

        // no positive needtoexpress-* literals, for any arity used in the communicative
        // goals. If we would just do this for all arities the LAMA planner cannot handle
        // the universal precondition involving needtoexpress predicates that do not occur
        // elsewhere as an effect
        for (Integer i : problem.getComgoalArities()) {
            List<Term> tlPredicate = new ArrayList<Term>();
            List<String> tlPredicateTypes = new ArrayList<String>();
            tlPredicate.add(new Variable("?P"));
            tlPredicateTypes.add("predicate");

            List<Term> subterms = new ArrayList<Term>();
            subterms.add(new Variable("?P"));

            for (int j = 1; j <= i; j++) {
                tlPredicate.add(new Variable("?x" + j));
                tlPredicateTypes.add("individual");
                subterms.add(new Variable("?x" + j));
            }


            finalStateGoals.add(new Universal(tlPredicate, tlPredicateTypes,
                    new Literal(new Compound("needtoexpress-" + i, subterms), false)));
        }

        // since negated needtoexpress-* literals can also occur with other arity we
        // need to add predicates for any arity to the domain.
        for (int i = 0; i <= maximumArity; i++) {

            List<String> predNTEtypeList = new ArrayList<String>();
            predNTEtypeList.add("predicate");
            for (int j = 1; j <= i; j++) {
                predNTEtypeList.add("individual");
            }

            domain.addPredicate("needtoexpress-" + i, predNTEtypeList);
            domain.addPredicate("todo-" + i, predNTEtypeList);

        }


        problem.setGoal(new Conjunction(finalStateGoals));
    }

    /**
     * Sets up the PDDL domain by registering the requirements, types, and a
     * bunch of constants. This is called before parsing, therefore part of the
     * domain information cannot be set here (e.g. maximum plan length etc.)
     *
     * @param domain
     */
    private void setupDomain(Domain domain) {
        domain.clear();

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
        domain.addSubtype("predicate", "object");
        domain.addSubtype("imperative", "predicate");


        List<String> substTypeList = new ArrayList<String>();
        substTypeList.add("category");
        substTypeList.add("syntaxnode");
        domain.addPredicate("subst", substTypeList);

//        List<String> stepTypeList = new ArrayList<String>();
//        stepTypeList.add("stepindex");
//        domain.addPredicate("step", stepTypeList);

        List<String> nextTypeList = new ArrayList<String>();
        nextTypeList.add("syntaxnode");
        nextTypeList.add("syntaxnode");
        domain.addPredicate("next", nextTypeList);

        List<String> currentTypeList = new ArrayList<String>();
        currentTypeList.add("syntaxnode");
        domain.addPredicate("current", currentTypeList);

        List<String> nextReferentList = new ArrayList<String>();
        nextReferentList.add("individual");
        nextReferentList.add("individual");
        domain.addPredicate("next-referent", nextReferentList);


        List<String> referentAndDistractorTypeList = new ArrayList<String>();

        referentAndDistractorTypeList.add("syntaxnode");
        referentAndDistractorTypeList.add("individual");
        domain.addPredicate("distractor", referentAndDistractorTypeList);
        domain.addPredicate("referent", referentAndDistractorTypeList);

        List<String> adjoinTypeList = new ArrayList<String>();
        adjoinTypeList.add("category");
        adjoinTypeList.add("syntaxnode");
        domain.addPredicate("canadjoin", adjoinTypeList);
        domain.addPredicate("mustadjoin", adjoinTypeList);
    }

    private void setupProblem(Problem problem) {
        problem.clear();
        problem.addObject("root", "syntaxnode");
    }



    /**
     * Computes the domain of the PDDL planning problem.  In particular, this method
     * generates the actions.
     *
     * @param grammar The grammar from which actions are generated
     */
    private void computeDomain(Domain domain, Problem problem, CrispGrammar grammar, String indexIndividual) {
        Map<String, HashSet<String>> roles = new HashMap<String, HashSet<String>>();

        // for each tree in the grammar
        for (String treeName : grammar.getAllTreeNames()) {

            // Get all nodes in the tree
            ElementaryTree<Term> tree = grammar.getTree(treeName);
            Collection<String> allNodeIds = tree.getAllNodes();

            // store list of roles in each tree in a map by name
            HashSet<String> localRoles = new HashSet<String>();
            for (String node : allNodeIds) {
                Term decoration = tree.getNodeDecoration(node);
                if (decoration != null && (decoration.toString() != null) && tree.getNodeConstraint(node) != Constraint.NO_ADJUNCTION) {
                    localRoles.add(tree.getNodeDecoration(node).toString());
                }
            }
            roles.put(treeName, localRoles);

        }

        // Add syntaxnode literals

        String lastSyntaxNode = "n-1";
        problem.addObject(lastSyntaxNode, "syntaxnode");

        List<Term> currentSubterms = new ArrayList<Term>();
        currentSubterms.add(new Constant(lastSyntaxNode));
        problem.addToInitialState(new Compound("current", currentSubterms));

        String newSyntaxNode;
        for (int i = 2; i <= syntaxnodenum; i++) {
            newSyntaxNode = "n-" + i;
            problem.addObject(newSyntaxNode, "syntaxnode");

            if (lastSyntaxNode != null) {
                List<Term> subterms = new ArrayList<Term>();
                subterms.add(new Constant(lastSyntaxNode));
                subterms.add(new Constant(newSyntaxNode));
                problem.addToInitialState(new Compound("next", subterms));
            }
            lastSyntaxNode = newSyntaxNode;

        }

        String lastReferent = indexIndividual;
        problem.addObject(lastReferent, "individual");

        String newReferent;
        for (int i = 2; i <= referentnum; i++) {
            newReferent = "e-" + i;
            problem.addObject(newReferent, "individual");

            if (lastReferent != null) {
                List<Term> subterms = new ArrayList<Term>();
                subterms.add(new Constant(lastReferent));
                subterms.add(new Constant(newReferent));
                problem.addToInitialState(new Compound("next-referent", subterms));
            }
            lastReferent = newReferent;

        }

        // compute actions from lexical entries
        for (String word : grammar.getAllWords()) {
            //System.out.println("\n" + word + ":");

            Collection<CrispLexiconEntry> entries = grammar.getCrispLexiconEntries(word);
            //System.out.println(entries.size());
            for (CrispLexiconEntry entry : entries) {
                // Get the tree for this lexical entry from the hash map
                String treeRef = normalizeTreename(entry.tree);
                ElementaryTree<Term> tree = grammar.getTree(entry.tree);
                Collection<String> allNodes = tree.getAllNodesInDfsOrder();

                // Get lists of nodes that are open for substitution and adjunction
                ArrayList<String> substNodes = new ArrayList<String>();
                ArrayList<String> adjNodes = new ArrayList<String>();

                //System.out.println("  " + treeRef + ": " + tree.getSignatureString());

                for (String node : allNodes) {
                    String cat = tree.getNodeLabel(node).toLowerCase();
                    if (cat == null || cat.equals("")) {
                        cat = "none";
                    }
                    domain.addConstant(cat, "category"); // add constants for categories to the domain
                    if (tree.getNodeType(node) == NodeType.SUBSTITUTION) {
                        substNodes.add(node);
                    } else {
                        if ((tree.getNodeType(node) == NodeType.INTERNAL || tree.getNodeType(node) == NodeType.ANCHOR) &&
                                tree.getNodeConstraint(node) != Constraint.NO_ADJUNCTION &&
                                tree.getNodeDecoration(node) != null &&
                                tree.getNodeDecoration(node).toString() != null) {
                            adjNodes.add(node);
                        }
                    }

                }

                StringWriter actionNameBuf = new StringWriter();
                actionNameBuf.write(treeRef);
                actionNameBuf.write("-");
                actionNameBuf.write(entry.word);

                for (String lex : entry.auxLexicalItems.keySet()) {
                    actionNameBuf.write("-");
                    actionNameBuf.write(entry.auxLexicalItems.get(lex));
                }

                String actionName = actionNameBuf.toString();

                //System.out.println(actionName);

                String rootCategory = tree.getNodeLabel(tree.getRoot()).toLowerCase();

                List<Formula> goals = new ArrayList<Formula>();
                List<Formula> effects = new ArrayList<Formula>();

                // compute n and I as in the paper
                Map<String, String> n = new HashMap<String, String>();
                Map<String, String> I = new HashMap<String, String>();
                Map<String, String> rolesToSyntaxnodes = new HashMap<String, String>();
                Map<String, String> nextMap = new HashMap<String, String>();
                int roleno = 1;

                n.put("self", "?u");
                I.put("?u", "?x");
                for (String role : roles.get(entry.tree)) {
                    if (!role.equals("self")) {
                        n.put(role, "?u" + roleno);
                        String syntaxnode = "?x" + (roleno++);
                        I.put(n.get(role), syntaxnode);
                        rolesToSyntaxnodes.put(role, syntaxnode);
                    }
                }


                // compute the predicate
                String label = actionName;
                List<Term> variables = new ArrayList<Term>();
                List<String> variableTypes = new ArrayList<String>();

                // Individuals
                variables.add(new Variable(I.get(n.get("self"))));
                variableTypes.add("individual");
                //Syntax nodes
                variables.add(new Variable(n.get("self")));
                variableTypes.add("syntaxnode");

                Variable last = null;
                for (String role : roles.get(entry.tree)) {
                    if (!role.equals("self")) {
                        // Individuals
                        variables.add(new Variable(I.get(n.get(role))));
                        variableTypes.add("individual");

                        //Syntax nodes
                        Variable current = new Variable(n.get(role));
                        variables.add(current);
                        variableTypes.add("syntaxnode");


                        if (last == null) {
                            goals.add(new Literal("current(" + current + ")", true));
                            effects.add(new Literal("current(" + current + ")", false));
                        } else {
                            goals.add(new Literal("next(" + last + ", " + current + ")", true));
                            nextMap.put(last.toString(), current.toString());
                        }
                        last = current;
                    }
                }


                if (last != null) {
                    Variable current = new Variable("?un");
                    variables.add(current);
                    variableTypes.add("syntaxnode");
                    goals.add(new Literal("next(" + last + "," + current + ")", true));
                    effects.add(new Literal("current(?un)", true));
                    nextMap.put(last.toString(), current.toString());
                }

                for (String additionalParam : entry.getAdditionalParams().keySet()) {
                    variables.add(new Variable("?" + additionalParam));
                    String type = entry.getAdditionalParams().get(additionalParam);
                    variableTypes.add(type);
                    domain.addSubtype(type, "object");
                }

                Compound pred = new Compound(label, variables);

                // PRECONDITIONS

                // require reference from u to the parameter for role self
                goals.add(new Literal("referent(?u," + I.get("?u") + ")", true));

                if (tree.getType() == ElementaryTreeType.INITIAL) {
                    // initial tree: fills substitution node
                    goals.add(new Literal("subst(" + rootCategory + ", ?u)", true));
                    effects.add(new Literal("subst(" + rootCategory + ", ?u)", false));
                } else {
                    // auxiliary tree: adjoin, and satisfies mustadjoin requirements
                    goals.add(new Literal("canadjoin(" + rootCategory + ", ?u)", true));
                    effects.add(new Literal("mustadjoin(" + rootCategory + ", ?u)", false));
                }

                // all semantic contents must be satisfied
                List<Term> contentWithVariables = new ArrayList<Term>();
                boolean hasContent = false;
                boolean hasRequirements = false;


                Set<String> additionalParams = entry.getAdditionalParams().keySet();
                Map<String, String> additionalVars = entry.getAdditionalVars();

                for (Term semContTerm : entry.semantics) {
                    Compound semContCompound = ((Compound) semContTerm);
                    Compound termWithVariables = (Compound) newSubstituteVariablesForRoles(semContCompound, n, I, nextMap, additionalParams, additionalVars, ConstantType.NORMAL);

                    hasContent = true;

                    Compound semPredicate = makeSemanticPredicate(semContCompound);
                    List<String> semPredicateTypes = new ArrayList<String>();
                    for (int j = 0; j < semPredicate.getSubterms().size(); j++) {
                        semPredicateTypes.add("individual");
                    }
                    domain.addPredicate(semPredicate.getLabel(), semPredicateTypes);

                    goals.add(new Literal(termWithVariables, true));

                    contentWithVariables.add(termWithVariables);

                    effects.add(new Literal((Compound) flattenTerm(termWithVariables, "needtoexpress"), false));


                    if (semContCompound.getSubterms().size() > maximumArity) {
                        maximumArity = semContCompound.getSubterms().size();
                    }


                    domain.addConstant(renamePredicate(semContCompound.getLabel()), "predicate");

                }


                for (Term impEffTerm : entry.getImperativeEffects()) {
                    Compound impEffCompound = ((Compound) impEffTerm);
                    Compound termWithVariables = (Compound) newSubstituteVariablesForRoles(impEffTerm, n, I, nextMap, additionalParams, additionalVars, ConstantType.NORMAL);


                    //hasContent = true;

                    Compound impEffPredicate = makeSemanticPredicate(impEffCompound);
                    List<String> impEffPredicateTypes = new ArrayList<String>();
                    for (int j = 0; j < impEffPredicate.getSubterms().size(); j++) {
                        impEffPredicateTypes.add("individual");
                    }
                    domain.addPredicate(impEffPredicate.getLabel(), impEffPredicateTypes);
                    //goals.add(new Literal(termWithVariables, true));

                    contentWithVariables.add(termWithVariables);

                    effects.add(new Literal((Compound) flattenImpTerm(termWithVariables, "needtoexpress"), false));
                    effects.add(new Literal((Compound) flattenImpTerm(termWithVariables, "todo"), true));


                    if (impEffCompound.getSubterms().size() - 1 > maximumArity) {
                        maximumArity = impEffCompound.getSubterms().size() - 1;
                    }

                    domain.addConstant(renameImperative(impEffCompound.getLabel()), "imperative");

                }


                // Add semantic requirements to preconditions
                for (Term semReqTerm : entry.getSemanticRequirements()) {
                    Compound termWithVariables = (Compound) newSubstituteVariablesForRoles(semReqTerm, n, I, nextMap, additionalParams, additionalVars, ConstantType.NORMAL);
                    goals.add(new Literal(termWithVariables, true));
                    hasRequirements = true;
                }

                // Add pragmatic preconditions
                for (Term pragPrecondTerm : entry.getPragmaticPreconditions()) {
                    Compound termWithVariables = (Compound) newSubstituteVariablesForRoles(pragPrecondTerm, n, I, nextMap, additionalParams, additionalVars, ConstantType.NORMAL);
                    goals.add(new Literal(termWithVariables, true));
                }

                // Add pragmatic effects
                for (Term pragEffectTerm : entry.getPragmaticEffects()) {
                    Compound termWithVariables = (Compound) newSubstituteVariablesForRoles(pragEffectTerm, n, I, nextMap, additionalParams, additionalVars, ConstantType.NORMAL);
                    effects.add(new Literal(termWithVariables, true));
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
                if (hasContent) {
                    Variable distractorVar = new Variable("?y");
                    Substitution distractorSubst = new Substitution(new Variable("?x"), distractorVar);
                    List<Term> distractorQuantifierVars = new ArrayList<Term>();
                    List<String> distractorQuantifierVarTypes = new ArrayList<String>();
                    distractorQuantifierVars.add(distractorVar);
                    distractorQuantifierVarTypes.add("individual");

                    List<Formula> literals = new ArrayList<Formula>();
                    for (Term t : contentWithVariables) {
                        Literal l = new Literal((Compound) distractorSubst.apply(t), true);
                        literals.add(l);
                    }

                    Formula distractorPrecondition =
                            new Negation(new Conjunction(literals));

                    effects.add(new Universal(distractorQuantifierVars, distractorQuantifierVarTypes,
                            new Conditional(distractorPrecondition, new Literal("distractor(?u,?y)", false))));
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
                    //System.out.println("    For substNode "+substNode);
                    //System.out.println(role+" "+roleN);

                    String cat = tree.getNodeLabel(substNode).toLowerCase();
                    if (cat == null || cat.equals("")) {
                        cat = "NONE";
                    }

                    //subst
                    effects.add(new Literal("subst(" + cat + ", " + roleN + ")", true));

                    //referent
                    effects.add(new Literal("referent(" + roleN + ", " + I.get(roleN) + ")", true));

                    //distractors
                    if (hasContent || hasRequirements) {
                        Variable distractorVar = new Variable("?y");
                        Substitution distractorSubst = new Substitution(new Variable(I.get(roleN)), distractorVar);

                        List<Term> distractorQuantifierVars = new ArrayList<Term>();
                        List<String> distractorQuantifierVarTypes = new ArrayList<String>();
                        distractorQuantifierVars.add(distractorVar);
                        distractorQuantifierVarTypes.add("individual");


                        // TODO - it's a bit of a hack that we use the same semantic requirement
                        // (modulo substitution) for each substitution node, even if it is irrelevant
                        // for the distractors of this substitution node.  But it seems to be ok.
                        List<Formula> distractorPreconditions = new ArrayList<Formula>();
                        distractorPreconditions.add(new Literal("**equals**(?y," + I.get(roleN) + ")", false));

                        for (Term sr : entry.getSemanticRequirements()) {
                            Term term = distractorSubst.apply(newSubstituteVariablesForRoles(sr, n, I, nextMap, additionalParams, additionalVars, ConstantType.NORMAL));
                            distractorPreconditions.add(new Literal((Compound) term, true));

                            //Compound distractorPredicate = makeSemanticPredicate(term);
                            /*List<String> distractorPredicateTypes = new ArrayList<String>();
                            for (int j = 0; j < distractorPredicate.getSubterms().size(); j++) {
                                distractorPredicateTypes.add("individual");
                            }

                            )
                            domain.addPredicate(distractorPredicate.getLabel(), distractorPredicateTypes);
                            */
                        }

                        Formula distractorPrecondition = new Conjunction(distractorPreconditions);

                        effects.add(new Universal(distractorQuantifierVars, distractorQuantifierVarTypes,
                                new Conditional(distractorPrecondition,
                                        new Literal("distractor(" + roleN + ",?y)", true))));
                    }
                }

                // internal nodes: allow adjunction
                for (String adjNode : adjNodes) {

                    String role = tree.getNodeDecoration(adjNode).toString();
                    String roleN = n.get(role);
                    String cat = tree.getNodeLabel(adjNode).toLowerCase();

                    if (cat.equals("")) {
                        cat = "NONE";
                    }

                    // canadjoin
                    effects.add(new Literal("canadjoin(" + cat + ", " + roleN + ")", true));

                    // mustadjoin
                    if (tree.getNodeConstraint(adjNode) == Constraint.OBLIGATORY_ADJUNCTION) {
                        effects.add(new Literal("mustadjoin(" + cat + ", " + roleN + ")", true));
                        //domain.registerMustadjoin(); // set mustadjoin flag
                    }

                    // don't need to add constant to the domain because we ASSUME that every role
                    // except for "self" decorates some substitution node (and hence is added there)
                }


                // Finally create action and add it to the domain
                Action a = new Action(pred, variableTypes, new Conjunction(goals), new Conjunction(effects));
                domain.addAction(a);


            }

        }


    }

    /******** input, output, main program **********/
    /**
     * Parses the XML document given in problemfilename, as well as the grammar file
     * referenced from that document.
     *
     * @param grammar         the tree adjoining grammar to create the planning operators
     * @param problemfile     abstract filename of the problemfile to parse
     * @param domain          reference to an empty planning domain, will be completed by convert.
     * @param problem         reference to an empty planning problem, will be completed by convert
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public void convert(CrispGrammar grammar, Reader problemfile, Domain domain, Problem problem) throws ParserConfigurationException, SAXException, IOException {

        //initialize domain and problem
        setupDomain(domain);
        setupProblem(problem);

        // get the pathname where problem and grammar files are stored
        //problempath = problemfile.getAbsoluteFile().getParent();

        SAXParserFactory factory = SAXParserFactory.newInstance();

        ProblemfileHandler handler = new ProblemfileHandler(domain, problem);
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(problemfile), handler);

//            CrispGrammar filteredGrammar = (CrispGrammar) new GrammarFilterer<Term>().filter(grammar, new SemanticsPredicateListFilter(handler.predicatesInWorld) );
            computeDomain(domain, problem, grammar, handler.getIndexIndividual());
            computeGoal(domain, problem);

        } catch (ParserConfigurationException e) {
            throw new SAXException("Parser misconfigured: " + e);
        }

    }

    public void convert(CrispGrammar grammar, File problemfile, Domain domain, Problem problem) throws ParserConfigurationException, SAXException, IOException {
        convert(grammar, new FileReader(problemfile), domain, problem);

    }

    /*********** auxiliary functions *************/
    /**
     * Replaces all occurrences of semantic roles in the given term by the
     * variables that correspond to them.  That is, each occurrence of a role
     * r is replaced by I(n(r)) as defined in the paper.
     *
     * @param term
     * @param n    a mapping of role names to node identities
     * @param I    a mapping of node identities to variables
     * @return
     */

    /*
    private Term substituteVariablesForRoles(Term term, Map<String, String> n, Map<String, String> I) {
        if (term instanceof Compound) {
            Compound t = (Compound) term;
            List<Term> newChildren = new ArrayList<Term>();

            for (Term sub : t.getSubterms()) {
                newChildren.add(substituteVariablesForRoles(sub, n, I));
            }

            return new Compound(t.getLabel(), newChildren);
        } else if (term instanceof Constant) {
            Constant t = (Constant) term;
            if (n.containsKey(t.getName())) {
                return new Variable(I.get(n.get(t.getName())));
            } else {
                return t;
            }
        } else {
            return term;
        }
    }
    */

    /**
     * Translates XTAG style tree names into tree names that PDDL will accept.
     *
     * @param treename
     * @return
     */
    private String normalizeTreename(String treename) {
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
    private Term flattenTerm(Compound t, String newLabel) {
        List<Term> subterms = new ArrayList<Term>();

        subterms.add(new Constant(renamePredicate(t.getLabel())));
        subterms.addAll(t.getSubterms());

        return new Compound(newLabel + "-" + t.getSubterms().size(), subterms);
    }

    private Term flattenImpTerm(Compound t, String newLabel) {
        List<Term> subterms = new ArrayList<Term>();


        subterms.add(new Constant(renameImperative(t.getLabel())));

        List<Term> args = t.getSubterms();
        subterms.addAll(args);


        return new Compound(newLabel + "-" + t.getSubterms().size(), subterms);
    }


    private String renamePredicate(String predicate) {
        return "pred-" + predicate;
    }

    private String renameImperative(String predicate) {
        return "imp-" + predicate;
    }

    /**
     * Adds all constants that occur as arguments of the term to the domain.
     *
     * @param term
     * @param domain
     */
    private void addIndividualConstants(Term term, Domain domain) {
        if (term instanceof Compound) {
            for (Term sub : ((Compound) term).getSubterms()) {
                addIndividualConstants(sub, domain);
            }
        } else if (term instanceof Constant) {
            domain.addConstant(((Constant) term).getName(), "individual");
        }
    }

    /**
     * Adds all constants that occur as arguments of the term to the problem.
     *
     * @param term
     * @param problem
     */
    private void addObjectsToProblem(Term term, Problem problem) {
        if (term instanceof Compound) {
            for (Term sub : ((Compound) term).getSubterms()) {
                addObjectsToProblem(sub, problem);
            }
        } else if (term instanceof Constant) {
            problem.addObject(((Constant) term).getName(), "individual");
        }
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
        for (int i = 1; i <= t.getSubterms().size(); i++) {
            subterms.add(new Variable("?y" + i));
        }

        return new Compound(t.getLabel(), subterms);
    }


    /*
    public static File convertGrammarPath(String filename){
    return new File(problempath,filename);
    }
     */


    private Term newSubstituteVariablesForRoles(Term term, Map<String, String> n, Map<String, String> I, Map<String, String> nextMap, Set<String> additionalParams, Map<String, String> additionalVars, ConstantType type) {
        if (term.isCompound()) {
            Compound t = (Compound) term;

            if (t.getLabel().equals("id")) {
                return newSubstituteVariablesForRoles(t.getSubterms().get(0), n, I, nextMap, additionalParams, additionalVars, ConstantType.ID);
            } else if (t.getLabel().equals("next")) {
                return newSubstituteVariablesForRoles(t.getSubterms().get(0), n, I, nextMap, additionalParams, additionalVars, ConstantType.NEXT);
            } else if (t.getLabel().equals("forall")) {
                List<Term> newChildren = new ArrayList<Term>();
                List<Term> subterms = new ArrayList<Term>(t.getSubterms());
                newChildren.add(newSubstituteVariablesForRoles(subterms.get(0), n, I, nextMap, additionalParams, additionalVars, ConstantType.QUANT));
                subterms.remove(0);

                for (Term sub : subterms) {
                    newChildren.add(newSubstituteVariablesForRoles(sub, n, I, nextMap, additionalParams, additionalVars, ConstantType.NORMAL));
                }

                Compound forallCompound = new Compound("forall", newChildren);
                return forallCompound;

                //return new Compound("forall", newChildren);

            } else {
                List<Term> newChildren = new ArrayList<Term>();

                for (Term sub : t.getSubterms()) {
                    newChildren.add(newSubstituteVariablesForRoles(sub, n, I, nextMap, additionalParams, additionalVars, ConstantType.NORMAL));
                }

                return new Compound(t.getLabel(), newChildren);
            }

        } else if (term.isConstant()) {

            Constant t = (Constant) term;

            if (n.containsKey(t.getName())) {
                switch (type) {

                    case ID:
                        return new Variable(n.get(t.getName()));


                    case NEXT:
                        return new Variable(nextMap.get(n.get(t.getName())));


                    default:
                        return new Variable(I.get(n.get(t.getName())));


                }
            } else {
                switch (type) {
                    case QUANT:
                        if (additionalVars.containsKey(t.getName())) {
                            return new Constant("(?" + t.getName() + " - " + additionalVars.get(t.getName()) + ")");
                        } else {
                            return t;
                        }
                    default:
                        if (additionalParams.contains(t.getName()) || additionalVars.containsKey(t.getName())) {
                            return new Variable("?" + t.getName());
                        } else {
                            return t;
                        }
                }
            }

        } else {
            return term;
        }
    }


}
