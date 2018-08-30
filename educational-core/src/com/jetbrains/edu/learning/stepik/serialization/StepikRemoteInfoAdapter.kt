package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikWrappers
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikCourseRemoteInfo
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikLessonRemoteInfo
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikSectionRemoteInfo
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikTaskRemoteInfo
import org.fest.util.Lists
import java.lang.reflect.Type
import java.util.*

class StepikRemoteInfoAdapter : JsonDeserializer<StepikCourse>, JsonSerializer<StepikCourse> {
  private val IS_PUBLIC = "is_public"
  private val IS_ADAPTIVE = "is_adaptive"
  private val IS_IDEA_COMPATIBLE = "is_idea_compatible"
  private val ID = "id"
  private val UPDATE_DATE = "update_date"
  private val SECTIONS = "sections"
  private val INSTRUCTORS = "instructors"

  override fun serialize(course: StepikCourse?, type: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
    val tree = gson.toJsonTree(course)
    val jsonObject = tree.asJsonObject
    val remoteInfo = course?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikCourseRemoteInfo

    jsonObject.add(IS_PUBLIC, JsonPrimitive(stepikRemoteInfo?.isPublic ?: false))
    jsonObject.add(IS_ADAPTIVE, JsonPrimitive(stepikRemoteInfo?.isAdaptive ?: false))
    jsonObject.add(IS_IDEA_COMPATIBLE, JsonPrimitive(stepikRemoteInfo?.isIdeaCompatible ?: false))
    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.id ?: 0))
    jsonObject.add(SECTIONS, gson.toJsonTree(stepikRemoteInfo?.sectionIds ?: Lists.emptyList<Int>()))
    jsonObject.add(INSTRUCTORS, gson.toJsonTree(stepikRemoteInfo?.instructors ?: Lists.emptyList<Int>()))

    val updateDate = stepikRemoteInfo?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }
    return jsonObject
  }

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): StepikCourse {
    val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(StepikWrappers.StepOptions::class.java, StepikStepOptionsAdapter())
      .registerTypeAdapter(Lesson::class.java, StepikLessonRemoteInfoAdapter())
      .registerTypeAdapter(StepikWrappers.Reply::class.java, StepikReplyAdapter())
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()

    val course = gson.fromJson(json, StepikCourse::class.java)
    deserializeRemoteInfo(json, course, gson)
    return course
  }

  private fun deserializeRemoteInfo(json: JsonElement, course: StepikCourse, gson: Gson) {
    val jsonObject = json.asJsonObject
    val remoteInfo = StepikCourseRemoteInfo()
    val isPublic = jsonObject.get(IS_PUBLIC)?.asBoolean ?: false
    val isAdaptive = jsonObject.get(IS_ADAPTIVE)?.asBoolean ?: false
    val isCompatible = jsonObject.get(IS_IDEA_COMPATIBLE)?.asBoolean ?: false
    val id = jsonObject.get(ID)?.asInt ?: 0

    val jsonSections = jsonObject.get(SECTIONS)
    val jsonInstructors = jsonObject.get(INSTRUCTORS)
    val jsonUpdateDate = jsonObject.get(UPDATE_DATE)
    val sections = if (jsonSections != null) gson.fromJson<List<Int>>(jsonSections, object: TypeToken<List<Int>>(){}.type) else listOf()
    val instructors = if (jsonInstructors != null) gson.fromJson<List<Int>>(jsonInstructors, object: TypeToken<List<Int>>(){}.type) else listOf()
    val updateDate = if (jsonUpdateDate != null) gson.fromJson(jsonUpdateDate, Date::class.java) else Date(0)

    remoteInfo.isPublic = isPublic
    remoteInfo.isAdaptive = isAdaptive
    remoteInfo.isIdeaCompatible = isCompatible
    remoteInfo.id = id
    remoteInfo.sectionIds = sections
    remoteInfo.instructors = instructors
    remoteInfo.updateDate = updateDate

    course.remoteInfo = remoteInfo
  }
}

class StepikSectionRemoteInfoAdapter : JsonDeserializer<Section>, JsonSerializer<Section> {
  private val ID = "id"
  private val COURSE_ID = "course"
  private val POSITION = "position"
  private val UNITS = "units"
  private val UPDATE_DATE = "update_date"

  override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Section {
    val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(StepikWrappers.StepOptions::class.java, StepikStepOptionsAdapter())
      .registerTypeAdapter(Lesson::class.java, StepikLessonAdapter())
      .registerTypeAdapter(StepikWrappers.Reply::class.java, StepikReplyAdapter())
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()

    val section = gson.fromJson(json, Section::class.java)
    deserializeRemoteInfo(gson, section, json)
    return section
  }

  private fun deserializeRemoteInfo(gson: Gson, section: Section, json: JsonElement): Section? {
    val jsonObject = json.asJsonObject

    val remoteInfo = StepikSectionRemoteInfo()
    val id = jsonObject.get(ID)?.asInt ?: 0
    val courseId = jsonObject.get(COURSE_ID)?.asInt ?: 0
    val position = jsonObject.get(POSITION)?.asInt ?: 0
    val jsonUpdateDate = jsonObject.get(UPDATE_DATE)
    val jsonUnits = jsonObject.get(UNITS)
    val updateDate = if (jsonUpdateDate != null) gson.fromJson(jsonUpdateDate, Date::class.java) else Date(0)
    val units = if (jsonUnits != null) gson.fromJson<List<Int>>(jsonUnits, object: TypeToken<List<Int>>(){}.type) else listOf()

    remoteInfo.id = id
    remoteInfo.courseId = courseId
    remoteInfo.position = position
    remoteInfo.updateDate = updateDate
    remoteInfo.units = units

    section.remoteInfo = remoteInfo
    return section
  }

