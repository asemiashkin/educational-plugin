package com.jetbrains.edu.learning

import com.intellij.lang.LanguageExtensionPoint
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.components.impl.ComponentManagerImpl
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockManager
import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.IOException

abstract class EduTestCase : LightPlatformCodeInsightFixtureTestCase() {
  private lateinit var myManager: FileEditorManagerImpl
  private lateinit var myOldManager: FileEditorManager
  private lateinit var myOldDockContainers: Set<DockContainer>


  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    registerPlainTextConfigurator()
    createCourse()

    val dockManager = DockManager.getInstance(myFixture.project)
    myOldDockContainers = dockManager.containers
    myManager = FileEditorManagerImpl(myFixture.project, dockManager)
    myOldManager = (myFixture.project as ComponentManagerImpl).registerComponentInstance<FileEditorManager>(FileEditorManager::class.java, myManager)
    (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
  }

  override fun tearDown() {
    try {
      DockManager.getInstance(myFixture.project).containers
          .filterNot { myOldDockContainers.contains(it) }
          .forEach { Disposer.dispose(it) }

      (myFixture.project as ComponentManagerImpl).registerComponentInstance(FileEditorManager::class.java, myOldManager)
      myManager.closeAllFiles()

      EditorHistoryManager.getInstance(myFixture.project).files.forEach {
        EditorHistoryManager.getInstance(myFixture.project).removeFile(it)
      }

      (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
    }
    finally {
      super.tearDown()
    }
  }

  @Throws(IOException::class)
  protected open fun createCourse() {
  }

  @Throws(IOException::class)
  protected fun createLesson(index: Int, taskCount: Int): Lesson {
    val lesson = Lesson()
    lesson.name = "lesson" + index
    (1..taskCount)
        .map { createTask(index, it) }
        .forEach { lesson.addTask(it) }
    lesson.index = index
    return lesson
  }

  @Throws(IOException::class) private fun createTask(lessonIndex: Int, taskIndex: Int): Task {
    val task = EduTask()
    task.name = "task" + taskIndex
    task.index = taskIndex
    createTaskFile(lessonIndex, task, "taskFile$taskIndex.txt")
    return task
  }

  @Throws(IOException::class)
  private fun createTaskFile(lessonIndex: Int, task: Task, taskFilePath: String) {
    val taskFile = TaskFile()
    taskFile.task = task
    task.getTaskFiles().put(taskFilePath, taskFile)
    taskFile.name = taskFilePath

    val fileName = "lesson" + lessonIndex + "/" + task.name + "/" + taskFilePath
    val file = myFixture.findFileInTempDir(fileName)
    taskFile.text = VfsUtilCore.loadText(file)

    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
    val document = FileDocumentManager.getInstance().getDocument(file)
    for (placeholder in CCTestCase.getPlaceholders(document, true)) {
      taskFile.addAnswerPlaceholder(placeholder)
    }
    taskFile.sortAnswerPlaceholders()
  }

  protected fun configureByTaskFile(lessonIndex: Int, taskIndex: Int, taskFileName: String) {
    val fileName = "lesson$lessonIndex/task$taskIndex/$taskFileName"
    val file = myFixture.findFileInTempDir(fileName)
    myFixture.configureFromExistingVirtualFile(file)
    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
  }

  override fun getTestDataPath(): String {
    return "testData"
  }

  private fun registerPlainTextConfigurator() {
    val extension = LanguageExtensionPoint<Annotator>()
    extension.language = PlainTextLanguage.INSTANCE.id
    extension.implementationClass = PlainTextConfigurator::class.java.name
    PlatformTestUtil.registerExtension(
      ExtensionPointName.create(EduConfigurator.EP_NAME), extension, myFixture.testRootDisposable)
  }

  class PlainTextConfigurator : EduConfigurator<Unit> {
    override fun getCourseBuilder() = object : EduCourseBuilder<Unit> {
      override fun createTaskContent(project: Project, task: Task, parentDirectory: VirtualFile, course: Course): VirtualFile? = null
      override fun getLanguageSettings(): EduCourseBuilder.LanguageSettings<Unit> = EduCourseBuilder.LanguageSettings { Unit }
    }

    override fun getTestFileName() = "test.txt"

    override fun excludeFromArchive(name: String) = false

    override fun getTaskCheckerProvider() = TaskCheckerProvider{ task, project -> object: TaskChecker<EduTask>(task, project) {
      override fun check(): CheckResult {
        return CheckResult(CheckStatus.Solved, "")
      }
    }}
  }

}


