package crisp.result;

import java.util.LinkedList;
import java.util.List;

/**
 * InternalTreeNode represents an internal node in a TAG derived (or 
 * partially derived) tree.
 * Every internal node must have at least one daughter.
 * 
 * @author Mark Wilding
 *
 */
public class InternalTreeNode extends TreeNode {
	private LinkedList<TreeNode> daughters;
	
	/**
	 * Constructs an internal node for a tag tree
	 * 
	 * @param category Grammatical category of node
	 * @param index Index of node in grammar
	 * @param sem Semantic value of node
	 * @param daughters List of the node's daughters (at least one)
	 * @param parent Parent node (null for root node)
	 */
	public InternalTreeNode(String category, String index, String sem, LinkedList<TreeNode> daughters, InternalTreeNode parent) {
		this.parent = parent;
		this.category = category;
		this.index = index;
		this.sem = sem;
		this.daughters = daughters;
	}
	
	/**
	 * Constructs an internal node for a tag tree with no daughters
	 * 
	 * @param category Grammatical category of node
	 * @param index Index of node in grammar
	 * @param sem Semantic value of node
	 * @param parent Parent node (null for root node)
	 */
	public InternalTreeNode(String category, String index, String sem, InternalTreeNode parent) {
		this.parent = parent;
		this.category = category;
		this.index = index;
		this.sem = sem;
		this.daughters = new LinkedList<TreeNode>();
	}
	
	/**
	 * Constructs a root node for a tag tree (no parent)
	 * 
	 * @param category Grammatical category of node
	 * @param index Index of node in grammar
	 * @param sem Semantic value of node
	 * @param daughters List of the node's daughters (at least one)
	 */
	public InternalTreeNode(String category, String index, String sem, LinkedList<TreeNode> daughters) {
		this.category = category;
		this.index = index;
		this.sem = sem;
		this.daughters = daughters;
		this.parent = null;
	}
	
	/**
	 * Constructs a root node for a tag tree (no parent)
	 * 
	 * @param category Grammatical category of node
	 * @param index Index of node in grammar
	 * @param sem Semantic value of node
	 */
	public InternalTreeNode(String category, String index, String sem) {
		this.category = category;
		this.index = index;
		this.sem = sem;
		this.parent = null;
		this.daughters = new LinkedList<TreeNode>();
	}
	


	/**
	 * Returns the index within this tree of the given node, or -1 if it's not in the tree.
	 * The indices are assigned in a depth-first, left-to-right manner and include
	 * internal nodes and leaves.
	 * 
	 * @param node The node to look up
	 * @return The index in the tree of the node.
	 */
	//public int getNodeIndex(TreeNode node) {
		
	//}
	
	@Override
	public int getNumberOfNodes() {
		int nodes = 1;
		for (TreeNode n : getDaughters())
			nodes += n.getNumberOfNodes();
		
		return nodes;
	}
	

	/**
	 * @param index Index of daughter (ltr)
	 * @return The indexth daughter of the node
	 */
	public TreeNode getDaughter(int index) {
		return daughters.get(index);
	}
	
	/**
	 * Adds the given node as the rightmost daughter of this node
	 * 
	 * @param daughter The node to adds
	 */
	public void addRightDaughter(TreeNode daughter) {
		daughters.add(daughter);
	}

	/**
	 * @return the daughters of the node
	 */
	public LinkedList<TreeNode> getDaughters() {
		return daughters;
	}
	/**
	 * @param daughters the daughters of the node
	 */
	public void setDaughters(LinkedList<TreeNode> daughters) {
		this.daughters = daughters;
	}

	@Override
	public List<LeafTreeNode> getSubstitutionSites() {
		// Compile the list from those of daughter nodes
		List<LeafTreeNode> sites = new LinkedList<LeafTreeNode>();
		for (TreeNode daughter : daughters)
			sites.addAll(daughter.getSubstitutionSites());
		return sites;
	}

	@Override
	public LeafTreeNode getFoot() {
		LeafTreeNode found = null;
		for (TreeNode daughter : daughters) {
			found = daughter.getFoot();
			if (found!=null) break;
		}
		return found;
	}

	@Override
	public TreeNode getAdjunctionSite(String cat, String sem) {
		if (this.category.equals(cat) && this.sem.equals(sem))
			return this;
		else
			// Try each of the daughters recursively
			for (TreeNode daughter : daughters) {
				TreeNode found = daughter.getAdjunctionSite(cat, sem);
				if (found!=null) return found;
			}
		// No suitable node found here
		return null;
	}
	
	public String toString(int indent) {
		String output="";
		for (int i=0; i<indent; i++) output+=" ";
		output += "("+category;
		for (TreeNode daughter : daughters) {
			output += "\n";
			output += daughter.toString(indent+2);
		}
		output+=")";
		return output;
	}

	@Override
	public List<String> getSentence() {
		// Concatenate the strings from each daughter
		List<String> sentence = new LinkedList<String>();
		boolean first = true;
		for (TreeNode daughter : daughters) {
			sentence.addAll(daughter.getSentence());
		}
		return sentence;
	}
}
