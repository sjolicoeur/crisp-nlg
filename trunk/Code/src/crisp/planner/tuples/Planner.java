package crisp.planner.tuples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gecode.DFSSearch;
import org.gecode.Expr;
import org.gecode.Gecode;
import org.gecode.IntVar;
import org.gecode.Space;
import org.gecode.Statistics;
import org.gecode.VarArray;
import org.gecode.gist.Gist;

import crisp.pddl.PddlParser;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.TypedVariableList;
import crisp.planningproblem.goal.Literal;
import de.saar.basic.StringTools;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

public class Planner extends Space {
	@Option(label="Plan size")
	private static int PLANSIZE;

	@Option(label="Use Gist", defaultValue="true")
	private static boolean usingGui;


	private static File domainname;
	private static File problemname;
	private int plansize;

	// the constraint variables
	private final VarArray<IntVar> selectedAction;
	private final List<Map<String,TupleSetVariable>> predicate; // predicate.get(step).get(predicatesymbol)
	private final VarArray<IntVar> actionParameters;

	private Map<String,Integer> constantIndices; // TODO copy me



	public Planner(Domain domain, Problem problem, int plansize) {
		super();

		int parameterIndex = 1; //XX

		setName("crisp-planner-tuples");
		this.plansize = plansize;

		int numActions = domain.getActions().size();
		List<String> predicates = new ArrayList<String>();

		actionParameters = new VarArray<IntVar>();

		// initialize selected actions and predicate extensions
		selectedAction = new VarArray<IntVar>(this, plansize, IntVar.class, 0, numActions); // numActions = NOP

		//XX
		System.err.println("## Declaration of action selection variables ##");
		System.err.println("## (" + numActions + " declared actions plus one no-op action) ##\n");
		for( int i = 0; i < plansize; i++ ) {
			System.err.println("new FD variable A[" + i + "] with domain 0.." + numActions);
		}

		predicate = new ArrayList<Map<String,TupleSetVariable>>(plansize+1);

		predicates = new ArrayList<String>();
		constantIndices = new HashMap<String, Integer>();

		System.err.println("\n## Declaration of tuple-set variables ##");
		System.err.println("## (each of these denotes the extension of one predicate symbol at one state) ##\n");
		for( Predicate p : domain.getPredicates() ) {
			predicates.add(p.getLabel());

			//XX
			for( int i = 0; i <= plansize; i++ ) {
				System.err.print("new tuple-set variable S[" + i + "," + p.getLabel() + "] word length = " + p.getVariables().getItems().size() +
						", lower bound=\\emptyset, upper bound=/");
				for( Variable var : p.getVariables().getItems() ) {
					System.err.print("[0.." + (domain.getUniverse(p.getVariables().getType(var)).size()-1) + "]");
				}
				System.err.println("/");
			}
		}
		System.err.println();

		for( int i = 0; i <= plansize; i++ ) {
			Map<String,TupleSetVariable> map = new HashMap<String,TupleSetVariable>();

			for( String p : predicates ) {
				map.put(p, null); // TODO - put a real tuple set variable here!
			}
			predicate.add(map);
		}

		// specify initial state
		int nextConstantIndex = 0;
		for( String con : domain.getUniverse().keySet() ) {
			constantIndices.put(con, nextConstantIndex++);
		}


		System.err.println("\n## Initial state ##\n");
		for( String p : predicates ) {
			// TODO do something real here
			System.err.print("S[0," + p + "] = {");

			for( Term atom : problem.getInitialState() ) {
				if (atom instanceof Compound) {
					Compound termAsCompound = (Compound) atom;
					if( p.equals(termAsCompound.getLabel())) {
						System.err.print(atomToPseudoTuple(termAsCompound, null, constantIndices) + " ");
					}
				}
			}

			System.err.println("}");
		}

		// specify final state
		// TODO

		// encode the actions
		for( int i = 0; i < plansize; i++ ) {
			Map<Integer,Map<String,IntVar>> actionParameterMap = new HashMap<Integer,Map<String,IntVar>>();
			Map<Integer,Map<String,String>> actionParameterMap2 = new HashMap<Integer,Map<String,String>>(); //XX
			List<List<Literal>> actionGoals = new ArrayList<List<Literal>>();
			List<List<crisp.planningproblem.effect.Literal>> actionEffects = new ArrayList<List<crisp.planningproblem.effect.Literal>>();

			System.err.println("\n## FD variables and actions for step " + i + " ##\n");

			// extract static information from the actions
			for( int j = 0; j < numActions; j++ ) {
				// make variables for all action parameters
				Map<String,IntVar> myParameterMap = new HashMap<String,IntVar>();
				Map<String,String> myParameterMap2 = new HashMap<String,String>(); //XX
				TypedVariableList vars = domain.getActions().get(j).getPredicate().getVariables();

				actionParameterMap.put(j, myParameterMap);
				actionParameterMap2.put(j, myParameterMap2);

				for( Variable param : vars.getItems() ) {
					IntVar p = new IntVar(this, 0, domain.getUniverse(vars.getType(param)).size()-1);
					actionParameters.add(p);
					myParameterMap.put(param.getName(), p);

					String name = "Xp" + (parameterIndex++);
					myParameterMap2.put(param.getName(), name);
					System.err.println("new FD variable " + name + " with domain 0.." + (domain.getUniverse(vars.getType(param)).size()-1));
				}

				// get preconditions and effects
				// TODO check static applicability (=> selectedAction != j)
				actionGoals.add(domain.getActions().get(j).getPrecondition().getGoalList(problem));
				actionEffects.add(domain.getActions().get(j).getEffect().getEffects(problem));
			}

			System.err.println();

			for( String p : predicates ) {
				TupleSelectionConstraint c = TupleSelectionConstraint.makeSelectionConstraint(this, numActions+1);

				List<List<PseudoTuple>> plus = new ArrayList<List<PseudoTuple>>();
				List<List<PseudoTuple>> minus = new ArrayList<List<PseudoTuple>>();
				List<List<PseudoTuple>> subsets = new ArrayList<List<PseudoTuple>>();
				List<List<PseudoTuple>> disjoints = new ArrayList<List<PseudoTuple>>();

				for( int j = 0; j < numActions; j++ ) {
					List<PseudoTuple> myplus = new ArrayList<PseudoTuple>();
					List<PseudoTuple> myminus = new ArrayList<PseudoTuple>();
					List<PseudoTuple> mysubsets = new ArrayList<PseudoTuple>();
					List<PseudoTuple> mydisjoints = new ArrayList<PseudoTuple>();

					plus.add(myplus);
					minus.add(myminus);
					subsets.add(mysubsets);
					disjoints.add(mydisjoints);

					// preconditions
					for( Literal goal : actionGoals.get(j) ) {
						Compound goalAsCompound = (Compound) (goal.getAtom());
						if( goalAsCompound.getLabel().equals(p)) {
							PseudoTuple t = atomToPseudoTuple(goalAsCompound, actionParameterMap2.get(j), constantIndices);
							(goal.getPolarity() ? mysubsets : mydisjoints).add(t);
						}
					}

					// effects
					for( crisp.planningproblem.effect.Literal effect : actionEffects.get(j)) {
						Compound effectAsCompound = (Compound) (effect.getAtom());
						if( effectAsCompound.getLabel().equals(p)) {
							PseudoTuple t = atomToPseudoTuple(effectAsCompound, actionParameterMap2.get(j), constantIndices);
							(effect.getPolarity() ? myplus : myminus).add(t);
						}
					}
				}

				// NOP action
				subsets.add(new ArrayList<PseudoTuple>());
				disjoints.add(new ArrayList<PseudoTuple>());
				plus.add(new ArrayList<PseudoTuple>());
				minus.add(new ArrayList<PseudoTuple>());

				// post precondition constraints
				System.err.println(printTupleListList(subsets) + "[A" + i + "]) \\subseteq S[" + i + "," + p + "]");
				System.err.println(printTupleListList(disjoints) + "[A" + i + "]) disjoint with S[" + i + "," + p + "]");
				System.err.println("S[" + (i+1) + "," + p + "] = S[" + i + "," + p + "] + " + printTupleListList(plus) + "[A" + i + "] - " + printTupleListList(minus) + "[A" + i + "]\n");


				//c.selectContains(predicate.get(i).get(p), subsets, selectedAction.get(i));
				//c.selectDisjoint(predicate.get(i).get(p), disjoints, selectedAction.get(i));

				// post effects constraint
				//c.selectPlusMinus(predicate.get(i+1).get(p), predicate.get(i).get(p), plus, minus, selectedAction.get(i));
			}

		}

		// XX - final state
		Map<String,List<PseudoTuple>> posFinalTuples = new HashMap<String, List<PseudoTuple>>();
		Map<String,List<PseudoTuple>> negFinalTuples = new HashMap<String, List<PseudoTuple>>();
		for( Literal lit : problem.getGoal().getGoalList(problem) ) {
			Map<String,List<PseudoTuple>> map = lit.getPolarity() ? posFinalTuples : negFinalTuples;
			String pred = ((Compound) lit.getAtom()).getLabel();
			List<PseudoTuple> tupleList = map.get(pred);

			if( tupleList == null ) {
				tupleList = new ArrayList<PseudoTuple>();
				map.put(pred, tupleList);
			}

			tupleList.add(atomToPseudoTuple((Compound) lit.getAtom(), null, constantIndices));
		}

		System.err.println("\n## Goal state ##\n");

		for( String pred : posFinalTuples.keySet() ) {
			System.err.println(posFinalTuples.get(pred).toString().replaceAll("\\[", "{").replaceAll("\\]", "}") + " \\subseteq S[" + plansize + "," + pred + "]");
		}

		for( String pred : negFinalTuples.keySet() ) {
			System.err.println(negFinalTuples.get(pred).toString().replaceAll("\\[", "{").replaceAll("\\]", "}") + " disjoint with S[" + plansize + "," + pred + "]");
		}


		System.exit(0);
	}


