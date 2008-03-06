package dlgre;

import scala.collection.mutable.Queue;
import scala.collection.mutable.Set;

import dlgre.formula._;

class PositiveClassComputer(graph:Graph) {
  val classes = new ClassContainer(graph);
  
  
  def compute = {
    val simplifier = new dlgre.formula.Simplifier(graph);

    // initialize predicates
    graph.getAllPredicates.foreach { p =>
      classes.add(new Literal(p,true));
    }
    
    // iterate over roles
    var madeChanges = true;

    while( madeChanges && !classes.isAllSingletons ) {
      madeChanges = false;
      
      //println("\n\n\n\n\nClasses:");
      //classes.getClasses.foreach { fmla => println(simplifier.removeConjunctionsWithTop(fmla).prettyprint + ": " + util.StringUtils.join(fmla.extension(graph),",")) }
      print(".");
      
      graph.getAllRoles.foreach{ r =>
	classes.getClasses.foreach { cl =>
        	val changed = classes.add(new Existential(r,cl));
                madeChanges = madeChanges || changed;
        }
      }
    }
    
    print("[max=" + classes.getMaxSize + "]");
    
    classes.getClasses
    
  }
}
