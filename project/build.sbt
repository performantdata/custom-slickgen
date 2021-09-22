libraryDependencies ++= {
  val slickVersion = "3.3.3"

  Seq(
    "com.typesafe.slick" %% "slick-codegen" % slickVersion,
    "com.h2database" % "h2" % "1.4.200",
  )
}
