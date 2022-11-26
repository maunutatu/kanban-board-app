import Classes._
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, CheckBox, ContextMenu, DatePicker, Label, Menu, MenuBar, MenuItem, ProgressBar, RadioButton, ScrollPane, TextField, ToggleGroup}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{BorderPane, HBox, StackPane, VBox}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.Font
import javafx.stage.FileChooser.ExtensionFilter
import scalafx.stage.{FileChooser, Modality, Stage}

import java.io.FileInputStream
import java.time.LocalDate
import scala.collection.immutable._
import scala.collection.mutable._
import scala.io.Source


object GUI extends JFXApp {

  // Fetching user data from savefile

  var theUser: User = UserInfoRecover.readAndReturn

  // Primary stage for the program
  stage = new JFXApp.PrimaryStage {
    title.value = "Kanban Board -app"
    width = 1200
    height = 800
  }

  // Creating the root node and a container for scene graph
  val root = new BorderPane
  val scene = new Scene(root)
  stage.scene = scene

  // Filter for filtering tasks by tag
  var currentFilter: Option[TaskCategory] = None

  // Content is stored in a scrollpane to ensure object visibility
  val scrollpane = new ScrollPane
  scrollpane.setStyle("-fx-background: #FFFFFF;")

  // Variable to keep track of the current board
  var currentBoardIndex: Int = 0

  // Helper function for creating a new board pop up window for reducing redundancy
  def newBoardMenu() = {
    // Pop up windows are built by creating a new stage and applying content inside it
    val boardWindow = new Stage()
    boardWindow.initModality(Modality.ApplicationModal)
    boardWindow.initOwner(stage)

    val boardPane = new BorderPane()
    boardPane.setPrefSize(600, 300)

    val header = new Label("Create a new board")
    header.setFont(new Font(20))
    boardPane.top = header

    // Text inputs use text fields to attain user input
    val inputarea = new VBox
    inputarea.setAlignment(Pos.Center)
    inputarea.children += new Label("Insert name of the board:")
    val nameField = new TextField
    nameField.maxWidth = 300
    inputarea.children += nameField

    inputarea.children += new Label("Write the column names:")
    val columnInput = new HBox
    columnInput.setAlignment(Pos.Center)
    columnInput.spacing = 5

    val columnText = new TextField
    val addColumn = new Button("+")
    val columnBuffer: Buffer[TaskColumn] = Buffer()
    var warningLabel = new Label("")
    warningLabel.setTextFill(Color.Red)

    // Functionality is dealt by using event handlers which are activated by user action, usually Buttons
    addColumn.onAction = (event: ActionEvent) => {
      if (columnText.getText.trim.isEmpty) {
        warningLabel = new Label("Insert correct column name!")
        columnInput.children += warningLabel
      } else if (columnBuffer.exists(_.name == columnText.getText.trim)) {
        warningLabel = new Label("No duplicate columns!")
        columnInput.children += warningLabel
      } else {
        if (columnInput.children.size == 3) columnInput.children -= columnInput.children.last
        columnBuffer += new TaskColumn(columnText.getText.trim, Buffer())
        columnText.clear()
      }
    }

    columnInput.children = List(columnText, addColumn)
    inputarea.children += columnInput

    inputarea.children += new Label("Possible background picture")

    // File IO with java.io, java.stage and scala.stage functionalities. Chosen files source is saved and later added to task data
    val fileChoosing = new HBox
    fileChoosing.setAlignment(Pos.Center)
    fileChoosing.spacing = 5
    val openFilechooser = new Button("Choose file")
    val backgroundFileChooser = new FileChooser
    var chosenBackGround: Option[String] = None

    // Chosen files source is displayed besides the Choose file button
    openFilechooser.onAction = (event: ActionEvent) => {
      val file = backgroundFileChooser.showOpenDialog(stage)
      if (file != null) {
        if (fileChoosing.children.size == 2) fileChoosing.children -= fileChoosing.children.last
        chosenBackGround = Some(file.getPath)
        fileChoosing.children += new Label(file.getName)
      } else {
        chosenBackGround = None
        if (fileChoosing.children.size == 2) fileChoosing.children -= fileChoosing.children.last
      }
    }

    fileChoosing.children += openFilechooser
    inputarea.children += fileChoosing

    val createButton = new Button("Create board")

    // Main window content is updated by updating the root node content
    createButton.onAction = (event: ActionEvent) => {
      theUser.boards += new Board(nameField.getText.trim, Buffer(), Buffer(), columnBuffer, chosenBackGround)

      setMenuBar(theUser.boards.size - 1)
      setColumns(theUser.boards.size - 1, currentFilter)

      root.top = menuBar
      scrollpane.content = columns
      root.center = scrollpane

      boardWindow.close()
    }

    inputarea.children += createButton

    boardPane.center = inputarea
    BorderPane.setAlignment(header, Pos.TopCenter)
    inputarea.spacing = 10
    val boardScene = new Scene(boardPane, 600, 300)
    boardWindow.setScene(boardScene)
    boardWindow.show()
  }


  // Here goes the MenuBar
  /* The MenuBar has Menus as its children. Clicking a Menu opens a drop down box which contains the different menu options.
     These options are implemented by adding MenuItem components to the Menu component. */
  var menuBar = new MenuBar

