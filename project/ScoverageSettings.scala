import scoverage.ScoverageKeys
  
object ScoverageSettings {
  def apply() = Seq(
    ScoverageKeys.coverageExcludedPackages := Seq(
      "<empty",
      """.*\.domain\.models\..*""" ,
      """uk\.gov\.hmrc\.apiplatformmicroservice\.common\.controllers\.binders""",
      """uk\.gov\.hmrc\.apiplatformmicroservice\.apidefinition\.controllers\.binders""",
      """uk\.gov\.hmrc\.BuildInfo""" ,
      """.*\.Routes""" ,
      """.*\.RoutesPrefix""" ,
      """.*Filters?""" ,
      """MicroserviceAuditConnector""" ,
      """Module""" ,
      """GraphiteStartUp""" ,
      """.*\.Reverse[^.]*""",
    ).mkString(";"),
    ScoverageKeys.coverageMinimum := 86.50,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
