package chat4s.graph

import java.lang.ProcessBuilder as JProcessBuilder
import java.nio.file.{Files, Path}

/** Renders Mermaid diagrams for registered workflow blueprints as PNG images.
  *
  * Requires `mmdc` (Mermaid CLI) on the PATH:
  *   npm install -g @mermaid-js/mermaid-cli
  *
  * Each argument must be the fully-qualified name of a Scala companion object
  * that exposes a `blueprint: WorkflowBlueprint` member, e.g.:
  *
  * {{{
  *   sbt "mermaid"
  *   // or directly:
  *   sbt "server/runMain chat4s.graph.ExportMermaid sding.workflow.graph.ProjectContextGraph$"
  * }}}
  *
  * Images are written to `target/mermaid/<name>.png` relative to the working
  * directory.
  */
object ExportMermaid:
  def main(args: Array[String]): Unit =
    if args.isEmpty then
      System.err.println("Usage: ExportMermaid <fully.qualified.Object$> ...")
      sys.exit(1)

    val outDir = Path.of("target", "mermaid")
    Files.createDirectories(outDir)

    args.foreach { objectName =>
      val instance  = Class.forName(objectName).getField("MODULE$").get(null)
      val blueprint = instance.getClass.getMethod("blueprint").invoke(instance).asInstanceOf[WorkflowBlueprint]
      val mermaid   = WorkflowMermaid.render(blueprint)

      val safeName = blueprint.name.replaceAll("[^a-zA-Z0-9_-]", "_")
      val tmpMmd   = Files.createTempFile("mermaid-", ".mmd")
      try
        Files.writeString(tmpMmd, mermaid)

        val outFile = outDir.resolve(s"$safeName.png").toAbsolutePath
        // Run through the user's login shell so nvm/npm paths are on PATH.
        val shell   = sys.env.getOrElse("SHELL", "/bin/sh")
        val cmd     = s"""mmdc -i "${tmpMmd}" -o "${outFile}""""
        val process = new JProcessBuilder(shell, "-lc", cmd)
          .redirectErrorStream(true)
          .start()
        val cmdOutput = String(process.getInputStream.readAllBytes())
        val exitCode  = process.waitFor()

        if exitCode == 0 then println(s"[${blueprint.name}] → $outFile")
        else
          System.err.println(s"mmdc failed (exit $exitCode):\n$cmdOutput")
          System.err.println("Is `mmdc` installed? npm install -g @mermaid-js/mermaid-cli")
          sys.exit(exitCode)
      finally Files.deleteIfExists(tmpMmd)
    }
