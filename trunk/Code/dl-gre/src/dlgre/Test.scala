package dlgre;

object Test {
  def main(args : Array[String]) : Unit = {
    val g = new Graph;
    
    g.addPredicate("b1", "bowl");
    g.addPredicate("b2", "bowl");
    g.addPredicate("c1", "cup");
    g.addPredicate("c2", "cup");
    g.addPredicate("f1", "floor");
    g.addPredicate("t1", "table");
    
    g.addEdge("c1", "b1", "in");
    g.addEdge("c2", "b2", "in");
    
    g.addEdge("b1", "f1", "on");
    g.addEdge("b2", "t1", "on");
    
    println(g);
    
    println("\nBisimulation classes:");
    val c = new BisimulationClassesComputer(g);
    c.compute.foreach { fmla => println(fmla) }
  }
}
