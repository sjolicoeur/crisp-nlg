package crisp.result;

import java.util.LinkedList;
import java.util.List;

/**
 * A DerivationNode represents a node in a TAG derivation tree.
 * Except for the root node, every node is associated with either an
 * adjunction or substitution operation and the grammar tree that was 
 * adjoined or substituted. In the case of a substitution, the we need
 * to know the substitution site where it was 
 * substituted (we know what all of these are). In the case of an
 * adjunction, we must know the node in the tree at which the 
 * adjunction took place.
 * 
 * sem and cat specify the role and category of the site at which
 * the tree represented by the node gets substituted or adjoined in
 * the parent tree.
 * 
 * The list daughters contains all the daughter nodes of this
 * one. These are substitutions and adjunctions made into the
 * tree that this node gets from the grammar.
 * 
 * @author Mark Wilding
 *
 */
public abstract class DerivationNode {
	protected String sem;
	protected String cat;
	protected List<DerivationNode> daughters = new LinkedList<DerivationNode>();
	
	/**
	 * Get the grammar tree that is used by this node in the derivation
	 * (either for substitution or adjunction).
	 * 
	 * @return the grammar tree (InitialTree or AuxiliaryTree).
	 */
	public abstract GrammarTree getGrammarTree();
	
	/**
	 * Prepares the derivation nodes for performing derivation and 
	 * producing the derived tree. This must be called before calling
	 * performDerivation, since it is essential to plan the substitutions
	 * and adjunctions before any of them are performed.
	 * 
	 * @param parent The root of the tree to merge the new sub-tree into
	 */
	public abstract void prepareDerivation(TreeNode parent);
	
	/**
	 * Perform the operation represented by the node (substitution or 
	 * adjunction) at the appropriate place in the given tree 
	 * (as prepared previously by prepareDerivation).
	 */
	public abstract void performDerivation() throws DerivationException;

	
	
	/**
	 * @return the role of the site at which to merge
	 */
	public String getSem() {
		return sem;
	}

	/**
	 * @param sem the role to set
	 */
	public void setSem(String sem) {
		this.sem = sem;
	}

	/**
	 * @return the category of the site at which to merge
	 */
	public String getCat() {
		return cat;
	}

	/**
	 * @param cat the category to set
	 */
	public void setCat(String cat) {
		this.cat = cat;
	}

	/**
	 * @return the daughters of this node
	 */
	public List<DerivationNode> getDaughters() {
		return daughters;
	}
	/**
	 * Adds a new daughter node to this one.
	 * 
	 * @param daughter the new node to add.
	 */
	public void addDaughter(DerivationNode daughter) {
		daughters.add(daughter);
	}
	
	/**
	 * @return a list of the open substitution sites that are immediate 
	 *  daughters of this node.
	 */
	public List<SubstitutionDerivationNode> getOpenSubstitutionSites() {
		List<SubstitutionDerivationNode> sites = new LinkedList<SubstitutionDerivationNode>();
		for (DerivationNode daughter : daughters) {
			if (daughter instanceof SubstitutionDerivationNode)
				if (((SubstitutionDerivationNode) daughter).isOpenSubstitutionSite())
					sites.add((SubstitutionDerivationNode)daughter);
		}
		return sites;
	}
	
	/**
	 * Removes the "-n" from the end of the sem roles.
	 * @param rolename input role name
	 * @return name with number removed
	 */
	public static String removeRoleNumber(String rolename) {
		int index = rolename.lastIndexOf("-");
		if (index==-1) return rolename;
		else return rolename.substring(0, index);
	}
	
	public String toString() {
		return toString(0);
	}
	
	public abstract String toString(int indent);
	
	public class DerivationException extends Exception {}
}
