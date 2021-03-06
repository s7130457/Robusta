package ntut.csie.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.Assertor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BadSmellCollectorTest {

	String projectName = "ReportBuilderIntergrationTest";
	private JavaFileToString javaFileToString;
	private JavaProjectMaker javaProjectMaker;
	private SmellSettings smellSettings;
	private IProject project;
	private IJavaProject javaProject;
	private Boolean isDetecting = true;
	
	@Before
	public void setUp() throws Exception {
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/slf4j-api-1.5.0.jar");
		javaProjectMaker.addClasspathEntryToBuildPath(BuildPathSupport.getJUnit4ClasspathEntry(), null);
		javaProjectMaker.packageAgileExceptionClassesToJarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		javaProject = JavaCore.create(project);
		InitailSetting();
		loadClass(UnprotectedMainProgramWithoutTryExample.class);
		loadClass(SuppressWarningExampleForAnalyzer.class);		
	}

	private void InitailSetting() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		enableAllSmellDetect();
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void enableAllSmellDetect() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting );
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);		
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrintln);
		
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYSTATEMENT, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);

		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
	}
	
	private void loadClass(Class clazz) throws Exception	{
		javaFileToString.read(clazz, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				clazz.getPackage().getName(),
				clazz.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ clazz.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();		
	}
	
	private CompilationUnit getCompilationUnit(Class clazz) throws Exception {
		IType type = javaProject.findType(clazz.getName());
		CompilationUnit unit = parse(type.getCompilationUnit());
		return unit;
	}
	
	@After
	public void tearDown() throws Exception {
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testGetBadSmells() throws Exception {
		CompilationUnit root = getCompilationUnit(SuppressWarningExampleForAnalyzer.class);
		BadSmellCollector collector = new BadSmellCollector(project, root);
		collector.collectBadSmell();

		Assertor.assertMarkerInfoListSize(8, collector.getBadSmells(RLMarkerAttribute.CS_DUMMY_HANDLER));
		Assertor.assertMarkerInfoListSize(3, collector.getBadSmells(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		Assertor.assertMarkerInfoListSize(4, collector.getBadSmells(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		Assertor.assertMarkerInfoListSize(4, collector.getBadSmells(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		// TODO Example of this bad smell hasn't added to SuppressWarningExampleForAnalyzer
		assertEquals(0, collector.getBadSmells(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK).size());
		assertEquals(0, collector.getBadSmells(RLMarkerAttribute.CS_UNPROTECTED_MAIN).size());
		assertEquals(19, collector.getAllBadSmells().size());
	}
}
