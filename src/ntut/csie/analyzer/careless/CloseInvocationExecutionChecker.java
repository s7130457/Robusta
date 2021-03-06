package ntut.csie.analyzer.careless;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ntut.csie.util.BoundaryChecker;
import ntut.csie.util.CheckedExceptionCollectorVisitor;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * It will check whether any exception would be thrown before the execution of close invocation
 */
public class CloseInvocationExecutionChecker {

	private int startPosition;
	private List<ASTNode> nodesThatMayThrowException = new ArrayList<ASTNode>();
	public List<ASTNode> getASTNodesThatMayThrowExceptionBeforeCloseInvocation(MethodInvocation closeInvocation) {
		startPosition = findStartPosition(closeInvocation);
		ASTNode checkingNode = closeInvocation;
		
		while (startPosition < checkingNode.getStartPosition()) {
			ASTNode unsafeParentNode = getParentNodeThatMayThrowException(checkingNode);
			if(unsafeParentNode != null){
				nodesThatMayThrowException.add(unsafeParentNode);
			}
			List<ASTNode> unsafeSiblingNodes = getSiblingNodeThatMayThrowException(checkingNode);
			if(unsafeSiblingNodes.size() != 0){
				for(ASTNode node : unsafeSiblingNodes){
					CheckedExceptionCollectorVisitor checkExceptionVisitor = new CheckedExceptionCollectorVisitor();
					node.accept(checkExceptionVisitor);
					if(checkExceptionVisitor.getException().size()!=0){
						nodesThatMayThrowException.add(node);
					}
				}
			}
			checkingNode = checkingNode.getParent();
		}
		return nodesThatMayThrowException;
	}

	private List<ASTNode> getSiblingNodeThatMayThrowException(ASTNode checkingNode) {
		List<ASTNode> unsafeSiblingNodeList = new ArrayList<ASTNode>();
		
		// Set the detection area for checkingNode
		BoundaryChecker boundChecker = new BoundaryChecker(startPosition, checkingNode.getStartPosition());
		ASTNode parentNode = checkingNode.getParent();
		if (parentNode.getNodeType() == ASTNode.BLOCK) {
			List<Statement> siblingStatements = ((Block) parentNode).statements();
			
			for(Statement s : siblingStatements) {
				if(boundChecker.isInOpenInterval(s.getStartPosition()) && isUnsafeSiblingStatement(s))
					unsafeSiblingNodeList.add(s);
			}
		}

		return unsafeSiblingNodeList;
	}

	private ASTNode getParentNodeThatMayThrowException(ASTNode checkingNode) {
		if (checkingNode instanceof Statement) {
			ASTNode parent = checkingNode.getParent();
			int parentType = parent.getNodeType();
			boolean isParentBlock = (parentType == ASTNode.BLOCK);
			boolean isParentFinallBlockOrCatchClause = (parentType == ASTNode.TRY_STATEMENT);
			boolean isParentCatchBlock = (parentType == ASTNode.CATCH_CLAUSE);
			boolean isSafeIfStaementExpression = isSafeIfStaementExpression(parent);
			boolean isSafeForStaementExpression = isSafeForStaementExpression(parent);
			boolean isSafeWhileStaementExpression = isSafeWhileStaementExpression(parent);
			boolean isSafeDoStaementExpression = isSafeDoStaementExpression(parent);
			boolean isSafeSwitchStaementExpression = isSafeSwitchStaementExpression(parent);
			boolean isParentSafeSynchronizedStatement = isSynchronizedStatement(parent);

			if(!(isParentBlock || isParentCatchBlock || isParentFinallBlockOrCatchClause || 
					isSafeIfStaementExpression || isParentSafeSynchronizedStatement ||
					isSafeForStaementExpression || isSafeWhileStaementExpression ||
					isSafeDoStaementExpression || isSafeSwitchStaementExpression))
				return parent;
		}
		return null;
	}

