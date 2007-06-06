package crisp.planner.tuples;

import org.gecode.JavaSpace;

public interface TupleSetVariable {
	public TupleSetVariable copy(JavaSpace newHome, boolean share);
}
