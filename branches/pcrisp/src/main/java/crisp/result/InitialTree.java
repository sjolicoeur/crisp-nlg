package crisp.result;

import java.util.List;

/**
 * An initial TAG tree from the grammar.
 * 
 * @author Mark Wilding
 *
 */
public class InitialTree extends GrammarTree {
	/**
	 * Constructs a new InitialTree from the given (root node of a) tree.
	 * 
	 * @param word Word of lexical entry
	 * @param pos POS of lexical entry
	 * @param id ID of tree set
	 * @param root Root node of tree
	 */
	public InitialTree(String word, String pos, String id, TreeNode root) {
		this.word = word;
		this.pos = pos;
		this.id = id;
		this.root = root;
	}
	
	public String toString() {
		return "Initial tree for \""+word+"\":\n"+root.toString(2);
	}
}