  def setMenuBar(boardIndex: Int): Unit = {

    menuBar = new MenuBar

    val fileMenu = new Menu("File")
    val saveItem = new MenuItem("Save")
    val saveAndExitItem = new MenuItem("Save and Exit")

    // Menu functionality is dealt with EventHandlers. Here we save user data with UserInfoSaver class
    saveItem.onAction = (event: ActionEvent) => new UserInfoSaver(theUser).saveIntoFile()

    saveAndExitItem.onAction = (event: ActionEvent) => {
      theUser.latestBoardIndex = currentBoardIndex
      new UserInfoSaver(theUser).saveIntoFile()
      sys.exit(0)
    }

    fileMenu.items = List(saveItem, saveAndExitItem)

    val boardMenu = new Menu("Board")
    val taskMenu = new Menu("Task")
    val tagMenu = new Menu("Tag")
    val archiveMenu = new Menu("Archive")
    val filterMenu = new Menu("Filter")
    val columnMenu = new Menu("Column")

    val newBoardMenuItem = new MenuItem("New Board")
    val modifyBoardItem = new MenuItem("Modify this board")

    val archiveMenuItem = new MenuItem("Open Archive")
    archiveMenu.items = List(archiveMenuItem)

    val newColumnMenuItem = new MenuItem("New Column")
    columnMenu.items = List(newColumnMenuItem)


    newBoardMenuItem.onAction = (event: ActionEvent) => {
      newBoardMenu()
    }

    modifyBoardItem.onAction = (event: ActionEvent) => {
      val boardWindow = new Stage()
      boardWindow.initModality(Modality.ApplicationModal)
      boardWindow.initOwner(stage)
      val boardPane = new BorderPane()
      boardPane.setPrefSize(600, 300)

      val header = new Label("Modify current board")
      header.setFont(new Font(20))
      boardPane.top = header

      val inputarea = new VBox
      inputarea.setAlignment(Pos.Center)
      inputarea.children += new Label("Name of the board:")

      val nameField = new TextField
      nameField.maxWidth = 300
      nameField.text = theUser.boards(currentBoardIndex).name
      inputarea.children += nameField

      inputarea.children += new Label("Possible background picture:")
      val fileChoosing = new HBox
      fileChoosing.setAlignment(Pos.Center)
      fileChoosing.spacing = 5
      val openFilechooser = new Button("Choose file")
      val backgroundFileChooser = new FileChooser
      var chosenBackGround: Option[String] = None


      openFilechooser.onAction = (event: ActionEvent) => {
        val file = backgroundFileChooser.showOpenDialog(stage)
        if (file != null) {
          if (fileChoosing.children.size == 2) fileChoosing.children -= fileChoosing.children.last
          chosenBackGround = Some(file.getPath)
          fileChoosing.children += new Label(file.getName)
        } else {
          chosenBackGround = None
          if (fileChoosing.children.size == 2) fileChoosing.children -= fileChoosing.children.last
        }
      }

      fileChoosing.children += openFilechooser
      if (theUser.boards(currentBoardIndex).background.nonEmpty) fileChoosing.children += new Label(theUser.boards(currentBoardIndex).background.get)
      inputarea.children += fileChoosing

      val createButton = new Button("Change board")

      createButton.onAction = (event: ActionEvent) => {
        theUser.boards(currentBoardIndex).name = nameField.getText.trim
        theUser.boards(currentBoardIndex).background = chosenBackGround
        setMenuBar(theUser.boards.size - 1)
        setColumns(theUser.boards.size - 1, currentFilter)
        root.top = menuBar
        scrollpane.content = columns
        boardWindow.close()
      }

      inputarea.children += createButton

      val deleteButton = new Button("Delete board")
      deleteButton.textFill = Color.Red

      deleteButton.onAction = (event: ActionEvent) => {
        if (theUser.boards.size > 1) {
          val areYouSureWindow = new Stage()
          areYouSureWindow.initModality(Modality.ApplicationModal)
          areYouSureWindow.initOwner(stage)

          val areYouSurePane = new BorderPane
          areYouSurePane.setPrefSize(300, 50)

          val header = new Label("Are you sure you want to delete this board?")
          areYouSurePane.top = header

          val buttonArea = new HBox
          buttonArea.setAlignment(Pos.Center)
          buttonArea.spacing = 5

          val cancelButton = new Button("Cancel")
          cancelButton.onAction = (event: ActionEvent) => {
            areYouSureWindow.close()
          }

          val deleteButton = new Button("Delete")
          deleteButton.onAction = (event: ActionEvent) => {
            theUser.boards(currentBoardIndex) = theUser.boards.last
            theUser.boards = theUser.boards.dropRight(1)
            currentBoardIndex = 0
            setColumns(0, currentFilter)
            setMenuBar(0)
            root.top = menuBar
            scrollpane.content = columns
            areYouSureWindow.close()
            boardWindow.close()
          }

          buttonArea.children += cancelButton
          buttonArea.children += deleteButton

          areYouSurePane.center = buttonArea

          BorderPane.setAlignment(header, Pos.TopCenter)
          BorderPane.setAlignment(buttonArea, Pos.Center)

          val areYouSureScene = new Scene(areYouSurePane, 300, 50)
          areYouSureWindow.setScene(areYouSureScene)

          areYouSureWindow.show()
        } else {
          val warningLabel = Label("Cannot delete the only board")
          warningLabel.setTextFill(Color.Red)
          inputarea.children += warningLabel
        }
      }

      inputarea.children += deleteButton

      boardPane.center = inputarea

      BorderPane.setAlignment(header, Pos.TopCenter)
      BorderPane.setAlignment(inputarea, Pos.Center)

      inputarea.spacing = 10

      val boardScene = new Scene(boardPane, 600, 300)
      boardWindow.setScene(boardScene)
      boardWindow.show()
    }


    val changeBoardMenu = new Menu("Change to")
    boardMenu.items = List(newBoardMenuItem, modifyBoardItem, changeBoardMenu)

    for (oneBoard <- theUser.boards) {
      changeBoardMenu.items += new MenuItem(oneBoard.name)
      changeBoardMenu.items.last.onAction = (event: ActionEvent) => {
        setColumns(theUser.boards.indexOf(oneBoard), currentFilter)
        setMenuBar(theUser.boards.indexOf(oneBoard))

        root.top = menuBar
        scrollpane.content = columns
      }
    }

    val newTagItem = new MenuItem("New Tag")
    val deleteTagItem = new MenuItem("Delete Tag")

    // Pop up-window for creating a new Task Category
    newTagItem.onAction = (event: ActionEvent) => {
      val newTagWindow = new Stage()
      newTagWindow.initModality(Modality.ApplicationModal)
      newTagWindow.initOwner(stage)

      val newTagPane = new BorderPane()
      newTagPane.setPrefSize(300, 50)

      newTagPane.top = new Label("Insert new tag name and press Enter")

      val inputBox = new HBox
      inputBox.setAlignment(Pos.Center)

      val nameField = new TextField
      var warningText = new Label("")
      warningText.setTextFill(Color.Red)

      nameField.onAction = (event: ActionEvent) => {
        if (nameField.getText.trim.isEmpty || nameField.getText.trim == "No Tag") {
          warningText = new Label("Insert an appropriate name for a tag!")
          inputBox.children(inputBox.children.size - 1) = warningText
        } else if (theUser.taskCategories.exists(_.name == nameField.getText.trim)) {
          warningText = new Label("No duplicate tags!")
          inputBox.children(inputBox.children.size - 1) = warningText
        } else {
          theUser.newTag(nameField.getText.trim)
          setMenuBar(currentBoardIndex)
          newTagWindow.close
        }
      }

      inputBox.children += nameField
      newTagPane.center = inputBox

      val newTagScene = new Scene(newTagPane, 300, 50)
      newTagWindow.setScene(newTagScene)
      newTagWindow.show()
    }

    deleteTagItem.onAction = (event: ActionEvent) => {
      val deleteTagWindow = new Stage()
      deleteTagWindow.initModality(Modality.ApplicationModal)
      deleteTagWindow.initOwner(stage)

      val deleteTagPane = new BorderPane()
      deleteTagPane.setPrefSize(200, 200)

      deleteTagPane.top = new Label("Select the tag and press delete")
      val inputBox = new VBox
      inputBox.spacing = 5

      val tg = new ToggleGroup
      val tagBoxes: Buffer[RadioButton] = Buffer()

      for (aTag <- theUser.taskCategories) {
        tagBoxes += new RadioButton(aTag.name)
        tagBoxes.last.setToggleGroup(tg)
      }

      inputBox.children = tagBoxes

      val deleteButton = new Button("Delete")

      deleteButton.onAction = (event: ActionEvent) => {
        if (currentFilter.nonEmpty) if (currentFilter.get.name == tagBoxes.find(_.selected.value == true).get.text.value) currentFilter = None
        if (tagBoxes.nonEmpty && tagBoxes.exists(_.selected.value == true)) theUser.deleteTag(tagBoxes.find(_.selected.value == true).get.text.value)
        setMenuBar(boardIndex)
        setColumns(boardIndex, currentFilter)
        root.top = menuBar
        scrollpane.content = columns
        deleteTagWindow.close()
      }

      inputBox.children += deleteButton

      val scrollpaneForDeleteTag = new ScrollPane
      scrollpaneForDeleteTag.content = inputBox
      deleteTagPane.center = scrollpaneForDeleteTag
      val newTagScene = new Scene(deleteTagPane, 200, 200)
      deleteTagWindow.setScene(newTagScene)
      deleteTagWindow.show()
    }

    tagMenu.items = List(newTagItem, deleteTagItem)

    // New Task launches a pop up-window containing instrunctions and textfields for creating a new task.
    val newTaskItem = new MenuItem("New Task")
    newTaskItem.onAction = (event: ActionEvent) => {
      val taskWindow = new Stage()
      taskWindow.initModality(Modality.ApplicationModal)
      taskWindow.initOwner(stage)

      val taskPane = new BorderPane()
      taskPane.setPrefSize(600, 450)

      val header = new Label("Create a new task")
      header.setFont(new Font(20))
      taskPane.top = header

      val inputarea = new VBox
      inputarea.setAlignment(Pos.Center)
      inputarea.spacing = 10

      inputarea.children += new Label("Name of task:")
      val nameinput = new TextField
      nameinput.maxWidth = 300
      inputarea.children += nameinput

      inputarea.children += new Label("Description:")
      val descinput = new TextField
      descinput.maxWidth = 300
      inputarea.children += descinput

      inputarea.children += new Label("Deadline:")
      val deadlineinput = new HBox()
      deadlineinput.setAlignment(Pos.Center)

      val datepicker = new DatePicker()
      var pickedDate: LocalDate = null
      datepicker.onAction = (event: ActionEvent) => {
        pickedDate = datepicker.getValue
        if (deadlineinput.children.size == 2) deadlineinput.children -= deadlineinput.children.last
        deadlineinput.children += new Label(datepicker.getValue.toString)
      }

      deadlineinput.children += datepicker

      inputarea.children += deadlineinput

      inputarea.children += new Label("Write the possible steps of the task:")

      val stepInput = new HBox
      stepInput.setAlignment(Pos.Center)
      stepInput.spacing = 5
      val stepText = new TextField
      val addStep = new Button("+")
      val stepBuffer: Buffer[String] = Buffer()

      var warningLabel = new Label("")
      warningLabel.setTextFill(Color.Red)

      addStep.onAction = (event: ActionEvent) => {
        if (stepText.getText.trim.isEmpty) {
          warningLabel = new Label("Insert correct step description!")
          stepInput.children += warningLabel
        } else if (stepBuffer.contains(stepText.getText.trim)) {
          warningLabel = new Label("No duplicate steps!")
          stepInput.children += warningLabel
        } else {
          if (stepInput.children.size == 3) stepInput.children -= stepInput.children.last
          stepBuffer += stepText.getText.trim
          stepText.clear()
        }
      }

      stepInput.children += stepText
      stepInput.children += addStep
      inputarea.children += stepInput

      inputarea.children += new Label("Possible picture attachment:")

      val fileChoosing = new HBox()
      fileChoosing.setAlignment(Pos.Center)
      fileChoosing.spacing = 5
      val openFilechooser = new Button("Choose file")
      val picFileChooser = new FileChooser
      picFileChooser.extensionFilters.add(new ExtensionFilter("Image files", "*.png", "*.jpg"))
      var chosenAttachment: Option[String] = None

      openFilechooser.onAction = (event: ActionEvent) => {
        val file = picFileChooser.showOpenDialog(stage)
        if (file != null) {
          if (fileChoosing.children.size == 2) fileChoosing.children -= fileChoosing.children.last
          chosenAttachment = Some(file.getPath)
          fileChoosing.children += new Label(file.getName)
        } else {
          chosenAttachment = None
          if (fileChoosing.children.size == 2) fileChoosing.children -= fileChoosing.children.last
        }
      }

      fileChoosing.children += openFilechooser

      inputarea.children += fileChoosing

      inputarea.children += new Label("Possible text file attachment:")

      val txtFileChoosing = new HBox()
      txtFileChoosing.setAlignment(Pos.Center)
      fileChoosing.spacing = 5
      val txtOpenFilechooser = new Button("Choose file")
      val txtFileChooser = new FileChooser
      txtFileChooser.extensionFilters.add(new ExtensionFilter("Text files", "*.txt"))
      var txtChosenAttachment: Option[String] = None

      txtOpenFilechooser.onAction = (event: ActionEvent) => {
        val file = txtFileChooser.showOpenDialog(stage)
        if (file != null) {
          if (txtFileChoosing.children.size == 2) txtFileChoosing.children -= txtFileChoosing.children.last
          txtChosenAttachment = Some(file.getPath)
          txtFileChoosing.children += new Label(file.getName)
        } else {
          txtChosenAttachment = None
          if (txtFileChoosing.children.size == 2) txtFileChoosing.children -= txtFileChoosing.children.last
        }
      }

      txtFileChoosing.children += txtOpenFilechooser

      inputarea.children += txtFileChoosing

      inputarea.children += new Label("Choose the correct column:")

      val tg = new ToggleGroup
      var buttonBuffer: Buffer[RadioButton] = Buffer()
      for (column <- theUser.boards(boardIndex).taskColumns) {
        buttonBuffer += new RadioButton(column.name)
        buttonBuffer.last.setToggleGroup(tg)
      }
      buttonBuffer.foreach(inputarea.children += _)


      val finishedButton = new Button("Create task")
      finishedButton.onAction = (event: ActionEvent) => {
        if (buttonBuffer.exists(_.selected.value == true)) {
          val columnsToChooseFrom = theUser.boards(boardIndex).taskColumns

          def selectedColumn = columnsToChooseFrom.find(_.name == buttonBuffer.find(_.selected.value == true).get.text.value)

          if (pickedDate != null) {
            theUser.boards(boardIndex).newTask(nameinput.getText.trim, descinput.getText.trim, chosenAttachment, txtChosenAttachment, pickedDate, stepBuffer, Buffer(), selectedColumn.getOrElse(theUser.boards(boardIndex).taskColumns.head))
            setColumns(boardIndex, currentFilter)
            scrollpane.content = columns
            taskWindow.close()
          }

        }
      }
      inputarea.children += finishedButton

      val scrollpaneForNewTask = new ScrollPane
      scrollpaneForNewTask.content = inputarea
      scrollpaneForNewTask.setFitToWidth(true)
      taskPane.center = scrollpaneForNewTask

      BorderPane.setAlignment(header, Pos.TopCenter)
      BorderPane.setAlignment(scrollpaneForNewTask, Pos.Center)

      val taskScene = new Scene(taskPane, 600, 450)
      taskWindow.setScene(taskScene)
      taskWindow.show()
    }
    taskMenu.items = List(newTaskItem)

    val noFilterOption = new MenuItem("No Filter")

    filterMenu.items += noFilterOption

    noFilterOption.onAction = (event: ActionEvent) => {
      currentFilter = None
      setColumns(currentBoardIndex, currentFilter)
      scrollpane.content = columns
    }

    for (filter <- theUser.taskCategories) {
      filterMenu.items += new MenuItem(filter.name)
      filterMenu.items.last.onAction = (event: ActionEvent) => {
        currentFilter = Some(filter)
        setColumns(currentBoardIndex, currentFilter)
        scrollpane.content = columns
      }
    }

    archiveMenuItem.onAction = (event: ActionEvent) => {
      val archiveWindow = new Stage()
      archiveWindow.initModality(Modality.ApplicationModal)
      archiveWindow.initOwner(stage)

      val archivePane = new BorderPane()
      archivePane.setMinSize(800, 500)
      archivePane.setPrefSize(800, 500)

      val header = new Label("Task Archive")
      header.setFont(new Font(20))
      archivePane.top = header

      val scrollPaneForArchive = new ScrollPane()
      var taskarea = new HBox

      def setTasks(): Unit = {
        taskarea = new HBox()
        taskarea.spacing = 10
        for (task <- theUser.boards(boardIndex).taskArchive) {
          val contentArea = new VBox()
          contentArea.spacing = 5
          contentArea.children += taskCard(task, boardIndex)
          val returnButton = new Button("Return")
          returnButton.onAction = (event: ActionEvent) => {
            theUser.boards(boardIndex).dearchiveTask(task)
            setTasks()
            scrollPaneForArchive.content = taskarea
            setColumns(boardIndex, currentFilter)
            scrollpane.content = columns
          }
          contentArea.children += returnButton
          contentArea.setAlignment(Pos.Center)
          taskarea.children += contentArea
        }
      }


      setTasks()
      scrollPaneForArchive.content = taskarea
      archivePane.center = scrollPaneForArchive
      BorderPane.setAlignment(header, Pos.TopCenter)

      val archiveScene = new Scene(archivePane, 800, 500)
      archiveWindow.setScene(archiveScene)
      archiveWindow.show()
    }

    newColumnMenuItem.onAction = (event: ActionEvent) => {
      val columnWindow = new Stage()
      columnWindow.initModality(Modality.ApplicationModal)
      columnWindow.initOwner(stage)

      val columnPane = new BorderPane()
      columnPane.setPrefSize(300, 200)

      val header = new Label("Create a new column")
      header.setFont(new Font(20))
      columnPane.top = header

      val contentBox = new VBox()
      contentBox.setAlignment(Pos.Center)
      contentBox.spacing = 10
      contentBox.children += new Label("Insert a column name:")
      val nameField = new TextField()
      contentBox.children += nameField
      val createButton = new Button("Create")
      contentBox.children += createButton

      createButton.onAction = (event: ActionEvent) => {
        theUser.boards(currentBoardIndex).newColumn(nameField.getText.trim)
        setColumns(currentBoardIndex, currentFilter)
        setMenuBar(currentBoardIndex)
        root.top = menuBar
        scrollpane.content = columns
        columnWindow.close()
      }

      columnPane.center = contentBox
      BorderPane.setAlignment(header, Pos.TopCenter)
      val columnScene = new Scene(columnPane, 300, 200)
      columnWindow.setScene(columnScene)
      columnWindow.show()
    }

    menuBar.menus = List(fileMenu, boardMenu, taskMenu, tagMenu, archiveMenu, filterMenu, columnMenu)
  }

