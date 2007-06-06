/*
 * @(#)Planner.java created 01.10.2006
 * 
 * Copyright (c) 2006 Alexander Koller
 *  
 */


/*
 * TODO:
 * - deal with do-undo cycles, which occur frequently when the initial plansize is
 *   higher than the shortest plan
 * - keep track of mutex propositions: clear(a) => not on(b,a)
 * - can we somehow avoid multiplying out all instances and go back to
 * constraints on variable binding?
 * - estimate max/min cardinality of states based on max. number of pos/neg effects
 * of any action 
 *
 * - implement GP-interaction constraints between actions rather than serialisation
 *   - can be done but is horribly slow (at least with nested selection constraints)
 * - convert constraints from boolean to selection - OK
 * - deal with empty actions more smartly (e.g.: action set is empty iff previous
 *   state is a goal state)  - OK
 */

package crisp.planner;


import static org.gecode.Gecode.bool_and;
import static org.gecode.Gecode.bool_eq;
import static org.gecode.Gecode.bool_imp;
import static org.gecode.Gecode.bool_or;
import static org.gecode.Gecode.branch;
import static org.gecode.Gecode.dom;
import static org.gecode.Gecode.rel;
import static org.gecode.Gecode.selectUnion;
import static org.gecode.GecodeEnumConstants.SETBVAL_MIN;
import static org.gecode.GecodeEnumConstants.SETBVAR_MIN_UNKNOWN_ELEM;
import static org.gecode.GecodeEnumConstants.SOT_INTER;
import static org.gecode.GecodeEnumConstants.SOT_MINUS;
import static org.gecode.GecodeEnumConstants.SOT_UNION;
import static org.gecode.GecodeEnumConstants.SRT_DISJ;
import static org.gecode.GecodeEnumConstants.SRT_EQ;
import static org.gecode.GecodeEnumConstants.SRT_SUB;
import static org.gecode.GecodeEnumConstants.SRT_SUP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import mygecode.tools.GecodeUtil;

import org.gecode.BoolVar;
import org.gecode.DFSSearch;
import org.gecode.Gecode;
import org.gecode.IntSet;
import org.gecode.SetVar;
import org.gecode.Space;
import org.gecode.Statistics;
import org.gecode.VarArray;
import org.gecode.gist.Gist;

import crisp.pddl.PddlParser;
import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.goal.Literal;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;



public class Planner extends Space {
	@Option(label="Plan size")
	private static int PLANSIZE;

	@Option(label="Use Gist", defaultValue="true")
	private static boolean usingGui;

	@Option(label="Use mutex constraints", defaultValue="false")
	private static boolean USE_MUTEX;
	
	private static File domainname;
	private static File problemname;
	
	
    
    
    private VarArray<SetVar> trueAtoms;
    private VarArray<SetVar> selectedActions;
    
    private List<Action> actionInstances;
    private List<Term> predicateInstances;

    private List<List<VarArray<BoolVar>>> mutexActions;
    private List<List<VarArray<BoolVar>>> mutexPropositions;

    private int plansize;
    
    // TODO this could be done faster
    private int findPredicateInstance(Term t) {
        return predicateInstances.indexOf(t);
    }
    
    private IntSet arrayToIntSet(Collection<Integer> ints) {
        int[] array = new int[ints.size()];
        int i = 0;
        
        for( Integer val : ints ) {
            array[i++] = val;
        }
        
        return new IntSet(array);
    }
    
