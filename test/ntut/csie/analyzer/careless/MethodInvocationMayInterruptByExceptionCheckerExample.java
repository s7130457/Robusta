package ntut.csie.analyzer.careless;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ntut.csie.analyzer.careless.closingmethod.ResourceCloser;

public class MethodInvocationMayInterruptByExceptionCheckerExample {

	File file = null;
	MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();

	class ClassWithGetResource {
		public ClassWithGetResource() throws RuntimeException {
		}

		public ClassWithGetResource getResourceNotImpCloseable() {
			return this;
		}

		public void close() throws IOException {
		}

		public void closeResourceByInvokeMyClose() throws Exception {
			this.close(); // Is not
			close(); // Is
		}
	}

	class closableResourceContainsVariable implements Closeable {
		public boolean a = true;

		public void close() {
			// do something
		}

		public boolean get() {
			return this.a;
		}
	}

	public void testResourceClosingInTheCheckingQualifiedNameSameIfStatement()
			throws IOException {
		closableResourceContainsVariable qualifier = new closableResourceContainsVariable();
		if (false != qualifier.a) {
			qualifier.close(); // safe
		}
	}

	public void invokeGetResourceAndCloseItNotImpCloseable() throws Exception {
		ClassWithGetResource resourceManager = new ClassWithGetResource();
		resourceManager.getResourceNotImpCloseable().close(); // Is
	}

	public void closeByUserDefinedMethod(OutputStream zOut) throws IOException {
		(new MethodInvocationBeforeClose()).declaredCheckedExceptionOnMethodSignature();
		InputStream is = null;
		try {
			zOut.write(is.read());
		} finally {
			ResourceCloser.closeResourceDirectly(is); // Isn't
		}
	}

