package crisp.tools;

import java.util.Iterator;

import org.w3c.dom.Node;

/**
 * Iterates over the daughters of the given Node.
 * Only returns nodes of element type if onlyElements is set to true.
 *
 * @author Mark Wilding
 */
public class NodeIterator implements Iterator<Node> {
	private final Node parent;
	private Node currentDaughter;
	private boolean onlyElements = false;

	public NodeIterator(Node parent, boolean onlyElements) {
		this.parent = parent;
		this.onlyElements = onlyElements;

		if (parent==null) {
            currentDaughter = null;
        } else {
            currentDaughter = parent.getFirstChild();
        }

		// If this is a node, but not an element, get the next element
		if (onlyElements && currentDaughter!=null && currentDaughter.getNodeType()!=Node.ELEMENT_NODE) {
			currentDaughter = getNextElement(currentDaughter);
		}
	}

	public NodeIterator(Node parent) {
		this.parent = parent;
		currentDaughter = parent.getFirstChild();
	}

	private Node getNextElement(Node current) {
		Node next = current;

		if (next==null) {
            return null;
        } else {
            next = next.getNextSibling();
        }

		if (onlyElements) {
			while (next!=null) {
				if (next.getNodeType()==Node.ELEMENT_NODE) {
					return next;
				} else {
					next = next.getNextSibling();
				}
			}
			return null;
		} else {
			return next;
		}
	}

	public boolean hasNext() {
		return currentDaughter!=null;
	}

	public Node next() {
		Node daughter = currentDaughter;
		// Get the next sibling, or next element if onlyElements is set
		currentDaughter = getNextElement(currentDaughter);
		return daughter;
	}

	public void remove() {
		// Can't remove things
	}

}
