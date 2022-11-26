package Classes
import scala.collection.mutable._

// A class responsible for the Columns of a Board, for example to-do, done etc.
class TaskColumn(var name: String, var contents: Buffer[Task]) extends Serializable