    public Planner(Domain domain, Problem problem, int plansize) {
        super();
        
        
        
        setName("crisp-planner");
        this.plansize = plansize;
        
        VarArray<SetVar> actionPosPreconditions = new VarArray<SetVar>();
        VarArray<SetVar> actionNegPreconditions = new VarArray<SetVar>();
        VarArray<SetVar> actionPosEffects = new VarArray<SetVar>();
        VarArray<SetVar> actionNegEffects = new VarArray<SetVar>();
        
        Map<Integer,List<Integer>> positiveActionPreconditions = 
        	new HashMap<Integer,List<Integer>>();
        Map<Integer,List<Integer>> positiveActionEffects = 
        	new HashMap<Integer,List<Integer>>();
        
        Set<Predicate> staticPredicates = domain.getStaticPredicates();
        //System.err.println("Static predicates: " + staticPredicates);
        
        // collect all action and predicate instances (i.e. lots and lots)
        actionInstances = new ArrayList<Action>();
        predicateInstances = new ArrayList<Term>();
        
        for( Predicate p : domain.getPredicates() ) {
        	// add only dynamic predicates
        	if( !staticPredicates.contains(p)) {
        		Iterator<Substitution> it = domain.getSubstitutionsFor(p);

        		while( it.hasNext() ) {
        			Substitution subst = it.next();
        			predicateInstances.add(subst.apply(p.toTerm()));
        		}
        	}
        }
        
        int index = 0;
        for( Action a : domain.getActions() ) {
            Iterator<Substitution> it = domain.getSubstitutionsFor(a);
            
            while( it.hasNext() ) {
                Substitution subst = it.next();
                Action instantiated = a.instantiate(subst);
                
                /*
                System.err.println("\nAction instance " + index + ": " + instantiated);
                System.err.println("  dynamic goals = " + instantiated.getDynamicGoalList(problem));
                System.err.println("  static goals satisfied: " + instantiated.isStaticGoalsSatisfied(problem));
                System.err.println("  effects = " + instantiated.getEffect().getEffects(problem));
                */
                
                
                // only consider actions whose static goals are satisfied by the problem
                if( instantiated.isStaticGoalsSatisfied(problem)) {
                	actionInstances.add(instantiated);

                	// collect preconditions
                	List<Literal> preconditions = instantiated.getDynamicGoalList(problem);
                	List<Integer> posPrecondIndices = new ArrayList<Integer>();
                	List<Integer> negPrecondIndices = new ArrayList<Integer>();
                	for( Literal precond : preconditions ) {
                		if( precond.getPolarity() ) {
                			posPrecondIndices.add(findPredicateInstance(precond.getAtom()));
                		} else {
                			negPrecondIndices.add(findPredicateInstance(precond.getAtom()));
                		}
                	}

                	IntSet posPrecondSet = arrayToIntSet(posPrecondIndices);
                	IntSet negPrecondSet = arrayToIntSet(negPrecondIndices);

                	actionPosPreconditions.add(new SetVar(this, posPrecondSet, posPrecondSet));
                	actionNegPreconditions.add(new SetVar(this, negPrecondSet, negPrecondSet));

                	positiveActionPreconditions.put(index, posPrecondIndices);




                	// collect effects
                	List<crisp.planningproblem.effect.Literal> effects = instantiated.getEffect().getEffects(problem);
                	List<Integer> posEffectIndices = new ArrayList<Integer>();
                	List<Integer> negEffectIndices = new ArrayList<Integer>();
                	for( crisp.planningproblem.effect.Literal effect : effects ) {
                		if( effect.getPolarity() ) {
                			posEffectIndices.add(findPredicateInstance(effect.getAtom()));
                		} else {
                			negEffectIndices.add(findPredicateInstance(effect.getAtom()));
                		}
                	}

                	IntSet posEffectSet = arrayToIntSet(posEffectIndices);
                	IntSet negEffectSet = arrayToIntSet(negEffectIndices);

                	actionPosEffects.add(new SetVar(this, posEffectSet, posEffectSet));
                	actionNegEffects.add(new SetVar(this, negEffectSet, negEffectSet));

                	positiveActionEffects.put(index, posEffectIndices);

                	index++;
                }
            }
        }
        
        /*
        System.err.println("Predicate instances:");
        for( int i = 0; i < predicateInstances.size(); i++ ) {
        	System.err.printf("  %2d %s\n", i, predicateInstances.get(i));
        }
        
        System.err.println();
        */
        
        if( usingGui ) {

        	System.err.println("Action instances: ");
        	for( int i = 0; i < actionInstances.size(); i++ ) {
        		System.err.printf("  %2d %s\n", i, actionInstances.get(i).toString());
        		System.err.println("       goals: " + actionInstances.get(i).getDynamicGoalList(problem));
        		System.err.println("       effects: " + actionInstances.get(i).getEffect().getEffects(problem));

        	}
        }
        
        
        // set up set variables
        IntSet emptySet = new IntSet(new int[] { });
        IntSet allActions = new IntSet(0, actionInstances.size()-1);
        IntSet allAtoms = new IntSet(0, predicateInstances.size()-1);
        
        trueAtoms = new VarArray<SetVar>(this, plansize+1, SetVar.class, emptySet, allAtoms);
        selectedActions = new VarArray<SetVar>(this, plansize, SetVar.class, emptySet, allActions);

        mutexActions = makeMutexVars(actionInstances.size(), plansize);
        mutexPropositions = makeMutexVars(predicateInstances.size(), plansize+1);

        /*
        // avoid using the same state twice (this is generally a non-optimal plan)
        // This seems like a good strategy for cases where the max plansize is much
        // longer than the size of the shortest plans, but makes propagation horribly slow.
        for( int i = 0; i < plansize; i++ ) {
        	BoolVar isGoalState = reifiedIsGoal(i, problem);
        	
        	for( int j = 0; j < i; j++ ) {
        		BoolVar isUnequal = new BoolVar(this);
        		
        		rel(this, trueAtoms.get(i), SRT_NQ, trueAtoms.get(j), isUnequal);
        		bool_or(this, isGoalState, isUnequal, true);
        	}
        }
        */

        /*
        // just avoid do-undo cycles
        for( int i = 0; i < plansize-2; i++ ) {
        	BoolVar isGoalState = reifiedIsGoal(i+2, problem);
        	BoolVar isUnequal = new BoolVar(this);
        		
        	rel(this, trueAtoms.get(i), SRT_NQ, trueAtoms.get(i+2), isUnequal);
        	bool_or(this, isGoalState, isUnequal, true);
        }
        */

        /*
         // do-undo cycles with set constraints
        for( int i = 0; i < plansize-1; i++ ) {
        	BoolVar isGoalState = reifiedIsGoal(i+1, problem);
        	BoolVar isUnequal = new BoolVar(this);
        	SetVar pos = new SetVar(this);
        	SetVar neg = new SetVar(this);
        	
        	selectUnion(this, actionPosEffects, selectedActions.get(i), pos);
        	selectUnion(this, actionNegEffects, selectedActions.get(i+1), neg);
        	rel(this, pos, SRT_NQ, neg, isUnequal);
        	bool_or(this, isGoalState, isUnequal, true);
        	
        }
        */
                
        
        // initial state
        Set<Integer> initIndicesAsSet = new HashSet<Integer>();
        for( Term t : problem.getInitialState() ) {
        	if( predicateInstances.contains(t)) {
        		initIndicesAsSet.add(predicateInstances.indexOf(t));
        	}
        }
        
        int[] initIndices = new int[initIndicesAsSet.size()];
        int iii = 0;
        for( Integer x : initIndicesAsSet ) {
        	initIndices[iii++] = x.intValue();
        }
        
        IntSet initiallyTrueAtoms = new IntSet(initIndices);
        dom(this, trueAtoms.get(0), SRT_EQ, initiallyTrueAtoms);
        
        if( USE_MUTEX ) {
        	BoolVar TRUE = new BoolVar(this, true);
        	BoolVar FALSE = new BoolVar(this, false);

        	for( int j = 0; j < predicateInstances.size(); j++ ) {
        		for( int k = 0; k < predicateInstances.size(); k++ ) {
        			if( !initIndicesAsSet.contains(j)  || !initIndicesAsSet.contains(k) ) {
        				bool_eq(this, mutexPropositions.get(0).get(j).get(k), TRUE); 
        			} else {
        				bool_eq(this, mutexPropositions.get(0).get(j).get(k), FALSE);
        			}
        		}
        	}
        }
        
        // goal state
        postIsGoal(plansize, problem);
        
        
        // actions must be applicable
        for( int i = 0; i < plansize; i++ ) {
            SetVar selectedPosPreconditions = new SetVar(this);
            SetVar selectedNegPreconditions = new SetVar(this);
            
            selectUnion(this, actionPosPreconditions, selectedActions.get(i), selectedPosPreconditions);
            rel(this, selectedPosPreconditions, SRT_SUB, trueAtoms.get(i));
            
            selectUnion(this, actionNegPreconditions, selectedActions.get(i), selectedNegPreconditions);
            rel(this, selectedNegPreconditions, SRT_DISJ, trueAtoms.get(i));
        }
        
        
        for( int i = 0; i < plansize; i++ ) {
            SetVar allPositiveEffects = new SetVar(this);
            SetVar allNegativeEffects = new SetVar(this);
            SetVar remaining = new SetVar(this);
            
            // successor state constraint
            selectUnion(this, actionPosEffects, selectedActions.get(i), allPositiveEffects);
            selectUnion(this, actionNegEffects, selectedActions.get(i), allNegativeEffects);
            rel(this, trueAtoms.get(i), SOT_MINUS, allNegativeEffects, SRT_EQ, remaining);
            
            rel(this, allPositiveEffects, SOT_UNION, remaining, SRT_EQ, trueAtoms.get(i+1));
            
            // redundant
            rel(this, allNegativeEffects, SRT_DISJ, trueAtoms.get(i+1));
            
            // actions must be compatible
            rel(this, allPositiveEffects, SRT_DISJ, allNegativeEffects);
            
            IntSet allPropositionIndices = new IntSet(0, predicateInstances.size()-1);
            for( int j = 0; j < actionInstances.size(); j++ ) {
            	// NB: computation of conflict sets doesn't depend on i and could
            	// be done just once.
            	BoolVar isSelected = new BoolVar(this), noConflicts = new BoolVar(this);
            	VarArray<SetVar> conflicts = 
            		new VarArray<SetVar>(this, actionInstances.size(), SetVar.class, emptySet, allPropositionIndices);
            	SetVar allConflicts = new SetVar(this, emptySet, allPropositionIndices);
            	
            	for( int k = 0; k < actionInstances.size(); k++ ) {
            		if( j == k ) {
            			dom(this, conflicts.get(k), SRT_EQ, emptySet);
            		} else {
            			rel(this, actionNegEffects.get(j), SOT_INTER, actionPosPreconditions.get(k), SRT_EQ, conflicts.get(k));
            		}
            	}
            	
            	selectUnion(this, conflicts, selectedActions.get(i), allConflicts);
            	dom(this, selectedActions.get(i), SRT_SUP, j, isSelected);
            	dom(this, allConflicts, SRT_EQ, emptySet, noConflicts);
            	bool_imp(this, isSelected, noConflicts, true);
            }
         
            
            // post mutex constraints
            if( USE_MUTEX) {
                postMutexActionConstraints(i, positiveActionPreconditions);
                postMutexPropositionContraints(i, positiveActionEffects, positiveActionPreconditions);
            }
        }
        
        
        // serialisation and non-null steps (at most one action per step)
        for( int i = 0; i < plansize; i++ ) {
            // cardinality(this, selectedActions.get(i), 0, 1);
            ;
        }
        
        // empty action sets must occur at the end
        for( int i = 1; i < plansize; i++ ) {
            BoolVar thisIsEmpty = new BoolVar(this);
            BoolVar prevIsEmpty = new BoolVar(this);
            
            dom(this, selectedActions.get(i), SRT_EQ, emptySet, thisIsEmpty);
            dom(this, selectedActions.get(i-1), SRT_EQ, emptySet, prevIsEmpty);
            bool_imp(this, prevIsEmpty, thisIsEmpty, true);
            
            // this action set should be empty iff we were in a goal state already
            bool_eq(this, prevIsEmpty, reifiedIsGoal(i-1, problem));
        }
        
        /*
        // compute mutex lists first -- makes no runtime difference
        for( int i = 0; i < plansize; i++ ) {
        	for( int j = 0; j < mutexPropositions.get(i).size(); j++ ) {
        		branch(this, mutexPropositions.get(i).get(j), BVAR_NONE, BVAL_MIN);
        	}
        }
        */
        
        System.err.println("setup finished");
        
        branch(this, selectedActions, SETBVAR_MIN_UNKNOWN_ELEM, SETBVAL_MIN);
        branch(this, trueAtoms, SETBVAR_MIN_UNKNOWN_ELEM, SETBVAL_MIN);
    }
    
