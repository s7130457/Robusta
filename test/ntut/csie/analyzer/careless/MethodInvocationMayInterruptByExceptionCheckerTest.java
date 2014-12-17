package ntut.csie.analyzer.careless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MethodInvocationMayInterruptByExceptionCheckerTest {

	TestEnvironmentBuilder environmentBuilder;
	CompilationUnit compilationUnit;
	MethodInvocationMayInterruptByExceptionChecker checker;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder();
		environmentBuilder.createEnvironment();

		environmentBuilder
				.loadClass(MethodInvocationMayInterruptByExceptionCheckerExample.class);

		compilationUnit = environmentBuilder
				.getCompilationUnit(MethodInvocationMayInterruptByExceptionCheckerExample.class);

		checker = new MethodInvocationMayInterruptByExceptionChecker();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testIsMayInterruptByExceptionWithCloseResourceByInvokeMyClose()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeResourceByInvokeMyClose", "this.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));

		methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeResourceByInvokeMyClose", "close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithInvokeGetResourceAndCloseItWithX()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseItWithInterface",
				"resourceManager.getResourceWithInterface().close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));

		methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseItNotImpCloseable",
				"resourceManager.getResourceNotImpCloseable().close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithUserDefinedClosedMethodWithCloseableArgument()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeByUserDefinedMethod",
				"ResourceCloser.closeResourceDirectly(is)");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithResourceJustBeCreated()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"createAndCloseDirectlyWithNewFile", "fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithInTryBlock() throws Exception {
		// First "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 2055 - 1, 24);
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithInCatchBlock()
			throws Exception {
		// Second "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 2124 - 1, 24);
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithFirstStatementInCatchBlock()
			throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 2220 - 1, 21);
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithIntDeclare() throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation intDeclare = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 2445 - 1, 11);
		assertFalse(checker.isMayInterruptByException(intDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithStrtingDeclare()
			throws Exception {
		MethodInvocation strtingDeclare = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 2621 - 1, 11);
		assertFalse(checker.isMayInterruptByException(strtingDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithCharDeclare() throws Exception {
		MethodInvocation charDeclare = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 2790 - 1, 11);
		assertFalse(checker.isMayInterruptByException(charDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithbooleanDeclare()
			throws Exception {
		MethodInvocation booleanDeclare = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 2968 - 1, 11);
		assertFalse(checker.isMayInterruptByException(booleanDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithIntAssignt() throws Exception {
		MethodInvocation intAssign = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 3137 - 1, 11);
		assertFalse(checker.isMayInterruptByException(intAssign));
	}

	@Test
	public void testIsMayInterruptByExceptionWithStrtingAssign()
			throws Exception {
		MethodInvocation strtingAssign = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 3322 - 1, 11);
		assertFalse(checker.isMayInterruptByException(strtingAssign));
	}

	@Test
	public void testIsMayInterruptByExceptionWithCharAssign() throws Exception {
		MethodInvocation charAssign = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 3495 - 1, 11);
		assertFalse(checker.isMayInterruptByException(charAssign));
	}

	@Test
	public void testIsMayInterruptByExceptionWithBooleanAssign()
			throws Exception {
		MethodInvocation booleanAssign = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 3678 - 1, 11);
		assertFalse(checker.isMayInterruptByException(booleanAssign));
	}

	@Test
	public void testIsMayInterruptByExceptionWithSpectIntDeclare()
			throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation intDeclare = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 3910 - 1, 11);
		assertTrue(checker.isMayInterruptByException(intDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithSpectStrtingDeclare()
			throws Exception {
		MethodInvocation strtingDeclare = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 4122 - 1, 11);
		assertTrue(checker.isMayInterruptByException(strtingDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithSpectCharDeclare()
			throws Exception {
		MethodInvocation charDeclare = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 4325 - 1, 11);
		assertTrue(checker.isMayInterruptByException(charDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithsuSpectVariableDeclarationOrAssignment()
			throws Exception {
		MethodInvocation booleandeclare = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 4524 - 1, 11);
		assertTrue(checker.isMayInterruptByException(booleandeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithObjectDeclare()
			throws Exception {
		MethodInvocation objectDeclare = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 4736 - 1, 11);
		assertFalse(checker.isMayInterruptByException(objectDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithObjectNullDeclare()
			throws Exception {
		MethodInvocation objectNullDeclare = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 4947 - 1, 11);
		assertFalse(checker.isMayInterruptByException(objectNullDeclare));
	}

	@Test
	public void testIsMayInterruptByExceptionWithSpecialObjectNullDeclare()
			throws Exception {
		MethodInvocation specialObjectNullDeclare = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 5174 - 1, 11);
		assertFalse(checker.isMayInterruptByException(specialObjectNullDeclare));
	}

	private MethodInvocation getMethodInvocationByMethodNameAndCode(
			String methodName, String code) {
		List<MethodInvocation> methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						methodName, code);
		assertEquals(methodInvocation.size(), 1);

		return methodInvocation.get(0);
	}
}