	private String printTupleListList(List<List<PseudoTuple>> subsets) {
		List<String> parts = new ArrayList<String>();

		for( List<PseudoTuple> tupleList : subsets ) {
			parts.add(tupleList.toString().replaceAll("\\[", "{").replaceAll("\\]", "}"));
		}

		return "<" + StringTools.join(parts, ",") + ">";
	}


	private PseudoTuple atomToPseudoTuple(Compound atom, Map<String, String> parameterMap, Map<String,Integer> constantsMap) {
		PseudoTuple ret = new PseudoTuple();

		for( Term term : atom.getSubterms() ) {
			if (term instanceof Variable) {
				Variable var = (Variable) term;
				ret.add(parameterMap.get(var.getName()));
			} else if( term instanceof Constant ) {
				ret.add(constantsMap.get(((Constant) term).getName()).toString());
			} else {
				// TODO: do something for constants
				throw new RuntimeException("constants in tuples are not yet implemented");
			}
		}

		return ret;
	}


	private Tuple atomToTuple(Compound atom, Map<String, IntVar> parameterMap) {
		Tuple ret = new Tuple();

		for( Term term : atom.getSubterms() ) {
			if (term instanceof Variable) {
				Variable var = (Variable) term;
				ret.add(new Expr(parameterMap.get(var.getName())));
			} else {
				// TODO: do something for constants
				throw new RuntimeException("constants in tuples are not yet implemented");
			}
		}

		return ret;
	}