  // ColumnBox where we list the Boards columns
  var columns = new VBox
  var columnHeaders = new HBox
  var columnBox = new HBox

  // Method for creating a task card for a task
  def taskCard(aTask: Task, boardIndex: Int): StackPane = {

    // The base for a taskCard is it's shape which is formed first

    var stackPane = new StackPane
    stackPane.minWidth = 215

    var rectangle = new Rectangle
    rectangle.setWidth(200)
    rectangle.setHeight(250)
    rectangle.setArcWidth(30)
    rectangle.setArcHeight(20)
    rectangle.fill = Color.PaleTurquoise
    stackPane.getChildren.add(rectangle)

    // After the shape, comes the information

    var borderPane = new BorderPane
    borderPane.setMaxSize(rectangle.getWidth, rectangle.getHeight)
    var header = new Label(aTask.header)
    if (aTask.tag.nonEmpty) header = new Label(aTask.header + "\nTag: " + aTask.tag.get.name)
    header.setStyle("-fx-font-weight: bold")
    borderPane.top = header
    val modifyTaskItem = new MenuItem("Modify task")
    modifyTaskItem.onAction = (event: ActionEvent) => {
      val taskWindow = new Stage()
      taskWindow.initModality(Modality.ApplicationModal)
      taskWindow.initOwner(stage)
      val taskPane = new BorderPane()
      taskPane.setPrefSize(600, 350)

      val header = new Label("Modify task data")
      header.setFont(new Font(20))
      taskPane.top = header

      val inputarea = new VBox
      inputarea.setAlignment(Pos.Center)

      inputarea.children += new Label("Name of the task:")
      val nameField = new TextField
      nameField.maxWidth = 300
      nameField.text = aTask.header
      inputarea.children += nameField

      inputarea.children += new Label("Description of the task:")   
      val descField = new TextField
      descField.maxWidth = 300
      descField.text = aTask.desc
      inputarea.children += descField

      inputarea.children += new Label("Possible picture attachment:")
      val fileChoosing = new HBox
      fileChoosing.setAlignment(Pos.Center)
      fileChoosing.spacing = 5
      val openFilechooser = new Button("Choose file")
      val picFileChooser = new FileChooser
      picFileChooser.extensionFilters.add(new ExtensionFilter("Image files", "*.png", "*.jpg"))
      var chosenAttachment = aTask.picAttachment


      openFilechooser.onAction = (event: ActionEvent) => {
        val file = picFileChooser.showOpenDialog(stage)
        if (file != null) {
          if (fileChoosing.children.size == 2) fileChoosing.children -= fileChoosing.children.last
          chosenAttachment = Some(file.getPath)
          fileChoosing.children += new Label(file.getName)
        } else {
          chosenAttachment = None
          if (fileChoosing.children.size == 2) fileChoosing.children -= fileChoosing.children.last
        }
      }

      fileChoosing.children += openFilechooser

      if (aTask.picAttachment.nonEmpty) fileChoosing.children += new Label(aTask.picAttachment.get)


      inputarea.children += fileChoosing

      inputarea.children += new Label("Possible text file attachment:")

      val txtFileChoosing = new HBox()
      txtFileChoosing.setAlignment(Pos.Center)
      txtFileChoosing.spacing = 5
      val txtOpenFilechooser = new Button("Choose file")
      val txtFileChooser = new FileChooser()
      txtFileChooser.extensionFilters.add(new ExtensionFilter("Text files", "*.txt"))
      var txtChosenAttachment = aTask.txtAttachment

      txtOpenFilechooser.onAction = (event: ActionEvent) => {
        val file = txtFileChooser.showOpenDialog(stage)
        if (file != null) {
          if (txtFileChoosing.children.size == 2) txtFileChoosing.children -= txtFileChoosing.children.last
          txtChosenAttachment = Some(file.getPath)
          txtFileChoosing.children += new Label(file.getName)
        } else {
          txtChosenAttachment = None
          if (txtFileChoosing.children.size == 2) txtFileChoosing.children -= txtFileChoosing.children.last
        }
      }

      txtFileChoosing.children += txtOpenFilechooser

      if (aTask.txtAttachment.nonEmpty) txtFileChoosing.children += new Label(aTask.txtAttachment.get)

      inputarea.children += txtFileChoosing

      val changeButton = new Button("Change task")

      changeButton.onAction = (event: ActionEvent) => {
        aTask.header = nameField.getText.trim
        aTask.desc = descField.getText.trim
        aTask.picAttachment = chosenAttachment
        aTask.txtAttachment = txtChosenAttachment
        setColumns(currentBoardIndex, currentFilter)
        scrollpane.content = columns
        taskWindow.close()
      }

      inputarea.children += changeButton

      taskPane.center = inputarea
      BorderPane.setAlignment(header, Pos.TopCenter)
      BorderPane.setAlignment(inputarea, Pos.Center)
      inputarea.spacing = 10
      val taskScene = new Scene(taskPane, 600, 350)
      taskWindow.setScene(taskScene)
      taskWindow.show()
    }


    BorderPane.setAlignment(header, Pos.TopCenter)

    val bottomBox = new VBox

    var picIcon: Button = null
    if (aTask.picAttachment.nonEmpty) {
      val imagestream = new FileInputStream("./icons/picture icon")
      val iconImage = new Image(imagestream, 15, 15, false, false)
      val icon = new ImageView(iconImage)

      val iconButton = new Button()

      iconButton.onAction = (event: ActionEvent) => {
        val imageWindow = new Stage()
        imageWindow.initModality(Modality.ApplicationModal)
        imageWindow.initOwner(stage)

        val imagePane = new BorderPane()
        imagePane.setPrefSize(800, 600)

        val imagestream = new FileInputStream(aTask.picAttachment.get)
        val image = new Image(imagestream, 800, 600, false, false)
        val imageview = new ImageView(image)
        imageview.setFitWidth(800)
        imageview.setPreserveRatio(true)

        imagePane.center = imageview
        BorderPane.setAlignment(imageview, Pos.Center)

        val imageScene = new Scene(imagePane, 800, 600)
        imageWindow.setScene(imageScene)
        imageWindow.show()
      }
      icon.setFitWidth(15)
      icon.setPreserveRatio(true)
      iconButton.setGraphic(icon)
      picIcon = iconButton
    }

    var txtIcon: Button = null
    if (aTask.txtAttachment.nonEmpty) {
      val imagestream = new FileInputStream("./icons/text file icon")
      val iconImage = new Image(imagestream, 15, 15, false, false)
      val icon = new ImageView(iconImage)

      val iconButton = new Button()
      iconButton.onAction = (event: ActionEvent) => {
        val textWindow = new Stage()
        textWindow.initModality(Modality.ApplicationModal)
        textWindow.initOwner(stage)
        val textPane = new ScrollPane
        textPane.setPrefSize(600, 848)
        var theText = ""
        val bufferedSource = Source.fromFile(aTask.txtAttachment.get)
        for (line <- bufferedSource.getLines) {
          theText = theText + "\n" + line
        }
        textPane.content = new Label(theText)

        val textScene = new Scene(textPane, 600, 848)
        textWindow.setScene(textScene)
        textWindow.show()
      }
      icon.setFitWidth(15)
      icon.setPreserveRatio(true)
      iconButton.setGraphic(icon)
      txtIcon = iconButton
    }

    val attachmentButtons = new HBox()
    attachmentButtons.spacing = 5
    attachmentButtons.setAlignment(Pos.Center)

    if (aTask.picAttachment.nonEmpty && aTask.txtAttachment.nonEmpty) {
      attachmentButtons.children = List(picIcon, txtIcon)
    } else if (aTask.picAttachment.nonEmpty) {
      attachmentButtons.children = List(picIcon)
    } else if (aTask.txtAttachment.nonEmpty) {
      attachmentButtons.children = List(txtIcon)
    }

    val deadline = new Label("Deadline: " + aTask.deadline.toString)
    var daysLeft = new Label()
    if (LocalDate.now.getYear != aTask.deadline.getYear) {
      daysLeft = new Label(aTask.deadline.compareTo(LocalDate.now).toString + " years left")
    } else if (LocalDate.now.getMonth != aTask.deadline.getMonth) {
      daysLeft = new Label(aTask.deadline.compareTo(LocalDate.now).toString + " months left")
    } else daysLeft = new Label(aTask.deadline.compareTo(LocalDate.now).toString + " days left")

    if (aTask.deadline.compareTo(LocalDate.now) < 0) {
      daysLeft = new Label(daysLeft.getText.dropRight(4).drop(1) + "ago")
      daysLeft.setTextFill(Color.Red)
      daysLeft.setStyle("color: red;")
    } else if (aTask.deadline.compareTo(LocalDate.now) == 0) {
      daysLeft.setTextFill(Color.Red)
      daysLeft.setStyle("color: red;")
    }

    val progressBar = new ProgressBar
    progressBar.setProgress(aTask.calculateProgress)
    val progress = new Label("Completion: " + (aTask.calculateProgress * 100).toInt.toString + "%")

    if (aTask.possibleSteps.nonEmpty && (aTask.picAttachment.nonEmpty || aTask.txtAttachment.nonEmpty)) {
      bottomBox.children = List(attachmentButtons, deadline, daysLeft, progressBar, progress)
    } else if ((aTask.picAttachment.nonEmpty || aTask.txtAttachment.nonEmpty)) {
      bottomBox.children = List(attachmentButtons, deadline, daysLeft)
    } else if (aTask.possibleSteps.nonEmpty) {
      bottomBox.children = List(deadline, daysLeft, progressBar, progress)
    } else bottomBox.children = List(deadline, daysLeft)


    borderPane.bottom = bottomBox
    bottomBox.alignment = Pos.Center
    stackPane.getChildren.add(borderPane)

    val moveMenu = new Menu("Move to")
    for (oneColumn <- theUser.boards(boardIndex).taskColumns) {
      moveMenu.items += new MenuItem(oneColumn.name)
      moveMenu.items.last.onAction = (event: ActionEvent) => {
        theUser.boards(boardIndex).moveTask(aTask, oneColumn)
        setColumns(boardIndex, currentFilter)
        scrollpane.content = columns
      }
    }

    val archiveItem = new MenuItem("Archive")
    archiveItem.onAction = (event: ActionEvent) => {
      theUser.boards(boardIndex).archiveTask(aTask)
      setColumns(boardIndex, currentFilter)
      scrollpane.content = columns
    }
    val deleteItem = new MenuItem("Delete")
    deleteItem.onAction = (event: ActionEvent) => {
      theUser.boards(boardIndex).removeTask(aTask)
      setColumns(boardIndex, currentFilter)
      scrollpane.content = columns
    }

    val addATagItem = new MenuItem("Add a tag")

    addATagItem.onAction = (event: ActionEvent) => {
      val addATagWindow = new Stage()
      addATagWindow.initModality(Modality.ApplicationModal)
      addATagWindow.initOwner(stage)

      val addATagPane = new BorderPane()
      addATagPane.setPrefSize(200, 200)
      addATagPane.top = new Label("Select a tag for the task")

      val inputBox = new VBox
      inputBox.spacing = 5

      // Radio buttons and togglegroup are used for attaining user input from a set of options
      val tg = new ToggleGroup
      val tagBoxes: Buffer[RadioButton] = Buffer()

      for (aTag <- theUser.taskCategories) {
        tagBoxes += new RadioButton(aTag.name)
        tagBoxes.last.setToggleGroup(tg)
      }


      inputBox.children = tagBoxes

      val noTag = new RadioButton("No Tag")
      noTag.setToggleGroup(tg)
      inputBox.children += noTag

      val tagButton = new Button("Tag")


      def getSelectedTag = if (tagBoxes.exists(_.selected.value == true)) tagBoxes.find(_.selected.value == true).get.text.value else "No Tag"

      def selectedTag = theUser.taskCategories.find(_.name == getSelectedTag)


      tagButton.onAction = (event: ActionEvent) => {
        aTask.tag = selectedTag
        selectedTag.getOrElse(new TaskCategory("", Buffer())).contents += aTask
        setColumns(boardIndex, currentFilter)
        root.top = menuBar
        scrollpane.content = columns
        addATagWindow.close()
      }

      inputBox.children += tagButton

      addATagPane.center = inputBox
      val addATagScene = new Scene(addATagPane, 200, 200)
      addATagWindow.setScene(addATagScene)
      addATagWindow.show()
    }

    // A ContextMenu is used by right clicking the node it's applied to
    val taskcardMenu = new ContextMenu(modifyTaskItem, moveMenu, archiveItem, deleteItem, addATagItem)

    header.contextMenu = taskcardMenu

    var centerBox = new VBox
    centerBox.spacing = 10
    val desc = new Label(aTask.desc)
    centerBox.children += desc
    centerBox.margin = Insets.apply(0,0,10,10)

    var stepBoxes: Buffer[CheckBox] = Buffer()
    for (x <- aTask.possibleSteps.indices) {
      stepBoxes += new CheckBox(aTask.possibleSteps(x))
      stepBoxes(x).setSelected(aTask.completedSteps.contains(aTask.possibleSteps(x)))
      stepBoxes(x).onAction = (event: ActionEvent) => {
        aTask.stepCompletion(stepBoxes(x).text.value)
        setColumns(boardIndex, currentFilter)
        scrollpane.content = columns
      }
      centerBox.children += stepBoxes(x)
    }
    borderPane.center = centerBox

    stackPane
  }

