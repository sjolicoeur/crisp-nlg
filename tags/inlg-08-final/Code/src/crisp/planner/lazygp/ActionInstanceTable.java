package crisp.planner.lazygp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionInstanceTable {
	private List<ActionInstance> indexToInstance;
	private Map<ActionInstance,Integer> instanceToIndex;
	private int nextIndex;
	
	public ActionInstanceTable() {
		indexToInstance = new ArrayList<ActionInstance>();
		instanceToIndex = new HashMap<ActionInstance, Integer>();
		nextIndex = 0;
	}
	
	int getIndexForAtom(ActionInstance a) {
		Integer ret = instanceToIndex.get(a);
		
		if( ret == null ) {
			return -1;
		} else {
			return ret;
		}
	}	
	
	public ActionInstance get(int i) {
		return indexToInstance.get(i);
	}
	
	private void add(ActionInstance a) {
		//System.err.println("(add " + a + ")");
		indexToInstance.add(a);
		instanceToIndex.put(a, nextIndex);
		
		nextIndex++;
	}
	
	public int ensureKnown(ActionInstance a) {
		int ret = getIndexForAtom(a);
		
		if( ret > -1 ) {
			return ret;
		} else {
			add(a);
			return nextIndex-1;
		}
	}
	
	public int size() {
		return nextIndex;
	}
}
