package com.fokot.createimports;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.PluginAction;

public class CreateImportsAction implements IObjectActionDelegate {


	private Shell shell;
	private ICompilationUnit compilationUnit;
	private CompilationUnit astRoot;

	/**
	 * Constructor for Action1.
	 */
	public CreateImportsAction() {
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		PluginAction a = (PluginAction) action;
		TreeSelection s = (TreeSelection) a.getSelection();
		compilationUnit = (ICompilationUnit) s.getFirstElement();
		shell = targetPart.getSite().getShell();
	}


	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {


		try {

			CreateImportsOperation ci = new CreateImportsOperation(compilationUnit.getJavaProject());
			ci.execute(new NullProgressMonitor());

			if(ci.isErrors()){

				MessageDialog.openInformation(
						shell,
						"CreateImportsAction",
						"CreateImportsAction was executed. Some files were not processed, please refer to error log.");
			} else {

				MessageDialog.openInformation(
						shell,
						"CreateImportsAction",
						"CreateImportsAction was executed.");
			}


		} catch (Exception e) {

			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Error running CreateImportsAction occured.", e);
			Activator.getDefault().getLog().log(status);

			MessageDialog.openInformation(
					shell,
					"CreateImportsAction",
					"Error running CreateImportsAction occured.");
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}