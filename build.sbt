import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

name := "fuse"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++=
  Seq(
    "com.lmax" % "disruptor" % "3.3.4",
    "org.scalactic" %% "scalactic" % "2.2.6" % Test,
    "org.scalatest" %% "scalatest" % "2.2.6" % Test
  )

SbtScalariform.scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(DanglingCloseParenthesis, Force)
