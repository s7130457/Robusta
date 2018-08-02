package ntut.csie.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.PathUtils;
import ntut.csie.aspect.AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock;
import ntut.csie.aspect.AspectDemo;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.aspect.MethodDeclarationVisitor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DummyHandleConfigTest {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnitDummyAndEmpty;
	private DummyHandlerVisitor adVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> markerInfos;
	private AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock resoluation;
	private Path addAspectsMarkerResoluationExamplePath;
	private IMarker marker;

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		detectPrintStackTrace();
		markerInfos = visitCompilationAndGetSmellList();
		setUpMethodDeclarationIndexOfMarkerInfo();
		resoluation = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		addAspectsMarkerResoluationExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject" + "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
						+ PathUtils.dot2slash(AspectDemo.class.getName())
						+ JavaProjectMaker.JAVA_FILE_EXTENSION);
	}

	private void setUpTestingEnvironment() throws Exception, JavaModelException {
		environmentBuilder = new TestEnvironmentBuilder(
				"AddAspectsMarkerResoluationExampleProject");
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(AspectDemo.class);

		compilationUnitDummyAndEmpty = environmentBuilder
				.getCompilationUnit(AspectDemo.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodDeclarationIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnitDummyAndEmpty.accept(methodCollector);

		for (MarkerInfo m : markerInfos) {

			int methodDeclarationIdx = -1;
			for (MethodDeclaration methodDeclarationBlock : methodCollector.getMethodList()) {
				methodDeclarationIdx++;
				MethodDeclarationVisitor declarationVisitor = new MethodDeclarationVisitor(
						compilationUnitDummyAndEmpty);
				methodDeclarationBlock.accept(declarationVisitor);

				for (Integer a_integer : declarationVisitor
						.getCatchClauseLineNumberList()) {
					if (m.getLineNumber() == (int) a_integer) {
						m.setMethodIndex(methodDeclarationIdx);
						break;
					}
				}
				if (m.getMethodIndex() != -1)
					break;
			}
		}
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	/**
	 * badSmellLightBallIndex is the specific bad smell light ball index in this class
	 */
	@Test
	public void getMethodDeclarationWhichHasBadSmellFirstMarkerTest() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex, addAspectsMarkerResoluationExamplePath );
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String methodDeclarationWhichHasBadSmellExpected = "";

		try {
			methodDeclarationWhichHasBadSmellExpected = readAspectDemo();
			methodDeclarationWhichHasBadSmellExpected = methodDeclarationWhichHasBadSmellExpected
					.substring(methodDeclarationWhichHasBadSmellExpected
							.indexOf("public static"),
							methodDeclarationWhichHasBadSmellExpected
									.indexOf("private static"));
			methodDeclarationWhichHasBadSmellExpected = methodDeclarationWhichHasBadSmellExpected
					.replaceAll("\\s", "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		String actuallyBuilder = builder.getMethodDeclarationWhichHasBadSmell()
				.toString().replaceAll("\\s", "");
		Assert.assertEquals(methodDeclarationWhichHasBadSmellExpected,
				actuallyBuilder);
	}
	@Test
	public void getAllMethodThrowInSpecificExceptionListTest(){
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String Actual = builder.getFirstInvocationSameAsCatch().toString();
		String Excepted = "[write]";
		Assert.assertEquals(Excepted, Actual);
	}
	@Test
	public void getBadSmellLineNumberFirstTest() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		Assert.assertEquals(18, builder.getBadSmellLineNumber());
	}
	@Test
	public void getTryStatementsTest() {
		String tryStatementExpected = null;
		marker = getSpecificMarkerByMarkerInfoIndex(0,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String tryStatementActually = null;
		try {
			tryStatementExpected = readAspectDemo();
			tryStatementExpected = tryStatementExpected.substring(
					tryStatementExpected.indexOf("try"),
					tryStatementExpected.indexOf("private static void"));
			tryStatementExpected = tryStatementExpected.substring(0,
					tryStatementExpected.lastIndexOf("}"))
					.replaceAll("\\s", "");

			tryStatementActually = builder.getTryStatements().get(0).toString()
					.replaceAll("\\s", "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(tryStatementExpected, tryStatementActually);
	}

	@Test
	public void getTryStatementWillBeInjectTest() {
		String tryStatementWillBeInjectExpected = "";
		String tryStatementWillBeInjectActually = "";
		marker = getSpecificMarkerByMarkerInfoIndex(0,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		try {
			tryStatementWillBeInjectExpected = readAspectDemo();
			tryStatementWillBeInjectExpected = tryStatementWillBeInjectExpected
					.substring(tryStatementWillBeInjectExpected.indexOf("try"),
							tryStatementWillBeInjectExpected
									.indexOf("private static void"));
			tryStatementWillBeInjectExpected = tryStatementWillBeInjectExpected
					.substring(0,
							tryStatementWillBeInjectExpected.lastIndexOf("}"))
					.replaceAll("\\s", "");

			tryStatementWillBeInjectActually = builder
					.getTryStatementWillBeInject().toString()
					.replaceAll("\\s", "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(tryStatementWillBeInjectExpected,
				tryStatementWillBeInjectActually);
	}

	@Test
	public void getCatchClausesTest() {
		String catchClausesExpected = "";
		String catchClausesActually = "";
		StringBuilder strBuilder = null;
		List<String> catchClauseList = new ArrayList<String>();

		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);

		try {
			catchClausesExpected = readAspectDemo();
			catchClausesExpected = catchClausesExpected.substring(
					catchClausesExpected.indexOf("catch"),
					catchClausesExpected.indexOf("private static void"));
			catchClausesExpected = catchClausesExpected.substring(0,
					catchClausesExpected.lastIndexOf("}"))
					.replaceAll("\\s", "");
			
			strBuilder = new StringBuilder(catchClausesExpected);
			strBuilder.insert(catchClausesExpected.indexOf('}') + 1, ',');
			catchClauseList = Arrays.asList(strBuilder.toString().split(","));
			catchClausesExpected = catchClauseList.toString().replaceAll("\\s",
					"");
			catchClausesActually = builder.getCatchClauses().toString()
					.replaceAll("\\s", "");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Assert.assertEquals(catchClausesExpected, catchClausesActually);
	}

	@Test
	public void getExceptionTypeFirstTest() {
		String catchClausesActually = "";
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		catchClausesActually = builder.getExceptionType().toString();
		Assert.assertEquals("IOException", catchClausesActually);
	}
	@Test
	public void getBadSmellTypeFirstTest() {
		String badSmellTypeFirstActually = "";
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		badSmellTypeFirstActually = builder.getBadSmellType();
		Assert.assertEquals("DummyHandler", badSmellTypeFirstActually);
	}
	@Test
	public void getClassNameTest() {
		String classNameActually = "";
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		classNameActually = builder.getClassName();
		Assert.assertEquals("AspectDemo", classNameActually);
	}

	@Test
	public void getImportObjectsFirstTest() {
		String importObjectsExpected = "";
		boolean importObjectsExist = true;
		marker = getSpecificMarkerByMarkerInfoIndex(0,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		try {
			importObjectsExpected = readAspectDemo();
			importObjectsExpected = importObjectsExpected.substring(
					importObjectsExpected.indexOf("import"),
					importObjectsExpected.indexOf("public class"));
			importObjectsExpected = importObjectsExpected.replaceAll("\\s", "");

			for (String importObjectsActually : builder.getImportObjects()) {
				if (importObjectsExpected.indexOf(importObjectsActually) < 0
						&& importObjectsActually
								.indexOf(builder.getClassName()) < 0) {
					importObjectsExist = false;
					break;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertEquals(true, importObjectsExist);
	}
	
	@Test 
	public void testGetMethodCallerForStaticMethod() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "AspectDemo.callStaticMethod();";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}

	@Test 
	public void testGetMethodCallerForPublicMethod() {
		int badSmellLightBallIndex = 1;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "AspectDemo object = new AspectDemo();\n\t\t\tobject.callPublicMethod();";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetMethodCallerForPrivateMethod() {
		int badSmellLightBallIndex = 2;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "AspectDemo object = new AspectDemo();\n\t\t\t" +
						"Method privateMethod = AspectDemo.class.getDeclaredMethod(\"callPrivateMethod\");\n\t\t\t" +
						"privateMethod.setAccessible(true);\n\t\t\t" +
						"privateMethod.invoke(object);";
		Assert.assertEquals(Expected, builder.getMethodCaller(methodDeclaration));
	}
	
	@Test 
	public void testGetAssertionForStaticMethod() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "String exceptionMessage = e.getMessage().toString();\n\t\t\t"
						+"Assert.assertTrue(exceptionMessage.contains(\"This exception is thrown from DummyHandler's unit test by using AspectJ.\"));";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test 
	public void testGetAssertionForPublicMethod() {
		int badSmellLightBallIndex = 1;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "String exceptionMessage = e.getMessage().toString();\n\t\t\t"
						+"Assert.assertTrue(exceptionMessage.contains(\"This exception is thrown from DummyHandler's unit test by using AspectJ.\"));";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}
	
	@Test 
	public void testGetAssertionForPrivateMethod() {
		int badSmellLightBallIndex = 2;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex,addAspectsMarkerResoluationExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		MethodDeclaration methodDeclaration = builder.getMethodDeclarationWhichHasBadSmell();
		String Expected = "String exceptionMessage = e.getCause().getMessage().toString();\n\t\t\t"
						+"Assert.assertTrue(exceptionMessage.contains(\"This exception is thrown from DummyHandler's unit test by using AspectJ.\"));";
		Assert.assertEquals(Expected, builder.getAssertion(methodDeclaration));
	}

	private String readAspectDemo() throws FileNotFoundException, IOException,
			UnsupportedEncodingException {
		String ReadAspectDemoContent;
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/aspect/AspectDemo.java";
		File file = new File(packages);
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		ReadAspectDemoContent = new String(data, "UTF-8");
		return ReadAspectDemoContent;
	}


	private IMarker getSpecificMarkerByMarkerInfoIndex(int index, Path filePath) {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin
				.getWorkspace().getRoot()
				.getFile(filePath));
		IMarker tempMarker = null;
		try {
			tempMarker = javaElement.getResource().createMarker("test.test");

			tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX,
					Integer.toString(markerInfos.get(index).getMethodIndex()));
			tempMarker.setAttribute(IMarker.LINE_NUMBER, new Integer(
					markerInfos.get(index).getLineNumber()));
			tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE,
					markerInfos.get(index).getCodeSmellType());

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("throw exception");
		}
		return tempMarker;
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList()
			throws JavaModelException {
		adVisitor = new DummyHandlerVisitor(compilationUnitDummyAndEmpty);
		compilationUnitDummyAndEmpty.accept(adVisitor);
		return adVisitor.getDummyHandlerList();
	}

	

	private void detectPrintStackTrace() {
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER,
				SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

}