  // Here we create the columns. Columnheaders and column areas with task cards are made separately and added to columns.
  def setColumns(boardIndex: Int, possibleFilter: Option[TaskCategory]): Unit = {
    columnBox = new HBox
    columnHeaders = new HBox
    columns = new VBox
    // columnBox.spacing = 10

    // The VBoxes for the columns and adding tasks inside it
    for (oneColumn <- theUser.boards(boardIndex).taskColumns) {
      val column = new VBox

      // Building the columnheaders with two rectangles and a label.
      val stackPane = new StackPane
      stackPane.setMaxSize(215, 250)
      val rectangle1 = new Rectangle
      rectangle1.setWidth(215)
      rectangle1.setHeight(80)
      rectangle1.fill = Color.Black
      val rectangle2 = new Rectangle
      rectangle2.setWidth(212)
      rectangle2.setHeight(76)
      rectangle2.fill = Color.White
      val label = new Label(oneColumn.name)

      // Right-click option for changing a columns name
      val changeNameItem = new MenuItem("Change column name")
      changeNameItem.onAction = (event: ActionEvent) => {
        val changeNameWindow = new Stage()
        changeNameWindow.initModality(Modality.ApplicationModal)
        changeNameWindow.initOwner(stage)
        val changeNamePane = new BorderPane()
        changeNamePane.setPrefSize(200, 50)
        changeNamePane.top = new Label("Insert new name and press Enter")
        val nameField = new TextField
        nameField.onAction = (event: ActionEvent) => {
          oneColumn.name = nameField.getText
          setColumns(boardIndex, currentFilter)
          scrollpane.content = columns
          changeNameWindow.close
        }
        changeNamePane.center = nameField
        val changeNameScene = new Scene(changeNamePane, 200, 50)
        changeNameWindow.setScene(changeNameScene)
        changeNameWindow.show()
      }

      // Right-click option for deleting a column
      val deleteItem = new MenuItem("Delete")
      deleteItem.onAction = (event: ActionEvent) => {
        val deleteWindow = new Stage()
        deleteWindow.initModality(Modality.ApplicationModal)
        deleteWindow.initOwner(stage)
        val deletePane = new BorderPane()
        deletePane.setPrefSize(200, 50)
        deletePane.top = new Label("Are you sure?")
        BorderPane.setAlignment(deletePane.top.get, Pos.Center)
        val buttonArea = new HBox()
        buttonArea.setAlignment(Pos.Center)
        buttonArea.spacing = 20
        val deleteButton = new Button("Delete")
        deleteButton.setTextFill(Color.Red)
        val cancelButton = new Button("Cancel")
        deleteButton.onAction = (event: ActionEvent) => {
          theUser.boards(currentBoardIndex).deleteColumn(oneColumn)
          setColumns(currentBoardIndex, currentFilter)
          scrollpane.content = columns
          deleteWindow.close
        }
        cancelButton.onAction = (event: ActionEvent) => deleteWindow.close
        buttonArea.children = List(deleteButton, cancelButton)
        deletePane.center = buttonArea
        val deleteScene = new Scene(deletePane, 200, 50)
        deleteWindow.setScene(deleteScene)
        deleteWindow.show()
      }

      label.contextMenu = new ContextMenu(changeNameItem, deleteItem)

      // Stacking the components to make the columnheaders
      stackPane.children = List(rectangle1, rectangle2, label)
      columnHeaders.children += stackPane

      column.spacing = 10

      // Column areas with task cards is made here. Task Cards are built with the taskCard function. If there is a filter for a TaskCategory, only tasks with that certaing tag, come up.
      if (currentFilter.isEmpty) oneColumn.contents.foreach(column.children += taskCard(_, boardIndex)) else oneColumn.contents.filter(_.tag == currentFilter).foreach(column.children += taskCard(_, boardIndex))

      // If a column is empty. We reserve it's area by adding an empty stackpane to it.
      if (oneColumn.contents.isEmpty) {
        val emptyPane = new StackPane
        emptyPane.minWidth = 215
        column.children += emptyPane
      }

      columnBox.children += column

    }

    columns.spacing = 10
    columns.children = List(columnHeaders, columnBox)

    // If a board has a background, it is fetched and applied under other content using a StackPane
    if (theUser.boards(boardIndex).background.nonEmpty) {
      val imagestream = new FileInputStream(theUser.boards(boardIndex).background.get)
      val backgroundImage = new Image(imagestream, columnBox.width.toDouble, columnBox.height.toDouble, false, false)
      val background = new ImageView(backgroundImage)
      background.setFitWidth(1170)
      background.setPreserveRatio(true)
      val newColumnBox = new StackPane
      newColumnBox.children += background
      newColumnBox.children += columnBox
      columns.children = List(columnHeaders, newColumnBox)
    }
    currentBoardIndex = boardIndex
  }

