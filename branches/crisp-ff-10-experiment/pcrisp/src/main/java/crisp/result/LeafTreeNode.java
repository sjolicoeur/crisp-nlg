package crisp.result;

import java.util.LinkedList;
import java.util.List;


/**
 * Leaf represents a leaf in a TAG tree. This could be a word
 * (all leaves are words in a final tree), a foot (in an auxiliary 
 * tree), or a substitution site (in any tree not yet fully 
 * specified).
 * 
 * @author Mark Wilding
 *
 */
public class LeafTreeNode extends TreeNode {
	/** Enumeration of unknown leaf type - we hope this won't occur! */
	public static final int LEAF_TYPE_UNKNOWN = 0;
	/** Enumeration of anchor leaf type (to attach to a specific word) */
	public static final int LEAF_TYPE_ANCHOR = 1;
	/** Enumeration of substitution site leaf type (for substitution) */
	public static final int LEAF_TYPE_SUBSTITUTION = 2;
	/** Enumeration of foot leaf type (for adjunction) */
	public static final int LEAF_TYPE_FOOT = 3;
	/** Enumeration of terminal leaf type (role but no word) */
	public static final int LEAF_TYPE_TERMINAL = 4;
	
	private String word;
	private int type;
	private TreeNode substitutedWith = null;
	private TreeNode footReplacement = null;

	/**
	 * Constructs a leaf node for a tag tree
	 * 
	 * @param category Grammatical category of node
	 * @param index Index of node in grammar
	 * @param sem Semantic value of node
	 * @param parent Parent node (null for root node)
	 * @param word The word represented by the leaf if any, otherwise null
	 * @param type One of the valid types of a leaf (see LeafTreeNode.LEAF_TYPE_*)
	 */
	public LeafTreeNode(String category, String index, String sem, InternalTreeNode parent, String word, int type) {
		this.parent = parent;
		this.category = category;
		this.index = index;
		this.sem = sem;
		this.word = word;
		this.type = type;
	}
	

	@Override
	public int getNumberOfNodes() {
		// Only 1 node in this tree!
		return 1;
	}


	/**
	 * @return the word represented by this leaf if any, otherwise null
	 */
	public String getWord() {
		return word;
	}
	/**
	 * If setting to null, make sure the type is set appropriately
	 * 
	 * @param word the word to associate with the node.
	 */
	public void setWord(String word) {
		if (word!=null) {
			setType(LeafTreeNode.LEAF_TYPE_ANCHOR);
		}
		this.word = word;
	}


	/**
	 * @return the type of the node (see LeafTreeNode.LEAF_TYPE_*).
	 */
	public int getType() {
		return type;
	}
	/**
	 * @param type a valid node type (LeafTreeNode.LEAF_TYPE_*).
	 */
	public void setType(int type) {
		// If it's not an anchor, it doesn't make sense to have a word set.
		if (type!=LeafTreeNode.LEAF_TYPE_ANCHOR)
			setWord(null);
		this.type = type;
	}
	
	/**
	 * @return true if the leaf represents a fully resolved word in the sentence.
	 */
	public boolean isWord() {
		return (getType() == LeafTreeNode.LEAF_TYPE_ANCHOR && getWord() != null);
	}
	
	public static int getTypeNumberFromString(String typeString) {
		if ("substitution".equals(typeString))
			return LEAF_TYPE_SUBSTITUTION;
		else if ("anchor".equals(typeString))
			return LEAF_TYPE_ANCHOR;
		else if ("foot".equals(typeString))
			return LEAF_TYPE_FOOT;
		else if ("terminal".equals(typeString))
			return LEAF_TYPE_TERMINAL;
		else
			return LEAF_TYPE_UNKNOWN;
	}


	@Override
	public List<LeafTreeNode> getSubstitutionSites() {
		// Empty list if this isn't a substitution site, one-element list if it is
		List<LeafTreeNode> sites = new LinkedList<LeafTreeNode>();
		if (type==LEAF_TYPE_SUBSTITUTION)
			sites.add(this);
		return sites;
	}


	@Override
	public LeafTreeNode getFoot() {
		if (type==LEAF_TYPE_FOOT) return this;
		else return null;
	}


	@Override
	public TreeNode getAdjunctionSite(String cat, String sem) {
		if (this.category.equals(cat) && this.sem.equals(sem))
			return this;
		else
			return null;
	}

	/**
	 * If this is a substitution node and something has been substituted
	 * here, keeps a pointer to the root node of the tree that was 
	 * substituted.
	 * 
	 * Note that if the derivation has been planned but not yet performed,
	 * this may point to a planned substitution that has not yet taken 
	 * place. Not much should be done between planning and performing 
	 * derivations, so this should not cause a problem, but beware.
	 * 
	 * @return the root node of the tree that was substituted here, if any. Otherwise null.
	 */
	public TreeNode getSubstitutedWith() {
		return substitutedWith;
	}

	/**
	 * Record that a node has been substituted (or will be substituted)
	 * for this one. This should be set when planning substitutions.
	 * It ensures uniqueness of substitutions and also that adjunctions
	 * are made to the substituted node, not the old substitution site.
	 * 
	 * @param substitutedWith the node substituted for this one.
	 */
	public void setSubstitutedWith(TreeNode substitutedWith) {
		this.substitutedWith = substitutedWith;
	}
	
	public String toString(int indent) {
		String output = "";
		for (int i=0; i<indent; i++) output += " ";
		output += "("+category+" type="+type+(type==LEAF_TYPE_ANCHOR ? " \""+word+"\" \""+sem+"\"" : "")+")";
		return output;
	}


	/**
	 * If this is a foot node of an auxiliary tree that has already
	 * been adjoined, and we now want to adjoin to it, we should 
	 * adjoin to the root of the tree that replaced the foot node.
	 * This returns a pointer to that node.
	 * 
	 * @return a pointer to the node that has replaced this foot node, if any; null otherwise.
	 */
	public TreeNode getFootReplacement() {
		return footReplacement;
	}

	/**
	 * If this is the foot of an auxiliary tree, this should be set
	 * when the adjunction is prepared, pointing to the node that is
	 * replacing the foot (i.e. the node to which we're adjoining).
	 * 
	 * @param footReplacement the node replacing this foot
	 */
	public void setFootReplacement(TreeNode footReplacement) {
		this.footReplacement = footReplacement;
	}


	@Override
	public List<String> getSentence() {
		// If this is a word, return it; otherwise, we can't return a proper sentence
		List<String> wordList = new LinkedList<String>();
		if (word!=null) wordList.add(word);
		else wordList.add("<type="+type+">");
		
		return wordList;
	}
}
