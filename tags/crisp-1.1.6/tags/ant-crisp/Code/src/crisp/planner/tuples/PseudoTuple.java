package crisp.planner.tuples;

import java.util.ArrayList;

public class PseudoTuple extends ArrayList<String> {
	public String toString() {
		return super.toString().replaceAll("\\[","(").replaceAll("\\]", ")");
	}
}
