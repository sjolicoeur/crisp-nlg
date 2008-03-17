package crisp.converter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.TypedList;
import crisp.planningproblem.effect.Effect;
import crisp.planningproblem.goal.Goal;
import crisp.termparser.TermParser;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

public class CRISPtoPDDL {
	private static Document problemdoc;  // the XML document with the problem specification
	private static Document grammardoc;  // the XML document with the LTAG grammar
	private static XPath xpath;          // an XPath object for executing XPath queries

	private static int plansize;         // the maximum plan size as specified in the problem file
	private static int maximumArity = 0; // the maximum arity of any predicate in the problem file

	// some precompiled XPath expressions
	private static XPathExpression xpathSem;
	private static XPathExpression xpathSemContent;
	private static XPathExpression xpathPragCondition;
	private static XPathExpression xpathPragEffect;
	private static XPathExpression xpathSubstitutionNode;
	private static XPathExpression xpathSemreq;
	private static XPathExpression xpathNonSubstitutionNode;



	/************** methods for the CRISP-to-PDDL conversion ***************/

	/**
	 * Converts the CRISP specification in the problem file with the given
	 * name into a PDDL domain and problem object.
	 *
	 * @param problemfilename
	 * @param domain
	 * @param problem
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public static void convert(String problemfilename, Domain domain, Problem problem) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		parseDocuments(problemfilename);

		xpathSem = xpath.compile(".//*[@sem]");
		xpathSemContent = xpath.compile("semcontent");
		xpathPragCondition = xpath.compile("pragcondition");
		xpathPragEffect = xpath.compile("prageffect");
		xpathSubstitutionNode = xpath.compile(".//*[@type='substitution']");
		xpathSemreq = xpath.compile("semreq");
		xpathNonSubstitutionNode = xpath.compile(".//*[@cat][@cat != ''][@type != 'substitution']|.//*[@cat][@cat != ''][not(@type)]");

		plansize = Integer.parseInt(xp("/crispproblem/@plansize"));


		setupDomain(domain, problem);

		computeInitialState(domain, problem);
		computeDomain(domain, problem);
		computeGoal(domain, problem);

	}

	/**
	 * Compute the goal specification for the given CRISP problem.  This becomes the
	 * "goal" clause in the PDDL problem.
	 *
	 * @param domain
	 * @param problem
	 */
	private static void computeGoal(Domain domain, Problem problem) {
		TypedList tlNodeIndiv = new TypedList();
		tlNodeIndiv.addItem("?u", "syntaxnode");
		tlNodeIndiv.addItem("?x", "individual");

		TypedList tlCatNode = new TypedList();
		tlCatNode.addItem("?a", "category");
		tlCatNode.addItem("?u", "syntaxnode");

		// collect all goals in this list
		List<Goal> finalStateGoals = new ArrayList<Goal>();

		// no positive "subst" literals in the goal state
		Goal noSubst = new crisp.planningproblem.goal.Universal(tlCatNode,
				new crisp.planningproblem.goal.Literal("subst(?a,?u)", false));

		// no positive "distractor" literals in the goal state
		Goal noDistractors = new crisp.planningproblem.goal.Universal(tlNodeIndiv,
				new crisp.planningproblem.goal.Literal("distractor(?u,?x)", false));

		finalStateGoals.add(noSubst);
		finalStateGoals.add(noDistractors);

		// no positive needtoexpress-* literals, for any arity
		for( int i = 1; i <= maximumArity; i++ ) {
			TypedList tlPredicate = new TypedList();
			tlPredicate.addItem("?P", "predicate");

			Predicate predNTE = new Predicate();
			predNTE.setLabel("needtoexpress-" + i);
			predNTE.addVariable("?P", "predicate");

			List<Term> subterms = new ArrayList<Term>();
			subterms.add(new Variable("?P"));

			for( int j = 1; j <= i; j++ ) {
				tlPredicate.addItem("?x" + j, "individual");
				subterms.add(new Variable("?x" + j));

				predNTE.addVariable("?x" + j, "individual");
			}

			finalStateGoals.add(new crisp.planningproblem.goal.Universal(tlPredicate,
					new crisp.planningproblem.goal.Literal(new Compound("needtoexpress-" + i, subterms), false)));

			domain.addPredicate(predNTE);
		}


		problem.setGoal(new crisp.planningproblem.goal.Conjunction(finalStateGoals));
	}



