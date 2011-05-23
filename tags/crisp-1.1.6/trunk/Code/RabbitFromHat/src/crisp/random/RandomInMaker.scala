package crisp.random;

import crisp.planningproblem._;
import crisp.converter._;
import de.saar.chorus.term._;
import scala.collection.mutable._;

import crisp.rfh._;


object RandomInMaker {
  val rand = new Random();
  val edgeDensity = 0.2;
  
	def addRandomObjects(numRabbits:Int, numOthers:Int, domain:Domain, problem:Problem) = {
   	  (1 to numRabbits).foreach { i =>
		domain.addConstant("r" + i, "individual");
                domain.addConstant("h" + i, "individual");
                
                problem.addToInitialState(FfConverter.term("rabbit", "r" + i));
                problem.addToInitialState(FfConverter.term("hat", "h" + i));
          }
             
          problem.addToInitialState(FfConverter.term("in", "r1", "h1"));
             
          (1 to numOthers).foreach { i =>
            domain.addConstant("dl" + i, "individual");
            domain.addConstant("dr" + i, "individual");
          }
          
          (1 to numRabbits).foreach { i =>
            (1 to numOthers).foreach { j =>
              if( rand.nextDouble <= edgeDensity ) {
                problem.addToInitialState(FfConverter.term("in", "r" + i, "dr" + j));
              }
              
              if( rand.nextDouble <= edgeDensity ) {
                problem.addToInitialState(FfConverter.term("in", "dl" + j, "h" + i));
              }
            }
          }
        }
}
