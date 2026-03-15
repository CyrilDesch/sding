package sding.client.components

import com.raquo.laminar.api.L.*

object ProgressBar:

  private val allSteps = List(
    "human_requirements",
    "weird_problem_generation",
    "problem_reformulation",
    "trend_analysis",
    "problem_selection",
    "human_problem_selection",
    "user_interviews",
    "empathy_map",
    "jtbd_definition",
    "human_jtbd_selection",
    "hmw",
    "crazy8s",
    "scamper",
    "competitive_analysis",
    "human_comprehensive_selection",
    "prototype_builds",
    "human_project_selection",
    "premium_report",
    "markdown_generation"
  )

  private def friendlyName(step: String): String =
    step.replace("_", " ").split(" ").map(_.capitalize).mkString(" ")

  def render(completedSignal: Signal[Set[String]], activeSignal: Signal[Option[String]]): HtmlElement =
    htmlTag("nav")(
      cls := "w-64 bg-white border-r border-gray-200 overflow-y-auto py-6 px-4 flex-shrink-0",
      h3(cls := "text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4 px-2", "Workflow Steps"),
      ul(
        cls := "space-y-1",
        allSteps.map { step =>
          li(
            cls <-- completedSignal.combineWith(activeSignal).map { case (completed, active) =>
              val base = "flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors"
              if completed.contains(step) then s"$base text-green-700 bg-green-50"
              else if active.contains(step) then s"$base text-indigo-700 bg-indigo-50 font-medium"
              else s"$base text-gray-400"
            },
            span(
              cls <-- completedSignal.combineWith(activeSignal).map { case (completed, active) =>
                val base = "w-2 h-2 rounded-full flex-shrink-0"
                if completed.contains(step) then s"$base bg-green-500"
                else if active.contains(step) then s"$base bg-indigo-500 animate-pulse"
                else s"$base bg-gray-300"
              }
            ),
            span(friendlyName(step))
          )
        }
      )
    )
