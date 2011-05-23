package crisp.planner.tuples;

import java.util.List;

import org.gecode.IntVar;
import org.gecode.JavaSpace;

public abstract class TupleSelectionConstraint {
	public static TupleSelectionConstraint makeSelectionConstraint(JavaSpace home, int n) {
		return null;
	}
	
	public abstract void selectContains(TupleSetVariable set, List<List<Tuple>> tupleSets, IntVar I);
	public abstract void selectDisjoint(TupleSetVariable set, List<List<Tuple>> tupleSets, IntVar I);
	
	public abstract void selectPlusMinus(TupleSetVariable T2, TupleSetVariable T1, List<List<Tuple>> plus, List<List<Tuple>> minus, IntVar I);
	
	
}