	public Planner(boolean share, Planner planner) {
		super(share, planner);

		selectedAction = new VarArray<IntVar>(this, share, planner.selectedAction);
		actionParameters = new VarArray<IntVar>(this, share, planner.actionParameters);

		predicate = new ArrayList<Map<String,TupleSetVariable>>(planner.predicate.size());
		for( Map<String,TupleSetVariable> map : planner.predicate ) {
			Map<String,TupleSetVariable> copy = new HashMap<String,TupleSetVariable>();

			for( String pred : map.keySet() ) {
				copy.put(pred, map.get(pred).copy(this, share));
			}

			predicate.add(copy);
		}
	}


	@Override
    public Space copy(boolean share) {
		return new Planner(share, this);
	}

	@Override
    public String toString() {
		// TODO: do something useful here
		return "hallo";
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Domain domain = new Domain();
		Problem problem = new Problem();

		setOptions();


		PddlParser.parse(domainname, domain, problemname, problem);
		Planner planner = new Planner(domain, problem, PLANSIZE);

		if( !usingGui ) {
			int a_d = (int)Gecode.getDefault_a_d();
			int c_d = (int)Gecode.getDefault_c_d();

			long startTime = System.nanoTime();

			DFSSearch search = new DFSSearch(planner,a_d,c_d, null);
			Space sol = (Space)search.next();

			long endTime = System.nanoTime();

			System.out.println("Search finished!");
			System.out.println("Runtime: " + (endTime-startTime)/1000000 + "ms");
			System.out.println("Solution: " + sol);

			Statistics stat = search.statistics();
			if (stat != null) {
				System.out.println("  \tpropagations: " + stat.getPropagate() +
						"\n\tfailures:     " + stat.getFail() +
						"\n\tclones:       " + stat.getClone() +
						"\n\tcommits:      " + stat.getCommit() +
						"\n\tpeak memory:  " +
						((stat.getMemory()+1023)/1024) + "KB");
			}




		} else {
			Gist explorer = new Gist(planner); // 2nd arg: use optimizing search?
			explorer.exploreOne();
		}
	}

	private static void setOptions() {
		PlannerOptions options = new PlannerOptions();
		Properties props = new Properties();
		File propsfile = new File(System.getProperty("user.home"), ".crisp-planner.xml");

		// load previously saved option values
		try {
			props.loadFromXML(new FileInputStream(propsfile));
		} catch (Exception e1) {
			// NOP
		}

		try {
			// register all options and give them the stored or default values
			for( Field field : Planner.class.getDeclaredFields() ) {
				if( field.isAnnotationPresent(Option.class) ) {
					String val = field.getAnnotation(Option.class).defaultValue();

					if( props.containsKey(field.getName())) {
						val = props.getProperty(field.getName());
					}

					options.addOption(field.getName(), field.getAnnotation(Option.class).label(),
							field.getType(), val);
				}
			}

			if( props.containsKey("problemname") ) {
				options.setProblemName(props.getProperty("problemname"));
			}

			if( props.containsKey("domainname")) {
				options.setDomainName(props.getProperty("domainname"));
			}

			// compute options panel, and show dialog
			options.computeOptionsPanel();
			Map<String,Object> values = options.showDialog();

			if( values == null ) {
				System.exit(0);
			}

			// read values from the returned Map and set fields
			for( Field field : Planner.class.getDeclaredFields() ) {
				if( values.containsKey(field.getName())) {
					// this includes domainname and problemname!
					Object val = values.get(field.getName());

					field.set(Planner.class, val);
					props.setProperty(field.getName(), val.toString());
				}
			}

			// store current values in properties file for next time
			props.storeToXML(new FileOutputStream(propsfile), "Saved options for the CRISP planner");
		} catch(Exception e) {
			e.printStackTrace(System.err);
		}
	}


}
