package ntut.csie.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SuppressWarningVisitorTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	SuppressWarningVisitor visitor;

	@Before
	public void setUp() throws Exception {
		javaFileToString = new JavaFileToString();
		javaFileToString.read(SuppressWarningExampleForAnalyzer.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker = new JavaProjectMaker("SuppressWarningTest");
		javaProjectMaker.setJREDefaultContainer();
		
		javaProjectMaker.packageAgileExceptionClassesToJarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		
		javaProjectMaker.createJavaFile(
				SuppressWarningExampleForAnalyzer.class.getPackage().getName(),
				SuppressWarningExampleForAnalyzer.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ SuppressWarningExampleForAnalyzer.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedMainProgramWithoutTryExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutTryExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramWithoutTryExample.class.getPackage().getName() + ";\n"
				+ javaFileToString.getFileContent());
		javaFileToString.clear();
		
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.activateAllConditionsIfNotConfugured(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(SuppressWarningExampleForAnalyzer.class, javaProjectMaker.getProjectName()));
		
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		
		visitor = new SuppressWarningVisitor(compilationUnit);
	}
	
	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testAddSuppressWarningOnMethod() {
		MethodDeclaration method = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "withSuppressWaringDummyHandlerOnMethod");
		method.accept(visitor);
		List<SSMessage> ssList = visitor.getSuppressWarningList();
		assertEquals(1, ssList.size());
		assertFalse(ssList.get(0).isInsideCatchStatement());
		assertEquals(compilationUnit.getLineNumber(method.getStartPosition()), ssList.get(0).getLineNumber());
		assertEquals(method.getStartPosition(), ssList.get(0).getPosition());
		assertEquals(1, ssList.get(0).getSmellList().size());
		assertEquals("Dummy_Handler", ssList.get(0).getSmellList().get(0));
	}
	
	@Test
	public void testAddSuppressWarningOnCatch() {
		MethodDeclaration method = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "withSuppressWaringDummyHandlerOnCatch");
		method.accept(visitor);
		List<SSMessage> ssList = visitor.getSuppressWarningList();
		List<TryStatement> tryList = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(compilationUnit, "withSuppressWaringDummyHandlerOnCatch");
		CatchClause cc = (CatchClause)tryList.get(0).catchClauses().get(0);
		assertEquals(1, ssList.size());
		assertTrue(ssList.get(0).isInsideCatchStatement());
		assertEquals(compilationUnit.getLineNumber(cc.getStartPosition()), ssList.get(0).getLineNumber());
		assertEquals(cc.getStartPosition(), ssList.get(0).getPosition());
		assertEquals(1, ssList.get(0).getSmellList().size());
		assertEquals("Dummy_Handler", ssList.get(0).getSmellList().get(0));
	}
	
	@Test
	public void testAddSuppressWarningOnBoth() {
		MethodDeclaration method = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "withSuppressWaringNestedTryStatementOnMethod");
		method.accept(visitor);
		List<SSMessage> ssList = visitor.getSuppressWarningList();
		assertEquals(2, ssList.size());
		assertEquals(2, ssList.get(0).getSmellList().size());
		assertEquals("Nested_Try_Statement", ssList.get(0).getSmellList().get(0));
		assertEquals("Dummy_Handler", ssList.get(0).getSmellList().get(1));
		assertEquals("Dummy_Handler", ssList.get(1).getSmellList().get(0));
	}
	
	@Test
	public void testAddSuppressWarningOnBothWithMutliCases() {
		MethodDeclaration method = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "theFourthOrderInTheSameClass");
		method.accept(visitor);
		List<SSMessage> ssList = visitor.getSuppressWarningList();
		assertEquals(8, ssList.size());
		assertEquals(2, ssList.get(0).getSmellList().size());
		assertFalse(ssList.get(0).isInsideCatchStatement());
		assertEquals("Careless_Cleanup", ssList.get(0).getSmellList().get(0));
		assertEquals("Nested_Try_Statement", ssList.get(0).getSmellList().get(1));
		assertEquals(1, ssList.get(1).getSmellList().size());
		assertEquals("Nested_Try_Statement", ssList.get(1).getSmellList().get(0));
		assertEquals(2, ssList.get(2).getSmellList().size());
		assertEquals("Nested_Try_Statement", ssList.get(2).getSmellList().get(0));
		assertEquals("Empty_Catch_Block", ssList.get(2).getSmellList().get(1));
		assertEquals(1, ssList.get(3).getSmellList().size());
		assertEquals("Dummy_Handler", ssList.get(3).getSmellList().get(0));
	}
	
	@Test
	public void testAddSuppressWarningOnNothing() {
		MethodDeclaration method = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "withoutSuppressWaringCarelessCleanup");
		method.accept(visitor);
		List<SSMessage> ssList = visitor.getSuppressWarningList();
		assertEquals(0, ssList.size());
	}
}
