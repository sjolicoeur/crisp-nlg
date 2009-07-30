package crisp.planner.lazygp;

import java.util.ArrayList;

public class Plan extends ArrayList<ActionInstance> {

	public Plan(Plan partialPlan) {
		super(partialPlan);
	}

	public Plan() {
		super();
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();

		for( int i = 0; i < size(); i++ ) {
			buf.append("" + i + ": " + get(i) + "\n");
		}

		return buf.toString();
	}
}
