package Classes

import java.io.{File, FileInputStream, FileNotFoundException, IOException, ObjectInputStream}
import scala.collection.mutable._

// UserInfoRecover brings back the saved user data from the savefile
object UserInfoRecover extends ObjectInputStream {

  // Creating a returnable user if readAndReturn try block goes wrong
  var theUser: User = new User("emptyUser", Buffer(),Buffer(),0)

  // The method for recovering user info from the savefile using java.io.objectinputstream
  def readAndReturn = {
      try {
        val fileInputStream = new FileInputStream(new File("./savefile.txt"))
        val objectInputStream = new ObjectInputStream(fileInputStream)

        theUser = objectInputStream.readObject().asInstanceOf[User]

        objectInputStream.close()
        fileInputStream.close()

      } catch {
        case e: FileNotFoundException => println("Could not find the save file.")
        case f: IOException => println("An error occurred trying to read the file.")
      }
    theUser
  }

}
