package crisp.result;

import crisp.result.DerivationNode.DerivationException;

/**
 * The root node of a derivation tree.
 * 
 * @author Mark Wilding
 *
 */
public class RootDerivationNode extends DerivationNode {
	private SubstitutionDerivationNode initialSubst;
	
	public RootDerivationNode(String cat) {
		initialSubst = new SubstitutionDerivationNode("root",cat);
		addDaughter(initialSubst);
	}

	/* (non-Javadoc)
	 * @see crisp.result.DerivationNode#getGrammarTree()
	 */
	@Override
	public GrammarTree getGrammarTree() {
		// No valid grammar tree for this; shouldn't even be trying to get one
		return null;
	}

	@Override
	public void prepareDerivation(TreeNode parent) {
		getDaughter().prepareDerivation(parent);
	}

	/* (non-Javadoc)
	 * @see crisp.result.DerivationNode#performDerivation(crisp.result.TreeNode)
	 */
	@Override
	public void performDerivation() throws DerivationException {
		// Substitute the daughter into the parent
		getDaughter().performDerivation();
	}

	/**
	 * The root node has a single daughter. For consistency, this is
	 * added to the daughter list, but may also be accessed using this
	 * method.
	 * 
	 * @return the single daughter substitution node of this root node.
	 */
	public SubstitutionDerivationNode getDaughter() {
		return initialSubst;
	}
	
	public String toString(int indent) {
		String output = "";
		for (int i=0; i<indent; i++) output+=" ";
		output += "(Root\n";
		output += initialSubst.toString(indent+2);
		output +=")";
		return output;
	}
}
