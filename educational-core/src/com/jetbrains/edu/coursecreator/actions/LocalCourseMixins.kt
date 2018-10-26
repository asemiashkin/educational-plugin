@file:JvmName("CourseYamlUtil")

package com.jetbrains.edu.coursecreator.actions

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.lang.Language
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.serialization.SerializationUtils
import java.util.*

private const val VERSION = "version"
private const val TITLE = "title"
private const val LANGUAGE = "language"
private const val SUMMARY = "summary"
private const val PROGRAMMING_LANGUAGE = "programming_language"
private const val ITEMS = "items"
private const val NAME = "name"
private const val TASK_LIST = "task_list"
private const val TASK_FILES = "task_files"
private const val TEST_FILES = "test_files"
private const val ADDITIONAL_FILES = "additional_files"
private const val TASK_TYPE = "task_type"
private const val DESCRIPTION_TEXT = "description_text"
private const val DESCRIPTION_FORMAT = "description_format"
private const val TEXT = "text"
private const val PLACEHOLDERS = "placeholders"
private const val TYPE = "type"

@Suppress("unused", "UNUSED_PARAMETER") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder(VERSION, TITLE, SUMMARY, PROGRAMMING_LANGUAGE, LANGUAGE, ITEMS)
abstract class LocalCourseMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(SUMMARY)
  private lateinit var description: String

  @JsonSerialize(converter = ProgrammingLanguageConverter::class)
  @JsonProperty(PROGRAMMING_LANGUAGE)
  private lateinit var myProgrammingLanguage: String

  @JsonSerialize(converter = LanguageConverter::class)
  @JsonProperty(LANGUAGE)
  private lateinit var myLanguageCode: String

  @JsonProperty(ITEMS)
  private lateinit var items: List<StudyItem>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class LocalSectionMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(ITEMS)
  private lateinit var items: List<StudyItem>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class LocalLessonMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(TASK_LIST)
  private lateinit var taskList: List<Task>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class LocalTaskMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(TASK_FILES)
  private lateinit var myTaskFiles: MutableMap<String, TaskFile>

  @JsonProperty(TEST_FILES)
  private lateinit var testsText: MutableMap<String, String>

  @JsonProperty(DESCRIPTION_TEXT)
  private lateinit var descriptionText: String

  @JsonProperty(DESCRIPTION_FORMAT)
  private lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty(ADDITIONAL_FILES)
  protected lateinit var additionalFiles: MutableMap<String, AdditionalFile>
}

@Suppress("UNUSED_PARAMETER", "unused") // used for json serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
abstract class LocalTaskFileMixin {
  @JsonProperty(NAME)
  private lateinit var myName: String

  @JsonProperty(TEXT)
  private lateinit var _text: String

  @JsonProperty(PLACEHOLDERS)
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>
}

private class ProgrammingLanguageConverter : StdConverter<String, String>() {
  override fun convert(languageId: String): String = Language.findLanguageByID(languageId)!!.displayName
}

private class LanguageConverter : StdConverter<String, String>() {
  override fun convert(languageCode: String): String = Locale(languageCode).displayName
}

class TaskSerializer : JsonSerializer<Task>() {
  override fun serialize(task: Task, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeStartObject()
    generator.writeObjectField(TASK_TYPE, task.taskType)
    val serializer = getJsonSerializer(provider, Task::class.java)
    serializer.unwrappingSerializer(null).serialize(task, generator, provider)
    generator.writeEndObject()
  }
}

class LessonSerializer : JsonSerializer<Lesson>() {
  override fun serialize(lesson: Lesson, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeStartObject()
    generator.writeObjectField(TYPE, itemType(lesson))
    val serializer = getJsonSerializer(provider, Lesson::class.java)
    serializer.unwrappingSerializer(null).serialize(lesson, generator, provider)
    generator.writeEndObject()
  }

  private fun itemType(lesson: Lesson): String {
    var itemType = EduNames.LESSON
    if (lesson is FrameworkLesson) {
      itemType = SerializationUtils.Json.FRAMEWORK_TYPE
    }
    else if (lesson is Section) {
      itemType = EduNames.SECTION
    }
    return itemType
  }
}

class CourseSerializer : JsonSerializer<Course>() {
  override fun serialize(course: Course, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeStartObject()
    generator.writeObjectField(VERSION, JSON_FORMAT_VERSION)
    val serializer = getJsonSerializer(provider, Course::class.java)
    serializer.unwrappingSerializer(null).serialize(course, generator, provider)
    generator.writeEndObject()
  }
}

private fun getJsonSerializer(provider: SerializerProvider, itemClass: Class<out StudyItem>): JsonSerializer<Any> {
  val javaType = provider.constructType(itemClass)
  val beanDesc: BeanDescription = provider.config.introspect(javaType)
  return BeanSerializerFactory.instance.findBeanSerializer(provider, javaType, beanDesc)
}