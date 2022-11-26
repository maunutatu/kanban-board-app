package Classes
import scala.collection.mutable._

// A TaskCategory is an attachable tag of information, that one can use for sorting different tasks.
class TaskCategory(var name: String, var contents: Buffer[Task]) extends Serializable {
  override def toString = name
}