	/**
	 * Computes the domain of the PDDL planning problem.  In particular, this method
	 * generates the actions.
	 *
	 * @param domain
	 * @param problem
	 * @throws XPathExpressionException
	 */
	private static void computeDomain(Domain domain, Problem problem) throws XPathExpressionException {
		Map<String,Node> trees = new HashMap<String, Node>();
		Map<String,List<String>> roles = new HashMap<String, List<String>>();

		// read trees and store them in a map by name
		for( Node treeElement : nl(xg("/crisp-grammar/tree", XPathConstants.NODESET))) {
			String treeName = getAttribute(treeElement, "id");
			List<String> rolesHere = new ArrayList<String>();
			Set<String> rolesHereSet = new HashSet<String>();

			for( Node roleElement : nl(xpathSem.evaluate(treeElement, XPathConstants.NODESET)) ) {
				String rolename = getAttribute(roleElement, "sem");

				rolesHereSet.add(rolename);
				domain.addConstant(rolename, "rolename");
			}

			rolesHereSet.remove("self");
			rolesHere.add("self");
			rolesHere.addAll(rolesHereSet);

			roles.put(treeName, rolesHere);
			trees.put(treeName, treeElement);

			domain.addConstant(normalizeTreename(treeName), "treename");
		}

		// collect category names
		for( Node n : nl(xg("//*[@cat]", XPathConstants.NODESET)) ) {
			String catname = getAttribute(n, "cat");

			if( !"".equals(catname) ) {
				domain.addConstant(catname, "category");
			}
		}

		// generate actions
		for( Node treeElement : nl(xg("/crisp-grammar/entry/tree", XPathConstants.NODESET)) ) {
			String treeName = getAttribute(treeElement, "refid");
			String word = xpath.evaluate("../@word", treeElement);

			Node tree = trees.get(treeName);


			StringBuffer actionNameBuf = new StringBuffer(normalizeTreename(treeName + "-" + word));

			NodeList auxAnchors = (NodeList) xpath.evaluate("lex", treeElement, XPathConstants.NODESET);
			for( int i = 0; i < auxAnchors.getLength(); i++ ) {
			    Node auxAnchor = auxAnchors.item(i);
			    String pos = xpath.evaluate("@pos", auxAnchor);
			    String w = xpath.evaluate("@word", auxAnchor);

			    //System.err.println("auxiliary word: " + pos + ":" + w);
			    actionNameBuf.append("-" + pos + ":" + w);
			}




			String actionName = actionNameBuf.toString();
			String rootCategory = xpath.evaluate("*[1]/@cat", tree);

			IterableNodeList semnodes = nl(xpathSemContent.evaluate(treeElement, XPathConstants.NODESET));
			IterableNodeList pragConditionNodes = nl(xpathPragCondition.evaluate(treeElement, XPathConstants.NODESET));
			IterableNodeList semReqNodes = nl(xpathSemreq.evaluate(treeElement, XPathConstants.NODESET));
			IterableNodeList pragEffectNodes = nl(xpathPragEffect.evaluate(treeElement, XPathConstants.NODESET));
			IterableNodeList nonSubstitutionNodes = nl(xpathNonSubstitutionNode.evaluate(tree, XPathConstants.NODESET));
			IterableNodeList substitutionNodes = nl(xpathSubstitutionNode.evaluate(tree, XPathConstants.NODESET));

			for( int i = 1; i <= plansize; i++ ) {
				Predicate pred = new Predicate();
				List<Goal> goals = new ArrayList<Goal>();
				List<Effect> effects = new ArrayList<Effect>();

				// compute n and I as in the paper
				Map<String,String> n = new HashMap<String, String>();
				Map<String,String> I = new HashMap<String, String>();
				int roleno = 1;

				for( String role : roles.get(treeName) ) {
					if( role.equals("self") ) {
						n.put(role,"?u");
					} else {
						n.put(role, role + "-" + i);
					}

					I.put(n.get(role), "?x" + (roleno++));
				}

				// compute the predicate
				pred.setLabel(actionName + "-" + i);
				pred.addVariable("?u", "syntaxnode");
				for( String role : roles.get(treeName) ) {
					pred.addVariable(I.get(n.get(role)), "individual");
				}

				// count the step
				goals.add(new crisp.planningproblem.goal.Literal("step(step" + i + ")", true));
				effects.add(new crisp.planningproblem.effect.Literal("step(step" + i + ")", false));
				effects.add(new crisp.planningproblem.effect.Literal("step(step" + (i+1) + ")", true));

				// require reference from u to x1
				goals.add(new crisp.planningproblem.goal.Literal("referent(?u,?x1)", true));

				if( treeName.startsWith("i.") ) {
					// initial tree: fills substitution node
					goals.add(new crisp.planningproblem.goal.Literal("subst(" + rootCategory + ", ?u)", true));
					effects.add(new crisp.planningproblem.effect.Literal("subst(" + rootCategory + ", ?u)", false));
				} else {
					// auxiliary tree: adjoin
					goals.add(new crisp.planningproblem.goal.Literal("canadjoin(" + rootCategory + ", ?u)", true));
				}

				// semantic content must be satisfied
				List<Term> contentWithVariables = new ArrayList<Term>();
				boolean hasContent = false;

				for( Node semnode : semnodes ) {
					Compound term = (Compound) TermParser.parse(semnode.getTextContent());
					Compound termWithVariables = (Compound) substituteVariablesForRoles(term, n, I);

					hasContent = true;

					domain.addPredicate(makeSemanticPredicate(term));
					goals.add(new crisp.planningproblem.goal.Literal(termWithVariables, true));

					contentWithVariables.add(termWithVariables);

					effects.add(new crisp.planningproblem.effect.Literal(flattenTerm(termWithVariables, "needtoexpress"), false));
					if( term.getSubterms().size() > maximumArity ) {
						maximumArity = term.getSubterms().size();
					}

					domain.addConstant(term.getLabel(), "predicate");
				}

				//				 TODO semantic requirements must also be satisfied


				// pragmatic requirements must be satisfied
				// (for now, this is handled exactly like the semantic content)
				// TODO - if that's so, why is the body of this loop different than above?
				for( Node pragnode : pragConditionNodes ) {
					Compound term = (Compound) TermParser.parse(pragnode.getTextContent());
					domain.addPredicate(makeSemanticPredicate(term));
					goals.add(new crisp.planningproblem.goal.Literal(substituteVariablesForRoles(term, n, I), true));
				}

				// remove distractors
				if( hasContent ) {
					Variable distractorVar = new Variable("?y");
					Substitution distractorSubst = new Substitution(new Variable("?x1"), distractorVar);
					TypedList distractorQuantifierVars = new TypedList();
					distractorQuantifierVars.addItem("?y", "individual");

					List<crisp.planningproblem.goal.Goal> literals = new ArrayList<crisp.planningproblem.goal.Goal>();
					for( Term t : contentWithVariables ) {
						literals.add(new crisp.planningproblem.goal.Literal(distractorSubst.apply(t), true));
					}

				  	Goal distractorPrecondition =
						  new crisp.planningproblem.goal.Negation(new crisp.planningproblem.goal.Conjunction(literals));


				  	effects.add(new crisp.planningproblem.effect.Universal(distractorQuantifierVars,
							  new crisp.planningproblem.effect.Conditional(distractorPrecondition,
									  new crisp.planningproblem.effect.Literal("distractor(?u,?y)", false))));
				}


				// pragmatic effects
				for( Node prageffnode : pragEffectNodes ) {
					Compound effect = (Compound) TermParser.parse(prageffnode.getTextContent());

					if( "uniqueref".equals(effect.getLabel())) {
						String roleN = n.get(effect.getSubterms().get(0).toString());
						TypedList vars = new TypedList();
						vars.addItem("?y", "individual");

						effects.add(new crisp.planningproblem.effect.Universal(vars,
								new crisp.planningproblem.effect.Literal("distractor(" + roleN + ",?y)", false)));
						break;
					}
				}

				// effects for the substitution nodes
				for( Node substnode : substitutionNodes ) {
					String role = getAttribute(substnode, "sem");
					String roleN = n.get(role);

					// subst
					effects.add(new crisp.planningproblem.effect.Literal("subst(" + getAttribute(substnode, "cat") + ", " + roleN + ")", true));

					if( !role.equals("self") ) {
						domain.addConstant(roleN, "syntaxnode");
					}

					// referent
					effects.add(new crisp.planningproblem.effect.Literal("referent(" + roleN + ", " + I.get(roleN) + ")", true));

					// distractors
					Variable distractorVar = new Variable("?y");
					Substitution distractorSubst = new Substitution(new Variable(I.get(roleN)), distractorVar);
					TypedList distractorQuantifierVars = new TypedList();
					distractorQuantifierVars.addItem("?y", "individual");

					// TODO - it's a bit of a hack that we use the same semantic requirement
					// (modulo substitution) for each substitution node, even if it is irrelevant
					// for the distractors of this substitution node.  But it seems to be ok.
					List<Goal> distractorPreconditions = new ArrayList<Goal>();
					distractorPreconditions.add(new crisp.planningproblem.goal.Literal("**equals**(?y," + I.get(roleN) + ")", false));

					for( Node sr : semReqNodes ) {
						Term term = distractorSubst.apply(substituteVariablesForRoles(TermParser.parse(sr.getTextContent()), n, I));
						domain.addPredicate(makeSemanticPredicate(term));
						distractorPreconditions.add(new crisp.planningproblem.goal.Literal(term, true));
					}

					Goal distractorPrecondition = new crisp.planningproblem.goal.Conjunction(distractorPreconditions);

					effects.add(new crisp.planningproblem.effect.Universal(distractorQuantifierVars,
							new crisp.planningproblem.effect.Conditional(distractorPrecondition,
									new crisp.planningproblem.effect.Literal("distractor(" + roleN + ",?y)", true))));
				}

				// internal nodes: allow adjunction
				for( Node adjnode : nonSubstitutionNodes ) {
					String role = getAttribute(adjnode, "sem");
					String roleN = n.get(role);
					String cat = getAttribute(adjnode, "cat");

					// canadjoin
					effects.add(new crisp.planningproblem.effect.Literal("canadjoin(" + cat + ", " + roleN + ")", true));

					// don't need to add constant to the domain because we ASSUME that every role
					// except for "self" decorates some substitution node (and hence is added there)
				}


				Action a = new Action(pred, new crisp.planningproblem.goal.Conjunction(goals), new crisp.planningproblem.effect.Conjunction(effects));
				domain.addAction(a);
			}
		}
	}

