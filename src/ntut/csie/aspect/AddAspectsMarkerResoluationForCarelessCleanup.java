package ntut.csie.aspect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ntut.csie.analyzer.careless.CloseInvocationExecutionChecker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;
import ntut.csie.util.MethodInvocationCollectorVisitor;
import ntut.csie.util.PopupDialog;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;

public class AddAspectsMarkerResoluationForCarelessCleanup implements
		IMarkerResolution, IMarkerResolution2 {
	private String label;
	private String description = "Generate an aspectJ file and failing fast test case:[Careless_Cleanup] "
			+ "<br>1. AspectJ file is to expose influence of bad smell."
			+ "<br>2. Failing fast test case is to reproduce the failing scenario.";
	private QuickFixCore quickFixCore;
	private CompilationUnit compilationUnit;
	private List<String> importObjects = new ArrayList<String>();
	private IProject project;
	private IJavaProject javaproject;
	private String projectPath;
	private IMarker marker;
	private String packageFilePath;

	public AddAspectsMarkerResoluationForCarelessCleanup(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return label;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_ASSIST);
	}

	@Override
	public void run(IMarker markerInRun) {
		marker = markerInRun;
		BadSmellTypeConfig config = new BadSmellTypeConfig(markerInRun);
		generateRobustaUtilsPackageFile();
		generateAspectJFile(config);
		generateUtFileForAspectJ(config);

		refreshProject();

	}

	private void generateRobustaUtilsPackageFile() {
		String robustaUtilsPackage = "ntut.csie.RobustaUtils";
		createPackage(robustaUtilsPackage);
		projectPath = ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toOSString();
		String filePathAspectJSwitch = projectPath + "\\" + packageFilePath
				+ "\\ntut\\csie\\RobustaUtils\\AspectJSwitch.java";
		String filePathCustomRobustaException = projectPath + "\\"
				+ packageFilePath
				+ "\\ntut\\csie\\RobustaUtils\\CustomRobustaException.java";
		File aspectJSwitchFile = new File(filePathAspectJSwitch);
		File customRobustaExceptionFile = new File(
				filePathCustomRobustaException);
		if (!aspectJSwitchFile.exists()) {
			String aspectJSwitchFileContent = buildUpAspectJSwitch(robustaUtilsPackage);
			WriteFile(aspectJSwitchFileContent, filePathAspectJSwitch);
			refreshPackageExplorer(filePathAspectJSwitch);
		}
		if (!customRobustaExceptionFile.exists()) {
			String customExceptionFileContent = buildUpCustomException(robustaUtilsPackage);
			WriteFile(customExceptionFileContent,
					filePathCustomRobustaException);
			refreshPackageExplorer(filePathCustomRobustaException);
		}
	}

	private String buildUpCustomException(String packageChain) {
		String packageContent = "package " + packageChain + ";\n\n";
		String defineClass = "public class CustomRobustaException extends RuntimeException {\n\t";
		String constructorWithoutParams = "public CustomRobustaException() {\r\n"
				+ "\t\t super();\n" + "\t}\n\t";
		String constructorWithThrowableParams = "public CustomRobustaException(Throwable e) {\r\n"
				+ "\t\t super(e);\n" + "\t}\n\t";
		String constructorWithStringParams = "public CustomRobustaException(String msg) {\r\n"
				+ "\t\t super(msg);\n" + "\t}\n\t";
		String constructorWithMultiParams = "public CustomRobustaException(String msg, Throwable e) {\r\n"
				+ "\t\t super(msg, e);\n" + "\t}\n";
		return packageContent + defineClass + constructorWithoutParams
				+ constructorWithThrowableParams + constructorWithStringParams
				+ constructorWithMultiParams + "}";

	}

	private void generateUtFileForAspectJ(BadSmellTypeConfig config) {
		// create a package
		String packageChain = "ntut.csie.test." + config.getBadSmellType();
		createPackage(packageChain);

		// find method name
		// 命名需要修正

		// create a test file
		String testFilePath = projectPath + "\\" + packageFilePath
				+ "\\ntut\\csie\\test" + "\\" + config.getBadSmellType() + "\\"
				+ "test" + makeFirstCharacterUpperCase(config.getClassName())
				+ "MethodUseAspetctJ.java";
		String testFileContent = buildTestFile(config, packageChain,
				testFilePath);

		WriteFile(testFileContent, testFilePath);
		refreshPackageExplorer(testFilePath);

	}

	public String buildTestFile(BadSmellTypeConfig config, String packageName,
			String testFilePath) {

		String testFileContent = "";
		String beforeContent = "package "
				+ packageName
				+ ";\n\n"
				+ "import org.junit.Test;\n"
				+ "import ntut.csie.RobustaUtils.AspectJSwitch;\n"
				+ "import org.junit.Assert;\nimport java.lang.reflect.Method;\n"
				+ "import java.lang.reflect.InvocationTargetException;\n";
		String generateTestFile = "\r\n"
				+ "public class test"
				+ makeFirstCharacterUpperCase(config.getClassName())
				+ "MethodUseAspetctJ {"
				+ "\r\n\n\tprivate AspectJSwitch aspectJSwitch = AspectJSwitch.getInstance();\n";
		String appendNewTestCase = "";

		// Specific exception import
		String imports = "";
		for (String importObj : config.getImportObjects())
			imports = imports + "import " + importObj.trim() + ";\r\n";
		beforeContent += imports;
		String testMethodName = "";
		File filePathUnitTest = new File(testFilePath);
		String result = "";
		if (filePathUnitTest.exists()) {
			try {
				FileReader fr = new FileReader(filePathUnitTest);
				BufferedReader br = new BufferedReader(fr);
				String temp;
				while ((temp = br.readLine()) != null) {
					if (temp.indexOf("public class") > -1) {
						result = result + imports + "\r\n"; // 一次加入所有的import
					} else if (imports.indexOf(temp) > -1) // 重複的不加進去result
						continue;
					result = result + temp + "\r\n";
				}

				result = result.substring(0, result.lastIndexOf('}'));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		int exceptionIndex = 0;
		String methodDeclarationName = config
				.getMethodDeclarationWhichHasBadSmell().getName().toString();
		String testOneMethod = config.getCollectBadSmellMethods().get(0);
		if (!result.contains("ReleaseMethodShouldExecuteIn"
				+ makeFirstCharacterUpperCase(methodDeclarationName) + "()")) {
			if (!appendNewTestCase
					.contains("ReleaseMethodShouldExecuteIn"
							+ makeFirstCharacterUpperCase(methodDeclarationName)
							+ "()")) {
				String generateOneTestCase = "\n\t@Test" + "\n\t"
						+ "public void testReleaseMethodShouldExecuteIn"
						+ makeFirstCharacterUpperCase(methodDeclarationName)
						+ "() {\n\t\taspectJSwitch.initResponse();"
						+ "\n\t\taspectJSwitch.addResponse(\""
						+ testOneMethod
						+ "/f("
						+ config.getCollectBadSmellExceptionTypes().get(
								exceptionIndex)
						+ ")\");"
						+ "\n\t\taspectJSwitch.addResponse(\""
						+ "close"
						+ "/AOPCheckResources"
						+ "\");"
						+ "\n\t\taspectJSwitch.toFirstResponse();"
						+ "\n\t\ttry{\n\t\t\t"
						+ config.getMethodCaller(config
								.getMethodDeclarationWhichHasBadSmell())
						+ "\n\t\t\t"
						+ "Assert.assertTrue(aspectJSwitch.isResourceCleanup());"
						+ "\n\t\t}"
						+ "catch (Exception e) {\n\t\t\tAssert.assertTrue(aspectJSwitch.isResourceCleanup());"
						+ "\n\t\t}" + "\n\t}";
				appendNewTestCase = appendNewTestCase + generateOneTestCase;
				testMethodName = "test"
						+ makeFirstCharacterUpperCase(testOneMethod)
						+ "ThrowExceptionInMain";
				exceptionIndex++;
			}
		}
		if (!filePathUnitTest.exists())
			testFileContent = beforeContent + generateTestFile
					+ appendNewTestCase + "\n}";
		else
			testFileContent = result + appendNewTestCase + "\n}";

		return testFileContent;

	}

	private void generateAspectJFile(BadSmellTypeConfig config) {

		String nameOfMethodWhichHasBadSmell = config
				.getMethodDeclarationWhichHasBadSmell().getName().toString();
		String packageChain = "ntut.csie.aspect." + config.getBadSmellType();
		createPackage(packageChain);

		projectPath = ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toOSString();

		String filePathAspectJFile = projectPath + "\\" + packageFilePath
				+ "\\ntut\\csie\\aspect" + "\\" + config.getBadSmellType()
				+ "\\" + config.getClassName() + "AspectException.aj";
		String aspectJFileContent = config.buildUpAspectsFile(packageChain,
				filePathAspectJFile);
		WriteFile(aspectJFileContent, filePathAspectJFile);
		refreshPackageExplorer(filePathAspectJFile);
	}

	public String buildUpAspectJSwitch(String packageChain) {
		String importContent = "import java.util.ArrayList;" + "\r\n"
				+ "import java.util.Iterator;" + "\r\n"
				+ "import java.util.List;";

		String AspectJSwitchContentClassTitle = "\r\n"
				+ "public class AspectJSwitch {";
		String initialization = "private static AspectJSwitch mAspectJSwitch;"
				+ "\r\n" + "\t"
				+ "private List<String> actionList = new ArrayList<String>();"
				+ "\r\n" + "\t" + "public Iterator<String> iterator = null;"
				+ "\r\n\t" + "private  boolean isCleanup = false;";
		String constructor = "\r\n\r\n" + "\t" + "private AspectJSwitch() {"
				+ "\r\n" + "\t" + "}";
		String getInstanceContent = "\r\n\r\n" + "\t"
				+ "public static AspectJSwitch getInstance() {" + "\r\n"
				+ "\t\t" + "if (mAspectJSwitch == null)" + "\r\n" + "\t\t\t"
				+ "mAspectJSwitch = new AspectJSwitch();" + "\r\n" + "\t\t"
				+ "return mAspectJSwitch;" + "\r\n" + "\t" + "}";
		String initResponseContent = "\r\n\r\n" + "\t"
				+ "public void initResponse() {" + "\r\n" + "\t\t"
				+ "actionList.clear();" + "\r\n" + "\t" + "}";
		String addResponseContent = "\r\n\r\n" + "\t"
				+ "public void addResponse(String action) {" + "\r\n" + "\t\t"
				+ "actionList.add(action);" + "\r\n" + "\t" + "}";
		String toFirstResponse = "\r\n\r\n" + "\t"
				+ "public void toFirstResponse() {" + "\r\n" + "\t\t"
				+ "iterator = actionList.iterator();" + "\r\n" + "\t" + "}";
		String nextActionContent = "\r\n\r\n" + "\t"
				+ "public synchronized String getOperation(String operation) {"
				+ "\r\n" + "\t\t" + "String result = \"success\";" + "\r\n"
				+ "\t\t" + "for (String action : actionList)" + "\r\n"
				+ "\t\t\t" + "if (action.startsWith(operation + \"/\")) {"
				+ "\r\n" + "\t\t\t\t"
				+ "String exceptionType = action.split(\"/\")[1];" + "\r\n"
				+ "\t\t\t\t" + "result = exceptionType;" + "\r\n" + "\t\t\t\t"
				+ "actionList.remove(action);" + "\r\n" + "\t\t\t\t" + "break;"
				+ "\r\n" + "\t\t\t" + "}" + "\r\n" + "\t\t" + "return result;"
				+ "\r\n" + "\t" + "}";
		String checkResource = "\r\n\r\n" + "\t"
				+ "public void checkResource() {" + "\r\n" + "\t\t"
				+ "if (isCleanup == false)" + "\r\n" + "\t\t\t"
				+ "isCleanup = true;" + "\r\n" + "\t\t" + "\r\n" + "\t" + "}";
		String isResourceCleanup = "\r\n\r\n" + "\t"
				+ "public boolean isResourceCleanup() {" + "\r\n" + "\t\t"
				+ "boolean tmp = isCleanup;" + "\r\n" + "\t\t"
				+ "isCleanup = false;" + "\r\n" + "\t\t" + "return tmp;"
				+ "\r\n" + "\t" + "}";
		String aspectJSwitchEnd = "\r\n\r\n" + "}";

		String AspectJSwitchContent = "package " + packageChain + ";"
				+ "\r\n\r\n" + importContent + "\r\n\r\n"
				+ AspectJSwitchContentClassTitle + "\r\n" + "\t"
				+ initialization + "\r\n" + "\t" + constructor + "\t"
				+ getInstanceContent + "\t" + initResponseContent + "\t"
				+ addResponseContent + "\t" + toFirstResponse + "\t"
				+ nextActionContent + "\t" + checkResource + "\t"
				+ isResourceCleanup + "\t" + aspectJSwitchEnd;

		return AspectJSwitchContent;

	}

	// packageName could be in format like a.b.c.d
	public void createPackage(String packageName) {
		try {
			project = marker.getResource().getProject();
			javaproject = JavaCore.create(project);
			IPackageFragmentRoot root = getSourceFolderOfCurrentProject();
			packageFilePath = root.getPath().makeAbsolute().toOSString();
			String s = packageName.trim().replaceAll("\\.", "/");
			String filePathAspectJFile = ResourcesPlugin.getWorkspace()
					.getRoot().getLocation().toOSString()
					+ packageFilePath + "\\" + s;
			File file = new File(filePathAspectJFile);
			if (!file.exists()) {
				root.createPackageFragment(packageName, false, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	private void WriteFile(String str, String path) {
		BufferedWriter writer = null;
		try {
			File file = new File(path);
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file, false), "utf8"));
			writer.write(str);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} finally {
			closeWriter(writer);
		}
	}

	private void closeWriter(BufferedWriter writer) {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			ignore();
		}
	}

	private void ignore() {
	}

	private void refreshPackageExplorer(String fileCreateFile) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath location = Path.fromOSString(fileCreateFile);
		IFile file = workspace.getRoot().getFileForLocation(location);
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void refreshProject() {
		if (project != null) {
			// save project to a final variable so that it can be used in Job,
			// it should be safe for that project should not change over time
			final IProject project2 = project;
			Job job = new Job("Refreshing Project") {
				protected IStatus run(IProgressMonitor monitor) {
					refreshProject(project2);
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.SHORT);
			job.schedule();
		}
	}

	private void refreshProject(IProject project) {
		// build project to refresh
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD,
					new NullProgressMonitor());
		} catch (CoreException e) {
			showOneButtonPopUpMenu("Refresh failed",
					"Fail to refresh your project, please do it manually");
		}
	}

	private void showOneButtonPopUpMenu(final String title, final String msg) {
		PopupDialog.showDialog(title, msg);
	}

	private String makeFirstCharacterUpperCase(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	private String buildUpAspectsFile(String exceptionType,
			String injectedMethodReturnType, String objectTypeOfInjectedMethod,
			String className, String nameOfMethodWhichHasBadSmell,
			String returnTypeOfMethodWhichHasBadSmell,
			String injectedMethodName, String badSmellType,
			String packageChain,
			int lineNumberOfMethodWhichWillThrowSpecificException) {
		String space = " ";
		String and = "&&";
		String aspectJClassTitle = "\r\n" + "public aspect " + className
				+ "Aspect" + exceptionType + "In"
				+ makeFirstCharacterUpperCase(nameOfMethodWhichHasBadSmell)
				+ "FunctionAtLine"
				+ lineNumberOfMethodWhichWillThrowSpecificException + " {";
		String pointCut = "pointcut find"
				+ makeFirstCharacterUpperCase(injectedMethodName) + "("
				+ objectTypeOfInjectedMethod + " object" + ") : ";
		String call = "call" + "(" + injectedMethodReturnType + space
				+ objectTypeOfInjectedMethod + "." + injectedMethodName
				+ "(..))";
		String target = "target" + "(object)";
		String withInCode = "withincode" + "("
				+ returnTypeOfMethodWhichHasBadSmell + space + className + "."
				+ nameOfMethodWhichHasBadSmell + "(..)" + ");";

		String around = injectedMethodReturnType + " around("
				+ objectTypeOfInjectedMethod + " object" + ") throws "
				+ exceptionType + " : find"
				+ makeFirstCharacterUpperCase(injectedMethodName)
				+ "(object) {";
		String aroundContent = "\t" + "  boolean aspectSwitch = false;"
				+ "\r\n" + "\t" + "  if(aspectSwitch){" + "\r\n" + "\t\t"
				+ "throw new " + exceptionType + "();" + "\r\n" + "\t"
				+ "  } else {" + "\r\n";
		String elseContent = "";
		if (injectedMethodReturnType.equals("void")) {
			elseContent = "\t\t" + "object." + injectedMethodName + "();"
					+ "\r\n";
		} else {
			elseContent = "\t\t" + " return object." + injectedMethodName
					+ "();" + "\r\n";
		}
		String aroundEnd = "\t" + "  }" + "\r\n" + "\t" + "}" + "\r\n" + "}";

		String imports = "";
		for (String importObj : importObjects) {
			imports = imports + "import " + importObj.trim() + ";\r\n";
		}

		String aspectJFileConetent = "package " + packageChain + ";"
				+ "\r\n\r\n" + imports + aspectJClassTitle + "\r\n" + "\t"
				+ pointCut + call + "\r\n" + "\t" + and + target + "\r\n"
				+ "\t" + and + withInCode + "\r\n\r\n" + "\t" + around + "\r\n"
				+ aroundContent + elseContent + aroundEnd;

		return aspectJFileConetent;
	}

	private String getTheObjectTypeOfMethodInvocation(
			MethodInvocation methodWhichWillThrowSpecificException) {
		FindExpressionObjectOfMethodInvocationVisitor theFirstExpressionVisitor = new FindExpressionObjectOfMethodInvocationVisitor();
		methodWhichWillThrowSpecificException.accept(theFirstExpressionVisitor);
		checkIsDuplicate(theFirstExpressionVisitor.getObjectPackageName());
		return theFirstExpressionVisitor.getObjectName();
	}

	// packageName could be in format like a.b.c.d
	private void createPackage(String packageName, IPackageFragmentRoot root) {
		try {
			root.createPackageFragment(packageName, false, null);
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	private IPackageFragmentRoot getSourceFolderOfCurrentProject() {
		IPackageFragmentRoot[] roots;
		try {
			roots = javaproject.getAllPackageFragmentRoots();
			for (IPackageFragmentRoot root : roots) {
				if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
					return root;
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return null;
	}

	private String getMethodDeclarationReturnType(MethodDeclaration method) {
		ITypeBinding type = method.resolveBinding().getReturnType();
		if (!type.isPrimitive()) {
			checkIsDuplicate(type.getBinaryName());
		}
		return type.getName().toString();
	}

	private String getMethodInvocationReturnType(
			MethodInvocation methodWhichWillThrowSpecificException) {
		IMethodBinding returnType = methodWhichWillThrowSpecificException
				.resolveMethodBinding();
		ITypeBinding type = returnType.getReturnType();
		if (!type.isPrimitive()) {
			checkIsDuplicate(type.getBinaryName());
		}
		return methodWhichWillThrowSpecificException.resolveTypeBinding()
				.getName().toString();
	}

	private void checkIsDuplicate(String content) {
		for (String importContent : importObjects) {
			if (importContent.equalsIgnoreCase(content.trim())) {
				return;
			}
		}
		importObjects.add(content.trim());
	}

	private String getClassNameOfMethodDeclaration(MethodDeclaration method) {
		TypeDeclaration classOfMethod = (TypeDeclaration) method.getParent();
		String className = classOfMethod.resolveBinding().getName().toString();
		checkIsDuplicate(classOfMethod.getName().resolveTypeBinding()
				.getPackage().toString().replace("package", "")
				+ "." + className);
		return className;
	}

	private String getInjectedExceptionType(MethodInvocation invocation) {
		// get method signature from a node of constructor call
		IMethodBinding methodBinding = invocation.resolveMethodBinding();
		List<ITypeBinding> exceptions = new ArrayList<ITypeBinding>();
		if (methodBinding != null) {
			exceptions.addAll(Arrays.asList(methodBinding.getExceptionTypes()));
		}
		if (!exceptions.isEmpty()) {

			ITypeBinding exceptionType = exceptions.get(0);
			String exceptionPackage = exceptionType.getBinaryName();
			if (exceptionPackage.equalsIgnoreCase("java.lang.Exception")) {
				showOneButtonPopUpMenu("Remind you!",
						"It is not allowed to inject super Exception class in AspectJ!");
				return null;
			} else {
				checkIsDuplicate(exceptionPackage);
				return exceptionType.getName().toString();
			}
		}
		return null;
	}

	private MethodInvocation getCloseMethodInvocation(
			List<MethodInvocation> methodInvocations, int badSmellLineNumber) {
		MethodInvocation candidate = null;
		for (MethodInvocation methodInv : methodInvocations) {
			int lineNumberOfCloseInvocation = getStatementLineNumber(methodInv);
			if (lineNumberOfCloseInvocation == badSmellLineNumber) {
				return methodInv;
			}
		}
		return candidate;
	}

	private int getStatementLineNumber(ASTNode node) {
		int lineNumberOfTryStatement = compilationUnit.getLineNumber(node
				.getStartPosition());
		return lineNumberOfTryStatement;
	}

	private MethodDeclaration getMethodDeclarationWhichHasBadSmell(
			IMarker marker) {
		String methodIdx = "";
		try {
			methodIdx = (String) marker
					.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		quickFixCore.setJavaFileModifiable(marker.getResource());
		compilationUnit = quickFixCore.getCompilationUnit();
		return QuickFixUtils.getMethodDeclaration(compilationUnit,
				Integer.parseInt(methodIdx));
	}

	private int getBadSmellLineNumberFromMarker(IMarker marker) {
		int badSmellLineNumber = 0;
		try {
			badSmellLineNumber = (Integer) marker
					.getAttribute(IMarker.LINE_NUMBER);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		return badSmellLineNumber;
	}

}
