package crisp.planner.lazygp;

import java.util.BitSet;

public class ActionLayer {
	private final State previousState;

	private final int maxApplicableAction;
	private final ActionInstanceTable table;

	private final BitSet[] mutexActionInstances;

	private void setMutex(int inst1, int inst2) {
		if( mutexActionInstances[inst1] == null ) {
			mutexActionInstances[inst1] = new BitSet(maxApplicableAction+1);
		}

		mutexActionInstances[inst1].set(inst2);

		if( mutexActionInstances[inst2] == null ) {
			mutexActionInstances[inst2] = new BitSet(maxApplicableAction+1);
		}

		mutexActionInstances[inst2].set(inst1);
	}

	public ActionLayer(State previousState, int maxApplicableAction) {
		this.previousState = previousState;

		table = previousState.getActionTable();

		this.maxApplicableAction = maxApplicableAction;
		mutexActionInstances = new BitSet[maxApplicableAction+1];
	}

	public State getNextState() {
		return new State(previousState, this);
	}


	public void computeMutexActionInstances() {
		// TODO perhaps this can be done more efficiently than quadratic
		for( int inst1 = 0; inst1 <= maxApplicableAction; inst1++ ) {
			for( int inst2 = 0; inst2 <= maxApplicableAction; inst2++ ) {
				if( computeIsMutex(inst1, inst2)) {
					setMutex(inst1, inst2);
				}
			}
		}
	}

	private boolean computeIsMutex(int inst1ix, int inst2ix) {
		// not interested in self-mutexness
		if( inst1ix == inst2ix ) {
			return false;
		}

		// dependent actions are mutex
		if( isDependent(inst1ix, inst2ix) || isDependent(inst2ix, inst1ix) ) {
			//System.err.println(inst1.toString() + " mutex with " + inst2.toString() + " because they are dependent!");
			return true;
		}

		ActionInstance inst1 = table.get(inst1ix);
		ActionInstance inst2 = table.get(inst2ix);


		// actions with mutex preconditions are mutex
		for( int i = 0; i < inst1.getPreconditionIndices().length; i++ ) {
			for( int j = 0; j < inst2.getPreconditionIndices().length; j++ ) {
				if( previousState.isMutex(inst1.getPreconditionIndices()[i], inst1.getPreconditionPolarities()[i],
						inst2.getPreconditionIndices()[j], inst2.getPreconditionPolarities()[j]) ) {
					/*
					System.err.println(inst1.toString() + " mutex with " + inst2.toString() + " because of mutex preconditions "
							+ previousState.getTable().get(inst1.getPreconditionIndices()[i]) + " and "
							+ previousState.getTable().get(inst2.getPreconditionIndices()[j]));
							*/
					return true;
				}
			}
		}


		return false;
	}

	public boolean isMutex(ActionInstance inst1, ActionInstance inst2) {
		return isMutex(table.getIndexForAtom(inst1), table.getIndexForAtom(inst2));
	}

	public boolean isMutex(int inst1ix, int inst2ix) {
		BitSet mutexes = mutexActionInstances[inst1ix];

		return mutexes != null && mutexes.get(inst2ix);
	}


	/*  ** this doesn't do any good **
	private static Map<ActionInstance,Set<ActionInstance>> dependencyCache = new HashMap<ActionInstance, Set<ActionInstance>>();
	private static Set<ActionInstance> knownActionInstances = new HashSet<ActionInstance>();

	private static void addDependency(ActionInstance inst1, ActionInstance inst2) {
		if( !knownActionInstances.contains(inst1) ) {
			knownActionInstances.add(inst1);
			dependencyCache.put(inst1, new HashSet<ActionInstance>());
		}

		if( !knownActionInstances.contains(inst2)) {
			knownActionInstances.add(inst2);
			dependencyCache.put(inst2, new HashSet<ActionInstance>());
		}

		dependencyCache.get(inst1).add(inst2);
		dependencyCache.get(inst2).add(inst1);
	}
	*/

	// OPT: Can I do some static analysis to speed this up?
	// OPT: effects of the two are now compared twice (waste of time)
	private boolean isDependent(int inst1ix, int inst2ix) {
		//System.err.println("effects of " + inst1 + ": " + Arrays.toString(inst1.getEffectIndices()));
		//System.err.println("preconditions of " + inst2 + ": " + Arrays.toString(inst2.getPreconditionIndices()));

		// TODO - put the caching back in!

		/*
		if( knownActionInstances.contains(inst1) && knownActionInstances.contains(inst2) ) {
			return dependencyCache.get(inst1).contains(inst2);
		}
		*/

		ActionInstance inst1 = table.get(inst1ix);
		ActionInstance inst2 = table.get(inst2ix);

		for( int i = 0; i < inst1.getEffectIndices().length; i++ ) {
			int i1 = inst1.getEffectIndices()[i];
			boolean pol1 = inst1.getEffectPolarities()[i];

			// action instances are dependent if an effect of inst1 destroys
			// a precondition of inst2
			for( int j = 0; j < inst2.getPreconditionIndices().length; j++ ) {
				int i2 = inst2.getPreconditionIndices()[j];
				boolean pol2 = inst2.getPreconditionPolarities()[j];

				//System.err.println("isDep: compare effect " + inst1.getEffects().get(i) + " with precond " + inst2.getPreconditions().get(j));
				//System.err.println("isDep: compare effect " + i1 + " with precond " + i2);

				if( i1 == i2 && pol1 != pol2 ) {
					//System.err.println("Interaction between effect and precond " + previousState.getTable().get(i1));
					/// addDependency(inst1, inst2);
					return true;
				}
			}

			// ... or if an effect of inst1 and an effect of inst2 contradict
			// each other
			for( int j = 0; j < inst2.getEffectIndices().length; j++ ) {
				int i2 = inst2.getEffectIndices()[j];
				boolean pol2 = inst2.getEffectPolarities()[j];

				if( i1 == i2 && pol1 != pol2 ) {
					//System.err.println("Interaction between effect and effect " + previousState.getTable().get(i1));
					/// addDependency(inst1, inst2);
					return true;
				}
			}
		}


		return false;
	}


	@Override
    public String toString() {
		StringBuilder buf = new StringBuilder();

		for( int i = 0; i <= maxApplicableAction; i++ ) {
			ActionInstance inst = table.get(i);
			buf.append("   " + inst.toString() + " - mutex with:");

			for( int j = 0; j <= maxApplicableAction; j++ ) {
				ActionInstance inst2 = table.get(j);

				if( isMutex(i,j) ) {
					buf.append(" " + inst2);
				}
			}

			buf.append("\n");
		}

		buf.append("\n");


		return buf.toString();
	}


	public State getPreviousState() {
		return previousState;
	}



	public int getNumActionInstances() {
		return maxApplicableAction + 1;
	}


}
