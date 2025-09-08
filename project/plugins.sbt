resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"

resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("uk.gov.hmrc"        %  "sbt-auto-build"         % "3.24.0")
addSbtPlugin("uk.gov.hmrc"        %  "sbt-distributables"     % "2.6.0")
addSbtPlugin("org.playframework"  %  "sbt-plugin"             % "3.0.8")
addSbtPlugin("org.scoverage"      %  "sbt-scoverage"          % "2.3.1")
addSbtPlugin("org.scalameta"      %  "sbt-scalafmt"           % "2.5.5")
addSbtPlugin("ch.epfl.scala"      %  "sbt-bloop"              % "2.0.10")
addSbtPlugin("ch.epfl.scala"      %% "sbt-scalafix"           % "0.14.3")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
