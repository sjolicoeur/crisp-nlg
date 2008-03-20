package crisp.result;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import crisp.converter.IterableNodeList;
import crisp.result.AuxiliaryTree.WrongFootCategoryException;
import crisp.tools.IterableNodeWrapper;
import crisp.tools.NodeIterator;

/**
 * A class for getting information from an XTAG grammar
 * 
 * @author Mark Wilding
 */
public class Grammar {
	Document grammarDoc;
	XPath xpath;
	Map<String,Node> trees = new HashMap<String, Node>();
	Map<String,LinkedList<Node>> lexicon = new HashMap<String, LinkedList<Node>>();
	Map<Node,String> posMap = new HashMap<Node, String>();
	
	/**
	 * Create a grammar file accessor using the grammar file
	 * specified in the given problem file.
	 * 
	 * @param problemFilename location of the problem file 
	 * @throws IOException if there's a problem loading the file
	 */
	public Grammar(String problemFilename) throws IOException {
		try {
			loadGrammarFileFromProblem(problemFilename);
			// Load the tree definitions from the grammar
			loadTrees();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the location of the grammar file from a given problem
	 * filename and loads the grammar.
	 * 
	 * Most of this is directly from CRISPtoPDDL.
	 * 
	 * @param filename problem file
	 */
	private void loadGrammarFileFromProblem(String filename) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		// Parse the problem file
		File problemfile = new File(filename);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document problemdoc = builder.parse(problemfile);

		// Set up XPath engine
		XPathFactory xpfactory = XPathFactory.newInstance();
		xpath = xpfactory.newXPath();

		// Parse grammar file
		File grammarFile = new File(problemfile.getParentFile(), xpath.evaluate("/crispproblem/@grammar", problemdoc));
		grammarDoc = builder.parse(grammarFile);
	}
	
	/**
	 * Loads the trees into a hash map from the grammar file that's
	 * already been loaded.
	 * @throws XPathExpressionException 
	 */
	private void loadTrees() throws XPathExpressionException {
		
		// Read tree family definitions and store them in a map by name
		for( Node treeElement : nl(xg("/crisp-grammar/tree", XPathConstants.NODESET))) {
			String treeName = getAttribute(treeElement, "id");
			
			trees.put(treeName, treeElement);
		}
		
		// Read lexical entries and store them in a map by word
		for( Node entryElement : nl(xg("/crisp-grammar/entry", XPathConstants.NODESET)) ) {
			String word = getAttribute(entryElement, "word");
			String pos = getAttribute(entryElement, "pos");
			
			// Make sure we have a list in place for trees relating to this word (usually we won't)
			LinkedList<Node> wordNodes = lexicon.get(word);
			if (wordNodes==null) {
				wordNodes = new LinkedList<Node>();
				lexicon.put(word, wordNodes);
			}
			
			// Iterate over tree elements
			for (Node child : new IterableNodeWrapper(entryElement, true)) {
				// Only use sub-elements that are trees
				if ("tree".equals(child.getNodeName())) {
					// Add tree to the lexicon
					wordNodes.add(child);
					// Also note the POS of this lexical tree
					posMap.put(child, pos);
				}
			}
			
		}
	}
	
	private TreeNode buildTreeNodes(String treeName, List<Anchor> anchors) throws CouldNotLoadGrammarTreeException {
		Node treeSource = trees.get(treeName);
		
		if (treeSource==null) throw new CouldNotLoadGrammarTreeException();
		// There should only be one root node
		Node rootNode = null;
		IterableNodeWrapper inw = new IterableNodeWrapper(treeSource, true);
		rootNode = inw.iterator().next();
		
		// If there's no root node, we can't build a tree
		if (rootNode==null) {
			System.out.println("No root node in "+treeName);
			System.out.println("  "+treeSource);
			throw new CouldNotLoadGrammarTreeException();
		}
		
		// Build a TreeNode out of the grammar tree we have
		return getTreeNodeFromGrammarNode(rootNode, anchors, null);
	}
	
	/**
	 * Uses a Node read from the grammar to build a TAG TreeNode.
	 * Calling this will recursively build all the daughters as well.
	 * 
	 * @param grammarNode Input node from the XML grammar
	 * @param anchor Word to put in as anchor if encountered
	 * @param parent the parent of the tree node (null if it's the root note)
	 * @return the TreeNode built
	 * @throws CouldNotLoadGrammarTreeException 
	 */
	private TreeNode getTreeNodeFromGrammarNode(Node grammarNode, List<Anchor> anchors, InternalTreeNode parent) throws CouldNotLoadGrammarTreeException {
		// Check that this node is actually an element
		if (grammarNode.getNodeType()==Node.ELEMENT_NODE) {
			// Handle the strange substitution node (not leaf) in grammar (equiv to leaf)
			if ("leaf".equals(grammarNode.getNodeName()) ||
					("node".equals(grammarNode.getNodeName()) && 
							"substitution".equals(getAttributeIfExists(grammarNode, "type")))) {
				String cat = getAttributeIfExists(grammarNode, "cat");
				String sem = getAttributeIfExists(grammarNode, "sem");
				String index = getAttributeIfExists(grammarNode, "index");
				
				String type = getAttributeIfExists(grammarNode, "type");
				int typeNum = LeafTreeNode.getTypeNumberFromString(type);
				
				String word = null;
				if (typeNum==LeafTreeNode.LEAF_TYPE_ANCHOR) {
					Anchor anchored = null;
					// Check each available word to see if it can be anchored here
					for (Anchor anchor : anchors) {
						if (anchor.matches(cat)) {
							word = anchor.getWord();
							// Make sure the word isn't anchored elsewhere
							anchored = anchor;
						}
					}
					
					// Remove an anchor if we've used it
					if (anchored!=null) anchors.remove(anchored);
				}
				
				LeafTreeNode leaf = new LeafTreeNode(cat,index,sem,parent,word,typeNum);
				return leaf;
			} else if ("node".equals(grammarNode.getNodeName())) {
				String cat = getAttributeIfExists(grammarNode, "cat");
				String sem = getAttributeIfExists(grammarNode, "sem");
				String index = getAttributeIfExists(grammarNode, "index");
				
				InternalTreeNode node = new InternalTreeNode(cat,index,sem,parent);
				
				// Go through each child, recursively build it and add it as a daughter
				for (Node child : new IterableNodeWrapper(grammarNode, true)) {
					node.addRightDaughter(getTreeNodeFromGrammarNode(child, anchors, node));
				}
				
				return node;
			}
		}
		// No grammar tree could be built from the node in the grammar
		throw new CouldNotLoadGrammarTreeException();
	}
	
	/**
	 * Similar to getAttribute, but handles the case in which the attribute
	 * doesn't exist (some attributes are optional, e.g. index).
	 * 
	 * @param name name of attribute
	 * @param node node containing attribute
	 * @return value of attribute
	 */
	private String getAttributeIfExists(Node node, String name) {
		Node attNode = node.getAttributes().getNamedItem(name);
		if (attNode==null)
			return null;
		else
			return attNode.getNodeValue();
	}
	
	/**
	 * Loads the initial tree from the grammar that corresponds to the
	 * predicate name of the action that was used in a plan.
	 * 
	 * @param predicateName the name of the predicate used in a plan
	 * @return the grammar tree whose substitution is represented by the predicate
	 * @throws InvalidPredicateException 
	 * @throws XPathExpressionException 
	 * @throws CouldNotLoadGrammarTreeException 
	 */
	public InitialTree loadInitialTree(String predicateName) throws InvalidPredicateException, XPathExpressionException, CouldNotLoadGrammarTreeException {
		String cutName = "";
		if (!predicateName.startsWith("init-")) {
			// This predicate should not have been given to us
			throw new InvalidPredicateException();
		} else {
			cutName = predicateName.replaceFirst("init-", "i.");
		}
		
		// Divide the name into its parts: tree name, word, predicate index
		String[] nameParts = cutName.split("-");
		if (nameParts.length<3) throw new InvalidPredicateException();
		String treeName = nameParts[0];
		
		String word = nameParts[1];
		
		// Get the POS of the word that anchors in this tree
		String pos = null;
		LinkedList<Node> lexicalEntries = lexicon.get(word);
		for (Node node : lexicalEntries) {
			// Look for refid to correct tree
			String refid = getAttributeIfExists(node, "refid");
			if (treeName.equals(refid)) {
				pos = posMap.get(node);
				break;
			}
		}
		
		Anchor lexAnchor = new Anchor(pos,word);
		LinkedList<Anchor> anchors = new LinkedList<Anchor>();
		anchors.add(lexAnchor);
		
		// The last part is the arbitrary number, which we ignore
		// Any other parts relate to multiple anchors, e.g. in "takes from"
		for (int i=2; i<nameParts.length-1; i++) {
			// Found an auxiliary anchor for a lexical entry of the form POS:word
			String[] pair = nameParts[i].split(":");
			anchors.add(new Anchor(pair[0],pair[1]));
		}
		
		// Now build the InitialTree
		TreeNode rootNode = buildTreeNodes(treeName, anchors);
		InitialTree tree = new InitialTree(word, pos, treeName, rootNode);

		return tree;
	}
	
	/**
	 * Loads the auxiliary tree from the grammar that corresponds to the
	 * predicate name of the action that was used in a plan.
	 * 
	 * @param predicateName the name of the predicate used in a plan
	 * @return the grammar tree whose substitution is represented by the predicate
	 * @throws InvalidPredicateException 
	 * @throws XPathExpressionException 
	 * @throws WrongFootCategoryException 
	 * @throws CouldNotLoadGrammarTreeException 
	 */
	public AuxiliaryTree loadAuxiliaryTree(String predicateName) throws InvalidPredicateException, XPathExpressionException, WrongFootCategoryException, CouldNotLoadGrammarTreeException {
		String cutName = "";
		if (!predicateName.startsWith("aux-")) {
			// This predicate should not have been given to us
			throw new InvalidPredicateException();
		} else {
			cutName = predicateName.replaceFirst("aux-", "a.");
		}
		
		// Divide the name into its parts: tree name, word, predicate index
		String[] nameParts = cutName.split("-");
		if (nameParts.length<3) throw new InvalidPredicateException();
		String treeName = nameParts[0];
		
		String word = nameParts[1];
		
		// Get the POS of the word that anchors in this tree
		String pos = null;
		LinkedList<Node> lexicalEntries = lexicon.get(word);
		for (Node node : lexicalEntries) {
			// Look for refid to correct tree
			String refid = getAttributeIfExists(node, "refid");
			if (treeName.equals(refid)) {
				pos = posMap.get(node);
				break;
			}
		}
		
		Anchor lexAnchor = new Anchor(pos,word);
		LinkedList<Anchor> anchors = new LinkedList<Anchor>();
		anchors.add(lexAnchor);
		
		// The last part is the arbitrary number, which we ignore
		// Any other parts relate to multiple anchors, e.g. in "takes from"
		for (int i=2; i<nameParts.length-1; i++) {
			// Found an auxiliary anchor for a lexical entry of the form POS:word
			String[] pair = nameParts[i].split(":");
			anchors.add(new Anchor(pair[0],pair[1]));
		}
		
		// Now build the AuxiliaryTree
		TreeNode rootNode = buildTreeNodes(treeName, anchors);
		AuxiliaryTree tree = new AuxiliaryTree(word,pos,treeName,rootNode);

		return tree;
	}

	/**
	 * Evaluate the xpathExpr on the "grammar" XML document and interpret
	 * the result as specified by the "type" parameter.
	 *
	 * @param xpathExpr
	 * @param type
	 * @return
	 * @throws XPathExpressionException
	 */
	private Object xg(String xpathExpr, QName type) throws XPathExpressionException {
		return xpath.evaluate(xpathExpr, grammarDoc, type);
	}

	/**
	 * Convert the result of a NODESET XPath call to an IterableNodeList.
	 *
	 * @param o the result of a call to xpath.evaluate(..., XPathConstants.NODESET)
	 * @return an IterableNodeList containing these nodes
	 */
	private IterableNodeList nl(Object o) {
		return new IterableNodeList((NodeList) o);
	}
	
	/**
	 * Returns the value of the attribute with name "attributeName" from the
	 * XML element "node".
	 *
	 * @param node
	 * @param attributeName
	 * @return
	 */
	private static String getAttribute(Node node, String attributeName) {
		return node.getAttributes().getNamedItem(attributeName).getTextContent();
	}
	
	public class InvalidPredicateException extends Exception {}
	public class CouldNotLoadGrammarTreeException extends Exception {}
}
