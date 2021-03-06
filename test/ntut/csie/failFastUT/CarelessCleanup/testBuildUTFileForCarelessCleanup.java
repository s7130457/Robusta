package ntut.csie.failFastUT.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.careless.CarelessCleanupVisitor;
import ntut.csie.aspect.AddAspectsMarkerResoluationForCarelessCleanup;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.aspect.MethodDeclarationVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.failFastUT.CarelessCleanup.carelessCleanupExample;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.MethodInvocationCollectorVisitor;
import ntut.csie.util.NodeUtilsTest.MethodInvocationVisitor;
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
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class testBuildUTFileForCarelessCleanup {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private CarelessCleanupVisitor adVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> markerInfos;
	private AddAspectsMarkerResoluationForCarelessCleanup resoluation;
	private Path addAspectsMarkerResoluationExamplePath;
	private Path addpackagePath;
	private IMarker marker;
	private IProject project;
	private String AspectPackage = "ntut.csie.test.CarelessCleanup";

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		markerInfos = visitCompilationAndGetSmellList();
		setUpMethodDeclarationIndexOfMarkerInfo();
		resoluation = new AddAspectsMarkerResoluationForCarelessCleanup("test");
		addAspectsMarkerResoluationExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject"
						+ "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE
						+ "/"
						+ PathUtils.dot2slash(carelessCleanupExample.class
								.getName())
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
		environmentBuilder.loadClass(carelessCleanupExample.class);
		compilationUnit = environmentBuilder
				.getCompilationUnit(carelessCleanupExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodDeclarationIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);

		for (MarkerInfo m : markerInfos) {
			int methodDeclarationIdx = -1;
			for (MethodDeclaration methodDeclarationBlock : methodCollector
					.getMethodList()) {
				methodDeclarationIdx++;
				MethodInvocationCollectorVisitor methodInvocationCollector = new MethodInvocationCollectorVisitor();
				methodDeclarationBlock.accept(methodInvocationCollector);
				for (MethodInvocation invocation : methodInvocationCollector
						.getMethodInvocations()) {
					if (m.getLineNumber() == compilationUnit
							.getLineNumber(invocation.getStartPosition())) {
						m.setMethodIndex(methodDeclarationIdx);
						break;
					}

				}
			}
		}
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList()
			throws JavaModelException {
		adVisitor = new CarelessCleanupVisitor(compilationUnit, true);
		compilationUnit.accept(adVisitor);
		return adVisitor.getCarelessCleanupList();
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
	public void testBuildUnitTestFileOfStaticMethod() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResoluationForCarelessCleanup marker = new AddAspectsMarkerResoluationForCarelessCleanup(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile).replaceAll("\\s", "");
		Actual = Actual.replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/CarelessCleanup/UT4StaticMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[0].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}
	
	@Test
	public void testBuildUnitTestFileOfPublicMethod() {
		int badSmellLightBallIndex = 2;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResoluationForCarelessCleanup marker = new AddAspectsMarkerResoluationForCarelessCleanup(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile).replaceAll("\\s", "");
		Actual = Actual.replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/CarelessCleanup/UT4PublicMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[0].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}

	@Test
	public void testBuildUnitTestFileOfPrivateMethod() {
		int badSmellLightBallIndex = 4;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResoluationForCarelessCleanup marker = new AddAspectsMarkerResoluationForCarelessCleanup(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile).replaceAll("\\s", "");
		Actual = Actual.replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/CarelessCleanup/UT4PrivateMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[0].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}

}
