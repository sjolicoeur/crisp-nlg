package crisp.result;

import crisp.planner.lazygp.ActionInstance;
import crisp.planningproblem.effect.Literal;

/**
 * Represents a substitution branch in the derivation tree.
 * 
 * Every time an unfilled substitution site is introduced, a SubstitutionDerivationNode
 * is created for that site. It has no tree, but will be filled out properly when 
 * the necessary substitution is found.
 * 
 * @author Mark Wilding
 *
 */
public class SubstitutionDerivationNode extends DerivationNode {
	private InitialTree tree;
	private LeafTreeNode site = null;
	
	public SubstitutionDerivationNode(InitialTree tree, String sem, String cat) {
		this.tree = tree;
		this.sem = removeRoleNumber(sem);
		this.cat = cat;
	}
	
	public SubstitutionDerivationNode(String sem, String cat) {
		this.sem = removeRoleNumber(sem);
		this.cat = cat;
		this.tree = null;
	}
	
	/**
	 * @return true if this node represents an unfilled substitution site.
	 */
	public boolean isOpenSubstitutionSite() {
		return tree==null;
	}
	
	/**
	 * Fills an open substitution site by substituting the initial tree given.
	 * 
	 * @param tree
	 */
	public void makeSubstitution(InitialTree tree, ActionInstance action) {
		if (isOpenSubstitutionSite()) {
			this.tree = tree;
		}
	}

	@Override
	public GrammarTree getGrammarTree() {
		return tree;
	}

	@Override
	public void prepareDerivation(TreeNode parent) {
		// Replace sem="self" with the sem of the root
		tree.getRoot().replaceSelfRoles(sem);
		// Check through substitution sites to find the right one
		for (LeafTreeNode sitePos : parent.getSubstitutionSites()) {
			if (sitePos.getCategory().equals(cat) && 
					sitePos.getSem().equals(sem) &&
					// Don't substitute if another substitution is already planned
					sitePos.getSubstitutedWith()==null) {
				site = sitePos;
				// Record that this node's being replaced
				site.setSubstitutedWith(tree.getRoot());
				break;
			}
		}
		if (site==null) {
			System.out.println("Didn't find a substitution site for "+cat+","+sem);
			System.out.println("Tree searched was: "+parent);
		}
		
		// Recurse, so that daughters can work out where to perform their operations.
		for (DerivationNode daughter : daughters) {
			daughter.prepareDerivation(tree.getRoot());
		}
	}

	@Override
	public void performDerivation() throws DerivationException {
		if (site==null) {
			System.out.println("Could not find a suitable substitution site. Is derivation prepared?");
			throw new DerivationException();
		} else {
			// Put the root of our tree into the substitution slot
			site.replaceInParent(tree.getRoot());
		}
		
		// Recurse to derive subtrees
		for (DerivationNode daughter : daughters) {
			daughter.performDerivation();
		}
	}

	public TreeNode getSite() {
		return site;
	}
	
	public String toString(int indent) {
		String output = "";
		for (int i=0; i<indent; i++) output+=" ";
		output += "(Subst["+cat+", "+sem+"]";
		for (DerivationNode daughter : daughters) {
			output+="\n";
			output += daughter.toString(indent+2);
		}
		output +=")";
		return output;
	}
}
