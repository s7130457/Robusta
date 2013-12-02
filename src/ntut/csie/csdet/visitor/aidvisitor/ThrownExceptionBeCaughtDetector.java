package ntut.csie.csdet.visitor.aidvisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

public class ThrownExceptionBeCaughtDetector {
	private ASTNode onlyDetectInThisBlock;
	private List<ITypeBinding> thrownExceptions;

	public ThrownExceptionBeCaughtDetector(Block block) {
		onlyDetectInThisBlock = block;
		thrownExceptions = new ArrayList<ITypeBinding>();
	}

	public boolean isAnyDeclaredExceptionBeenThrowOut(MethodInvocation node) {
		// initialize
		addDeclaredExceptionsToList(NodeUtils.getDeclaredExceptions(node));

		return isAnyOfTheseExceptionsBeenThrowOut(node);
	}

	public boolean isAnyDeclaredExceptionBeenThrowOut(SuperMethodInvocation node) {
		// initialize
		addDeclaredExceptionsToList(NodeUtils.getDeclaredExceptions(node));

		return isAnyOfTheseExceptionsBeenThrowOut(node);
	}

	public boolean isAnyDeclaredExceptionBeenThrowOut(ThrowStatement node) {
		// initialize
		ITypeBinding iTypeBinding = NodeUtils.getExpressionBinding(node);
		if (iTypeBinding != null) {
			thrownExceptions.add(iTypeBinding);
		}

		return isAnyOfTheseExceptionsBeenThrowOut(node);
	}

	public boolean isAnyDeclaredExceptionBeenThrowOut(ClassInstanceCreation node) {
		// initialize
		addDeclaredExceptionsToList(NodeUtils.getDeclaredExceptions(node));

		return isAnyOfTheseExceptionsBeenThrowOut(node);
	}

	private boolean isAnyOfTheseExceptionsBeenThrowOut(ASTNode statement) {
		final int statementStartAt = statement.getStartPosition();

		TryStatement tryStatement = getParentTryStatementWithinRangeToDetect(statement);

		/*
		 * Is these any exception may be thrown out? Go through each parent
		 * which is try statement
		 */
		while (thrownExceptions.size() > 0) {
			/*
			 * These isn't any more catch, but still are some exceptions will be
			 * thrown out
			 */
			if (tryStatement == null) {
				return true;
			}

			/*
			 * Remove the exception already been caught by this try statement
			 */
			if (isPositionInTryBlock(statementStartAt, tryStatement)) {
				removeThrownExceptionWhichBeenCaught(tryStatement);
			}

			// Go to next parent which is try statement
			tryStatement = getParentTryStatementWithinRangeToDetect(tryStatement);
		}
		return false;
	}

	private boolean isPositionInTryBlock(int position, TryStatement tryStatement) {
		Block tryBlock = tryStatement.getBody();
		int tryBlockStartAt = tryBlock.getStartPosition();
		int tryBlockEndAt = tryBlockStartAt + tryBlock.getLength();
		return (tryBlockStartAt <= position) && (position <= tryBlockEndAt);
	}

	/**
	 * @return Return null if it doesn't exist
	 */
	private TryStatement getParentTryStatementWithinRangeToDetect(ASTNode node) {
		TryStatement tryStatement = (TryStatement) NodeUtils
				.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);

		if (tryStatement == null) {
			return null;
		}

		// if the try statement is in the area that should be detected
		if (tryStatement.getStartPosition() >= onlyDetectInThisBlock
				.getStartPosition()
				&& tryStatement.getLength() < onlyDetectInThisBlock.getLength()) {
			return tryStatement;
		}

		return null;
	}

	/**
	 * Remove all exception been caught for this specific TryStatement
	 */
	private void removeThrownExceptionWhichBeenCaught(TryStatement tryStatement) {
		@SuppressWarnings("unchecked")
		List<CatchClause> catchClauses = tryStatement.catchClauses();
		for (CatchClause eachCatchClause : catchClauses) {
			for (Iterator<ITypeBinding> iter = thrownExceptions.iterator(); iter
					.hasNext();) {
				ITypeBinding thrownException = iter.next();
				if (NodeUtils.isITypeBindingExtended(thrownException,
						NodeUtils.getClassFromCatchClause(eachCatchClause))) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * Transform input array to a list, and add it to the thrownExceptions
	 */
	private void addDeclaredExceptionsToList(ITypeBinding[] iTypeBindings) {
		thrownExceptions.addAll(Arrays.asList(iTypeBindings));
	}
}
