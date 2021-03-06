package ntut.csie.failFastUT.Thrown;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.thrown.ExceptionThrownFromFinallyBlockVisitor;
import ntut.csie.aspect.AddAspectsMarkerResolutionForThrowFromFinally;
import ntut.csie.aspect.BadSmellTypeConfig;
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


public class testBuildUTFileForThrown {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private ExceptionThrownFromFinallyBlockVisitor throwFromFinallyVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> markerInfos;
	private AddAspectsMarkerResolutionForThrowFromFinally resoluation;
	private Path addAspectsMarkerResoluationExamplePath;
	private Path addpackagePath;
	private IMarker marker;
	private IProject project;
	private String AspectPackage = "ntut.csie.TestAspectPackage";

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		markerInfos = visitCompilationAndGetSmellList();
		setUpMethodDeclarationIndexOfMarkerInfo();
		resoluation = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		addAspectsMarkerResoluationExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject" + "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
						+ PathUtils.dot2slash(thrownFromFinallyExample.class.getName())
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
		environmentBuilder.loadClass(thrownFromFinallyExample.class);
		compilationUnit = environmentBuilder
				.getCompilationUnit(thrownFromFinallyExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodDeclarationIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		for (MarkerInfo m : markerInfos) {
			int methodDeclarationIdx = -1;
			int methodDeclarationSize = methodCollector.getMethodList().size();
			for (MethodDeclaration methodDeclarationBlock : methodCollector.getMethodList()) {
				methodDeclarationIdx++;
				if(m.getLineNumber()<compilationUnit.getLineNumber(methodDeclarationBlock.getStartPosition())){
					m.setMethodIndex(methodDeclarationIdx-1);
					break;
				} else if(methodDeclarationBlock == methodCollector.getMethodList().get(methodDeclarationSize-1)) {
					m.setMethodIndex(methodDeclarationSize-1);
					break;
				}
				
			}
		}
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList()
			throws JavaModelException {
		 throwFromFinallyVisitor= new ExceptionThrownFromFinallyBlockVisitor(compilationUnit);
		compilationUnit.accept(throwFromFinallyVisitor);
		return throwFromFinallyVisitor.getThrownInFinallyList();
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
			e.printStackTrace();
			Assert.fail("throw exception");
		}
		return tempMarker;
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
			e.printStackTrace();
		}
		return content;
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	/**
	 * badSmellLightBallIndex is the specific bad smell light ball index in this class
	 */
	@Test
	public void testBuildUnitTestFileFirstMethodIndexOfStaticMethod() {
		int badSmellLightBallIndex = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
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
				+ "test/ntut/csie/failFastUT/Thrown/UT4StaticMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[0].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}
	
	@Test
	public void testBuildUnitTestFileSecondMethodIndexOfStaticMethod() {
		int badSmellLightBallIndex = 1;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile);
		Actual = Actual.replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/UT4StaticMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[1].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}
	
	@Test
	public void testBuildUnitTestFileThirdMethodIndexOfStaticMethod() {
		int badSmellLightBallIndex = 2;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile);
		Actual = Actual.replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/UT4StaticMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file).replaceAll("\\s", "");
		utContentExpected = utContentExpected.split("@end")[2].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}

	@Test
	public void testBuildUnitTestFileFirstMethodIndexOfPublicMethod() {
		int badSmellLightBallIndex = 3;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile).replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/UT4PublicMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[0].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}
	
	@Test
	public void testBuildUnitTestFileSecondMethodIndexOfPublicMethod() {
		int theSecondMarkerInfo = 4;
		marker = getSpecificMarkerByMarkerInfoIndex(theSecondMarkerInfo);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile);
		Actual = Actual.replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/UT4PublicMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[1].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}
	
	@Test
	public void testBuildUnitTestFileThirdMethodIndexOfPublicMethod() {
		int theSecondMarkerInfo = 5;
		marker = getSpecificMarkerByMarkerInfoIndex(theSecondMarkerInfo);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile);
		Actual = Actual.replaceAll("\\s", "");
		
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/UT4PublicMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[2].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}
	
	@Test
	public void testBuildUnitTestFileFirstMethodIndexOfPrivateMethod() {
		int badSmellLightBallIndex = 6;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile).replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/UT4PrivateMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[0].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}
	
	@Test
	public void testBuildUnitTestFileSecondMethodIndexOfPrivateMethod() {
		int badSmellLightBallIndex = 7;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile);
		Actual = Actual.replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/UT4PrivateMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[1].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}
	
	@Test
	public void testBuildUnitTestFileThirdMethodIndexOfPrivateMethod() {
		int badSmellLightBallIndex = 8;
		marker = getSpecificMarkerByMarkerInfoIndex(badSmellLightBallIndex);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForThrowFromFinally marker = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile);
		Actual = Actual.replaceAll("\\s", "");
		
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/UT4PrivateMethodExpected";
		File file = new File(packages);
		String utContentExpected = "";
		utContentExpected = readFile(file);
		utContentExpected = utContentExpected.split("@end")[2].replaceAll("\\s", "");
		Assert.assertEquals(utContentExpected, Actual);
	}

}
