package crisp.rfh;

import crisp.planningproblem._;
import crisp.converter._;
import de.saar.chorus.term._;
import scala.collection.mutable._;

object FfConverter {
  val newAction = action(pred("init-transImperative-take_P-from-1",
                ("?u","syntaxnode"), ("?x1","individual"), ("?x2","individual"), ("?x3","individual"), ("?s","partition")),
gconj(glit(true, "distractorset", "?s", "pred-in", "?x2", "?x3"),
     glit(true, "step", "step1"),
     glit(true, "referent", "?u", "?x1"),
     glit(true, "subst", "S", "?u"),
     glit(true, "takefrom", "?x1", "?x2", "?x3")),
econj(elit(false, "step", "step1"),
     elit(true, "step", "step2"),
     elit(false, "subst", "S", "?u"),
     elit(false, "needtoexpress-3", "pred-takefrom", "?x1", "?x2", "?x3"),
     euniv(econd(glit(false, "takefrom", "?y", "?x2", "?x3"), elit(false, "distractor", "?u", "?y")),
           ("?y", "individual")),
     elit(true, "subst", "NP", "obj-1"),
     elit(true, "referent", "obj-1", "?x2"),
     elit(true, "subst", "NP", "ppobj-1"),
     elit(true, "referent", "ppobj-1", "?x3"),
     elit(true, "canadjoin", "S", "?u"),
     elit(true, "canadjoin", "VP", "?u"),
     elit(true, "canadjoin", "V", "?u"),
     elit(true, "canadjoin", "PP", "ppobj-1"),
     elit(true, "canadjoin", "PP", "ppobj-1"),
     euniv(econd(glit(true, "distractorL", "?s", "?y"), elit(true, "distractor", "obj-1", "?y")),
           ("?y", "individual")),
     euniv(econd(glit(true, "distractorR", "?s", "?y"), elit(true, "distractor", "ppobj-1", "?y")),
           ("?y", "individual"))));
  
  
  def addSplits(domain:Domain, problem:Problem) = {
    val start_time = System.currentTimeMillis();
    
    val individuals = new JavaSetAdaptor(new java.util.HashSet[String](domain.getUniverse("individual")));
    val initialState = new JavaSetAdaptor(new java.util.HashSet[Term](problem.getInitialState()));
    var numSplits = 0;
    
    
    //  add necessary metadata to the domain

    domain.addSubtype("partition", "object");

    val predDistractorSet = new Predicate();
    predDistractorSet.setLabel("distractorset");
    predDistractorSet.addVariable("?x", "partition");
    predDistractorSet.addVariable("?y", "predicate");
    predDistractorSet.addVariable("?z", "individual");
    predDistractorSet.addVariable("?w", "individual");
    domain.addPredicate(predDistractorSet);

    val predDistractorL = new Predicate();
    predDistractorL.setLabel("distractorL");
    predDistractorL.addVariable("?x", "partition");
    predDistractorL.addVariable("?z", "individual");
    domain.addPredicate(predDistractorL);

    val predDistractorR = new Predicate();
    predDistractorR.setLabel("distractorR");
    predDistractorR.addVariable("?x", "partition");
    predDistractorR.addVariable("?z", "individual");
    domain.addPredicate(predDistractorR);

    domain.addConstant("pred-in", "predicate");



    // compute the splits and add them to the problem

    var i = 1;
    val isIn = computeIsRelated("in", individuals, initialState); 
    individuals.foreach { x =>
      individuals.foreach { y =>
        if( isRelated(x,y,"in",initialState) ) {
          System.err.println("Computing splits for " + x + "," + y + "...");
          val splits = SplitMaker.makeSplits(x, y, individuals, isIn);
  
          splits.foreach { split =>
            val splitname = "split-" + i;
    
            domain.addConstant(splitname, "partition");
            problem.addToInitialState(term("distractorset", splitname, "pred-in", x, y));
    
            split._1.foreach { l =>
              problem.addToInitialState(term("distractorL", splitname, l));
            }
    
            split._2.foreach { r =>
              problem.addToInitialState(term("distractorR", splitname, r));
            }
    
            i = i+1;
          }
          
          numSplits = numSplits + splits.size;
        }
      }
    }
    
    numSplits
  }
  
  
  def main(args : Array[String]) : Unit = {
    val domain = new Domain;
    val problem = new Problem();
    
    val start_time = System.currentTimeMillis();

    val start = System.currentTimeMillis();
    CRISPConverter.convert(args(0), domain, problem);
    val end = System.currentTimeMillis();
    
    crisp.random.RandomInMaker.addRandomObjects(3, 2, domain, problem);
    
    
    val individuals = new JavaSetAdaptor(new java.util.HashSet[String](domain.getUniverse("individual")));
    val initialState = new JavaSetAdaptor(new java.util.HashSet[Term](problem.getInitialState()));
    
    /*
    System.err.println("Extension of 'in':");
    individuals.foreach { x => individuals.foreach { y =>
    	if( isRelated(x,y,"in",initialState) ) {
	   System.err.println("in(" + x + "," + y + ")");              
        }
    }}
    */
    
    
    val numSplits = addSplits(domain,problem);
    println("Found " + numSplits + " splits");
    
    domain.removeAction(pred("init-transImperative-take-P_from-1", 
        ("?u", "syntaxnode"), ("?x1", "individual"), ("?x2", "individual"), ("?x3", "individual")));
    domain.addAction(newAction);

    new crisp.planningproblem.codec.PddlOutputCodec().writeToDisk(domain, problem, "", domain.getName());
    
    val end_time = System.currentTimeMillis();
    
    System.out.println("Time: " + (end_time-start_time));
  }
  
