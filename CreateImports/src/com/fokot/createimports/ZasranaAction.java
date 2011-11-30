package com.fokot.createimports;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.PluginAction;

public class ZasranaAction implements IObjectActionDelegate {


	private Shell shell;
	private ICompilationUnit compilationUnit;
	private CompilationUnit astRoot;

	/**
	 * Constructor for Action1.
	 */
	public ZasranaAction() {
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

			new CreateImportsOperation(compilationUnit.getJavaProject()).execute(new NullProgressMonitor());

//			new ToImportsSingleSourceFileOperation(compilationUnit.getJavaProject(), compilationUnit).run();

			MessageDialog.openInformation(
					shell,
					"ZasranyOrganizeImport",
					"ZasranaAction was executed.");
		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openInformation(
					shell,
					"ZasranyOrganizeImport",
					"ZasranaAction sa posrala.");
		}
	}

	/*
	 * Zoberiem vsetky nazvy typov pre fieldy, navratove a parametre metod
	 * rozdelim ich podla simple a qualified.
	 * Odstranim importy, ktore sa nepouzivaju.
	 * Importy ktore su .* prepisem na bez *, podla simple nameov.
	 * Prejdem vsetky Qualified nazvy:
	 *  - ak je taky import zjednodusim ho
	 *  - ak nie je pozriem, ci sa mi jeho simple name nebije so simple name nejakeho importu, ak nie zjednodusim ho a pridam import, inak preskocim..
	 *
	 *  Preiterovat IJavaModel a spustit vsade
	 *
	 *
		 Type:
		    PrimitiveType
		    ArrayType
		    SimpleType
		    QualifiedType
		    ParameterizedType
		    WildcardType
	 *
	 */

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}