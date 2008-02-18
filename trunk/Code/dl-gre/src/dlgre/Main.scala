package dlgre;

import java.io._

import scala.xml.parsing.ConstructingParser

object Main {
  def main(args : Array[String]) : Unit = {
    val p = ConstructingParser.fromFile(new File(args(0)), true)
    val doc: xml.Document = p.document
    
    val graph = new Graph
    
    (doc \ "individual").foreach { indiv =>
    	val node = mygetattr(indiv, "id");

        (indiv \ "predicate" ).foreach { element =>
        	val pred = mygetattr(element, "pred");
                graph.addPredicate(node, pred); 
        }
        
        (indiv \ "related").foreach { element =>
        	val rel = mygetattr(element, "rel");
                val to = mygetattr(element, "to");
                graph.addEdge(node, to, rel);
        }
    }
    
    println("Loaded graph: " + graph.getAllNodes.size + " nodes, " + graph.getAllEdges.size + " edges.");
    
    print("\nComputing bisimulation classes ... ");
    
    val start = System.currentTimeMillis;
    //val result = new BisimulationClassesComputer(graph).compute;
    val result = new PositiveClassComputer(graph).compute;
    val simplifier = new dlgre.formula.Simplifier(graph);

    println("done, " + (System.currentTimeMillis - start) + " ms.");
    
    println("\nBisimulation classes with their concepts:");
    result.foreach { fmla => println(simplifier.removeConjunctionsWithTop(fmla).prettyprint + ": " + util.StringUtils.join(fmla.extension(graph),",")) }
  }
  
  private def mygetattr(node : scala.xml.Node, attr : String) = {
    node.attribute(attr) match { 
      case Some(a) => a.last.text; 
      case _ => throw new Exception("Undefined attribute " + attr + " in node " + node); 
    }
  }
}