    private void postMutexPropositionContraints(int planstep, Map<Integer, List<Integer>> positiveActionEffects, Map<Integer, List<Integer>> positiveActionPreconditions) {
        for( int j = 0; j < predicateInstances.size(); j++ ) {
        	for( int k = 0; k < predicateInstances.size(); k++ ) {
        		BoolVar isMutex = new BoolVar(this, true);

        		// j,k not mutex if there is are non-mutex actions that have
        		// them as effects
        		for( int aj = 0; aj < actionInstances.size(); aj++ ) {
        			if( positiveActionEffects.get(aj).contains(j)) {
        				for( int ak = 0; ak < actionInstances.size(); ak++ ) {
        					if( positiveActionEffects.get(ak).contains(k)) {
        						BoolVar newIsMutex = new BoolVar(this);
        						
        						bool_and(this, isMutex, mutexActions.get(planstep).get(aj).get(ak), newIsMutex);
        						isMutex = newIsMutex;
        					}
        				}
        			}
        		}
        		
        		// or if there is an action that achieves j and that is
        		// not mutex with no-op(k)
        		for( int aj = 0; aj < actionInstances.size(); aj++ ) {
        			if( positiveActionEffects.get(aj).contains(j)) {
        				BoolVar hasMutexPrecondition = new BoolVar(this, false);
        				BoolVar newIsMutex = new BoolVar(this);
        				
        				for( int p : positiveActionPreconditions.get(aj)) {
        					BoolVar newHasMutexPrecond = new BoolVar(this);
    						
    						bool_or(this, hasMutexPrecondition, mutexPropositions.get(planstep).get(p).get(k), newHasMutexPrecond);
    						hasMutexPrecondition = newHasMutexPrecond;
        				}
        				
        				bool_and(this, isMutex, hasMutexPrecondition, newIsMutex);
        				isMutex = newIsMutex;
        			}
        		}
        		
        		// or if there is an action that achieves k and that is
        		// not mutex with no-op(j)
        		for( int ak = 0; ak < actionInstances.size(); ak++ ) {
        			if( positiveActionEffects.get(ak).contains(k)) {
        				BoolVar hasMutexPrecondition = new BoolVar(this, false);
        				BoolVar newIsMutex = new BoolVar(this);
        				
        				for( int p : positiveActionPreconditions.get(ak)) {
        					BoolVar newHasMutexPrecond = new BoolVar(this);
    						
    						bool_or(this, hasMutexPrecondition, mutexPropositions.get(planstep).get(j).get(p), newHasMutexPrecond);
    						hasMutexPrecondition = newHasMutexPrecond;
        				}
        				
        				bool_and(this, isMutex, hasMutexPrecondition, newIsMutex);
        				isMutex = newIsMutex;
        			}
        		}
        		
        		// or if j and k weren't mutex before (i.e. no-op(j) and
        		// no-op(k) aren't mutex)
        		BoolVar newIsMutex = new BoolVar(this);
        		bool_and(this, isMutex, mutexPropositions.get(planstep).get(j).get(k), newIsMutex);
        		isMutex = newIsMutex;

        		// that's proposition-mutex(i,j,k)
        		bool_eq(this, isMutex, mutexPropositions.get(planstep+1).get(j).get(k));
        		notBothIn(j, k, trueAtoms.get(planstep+1), isMutex);
        	}
        }
	}

