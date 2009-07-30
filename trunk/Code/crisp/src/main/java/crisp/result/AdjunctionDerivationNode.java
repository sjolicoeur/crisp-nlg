package crisp.result;

/**
 * Represents an adjunction branch in the derivation tree
 *
 * @author Mark Wilding
 *
 */
public class AdjunctionDerivationNode extends DerivationNode {
	private AuxiliaryTree tree;
	private TreeNode site = null;
	
	public AdjunctionDerivationNode(AuxiliaryTree tree, String sem, String cat) {
		this.tree = tree;
		this.sem = removeRoleNumber(sem);
		this.cat = cat;
	}

	@Override
	public GrammarTree getGrammarTree() {
		return tree;
	}

	@Override
	public void prepareDerivation(TreeNode parent) {
		// Replace sem="self" with sem of root
		tree.getRoot().replaceSelfRoles(sem);
		// Work out where to adjoin
		// For now just returns the first one found: there could be more than one
		site = parent.getAdjunctionSite(cat, sem);
		if (site==null) {
			System.out.println("Did not find adjunction site for "+cat+","+sem);
			System.out.println("Tree searched was: "+parent);
		}
		// Recurse, to make sure daughters are sorted out before tree is modified
		for (DerivationNode daughter : daughters) {
			daughter.prepareDerivation(tree.getRoot());
		}
	}

	@Override
	public void performDerivation() throws DerivationException {
		if (site==null) {
			System.out.println("Could not find a suitable site for adjunction");
			throw new DerivationException();
		} else {
			// First check whether this has had something substituted for it
			if (site instanceof LeafTreeNode) {
				LeafTreeNode leaf = (LeafTreeNode)site;
				if (leaf.getSubstitutedWith()!=null)
					// This node was a substitution site and has been filled.
					// Adjoin to the root node of the initial tree that filled it.
					site = leaf.getSubstitutedWith();
				else if (leaf.getFootReplacement()!=null)
					// This node was the foot of an adjunction which has now been performed.
					// Adjoin to the root of the tree now at the foot of the auxiliary tree.
					site = leaf.getFootReplacement();
			}
			
			// Replace the site node with the root node of the aux tree
			InternalTreeNode siteParent = site.getParent();
			int siteIndex = siteParent.getDaughters().indexOf(site);
			siteParent.getDaughters().remove(siteIndex);
			siteParent.getDaughters().add(siteIndex, tree.getRoot());
			tree.getRoot().setParent(siteParent);
			// Stick the adjunction site (+ subtree) at the foot of the aux tree
			// Subsequent adjunctions to the same node will adjoin here.
			tree.getFoot().replaceInParent(site);
			tree.getFoot().setFootReplacement(site);
		}

		// Recurse to derive subtrees
		for (DerivationNode daughter : daughters) {
			daughter.performDerivation();
		}
	}
	
	public String toString(int indent) {
		String output = "";
		for (int i=0; i<indent; i++) output+=" ";
		output += "(Adj["+cat+", "+sem+"]";
		for (DerivationNode daughter : daughters) {
			output+="\n";
			output += daughter.toString(indent+2);
		}
		output +=")";
		return output;
	}
	
	/**
	 * If this derivation node is planning to adjoin at the node
	 * oldNode, instead adjoin at newNode. This may be because other 
	 * adjunction or a substitution has taken place at the node 
	 * where this one planned to adjoin.
	 * 
	 * @param oldNode the node we might have been planning to adjoin to
	 * @param newNode the node we must now adjoin to if we were
	 */
	public void replaceReference(TreeNode oldNode, TreeNode newNode) {
		if (site==oldNode && site!=null && newNode!=null) site = newNode;
	}
}
