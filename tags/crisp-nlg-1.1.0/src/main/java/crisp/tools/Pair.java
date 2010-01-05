package crisp.tools;

/**
 * A generic pair, with equality checking passed on to
 * component types.
 * 
 * @author Mark Wilding
 *
 */
public class Pair<S,T> {
	private S first;
	private T second;
	
	public Pair(S first, T second) {
		this.first = first;
		this.second = second;
	}
	/*
	public boolean equals(Object otherObj) {
		if (!(otherObj instanceof Pair)) return false;
		
		Pair<S,T> other = (Pair<S,T>) otherObj;
		boolean result = (other!=null &&
				((first==null && other.getFirst()==null) || other.getFirst().equals(first)) && 
				((second==null && other.getSecond()==null) || other.getSecond().equals(second))
					);
		System.out.println("Comparing "+this+" to "+other+": "+result);
		return result;
	}*/

	public S getFirst() {
		return first;
	}

	public void setFirst(S first) {
		this.first = first;
	}

	public T getSecond() {
		return second;
	}

	public void setSecond(T second) {
		this.second = second;
	}
	
	public String toString() {
		return "("+first.toString()+","+second.toString()+")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Pair other = (Pair) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}
}
