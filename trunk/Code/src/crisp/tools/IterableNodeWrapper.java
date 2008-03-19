package crisp.tools;

import java.util.Iterator;

import org.w3c.dom.Node;

/**
 * Wrapper for Node so we can iterate over one. By default,
 * iterates over all daughters of the node. If onlyElements
 * is set to true, iterates over only nodes of element type.
 * 
 * @author Mark Wilding
 *
 */
public class IterableNodeWrapper implements Iterable<Node> {
	private Node node;
	private boolean onlyElements = false;
	private NodeIterator iterator;
	
	public IterableNodeWrapper(Node node) {
		this.node = node;
		iterator = new NodeIterator(node, onlyElements);
	}
	
	public IterableNodeWrapper(Node node, boolean onlyElements) {
		this.onlyElements = onlyElements;
		this.node = node;
		iterator = new NodeIterator(node, onlyElements);
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Node> iterator() {
		return iterator;
	}

}
