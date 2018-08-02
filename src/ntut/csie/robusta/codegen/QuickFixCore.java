package ntut.csie.robusta.codegen;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.util.MethodInvocationCollectorVisitor;
import ntut.csie.util.TryStatementCollectorVisitor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class QuickFixCore {
	private static Logger logger = LoggerFactory.getLogger(QuickFixCore.class);

    protected IOpenable actOpenable = null;
	/** Java AST root node whick will be Quick Fix */
	private CompilationUnit compilationUnit = null;
	private ASTRewrite astRewrite = null;
	private IMarker marker;
	private List<String> stmtMovedIntoTry = new ArrayList<String>();

	private int trystmtIndex = 0;

	private boolean hasTryStmtButReleaseMethodNotInTry = false;
	
	public List<String> getStmtMovedIntoTry(){
	  return stmtMovedIntoTry;
	}

	public void setJavaFileModifiable(IResource resource) {

		IJavaElement javaElement = JavaCore.create(resource);
		if (javaElement instanceof IOpenable) {
			actOpenable = (IOpenable) javaElement;
		}
		

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource((ICompilationUnit) javaElement);
		parser.setResolveBindings(true);
		compilationUnit = (CompilationUnit) parser.createAST(null);
		compilationUnit.recordModifications();
		astRewrite = ASTRewrite.create(compilationUnit.getRoot().getAST());
	}
	
	public void setMarker( IMarker _marker ){
		marker = _marker;
	}

	public void generateRobustnessLevelAnnotation(
			MethodDeclaration methodDeclaration, int level,
			Class<?> exceptionType) {
		AST rootAST = compilationUnit.getAST();

		// Detect if the Robustness Level Annotation exists or not.
		if (QuickFixUtils.getExistingRLAnnotation(methodDeclaration) != null) {
			appendRLAnnotation(level, exceptionType, rootAST, methodDeclaration);
		} else {
			createRLAnnotation(level, exceptionType, rootAST, methodDeclaration);
		}

		// Import robustness level package if it's not imported.
		boolean isRobustnessClassImported = QuickFixUtils.isClassImported(
				Robustness.class, compilationUnit);
		boolean isRLClassImported = QuickFixUtils.isClassImported(RTag.class,
				compilationUnit);

		if (!isRobustnessClassImported) {
			ImportDeclaration importDeclaration = rootAST
					.newImportDeclaration();
			importDeclaration.setName(rootAST.newName(Robustness.class
					.getName()));

			// The rewrite list from the AST of compilation unit that you want
			// to modify.
			ListRewrite addImportDeclarationList = astRewrite.getListRewrite(
					compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
			addImportDeclarationList.insertLast(importDeclaration, null);
		}

		if (!isRLClassImported) {
			ImportDeclaration importDeclaration = rootAST
					.newImportDeclaration();
			importDeclaration.setName(rootAST.newName(RTag.class.getName()));

			// The rewrite list from the AST of compilation unit that you want
			// to modify.
			ListRewrite addImportDeclarationList = astRewrite.getListRewrite(
					compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
			addImportDeclarationList.insertLast(importDeclaration, null);
		}
	}

	/**
	 * When a method is without any Robustness Level Annotation, you should use
	 * this method to create RL Annotation.
	 * 
	 * @param level
	 * @param exceptionType
	 * @param rootAST
	 * @param methodDeclaration
	 */
	private void createRLAnnotation(int level, Class<?> exceptionType,
			AST rootAST, MethodDeclaration methodDeclaration) {

		/*
		 * Add Robustness level annotation
		 */
		// Robustness level annotation belongs NormalAnnotation class.
		NormalAnnotation normalAnnotation = rootAST.newNormalAnnotation();
		normalAnnotation.setTypeName(rootAST.newSimpleName(Robustness.class
				.getSimpleName()));
		// RL "level" and "exception type"
		MemberValuePair value = rootAST.newMemberValuePair();
		value.setName(rootAST.newSimpleName(Robustness.VALUE));

		ListRewrite normalAnnotationRewrite = astRewrite.getListRewrite(
				normalAnnotation, NormalAnnotation.VALUES_PROPERTY);
		normalAnnotationRewrite.insertLast(value, null);

		ArrayInitializer rlArrayInitializer = rootAST.newArrayInitializer();
		value.setValue(rlArrayInitializer);

		ListRewrite rlArrayInitializerRewrite = astRewrite.getListRewrite(
				rlArrayInitializer, ArrayInitializer.EXPRESSIONS_PROPERTY);
		rlArrayInitializerRewrite.insertLast(
				QuickFixUtils.makeRLAnnotation(rootAST, level,
						exceptionType.getName()), null);

		// // the meaning of this code is removing original RLAnnotation
		// List<IExtendedModifier> lstModifiers = methodDeclaration.modifiers();
		//
		// for(IExtendedModifier ieModifier : lstModifiers){
		// //remove old Robustness Annotation
		// if(ieModifier.isAnnotation() &&
		// ieModifier.toString().indexOf("Robustness") != -1){
		// ListRewrite mdModifiers =
		// astRewrite.getListRewrite(methodDeclaration,
		// MethodDeclaration.MODIFIERS2_PROPERTY);
		// mdModifiers.remove((ASTNode)ieModifier, null);
		// break;
		// }
		// }

		/*
		 * Add Annotation into MethodDeclaration node. Attention: The ASTNode
		 * and the ChildListPropertyDescriptor are special.
		 */
		ListRewrite addAnnotationList = astRewrite.getListRewrite(
				methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
		addAnnotationList.insertFirst(normalAnnotation, null);
	}

	/**
	 * add an exception's declaration on method's signature
	 * 
	 * @param astRewrite
	 * @param methodDeclaration
	 */
	public void generateThrowExceptionOnMethodSignature(
			MethodDeclaration methodDeclaration, Class<?> exceptionType) {
		ListRewrite addingThrownException = astRewrite
				.getListRewrite(methodDeclaration,
						MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
		ASTNode simpleName = QuickFixUtils
				.generateThrowExceptionForDeclaration(
						methodDeclaration.getAST(), exceptionType);
		addingThrownException.insertLast(simpleName, null);
	}
	
	/**
	 * add an exception's declaration on method's signature
	 * 
	 * @param astRewrite
	 * @param methodDeclaration
	 */
	public void generateThrowExceptionOnMethodSignature(
			MethodDeclaration methodDeclaration, String exceptionType) {
		ListRewrite addingThrownException = astRewrite
				.getListRewrite(methodDeclaration,
						MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
		ASTNode simpleName = QuickFixUtils
				.generateThrowExceptionForDeclaration(
						methodDeclaration.getAST(), exceptionType);
		addingThrownException.insertLast(simpleName, null);
	}

	/**
	 * remove specified code statement in specified catch clause
	 * 
	 * @param catchClause
	 * @param removingStatement
	 */
	public void removeNodeInCatchClause(CatchClause catchClause,
			String... removingStatements) {
		// The rewrite list from the AST of catch clause that you want to
		// modify.
		ListRewrite modifyingCatchClause = astRewrite.getListRewrite(
				catchClause.getBody(), Block.STATEMENTS_PROPERTY);
		for (String statement : removingStatements) {
			ExpressionStatementStringFinderVisitor expressionStatementFinder = new ExpressionStatementStringFinderVisitor(
					statement);
			catchClause.accept(expressionStatementFinder);
			ASTNode removeNode = expressionStatementFinder
					.getFoundExpressionStatement();
			if (removeNode != null) {
				modifyingCatchClause.remove(removeNode, null);
			}
		}
	}

	/**
	 * add throw new xxxException(e) statement
	 * 
	 * @param cc
	 * @param exceptionType
	 */
	public void addThrowRefinedExceptionInCatchClause(CatchClause cc,
			Class<?> exceptionType) {
		ASTNode createInstanceNode = QuickFixUtils
				.generateThrowNewExceptionNode(cc.getException()
						.resolveBinding().getName(), cc.getAST(), exceptionType);
		ListRewrite modifyingCatchClause = astRewrite.getListRewrite(
				cc.getBody(), Block.STATEMENTS_PROPERTY);
		modifyingCatchClause.insertLast(createInstanceNode, null);
	}

	public void addThrowExceptionInCatchClause(CatchClause cc) {
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
				.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		ListRewrite modifyingCatchClauseList = astRewrite.getListRewrite(
				cc.getBody(), Block.STATEMENTS_PROPERTY);
		ASTNode throwCheckedException = astRewrite.createStringPlaceholder(
				"throw " + svd.resolveBinding().getName() + ";",
				ASTNode.EMPTY_STATEMENT);
		modifyingCatchClauseList.insertLast(throwCheckedException, null);
	}

	/**
	 * When there is existing Robustness Level Annotation, and you just want to
	 * append new annotation, you should use this.
	 * 
	 * @param level
	 * @param exceptionType
	 * @param rootAST
	 * @param methodDeclaration
	 */
	private void appendRLAnnotation(int level, Class<?> exceptionType,
			AST rootAST, MethodDeclaration methodDeclaration) {
		NormalAnnotation na = QuickFixUtils
				.getExistingRLAnnotation(methodDeclaration);
		MemberValuePair mvp = (MemberValuePair) na.values().get(
				na.values().size() - 1);
		ListRewrite normalA = astRewrite.getListRewrite(mvp.getValue(),
				ArrayInitializer.EXPRESSIONS_PROPERTY);
		normalA.insertLast(
				QuickFixUtils.makeRLAnnotation(rootAST, level,
						exceptionType.getName()), null);
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public ICompilationUnit getICompilationUnit() {
		return (ICompilationUnit) actOpenable;
	}

	/**
	 * update modification of quick fix in editor
	 * 
	 * @param rewrite
	 */
	public void applyChange() {
		try {
			// consult
			// org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator
			// run
			// org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			IEditorPart part = EditorUtility.isOpenInEditor(cu);
			IEditorInput input = part.getEditorInput();
			IDocument doc = JavaPlugin.getDefault()
					.getCompilationUnitDocumentProvider().getDocument(input);
			performChange(JavaPlugin.getActivePage().getActiveEditor(), doc,
					astRewrite);
		} catch (CoreException e) {
			logger.error("[Core Exception] EXCEPTION ", e);
		}
	}

	/**
	 * update modification of refactoring in editor
	 * 
	 * @param textFileChange
	 * @throws CoreException
	 */
	public TextFileChange applyRefactoringChange() throws JavaModelException {
		ICompilationUnit cu = (ICompilationUnit) actOpenable;
		Document document = new Document(cu.getBuffer().getContents());
		TextEdit edits = compilationUnit.rewrite(document, cu.getJavaProject()
				.getOptions(true));
		TextFileChange textFileChange = new TextFileChange(cu.getElementName(),
				(IFile) cu.getResource());
		textFileChange.setEdit(edits);
		return textFileChange;
	}

	/**
	 * invoke Quick Fix
	 * 
	 * @param activeEditor
	 * @param document
	 * @param rewrite
	 * @throws CoreException
	 */
	private void performChange(IEditorPart activeEditor, IDocument document,
			ASTRewrite rewrite) throws CoreException {
		Change change = null;
		IRewriteTarget rewriteTarget = null;
		try {
			change = getChange(compilationUnit, rewrite);
			if (change != null) {
				if (document != null) {
					LinkedModeModel.closeAllModels(document);
				}
				if (activeEditor != null) {
					rewriteTarget = (IRewriteTarget) activeEditor
							.getAdapter(IRewriteTarget.class);
					if (rewriteTarget != null) {
						rewriteTarget.beginCompoundChange();
					}
				}

				change.initializeValidationData(new NullProgressMonitor());
				RefactoringStatus valid = change
						.isValid(new NullProgressMonitor());
				if (valid.hasFatalError()) {
					IStatus status = new Status(
							IStatus.ERROR,
							JavaPlugin.getPluginId(),
							IStatus.ERROR,
							valid.getMessageMatchingSeverity(RefactoringStatus.FATAL),
							null);
					throw new CoreException(status);
				} else {
					IUndoManager manager = RefactoringCore.getUndoManager();
					manager.aboutToPerformChange(change);
					Change undoChange = change
							.perform(new NullProgressMonitor());
					manager.changePerformed(change, true);
					if (undoChange != null) {
						undoChange
								.initializeValidationData(new NullProgressMonitor());
						manager.addUndo("Quick Undo", undoChange);
					}
				}
			}
		} finally {
			if (rewriteTarget != null) {
				rewriteTarget.endCompoundChange();
			}

			if (change != null) {
				change.dispose();
			}
		}
	}

	private boolean isCatchingAllException(TryStatement tryStatement) {
		List<CatchClause> catchClauseList = tryStatement.catchClauses();
		for (CatchClause catchClause : catchClauseList) {
			if (catchClause.getException().getType().toString()
					.equals("Exception")
					|| catchClause.getException().getType().toString()
							.equals("Throwable")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * get statements after Quick Fix
	 */
	private Change getChange(CompilationUnit actRoot, ASTRewrite rewrite) {
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());

			TextEdit edits = null;
			edits = rewrite.rewriteAST(document, null);

			TextFileChange textFileChange = new TextFileChange(
					cu.getElementName(), (IFile) cu.getResource());
			textFileChange.setEdit(edits);

			return textFileChange;
		} catch (JavaModelException e) {
			logger.error(
					"[Apply Change Rethrow Unchecked Exception] EXCEPTION ", e);
		}
		return null;
	}
	
	
	public void InsertLog4jImportAndStmt(MethodDeclaration methodDeclaration){
		//add log4j declaration before main method declaration
		String delcarationClassName = ((TypeDeclaration)methodDeclaration.getParent()).resolveBinding().getName();
		ListRewrite  parentListRewrite = astRewrite.getListRewrite(methodDeclaration.getParent(), TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		String Logger="private static Logger logger = Logger.getLogger("+delcarationClassName+".class);";
		ASTNode placeHolder = astRewrite.createStringPlaceholder(Logger,ASTNode.EMPTY_STATEMENT);
		parentListRewrite.insertBefore(placeHolder, methodDeclaration,null);
		
		
		//import log4j 
		String[] importClassNameCollection = {"org.apache.log4j.Logger","org.apache.log4j.PropertyConfigurator"};
		AST rootAST = compilationUnit.getAST();
		for(String importClassName : importClassNameCollection){
			ImportDeclaration importDeclaration = rootAST.newImportDeclaration();
			importDeclaration.setName(rootAST.newName(importClassName));
			// The rewrite list from the AST of compilation unit that you want to modify. 
			ListRewrite addImportDeclarationList = astRewrite.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY);
			addImportDeclarationList.insertLast(importDeclaration, null);
		}
	}
	

	/**
	 * generate a try statement to contain all statements in method
	 * 
	 * @param methodDeclaration
	 *            a method which will have a try statement to contain all
	 *            statement of method
	 */
	public void generateTryStatementForQuickFix(MethodDeclaration methodDeclaration ) {
		InsertLog4jImportAndStmt(methodDeclaration);
		List<ASTNode> statements = methodDeclaration.getBody().statements();
		Queue<TryStatement> tryStatements = new LinkedList<TryStatement>();
		Queue<ASTNode> moveTargets = new LinkedList<ASTNode>();
		classifyStatementsToDifferentQueue(statements, tryStatements,
				moveTargets);
		if (tryStatements.isEmpty()) {
			TryStatement tryStatement = createTryCatchStatement(methodDeclaration);
			moveAllStatementInTryStatement(methodDeclaration, tryStatement);
			return;
		}
		Stack<ASTNode> variableDeclarations = new Stack<ASTNode>();
		Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer = new Stack<ASTNode>();
		while (!tryStatements.isEmpty()) {
			TryStatement tryStatement = tryStatements.poll();
			if (!isCatchingAllException(tryStatement)) {
				appendCatchClause(tryStatement);
			}
			ListRewrite body = astRewrite.getListRewrite(
					tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
			ListRewrite neededToBeRefactoredMethodBody = astRewrite
					.getListRewrite(methodDeclaration.getBody(),
							Block.STATEMENTS_PROPERTY);
			Stack<ASTNode> expressionStatements = new Stack<ASTNode>();
			if (!tryStatements.isEmpty()) {
				moveStatementsAboveTryStatementInTryBlock(moveTargets,
						variableDeclarations,
						variableDeclarationEndWithLiteralOrNullInitializer,
						tryStatement, body, expressionStatements);
			} else {
				moveStatementsAboveTryStatementInTryBlock(moveTargets,
						variableDeclarations,
						variableDeclarationEndWithLiteralOrNullInitializer,
						tryStatement, body, expressionStatements);
				moveStatementsBelowTryStatementInTryBlock(moveTargets,
						variableDeclarations,
						variableDeclarationEndWithLiteralOrNullInitializer,
						tryStatement, body, expressionStatements);
				insertStatementsToMethodDeclaration(variableDeclarations,
						variableDeclarationEndWithLiteralOrNullInitializer,
						neededToBeRefactoredMethodBody);
			}
			moveReturnAndThrowStatementToTheLastOfTryStatement(tryStatement);
		}
	}

	private void classifyStatementsToDifferentQueue(List<ASTNode> statements,
			Queue<TryStatement> tryStatements, Queue<ASTNode> moveTargets) {
		for (ASTNode statement : statements) {
			if (statement.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT
					&& statement.getNodeType() != ASTNode.TRY_STATEMENT) {
				ASTNode target = astRewrite.createMoveTarget(statement);

				target.setProperty("startPosition",
						statement.getStartPosition());
				moveTargets.offer(target);
			} else if (statement.getNodeType() == ASTNode.TRY_STATEMENT) {
				tryStatements.offer((TryStatement) statement);
			} else {
				moveTargets.offer(statement);
			}
		}
	}

	private void insertStatementsToMethodDeclaration(
			Stack<ASTNode> variableDeclarations,
			Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer,
			ListRewrite neededToBeRefactoredMethodBody) {
		while (!variableDeclarations.isEmpty()) {
			neededToBeRefactoredMethodBody.insertFirst(
					variableDeclarations.pop(), null);
		}
		while (!variableDeclarationEndWithLiteralOrNullInitializer.isEmpty()) {
			neededToBeRefactoredMethodBody.insertFirst(
					variableDeclarationEndWithLiteralOrNullInitializer.pop(),
					null);
		}
	}

	private void moveStatementsBelowTryStatementInTryBlock(
			Queue<ASTNode> moveTargets, Stack<ASTNode> variableDeclarations,
			Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer,
			TryStatement tryStatement, ListRewrite body,
			Stack<ASTNode> expressionStatements) {
		int targetStartPos = -1;
		targetStartPos = getTargetStartPosition(moveTargets);
		while (targetStartPos > tryStatement.getStartPosition()
				&& !moveTargets.isEmpty()) {
			ASTNode statement = moveTargets.poll();
			if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				identifyVariableDeclaration(variableDeclarations,
						variableDeclarationEndWithLiteralOrNullInitializer,
						expressionStatements, statement);
			} else {
				expressionStatements.push(statement);
			}
			targetStartPos = getTargetStartPosition(moveTargets);
		}
		for (ASTNode expressionStatement : expressionStatements) {
			body.insertLast(expressionStatement, null);
		}
		expressionStatements.clear();
	}

	private void moveStatementsAboveTryStatementInTryBlock(
			Queue<ASTNode> moveTargets, Stack<ASTNode> variableDeclarations,
			Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer,
			TryStatement tryStatement, ListRewrite body,
			Stack<ASTNode> expressionStatements) {
		int targetStartPos = -1;
		targetStartPos = getTargetStartPosition(moveTargets);
		while (targetStartPos < tryStatement.getStartPosition()
				&& !moveTargets.isEmpty()) {
			ASTNode statement = moveTargets.poll();
			if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				identifyVariableDeclaration(variableDeclarations,
						variableDeclarationEndWithLiteralOrNullInitializer,
						expressionStatements, statement);
			} else {
				expressionStatements.push(statement);
			}
			targetStartPos = getTargetStartPosition(moveTargets);
		}
		while (!expressionStatements.isEmpty()) {
			body.insertFirst(expressionStatements.pop(), null);

		}
	}

	private void identifyVariableDeclaration(
			Stack<ASTNode> variableDeclarations,
			Stack<ASTNode> variableDeclarationEndWithLiteralOrNullInitializer,
			Stack<ASTNode> expressionStatements, ASTNode statement) {
		VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
		AST rootAST = compilationUnit.getAST();
		List<?> fragments = variableDeclarationStatement.fragments();
		VariableDeclarationFragment declarationFragment = (VariableDeclarationFragment) (fragments
				.get(fragments.size() - 1));
		Expression initializer = declarationFragment.getInitializer();
		if (initializer != null
				&& !initializer.getClass().getName().endsWith("Literal")) {
			separateVariableDeclatrionIntoVariableAssignmentAndExpressionStatement(
					variableDeclarations, expressionStatements,
					variableDeclarationStatement, rootAST, fragments,
					initializer);
		} else {
			ASTNode target = astRewrite
					.createMoveTarget(variableDeclarationStatement);
			variableDeclarationEndWithLiteralOrNullInitializer.push(target);
		}
	}

	private void separateVariableDeclatrionIntoVariableAssignmentAndExpressionStatement(
			Stack<ASTNode> variableDeclarations,
			Stack<ASTNode> expressionStatements,
			VariableDeclarationStatement variableDeclarationStatement,
			AST rootAST, List<?> fragments, Expression initializer) {
		for (Object node : fragments) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) node;
			VariableDeclarationFragment newFragment = rootAST
					.newVariableDeclarationFragment();
			newFragment.setName(rootAST.newSimpleName(fragment.getName()
					.toString()));
			if (variableDeclarationStatement.getType().getNodeType() != ASTNode.PRIMITIVE_TYPE) {
				newFragment.setInitializer(rootAST.newNullLiteral());
			}
			ListRewrite variableDeclarationRewrite = astRewrite.getListRewrite(
					variableDeclarationStatement,
					VariableDeclarationStatement.FRAGMENTS_PROPERTY);
			variableDeclarationRewrite.replace(fragment, newFragment, null);
			ASTNode variableDeclaration = astRewrite
					.createMoveTarget(variableDeclarationStatement);
			variableDeclarations.push(variableDeclaration);

			Assignment assignment = rootAST.newAssignment();
			assignment.setOperator(Assignment.Operator.ASSIGN);
			assignment.setLeftHandSide(rootAST.newSimpleName(fragment.getName()
					.toString()));
			ASTNode copyNode = ASTNode.copySubtree(initializer.getAST(),
					initializer);
			assignment.setRightHandSide((Expression) copyNode);
			ExpressionStatement exp = rootAST
					.newExpressionStatement(assignment);
			expressionStatements.push(exp);
		}
	}

	private int getTargetStartPosition(Queue<ASTNode> moveTargets) {
		ASTNode top = moveTargets.peek();
		if (top != null) {
			Object nextStarPos = top.getProperty("startPosition");
			if (nextStarPos != null) {
				return (Integer) nextStarPos;
			} else {
				return moveTargets.peek().getStartPosition();
			}
		}
		return -1;
	}

	private void moveReturnAndThrowStatementToTheLastOfTryStatement(
			TryStatement tryStatement) {
		List<Statement> statementInTryStatement = tryStatement.getBody()
				.statements();
		ListRewrite body = astRewrite.getListRewrite(tryStatement.getBody(),
				Block.STATEMENTS_PROPERTY);
		for (ASTNode node : statementInTryStatement) {
			if (node.getNodeType() == ASTNode.THROW_STATEMENT
					|| node.getNodeType() == ASTNode.RETURN_STATEMENT) {
				body.insertLast(body.createMoveTarget(node, node), null);
			}
		}
	}

	private void appendCatchClause(TryStatement tryStatement) {
		AST ast = compilationUnit.getAST();
		// generate a Try statement
		ListRewrite catchRewrite = astRewrite.getListRewrite(tryStatement,
				TryStatement.CATCH_CLAUSES_PROPERTY);
		// generate Catch Clause
		@SuppressWarnings("unchecked")
		CatchClause cc = ast.newCatchClause();
		ListRewrite newCreateCatchRewrite = astRewrite.getListRewrite(
				cc.getBody(), Block.STATEMENTS_PROPERTY);
		// set the exception type will be caught. ex. catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Throwable")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		StringBuffer comment = new StringBuffer();
		comment.append("ex.printStackTrace();\n");
		comment.append("PropertyConfigurator.configure(\"log4j.properties\");\n");
		comment.append("logger.info(").append("ex);");
		ASTNode placeHolder = astRewrite.createStringPlaceholder(
				comment.toString(), ASTNode.EMPTY_STATEMENT);
		newCreateCatchRewrite.insertLast(placeHolder, null);
		catchRewrite.insertLast(cc, null);
	}

	private void moveAllStatementInTryStatement(
			MethodDeclaration methodDeclaration,
			TryStatement tryStatementCreatedByQuickFix) {
		/* move all statements of method in try block */
		ListRewrite tryStatement = astRewrite.getListRewrite(
				tryStatementCreatedByQuickFix.getBody(),
				Block.STATEMENTS_PROPERTY);
		ListRewrite neededToBeRefactoredMethodBody = astRewrite.getListRewrite(
				methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
		List<ASTNode> statements = methodDeclaration.getBody().statements();
		for (ASTNode statement : statements) {
			ASTNode target = astRewrite.createMoveTarget(statement);
			tryStatement.insertLast(target, null);
		}
		neededToBeRefactoredMethodBody.insertLast(
				tryStatementCreatedByQuickFix, null);
	}

	private TryStatement createTryCatchStatement(
			MethodDeclaration methodDeclaration) {
		AST ast = compilationUnit.getAST();
		// generate a Try statement
		TryStatement bigOuterTryStatement = ast.newTryStatement();

		// generate Catch Clause
		@SuppressWarnings("unchecked")
		List<CatchClause> catchStatement = bigOuterTryStatement.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = astRewrite.getListRewrite(cc.getBody(),
				Block.STATEMENTS_PROPERTY);
		// set the exception type will be caught. ex. catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Throwable")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		StringBuffer catchContent = new StringBuffer();
		catchContent.append("PropertyConfigurator.configure(\"log4j.properties\");\n");
		catchContent.append("logger.info(").append("ex);");
		ASTNode placeHolder = astRewrite.createStringPlaceholder(
				catchContent.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);

		return bigOuterTryStatement;
	}

	public Statement getReleaseMethodInvocation(
			MethodDeclaration methodDeclaration, int badSmellLineNumber) {
		List<ASTNode> statements = methodDeclaration.getBody().statements();
		Statement candidate = null;
		for (ASTNode ast : statements) {
			if(ast.getNodeType() == ASTNode.TRY_STATEMENT){
				List<ASTNode> statementsss = ((TryStatement)ast).getBody().statements();
				for (ASTNode astintry : statementsss){
					int lineNumberOfCloseInvocation = getStatementLineNumber(astintry);
					if (lineNumberOfCloseInvocation == badSmellLineNumber) {
						candidate = (Statement) astintry;
						break;
					}
				}
			}
			int lineNumberOfCloseInvocation = getStatementLineNumber(ast);
			if (lineNumberOfCloseInvocation == badSmellLineNumber) {
				candidate = (Statement) ast;
				break;
			}
		}
		return candidate;
	}

	public int getStatementLineNumber(ASTNode node) {
		int lineNumberOfTryStatement = compilationUnit.getLineNumber(node
				.getStartPosition());
		return lineNumberOfTryStatement;
	}

	public int getBadSmellLineNumberFromMarker(IMarker marker) {
		int badSmellLineNumber = 0;
		try {
			badSmellLineNumber = (Integer) marker
					.getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return badSmellLineNumber;
	}

	public void quickFixMethodDeclarationWithoutTryStmt(
			MethodDeclaration methodDeclaration) {
		
		int badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);
		Statement releaseMethod = getReleaseMethodInvocation(methodDeclaration,
				badSmellLineNumber);
		
		TryStatement tryStatement = createTryFinallyStatement(methodDeclaration, releaseMethod);
		removeReleaseMethod(methodDeclaration, releaseMethod);
		
		moveIntoTryStmt(methodDeclaration, tryStatement);	
	}

	
	private void removeReleaseMethod(MethodDeclaration methodDeclaration,
			Statement closeMethod) {
		ListRewrite modifyingClose = astRewrite.getListRewrite( 
				methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
		ExpressionStatementStringFinderVisitor expressionStatementFinder = new ExpressionStatementStringFinderVisitor(
				closeMethod.toString());
		closeMethod.accept(expressionStatementFinder);
		ASTNode removeNode = expressionStatementFinder
				.getFoundExpressionStatement();
		modifyingClose.remove(removeNode, null);
	}

	private TryStatement createTryFinallyStatement(
			MethodDeclaration methodDeclaration, Statement closeMethod) {
		// generate a Try statement
		AST ast = compilationUnit.getAST();
		TryStatement bigOuterTryStatement = ast.newTryStatement();
		Block block = ast.newBlock();
		bigOuterTryStatement.setFinally(block);
		
		//parse releaseMethod exception type and variable name
		MethodInvocationCollectorVisitor visitor = new MethodInvocationCollectorVisitor();
		closeMethod.accept(visitor);
		MethodInvocation releaseMethodInvocation = visitor.getMethodInvocations().get(0);
		String exceptionType = releaseMethodInvocation.resolveMethodBinding().getExceptionTypes()[0].getName();
		String objName = closeMethod.toString().substring(0,closeMethod.toString().indexOf("."));
		
		//prepare new tryStatement in finally block
		TryStatement tryStmtInFinally = ast.newTryStatement();
		//set try config of tryStatement at finally： check object is null or not. 
		ListRewrite finallyListRewrite = astRewrite.getListRewrite(tryStmtInFinally.getBody(), Block.STATEMENTS_PROPERTY);
		
		//add release method into try block in finally
		ASTNode releaseplaceHolder = astRewrite.createStringPlaceholder(closeMethod.toString().replaceAll("\n", ""), ASTNode.EMPTY_STATEMENT);
		finallyListRewrite.insertLast(releaseplaceHolder, null);

		//set catch config of tryStatement at finally
		CatchClause catchClauseInFinally = ast.newCatchClause();
		ListRewrite catchClauseListRewriteInFinally = astRewrite.getListRewrite(catchClauseInFinally.getBody(), Block.STATEMENTS_PROPERTY);
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName(exceptionType)));
		sv.setName(ast.newSimpleName("e"));
		catchClauseInFinally.setException(sv);
		StringBuffer catchContent = new StringBuffer();
		catchContent.append("/* \n * Although it's a Dummy Handler bad smell,\n" 
				+" * Robusta recommends that you keep this bad smell\n"
				+" * instead of choosing Quick Fix or Refactor that we provide.\n */\n"
				+"PropertyConfigurator.configure(\"log4j.properties\");\n"
				+"logger.info(e);");
		ASTNode catchContentPlaceHolder = astRewrite.createStringPlaceholder(catchContent.toString(), ASTNode.EMPTY_STATEMENT);
		catchClauseListRewriteInFinally.insertLast(catchContentPlaceHolder, null);
		tryStmtInFinally.catchClauses().add(catchClauseInFinally);

		ListRewrite listRewrite = astRewrite.getListRewrite(
				bigOuterTryStatement.getFinally(), Block.STATEMENTS_PROPERTY);
		listRewrite.insertLast(tryStmtInFinally, null);
		return bigOuterTryStatement;
	}

	public ListRewrite moveIntoTryStmt(MethodDeclaration methodDeclaration,
			TryStatement tryStatementCreatedByQuickFix) {
		ListRewrite tryStatement = astRewrite.getListRewrite(
				tryStatementCreatedByQuickFix.getBody(),
				Block.STATEMENTS_PROPERTY);
		ListRewrite neededToBeRefactoredMethodBody = astRewrite.getListRewrite(
				methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
		List<ASTNode> statements = methodDeclaration.getBody().statements();
		int badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);
		Statement closeMethod = getReleaseMethodInvocation(methodDeclaration,
				badSmellLineNumber);
		String closeMethodName = closeMethod.toString().substring(0,closeMethod.toString().indexOf("."));
		int count = 0;
		ASTNode position = null;
		String leftOperationStatement = "";
		String rightOperationStatement = "";
		for (ASTNode statement : statements) {
			if(statement.getNodeType() == ASTNode.TRY_STATEMENT){
				count++;
				if(count == trystmtIndex){
					position = statement;
					List<ASTNode> statementsss = ((TryStatement)statement).getBody().statements();
					for (ASTNode astintry : statementsss){
						boolean isSameAsCloseMethodName = false;
						if (astintry.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT  ){
							String varDeclareStmt = ((VariableDeclarationStatement)astintry).fragments().get(0).toString();
							if(varDeclareStmt.contains("=")){
								String varDeclareStmtName = varDeclareStmt.substring(0, varDeclareStmt.indexOf("="));
								isSameAsCloseMethodName = varDeclareStmtName.equals(closeMethodName);
							}
							
						}
						if(isSameAsCloseMethodName){
							leftOperationStatement = astintry.toString().substring(0, astintry.toString().indexOf("=") + 1);
							leftOperationStatement += " null;";
							rightOperationStatement = astintry.toString().substring(astintry.toString().indexOf(closeMethodName)).replaceAll("\\n", "");
							StringBuffer insertAssignmentInTry = new StringBuffer();
							insertAssignmentInTry.append(rightOperationStatement);
							ASTNode placeHolder2 = astRewrite.createStringPlaceholder(
									insertAssignmentInTry.toString(), ASTNode.EMPTY_STATEMENT);
							tryStatement.insertLast(placeHolder2, null);
						}
						else if (astintry != closeMethod && !isSameAsCloseMethodName ){
							stmtMovedIntoTry.add(astintry.toString());
							ASTNode target = astRewrite.createMoveTarget(astintry);
							tryStatement.insertLast(target, null);
						}
					}
				}
			}
			//除了宣告的變數和release的method，其餘的都移到trystmt
			else if (statement != closeMethod  && statement.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				stmtMovedIntoTry.add(statement.toString());
				ASTNode target = astRewrite.createMoveTarget(statement);
				tryStatement.insertLast(target, null);
			}
		}
		if(leftOperationStatement.length()!=0 && rightOperationStatement.length()!=0){
			AST ast = compilationUnit.getAST();
			StringBuffer insertVariableDeclarationBeforeTry = new StringBuffer();
			insertVariableDeclarationBeforeTry.append(leftOperationStatement);
			ASTNode placeHolder = astRewrite.createStringPlaceholder(
					insertVariableDeclarationBeforeTry.toString(),
					ASTNode.EMPTY_STATEMENT);
			neededToBeRefactoredMethodBody.insertBefore(placeHolder, position,
					null);
		}
        if(trystmtIndex != 0)
			neededToBeRefactoredMethodBody.insertAfter(tryStatementCreatedByQuickFix, position, null);
		else 
			neededToBeRefactoredMethodBody.insertLast(tryStatementCreatedByQuickFix, null);
		return tryStatement;
	}
	
	public ASTNode removetrystatement(MethodDeclaration methodDeclaration,
			Statement trystatement) {
		ListRewrite modifyingClose = astRewrite.getListRewrite( 
				methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY);
		TryStatementCollectorVisitor tryStatementFinder = new TryStatementCollectorVisitor();
		trystatement.accept(tryStatementFinder);
		ASTNode removeNode = tryStatementFinder.getTryStatements().get(0);
		modifyingClose.remove(removeNode, null);	
		return removeNode;
	}

	public void quickFixMethodDeclarationWithTryStmt(
			MethodDeclaration methodDeclaration , TryStatement target) {
		int badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);
		Statement closeMethod = getReleaseMethodInvocation(methodDeclaration,
				badSmellLineNumber);
		MethodInvocationCollectorVisitor visitor = new MethodInvocationCollectorVisitor(); 
		closeMethod.accept(visitor);
		String exceptionType = getExceptionTypeWhichWillThrow(visitor.getFirstInvocations().get(0));
		generateThrowExceptionOnMethodSignature(methodDeclaration, exceptionType);
		
		
		TryStatement tryStatement = createTryStatement(closeMethod , target);
		removetrystatement(methodDeclaration, (Statement)target);
		if(hasTryStmtButReleaseMethodNotInTry == true)
			removeReleaseMethod(methodDeclaration,closeMethod);
		 moveIntoTryStmt(methodDeclaration, tryStatement).getRewrittenList().get(0).toString();
	}
	private String getExceptionTypeWhichWillThrow(MethodInvocation method) {
		String exceptionTypes = method.resolveMethodBinding()
				.getExceptionTypes()[0].getName();
		return exceptionTypes;
	}

	public void quickFixMethodDeclarationWithTryStmtButReleaseMethodNotInTry(
		MethodDeclaration methodDeclaration , TryStatement target) {
		int badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);
		Statement closeMethod = getReleaseMethodInvocation(methodDeclaration,
				badSmellLineNumber);
		TryStatement tryStatement = createTryStatement(closeMethod , target);
		removeReleaseMethod(methodDeclaration,closeMethod);
		removetrystatement(methodDeclaration, (Statement)target);
		moveIntoTryStmt(methodDeclaration, tryStatement).getRewrittenList().get(0).toString();
	}
	
	public TryStatement createTryStatement(Statement releaseMethodStmt ,TryStatement trystatement) {
		// generate a Try statement
		AST ast = compilationUnit.getAST();
		TryStatement bigOuterTryStatement = ast.newTryStatement();
		// generate a Catch statement
		List<CatchClause> targetCatchStatement = bigOuterTryStatement.catchClauses();
		List<CatchClause> sourceCatchStatement = trystatement.catchClauses();
		for(int i=0 ; i<sourceCatchStatement.size(); i++){
			CatchClause cc = ast.newCatchClause();
			ListRewrite catchRewrite = astRewrite.getListRewrite(cc.getBody(),
					Block.STATEMENTS_PROPERTY);
			String tmp[] =  sourceCatchStatement.get(i).getException().toString().split(" ");
			SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
			sv.setType(ast.newSimpleType(ast.newSimpleName(tmp[0])));
			sv.setName(ast.newSimpleName(tmp[1]));
			cc.setException(sv);
			
			String tmp2 = sourceCatchStatement.get(i).getBody().toString();
			ASTNode placeHolder = astRewrite.createStringPlaceholder(
			tmp2.substring(tmp2.indexOf("{")+1, tmp2.indexOf("}")-1), ASTNode.EMPTY_STATEMENT);
			catchRewrite.insertLast(placeHolder, null);
			targetCatchStatement.add(cc);
		}
		// generate a Catch statement
		Block block = ast.newBlock();
		bigOuterTryStatement.setFinally(block);
		ListRewrite listRewrite = astRewrite.getListRewrite(
				bigOuterTryStatement.getFinally(), Block.STATEMENTS_PROPERTY);
		if(trystatement.getFinally()!=null){
			String tmp2 =  trystatement.getFinally().toString();
			ASTNode placeHolder = astRewrite.createStringPlaceholder(
					tmp2.substring(tmp2.indexOf("{")+1, tmp2.indexOf("}")-1), ASTNode.EMPTY_STATEMENT);
			listRewrite.insertLast(placeHolder, null);
		}
		
		
		
		//parse releaseMethod exception type and variable name
		MethodInvocationCollectorVisitor visitor = new MethodInvocationCollectorVisitor();
		releaseMethodStmt.accept(visitor);
		MethodInvocation releaseMethodInvocation = visitor.getMethodInvocations().get(0);
		String exceptionType = releaseMethodInvocation.resolveMethodBinding().getExceptionTypes()[0].getName();
		String objName = releaseMethodStmt.toString().substring(0,releaseMethodStmt.toString().indexOf("."));
		
		//prepare new tryStatement in finally block
		TryStatement tryStmtInFinally = ast.newTryStatement();
		//set try config of tryStatement at finally： check object is null or not. 
		ListRewrite finallyListRewrite = astRewrite.getListRewrite(tryStmtInFinally.getBody(), Block.STATEMENTS_PROPERTY);
		//add release method into try block in finally
		ASTNode releaseplaceHolder = astRewrite.createStringPlaceholder(releaseMethodStmt.toString().replaceAll("\n", ""), ASTNode.EMPTY_STATEMENT);
		finallyListRewrite.insertLast(releaseplaceHolder, null);

		//set catch config of tryStatement at finally
		CatchClause catchClauseInFinally = ast.newCatchClause();
		ListRewrite catchClauseListRewriteInFinally = astRewrite.getListRewrite(catchClauseInFinally.getBody(), Block.STATEMENTS_PROPERTY);
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName(exceptionType)));
		sv.setName(ast.newSimpleName("e"));
		catchClauseInFinally.setException(sv);
		StringBuffer catchContent = new StringBuffer();
		catchContent.append("/* \n * Although it's a Dummy Handler bad smell,\n" 
						+" * Robusta recommends that you keep this bad smell\n"
						+" * instead of choosing Quick Fix or Refactor that we provide.\n */\n"
						+"PropertyConfigurator.configure(\"log4j.properties\");\n"
						+"logger.info(e);");
		ASTNode catchContentPlaceHolder = astRewrite.createStringPlaceholder(catchContent.toString(), ASTNode.EMPTY_STATEMENT);
		catchClauseListRewriteInFinally.insertLast(catchContentPlaceHolder, null);
		tryStmtInFinally.catchClauses().add(catchClauseInFinally);

		listRewrite.insertLast(tryStmtInFinally, null);
		return bigOuterTryStatement;
	}

	public void setTryIndex(int _trystmtIndex) {
		trystmtIndex  = _trystmtIndex;
	}
	
	public void setHasTryStmtButReleaseMethodNotInTry(){
		hasTryStmtButReleaseMethodNotInTry  = true;
	}
}
