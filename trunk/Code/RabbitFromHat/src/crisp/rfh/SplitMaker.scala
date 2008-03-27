package crisp.rfh;

import scala.collection.mutable._;

object SplitMaker {
  def main(args : Array[String]) : Unit = {
    val domain = List("r1", "r2", "r3", "f1", "f2", "h1", "h2", "h3", "b1", "b2");
    
    println(makeSplits("r1", "h1", domain, isIn));
  }
  
  def makeSplits(targetL:String, targetR:String, domain:Iterable[String], isRelated:(String,String) => Boolean) = {
    val splits = new HashSet[(Set[String],Set[String])];
    val target = new HashSet[(String,String)];
    val bothSubset = {(s1:(Set[String],Set[String]), s2:(Set[String],Set[String])) => (s1._1 subsetOf s2._1) && (s1._2 subsetOf s2._2)};
    
    target += ((targetL,targetR));
    
    SubsetGenerator.foreachSubset(domain, { subset:Set[String] =>
      if( !subset.contains(targetL) ) {
        var notR = new HashSet[String];
        notR ++= domain;
        notR --= subset;
        
        val h = new HashSet[String];
        h ++= domain.filter { x => (x != targetR) && notR.exists { y => isRelated(y,x) }};
        
        val intersect = new HashSet[(String,String)];
        domain.foreach { x => domain.foreach { y => 
        	if( isRelated(x,y) && !subset.contains(x) && !h.contains(y) ) {
           		intersect += ((x,y));       
                }
        }};
        
        
        if( intersect == target ) {
          val pair = ((subset,h));
          if( ! splits.exists { split => bothSubset(split, pair) } ) {
            splits += ((subset,h));
          }
        }
      }
    });

    minimize[(Set[String],Set[String])](splits, {(s1,s2) => if( bothSubset(s1,s2) ) { -1 } else { 1 }});

    splits
  }
    
  
  
  def minimize[E](set:Set[E], compare:((E,E) => Int)) = {
    val elements = set.toList;
    
    elements.foreach { x =>
      if( set.contains(x) ) {
        elements.foreach { y =>
          if( (x != y) && set.contains(y) && (compare(x,y) < 0) ) {
            set -= y;
          }
        }
      }
    }
  }
  
  def isIn(x:String, y:String) = {
    ((x == "r1") && (y == "h1")) ||
    ((x == "f2") && (y == "h1")) ||
    ((x == "f1") && (y == "h3")) ||
    ((x == "r3") && (y == "b1"))
  }
}
