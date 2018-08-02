package ntut.csie.failFastUT.Empty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.empty.EmptyCatchBlockVisitor;
import ntut.csie.aspect.AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock;
import ntut.csie.aspect.AspectDemo;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.aspect.MethodDeclarationVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
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


public class testBuildUTFileForEmpty {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private EmptyCatchBlockVisitor EmptyVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> markerInfos;
	private AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock resoluation;
	private Path addAspectsMarkerResoluationExamplePath;
	private Path addpackagePath;
	private IMarker marker;
	private IProject project;
	private String AspectPackage = "ntut.csie.test.EmptyCatchBlock";
	
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
						+ PathUtils.dot2slash(emptyExample.class.getName())
						+ JavaProjectMaker.JAVA_FILE_EXTENSION);
		// /AddAspectsMarkerResoluationExampleProject/src/ntut/csie/TestAspectPackage
		addpackagePath = new Path("AddAspectsMarkerResoluationExampleProject"
				+ "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(AspectPackage));

	}

	private void setUpTestingEnvironment() throws Exception, JavaModelException {
		environmentBuilder = new TestEnvironmentBuilder(
				"AddAspectsMarkerResoluationExampleProject");
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(emptyExample.class);
		compilationUnit = environmentBuilder
				.getCompilationUnit(emptyExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodDeclarationIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);

		for (MarkerInfo m : markerInfos) {

			int methodDeclarationIdx = -1;
			for (MethodDeclaration methodDeclarationBlock : methodCollector.getMethodList()) {
				methodDeclarationIdx++;
				MethodDeclarationVisitor declarationVisitor = new MethodDeclarationVisitor(
						compilationUnit);
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

	private void detectPrintStackTrace() {
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER,
				SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList()
			throws JavaModelException {
		EmptyVisitor = new EmptyCatchBlockVisitor(compilationUnit);
		compilationUnit.accept(EmptyVisitor);
		return EmptyVisitor.getEmptyCatchList();
	}

	private IMarker getSpecificMarkerByMarkerInfoIndex(int index) {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin
				.getWorkspace().getRoot()
				.getFile(addAspectsMarkerResoluationExamplePath));
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

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void createPackageTest() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock markerResoluationForDummyAndEmpty = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		markerResoluationForDummyAndEmpty.setMarker(marker);
		markerResoluationForDummyAndEmpty.createPackage(AspectPackage);
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		File file = new File(createPackagePath);
		Assert.assertEquals(true, file.exists());
	}

	
	private String readFile(File file) {
		String content = "";
		try {
			FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			fis.close();
			content = new String(data, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
	}

	/**
	 * badSmellLightBallIndex is the specific bad smell light ball index in this class
	 */
	@Test
	public void testBuildTestFileForStaticMethod() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock marker = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";

		String Actual = marker.buildTestFile(config,packageChain,filePathUTFile).replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Empty/UT4StaticMethodExpected";
		File file = new File(packages);
		String aspectContentExpected = "";
		aspectContentExpected = readFile(file).replaceAll("\\s", "");
		Assert.assertEquals(aspectContentExpected,Actual);
	}
	
	@Test
	public void testBuildTestFileForPublicMethod() {
		int badSmellLightBallIndex = 1;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock marker = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";

		String Actual = marker.buildTestFile(config,packageChain,filePathUTFile).replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Empty/UT4PublicMethodExpected";
		File file = new File(packages);
		String aspectContentExpected = "";
		aspectContentExpected = readFile(file).replaceAll("\\s", "");
		Assert.assertEquals(aspectContentExpected,Actual);
	}
	
	@Test
	public void testBuildTestFileForPrivateMethod() {
		int badSmellLightBallIndex = 2;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock marker = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";

		String Actual = marker.buildTestFile(config,packageChain,filePathUTFile).replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Empty/UT4PrivateMethodExpected";
		File file = new File(packages);
		String aspectContentExpected = "";
		aspectContentExpected = readFile(file).replaceAll("\\s", "");
		Assert.assertEquals(aspectContentExpected,Actual);
	}
}
