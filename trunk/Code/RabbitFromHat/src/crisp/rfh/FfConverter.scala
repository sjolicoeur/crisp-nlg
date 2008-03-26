package crisp.rfh;

import crisp.planningproblem._;
import crisp.converter._;
import de.saar.chorus.term._;
import scala.collection.mutable._;

object FfConverter {
  def main(args : Array[String]) : Unit = {
    val domain = new Domain;
    val problem = new Problem();

    val start = System.currentTimeMillis();
    CRISPtoPDDL.convert(args(0), domain, problem);
    val end = System.currentTimeMillis();
    
    val individuals = new JavaSetAdaptor(new java.util.HashSet[String](domain.getUniverse("individual")));
    val initialState = new JavaSetAdaptor(new java.util.HashSet[Term](problem.getInitialState()));
    
    println("individuals: " + individuals);
    println("initialState: " + initialState);
    
    val splits = SplitMaker.makeSplits("r1", "h1", individuals, { (x:String,y:String) => isRelated(x,y,"in",initialState) });
    
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
    
    domain.addConstant("in", "predicate");
    
    var i = 1;
    
    splits.foreach { split =>
      val splitname = "split-" + i;
      
      domain.addConstant(splitname, "partition");
      problem.addToInitialState(term("distractorset", splitname, "in", "r1", "h1"));
      
      split._1.foreach { l =>
        problem.addToInitialState(term("distractorL", splitname, l));
      }
      
      split._2.foreach { r =>
        problem.addToInitialState(term("distractorR", splitname, r));
      }
      
      i = i+1;
    }
    
    
    
    splits.foreach { x => println(x) };
    //System.exit(0);
    
    

    System.err.println("Total runtime: " + (end-start) + "ms");

    System.out.println("Domain: " + domain);
    System.out.println("Problem: " + problem);

    CRISPtoPDDL.writeToDisk(domain, problem, "");
  }
  
  
  def term(f:String, xs:String*) = {
    new Compound(f, (xs.map { x => ( (new Constant(x)).asInstanceOf[Term] ) }).toArray);
  }
  
  def isRelated(x:String, y:String, rel:String, p:Set[Term]) = {
    //val term = new Compound(rel, List[Term](new Constant(x), new Constant(y)).toArray);
    val t = term(rel, x, y);
    
    p.contains(t)
  }
}
