package Classes

import java.time.LocalDate
import scala.collection.mutable._

// The class responsible for a single tasks information
class Task(var header: String, var desc: String, var picAttachment: Option[String], var txtAttachment: Option[String], var deadline: LocalDate, var possibleSteps: Buffer[String], var completedSteps: Buffer[String], var taskColumn: TaskColumn, var archived: Boolean, var tag: Option[TaskCategory]) extends  Serializable{

  // Calculates the progress of the task
  def calculateProgress: Double = (this.completedSteps.size * 1.0 / this.possibleSteps.size)

  def stepCompletion(step: String) : Unit = {
    if(completedSteps.contains(step)) completedSteps -= step else completedSteps += step
  }

  // For testing purposes
  override def toString = this.header + "\n" + this.desc + "\n" + this.deadline + "\n" + "Tag: " + this.tag.getOrElse("None").toString

}
