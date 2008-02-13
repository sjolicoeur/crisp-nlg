package dlgre;

import dlgre.formula._;

class Subset(from:Property, graph:Graph, simplifier:Simplifier) {
  val individuals = from.filter(graph)
          
  	// Returns the list of individuals in this class.
	def getIndividuals : List[String] = {
          individuals
        }
  
  	// If the given literal splits this subset into two new subsets sub1 and sub2,
  	// this method returns Some((sub1,sub2)) (i.e. an Option of a tuple).  Otherwise,
  	// it returns None.
  	def splitOverLiteral(pred:String, polarity:Boolean) = {
            if( getIndividuals.size <= 1 ) {
              None
            } else {
              val sub1 = new Subset(Literal(this, pred, polarity, true), graph, simplifier);
              val sub2 = new Subset(Literal(this, pred, polarity, false), graph, simplifier);
              
              if( getIndividuals != sub1.getIndividuals  &&  getIndividuals != sub2.getIndividuals ) {
                Some((sub1,sub2))
              } else {
                None
              }
            }
          }

        
        // If traversing the given role maps two individuals in this subset to different
        // classes (given in the subsets argument), then this method splits this subset into
        // two classes accordingly and returns Some((sub1,sub2)) (i.e., an Option of a tuple).
        // Otherwise, it returns None.
        def splitOverRole(role:String, subsets:(Subset,Subset)) = {
          if( canSplitOverRole(role, subsets) ) {
            Some((new Subset(Existential(this, role, subsets._1), graph, simplifier),
                  new Subset(Existential(this, role, subsets._2), graph, simplifier)))
          } else {
            None
          }
        }
        
        private def canSplitOverRole(role:String, subsets:(Subset,Subset)) = {
          getIndividuals.size > 1 &&
          getIndividuals.exists { x => subsets._1.getIndividuals.exists {y => graph.hasEdge(x,role,y)} } &&
            getIndividuals.exists { x => subsets._2.getIndividuals.exists {y => graph.hasEdge(x,role,y)} }
        }
        
        
        def splitOverRole1(role:String, subset:Subset) = {
          if( getIndividuals.size > 1 &&
            getIndividuals.exists { x => subset.getIndividuals.exists { y => graph.hasEdge(x,role,y)} } &&
            getIndividuals.exists { x => subset.getIndividuals.forall { y => !graph.hasEdge(x,role,y)} } ) {
            Some((new Subset(Existential(this, role, subset), graph, simplifier),
                  new Subset(NotExistential(this, role, subset), graph, simplifier)))
          } else {
            None
          }
        }
        
        
        // Returns the characteristic concept of this class.
        def getFormula : dlgre.formula.Formula = {
          from match {
            case Top() => dlgre.formula.Top
            case Literal(fromSubset, p, polarity, isTrue) => 
               dlgre.formula.Conjunction(List(dlgre.formula.Literal(p, polarity == isTrue), fromSubset.getFormula))
            case Existential(fromSubset, role, roleInto) =>
               dlgre.formula.Conjunction(List(dlgre.formula.Existential(role, roleInto.getFormula), fromSubset.getFormula))
            case NotExistential(fromSubset, role, roleInto) =>
               dlgre.formula.Conjunction(List(dlgre.formula.Negation(dlgre.formula.Existential(role, roleInto.getFormula)), fromSubset.getFormula))
          }
        }
        
        override def toString = {
          simplifier.simplify(getFormula).prettyprint + ": " + getIndividuals
        }
        
/*
        def simplify(fmla:Formula) : Formula = {
          fmla match {
            case Conjunction(dlgre.formula.Literal(p,true),dlgre.formula.Top()) => dlgre.formula.Literal(p,true) 
            case Conjunction(x,dlgre.formula.Top()) => simplify(x)
            case Conjunction(dlgre.formula.Literal(p,false),x) => simplify(x)
            case Conjunction(dlgre.formula.Literal(p,true),x) => Conjunction(dlgre.formula.Literal(p,true), simplify(x))
            case _ => fmla
          }
        }
*/
}
