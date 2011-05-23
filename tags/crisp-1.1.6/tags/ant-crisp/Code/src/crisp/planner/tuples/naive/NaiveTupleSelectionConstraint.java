package crisp.planner.tuples.naive;

import java.util.List;

import org.gecode.IntVar;
import org.gecode.JavaSpace;

import crisp.planner.tuples.Tuple;
import crisp.planner.tuples.TupleSelectionConstraint;
import crisp.planner.tuples.TupleSetVariable;

public class NaiveTupleSelectionConstraint extends TupleSelectionConstraint {
	public static TupleSelectionConstraint makeSelectionConstraint(JavaSpace home, int n) {
		return new NaiveTupleSelectionConstraint();
	}

	@Override
	public void selectContains(TupleSetVariable set, List<List<Tuple>> tupleSets, IntVar I) {
		
		
		
		
		// TODO Auto-generated method stub

	}

	@Override
	public void selectDisjoint(TupleSetVariable set, List<List<Tuple>> tupleSets, IntVar I) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectPlusMinus(TupleSetVariable T2, TupleSetVariable T1, List<List<Tuple>> plus, List<List<Tuple>> minus, IntVar I) {
		// TODO Auto-generated method stub

	}

}