	private void postMutexActionConstraints(int planstep, Map<Integer, List<Integer>> positiveActionPreconditions) {
    	for( int j = 0; j < actionInstances.size(); j++ ) {
    		for( int k = 0; k < actionInstances.size(); k++ ) {
    			BoolVar isMutex = new BoolVar(this, false);

    			for( int precond1 : positiveActionPreconditions.get(j)) {
    				for( int precond2 : positiveActionPreconditions.get(k)) {
    					BoolVar newIsMutex = new BoolVar(this);

    					bool_or(this, isMutex, mutexPropositions.get(planstep).get(precond1).get(precond2), newIsMutex);
    					isMutex = newIsMutex;
    				}
    			}

    			bool_eq(this, mutexActions.get(planstep).get(j).get(k), isMutex);
    			notBothIn(j, k, selectedActions.get(planstep), isMutex);
    		}
    	}
    }

	private List<List<VarArray<BoolVar>>> makeMutexVars(int dim, int plansize) {
    	List<List<VarArray<BoolVar>>> ret = new ArrayList<List<VarArray<BoolVar>>>(plansize);
    	
    	for( int i = 0; i < plansize; i++ ) {
    		List<VarArray<BoolVar>> here = new ArrayList<VarArray<BoolVar>>(dim);
    		ret.add(here);
    		
    		for( int j = 0; j < dim; j++ ) {
    			VarArray<BoolVar> inner = new VarArray<BoolVar>(this, dim, BoolVar.class);
    			here.add(inner);
    		}
    	}
    	
    	return ret;
	}

