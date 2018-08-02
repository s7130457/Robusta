package ntut.csie.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.thrown.ExceptionThrownFromFinallyBlockVisitor;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitor;
import ntut.csie.aspect.AddAspectsMarkerResolutionForThrowFromFinally;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.failFastUT.Thrown.thrownFromFinallyExample;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class thrownExceptionFromFinallyConfigTest {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnitThrowFromFinally;
	private ExceptionThrownFromFinallyBlockVisitor throwFromFinallyVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> ThrowFromFinallyMarkerInfos;
	private AddAspectsMarkerResolutionForThrowFromFinally ThrowFromFinallyResoluation;
	private IMarker marker;
	private Path ThrowFromFinallyExamplePath;

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		ThrowFromFinallyMarkerInfos = visitCompilationUnitThrowFromFinallyAndGetSmellList();
		setUpMethodDeclarationIndexOfMarkerInfo();
		ThrowFromFinallyResoluation = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		ThrowFromFinallyExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject"
						+ "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE
						+ "/"
						+ PathUtils.dot2slash(thrownFromFinallyExample.class
								.getName())
						+ JavaProjectMaker.JAVA_FILE_EXTENSION);
	}

	private void setUpTestingEnvironment() throws Exception, JavaModelException {
		environmentBuilder = new TestEnvironmentBuilder(
				"AddAspectsMarkerResoluationExampleProject");
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(thrownFromFinallyExample.class);
		compilationUnitThrowFromFinally = environmentBuilder
				.getCompilationUnit(thrownFromFinallyExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodDeclarationIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnitThrowFromFinally.accept(methodCollector);
		for (MarkerInfo m : ThrowFromFinallyMarkerInfos) {
			int methodDeclarationIdx = -1;
			for (MethodDeclaration methodDeclarationBlock : methodCollector.getMethodList()) {
				methodDeclarationIdx++;
				int methodDeclarationSize = methodCollector.getMethodList().size();
				if(m.getLineNumber()<compilationUnitThrowFromFinally.getLineNumber(methodDeclarationBlock.getStartPosition())){
					m.setMethodIndex(methodDeclarationIdx-1);
					break;
				} else if(methodDeclarationBlock == methodCollector.getMethodList().get(methodDeclarationSize-1)) {
					m.setMethodIndex(methodDeclarationSize-1);
					break;
				}

			}
		}
	}

	private IMarker getSpecificMarkerByMarkerInfoIndex(int index, Path filePath) {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin
				.getWorkspace().getRoot().getFile(filePath));
		IMarker tempMarker = null;
		try {
			tempMarker = javaElement.getResource().createMarker("test.test");
			tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, Integer
					.toString(ThrowFromFinallyMarkerInfos.get(index)
							.getMethodIndex()));
			tempMarker.setAttribute(IMarker.LINE_NUMBER, new Integer(
					ThrowFromFinallyMarkerInfos.get(index).getLineNumber()));
			tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE,
					ThrowFromFinallyMarkerInfos.get(index).getCodeSmellType());

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("throw exception");
		}
		return tempMarker;
	}

	private List<MarkerInfo> visitCompilationUnitThrowFromFinallyAndGetSmellList()
			throws JavaModelException {
		throwFromFinallyVisitor = new ExceptionThrownFromFinallyBlockVisitor(
				compilationUnitThrowFromFinally);
		compilationUnitThrowFromFinally.accept(throwFromFinallyVisitor);
		return throwFromFinallyVisitor.getThrownInFinallyList();
	}

	private String readFile(String fileName) throws FileNotFoundException,
			IOException, UnsupportedEncodingException {
		String ReadContent;
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/" + fileName + ".java";
		File file = new File(packages);
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		ReadContent = new String(data, "UTF-8");
 		return ReadContent;
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}
	
	@Test
	public void getTryStatementsTest(){
		String expected = "";
		try {
			expected = readFile("thrownFromFinallyExample");
			expected = expected.substring(
					expected.indexOf("try {"),
					expected.indexOf("private static")).replaceAll(
					"\\s", "");
			expected = expected.substring(0 ,expected.lastIndexOf("}"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getTryStatements().get(0)
				.toString().replaceAll("\\s", "");
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void getTryStatementWillBeInjectTest(){
		String expected = "";
		try {
			expected = readFile("thrownFromFinallyExample");

			expected = expected.substring(
					expected.indexOf("try {"),
					expected.indexOf("private static")).replaceAll(
					"\\s", "");
			expected = expected.substring(0 ,expected.lastIndexOf("}"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getTryStatementWillBeInject()
				.toString().replaceAll("\\s", "");
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void getExceptionTypeFirstTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getExceptionType();
		Assert.assertEquals("IOException", actual);
	}
	
	@Test
	public void getExceptionTypeSecondTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(1,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getExceptionType();
		Assert.assertEquals("IOException", actual);
	}
	
	@Test
	public void getExceptionTypeThirdTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(2,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getExceptionType();
		Assert.assertEquals("SQLException", actual);
	}
	
	@Test
	public void getMethodInFinalFirstTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getMethodInFinal();
		Assert.assertEquals("close", actual);
	}
	
	@Test
	public void getMethodInFinalSecondTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(1,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getMethodInFinal();
		Assert.assertEquals("close", actual);
	
	}
	
	@Test
	public void getMethodInFinalThirdTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(2,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getMethodInFinal();
		Assert.assertEquals("throwEx", actual);
	}
	
	@Test 
	public void testGetMethodCallerFirstLightBallForStaticMethod() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample.callStaticMethod();";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerSecondLightBallForStaticMethod() {
		int badSmellLightBallIndex = 1;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample.callStaticMethod();";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerThirdLightBallForStaticMethod() {
		int badSmellLightBallIndex = 2;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample.callStaticMethod();";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerFirstLightBallForPublicMethod() {
		int badSmellLightBallIndex = 3;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample object = new thrownFromFinallyExample();\n\t\t\tobject.callPublicMethod();";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerSecondLightBallForPublicMethod() {
		int badSmellLightBallIndex = 4;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample object = new thrownFromFinallyExample();\n\t\t\tobject.callPublicMethod();";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerThirdLightBallForPublicMethod() {
		int badSmellLightBallIndex = 5;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample object = new thrownFromFinallyExample();\n\t\t\tobject.callPublicMethod();";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerFirstLightBallForPrivateMethod() {
		int badSmellLightBallIndex = 6;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample object = new thrownFromFinallyExample();\n\t\t\t" +
						"Method privateMethod = thrownFromFinallyExample.class.getDeclaredMethod(\"callPrivateMethod\");\n\t\t\t" +
						"privateMethod.setAccessible(true);\n\t\t\t" +
						"privateMethod.invoke(object);";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerSecondLightBallForPrivateMethod() {
		int badSmellLightBallIndex = 7;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample object = new thrownFromFinallyExample();\n\t\t\t" +
						"Method privateMethod = thrownFromFinallyExample.class.getDeclaredMethod(\"callPrivateMethod\");\n\t\t\t" +
						"privateMethod.setAccessible(true);\n\t\t\t" +
						"privateMethod.invoke(object);";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerThirdLightBallForPrivateMethod() {
		int badSmellLightBallIndex = 8;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "thrownFromFinallyExample object = new thrownFromFinallyExample();\n\t\t\t" +
						"Method privateMethod = thrownFromFinallyExample.class.getDeclaredMethod(\"callPrivateMethod\");\n\t\t\t" +
						"privateMethod.setAccessible(true);\n\t\t\t" +
						"privateMethod.invoke(object);";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetAssertioFirstLightBallForStaticMethod() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch (CustomRobustaException e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",e.getMessage());"
						+ "\n\t\t} catch (Exception e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.fail(\"Exception is thrown from finally block.\");"+"\n\t\t}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test
	public void testGetAssertioSecondLightBallForStaticMethod() {
		int badSmellLightBallIndex = 1;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch (CustomRobustaException e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",e.getMessage());"
						+ "\n\t\t} catch (Exception e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.fail(\"Exception is thrown from finally block.\");"+"\n\t\t}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test
	public void testGetAssertioThirdLightBallForStaticMethod() {
		int badSmellLightBallIndex = 2;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch (CustomRobustaException e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",e.getMessage());"
						+ "\n\t\t} catch (Exception e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.fail(\"Exception is thrown from finally block.\");"+"\n\t\t}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test 
	public void testGetAssertioFirstLightBallForPublicMethod() {
		int badSmellLightBallIndex = 3;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch (CustomRobustaException e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",e.getMessage());"
						+ "\n\t\t} catch (Exception e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.fail(\"Exception is thrown from finally block.\");"+"\n\t\t}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test
	public void testGetAssertioSecondLightBallForPublicMethod() {
		int badSmellLightBallIndex = 4;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch (CustomRobustaException e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",e.getMessage());"
						+ "\n\t\t} catch (Exception e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.fail(\"Exception is thrown from finally block.\");"+"\n\t\t}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test
	public void testGetAssertioThirdLightBallForPublicMethod() {
		int badSmellLightBallIndex = 5;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch (CustomRobustaException e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",e.getMessage());"
						+ "\n\t\t} catch (Exception e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.fail(\"Exception is thrown from finally block.\");"+"\n\t\t}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	//todo
	@Test 
	public void testGetAssertioFirstLightBallForPrivateMethod() {
		int badSmellLightBallIndex = 6;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch(Exception e){\n\t\t\t"
						+ "e.printStackTrace();\n\t\t\t"
						+ "String exceptionMessage = e.getCause().getMessage().toString();\n\t\t\t"
						+ "Assert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",exceptionMessage);\n\t\t"
						+ "}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test
	public void testGetAssertioSecondLightBallForPrivateMethod() {
		int badSmellLightBallIndex = 7;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch(Exception e){\n\t\t\t"
			+ "e.printStackTrace();\n\t\t\t"
			+ "String exceptionMessage = e.getCause().getMessage().toString();\n\t\t\t"
			+ "Assert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",exceptionMessage);\n\t\t"
			+ "}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test
	public void testGetAssertioThirdLightBallForPrivateMethod() {
		int badSmellLightBallIndex = 8;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "catch(Exception e){\n\t\t\t"
			+ "e.printStackTrace();\n\t\t\t"
			+ "String exceptionMessage = e.getCause().getMessage().toString();\n\t\t\t"
			+ "Assert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",exceptionMessage);\n\t\t"
			+ "}";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
}
