package com.fokot.createimports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IActionDelegate;

/**
 * Create imports in single Java source file
 * Transforms fully qualified names in source file into imports and simple names if possible
 *
 * It is done in these steps:
 *
 * 1. Single imports, on-demand imports, simple type names and qualified type names are collected
 * 2. All unused single imports are removed
 * 3. Single imports for simple type names for which there is no single import (just on-demand import) are created
 * 4. Imports for qualified type names are created if possible (no import with simple name like that exists)
 *
 * Import from java.lang.* package are removed, if possible
 *
 * Static imports are kept untouched
 *
 */
public class CreateSingleSourceFileOperation  {

	private static final Pattern JAVA_LANG_CLASS = Pattern.compile("java\\.lang\\.[^\\.]*");

	/**
	 * Processed java project
	 */
	private IJavaProject javaProject;

	/**
	 * Processed java source file
	 */
	private ICompilationUnit compilationUnit;

	/**
	 *
	 * AST Root of processed java source file
	 */
	private CompilationUnit astRoot;

	public CreateSingleSourceFileOperation(IJavaProject javaProject, ICompilationUnit compilationUnit) {
		this.javaProject = javaProject;
		this.compilationUnit = compilationUnit;
	}


	public void run() throws JavaModelException {

		astRoot = SharedASTProvider.getAST(compilationUnit, SharedASTProvider.WAIT_YES, null);
		ImportsAndTypesVisitor visitor = new ImportsAndTypesVisitor();
		astRoot.accept(visitor);
		List<ImportDeclaration> allSingleImports = visitor.singleImports;
		ASTRewrite astRewrite = ASTRewrite.create(astRoot.getAST());

		// remove unused imports
		List<ImportDeclaration> removedImports = removeUnusedSingleImports(visitor.singleImports, visitor.simpleTypeNames, astRewrite);
		allSingleImports.removeAll(removedImports);

		// expand on-demand imports (expand imports ending with .*)
		// creates a single imports for simple type names for which there is no single import (just on-demand import)
		List<ImportDeclaration> newSingleImports = expandOnDemandImports(allSingleImports, visitor.onDemandImports, visitor.simpleTypeNames, astRewrite);
		allSingleImports.addAll(newSingleImports);


		// simplify qualified type names
		// if comes to no ambiguity for qualified type name, turns it into simple name and adds an import
		simplifyQualifiedNames(allSingleImports, visitor.qualifiedTypeNames, astRewrite);

		NullProgressMonitor npm = new NullProgressMonitor();
		TextEdit edit = astRewrite.rewriteAST();
		compilationUnit.applyTextEdit(edit, npm);
		compilationUnit.save(npm, true);
	}

	private boolean isClassInThisPackage(String className) throws JavaModelException {

		if( compilationUnit.getPackageDeclarations().length == 0 ){
			return javaProject.findType(className) != null;
		}

		IPackageDeclaration pd = compilationUnit.getPackageDeclarations()[0];
		return javaProject.findType(pd.getElementName() + "." + className) != null;
	}

	private String getSimpleName(Name name){
		if(name.isSimpleName()) {
			return  ((SimpleName)name).getIdentifier();
		}
		return ((QualifiedName) name).getName().getIdentifier();
	}


	/**
	 * Makes qualified type names simple and adds and import if possible
	 * Iterate through all qualified names
	 *  - if the name is already imported -> simplify it
	 *  - if name is not imported -> if there is not the same simple name in imports -> add new import and simplify
	 *                            -> else do nothing
	 * @return simple names of new imports
	 */
	private void simplifyQualifiedNames(List<ImportDeclaration> allSingleImports, List<QualifiedName> qualifiedTypeNames, ASTRewrite astRewrite) throws JavaModelException {

		List<String> singleImportFullNames = Mapper.map(allSingleImports, new Mapper<ImportDeclaration, String>() {
			public String map(ImportDeclaration i) {
				return i.getName().toString();
			}
		});
		List<String> singleImportSimpleNames = Mapper.map(allSingleImports, new Mapper<ImportDeclaration, String>() {
			public String map(ImportDeclaration i) {
				return getSimpleName(i.getName());
			}
		});

		List<String> newSingleImportFullNames = new ArrayList<String>();
		List<String> newSingleImportSimpleNames = new ArrayList<String>();

		for(QualifiedName qn : qualifiedTypeNames){

			String simpleName = qn.getName().getIdentifier();

			if(singleImportFullNames.contains(qn.getFullyQualifiedName()) || newSingleImportFullNames.contains(qn.getFullyQualifiedName())) {
				// it was already imported
				astRewrite.set(qn.getParent(), SimpleType.NAME_PROPERTY, qn.getName(), null);
			}

			else if( !newSingleImportSimpleNames.contains(simpleName) && !singleImportSimpleNames.contains(simpleName)){

				// java.lang package are by default imported
				if(isInJavaLangPackage(qn)){
					// if there is class that can collide - keep qualified name
					if ( isClassInThisPackage(qn.getName().getIdentifier()) ){
						continue;
					}
				}

				// it was not imported, but can be imported
				if(qn.getParent().getClass().getName().equals(MethodInvocation.class.getName())){
					System.out.println(qn.getParent().toString());
				}

				ASTNode parent = qn.getParent();

				switch (parent.getNodeType()) {
				case ASTNode.METHOD_INVOCATION:
					astRewrite.set(parent, MethodInvocation.EXPRESSION_PROPERTY, qn.getName(), null);
					break;
				case ASTNode.SIMPLE_TYPE:
					astRewrite.set(parent, SimpleType.NAME_PROPERTY, qn.getName(), null);
					break;
				case ASTNode.PARAMETERIZED_TYPE:
					astRewrite.set(parent, ParameterizedType.TYPE_PROPERTY, qn.getName(), null);
					break;
				default:
					throw new RuntimeException();
				}

				createImport(qn.getFullyQualifiedName(), astRewrite);
				newSingleImportFullNames.add(qn.getFullyQualifiedName());
				newSingleImportSimpleNames.add(simpleName);
			}

			else {
				// it can not be imported
				// different import has the same simple name
			}
		}

	}

