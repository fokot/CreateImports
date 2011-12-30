package com.fokot.createimports;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * Create imports on whole Java project
 *
 */
public class CreateImportsOperation extends WorkspaceModifyOperation {

	/**
	 * Selected element (project, or package fragment root (source directory), or package fragment, or compilation unit (source file))
	 */
	private IJavaElement selectedElement;

	public CreateImportsOperation(IJavaElement selectedElement) {
		this.selectedElement = selectedElement;
	}

	/**
	 * True if there was an error in a file
	 */
	private boolean errors = false;

	public boolean isErrors() {
		return errors;
	}

	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		// All java source files in project
		Collection<ICompilationUnit> sourceFiles = collectSourceFiles(selectedElement);

		int i = 0;
		monitor.beginTask(CreateImportsOperation.class.getSimpleName(), sourceFiles.size());

		IJavaProject project = ((IJavaElement)selectedElement).getJavaProject();

		/**
		 * Iterates through all java sources and tries to organize imports
		 */
		for(ICompilationUnit cu : sourceFiles){

			try {
				new CreateSingleSourceFileOperation(project, cu).run();
			} catch (Exception e) {
				errors = true;

				IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Could not create imports in file " + cu.getElementName(), e);
				Activator.getDefault().getLog().log(status);
			} finally {
				monitor.worked(++i);
			}

		}
		monitor.done();
	}

	private Collection<ICompilationUnit> collectSourceFiles(IJavaElement e) throws JavaModelException {
		switch (e.getElementType()) {
		case IJavaElement.JAVA_PROJECT:
			return collectSourceFiles((IJavaProject)e);
		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			return collectSourceFiles((IPackageFragmentRoot)e);
		case IJavaElement.PACKAGE_FRAGMENT:
			return collectSourceFiles((IPackageFragment)e);
		case IJavaElement.COMPILATION_UNIT:
			return collectSourceFiles((ICompilationUnit)e);
		default:
			throw new RuntimeException("Unknown java element");
		}
	}

	private Collection<ICompilationUnit> collectSourceFiles(IJavaProject p) throws JavaModelException {
		Collection<ICompilationUnit> result = new ArrayList<ICompilationUnit>();
		for(IJavaElement e : p.getChildren()){
			if(e.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT){
				IPackageFragmentRoot pfr = (IPackageFragmentRoot) e;
				result.addAll(collectSourceFiles(pfr));
			}
		}
		return result;
	}

	private Collection<ICompilationUnit> collectSourceFiles(IPackageFragmentRoot p) throws JavaModelException {
		if (p.getKind() != IPackageFragmentRoot.K_SOURCE) {
			return Collections.emptyList();
		}

		Collection<ICompilationUnit> result = new ArrayList<ICompilationUnit>();
		for(IJavaElement e : p.getChildren()){
			if(e.getElementType() == IJavaElement.PACKAGE_FRAGMENT){
				PackageFragment pf = (PackageFragment) e;
				result.addAll(collectSourceFilesFromSinglePackage(pf));
			}
		}
		return result;
	}

	private Collection<ICompilationUnit> collectSourceFiles(IPackageFragment p) throws JavaModelException {

		String packageName = p.getElementName();
		IPackageFragmentRoot pfr = (IPackageFragmentRoot) p.getParent();

		Collection<ICompilationUnit> result = new ArrayList<ICompilationUnit>();
		for(IJavaElement e : pfr.getChildren()){
			if(e.getElementType() == IJavaElement.PACKAGE_FRAGMENT && e.getElementName().startsWith(packageName)){
				PackageFragment pf = (PackageFragment) e;
				result.addAll(collectSourceFilesFromSinglePackage(pf));
			}
		}

		return result;

	}
	private Collection<ICompilationUnit> collectSourceFilesFromSinglePackage(IPackageFragment p) throws JavaModelException {
		Collection<ICompilationUnit> result = new ArrayList<ICompilationUnit>();
		for(IJavaElement e : p.getChildren()){
			if(e.getElementType() == IJavaElement.COMPILATION_UNIT){
				result.add((ICompilationUnit) e);
			}
		}
		return result;
	}

	private Collection<ICompilationUnit> collectSourceFiles(ICompilationUnit c) throws JavaModelException {
		return Arrays.asList(new ICompilationUnit[] { c });
	}

}