	private boolean isSafeSwitchStaementExpression(ASTNode parent) {
		SwitchStatement switchStatement = null;
		Expression expression = null;
		boolean safeSwitchStatement = false;
		if (parent.getNodeType() == ASTNode.SWITCH_STATEMENT) {
			switchStatement = ((SwitchStatement) parent);
			expression = switchStatement.getExpression();
			CheckedExceptionCollectorVisitor checkExpression = new CheckedExceptionCollectorVisitor();
			expression.accept(checkExpression);
			safeSwitchStatement = checkExpression.getException().size()==0;
		}
		return safeSwitchStatement;
	}

	private boolean isSafeDoStaementExpression(ASTNode parent) {
		DoStatement doStatement = null;
		Expression expression = null;
		boolean safeWhileStatement = false;
		if (parent.getNodeType() == ASTNode.DO_STATEMENT) {
			doStatement = ((DoStatement) parent);
			expression = doStatement.getExpression();
			CheckedExceptionCollectorVisitor checkExpression = new CheckedExceptionCollectorVisitor();
			expression.accept(checkExpression);
			safeWhileStatement = checkExpression.getException().size()==0;
		}
		return safeWhileStatement;
	}

	private boolean isSafeWhileStaementExpression(ASTNode parent) {
		WhileStatement whileStatement = null;
		Expression expression = null;
		boolean safeWhileStatement = false;
		if (parent.getNodeType() == ASTNode.WHILE_STATEMENT) {
			whileStatement = ((WhileStatement) parent);
			expression = whileStatement.getExpression();
			CheckedExceptionCollectorVisitor checkExpression = new CheckedExceptionCollectorVisitor();
			expression.accept(checkExpression);
			safeWhileStatement = checkExpression.getException().size()==0;
		}
		return safeWhileStatement;
	}

	private boolean isSafeForStaementExpression(ASTNode parent) {
		ForStatement forStatement = null;
		Expression expression = null;
		List<Expression> initializer = null;
		List<Expression> updater =  null;
		boolean isInitializer = true;
		boolean isUpdater = true;
		boolean isExpressionSafe = true;
		
		boolean safeForStatement = false;
		if (parent.getNodeType() == ASTNode.FOR_STATEMENT) {
			forStatement = ((ForStatement) parent);
			expression = forStatement.getExpression();
			initializer = forStatement.initializers();
			updater = forStatement.updaters();
			CheckedExceptionCollectorVisitor checkExpression = new CheckedExceptionCollectorVisitor();
			if(expression!=null){
				expression.accept(checkExpression);
				isExpressionSafe = checkExpression.getException().size()==0;
			}
			CheckedExceptionCollectorVisitor checkInitializer = new CheckedExceptionCollectorVisitor();
			if(initializer.size()>0){
				initializer.get(0).accept(checkInitializer);
				isInitializer = checkInitializer.getException().size()==0;
			}
			CheckedExceptionCollectorVisitor checkUpdater = new CheckedExceptionCollectorVisitor();
			if(updater.size()>0){
				updater.get(0).accept(checkUpdater);
				isUpdater = checkUpdater.getException().size()==0;
			}
			return isExpressionSafe && isInitializer && isUpdater;
		}
		return safeForStatement;
	}

	private int findStartPosition(MethodInvocation methodInvocation) {
		return new ClosingResourceBeginningPositionFinder().findPosition(methodInvocation);
	}

	private boolean isExtendOperandElementSafe(
			List<Boolean> checkExtendOperandSafe) {
		Iterator<Boolean> iter = checkExtendOperandSafe.iterator();
		while (iter.hasNext()) {
			boolean statementsituation = iter.next();
			if (!statementsituation) {
				return false;
			}
		}
		return true;
	}

