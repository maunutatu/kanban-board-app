package Classes

import java.time.LocalDate
import scala.collection.mutable._

// The Board class is responsible for storing all the data for a single board.
class Board(var name: String, var contents: Buffer[Task], var taskArchive: Buffer[Task], var taskColumns: Buffer[TaskColumn], var background: Option[String]) extends Serializable {

  // Method for adding new tasks to a board.
  def newTask(header: String, desc: String, picAttachment: Option[String], txtAttachment: Option[String], deadline: LocalDate, possibleSteps: Buffer[String], completedSteps: Buffer[String], taskColumn: TaskColumn) = {
    val theNewTask = new Task(header, desc, picAttachment, txtAttachment, deadline, possibleSteps, completedSteps, taskColumn, false, None)
    theNewTask.taskColumn.contents += theNewTask
    contents += theNewTask
  }

  // Method for removing tasks from a board. This is irreversible.
  def removeTask(theTask: Task) = {
    if (!theTask.archived) {
      theTask.taskColumn.contents -= theTask
      contents -= theTask
    } else {
      taskArchive -= theTask
      contents -= theTask
    }
  }

  // Method for archiving tasks away from the board. This is reversible.
  def archiveTask(theTask: Task) = {
    theTask.taskColumn.contents -= theTask
    taskArchive += theTask
  }

  // Method for bringing tasks back from the archive.
  def dearchiveTask(theTask: Task) = {
    if (taskColumns.nonEmpty) {
      theTask.taskColumn.contents += theTask
      taskArchive -= theTask
    }
  }

  // The method responsible for moving tasks from a column to another.
  def moveTask(theTask: Task, columnToMoveTo: TaskColumn) = {
    theTask.taskColumn.contents -= theTask
    columnToMoveTo.contents += theTask
    theTask.taskColumn = columnToMoveTo
  }

  //  Method for creating new columns to a board, for example to-do, done etc.
  def newColumn(name: String) = taskColumns += new TaskColumn(name, Buffer())

  def deleteColumn(theColumn: TaskColumn) = {
    taskColumns -= theColumn
    theColumn.contents.foreach(contents -= _)
  }

}
