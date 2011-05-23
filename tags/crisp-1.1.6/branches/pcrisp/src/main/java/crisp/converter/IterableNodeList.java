package crisp.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IterableNodeList implements Iterable<Node>, NodeList {
	private NodeList x;
	private List<Node> xAsList;
	
	public IterableNodeList(NodeList x) {
		this.x = x;
		
		xAsList = new ArrayList<Node>();
		for( int i = 0; i < x.getLength(); i++ ) {
			xAsList.add(x.item(i));
		}
	}

	public int getLength() {
		return x.getLength();
	}

	public Node item(int arg0) {
		return x.item(arg0);
	}

	public Iterator<Node> iterator() {
		return xAsList.iterator();
	}
	
	public Collection<Node> asCollection() {
		return xAsList;
	}

}