    private List<List<VarArray<BoolVar>>> copyMutexVars(boolean share, List<List<VarArray<BoolVar>>> name) {
    	int size = name.size();
    	List<List<VarArray<BoolVar>>> ret = new ArrayList<List<VarArray<BoolVar>>>(size);
    	int dim = name.isEmpty() ? 0 : name.get(0).size();
    	
    	for( int i = 0; i < size; i++ ) {
    		List<VarArray<BoolVar>> here = new ArrayList<VarArray<BoolVar>>(dim);
    		ret.add(here);
    		
    		for( int j = 0; j < dim; j++ ) {
    			VarArray<BoolVar> inner = new VarArray<BoolVar>(this, share, name.get(i).get(j));
    			here.add(inner);
    		}
    	}
    	
    	return ret;
	}

    
	/**
     * Posts the constraint that not both x and y may be in A
     * at the same time.
     * 
     */
    private void notBothIn(int x, int y, SetVar A, BoolVar b) {
        BoolVar xNotInA = new BoolVar(this);
        BoolVar yNotInA = new BoolVar(this);
        BoolVar notBothIn = new BoolVar(this);
        
        dom(this, A, SRT_DISJ, x, xNotInA);
        dom(this, A, SRT_DISJ, y, yNotInA);
        bool_or(this, xNotInA, yNotInA, notBothIn);
        
        bool_imp(this, b, notBothIn, true);
    }
    
    
    private void postIsGoal(int step, Problem problem) {
    	List<Literal> goalTerms = problem.getGoal().getGoalList(problem);
        IntSet positiveGoalAtoms = getPositiveGoalIndices(goalTerms, predicateInstances);
        IntSet negativeGoalAtoms = getNegativeGoalIndices(goalTerms, predicateInstances);
        
        dom(this, trueAtoms.get(step), SRT_SUP, positiveGoalAtoms);
        dom(this, trueAtoms.get(step), SRT_DISJ, negativeGoalAtoms);
    }
    
