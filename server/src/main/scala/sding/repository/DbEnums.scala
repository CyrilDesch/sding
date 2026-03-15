package sding.repository

enum SenderType:
  case User, System, Agent

enum ContentType(val formatString: String):
  case Text     extends ContentType("text")
  case Markdown extends ContentType("markdown")
  case Html     extends ContentType("html")

enum ProjectStatus:
  case Draft, InProgress, Completed, Archived
