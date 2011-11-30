package com.fokot.createimports;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * Create imports on whole Java project
 *
 */
public class CreateImportsOperation extends WorkspaceModifyOperation {

	/**
	 * Processed java project
	 */
	private IJavaProject javaProject;

	/**
	 * All java source files in project
	 */
	private Collection<ICompilationUnit> sourceFiles;

	public CreateImportsOperation(IJavaProject javaProject) {
		this.javaProject = javaProject;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		sourceFiles = new ArrayList<ICompilationUnit>();

		IPackageFragmentRoot[] pfrs = javaProject.getPackageFragmentRoots();
		for(IPackageFragmentRoot pfr : pfrs){

			/**
			 * if it is a source folder it will be progressed
			 */
			if(pfr.getKind() == IPackageFragmentRoot.K_SOURCE){
				collectSourceFiles(pfr);
			}
		}


		int i = 0;
		monitor.beginTask(CreateImportsOperation.class.getSimpleName(), sourceFiles.size());


		/**
		 * Iterates through all java sources and tries to organize imports
		 */
		for(ICompilationUnit cu : sourceFiles){

			try {
				new CreateSingleSourceFileOperation(javaProject, cu).run();
			} catch (Exception e) {
				e.printStackTrace();
				// TODO logging
			} finally {
				monitor.worked(++i);
			}

		}

		monitor.done();
	}

	private void collectSourceFiles(IParent p) throws JavaModelException {
		for (IJavaElement child : p.getChildren()) {

			switch (child.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				collectSourceFiles((IPackageFragment) child);
				break;
			case IJavaElement.COMPILATION_UNIT:
				sourceFiles.add((ICompilationUnit) child);
				break;
			}
		}
	}

}