    private BoolVar reifiedIsGoal(int step, Problem problem) {
    	BoolVar positiveOk = new BoolVar(this);
    	BoolVar negativeOk = new BoolVar(this);
    	BoolVar ret = new BoolVar(this);
    	
    	List<Literal> goalTerms = problem.getGoal().getGoalList(problem);
        IntSet positiveGoalAtoms = getPositiveGoalIndices(goalTerms, predicateInstances);
        IntSet negativeGoalAtoms = getNegativeGoalIndices(goalTerms, predicateInstances);
        
        dom(this, trueAtoms.get(step), SRT_SUP, positiveGoalAtoms, positiveOk);
        dom(this, trueAtoms.get(step), SRT_DISJ, negativeGoalAtoms, negativeOk);
        
        bool_and(this, positiveOk, negativeOk, ret);
        
        return ret;
    }
    
    private IntSet getPositiveGoalIndices(List<Literal> goals, List<Term> predicateInstances) {
        List<Integer> indices = new ArrayList<Integer>();
        
        for( Literal goal : goals ) {
            if( goal.getPolarity() && predicateInstances.contains(goal.getAtom())) {
          //      System.err.println("  " + goal.getAtom());
                indices.add(predicateInstances.indexOf(goal.getAtom()));
            }
        }
        
        int[] indicesAsArray = new int[indices.size()];
        for( int i = 0; i < indices.size(); i++ ) {
            indicesAsArray[i] = indices.get(i);
        }
        
        return new IntSet(indicesAsArray);
    }


    private IntSet getNegativeGoalIndices(List<Literal> goals, List<Term> predicateInstances) {
        List<Integer> indices = new ArrayList<Integer>();
        
        for( Literal goal : goals ) {
            if( ! goal.getPolarity() && predicateInstances.contains(goal.getAtom())) {
        //        System.err.println("  not " + goal.getAtom());
                indices.add(predicateInstances.indexOf(goal.getAtom()));
            }
        }
        
        int[] indicesAsArray = new int[indices.size()];
        for( int i = 0; i < indices.size(); i++ ) {
            indicesAsArray[i] = indices.get(i);
        }
        
        return new IntSet(indicesAsArray);
    }
    
    
    public Planner(boolean share, Planner planner) {
        super(share, planner);

        plansize = planner.plansize;

        selectedActions = new VarArray<SetVar>(this, share, planner.selectedActions);
        trueAtoms = new VarArray<SetVar>(this, share, planner.trueAtoms);
        
        mutexActions = copyMutexVars(share, planner.mutexActions);
        mutexPropositions = copyMutexVars(share, planner.mutexPropositions);
        
        actionInstances = planner.actionInstances;
        predicateInstances = planner.predicateInstances;
        
    }


