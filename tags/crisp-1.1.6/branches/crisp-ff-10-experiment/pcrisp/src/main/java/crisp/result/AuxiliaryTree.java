package crisp.result;

import java.util.List;

/**
 * 
 * @author Mark Wilding
 *
 */
public class AuxiliaryTree extends GrammarTree {
	private LeafTreeNode foot;

	/**
	 * Constructs a new InitialTree from the given (root node of a) tree.
	 * 
	 * @param word Word of lexical entry
	 * @param pos POS of lexical entry
	 * @param id ID of tree set
	 * @param root Root node of tree
	 * @param substitutionSites List of all substitution sites in the tree
	 * @throws WrongFootCategoryException if the foot category doesn't match the root category
	 */
	public AuxiliaryTree(String word, String pos, String id, TreeNode root
			) throws WrongFootCategoryException {
		this.word = word;
		this.pos = pos;
		this.id = id;
		this.root = root;
		setFoot(root.getFoot());
	}

	/**
	 * @return the foot of the tree
	 */
	public LeafTreeNode getFoot() {
		return foot;
	}

	/**
	 * @param foot the foot to set. Must be of the same category as the root node.
	 */
	public void setFoot(LeafTreeNode foot) throws WrongFootCategoryException {
		if (!foot.getCategory().equals(root.getCategory()))
			throw new WrongFootCategoryException();
		
		this.foot = foot;
	}
	
	public class WrongFootCategoryException extends Exception {
		private static final long serialVersionUID = 8284831516760125957L;
	}
	
	public String toString() {
		return "Auxiliary tree for \""+word+"\":\n"+root.toString(2);
	}
}