  def pred(f:String, xs:(String,String)*) = {
    val p = new crisp.planningproblem.Predicate();
    
    p.setLabel(f);
    xs.foreach { a => a match { case (v:String,t:String) => p.addVariable(v,t) }};
    p
  }
  
  
  def term(f:String, xs:String*) : Term = {
    //new Compound(f, (xs.map { x => ( (new Constant(x)).asInstanceOf[Term] ) }).toArray);
    terma(f,xs.toArray)
  }

  def terma(f:String, xs:Array[String]) : Term = {
    new Compound(f, (xs.map { x => ( 
        if( x.startsWith("?") ) {
          (new Variable(x)).asInstanceOf[Term]
        } else {
          (new Constant(x)).asInstanceOf[Term]
        })}).toArray);
  }

  def isRelated(x:String, y:String, rel:String, p:Set[Term]) = {
    //val term = new Compound(rel, List[Term](new Constant(x), new Constant(y)).toArray);
    val t = term(rel, x, y);
    
    p.contains(t)
  }
  
  def computeIsRelated(rel:String, individuals:Collection[String], p:Set[Term]) = {
    val pairs = new HashSet[(String,String)];
    
    individuals.foreach { x => individuals.foreach { y => if( p.contains(term(rel, x, y)) ) pairs += ((x,y)) } };
    
    {(x:String,y:String) => pairs.contains((x,y))}
  }
  
  def gconj(sub:crisp.planningproblem.goal.Goal*) = {
    val args = new java.util.ArrayList[crisp.planningproblem.goal.Goal]();
    
    sub.foreach { g => args.add(g) };
    new crisp.planningproblem.goal.Conjunction(args);
  }

  def econj(sub:crisp.planningproblem.effect.Effect*) = {
    val args = new java.util.ArrayList[crisp.planningproblem.effect.Effect]();
    
    sub.foreach { g => args.add(g) };
    new crisp.planningproblem.effect.Conjunction(args);
  }

  def glit(p:Boolean, f:String, xs:String*) = {
    val t = terma(f,xs.toArray);
    new crisp.planningproblem.goal.Literal(t, p);
  }
                                               
  def elit(p:Boolean, f:String, xs:String*) = {
    val t = terma(f,xs.toArray);
    new crisp.planningproblem.effect.Literal(t, p);
  }
  
  def euniv(sub:crisp.planningproblem.effect.Effect, vars:(String,String)*) = {
    val varlist = new crisp.planningproblem.TypedList();
    
    vars.foreach { a => a match { case (v,t) => varlist.addItem(v,t) }};
    new crisp.planningproblem.effect.Universal(varlist, sub);
  }
  
  def econd(pre:crisp.planningproblem.goal.Goal, con:crisp.planningproblem.effect.Effect) = {
    new crisp.planningproblem.effect.Conditional(pre,con);
  }
  
  def action(header:Predicate, pre:crisp.planningproblem.goal.Goal, eff:crisp.planningproblem.effect.Effect) = {
    new Action(header,pre,eff)
  }
  
  
}
