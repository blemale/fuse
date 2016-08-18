import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

name := "fuse"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++=
  Seq(
    "org.agrona" % "Agrona" % "0.5.3",
    "org.scalactic" %% "scalactic" % "2.2.6" % Test,
    "org.scalatest" %% "scalatest" % "2.2.6" % Test,
    "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0" % Test
  )

SbtScalariform.scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(DanglingCloseParenthesis, Force)
