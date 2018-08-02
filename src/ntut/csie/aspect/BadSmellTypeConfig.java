package ntut.csie.aspect;

import ntut.csie.analyzer.careless.CloseInvocationExecutionChecker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ntut.csie.util.MethodInvocationCollectorVisitor;
import ntut.csie.util.NodeUtilsTest.MethodInvocationVisitor;
import ntut.csie.util.PopupDialog;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class BadSmellTypeConfig {
	private QuickFixCore quickFixCore = new QuickFixCore();
	private CompilationUnit compilationUnit;
	private List<String> importObjects = new ArrayList<String>();

	private MethodDeclaration methodDeclarationWhichHasBadSmell;
	private int badSmellLineNumber;
	private List<TryStatement> tryStatements;
	private TryStatement tryStatementWillBeInject;
	private List<CatchClause> catchClauses;
	private String exceptionType;
	private String exceptionTypeForThrowFromFinally;
	private String objectTypeOfInjectedMethod;// import用
	private String badSmellType = "";
	private String className;
	private List<String> allMethodInvocationInMain;
	private List<String> methodThrowInSpecificExceptionList;
	private String specificMethodNameInFinal;
	private List<String> collectBadSmellMethods;
	private List<String> collectBadSmellExceptionTypes;
	private HashMap map = new HashMap();
	private int trystatementIndexInMethodDeclaration=-1;

	public enum BadSmellType_enum {
		DummyHandler, EmptyCatchBlock, UnprotectedMainProgram, ExceptionThrownFromFinallyBlock, CarelessCleanup
	}

	BadSmellType_enum badSmellType_enum = null;

	public BadSmellTypeConfig(IMarker marker) {

		badSmellType = getBadSmellType(marker);
		for (BadSmellType_enum tempBadSmellType : BadSmellType_enum.values()) {
			if (tempBadSmellType.name().equals(badSmellType))
				badSmellType_enum = tempBadSmellType;
		}
		methodDeclarationWhichHasBadSmell = getMethodDeclarationWhichHasBadSmell(marker);
		className = getClassNameOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
		badSmellLineNumber = getBadSmellLineNumberFromMarker(marker);
		switch (badSmellType_enum) {

		case DummyHandler:
		case EmptyCatchBlock:
			methodThrowInSpecificExceptionList = new ArrayList<String>();
			tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
			tryStatementWillBeInject = getTargetTryStetment(tryStatements,
					badSmellLineNumber);

			catchClauses = tryStatementWillBeInject.catchClauses();
			exceptionType = getExceptionTypeOfCatchClauseWhichHasBadSmell(
					badSmellLineNumber, catchClauses);

			if (exceptionType == null)
				return;
			List<MethodInvocation> allMethodWhichWillThrowException = getMethodInvocationWhichWillThrowTheSameExceptionAsInput(
					exceptionType, tryStatementWillBeInject);
			for (MethodInvocation objectMethod : allMethodWhichWillThrowException) {
				int specificExceptionSize = objectMethod.resolveMethodBinding().getExceptionTypes().length;
				for(int i=0; i<specificExceptionSize; i++){
					String specificException = objectMethod.resolveMethodBinding().getExceptionTypes()[i].getName();
					if (specificException.equals(getExceptionType())) {
						String methodName = objectMethod.resolveMethodBinding().getName().toString();
						methodThrowInSpecificExceptionList.add(methodName);
						break;
					}
				}
				if(methodThrowInSpecificExceptionList.size() != 0)
					break;
			}
			break;

		case UnprotectedMainProgram:

			exceptionType = "RuntimeException";

			List<MethodInvocation> allMethod = getAllMethodInvocation();
			allMethodInvocationInMain = new ArrayList<String>();

			tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
			// 找main裡面的所有catch、final裡的method
			List<MethodInvocation> methodInTryStatement = new ArrayList<MethodInvocation>();
			methodInTryStatement = addMethodInTryStatement(tryStatements);
			collectMethodInvocationOutOfTry(allMethod, methodInTryStatement);
			break;
		case ExceptionThrownFromFinallyBlock:
			tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
			tryStatementWillBeInject = getTargetTryStetment(tryStatements,
					badSmellLineNumber);
			Block finallyBlock = tryStatementWillBeInject.getFinally();
			MethodInvocation methodInvocationInFinal = getBadSmellInvocationFromFinally(
					badSmellLineNumber, finallyBlock);
			specificMethodNameInFinal = methodInvocationInFinal
					.resolveMethodBinding().getName().toString();
			exceptionType = getExceptionTypeWhichWillThrow(methodInvocationInFinal);// 拿finally指定method的exception type
			MethodInvocationCollectorVisitor visitor = new MethodInvocationCollectorVisitor();
			tryStatementWillBeInject.getBody().accept(visitor);
			MethodInvocation firstMethodWillThrowExceptionInTry = visitor
					.getFirstInvocations().get(0);

			setFirstMethodWillThrowExInTry(firstMethodWillThrowExceptionInTry);
			break;
		case CarelessCleanup:
			importObjects.add("java.io.IOException");
			MethodInvocationCollectorVisitor allMethodInvVisitor = new MethodInvocationCollectorVisitor();
			methodDeclarationWhichHasBadSmell.accept(allMethodInvVisitor);
			tryStatements = getAllTryStatementOfMethodDeclaration(methodDeclarationWhichHasBadSmell);
			boolean noTrystmt=false;
			if(tryStatements.size() == 0){
				noTrystmt = true;
			}
			if(noTrystmt)
				trystatementIndexInMethodDeclaration++;
			else
				getTargetTryStetment(tryStatements, badSmellLineNumber);
			List<MethodInvocation> methodInvocations = allMethodInvVisitor
					.getMethodInvocations();
			MethodInvocation closeMethod = getCloseMethodInvocation(
					methodInvocations, badSmellLineNumber);
			if (closeMethod == null) {
				return;
			}
			collectBadSmellMethods = new ArrayList<String>();
			collectBadSmellExceptionTypes = new ArrayList<String>();
			CloseInvocationExecutionChecker closeChecker = new CloseInvocationExecutionChecker();
			List<ASTNode> methodThrowExBeforeClose = new ArrayList<ASTNode>();
			methodThrowExBeforeClose = closeChecker
					.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(closeMethod);

			// 讓ASTNode轉成MethodInvocation
			MethodInvocationCollectorVisitor invocationVisitor = new MethodInvocationCollectorVisitor();
			for (ASTNode astNodeBeforeClose : methodThrowExBeforeClose) {
				astNodeBeforeClose.accept(invocationVisitor);
			}
			MethodInvocationCollectorVisitor invocationVisitorForTryStmts = new MethodInvocationCollectorVisitor();

			// 收集Try裡的第一個MethodInvocation
			for (TryStatement tmpTry : tryStatements) {
				tmpTry.accept(invocationVisitorForTryStmts);
				invocationVisitorForTryStmts.resetFirstInv();
			}
			for (MethodInvocation eachMethodBeforeClose : invocationVisitor
					.getMethodInvocations()) {
				boolean objectMethodInTryStmt = false;

				// 比對method是不是在tryStatement裡，如果是就把objectMethodInTryStmt=true
				if (tryStatements != null) {
					objectMethodInTryStmt = checkMethodHasSameScope(
							eachMethodBeforeClose,
							invocationVisitorForTryStmts.getMethodInvocations());
				}

				// 把不在tryStatement的method加進collectBadSmellMethods
				if (!objectMethodInTryStmt) {
					collectBadSmellMethods
							.add(getObjMethodName(eachMethodBeforeClose));
					collectBadSmellExceptionTypes
							.add(getExceptionTypeWhichWillThrow(eachMethodBeforeClose));
				}
			}

			// 把tryStatement裡的第一個method加進collectBadSmellMethods
			for (MethodInvocation firstMethodInTry : invocationVisitorForTryStmts
					.getFirstInvocations()) {

				collectBadSmellMethods.add(getObjMethodName(firstMethodInTry));
				collectBadSmellExceptionTypes
						.add(getExceptionTypeWhichWillThrow(firstMethodInTry));
			}

			break;
		}
	}

	private void collectMethodInvocationOutOfTry(
			List<MethodInvocation> allMethod,
			List<MethodInvocation> methodInTryStatement) {
		for (MethodInvocation objectMethod : allMethod) {
			boolean objectMethodOutOfTryStatement = true;
			// 比對getStartPosition
			if (methodInTryStatement != null) {// 先確定這個List不是null
				for (MethodInvocation tmp : methodInTryStatement) {
					if (tmp.getStartPosition() == objectMethod
							.getStartPosition()) {
						objectMethodOutOfTryStatement = false;
						break;
					}
				}
			}
			// allMethodInvocationInMain.add(要產生UT的method)
			if (objectMethodOutOfTryStatement) {
				String tempMethod = "";
				int position;
				String methodName = objectMethod.resolveMethodBinding().getName().toString();
				allMethodInvocationInMain.add(methodName);

			}
		}
	}
	
	private void setFirstMethodWillThrowExInTry(
			MethodInvocation firstMethodWillThrowExceptionInTry) {
		// set method's method name and exception type to map
		// map:{"method name" : "exception type"}
		map.put(getObjMethodName(firstMethodWillThrowExceptionInTry).toString(),
				firstMethodWillThrowExceptionInTry.resolveMethodBinding()
						.getExceptionTypes()[0].getName());
		String importException = firstMethodWillThrowExceptionInTry.resolveMethodBinding().getExceptionTypes()[0].getBinaryName();
		checkIsDuplicate(importException);
	}
	
    //取得try裡面第一個會丟出例外的method裡的名字和其例外類型
	public Map getFirstMethodWillThrowExInTry() {
		return map;
	}

	public String buildUpAspectsFile(String packageChain,
			String filePathAspectJFile) {
		String tempAspectContent = "";
		String Newimports = "";
		String existFileWithNewImports = "";
		String exception = "";
		String space = " ";
		String and = "&&";
		String before = "";
		String call = "";
		String withIn = "";
		String aspectForFinallyBlock = "";
		String AJInsertPositionContent = "";
		File file = new File(filePathAspectJFile);
		switch (badSmellType_enum) {
		case DummyHandler:
		case EmptyCatchBlock:
			for (String importObj : getImportObjects()) {
				Newimports = Newimports + "import " + importObj.trim()
						+ ";\r\n";
			}
			Newimports += "import ntut.csie.RobustaUtils.AspectJSwitch;\n";
			existFileWithNewImports = addNewImports(Newimports, filePathAspectJFile, file);
			exception = getExceptionType();
			if (!tempAspectContent.contains(exception)) {

				AJInsertPositionContent = decideWhereToInsertAJ(
						badSmellType, exception);

				String beforeContent = "\t"
						+ "String name = thisJoinPoint.getSignature().getName();"
						+ "\r\n"
						+ "\t"
						+ "String operation = AspectJSwitch.getInstance().getOperation(name);"
						+ "\r\n"
						+ "\t"
						+ "if (operation.equals(\"f("
						+ exception
						+ ")\"))"
						+ "\r\n"
						+ "\t\t"
						+ "throw new "
						+ exception
						+ "(\"This exception is thrown from " + badSmellType_enum + "'s unit test by using AspectJ.\");"
						+ "\r\n\r\n" + "\t" + "}";
				String newAspectContent = AJInsertPositionContent + "\r\n\r\n"
						+ beforeContent;
				if (file.exists()
						&& existFileWithNewImports.replaceAll("\\s", "").indexOf(
								newAspectContent.replaceAll("\\s", "")) < 0) {
					tempAspectContent += newAspectContent + "\r\n" + "\t";
				} else if (!file.exists()) {
					tempAspectContent += newAspectContent + "\r\n" + "\t";
				}
			}

			break;
		case UnprotectedMainProgram:
			for (String importObj : getImportObjects()) {
				Newimports = Newimports + "import " + importObj.trim()
						+ ";\r\n";
			}
			Newimports += "import ntut.csie.RobustaUtils.AspectJSwitch;\n";
			existFileWithNewImports = addNewImports(Newimports, filePathAspectJFile, file);
			exception = getExceptionType();
			if (!tempAspectContent.contains(exception)) {

				AJInsertPositionContent = decideWhereToInsertAJ(
						badSmellType, exception);

				String beforeContent = "\t"
						+ "String name = thisJoinPoint.getSignature().getName();"
						+ "\r\n"
						+ "\t"
						+ "String operation = AspectJSwitch.getInstance().getOperation(name);"
						+ "\r\n"
						+ "\t"
						+ "if (operation.equals(\"f("
						+ exception
						+ ")\"))"
						+ "\r\n"
						+ "\t\t"
						+ "throw new "
						+ exception
						+ "(\"Main Program is not surround with try/catch.\");"
						+ "\r\n" + "\t" + "}";
				String newAspectContent = AJInsertPositionContent + "\r\n"
						+ beforeContent;
				if (file.exists()
						&& existFileWithNewImports.replaceAll("\\s", "").indexOf(
								newAspectContent.replaceAll("\\s", "")) < 0) {
					tempAspectContent += newAspectContent + "\r\n" + "\t";
				} else if (!file.exists()) {
					tempAspectContent += newAspectContent + "\r\n" + "\t";
				}
			}

			break;
		case ExceptionThrownFromFinallyBlock:
			//import
			for (String importObj : getImportObjects()) {
				Newimports = Newimports + "import " + importObj.trim()
						+ ";\r\n";
			}
			Newimports += "import ntut.csie.RobustaUtils.CustomRobustaException;\n"
						  +"import ntut.csie.RobustaUtils.AspectJSwitch;\n";
			
			//從現有的aj file 加入新的import及原本aj file的內容
			existFileWithNewImports = addNewImports(Newimports, filePathAspectJFile, file);
			//從source code 的 finally block提取aj需要的資訊並建立對應 aj file
			exception = getExceptionType();
			before = "\r\n\tbefore() throws " + getExceptionType() + " : (";
			call = "call" + "(* *." + getMethodInFinal() + "(..) throws "
					+ getExceptionType() + "))";
			withIn = "within" + "(" + getClassName() + "){";

			String finallyExceptionComponent = "\t"
					+ "String name = thisJoinPoint.getSignature().getName();"
					+ "\r\n"
					+ "\t"
					+ "String operation = AspectJSwitch.getInstance().getOperation(name);"
					+ "\r\n"
					+ "\t"
					+ "if (operation.equals(\"f("
					+ exception
					+ ")\"))"
					+ "\r\n"
					+ "\t\t"
					+ "throw new " 
					+ exception
					+ "(\"This Exception is thrown from finally block, so it is a Exception Thrown From Finally Block bad smell.\");"
					+ "\r\n" + "\t" + "}";

			aspectForFinallyBlock =  before + call + space + and + space +withIn
					+ "\r\n" + finallyExceptionComponent + "\r\n\t\r\n\t";
			if(!existFileWithNewImports.replaceAll("\\s", "").contains(aspectForFinallyBlock.replaceAll("\\s", "")))
				tempAspectContent += aspectForFinallyBlock;
			
			//從source code 的 try block提取aj需要的資訊並建立對應 aj file 
			Object exceptionMapKey = getFirstMethodWillThrowExInTry().keySet().toArray()[0];
			String firstMethodExceptionTypeInTry = getFirstMethodWillThrowExInTry().get(exceptionMapKey)
					.toString();
			
			String methodDeclarationName = methodDeclarationWhichHasBadSmell.resolveBinding().getName();
			before = "before()" + ": (";
			call = "call" + "(* *(..) throws "
					+ firstMethodExceptionTypeInTry + "))";
			withIn = "withincode" + "(* " + getClassName() + "." + methodDeclarationName + "(..)){";
			String firstMethodInTryExceptionComponent = "\t"
				+ "String name = thisJoinPoint.getSignature().getName();"
				+ "\r\n"
				+ "\t"
				+ "String operation = AspectJSwitch.getInstance().getOperation(name);"
				+ "\r\n"
				+ "\t"
				+ "if (operation.equals(\"f(CustomRobustaException)\"))"
				+ "\r\n"
				+ "\t\t"
				+ "throw new CustomRobustaException(\"This Exception is thrown from try/catch block, so the bad smell is removed.\");"
				+ "\r\n" + "\t" + "}";
			String aspectForFirstMethodInTry = before + call + space + and + space +withIn + "\r\n"
			+ firstMethodInTryExceptionComponent;
			
			if(!existFileWithNewImports.replaceAll("\\s", "").contains(aspectForFirstMethodInTry.replaceAll("\\s", "")))
				tempAspectContent += aspectForFirstMethodInTry;
			break;
		case CarelessCleanup:
			String aspectJClassTitle = "\r\n" + "public aspect "
					+ getClassName() + "AspectException {";
			tempAspectContent = "";
			Newimports = "";
			existFileWithNewImports = "";
			for (String importObj : getImportObjects()) {
				Newimports = Newimports + "import " + importObj.trim()
						+ ";\r\n";
			}
			Newimports += "import ntut.csie.RobustaUtils.AspectJSwitch;\n";
			file = new File(filePathAspectJFile);
			existFileWithNewImports = addNewImports(Newimports, filePathAspectJFile, file);

			methodDeclarationName = methodDeclarationWhichHasBadSmell.resolveBinding().getName();
			before = "after()" + ": (";
			call = "call" + "(* *.close(..) throws IOException" + "))";
			withIn = "withincode" + "(* "+ getClassName() + ".*(..)){";
			String beforeContent = "\t"
					+ "String name = thisJoinPoint.getSignature().getName();"
					+ "\r\n"
					+ "\t"
					+ "String operation = AspectJSwitch.getInstance().getOperation(name);"
					+ "\r\n"
					+ "\t"
					+ "if (operation.equals(\"AOPCheckResources\"))"
					+ "\r\n"
					+ "\t\t"
					+ "AspectJSwitch.getInstance().checkResource();"
					+ "\r\n" + "\t" + "}";
			aspectForFinallyBlock = "\r\n\t"+before + call + space + and + space + withIn
					+ "\r\n" + beforeContent + "\r\n\t";

			
			for (String CarelessException : getCollectBadSmellExceptionTypes()) {
				if (!tempAspectContent.contains("f(" + CarelessException + ")")) {
					AJInsertPositionContent = decideWhereToInsertAJ(
							badSmellType, CarelessException);
					beforeContent = "\t"
							+ "String name = thisJoinPoint.getSignature().getName();"
							+ "\r\n"
							+ "\t"
							+ "String operation = AspectJSwitch.getInstance().getOperation(name);"
							+ "\r\n" + "\t" + "if (operation.equals(\"f("
							+ CarelessException + ")\"))" + "\r\n" + "\t\t"
							+ "throw new " + CarelessException
							+ "(\"This Exception is thrown from Robusta.\");"
							+ "\r\n" + "\t" + "}";
					String newAspectContent = AJInsertPositionContent
							+ "\r\n" + beforeContent;
					if (file.exists()
							&& existFileWithNewImports.replaceAll("\\s", "").indexOf(
									newAspectContent.replaceAll("\\s", "")) < 0) {
						tempAspectContent += newAspectContent + "\r\n" + "\t";
					} else if (!file.exists()) {
						if (tempAspectContent
								.contains("RuntimeException(\"This exception is thrown from Robusta's AspectJ.\")"))
							tempAspectContent += newAspectContent + "\r\n"
									+ "\t";
						else
							tempAspectContent += aspectForFinallyBlock
									+ newAspectContent + "\r\n" + "\t";
					}
				}
			}
			break;

		}
		return getCompleteAJContent(Newimports, tempAspectContent, existFileWithNewImports,
				file, packageChain);

	}

	private String getCompleteAJContent(String Newimports,
			String tempAspectContent, String existFileWithNewImports, File file,
			String packageChain) {
		String beforeEnd = "\r\n" + "}";
		String aspectJClassTitle = "\r\n" + "public aspect " + getClassName()
				+ "AspectException {";
		String ignoreMethodLogic = "\r\n\t"+ "Object around():(call(* *(..))||call(*.new(..))) && withincode (* "+ getClassName()+".*(..)){"+"\r\n"
						+ "\t\tString method = thisJoinPoint.toString();\r\n"
						+ "\t\tif(method.contains(\"Exception(Throwable)\")){\n"
						+ "\t\t\treturn proceed();\r\n"
						+ "\t\t}else{\n"
						+"\t\t\treturn null;\n\t\t}\r\n\t}";
		String aspectJFileConetent = "package " + packageChain + ";"
				+ "\r\n\r\n" + Newimports + aspectJClassTitle + "\r\n" + "\t"
				+tempAspectContent+"\r\n";
		if(badSmellType.equals("ExceptionThrownFromFinallyBlock")) {
			if (!file.exists()) {
				ignoreMethodLogic = "\r\n\t"+ "Object around():(call(* *(..))||call(*.new(..))) && withincode (* "+ getClassName()+".*(..)){"+"\r\n"
									+"\t\ttry{\n"
									+"\t\t\treturn proceed();\r\n"
									+"\t\t}catch(Exception e){\n"
									+"\t\t\treturn null;\n\t\t}\r\n\t}";
				return aspectJFileConetent+ "\t"+ ignoreMethodLogic + beforeEnd;
			} else {
				String completeAJContent = "";
				String existContentSplitByObjectAround[] = existFileWithNewImports.split("Object around");
				
				String AJContentWithoutIgnoreMethodLogic = existContentSplitByObjectAround[0];
				ignoreMethodLogic = existContentSplitByObjectAround[1];
				completeAJContent = AJContentWithoutIgnoreMethodLogic.concat(tempAspectContent)+"\r\n\t"
																	.concat("Object around"+ignoreMethodLogic)+"\r\n\t"
																	.concat(beforeEnd);
				return completeAJContent;
			}
		} else if(badSmellType.equals("DummyHandler")||badSmellType.equals("EmptyCatchBlock")) {
			if (!file.exists()) {
				return aspectJFileConetent+ "\t"+ ignoreMethodLogic + beforeEnd;
			} else {
				String completeAJContent = "";
                String existContentSplitByObjectAround[] = existFileWithNewImports.split("Object around");
                String AJContentWithoutIgnoreMethodLogic = existContentSplitByObjectAround[0];
                ignoreMethodLogic = existContentSplitByObjectAround[1];
                completeAJContent = AJContentWithoutIgnoreMethodLogic.concat(tempAspectContent)
                													 .concat("Object around"+ignoreMethodLogic)+"\r\n\t"
                													 .concat(beforeEnd);
				return completeAJContent;
			}
		}
		else if (badSmellType.equals("UnprotectedMainProgram")){
			 	if(!file.exists())
			 		return aspectJFileConetent+beforeEnd;
			 	else
			 		return existFileWithNewImports+beforeEnd;
		}
		else if(badSmellType.equals("CarelessCleanup")){
			if(!file.exists()){
				ignoreMethodLogic = "\r\n\t"+ "Object around():(call(* *(..))||call(*.new(..))) && withincode (* "+ getClassName()+".*(..)){"+"\r\n"
				+"\t\ttry{\n"
				+"\t\t\treturn proceed();\r\n"
				+"\t\t}catch(Exception e){\n"
				+"\t\t\treturn null;\n\t\t}\r\n\t}";
				return aspectJFileConetent+"\t"+ignoreMethodLogic+beforeEnd;
			}else{
				String completeAJContent = "";
				String existContentSplitByObjectAround[] = existFileWithNewImports.split("Object around");
				
				String AJContentWithoutIgnoreMethodLogic = existContentSplitByObjectAround[0];
				ignoreMethodLogic = existContentSplitByObjectAround[1];
				completeAJContent = AJContentWithoutIgnoreMethodLogic.concat(tempAspectContent)
																	.concat("Object around"+ignoreMethodLogic)+"\r\n\t"
																	.concat(beforeEnd);
				return completeAJContent;
			}
		}
		else
			return aspectJFileConetent;

	}

	private String addNewImports(String Newimports, String filePathAspectJFile,
			File file) {
		String result = "";
		if (file.exists()) {
			try {
				FileReader fr = new FileReader(filePathAspectJFile);
				BufferedReader br = new BufferedReader(fr);
				String temp;
				while ((temp = br.readLine()) != null) {
					if (temp.indexOf("public aspect ") > -1)
						result = result + Newimports + "\r\n";
					else if (Newimports.indexOf(temp) > -1)
						continue;
					result = result + temp + "\r\n";
				}
				result = result.substring(0, result.lastIndexOf('}'));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;

	}

	private String decideWhereToInsertAJ(String badSmellType, String exception) {
		String before;
		String call;
		String withIn;
		String space = " ";
		String and = "&&";

		if (badSmellType.equals("UnprotectedMainProgram")) {
			before = "before()  : (";
			call = "call" + "(* *(..) ) )";
			withIn = "withincode(* " + getClassName() + ".main(..) ){";
		} else if (badSmellType.equals("CarelessCleanup")) {
			String methodDeclarationName = methodDeclarationWhichHasBadSmell.resolveBinding().getName();
			before = "\r\n\tbefore() throws " + exception + ": (";
			call = "call" + "(* *." +getCollectBadSmellMethods().get(trystatementIndexInMethodDeclaration)+
					"(..) throws " + exception
					+ ") )";
			withIn = "withincode" + "(* " + getClassName() + "."+methodDeclarationName+"(..)){";
		} else {
			String methodDeclarationName = methodDeclarationWhichHasBadSmell.resolveBinding().getName();
			before = "\r\n\tbefore() throws " + exception + ": (";
			call = "call" + "(* *(..) throws " + exception
					+ ") )";
			withIn = "withincode" + "(* " + getClassName() + "."+methodDeclarationName+"(..)){";
		}
		
		return before + call + space + and + space + withIn;

	}

	private boolean checkMethodHasSameScope(MethodInvocation storeMethod,
			List<MethodInvocation> checkMethodCollect) {
		for (MethodInvocation eachCheckMethod : checkMethodCollect) {
			if (storeMethod.getStartPosition() == eachCheckMethod
					.getStartPosition())
				return true;
		}

		return false;
	}

	private String getObjMethodName(MethodInvocation method) {
		String objMethod = method.resolveMethodBinding().getName().toString();
		return objMethod;
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

	// 回傳一個list包含try、catch、finally裡的所有method
	private List<MethodInvocation> addMethodInTryStatement(
			List<TryStatement> tryStatements) {
		MethodInvocationCollectorVisitor getAllMethodInvocation = new MethodInvocationCollectorVisitor();

		for (TryStatement ts : tryStatements) {
			ts.accept(getAllMethodInvocation);
		}
		return getAllMethodInvocation.getMethodInvocations();
	}

	public String getAssertion(MethodDeclaration methodDeclaration) {
		String assertion = "";
		int modifierNum = methodDeclaration.getModifiers();
		switch (badSmellType_enum) {
		case DummyHandler:
		case EmptyCatchBlock:
			if (Modifier.isPrivate(modifierNum)) {
				assertion = "String exceptionMessage = e.getCause().getMessage().toString();\n\t\t\t"
						+ "Assert.assertTrue(exceptionMessage.contains(\"This exception is thrown from " + badSmellType_enum + "'s unit test by using AspectJ.\"));";
			} else {
				assertion = "String exceptionMessage = e.getMessage().toString();\n\t\t\t"
						+ "Assert.assertTrue(exceptionMessage.contains(\"This exception is thrown from " + badSmellType_enum + "'s unit test by using AspectJ.\"));";
			}
			break;
		case ExceptionThrownFromFinallyBlock:
			if (Modifier.isPrivate(modifierNum)) {
				assertion = "catch(Exception e){\n\t\t\t"
						+ "e.printStackTrace();\n\t\t\t"
						+ "String exceptionMessage = e.getCause().getMessage().toString();\n\t\t\t"
						+ "Assert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",exceptionMessage);\n\t\t"
						+ "}";
			} else {
				assertion = "catch (CustomRobustaException e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.assertEquals(\"This Exception is thrown from try/catch block, so the bad smell is removed.\",e.getMessage());"
						+ "\n\t\t} catch (Exception e) {\n\t\t\te.printStackTrace();\n\t\t\tAssert.fail(\"Exception is thrown from finally block.\");"+"\n\t\t}";
			}
			break;
		}
		return assertion;
	}

	public String getMethodCaller(MethodDeclaration methodDeclaration) {
		String methodCallerWay = null;
		String methodName = methodDeclaration.getName().toString();
		int modifierNum = methodDeclaration.getModifiers();
		if (Modifier.isStatic(modifierNum)) {

			methodCallerWay = getClassName() + "." + methodName + "("+ getMDParameters(methodDeclaration) +");";
		} else if (Modifier.isPrivate(modifierNum)) {
			methodCallerWay = getClassName() + " object = new "
					+ getClassName() + "();\n\t\t\t"
					+ "Method privateMethod = " + getClassName()
					+ ".class.getDeclaredMethod(\"" + methodName
					+ "\");\n\t\t\t"
					+ "privateMethod.setAccessible(true);\n\t\t\t"
					+ "privateMethod.invoke(object);";

		} else {
			methodCallerWay = getClassName() + " object = new "
					+ getClassName() + "();\n\t\t\t" + "object." + methodName
					+ "(" + getMDParameters(methodDeclaration) + ");";
		}
		return methodCallerWay;
	}

	private String getMDParameters(MethodDeclaration methodDeclaration) {
		ITypeBinding[] a = methodDeclaration.resolveBinding().getParameterTypes();
		String[] MDParametersCollection = new String[a.length];
		
		int index=0;
		for(ITypeBinding  parameter: a) {
			if(parameter.isPrimitive()) {
				if(parameter.getQualifiedName().equals("byte"))
					MDParametersCollection[index] ="(byte)0";
				else if(parameter.getQualifiedName().equals("short"))
					MDParametersCollection[index] ="(short)0";
				else if(parameter.getQualifiedName().equals("int"))
					MDParametersCollection[index] ="0";
				else if(parameter.getQualifiedName().equals("long"))
					MDParametersCollection[index] ="0L";
				else if(parameter.getQualifiedName().equals("float"))
					MDParametersCollection[index] ="0.0f";
				else if(parameter.getQualifiedName().equals("double"))
					MDParametersCollection[index] ="0.0d";
				else if(parameter.getQualifiedName().equals("char"))
					MDParametersCollection[index] ="\'0\'";
				else if(parameter.getQualifiedName().equals("boolean"))
					MDParametersCollection[index] ="false";
			} else {
				MDParametersCollection[index] = "null";
			}
			index++;
		}
		String MDParameters = Arrays.toString(MDParametersCollection).replaceAll("\\[", "").replaceAll("\\]", "");
		return MDParameters;
	}

	public String getMethodInFinal() {
		return specificMethodNameInFinal;
	}

	private String getExceptionTypeWhichWillThrow(MethodInvocation method) {
		String importException = method.resolveMethodBinding()
				.getExceptionTypes()[0].getBinaryName();
		checkIsDuplicate(importException);

		String exceptionTypes = method.resolveMethodBinding()
				.getExceptionTypes()[0].getName();
		return exceptionTypes;
	}

	private MethodInvocation getBadSmellInvocationFromFinally(
			int badSmellLineNumber, Block finallyBlock) {
		MethodInvocationCollectorVisitor getAllMethodInvocation = new MethodInvocationCollectorVisitor();
		finallyBlock.accept(getAllMethodInvocation);
		List<MethodInvocation> methodThrowExceptionList = getAllMethodInvocation
				.getMethodInvocations();

		for (MethodInvocation m : methodThrowExceptionList)
			if (getStatementLineNumber(m) == badSmellLineNumber)
				return m;
		return null;
		
	}

	public List<String> getAllMethodInvocationInMain() {
		return allMethodInvocationInMain;
	}

	private List<MethodInvocation> getAllMethodInvocation() {
		MethodInvocationCollectorVisitor getAllMethodInvocation = new MethodInvocationCollectorVisitor();
		methodDeclarationWhichHasBadSmell.accept(getAllMethodInvocation);
		List<MethodInvocation> methodThrowExceptionList = getAllMethodInvocation
				.getMethodInvocations();
		return methodThrowExceptionList;
	}

	public List<String> getCollectBadSmellMethods() {
		return collectBadSmellMethods;
	}

	public List<String> getCollectBadSmellExceptionTypes() {
		return collectBadSmellExceptionTypes;
	}

	public List<String> getFirstInvocationSameAsCatch() {
		return methodThrowInSpecificExceptionList;
	}

	public MethodDeclaration getMethodDeclarationWhichHasBadSmell() {
		return methodDeclarationWhichHasBadSmell;
	}

	public int getBadSmellLineNumber() {
		return badSmellLineNumber;
	}

	public List<TryStatement> getTryStatements() {
		return tryStatements;
	}

	public TryStatement getTryStatementWillBeInject() {
		return tryStatementWillBeInject;
	}

	public List<CatchClause> getCatchClauses() {
		return catchClauses;
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public String getBadSmellType() {
		return badSmellType;
	}

	public String getClassName() {
		return className;
	}

	public List<String> getImportObjects() {
		return importObjects;
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

	private List<TryStatement> getAllTryStatementOfMethodDeclaration(
			MethodDeclaration methodDeclaration) {
		// 取出在method中所有的try block
		FindAllTryStatementVisitor visitor = new FindAllTryStatementVisitor();
		methodDeclaration.accept(visitor);
		return visitor.getTryStatementsList();
	}

	private TryStatement getTargetTryStetment(List<TryStatement> tryStatements,
			int badSmellLineNumber) {
		TryStatement candidate = null;
		for (TryStatement tryStatement : tryStatements) {
			int lineNumberOfTryStatement = getStatementLineNumber(tryStatement);
			if (lineNumberOfTryStatement < badSmellLineNumber) {
				candidate = tryStatement;
				trystatementIndexInMethodDeclaration++;
			} else {
				break;
			}
		}
		return candidate;
	}

	private int getStatementLineNumber(ASTNode node) {
		int lineNumberOfTryStatement = compilationUnit.getLineNumber(node
				.getStartPosition());
		return lineNumberOfTryStatement;
	}

	private String getExceptionTypeOfCatchClauseWhichHasBadSmell(
			int badSmellLineNumber, List<CatchClause> catchClauses) {
		for (CatchClause catchBlock : catchClauses) {
			int catchClauseLineNumber = compilationUnit
					.getLineNumber(catchBlock.getStartPosition());
			if (badSmellLineNumber == catchClauseLineNumber) {
				ITypeBinding exceptionType = catchBlock.getException()
						.getType().resolveBinding();
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
		}
		return null;
	}

	private void showOneButtonPopUpMenu(final String title, final String msg) {
		PopupDialog.showDialog(title, msg);
	}

	private void checkIsDuplicate(String content) {
		for (String importContent : importObjects) {
			if (importContent.equalsIgnoreCase(content.trim())) {
				return;
			}
		}
		importObjects.add(content.trim());
	}

	private List<MethodInvocation> getMethodInvocationWhichWillThrowTheSameExceptionAsInput(
			String exceptionType, TryStatement tryStatementWillBeInject) {
		Block body = tryStatementWillBeInject.getBody();
		MethodInvocationCollectorVisitor getAllMethodInvocation = new MethodInvocationCollectorVisitor();
		body.accept(getAllMethodInvocation);
		List<MethodInvocation> methodInv = getAllMethodInvocation
				.getMethodInvocations();
		List<MethodInvocation> MethodInvocationWithSpecificException = new ArrayList<MethodInvocation>();
		FindThrowSpecificExceptionStatementVisitor getTheFirstMethodInvocationWithSpecificException = null;
		for (MethodInvocation method : methodInv) {
			getTheFirstMethodInvocationWithSpecificException = new FindThrowSpecificExceptionStatementVisitor(
					exceptionType);
			method.accept(getTheFirstMethodInvocationWithSpecificException);
			if (getTheFirstMethodInvocationWithSpecificException
					.isGetAMethodInvocationWhichWiThrowException()) {
				MethodInvocationWithSpecificException
						.add(getTheFirstMethodInvocationWithSpecificException
								.getMethodInvocationWhichWillThrowException());
			}
		}

		return MethodInvocationWithSpecificException;
	}

	private String getTheObjectTypeOfMethodInvocation(
			MethodInvocation methodWhichWillThrowSpecificException) {
		FindExpressionObjectOfMethodInvocationVisitor theFirstExpressionVisitor = new FindExpressionObjectOfMethodInvocationVisitor();
		methodWhichWillThrowSpecificException.accept(theFirstExpressionVisitor);
		checkIsDuplicate(theFirstExpressionVisitor.getObjectPackageName());
		return theFirstExpressionVisitor.getObjectName();
	}

	private String getClassNameOfMethodDeclaration(MethodDeclaration method) {
		TypeDeclaration classOfMethod = (TypeDeclaration) method.getParent();
		String className = classOfMethod.resolveBinding().getName().toString();
		checkIsDuplicate(classOfMethod.getName().resolveTypeBinding()
				.getPackage().toString().replace("package", "")
				+ "." + className);
		return className;
	}

	private String getBadSmellType(IMarker marker) {
		String badSmellType = "";
		try {
			badSmellType = (String) marker
					.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}

		badSmellType = badSmellType.replace("_", "");
		return badSmellType;
	}

}