  override fun serialize(section: Section?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
    val tree = gson.toJsonTree(section)
    val jsonObject = tree.asJsonObject
    val remoteInfo = section?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikSectionRemoteInfo

    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.id ?: 0))
    jsonObject.add(COURSE_ID, JsonPrimitive(stepikRemoteInfo?.courseId ?: 0))
    jsonObject.add(POSITION, JsonPrimitive(stepikRemoteInfo?.position ?: 0))
    jsonObject.add(UNITS, gson.toJsonTree(stepikRemoteInfo?.units ?: Lists.emptyList<Int>()))

    val updateDate = stepikRemoteInfo?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }
    return jsonObject
  }
}

class StepikLessonRemoteInfoAdapter : JsonDeserializer<Lesson>, JsonSerializer<Lesson> {
  private val ID = "id"
  private val UPDATE_DATE = "update_date"
  private val UNIT_ID = "unit_id"
  private val IS_PUBLIC = "is_public"
  private val STEPS = "steps"

  override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Lesson {
    val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(StepikWrappers.StepOptions::class.java, StepikStepOptionsAdapter())
      .registerTypeAdapter(Lesson::class.java, StepikLessonAdapter())
      .registerTypeAdapter(StepikWrappers.Reply::class.java, StepikReplyAdapter())
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()

    val lesson = gson.fromJson(json, Lesson::class.java)
    deserializeRemoteInfo(gson, lesson, json)
    return lesson
  }

  private fun deserializeRemoteInfo(gson: Gson, lesson: Lesson, json: JsonElement): Lesson? {
    val jsonObject = json.asJsonObject

    val remoteInfo = StepikLessonRemoteInfo()

    val id = jsonObject.get(ID)?.asInt ?: 0
    val unitId = jsonObject.get(UNIT_ID)?.asInt ?: 0
    val isPublic = jsonObject.get(IS_PUBLIC)?.asBoolean ?: false
    val jsonUpdateDate = jsonObject.get(UPDATE_DATE)
    val updateDate = if (jsonUpdateDate != null) gson.fromJson(jsonUpdateDate, Date::class.java) else Date(0)
    val jsonSteps = jsonObject.get(STEPS)
    val steps = if (jsonSteps != null) gson.fromJson<List<Int>>(jsonSteps, object: TypeToken<List<Int>>(){}.type) else listOf()

    remoteInfo.id = id
    remoteInfo.unitId = unitId
    remoteInfo.isPublic = isPublic
    remoteInfo.updateDate = updateDate
    remoteInfo.steps = steps

    lesson.remoteInfo = remoteInfo
    return lesson
  }

  override fun serialize(lesson: Lesson?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
    val tree = gson.toJsonTree(lesson)
    val jsonObject = tree.asJsonObject
    val remoteInfo = lesson?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikLessonRemoteInfo

    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.id ?: 0))
    jsonObject.add(UNIT_ID, JsonPrimitive(stepikRemoteInfo?.unitId ?: 0))
    jsonObject.add(IS_PUBLIC, JsonPrimitive(stepikRemoteInfo?.isPublic ?: false))
    jsonObject.add(STEPS, gson.toJsonTree(stepikRemoteInfo?.steps ?: Lists.emptyList<Int>()))

    val updateDate = stepikRemoteInfo?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }

    return jsonObject
  }
}

class StepikTaskRemoteInfoAdapter : JsonDeserializer<Task>, JsonSerializer<Task> {
  private val ID = "stepic_id"
  private val UPDATE_DATE = "update_date"

  override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Task {
    val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(StepikWrappers.StepOptions::class.java, StepikStepOptionsAdapter())
      .registerTypeAdapter(StepikWrappers.Reply::class.java, StepikReplyAdapter())
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()

    val task = gson.fromJson(json, Task::class.java)
    deserializeRemoteInfo(gson, task, json)
    return task
  }

  private fun deserializeRemoteInfo(gson: Gson, task: Task, json: JsonElement): Task? {
    val jsonObject = json.asJsonObject

    val remoteInfo = StepikTaskRemoteInfo()

    val id = jsonObject.get(ID)?.asInt ?: 0
    val jsonUpdateDate = jsonObject.get(UPDATE_DATE)
    val updateDate = if (jsonUpdateDate != null) gson.fromJson(jsonUpdateDate, Date::class.java) else Date(0)

    remoteInfo.stepId = id
    remoteInfo.updateDate = updateDate

    task.remoteInfo = remoteInfo
    return task
  }

  override fun serialize(task: Task?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
    val tree = gson.toJsonTree(task)
    val jsonObject = tree.asJsonObject
    val remoteInfo = task?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikTaskRemoteInfo

    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.stepId ?: 0))

    val updateDate = stepikRemoteInfo?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }

    return jsonObject
  }
}