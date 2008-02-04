package dlgre;

import dlgre.formula._;

class Subset(from:Property, graph:Graph) {
  val individuals = from.filter(graph)
          
	def getIndividuals : List[String] = {
          individuals
        }
  
  	def splitOverLiteral(pred:String, polarity:Boolean) = {
     		(new Subset(Literal(this, pred, polarity, true), graph), 
                 new Subset(Literal(this, pred, polarity, false), graph))
          }
          
        def canSplitOverLiteral(pred:String, polarity:Boolean) = {
          getIndividuals.size > 1 &&
          getIndividuals != Literal(this, pred, polarity, true).filter(graph) &&
            getIndividuals != Literal(this, pred, polarity, false).filter(graph)
        }
        
        
        def splitOverRole(role:String, subsets:(Subset,Subset)) = {
          (new Subset(Existential(this, role, subsets._1), graph),
           new Subset(Existential(this, role, subsets._2), graph))
        }
        
        def canSplitOverRole(role:String, subsets:(Subset,Subset)) = {
          getIndividuals.size > 1 &&
          getIndividuals.exists { x => subsets._1.getIndividuals.exists {y => graph.hasEdge(x,role,y)} } &&
            getIndividuals.exists { x => subsets._2.getIndividuals.exists {y => graph.hasEdge(x,role,y)} }
        }
        
        def getFormula : dlgre.formula.Formula = {
          from match {
            case Top() => dlgre.formula.Top
            case Literal(fromSubset, p, polarity, isTrue) => 
               dlgre.formula.Conjunction(dlgre.formula.Literal(p, polarity == isTrue), fromSubset.getFormula)
            case Existential(fromSubset, role, roleInto) =>
               dlgre.formula.Conjunction(dlgre.formula.Existential(role, roleInto.getFormula), fromSubset.getFormula)
          }
        }
        
        
        def getGraph = graph;
        
        
        override def toString = {
          getFormula.prettyprint + ": " + getIndividuals
        }
        

        def simplify(fmla:Formula) : Formula = {
          fmla match {
            case Conjunction(dlgre.formula.Literal(p,true),dlgre.formula.Top()) => dlgre.formula.Literal(p,true) 
            case Conjunction(x,dlgre.formula.Top()) => simplify(x)
            case Conjunction(dlgre.formula.Literal(p,false),x) => simplify(x)
            case Conjunction(dlgre.formula.Literal(p,true),x) => Conjunction(dlgre.formula.Literal(p,true), simplify(x))
            case _ => fmla
          }
        }
}
