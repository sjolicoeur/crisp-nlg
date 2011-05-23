package crisp.planner.tuples.naive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import crisp.planner.tuples.ConstantTuple;

public class TupleSignature {
	private List<List<String>> values;
	
	public TupleSignature(List<String> types, Map<String,String> typedUniverse) {
		values = new ArrayList<List<String>>(types.size());
		
		for( String type : types ) {
			List<String> domain = new ArrayList<String>();
			values.add(domain);
			
			for( Map.Entry<String, String> c : typedUniverse.entrySet() ) {
				if( c.getValue().equals(type)) {
					domain.add(c.getKey());
				}
			}
		}
	}
	
	public int encode(ConstantTuple tuple) {
		int ret = 0;
		
		for( int i = tuple.size()-1; i >= 0; i-- ) {
			ret *= values.get(i).size();
			ret += getNumericValue(tuple.get(i), i);
		}

		return ret;
	}

	private int getNumericValue(String con, int i) {
		for( int j = 0; j < values.get(i).size(); j++ ) {
			if( con.equals(values.get(i).get(j))) {
				return j;
			}
		}
		
		return -1;
	}
	
	public ConstantTuple decode(int val) {
		ConstantTuple ret = new ConstantTuple();
		
		for( int i = values.size()-1; i >= 0; i-- ) {
			int here = val % values.get(i).size();
			val /= values.get(i).size();
			
			ret.add(values.get(i).get(here));
		}
		
		return ret;
	}
	
	public int getMaximumValue() {
		int ret = 0;
		
		for( int i = values.size()-1; i >= 0; i-- ) {
			ret *= values.get(i).size();
			ret += values.get(i).size()-1;
		}

		return ret;
	}
}
