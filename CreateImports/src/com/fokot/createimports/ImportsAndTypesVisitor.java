package com.fokot.createimports;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Traverses AST and collects imports and names
 */
class ImportsAndTypesVisitor extends ASTVisitor {

	/**
	 * e.g. import java.util.ArrayList;
	 * */
	public final List<ImportDeclaration> singleImports = new ArrayList<ImportDeclaration>();

	/**
	 * e.g. import java.util.*;
	 */
	public final List<ImportDeclaration> onDemandImports = new ArrayList<ImportDeclaration>();

	/**
	 * e.g.  private java.math.BigDecimal price;
	 */
	public final List<QualifiedName> qualifiedTypeNames = new ArrayList<QualifiedName>();

	/**
	 * e.g.  private BigDecimal price;
	 */
	public final List<SimpleName> simpleTypeNames = new ArrayList<SimpleName>();

	public boolean visit(ImportDeclaration node) {

		// skip static imports
		if (node.isStatic()){
			return false;
		}

		if(node.isOnDemand()){
			onDemandImports.add(node);
		} else {
			singleImports.add(node);
		}
		return false;
	};

	public boolean visit(SimpleName node) {

		// we are not interested in type parameters
		if(node.getParent().getNodeType() == ASTNode.TYPE_PARAMETER){
			return false;
		}

		// we are not interested in other names, e.g. variable names.., just types
		if(node.resolveBinding().getKind() == IBinding.TYPE){
			simpleTypeNames.add(node);
		}
		return false;
	};

	public boolean visit(QualifiedName node) {

		IBinding binding = node.resolveBinding();

		// we are interested in types bindings
		if(binding.getKind() == IBinding.TYPE){
			qualifiedTypeNames.add(node);
		}
		// variable binding are also interesting,
		// e.g. @GeneratedValue(strategy = GenerationType.AUTO)
		// GenerationType.AUTO is variable binding
		if(binding.getKind() == IBinding.VARIABLE){
			return true;
		}
		return false;
	};
}