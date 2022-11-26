package Classes

import java.io.{File, FileNotFoundException, FileOutputStream, IOException, ObjectOutputStream}

// UserInfoSaver saves the users data into the savefile
class UserInfoSaver(theUser: User) extends ObjectOutputStream {

  // The method for saving user info to the savefile using java.io.objectoutputstream
  def saveIntoFile() = {
  try {
    val fileOutputStream = new FileOutputStream(new File("savefile.txt"))
    val objectOutputStream = new ObjectOutputStream(fileOutputStream)

    objectOutputStream.writeObject(theUser)

    objectOutputStream.close()
    fileOutputStream.close()

  } catch {
    case e: FileNotFoundException => println("Could not find the save file.")
    case f: IOException => println("An error occurred trying to read the file.")
  }

  }

}
