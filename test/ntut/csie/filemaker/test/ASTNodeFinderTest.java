package ntut.csie.filemaker.test;

import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.jdt.util.testSampleCode.NodeUtilsTestSample;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ASTNodeFinderTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	String projectName;
	
	@Before
	public void setUp() throws Exception {
		projectName = "NodeUtilsExampleProject";
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFileToString.read(NodeUtilsTestSample.class, "test");
		javaProjectMaker.createJavaFile(NodeUtilsTestSample.class.getPackage().getName(),
				NodeUtilsTestSample.class.getSimpleName() + ".java",
				"package " + NodeUtilsTestSample.class.getPackage().getName() + ";\n"
						+ javaFileToString.getFileContent());
		javaFileToString.clear();
		
		Path ccExamplePath = new Path(
				projectName	+ "/src/" + NodeUtilsTestSample.class.getName().replace(".", "/") + ".java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin
				.getWorkspace().getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}
	
	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testGetNodeFromSpecifiedClass() throws Exception {
		/*
		 * 略述
		 * lineNumber = 1 → 該目標 .java 全部 code
		 * lineNumber method → 整個 method code
		 * lineNumber class → 整個 class code
		 * lineNumber 分號 → null (line number not match)
		 * lineNumber 註解 → null
		 * lineNumber 大括弧 → null (line number not match)
		 * lineNumber 空白行數 → null
		 */
		
		//取得該行內容
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<ASTNode> list = methodCollector.getMethodList();
		MethodDeclaration mDeclaration = (MethodDeclaration)list.get(1);
		ExpressionStatement statement = (ExpressionStatement) mDeclaration.getBody().statements().get(1);
		
		//輸入class指定行數
		int lineNumber = 20;
		
		//case#1:一般情況
		ASTNode astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(ASTNode.EXPRESSION_STATEMENT, astNode.getNodeType());
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
		assertEquals(astNode.toString(), statement.toString());
		
		//case#2:指向註解
		lineNumber = 9;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//case#3:指向method
		lineNumber = 17;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(ASTNode.METHOD_DECLARATION, astNode.getNodeType());
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
		assertEquals(astNode.toString(), mDeclaration.toString());
		
		//case#4:指向空白
		lineNumber = 49;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//case#5:超過該java行數
		lineNumber = 999999999;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//(尚未解決)
		//類似的case有 ";" "{" "}"
		//case#6:指向分號get到node是正確的但是行數不match回傳null
		lineNumber = 46;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
	}
}