	/**
	 * Creates a single imports for simple type names for which there is no single import (just on-demand import)
	 * Iterate through all used type names
	 * 	- if a name is in single import do nothing
	 * 	- if not try find matching on-demand import and add single import referencing the name
	 * Delete all on-demand imports
	 *
	 * @return simple names of new imports
	 */
	private List<ImportDeclaration> expandOnDemandImports(List<ImportDeclaration> allSingleImports, List<ImportDeclaration> onDemandImports, List<SimpleName> simpleTypeNames, ASTRewrite astRewrite) {

		if(onDemandImports.isEmpty()){
			return Collections.emptyList();
		}

		List<String> singleImportSimpleNames = Mapper.map(allSingleImports, new Mapper<ImportDeclaration, String>() {
			public String map(ImportDeclaration i) {
				return getSimpleName(i.getName());
			}
		});

		List<String> newSingleImportSimpleNames = new ArrayList<String>();
		List<ImportDeclaration> newSingleImports = new ArrayList<ImportDeclaration>();

		for(SimpleName simpleTypeName : simpleTypeNames){


			if(singleImportSimpleNames.contains(simpleTypeName.getIdentifier())) {
				// it was already imported in original source file
				continue;
			}

			if(newSingleImportSimpleNames.contains(simpleTypeName.getIdentifier())){
				// it was already imported by this action
				continue;
			}

			// it was not imported expand on-demand imports if possible
			ITypeBinding b = (ITypeBinding) simpleTypeName.resolveBinding();
			String fullyQuallifiedName = b.getErasure().getQualifiedName();
			ImportDeclaration newImport = createImport(fullyQuallifiedName, astRewrite);
			newSingleImports.add(newImport);
			newSingleImportSimpleNames.add(simpleTypeName.getIdentifier());
		}

		// deletes all on-demand imports
		for(ImportDeclaration onDemandImport : onDemandImports){
			astRewrite.remove(onDemandImport, null);
		}
		return newSingleImports;
	}


	/**
	 * Creates import and adds it to AST
	 */
	private ImportDeclaration createImport(String fullyQuallifiedName, ASTRewrite astRewrite) {
		ImportDeclaration id = astRewrite.getAST().newImportDeclaration();
		Name name = createQualifiedName(fullyQuallifiedName, astRewrite.getAST());
		id.setName(name);

		ListRewrite listRewrite = astRewrite.getListRewrite(astRoot, CompilationUnit.IMPORTS_PROPERTY);
		listRewrite.insertLast(id, null);
		return id;
	}

	/**
	 * Constructs qualified name for given string
	 */
	private Name createQualifiedName(String fullyQuallifiedName, AST ast){

		if( !fullyQuallifiedName.contains(".")) {
			return ast.newSimpleName(fullyQuallifiedName);
		}

		String [] sns = fullyQuallifiedName.split("\\.");
		Name result = ast.newSimpleName(sns[0]);

		for(int i=1; i<sns.length; i++){
			result = ast.newQualifiedName(result, ast.newSimpleName(sns[i]));
		}
		return result;
	}


	/**
	 * Removes imports
	 * Iterate through all single import, if the import is not used, removes it
	 *
	 * @return removed imports
	 */
	private List<ImportDeclaration> removeUnusedSingleImports(List<ImportDeclaration> singleImports, List<SimpleName> simpleTypeNames, ASTRewrite astRewrite) throws JavaModelException{

		List<String> simpleNamesString = Mapper.map(simpleTypeNames, new Mapper<SimpleName, String>() {
			public String map(SimpleName a) {
				return a.getIdentifier();
			}
		});

		List<ImportDeclaration> res = new ArrayList<ImportDeclaration>();

		for(ImportDeclaration singleImport : singleImports){

			if(singleImport.getName().isSimpleName()){
				astRewrite.remove(singleImport, null);
				res.add(singleImport);
			} else {
				QualifiedName qn = (QualifiedName) singleImport.getName();

				if(! simpleNamesString.contains(qn.getName().getIdentifier())){
					astRewrite.remove(singleImport, null);
					res.add(singleImport);
					continue;
				}

				// java.lang package is by default imported - so it can be removed even when it is used
				if(isInJavaLangPackage(qn)){
					// if there is no class it can collide
					if (! isClassInThisPackage(qn.getName().getIdentifier()) ){
						astRewrite.remove(singleImport, null);
						res.add(singleImport);
						continue;
					}
				}
			}
		}

		return res;
	}

	private boolean isInJavaLangPackage(QualifiedName qn){
		return JAVA_LANG_CLASS.matcher(qn.getFullyQualifiedName()).matches();
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
}