	private boolean isSafeIfStaementExpression(ASTNode parent) {
		IfStatement ifStatement = null;
		Expression expression = null;
		boolean safeIfStatement = false;
		if (parent.getNodeType() == ASTNode.IF_STATEMENT) {
			ifStatement = ((IfStatement) parent);
			expression = ifStatement.getExpression();
			CheckedExceptionCollectorVisitor checkExpression = new CheckedExceptionCollectorVisitor();
			expression.accept(checkExpression);
			safeIfStatement = checkExpression.getException().size()==0;
		}
		return safeIfStatement;
	}

	private boolean isSynchronizedStatement(ASTNode parent) {
		if (parent.getNodeType() == ASTNode.SYNCHRONIZED_STATEMENT) {
			SynchronizedStatement synchronizedStatement = ((SynchronizedStatement) parent);
			Expression expression = synchronizedStatement.getExpression();
			return expression.getNodeType() == ASTNode.SIMPLE_NAME;
		}
		return false;
	}
	

	private boolean isSafeSiblingSynchronizedStatement(Statement statement) {
		if(statement.getNodeType() == ASTNode.SYNCHRONIZED_STATEMENT) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
			
			for(Object obj : synchronizedStatement.getBody().statements()) {
				Statement s = (Statement) obj;
				if(isUnsafeSiblingStatement(s))
					return false;
			}
			return true;
		}
		return false;
	}

	private boolean isSafeOperand(ASTNode operand) {
		if (operand.getNodeType() == ASTNode.INFIX_EXPRESSION) {
			return isSafeInInFixExpressionInOperand(operand);
		}
		if (operand.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
			return isSafePrefixExpressionInIfstatement(operand);
		}
		if (operand.getNodeType() == ASTNode.SIMPLE_NAME) {
			return true;
		}
		if (operand.getClass().getName().endsWith("Literal")) {
			return true;
		}
		if (operand.getNodeType() == ASTNode.QUALIFIED_NAME) {
			return true;
		}
		if (operand.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) operand;
			Expression expressionOfParenthesizedExpression = parenthesizedExpression
					.getExpression();
			return isExpressionSafe(expressionOfParenthesizedExpression);
		}
		return false;
	}

	private boolean isSafeInInFixExpressionInOperand(ASTNode parent) {
		InfixExpression infix = (InfixExpression) parent;
		ASTNode rightOperand = infix.getRightOperand();
		ASTNode leftOperand = infix.getLeftOperand();
		return (isSafeOperand(rightOperand) && isSafeOperand(leftOperand));
	}

	private boolean isSafeInfixExpressionInIfstatement(Expression expression) {
		InfixExpression infix = (InfixExpression) expression;
		ASTNode rightOperand = infix.getRightOperand();
		ASTNode leftOperand = infix.getLeftOperand();
		List<ASTNode> extendOperand = infix.extendedOperands();
		List<Boolean> checkExtendOperandSafe = checkExtendOperandInInFixStatement(extendOperand);
		return (isSafeOperand(rightOperand) && isSafeOperand(leftOperand) && isExtendOperandElementSafe(checkExtendOperandSafe));
	}

	private boolean isSafePrefixExpressionInIfstatement(ASTNode expression) {
		PrefixExpression prefix = (PrefixExpression) expression;
		ASTNode operand = prefix.getOperand();
		return isSafeOperand(operand);
	}

	private List<Boolean> checkExtendOperandInInFixStatement(
			List<ASTNode> extendOperand) {
		List<Boolean> checkExtendOperandSafe = new ArrayList<Boolean>();
		Iterator<ASTNode> iter = extendOperand.iterator();
		while (iter.hasNext()) {
			ASTNode ExtendOperandElement = iter.next();
			checkExtendOperandSafe.add(isSafeOperand(ExtendOperandElement));
		}
		return checkExtendOperandSafe;
	}

	private boolean isSafeVariableDelarcation(Statement statement) {
		if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
			List<VariableDeclarationFragment> allStatements = variableDeclarationStatement.fragments();
			for (VariableDeclarationFragment fragment : allStatements) {
				if (fragment.getInitializer() == null) {
					return true;
				}
				if (fragment.getInitializer().getClass().getName()
						.endsWith("Literal")) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isLiteralReturnStatement(Statement statement) {
		if (statement.getNodeType() == ASTNode.RETURN_STATEMENT) {
			ReturnStatement returnStatememt = (ReturnStatement) statement;
			if (returnStatememt.getExpression() == null) {
				return true;
			}
			if (returnStatememt.getExpression().getClass().getName()
					.endsWith("Literal")) {
				return true;
			}
		}
		return false;
	}

	private boolean isSafeInStatement(List<Boolean> checkChildStatementSafe) {
		Iterator<Boolean> iter = checkChildStatementSafe.iterator();
		while (iter.hasNext()) {
			boolean statementsituation = iter.next();
			if (statementsituation) {
				return false;
			}
		}
		return true;
	}

	private List<Boolean> checkStatementSafe(ASTNode ifBodyStatement) {
		List<Boolean> checkChildStatementSafe = new ArrayList<Boolean>();
		if (ifBodyStatement.getNodeType() == ASTNode.BLOCK) {
			List<Statement> allStatements = ((Block) ifBodyStatement)
					.statements();
			Iterator<Statement> iter = allStatements.iterator();
			while (iter.hasNext()) {
				Statement statementInIfBody = iter.next();
				checkChildStatementSafe
						.add(isUnsafeSiblingStatement(statementInIfBody));
			}
		}
		return checkChildStatementSafe;
	}

	private boolean isSafeThenStatement(IfStatement statement) {
		ASTNode thenStatement = statement.getThenStatement();
		List<Boolean> checkStatementSafe = checkStatementSafe(thenStatement);
		if (isSafeInStatement(checkStatementSafe)) {
			return true;
		}
		return false;
	}

	private boolean isSafeElseStatement(IfStatement statement) {
		ASTNode elseStatement = statement.getElseStatement();
		if (elseStatement != null) {
			List<Boolean> checkStatementSafe = checkStatementSafe(elseStatement);
			if (isSafeInStatement(checkStatementSafe)) {
				return true;
			} else {
				return false;
			}
		}
		// not all if statement has else statement
 		return true;
	}

	private boolean isSafeElseIfStatement(IfStatement statement) {
		ASTNode elseStatement = statement.getElseStatement();
		if (elseStatement != null) {
			if (elseStatement.getNodeType() == ASTNode.IF_STATEMENT) {
				IfStatement elseIfstatement = (IfStatement) elseStatement;
				if (isSafeThenStatement(elseIfstatement)
						&& isSafeElseStatement(elseIfstatement)) {
					return true;
				} else {
					return false;
				}
			}
		}
		// not all if statement has else if statement
		return true;
	}

	private boolean isSafeIfStatement(Statement statement) {
		ASTNode sibilingStatement = (ASTNode) statement;
		if (isSafeIfStaementExpression(sibilingStatement)) {
			IfStatement ifstatement = (IfStatement) sibilingStatement;
			if (isSafeThenStatement(ifstatement)
					&& isSafeElseStatement(ifstatement)
					&& isSafeElseIfStatement(ifstatement)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSafeTryCatchStatement(Statement statement) {
		ASTNode sibilingStatement = (ASTNode) statement;
		if (sibilingStatement.getNodeType() == ASTNode.TRY_STATEMENT) {
			TryStatement tryStatement = (TryStatement) sibilingStatement;

			// a visitor to collect all checked exceptions that might be thrown from the try block. 
			CheckedExceptionCollectorVisitor cecv = new CheckedExceptionCollectorVisitor();
			for(Object obj : tryStatement.getBody().statements()) {
				Statement s = (Statement) obj;
				s.accept(cecv);
			}

			List<ITypeBinding> thrownExceptions = cecv.getException();
			List<CatchClause> catchClauseList = tryStatement.catchClauses();
			
			return (isAllCatchBlockSafe(catchClauseList) && isAllExceptionCaught(thrownExceptions, catchClauseList));
		}
		return false;
	}

	private boolean isAllExceptionCaught(List<ITypeBinding> thrownExceptions, List<CatchClause> catchClauseList) {
		ArrayList<ITypeBinding> caughtExceptions = new ArrayList<ITypeBinding>();
		
		// compare the lists using an utility method
		for(ITypeBinding exception : thrownExceptions) {
			for(CatchClause catchClause : catchClauseList) {
				if(NodeUtils.isExceptionCatught(exception, catchClause)) {
					caughtExceptions.add(exception);
					// an exception can be caught by more than one catch clause; thus,
					// to avoid record the same exception multiple times.
					break;
				}
			}
		}
		return caughtExceptions.size() == thrownExceptions.size();
	}

	private boolean isAllCatchBlockSafe(List<CatchClause> catchClauseList) {
		for (CatchClause catchClause : catchClauseList) {
			List<Statement> StatementList = catchClause.getBody().statements();
			boolean allInBlockStatementSafe = true;
			if (StatementList.isEmpty()) {
				return true;
			}
			for (Statement suspectStatement : StatementList) {
				if(isUnsafeSiblingStatement(suspectStatement)){
					allInBlockStatementSafe = false;
				}
			}
			return allInBlockStatementSafe;
		}
		return false;
	}

	private boolean isSafeExpressionStatement(Statement statement) {
		ASTNode sibilingStatement = (ASTNode) statement;
		if (sibilingStatement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			ExpressionStatement ExpressionExpression = (ExpressionStatement) sibilingStatement;
			Expression expression = ExpressionExpression.getExpression();
			if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment = (Assignment) expression;
				return isExpressionSafe(assignment.getLeftHandSide()) && isExpressionSafe(assignment.getRightHandSide());
			}
			if (expression.getNodeType() == ASTNode.POSTFIX_EXPRESSION) {
				return true;
			}
			if (expression.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
				return true;
			}
			if(expression.getNodeType() == ASTNode.METHOD_INVOCATION){
				CheckedExceptionCollectorVisitor cecv = new CheckedExceptionCollectorVisitor();
				expression.accept(cecv);
				if(cecv.getException().size() == 0){
					return true;
				}
			}
		}
		return false;
	}

	private boolean isExpressionSafe(Expression expression) {
		if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.QUALIFIED_NAME) {
			return true;
		}
		if (expression.getClass().getName().endsWith("Literal")) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment = (Assignment) expression;
			return isExpressionSafe(assignment.getLeftHandSide()) && isExpressionSafe(assignment.getRightHandSide());
		}
		if (expression.getNodeType() == ASTNode.INFIX_EXPRESSION) {
			return isSafeInfixExpressionInIfstatement(expression);
		}
		if (expression.getNodeType() == ASTNode.POSTFIX_EXPRESSION) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
			Expression expressionOfParenthesizedExpression = parenthesizedExpression.getExpression();
			return isExpressionSafe(expressionOfParenthesizedExpression);
		}
		return false;
	}

	/**
	 * Returns false only if the statements will not throw any exception 100%.
	 */
	private boolean isUnsafeSiblingStatement(Statement statement) {
		if (statement.getNodeType() == ASTNode.EMPTY_STATEMENT) {
			return false;
		}
		if (statement.getParent().getNodeType() == ASTNode.TRY_STATEMENT) {
			return false;
		}
		if (isSafeVariableDelarcation(statement)) {
			return false;
		}
		if (isLiteralReturnStatement(statement)) {
			return false;
		}
		if (isSafeExpressionStatement(statement)) {
			return false;
		}
		if (isSafeIfStatement(statement)) {
			return false;
		}
		if (isSafeTryCatchStatement(statement)) {
			return false;
		}
		if (isSafeSiblingSynchronizedStatement(statement)) {
			return false;
		}
		return true;
	}
}