	public Space copy(boolean share) {
        return new Planner(share, this);
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        
        
        for( int i = 0; i < plansize; i++ ) {
            buf.append("world state " + i);
            
            if( trueAtoms.get(i).cardMax() != trueAtoms.get(i).cardMin() ) {
                buf.append(" (" + trueAtoms.get(i).cardMin() + "-" + trueAtoms.get(i).cardMax() + ")");
            } else {
                buf.append(" (" + trueAtoms.get(i).cardMax() + ")");
            }
            
            buf.append(": ");
            
            for( Integer j : GecodeUtil.getSafeMembers(trueAtoms.get(i), predicateInstances.size())) {
            	if( ! predicateInstances.get(j).toString().startsWith("**equals**")) {
            		buf.append(predicateInstances.get(j) + " ");
            	}
            }
            
            for( Integer j : GecodeUtil.getPossibleMembers(trueAtoms.get(i), predicateInstances.size())) {
                buf.append("[" + predicateInstances.get(j) + "] ");
            }
            
            buf.append("\n");
            
            
            
            buf.append("step " + i);

            if( selectedActions.get(i).cardMax() != selectedActions.get(i).cardMin() ) {
                buf.append(" (" + selectedActions.get(i).cardMin() + "-" + selectedActions.get(i).cardMax() + ")");
            } else {
                buf.append(" (" + selectedActions.get(i).cardMax() + ")");
            }

            buf.append(": ");
            
            for( Integer j : GecodeUtil.getSafeMembers(selectedActions.get(i), actionInstances.size())) {
                buf.append(actionInstances.get(j) + " ");
            }
            
            for( Integer j : GecodeUtil.getPossibleMembers(selectedActions.get(i), actionInstances.size())) {
                buf.append("[" + actionInstances.get(j) + "] ");
            }
            
            buf.append("\n");
        }

        buf.append("world state " + plansize + ": ");
        
        for( Integer j : GecodeUtil.getSafeMembers(trueAtoms.get(plansize), predicateInstances.size())) {
        	if( ! predicateInstances.get(j).toString().startsWith("**equals**")) {
        		buf.append(predicateInstances.get(j) + " ");
        	}
        }
        
        for( Integer j : GecodeUtil.getPossibleMembers(trueAtoms.get(plansize), predicateInstances.size())) {
            buf.append("[" + predicateInstances.get(j) + "] ");
        }
        
        buf.append("\n");
        
        if( USE_MUTEX ) {
            for( int i = 0; i < plansize; i++ ) {
                buf.append("Non-mutex propositions at step " + i + ":\n");
                for( int j = 0; j < predicateInstances.size(); j++ ) {
                    buf.append(predicateInstances.get(j) + " with: ");
                    for( int k = 0; k < predicateInstances.size(); k++ ) {
                        //buf.append(GecodeUtil.getBooleanStatus(mutexPropositions.get(i).get(j).get(k)));
                        if( GecodeUtil.getBooleanStatus(mutexPropositions.get(i).get(j).get(k)).equals("0") ) {
                            buf.append(predicateInstances.get(k) + " ");
                        }
                    }
                    
                    for( int k = 0; k < predicateInstances.size(); k++ ) {
                        //buf.append(GecodeUtil.getBooleanStatus(mutexPropositions.get(i).get(j).get(k)));
                        if( GecodeUtil.getBooleanStatus(mutexPropositions.get(i).get(j).get(k)).equals("?") ) {
                            buf.append("[" + predicateInstances.get(k) + "] ");
                        }
                    }
                    
                    buf.append("\n");
                }
                
                buf.append("Non-mutex actions at step " + i + ":\n");
                for( int j = 0; j < actionInstances.size(); j++ ) {
                    buf.append(actionInstances.get(j) + " with: ");
                    for( int k = 0; k < actionInstances.size(); k++ ) {
                        //buf.append(GecodeUtil.getBooleanStatus(mutexActions.get(i).get(j).get(k)));
                        if( GecodeUtil.getBooleanStatus(mutexActions.get(i).get(j).get(k)).equals("0")) {
                            buf.append(actionInstances.get(k) + " ");
                        }
                    }
                    for( int k = 0; k < actionInstances.size(); k++ ) {
                        //buf.append(GecodeUtil.getBooleanStatus(mutexActions.get(i).get(j).get(k)));
                        if( GecodeUtil.getBooleanStatus(mutexActions.get(i).get(j).get(k)).equals("?")) {
                            buf.append("[" + actionInstances.get(k) + "] ");
                        }
                    }
                    buf.append("\n");
                }
            }
            
            buf.append("Non-mutex propositions at step " + plansize + ":\n");
            for( int j = 0; j < predicateInstances.size(); j++ ) {
                for( int k = 0; k < predicateInstances.size(); k++ ) {
                    buf.append(GecodeUtil.getBooleanStatus(mutexPropositions.get(plansize).get(j).get(k)));
                }
                buf.append("\n");
            }
        }

        
        return buf.toString();
    }
    
     
    

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Domain domain = new Domain();
        Problem problem = new Problem();
        
        setOptions();
        
        
      //  int plansize = PLANSIZE;
        
