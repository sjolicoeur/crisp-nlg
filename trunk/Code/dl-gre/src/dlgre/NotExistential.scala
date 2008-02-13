package dlgre;

case class NotExistential(from:Subset, role:String, roleInto:Subset) extends Property {
  override def filter(g:Graph) : List[String] = {
    for( x <- from.getIndividuals if ! roleInto.getIndividuals.exists { y => g.hasEdge(x,role,y) } )
      yield x
}

}
