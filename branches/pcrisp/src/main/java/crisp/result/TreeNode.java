package crisp.result;

import java.util.LinkedList;
import java.util.List;

/**
 * TreeNode is the abstract superclass of all node classes for TAG trees.
 * A node may be an internal node (a non-elementary category) or a leaf 
 * node (a word) or a substitution site (a site where a substitution must
 * be performed before the tree is fully specified).
 * 
 * @author Mark Wilding
 *
 */
public abstract class TreeNode {
	protected InternalTreeNode parent;
	protected String category;
	protected String index;
	protected String sem;

	/**
	 * Counts up the number of nodes in this tree (this one, plus
	 * all subtrees)
	 * 
	 * @return The number of nodes in the tree rooted by this node.
	 */
	public abstract int getNumberOfNodes();
	
	/**
	 * @return the parent node in the TAG tree.
	 */
	public InternalTreeNode getParent() {
		return parent;
	}
	/**
	 * @param parent the parent node of this in the TAG tree (take care setting this).
	 */
	public void setParent(InternalTreeNode parent) {
		this.parent = parent;
	}
	
	/**
	 * @return the grammatical category represented by this node, if any. null otherwise.
	 */
	public String getCategory() {
		return category;
	}
	/**
	 * @param category the grammatical category represented by this node
	 */
	public void setCategory(String category) {
		this.category = category;
	}
	
	/**
	 * @return the index string of the node
	 */
	public String getIndex() {
		return index;
	}
	/**
	 * @param index the index string of the node
	 */
	public void setIndex(String index) {
		this.index = index;
	}
	
	/**
	 * @return the semantic value of the node
	 */
	public String getSem() {
		return sem;
	}
	/**
	 * @param sem the semantic value of the node
	 */
	public void setSem(String sem) {
		this.sem = sem;
	}
	
	/**
	 * @return true if this is the root of the tree (has no parent), false otherwise.
	 */
	public boolean isRoot() {
		return parent==null;
	}
	
	/**
	 * @return a list of all the substitution sites to be found in the 
	 *  tree rooted by this node.
	 */
	public abstract List<LeafTreeNode> getSubstitutionSites();
	
	/**
	 * Looks for a foot node in this tree and returns it if it can find one.
	 * 
	 * @return a foot node, if found, otherwise null
	 */
	public abstract LeafTreeNode getFoot();
	
	/**
	 * Replaces the reference to this node in its parent with a 
	 * reference to the given new node. This is used for substitution
	 * and adjunction.
	 * 
	 * @param newNode the new node to point to instead
	 */
	public void replaceInParent(TreeNode newNode) {
		if (parent!=null) {
			int index = parent.getDaughters().indexOf(this);
			parent.getDaughters().remove(index);
			parent.getDaughters().add(index, newNode);
			// Detach this
			newNode.setParent(parent);
			parent=null;
		} else {
			// This shouldn't happen anywhere. Throw out a warning.
			System.out.println("WARNING: Tried to replace a root node");
		}
	}
	
	/**
	 * Find a potential adjunction site that has the given values
	 * of cat and sem, if one exists in the tree rooted by this 
	 * node.
	 * 
	 * @param cat the cat value required
	 * @param sem the sem value required
	 * @return the node found if any, otherwise null
	 */
	public abstract TreeNode getAdjunctionSite(String cat, String sem);
	
	/**
	 * Go through every node in the tree replacing any "self" sem
	 * values with the given alternative.
	 * 
	 * @param newRole the new sem value
	 */
	public void replaceSelfRoles(String newRole) {
		if ("self".equals(sem)) sem = new String(newRole);
		if (this instanceof InternalTreeNode)
			for (TreeNode daughter : ((InternalTreeNode)this).getDaughters())
				daughter.replaceSelfRoles(newRole);
	}
	
	/**
	 * Reads off the sentence that is encoded in the tree rooted by
	 * this node.
	 * 
	 * @return the sentence as a String list
	 */
	public abstract List<String> getSentence();
	
	public String getSentenceString() {
		String sentence = "";
		for (String word : getSentence()) {
			sentence += word+" ";
		}
		return sentence;
	}
	
	public String toString() {
		return toString(0);
	}
	
	public abstract String toString(int indent);
}
