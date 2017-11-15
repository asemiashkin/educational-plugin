package com.jetbrains.edu.learning.courseFormat.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.checker.StudyCheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.editor.StudyChoiceVariantsPanel;
import com.jetbrains.edu.learning.stepic.EduAdaptiveStepicConnector;
import com.jetbrains.edu.learning.stepic.StepicUser;
import com.jetbrains.edu.learning.ui.StudyToolWindow;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChoiceTask extends Task {

  @SuppressWarnings("unused") //used for deserialization
  public ChoiceTask() {}

  @Expose @SerializedName("choice_variants") private List<String> myChoiceVariants = new ArrayList<>();
  @Expose @SerializedName("is_multichoice") private boolean myIsMultipleChoice;
  @SerializedName("selected_variants") private List<Integer> mySelectedVariants = new ArrayList<>();

  public List<Integer> getSelectedVariants() {
    return mySelectedVariants;
  }

  public void setSelectedVariants(List<Integer> selectedVariants) {
    mySelectedVariants = selectedVariants;
  }

  public boolean isMultipleChoice() {
    return myIsMultipleChoice;
  }

  public void setMultipleChoice(boolean multipleChoice) {
    myIsMultipleChoice = multipleChoice;
  }

  public List<String> getChoiceVariants() {
    return myChoiceVariants;
  }

  public void setChoiceVariants(List<String> choiceVariants) {
    myChoiceVariants = choiceVariants;
  }

  public ChoiceTask(@NotNull final String name) {
    super(name);
  }

  @Override
  public String getTaskType() {
    return "choice";
  }

  @Override
  public TaskChecker getChecker(@NotNull Project project) {
    return new TaskChecker<ChoiceTask>(this, project) {
      @Override
      public StudyCheckResult checkOnRemote() {
        StepicUser user = EduSettings.getInstance().getUser();
        if (user == null) {
          return new StudyCheckResult(StudyStatus.Unchecked, CheckAction.FAILED_CHECK_LAUNCH);
        }
        return EduAdaptiveStepicConnector.checkChoiceTask(myTask, user);
      }

      @Override
      public void onTaskFailed(@NotNull String message) {
        super.onTaskFailed(message);
        repaintChoicePanel(project, myTask);
      }

      private void repaintChoicePanel(@NotNull Project project, @NotNull ChoiceTask task) {
        final StudyToolWindow toolWindow = StudyUtils.getStudyToolWindow(project);
        if (toolWindow != null) {
          toolWindow.setBottomComponent(new StudyChoiceVariantsPanel(task));
        }
      }
    };
  }
}
