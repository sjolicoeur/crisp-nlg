package crisp.planner.tuples.naive;

import java.util.List;

import org.gecode.Gecode;
import org.gecode.IntSet;
import org.gecode.JavaSpace;
import org.gecode.SetRelType;
import org.gecode.SetVar;

import crisp.planner.tuples.ConstantTuple;
import crisp.planner.tuples.TupleSetVariable;

public class NaiveTupleSetVariable implements TupleSetVariable {
	private SetVar var;
	private JavaSpace home;
	private TupleSignature sig;
	
	public NaiveTupleSetVariable(JavaSpace home, TupleSignature sig) {
		this.home = home;
		this.sig = sig;
		
		var = new SetVar(home, new IntSet(new int[] { }), new IntSet(0, sig.getMaximumValue()));
	}
	
	private NaiveTupleSetVariable(JavaSpace newHome, SetVar var2, boolean share, TupleSignature sig2) {
		home = newHome;
		var = var2.copy(newHome, share);
		sig = sig2;
	}
	
	public NaiveTupleSetVariable(JavaSpace home, TupleSignature sig, List<ConstantTuple> elements) {
		this(home,sig);
		
		int[] values = new int[elements.size()];
		for( int i = 0; i < elements.size(); i++ ) {
			values[i] = sig.encode(elements.get(i));
		}
		
		Gecode.dom(home, var, SetRelType.SRT_EQ, new IntSet(values));
	}
	
	
	

	public TupleSetVariable copy(JavaSpace newHome, boolean share) {
		NaiveTupleSetVariable newVar = new NaiveTupleSetVariable(newHome, var, share, sig);
		
		return newVar;
	}

}