	/**
	 * Computes the initial state for the PDDL problem.  In particular, this encodes
	 * the knowledge base and the communicative goal.
	 *
	 * @param domain
	 * @param problem
	 * @throws XPathExpressionException
	 */
	private static void computeInitialState(Domain domain, Problem problem) throws XPathExpressionException {
		Set<Term> trueAtoms = new HashSet<Term>();

		// knowledge base
		for( Node w : nl(xp("/crispproblem/world", XPathConstants.NODESET)) ) {
			Term term = TermParser.parse(w.getTextContent());

			domain.addPredicate(makeSemanticPredicate(term));
			addIndividualConstants(term, domain);

			problem.addToInitialState(term);
			trueAtoms.add(term);
		}

		// communicative goal
		for( Node w : nl(xp("/crispproblem/commgoal", XPathConstants.NODESET))) {
			Term term = TermParser.parse(w.getTextContent());

			// keep track of maximum arity
			if( term instanceof Compound ) {
				Compound c = (Compound) term;

				int arity = c.getSubterms().size();
				if( arity > maximumArity ) {
					maximumArity = arity;
				}

				domain.addConstant(c.getLabel(), "predicate");

				problem.addToInitialState(flattenTerm(c, "needtoexpress"));
			}
		}

		// other stuff in the initial state
		problem.addToInitialState(TermParser.parse("subst(" + xp("/crispproblem/@cat") + ", root)"));
		problem.addToInitialState(TermParser.parse("referent(root, " + xp("/crispproblem/@index") + ")"));
		problem.addToInitialState(TermParser.parse("step(step1)"));
	}