      //  usingGui = ! ((args.length > 0) && "--nogui".equals(args[0]));
    	
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






/*
 * old implementations, with boolean rather than selection constraints
 */


/* ** SUCCESSOR STATE CONSTRAINTS - ALTERNATIVE WITHOUT SELECTION CONSTRAINTS **
for( int predId = 0; predId < predicateInstances.size(); predId++ ) {
    Term p = predicateInstances.get(predId);
    crisp.planningproblem.effect.Literal pos = new crisp.planningproblem.effect.Literal(p, true);
    crisp.planningproblem.effect.Literal neg = new crisp.planningproblem.effect.Literal(p, false);
    
    List<Integer> creators = new ArrayList<Integer>();
    List<Integer> deleters = new ArrayList<Integer>();
    
    for( int actionId = 0; actionId < actionInstances.size(); actionId++ ) {
        Action a = actionInstances.get(actionId);
        List<crisp.planningproblem.effect.Literal> effects = a.getEffect().getEffects();
        
        //System.out.println("Can I find " + pos + " or " + neg + " in " + effects + "?");
        
        if( effects.contains(pos) ) {
            creators.add(actionId);
        } else if( effects.contains(neg) ) {
            deleters.add(actionId);
        }
    }
    
    for( int i = 0; i < plansize; i++ ) {
        VarArray<BoolVar> creatorSelected = new VarArray<BoolVar>(this, creators.size(), BoolVar.class);
        VarArray<BoolVar> deleterNotSelected = new VarArray<BoolVar>(this, deleters.size(), BoolVar.class);
        BoolVar aCreatorSelected = new BoolVar(this);
        BoolVar noDeletorSelected = new BoolVar(this);
        
        for( int j = 0; j < creators.size(); j++ ) {
            dom(this, selectedActions.get(i), SRT_SUP, creators.get(j).intValue(), creatorSelected.get(j));
        }
        bool_or(this, creatorSelected, aCreatorSelected); 
        
        for( int j = 0; j < deleters.size(); j++ ) {
            BoolVar thisDeleterSelected = new BoolVar(this);
            dom(this, selectedActions.get(i), SRT_SUP, deleters.get(j).intValue(), thisDeleterSelected);
            bool_not(this, thisDeleterSelected, deleterNotSelected.get(j));
        }
        bool_and(this, deleterNotSelected, noDeletorSelected);
        
        *
         * TODO: Right now, we make two different boolean variables
         * for P(s,i) -- one in the iteration for i and one for i-1.
         * Use only one variable instead!
         *
     
        
        BoolVar pIsCopied = new BoolVar(this);
        BoolVar pWasTrue = new BoolVar(this);
        dom(this, trueAtoms.get(i), SRT_SUP, predId, pWasTrue);
        bool_and(this, pWasTrue, noDeletorSelected, pIsCopied);
        
        BoolVar pShouldBeTrue = new BoolVar(this);
        BoolVar pWillBeTrue = new BoolVar(this);
        bool_or(this, aCreatorSelected, pIsCopied, pShouldBeTrue);
        dom(this, trueAtoms.get(i+1), SRT_SUP, predId, pWillBeTrue);
        bool_eq(this, pShouldBeTrue, pWillBeTrue);
    }
}*/




/*  ** ACTION APPLICABILITY CONSTRAINTS - ALTERNATIVE WITHOUT SELECTION CONSTRAINTS **
for( int actionId = 0; actionId < actionInstances.size(); actionId++ ) {
    Action a = actionInstances.get(actionId);
    IntSet positivePreconditions = getPositiveGoalIndices(a.getPrecondition().getGoalList(), predicateInstances);
    IntSet negativePreconditions = getNegativeGoalIndices(a.getPrecondition().getGoalList(), predicateInstances);
    
    for( int i = 0; i < plansize; i++ ) {
        BoolVar thisActionSelected = new BoolVar(this);
        BoolVar positiveSatisfied = new BoolVar(this);
        BoolVar negativeSatisfied = new BoolVar(this);
        BoolVar both = new BoolVar(this);
        
        dom(this, selectedActions.get(i), SRT_SUP, actionId, thisActionSelected);
        dom(this, trueAtoms.get(i), SRT_SUP, positivePreconditions, positiveSatisfied);
        dom(this, trueAtoms.get(i), SRT_DISJ, negativePreconditions, negativeSatisfied);
        bool_and(this, positiveSatisfied, negativeSatisfied, both);
        
        bool_imp(this, thisActionSelected, both, true);
    }
}
*/




/*
// actions must be compatible
// Oh dear: The nested selection constraints propagate horribly.
// Yay, it works better if we do it differently. :)
rel(this, allPositiveEffects, SRT_DISJ, allNegativeEffects);

IntSet allActionIndices = new IntSet(0, actionInstances.size()-1);
VarArray<SetVar> negPosClashes = new VarArray<SetVar>(this, actionInstances.size(), SetVar.class, emptySet, allActionIndices);
VarArray<SetVar> posNegClashes = new VarArray<SetVar>(this, actionInstances.size(), SetVar.class, emptySet, allActionIndices);

for( int j = 0; j < actionInstances.size(); j++ ) {
    SetVar allOtherActions = new SetVar(this);
    SetVar allButJ = new SetVar(this);
    IntSet jAsSet = new IntSet(j,j);
    SetVar allOtherPosPreconditions = new SetVar(this);
    SetVar allOtherNegPreconditions = new SetVar(this);
    
    rel(this, allActionIndices, SOT_MINUS, jAsSet, SRT_EQ, allButJ);
    rel(this, selectedActions.get(i), SOT_MINUS, jAsSet, SRT_EQ, allOtherActions);

    selectUnion(this, actionPosPreconditions, allOtherActions, allOtherPosPreconditions);
    selectUnion(this, actionNegPreconditions, allOtherActions, allOtherNegPreconditions);
    
    rel(this, actionPosEffects.get(j), SOT_INTER, allOtherNegPreconditions, SRT_EQ, posNegClashes.get(j));
    rel(this, actionNegEffects.get(j), SOT_INTER, allOtherPosPreconditions, SRT_EQ, negPosClashes.get(j));  
}

SetVar emptySetAsVar = new SetVar(this);
cardinality(this, emptySetAsVar, 0, 0);
selectUnion(this, negPosClashes, selectedActions.get(i), emptySetAsVar);
selectUnion(this, posNegClashes, selectedActions.get(i), emptySetAsVar);
*/