  // If there is no user data, the program asks the user to create a user and the first board
  if (theUser.name == "emptyUser") {
    val welcomeWindow = new Stage()
    welcomeWindow.initModality(Modality.ApplicationModal)
    welcomeWindow.initOwner(stage)
    val welcomePane = new BorderPane()
    welcomePane.setPrefSize(400, 400)
    val header = new Label("         Welcome to kanban board application\nCreate a username and the name of your first board")
    header.setFont(new Font(15))
    welcomePane.top = header
    val contentBox = new VBox()
    contentBox.spacing = 10
    contentBox.setAlignment(Pos.Center)
    val usernameBox = new HBox()
    usernameBox.setAlignment(Pos.Center)
    usernameBox.spacing = 5
    usernameBox.children += new Label("Username:")
    val usernameField = new TextField
    usernameBox.children += usernameField
    contentBox.children += usernameBox
    val doneButton = new Button("Done")

    // After the needed user data is given, the new board menu is launched
    doneButton.onAction = (event: ActionEvent) => {
      theUser = new User(usernameField.getText.trim, Buffer(), Buffer(), 0)

      welcomeWindow.close()

      newBoardMenu()
    }

    contentBox.children += doneButton
    contentBox.setAlignment(Pos.Center)

    welcomePane.center = contentBox
    BorderPane.setAlignment(header, Pos.TopCenter)

    val welcomeScene = new Scene(welcomePane, 400, 400)
    welcomeWindow.setScene(welcomeScene)
    welcomeWindow.show()
  }

  // The program opens the users latest used board when opened
  if (theUser.name != "emptyUser") {
    setMenuBar(theUser.latestBoardIndex)
    setColumns(theUser.latestBoardIndex, currentFilter)

    root.top = menuBar

    scrollpane.content = columns
    root.center = scrollpane
  }

}
