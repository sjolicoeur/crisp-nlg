package crisp.result;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import crisp.planner.lazygp.ActionInstance;
import crisp.planner.lazygp.Plan;
import crisp.planningproblem.goal.Literal;
import crisp.result.AuxiliaryTree.WrongFootCategoryException;
import crisp.result.DerivationNode.DerivationException;
import crisp.result.Grammar.CouldNotLoadGrammarTreeException;
import crisp.result.Grammar.InvalidPredicateException;
import crisp.tools.Pair;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;


/**
 * Represents the top level of a derivation tree (encompassing the 
 * root node).
 * All other nodes represent either a substitution or adjunction, so this 
 * node must be built into this class.
 * 
 * This class has a field rootCat, for the category of the root node (usually S).
 * In the plan this is the substitution node that is created as part
 * of the initial state.
 * 
 * There is a single daughter of the root node (which is a substitution
 * site).
 * 
 * @author Mark Wilding
 *
 */
public class DerivationTree {
	private String rootCat;
	private RootDerivationNode root;
	private Plan plan;
	private Grammar grammar;
	
	/**
	 * Builds a new derivation tree from a plan.
	 * @throws InvalidPredicateException 
	 * @throws XPathExpressionException 
	 * @throws WrongFootCategoryException 
	 * @throws ImpossibleAdjunctionException 
	 * @throws ImpossibleSubstitutionException 
	 */
	public DerivationTree(String rootCat, Plan plan, Grammar grammar) throws InvalidPredicateException, XPathExpressionException, WrongFootCategoryException, ImpossibleAdjunctionException, ImpossibleSubstitutionException {
		this.rootCat = rootCat;
		this.plan = plan;
		this.grammar = grammar;
		
		///////// Now use the plan to build a derivation tree
		
		// Keep a record of nodes with substitution and adjunction sites:
		//  map from role to derivation node.
		HashMap<String, LinkedList<SubstitutionDerivationNode>> substitutionSites = 
			new HashMap<String, LinkedList<SubstitutionDerivationNode>>();
		// Map from cat/role pairs to derivation nodes.
		HashMap<Pair<String, String>, LinkedList<DerivationNode>> adjunctionSites = 
			new HashMap<Pair<String, String>, LinkedList<DerivationNode>>();
		
		// Start with the effects of the root derivation node:
		//  this has single subst site with cat S and role root.
		LinkedList<SubstitutionDerivationNode> rootList = new LinkedList<SubstitutionDerivationNode>();
		root = new RootDerivationNode(rootCat);
		rootList.add(root.getDaughter());
		substitutionSites.put("root", rootList);
		// No possible adjunctions to start with
		
		// Go through each step of the plan
		for (ActionInstance action : plan) {
			String predicateName = action.getAction().getPredicate().getLabel();
			
			// Won't be doing subst and adj. Only check for adj if subst not found.
			boolean operationFound = false;
			
			// Check through the preconditions of this action
			for (Compound comp : getSubstPreconditions(action)) {
				operationFound = true;
				// Found action representing substitution
				Constant catTerm = (Constant) comp.getSubterms().get(0);
				Constant roleTerm = (Constant) comp.getSubterms().get(1);
				String sem = roleTerm.getName();
				String cat = catTerm.getName();
				
				// Look for a substitution site with the correct role
				LinkedList<SubstitutionDerivationNode> subSites = substitutionSites.get(sem);
				
				if (subSites==null) {
					System.out.println("No suitable substitution site found for role "+sem);
				} else {
					// Look through sites with correct role to find one with correct category
					SubstitutionDerivationNode substituted = null;
					for (SubstitutionDerivationNode node : subSites) {
						if (node.getCat().equals(cat)) {
							// Substitute here
							// Retrieve the grammar tree to substitute
							try {
								InitialTree grammarTree = grammar.loadInitialTree(predicateName);
								node.makeSubstitution(grammarTree,action);
								// Remove the subst site that we've just filled
								substituted = node;
								
								// Add new possibilities for subst and adj
								addNewSubstAndAdjSites(substitutionSites, 
										adjunctionSites, action, node);
								break;
							} catch (CouldNotLoadGrammarTreeException e) {
								e.printStackTrace();
								System.out.println("Was unable to load a suitable tree from the grammar file for predicate "+predicateName);
								continue;
							}
						}
					}
					
					// Remove a subst node if we've filled it
					if (substituted!=null) subSites.remove(substituted);
					else {
						System.out.println("No substitution was performed for action "+action);
						throw new ImpossibleSubstitutionException();
					}
				}
			}
			
			if (!operationFound)
				for (Compound comp : getAdjPreconditions(action)) {
					// Found action representing adjunction
					Constant catTerm = (Constant) comp.getSubterms().get(0);
					Constant roleTerm = (Constant) comp.getSubterms().get(1);
					String sem = roleTerm.getName();
					String cat = catTerm.getName();
					
					// Look for a possible adjunction site
					Pair<String, String> catSem = new Pair<String, String>(cat,sem);
					LinkedList<DerivationNode> adjNodes = 
						adjunctionSites.get(catSem);
					// TODO What if there's more than one? - different possible derivation trees
					// For now just take the first
					if (adjNodes==null || adjNodes.size()==0) {
						System.out.println("Could not adjoin tree to a "+catSem+": "+action);
						throw new ImpossibleAdjunctionException();
					} else {
						DerivationNode adjoinTo = adjNodes.get(0);
						try {
							AuxiliaryTree grammarTree = grammar.loadAuxiliaryTree(predicateName);
							AdjunctionDerivationNode adjNode = 
								new AdjunctionDerivationNode(grammarTree,sem,cat);
							adjoinTo.addDaughter(adjNode);
							
							// Add new possibilities for subst and adj
							addNewSubstAndAdjSites(substitutionSites, 
									adjunctionSites, action, adjNode);
						} catch (CouldNotLoadGrammarTreeException e) {
							e.printStackTrace();
							System.out.println("Could not load suitable tree from grammar file for predicate "+predicateName);
						}
					}
				}
		}
	}