	/**
	 * Sets up the PDDL domain by registering the requirements, types, and a bunch of
	 * constants.
	 *
	 * @param domain
	 * @param problem
	 * @throws XPathExpressionException
	 */
	private static void setupDomain(Domain domain, Problem problem) throws XPathExpressionException {
		domain.clear();
		problem.clear();

		domain.setName("crispdomain");
		problem.setName("crispproblem");
		problem.setDomain("crispdomain");

		domain.addRequirement(":strips");
		domain.addRequirement(":equality");
		domain.addRequirement(":typing");
		domain.addRequirement(":conditional-effects");
		domain.addRequirement(":universal-preconditions");
		domain.addRequirement(":quantified-preconditions");

		domain.addSubtype("individual", "object");
		domain.addSubtype("category", "object");
		domain.addSubtype("syntaxnode", "object");
		domain.addSubtype("stepindex", "object");
		domain.addSubtype("predicate", "object");

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

		for( int i = 1; i <= plansize+1; i++ ) {
			domain.addConstant("step" + i, "stepindex");
		}

		domain.addConstant("root", "syntaxnode");
		domain.addConstant(xp("/crispproblem/@index"), "individual");
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
	 * @throws XPathExpressionException
	 */
	private static void parseDocuments(String problemfilename) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		// parse XML document
		File problemfile = new File(problemfilename);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		problemdoc = builder.parse(problemfile);

		// set up XPath engine
		XPathFactory xpfactory = XPathFactory.newInstance();
		xpath = xpfactory.newXPath();

		// obtain and parse grammar file
		File grammarFile = new File(problemfile.getParentFile(), xpath.evaluate("/crispproblem/@grammar", problemdoc));
		grammardoc = builder.parse(grammarFile);
	}

