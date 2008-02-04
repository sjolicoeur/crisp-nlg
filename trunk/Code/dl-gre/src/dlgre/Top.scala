package dlgre;

case class Top extends Property {
  override def filter(g:Graph) : List[String] = g.getAllNodes
}
