import scoverage.ScoverageKeys
  
object ScoverageSettings {
  def apply() = Seq(
    ScoverageKeys.coverageExcludedPackages := Seq(
      "<empty",
      """.*\.domain\.models\..*""" ,
      """uk\.gov\.hmrc\.apiplatformmicroservice\.common\.controllers\.binders""",
      """uk\.gov\.hmrc\.apiplatformmicroservice\.apidefinition\.controllers\.binders""",
      """uk\.gov\.hmrc\.apiplatform.modules\.apis\..*""",
      """uk\.gov\.hmrc\.apiplatform.modules\.applications\..*""",
      """uk\.gov\.hmrc\.apiplatform.modules\.developers\..*""",
      """uk\.gov\.hmrc\.apiplatform.modules\.common\..*""",
      """uk\.gov\.hmrc\.BuildInfo""" ,
      """.*\.Routes""" ,
      """.*\.RoutesPrefix""" ,
      """.*Filters?""" ,
      """MicroserviceAuditConnector""" ,
      """Module""" ,
      """GraphiteStartUp""" ,
      """.*\.Reverse[^.]*""",
    ).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 84.20,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