	/**
	 * Builds the derived tree that is implicitly represented by this 
	 * derivation tree.
	 * 
	 * @return the derived tree (root node)
	 * @throws DerivationException 
	 */
	public TreeNode buildDerivedTree() throws DerivationException {
		/*
		 * Start with a dummy root node and a single substitution site.
		 */
		InternalTreeNode rootNode = new InternalTreeNode("ROOTNODE","","");
		LeafTreeNode initialSubstSite = new LeafTreeNode(rootCat,"","root",rootNode,"",LeafTreeNode.LEAF_TYPE_SUBSTITUTION);
		rootNode.addRightDaughter(initialSubstSite);
		
		// Build the tree from the derivation tree
		root.prepareDerivation(rootNode);
		root.performDerivation();
		return rootNode;
	}
	
	/**
	 * Adds new open substitution sites to the list of subst sites and new
	 * possible adjunction locations to the adjunction list according to 
	 * the effects of the given action. The derivation node given is that
	 * which has just been added by the action.
	 */
	private void addNewSubstAndAdjSites(
			HashMap<String, LinkedList<SubstitutionDerivationNode>> substitutionSites,
			HashMap<Pair<String, String>, LinkedList<DerivationNode>> adjunctionSites,
			ActionInstance action,
			DerivationNode node) {

		// Deal with substitutions
		for (Compound substEffect : getSubstEffects(action)) {
			// Found effect representing substitution
			Constant catEffectTerm = (Constant) substEffect.getSubterms().get(0);
			Constant roleEffectTerm = (Constant) substEffect.getSubterms().get(1);
			String semEffect = roleEffectTerm.getName();
			String catEffect = catEffectTerm.getName();
			
			// Create an open site for each required substitution
			SubstitutionDerivationNode substSite = 
				new SubstitutionDerivationNode(semEffect, catEffect);
			node.addDaughter(substSite);

			// Add the new node to the list of substitution sites with this sem value
			LinkedList<SubstitutionDerivationNode> effectSites = 
				substitutionSites.get(semEffect);
			if (effectSites==null) {
				// Create the list if necessary 
				effectSites = new LinkedList<SubstitutionDerivationNode>();
				substitutionSites.put(semEffect, effectSites);
			}
			effectSites.add(substSite);
		}
		
		// Deal with adjunctions
		for (Compound adjEffect : getAdjEffects(action)) {
			// Found effect representing substitution
			Constant catEffectTerm = (Constant) adjEffect.getSubterms().get(0);
			Constant roleEffectTerm = (Constant) adjEffect.getSubterms().get(1);
			String semEffect = roleEffectTerm.getName();
			String catEffect = catEffectTerm.getName();

			// Add the new node to the list of adjunction sites with these sem and cat values
			Pair<String,String> catSem = new Pair<String,String>(catEffect,semEffect);
			LinkedList<DerivationNode> effectSites = 
				adjunctionSites.get(catSem);
			if (effectSites==null) {
				// Create the list if necessary 
				effectSites = new LinkedList<DerivationNode>();
				adjunctionSites.put(catSem, effectSites);
			}
			effectSites.add(node);
		}
	}
	
