package Classes
import scala.collection.mutable._


// The User class is the highest of the classes in class hierarchy. A user class contains all the information of a user, boards and tasks etc.v
class User(var name : String, var boards: Buffer[Board], var taskCategories: Buffer[TaskCategory], var latestBoardIndex: Int) extends Serializable{

  // Creates a new empty board for a user
  def newBoard(name : String) = {
    boards += new Board(name,Buffer(),Buffer(), Buffer(), None)
  }

  // Removes the given board from a user. An irreversible action.
  def removeBoard(theBoard: Board) = boards -= theBoard

  // Creates a new TaskCategory for a user.
  def newTag(name: String) = taskCategories += new TaskCategory(name, Buffer())

  def deleteTag(name: String) = {
    taskCategories.find(_.name == name).get.contents.foreach(_.tag = None)
    taskCategories -= taskCategories.find(_.name == name).get
  }

  // Applies the given TaskCategory to a given task
  def tagATask(theTask: Task, theTag: TaskCategory) = {
    theTag.contents += theTask
    theTask.tag = Some(theTag)
  }

}