	public void createAndCloseDirectlyWithNewFile() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		fis.close(); // Isn't
	}

	public void sameResourceCloseManyTimes(byte[] context, File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close(); // Unsafe
		} catch (IOException e) {
			fileOutputStream.close(); // Safe
			throw e;
		} finally {
			fileOutputStream.flush();
			fileOutputStream.close(); // Unsafe
		}
	}

	public void variableIntDeclaration() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		int intDeclare;
		fis.close();
	}

	public void variableStringDeclaration() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		String strtingDeclare;
		fis.close();
	}

	public void variableCharDeclaration() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		char charDeclare;
		fis.close();
	}

	public void variableBooleanDeclaration() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		boolean booleanDeclare;
		fis.close();
	}

	public void variableIntAssignment() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		int intAssign = 10;
		fis.close();
	}

	public void variableStringAssignment() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		String strtingAssign = "string";
		fis.close();
	}

	public void variableCharAssignment() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		char charAssign = 'a';
		fis.close();
	}

	public void variableBooleanAssignment() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		boolean booleanAssign = true;
		fis.close();
	}

	public void suspectVariableIntDeclarationOrAssignment() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		int intDeclare = returnInt();
		fis.close();
	}

	public void suspectVariableStringDeclarationOrAssignment() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		String strtingDeclare = returnString();
		fis.close();
	}

	public void suspectVariableCharDeclarationOrAssignment() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		char charDeclare = returnChar();
		fis.close();
	}

	public void suspectVariableBooleanDeclarationOrAssignment()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		boolean booleanDeclare = returnBoolean();
		fis.close();
	}

	public void specialVariableDeclaration() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		MethodInvocationBeforeClose sampleMethod;
		fis.close();
	}

	public void specialVariableDeclarationWithNull() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		MethodInvocationBeforeClose sampleMethod = null;
		fis.close();
	}

	public void specialVariableAssignmentWithNull() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		MethodInvocationBeforeClose sampleMethod;
		sampleMethod = null;
		fis.close();
		String a = "123";
	}

	public boolean returnBoolean() {
		return true;
	}

	public int returnInt() {
		return 10;
	}

	public String returnString() {
		return "string";
	}

	public char returnChar() {
		return 'a';
	}

	public void resourceCloseInsideCheckingBooleanIfStatement(boolean pass)
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideInsideComparingBooleanStateIfStatement(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass == true) {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideElseStatementAfterCheckingBooleanIfStatement(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
		} else {
			fis.close();// safe
		}
	}

	public void resourceCloseInsidElseStatementAfterComparingBooleanStateIfStatement(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass == true) {
		} else {
			fis.close();// safe
		}
	}

	public void resourceCloseAfterCheckingBooleanIfStatementContainVariableDeclaration(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
			int a = 10;
			int b;
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingBooleanIfStatementContainMethodInvocation(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
			int a = returnInt();
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterBooleanComparingIfStatementContainVariableDeclaration(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass == true) {
			int a = 10;
			int b;
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCompareBooleanStateIfStatementContainMethodInvocation(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass == true) {
			int a = returnInt();
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndVariableDeclaration(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
			if (pass) {
				int a = 10;
				int b;
			}
		}
		fis.close();// safe
	}

	public void resourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndMethodInvocation(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
			if (pass) {
				int a = returnInt();
			}
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndVariableDeclaration(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
			if (pass == true) {
				int a = 10;
				int b;
			}
		}
		fis.close();// safe
	}

	public void resourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndMethodInvocation(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
			if (pass == true) {
				int a = returnInt();
			}
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterBooleanCheckingIfElseStatementContainVariableDeclaration(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
		} else {
			int a = 10;
			int b;
		}
		fis.close();// safe
	}

	public void resourceCloseAfterBooleanComparingIfElseStatement(boolean pass)
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass == true) {
		} else {
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndVariableDeclare(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
		} else {
			if (pass) {
				int a = 10;
				int b;
			}
		}
		fis.close();// safe
	}

	public void resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndMethodInvocation(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
		} else {
			if (pass) {
				int a = returnInt();
			}
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanComparingIfStatementAndVariableDeclaration(
			boolean pass) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (pass) {
		} else {
			if (pass == true) {
				int a = 10;
				int b;
			}
		}
		fis.close();// safe
	}

	public void resourceCloseInsideCheckingtwoBooleanIfStatement(boolean a,
			boolean b) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b) {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideCheckingThreeAndOperandBooleanIfStatement(
			boolean a, boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b && c && a) {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideCheckingThreeBooleanIfStatement(boolean a,
			boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c) {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideCheckingtwoBooleanIfElseStatement(boolean a,
			boolean b) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b) {
		} else {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideCheckingThreeAndOperandBooleanIfElseStatement(
			boolean a, boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b && c && a) {
		} else {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideCheckingThreeBooleanIfElseStatement(
			boolean a, boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c) {
		} else {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideBooleanComparingIfElseStatement(boolean a,
			boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || !c == c) {
		} else {
			fis.close();// safe
		}
	}

	public void resourceCloseInsideCheckingBooleanIfElseStatement(boolean a,
			boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || returnBoolean()) {
		} else {
			fis.close();// unsafe
		}
	}

	public void resourceCloseAfterCheckingtwoBooleanIfStatement(boolean a,
			boolean b) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b) {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingThreeAndOperandBooleanIfStatement(
			boolean a, boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b && c && a) {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingThreeBooleanIfStatement(boolean a,
			boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c) {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingtwoBooleanIfElseStatement(boolean a,
			boolean b) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b) {
		} else {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingThreeAndOperandBooleanIfElseStatement(
			boolean a, boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b && c && a) {
		} else {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingThreeBooleanIfElseStatement(
			boolean a, boolean b, boolean c) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c) {
		} else {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingTwoBooleanIfStatementContainMethodInvocation(
			boolean a, boolean b) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b) {
			int c = returnInt();
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterCheckingMultiBooleanBooleanIfStatementContainMethodInvocation(
			boolean a, boolean b, boolean c, boolean d) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c && !d) {
			int cc = returnInt();
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterCheckingMultiBooleanBooleanIfStatementContainVariableDeclaration(
			boolean a, boolean b, boolean c, boolean d) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c && !d) {
			int cc = 1;
		}
		fis.close();// safe
	}

	public void resourceCloseInsideCheckingTwoBooleanIfStatementAfterMethodInvocation(
			boolean a, boolean b) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b) {
			int c = returnInt();
			fis.close();// unsafe
		}
	}

	public void resourceCloseInsideCheckingTwoBooleanIfStatementBeforeMethodInvocation(
			boolean a, boolean b) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b) {
			fis.close();// safe
			int c = returnInt();
		}
	}

	public void resourceCloseInsideCheckingMultiBooleanBooleanIfStatementContainMethodInvocation(
			boolean a, boolean b, boolean c, boolean d) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c && !d) {
			int cc = returnInt();
			fis.close();// unsafe
		}
	}

	public void resourceCloseInsideCheckingMultiBooleanBooleanElseStatementContainMethodInvocation(
			boolean a, boolean b, boolean c, boolean d) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c && !d) {
		} else {
			int cc = returnInt();
			fis.close();// unsafe
		}
	}

	public void resourceCloseAfterMultiBooleanOperandCheckingIfStatementContainMultiBooleanOperandCheckingIfStatementAndMethodInvocation(
			boolean a, boolean b, boolean c, boolean d) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c && !d) {
			if (a && b || c && !d) {
				int aa = returnInt();
			}
		}
		fis.close();// unsafe
	}

	public void resourceCloseInsideMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation(
			boolean a, boolean b, boolean c, boolean d) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c && !d) {
		} else {
			if (a && b || c && !d) {
				int as = returnInt();
			}
			fis.close();// safe
		}
	}

	public void resourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation(
			boolean a, boolean b, boolean c, boolean d) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c && !d) {
		} else {
			if (a && b || c && !d) {
				int as = returnInt();
			}
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndVariableDeclaration(
			boolean a, boolean b, boolean c, boolean d) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a && b || c && !d) {
		} else {
			if (a && b || c && !d) {
				int as = 1;
			}
		}
		fis.close();// safe
	}

	public void resourceCloseInThePrefixIfStatement(boolean a) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (!a) {
			fis.close();// safe
		}
	}

	public void resourceCloseInThePrefixElseStatement(boolean a)
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (!a) {
		} else {
			fis.close();// safe
		}
	}

	public void resourceCloseInThePrefixElseIfStatement(boolean a)
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a) {
		} else if (!a) {
			fis.close();// safe
		}
	}

	public void resourceCloseAfterPrefixIfStatement(boolean a) throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (!a) {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterPrefixElseStatement(boolean a)
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (!a) {
		} else {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterPrefixElseIfStatement(boolean a)
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a) {
		} else if (!a) {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingInstanceIsNullIfStatement()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (fis == null) {
		} else {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideIfStatement()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (fis == null) {
			int intDeclare = returnInt();
		} else {
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideElseStatement()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (fis == null) {
		} else {
			int intDeclare = returnInt();
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterCheckingInstanceIsNullElseIfStatement()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (fis == null) {
		} else if (fis == null) {
		}
		fis.close();// safe
	}

	public void resourceCloseAfterCheckingInstanceIsNullElseIfStatementAndAMethodInvocationInside()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (fis == null) {
		} else if (fis == null) {
			int intDeclare = returnInt();
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterCheckingInstanceIsSameIfStatement()
			throws Exception {
		int a = 1;
		int b = 2;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a == b) {
		}
		fis.close();// safe
	}

	public void resourceCloseInCheckingInstanceIsSameIfStatement()
			throws Exception {
		int a = 1;
		int b = 2;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a == b) {
			fis.close();// safe
		}
	}

	public void resourceCloseAfterCheckingInstanceIsSameElseIfStatement()
			throws Exception {
		int a = 1;
		int b = 2;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (fis == null) {
		} else if (a == b) {
		}
		fis.close();// safe
	}

	public void resourceCloseInCheckingInstanceIsSameElseIfStatement()
			throws Exception {
		int a = 1;
		int b = 2;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (fis == null) {
		} else if (a == b) {
			fis.close();// safe
		}
	}

	public void resourceCloseAfterCheckingInstanceIsSameIfStatementAndAMethodInvocationInside()
			throws Exception {
		int a = 1;
		int b = 2;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (a == b) {
			int intDeclare = returnInt();
		}
		fis.close();// unsafe
	}

	public void resourceCloseAfterCheckingInstanceIsSameElseIfStatementAndAMethodInvocationInside()
			throws Exception {
		int a = 1;
		int b = 2;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		if (fis == null) {
		} else if (a == b) {
			int intDeclare = returnInt();
		}
		fis.close();// unsafe
	}

	public void resourceCloseInTheCheckingQualifiedNameSameIfStatement()
			throws IOException {
		closableResourceContainsVariable qualifier = new closableResourceContainsVariable();
		if (false != qualifier.a) {
			qualifier.close(); // safe
		}
	}

	public void resourceCloseInTheCheckingQualifiedNameSameElseIfStatement()
			throws IOException {
		closableResourceContainsVariable qualifier = new closableResourceContainsVariable();
		if (qualifier == null) {
		} else if (false != qualifier.a) {
			qualifier.close(); // safe
		}
	}

	public void resourceCloseAfterTheCheckingQualifiedNameSameIfStatement()
			throws IOException {
		closableResourceContainsVariable qualifier = new closableResourceContainsVariable();
		if (false != qualifier.a) {
		}
		qualifier.close(); // safe
	}

	public void resourceCloseAfterTheCheckingQualifiedNameSameElseIfStatement()
			throws IOException {
		closableResourceContainsVariable qualifier = new closableResourceContainsVariable();
		if (qualifier == null) {
		} else if (false != qualifier.a) {
		}
		qualifier.close(); // safe
	}

	public void resourceCloseAfterTheCheckingQualifiedNameSameIfStatementAndAMethodInvocationInside()
			throws IOException {
		closableResourceContainsVariable qualifier = new closableResourceContainsVariable();
		if (false != qualifier.a) {
			int intDeclare = returnInt();
		}
		qualifier.close(); // unsafe
	}

	public void resourceCloseAfterTheCheckingQualifiedNameSameElseStatementAndAMethodInvocationInside()
			throws IOException {
		closableResourceContainsVariable qualifier = new closableResourceContainsVariable();
		if (false != qualifier.a) {
		} else {
			int intDeclare = returnInt();
		}
		qualifier.close(); // unsafe
	}

	public void resourceCloseAfterTheCheckingQualifiedNameSameElseIfStatementAndAMethodInvocationInside()
			throws IOException {
		closableResourceContainsVariable qualifier = new closableResourceContainsVariable();
		if (qualifier == null) {
		} else if (false != qualifier.a) {
			int intDeclare = returnInt();
		}
		qualifier.close(); // unsafe
	}

	public void resourceCloseAfterVariableAssignmentStatement()
			throws Exception {
		boolean a;
		FileInputStream fis = null;
		fis = new FileInputStream(new File("C:\\123"));
		a = true;
		fis.close();// safe
	}

	public void resourceCloseAfterVariableAssignmenWithInfixExpressionStatement()
			throws Exception {
		boolean a;
		boolean b = true;
		boolean c = false;
		FileInputStream fis = null;
		fis = new FileInputStream(new File("C:\\123"));
		a = b == c;
		fis.close();// safe
	}

	public void resourceCloseAfterVariableAssignmentWithInfixExpressionAndExtandOperandStatement()
			throws Exception {
		boolean a;
		boolean b = true;
		boolean c = true;
		boolean d = true;
		FileInputStream fis = null;
		fis = new FileInputStream(new File("C:\\123"));
		a = b && c && d;
		fis.close();// safe
	}

	public void resourceCloseAfterVariableAssignmentWithParenthesizeExpression()
			throws Exception {
		boolean a;
		boolean b = true;
		boolean c = true;
		boolean d = true;
		FileInputStream fis = null;
		fis = new FileInputStream(new File("C:\\123"));
		a = (b && (c && d));
		fis.close();// safe
	}

	public void resourceCloseAfterMultiVariableAssignmentStatement()
			throws Exception {
		boolean a;
		boolean b;
		boolean c;
		boolean d;
		FileInputStream fis = null;
		fis = new FileInputStream(new File("C:\\123"));
		a = b = c = d = true;
		fis.close();// safe
	}

	public void resourceCloseAfterVariablePrefixExpressionStatement()
			throws Exception {
		int a = 1;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		++a;
		fis.close();// safe
	}

	public void resourceCloseAfterVariablePostfixExpressionStatement()
			throws Exception {
		int a = 1;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		a++;
		fis.close();// safe
	}

	public void resourceCloseInTheSynchronizedStatement() throws Exception {
		Integer a = new Integer(1);
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		synchronized (a) {
			fis.close();// safe
		}
	}

	public void resourceCloseAfterAUnsafeSynchronizedStatement() throws Exception {
		Integer a = new Integer(1);
		FileOutputStream fos = new FileOutputStream(new File("D:\\234"));
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		synchronized (a) {
			try {
				fos.close();
				throw new RuntimeException();
			} catch (IOException e) {
				// ignore exception
			}
		}
		fis.close();// unsafe
	}
	
	public void resourceCloseAfterASafeSynchronizedStatement() throws Exception {
		Integer a = new Integer(1);
		FileOutputStream fos = new FileOutputStream(new File("D:\\234"));
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		synchronized (a) {
			try {
				fos.close();
			} catch (IOException e) {
				// ignore exception
			}
		}
		fis.close();// safe
	}
	
	public void resourceCloseAfterExceptionTryCatchBlock() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		try {
			// do something
		} catch (Exception e) {
			// do something
		}
		fis.close();// safe
	}

	public void resourceCloseAfterNestExceptionIOExceptionTryCatchBlock()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		try {
			// do something
		} catch (Exception e) {
			try {
				if (fis != null) {
					throw new IOException();
				}
			} catch (IOException ex) {
				// do something
			}
		}
		fis.close();// unsafe
	}
	
	public void resourceCloseAfterMethodInvocationTryCatchBlock()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		int a;
		try {
			// do something
		} catch (Exception e) {
			a = returnInt();
		}
		fis.close();// unsafe
	}
	
	public void resourceCloseAfterNestExceptionTryCatchBlock()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		int a = 1;
		try {
			// do something
		} catch (Exception e) {
			try {
				// do something
			} catch (Exception ex) {
				
			}
		}
		fis.close();// safe
	}

	public void resourceCloseAfterThrowableTryCatchBlock() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		try {
			// do something
		} catch (Throwable e) {
			// do something
		}
		fis.close();// safe
	}

	public void resourceCloseAfterIOExceptionTryCatchBlock()
			throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		try {
			throw new IOException();
		} catch (IOException e) {
		}
		fis.close(); //safe
	}
	
	public void resourceCloseAfterTryStatementThatThrowsRuntimeException() throws IOException {
		FileOutputStream fos = new FileOutputStream(new File("D:\\234"));
		FileInputStream fis = new FileInputStream(new File("C:\\123"));;
		try {
			fos.close();
			throw new RuntimeException();
		} catch (IOException e) {
		}
		fis.close(); //unsafe
	}
	
	public void resourceCloseAfterTryStatementThatCatchGenericException() throws IOException {
		FileOutputStream fos = new FileOutputStream(new File("D:\\234"));
		FileInputStream fis = new FileInputStream(new File("C:\\123"));;
		try {
			fos.close();
			if(true)
				throw new RuntimeException();
		} catch (Exception e) {
		}
		fis.close(); //safe
	}
	
	public void resourceCloseAfterTryStatementThatUsesBlanketCatchClause() throws IOException {
		FileOutputStream fos = new FileOutputStream(new File("D:\\234"));
		FileInputStream fis = new FileInputStream(new File("C:\\123"));;
		try {
			fos.close();
			if(true)
				throw new RuntimeException();
		} catch (IOException io) { 
		} catch (Exception e) {
		}
		fis.close(); //safe
	}
	
	public void resourceCloseAfterTryStatementThatCatchesThrowable() throws IOException {
		FileOutputStream fos = new FileOutputStream(new File("D:\\234"));
		FileInputStream fis = new FileInputStream(new File("C:\\123"));;
		try {
			fos.close();
			if(true)
				throw new RuntimeException();
		} catch (IOException io) { 
		} catch (Throwable e) {
		}
		fis.close(); //safe
	}
}