	public List<Compound> getSubstPreconditions(ActionInstance action) {
		List<Compound> output = new LinkedList<Compound>();
		// Check through action preconditions
		for (Literal literal : action.getPreconditions()) {
			// Cast to compound
			if (literal.getAtom() instanceof Compound) {
				Compound comp = (Compound) literal.getAtom();
				// Take only subst terms that are positive
				if ("subst".equals(comp.getLabel()) && literal.getPolarity()) {
					output.add(comp);
				}
			}
		}
		return output;
	}
	
	public List<Compound> getSubstEffects(ActionInstance action) {
		List<Compound> output = new LinkedList<Compound>();
		// Check through action preconditions
		for (crisp.planningproblem.effect.Literal literal : action.getEffects()) {
			// Cast to compound
			if (literal.getAtom() instanceof Compound) {
				Compound comp = (Compound) literal.getAtom();
				// Take only subst terms that are positive
				if ("subst".equals(comp.getLabel()) && literal.getPolarity()) {
					output.add(comp);
				}
			}
		}
		return output;
	}
	
	public List<Compound> getAdjPreconditions(ActionInstance action) {
		List<Compound> output = new LinkedList<Compound>();
		// Check through action preconditions
		for (Literal literal : action.getPreconditions()) {
			// Cast to compound
			if (literal.getAtom() instanceof Compound) {
				Compound comp = (Compound) literal.getAtom();
				// Take only adj terms that are positive
				if ("canadjoin".equals(comp.getLabel()) && literal.getPolarity()) {
					output.add(comp);
				}
			}
		}
		return output;
	}
	
	public List<Compound> getAdjEffects(ActionInstance action) {
		List<Compound> output = new LinkedList<Compound>();
		// Check through action preconditions
		for (crisp.planningproblem.effect.Literal literal : action.getEffects()) {
			// Cast to compound
			if (literal.getAtom() instanceof Compound) {
				Compound comp = (Compound) literal.getAtom();
				// Take only subst terms that are positive
				if ("canadjoin".equals(comp.getLabel()) && literal.getPolarity()) {
					output.add(comp);
				}
			}
		}
		return output;
	}

	/**
	 * @return the root category of the tree
	 */
	public String getRootCat() {
		return rootCat;
	}
	/**
	 * @param the root category of the tree
	 */
	public void setRootCat(String rootCat) {
		this.rootCat = rootCat;
	}

	/**
	 * @return the substitution that is performed on the root node
	 * 	of the derived tree.
	 */
	public RootDerivationNode getRootNode() {
		return root;
	}
	/**
	 * @param daughter the substitution to perform.
	 */
	public void setRootNode(RootDerivationNode root) {
		this.root = root;
	}

	public String toString() {
		return root.toString();
	}
	
	public class ImpossibleAdjunctionException extends Exception {}
	public class ImpossibleSubstitutionException extends Exception {}
}
