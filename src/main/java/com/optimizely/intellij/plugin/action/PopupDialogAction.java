// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.optimizely.intellij.plugin.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.pom.Navigatable;
import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.HttpProjectConfigManager;
import com.optimizely.ab.config.ProjectConfigManager;
import com.optimizely.intellij.plugin.service.OptimizelyFactoryService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Action class to demonstrate how to interact with the IntelliJ Platform.
 * The only action this class performs is to provide the user with a popup dialog as feedback.
 * Typically this class is instantiated by the IntelliJ Platform framework based on declarations
 * in the plugin.xml file. But when added at runtime this class is instantiated by an action group.
 */
public class PopupDialogAction extends AnAction {

  /**
   * This default constructor is used by the IntelliJ Platform framework to
   * instantiate this class based on plugin.xml declarations. Only needed in PopupDialogAction
   * class because a second constructor is overridden.
   * @see AnAction#AnAction()
   */
  public PopupDialogAction() {
    super();
  }
  
  /**
   * This constructor is used to support dynamically added menu actions.
   * It sets the text, description to be displayed for the menu item.
   * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
   * @param text  The text to be displayed as a menu item.
   * @param description  The description of the menu item.
   * @param icon  The icon to be used with the menu item.
   */
  public PopupDialogAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }
  
  /**
   * Gives the user feedback when the dynamic action menu is chosen.
   * Pops a simple message dialog. See the psi_demo plugin for an
   * example of how to use AnActionEvent to access data.
   * @param event Event received when the associated menu item is chosen.
   */
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {

    OptimizelyFactoryService factory = ServiceManager.getService(OptimizelyFactoryService.class);

    Optimizely optimizely = factory.getCurrentOptimizely();
    if (optimizely == null) {
      Messages.showErrorDialog("Please set your SDK Key first!", "Optimizely not initialized");
      return;
    }

    // Using the event, create and show a dialog
    Project currentProject = event.getProject();
    StringBuffer dlgMsg = new StringBuffer(event.getPresentation().getText() + " Selected!");
    String dlgTitle = event.getPresentation().getDescription();
    // If an element is selected in the editor, add info about it.
    Navigatable nav = event.getData(CommonDataKeys.NAVIGATABLE);
    if (nav != null) {
      dlgMsg.append(String.format("\nSelected Element: %s", nav.toString()));
    }

    List list;
    String title = "Experiments";

    String id = ActionManager.getInstance().getId(this);
    if (id.contains("Experiment")) {
      list = new ArrayList<String>(optimizely.getOptimizelyConfig().getExperimentsMap().keySet());
    }
    else if (id.contains("Feature")) {
      list = new ArrayList<String>(optimizely.getOptimizelyConfig().getFeaturesMap().keySet());
      title = "Features";
    }
    else if (id.contains("Event")){
      list = new ArrayList<String>(optimizely.getProjectConfig().getEventNameMapping().keySet());
      title = "Events";
    }
    else {
      list = new ArrayList<String>(optimizely.getProjectConfig().getAttributeKeyMapping().keySet());
      title = "Attributes";
    }

    ListPopupStep<String> step = new BaseListPopupStep<String>(title, list) {
      @Override
      public boolean isSelectable(String value) {
        return true;
      }

      @Nullable
      @Override
      public Icon getIconFor(String aValue) {
        return null;
      }

      @NotNull
      @Override
      public String getTextFor(String value) {
        return value;
      }

      @Nullable
      @Override
      public ListSeparator getSeparatorAbove(String value) {
        return null;
      }

      @Override
      public int getDefaultOptionIndex() {
        return 0;
      }

      @Nullable
      @Override
      public PopupStep onChosen(String selectedValue, boolean finalChoice) {
        Editor editor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();

        Runnable r = ()-> EditorModificationUtil.insertStringAtCaret(editor, "\"" + selectedValue + "\"");

        WriteCommandAction.runWriteCommandAction(currentProject, r);

        return null;
      }
    };
    ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);

    Editor editor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();
    popup.showInBestPositionFor(editor);
  }
  
  /**
   * Determines whether this menu item is available for the current context.
   * Requires a project to be open.
   * @param e Event received when the associated group-id menu is chosen.
   */
  @Override
  public void update(AnActionEvent e) {
    // Set the availability based on whether a project is open
    Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null);
  }
  
}