	/**
	 * Writes the PDDL domain and problem to disk.
	 *
	 * @param domain
	 * @param problem
	 * @throws XPathExpressionException
	 * @throws IOException
	 */
	public static void writeToDisk(Domain domain, Problem problem, String filenamePrefix) throws XPathExpressionException, IOException {
		String problemname = xp("/crispproblem/@name");

		System.err.println("Writing domain file ...");
		PrintWriter dw = new PrintWriter(new FileWriter(filenamePrefix + problemname + "-domain.lisp"));
		domain.writePddl(dw);
		dw.close();

		System.err.println("Writing problem file ...");
		PrintWriter pw = new PrintWriter(new FileWriter(filenamePrefix + problemname + "-problem.lisp"));
		problem.writePddl(pw);
		pw.close();

		System.err.println("Done.");
	}

	/**
	 * Main program.  When running the converter from the command line, pass
	 * the name of the CRISP problem file as the first argument.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Domain domain = new Domain();
		Problem problem = new Problem();

		long start = System.currentTimeMillis();
		convert(args[0], domain, problem);
		long end = System.currentTimeMillis();

		System.err.println("Total runtime: " + (end-start) + "ms");

		System.out.println("Domain: " + domain);
		System.out.println("Problem: " + problem);

		writeToDisk(domain, problem, "");
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

		subterms.add(new Constant(t.getLabel()));
		subterms.addAll(t.getSubterms());

		return new Compound(newLabel + "-" + t.getSubterms().size(), subterms);
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






	/******* XML auxiliary methods ************/

	/**
	 * Returns the value of the attribute with name "attributeName" from the
	 * XML element "node".
	 *
	 * @param node
	 * @param attributeName
	 * @return
	 */
	private static String getAttribute(Node node, String attributeName) {
		return node.getAttributes().getNamedItem(attributeName).getTextContent();
	}

	/**
	 * Convert the result of a NODESET XPath call to an IterableNodeList.
	 *
	 * @param o the result of a call to xpath.evaluate(..., XPathConstants.NODESET)
	 * @return an IterableNodeList containing these nodes
	 */
	private static IterableNodeList nl(Object o) {
		return new IterableNodeList((NodeList) o);
	}

	/**
	 * Evaluate the xpathExpr on the "problem" XML document.
	 *
	 * @param xpathExpr
	 * @return
	 * @throws XPathExpressionException
	 */
	private static String xp(String xpathExpr) throws XPathExpressionException {
		return xpath.evaluate(xpathExpr, problemdoc);
	}

	/**
	 * Evaluate the xpathExpr on the "problem" XML document and interpret
	 * the result as specified by the "type" parameter.
	 *
	 * @param xpathExpr
	 * @param type
	 * @return
	 * @throws XPathExpressionException
	 */
	private static Object xp(String xpathExpr, QName type) throws XPathExpressionException {
		return xpath.evaluate(xpathExpr, problemdoc, type);
	}

	/**
	 * Evaluate the xpathExpr on the "grammar" XML document and interpret
	 * the result as specified by the "type" parameter.
	 *
	 * @param xpathExpr
	 * @param type
	 * @return
	 * @throws XPathExpressionException
	 */
	private static Object xg(String xpathExpr, QName type) throws XPathExpressionException {
		return xpath.evaluate(xpathExpr, grammardoc, type);
	}
}