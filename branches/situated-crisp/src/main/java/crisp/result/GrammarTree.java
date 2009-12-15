package crisp.result;

import java.util.List;

/**
 * GrammarTree represents a tree read from the XTAG grammar.
 * It may be an auxiliary tree or an initial tree.
 * It has a word associated with it: this is the word of the lexical
 * entry from which it was retrieved. It also knows about this
 * word's POS. There is an ID field, which is the ID of the tree
 * in the grammar that the word was anchored in to get this tree.
 * 
 * substitutionSites provides a list of the nodes in the
 * tree at which substitution must take place.
 * 
 * adjunctionSites provides a list of the sites in the tree where
 * adjunction will be allowed (for easy reference).
 * 
 * @author Mark Wilding
 *
 */
public abstract class GrammarTree {
	protected String word;
	protected String pos;
	protected String id;
	protected TreeNode root;
	
	/**
	 * @return the word associated with the tree
	 */
	public String getWord() {
		return word;
	}
	/**
	 * @param word the word to set
	 */
	public void setWord(String word) {
		this.word = word;
	}
	
	/**
	 * @return the pos of the word
	 */
	public String getPos() {
		return pos;
	}
	/**
	 * @param pos the pos to set
	 */
	public void setPos(String pos) {
		this.pos = pos;
	}
	
	/**
	 * @return the id of the tree this was constructed from
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return the root node of the tree
	 */
	public TreeNode getRoot() {
		return root;
	}
	/**
	 * @param root the root to set
	 */
	public void setRoot(TreeNode root) {
		this.root = root;
	}
	
	/**
	 * @return a list of the substitution sites that exist in this tree
	 */
	public List<LeafTreeNode> getSubstitutionSites() {
		return root.getSubstitutionSites();
	}
}
