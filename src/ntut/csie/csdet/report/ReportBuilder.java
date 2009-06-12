package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.csdet.visitor.MainAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jdom.Attribute;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportBuilder {
	private static Logger logger = LoggerFactory.getLogger(ReportBuilder.class);

	//Report的資料
	private ReportModel model;
	//是否偵測全部的Package
	private Boolean isAllPackage;
	//儲存filter條件的List
	private List<String> filterRuleList = new ArrayList<String>();

	/**
	 * 建立Report
	 * @param project
	 * @param model
	 */
	public ReportBuilder(IProject project,ReportModel model)
	{
		this.model = model;
		model.setProjectName(project.getName());
		
		//將User對於Filter的設定存下來
		getFilterSettings();
		
		//解析專案
		analysisProject(project);

		//產生HTM
		SmellReport createHTM = new SmellReport(model);
		createHTM.build();

		//產生圖表
		BarChart smellChart = new BarChart(model);
		smellChart.build();
	}

	/**
	 * 將User對於Filter的設定存下來
	 * @return
	 */
	private void getFilterSettings() {
		Element root = JDomUtil.createXMLContent();

		//如果是null表示XML檔是剛建好的,還沒有EHSmellFilterTaq的tag,直接跳出去
		if(root.getChild(JDomUtil.EHSmellFilterTaq) != null) {

			// 這裡表示之前使用者已經有設定過preference了,去取得相關偵測設定值
			Element filter = root.getChild(JDomUtil.EHSmellFilterTaq).getChild("filter");
			isAllPackage = Boolean.valueOf(filter.getAttribute("IsAllPackage").getValue());

			//若不是偵測全部的Project，則把Rule條件儲存
			if (!isAllPackage) {
				List<Attribute> filterList = filter.getAttributes();
				
				for (int i=0; i<filterList.size(); i++) {
					//略過不屬於Rule的設定
					if (filterList.get(i).getQualifiedName() == "IsAllPackage")
						continue;
					//若Rule設成true才儲存
					if (Boolean.valueOf(filterList.get(i).getValue()))
						filterRuleList.add(filterList.get(i).getQualifiedName());
				}
			}
		}
	}
	
	/**
	 * 儲存Smell資訊
	 * @param icu
	 * @param isRecord
	 * @param newPackageModel
	 */
	private void setSmellInfo (ICompilationUnit icu, boolean isRecord, PackageModel newPackageModel) {
		ExceptionAnalyzer visitor = null;
		CodeSmellAnalyzer csVisitor = null;
		MainAnalyzer mainVisitor = null;

		//目前method內的ignore Exception資訊
		List<CSMessage> ignoreExList = new ArrayList<CSMessage>();
		//目前method內的dummy handler資訊
		List<CSMessage> dummyList = new ArrayList<CSMessage>();
		//目前method內的Nested Try Block資訊
		List<CSMessage> nestedTryList = new ArrayList<CSMessage>(); 
		//目前method內的Unprotected Main資訊
		List<CSMessage> unprotectedMain = new ArrayList<CSMessage>();

		// 目前method的Exception資訊
		List<RLMessage> currentMethodExList = null;
		// 目前method的RL Annotation資訊
		List<RLMessage> currentMethodRLList = null;
		List<CSMessage> spareHandler = null;

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit root = (CompilationUnit) parser.createAST(null);

		ASTMethodCollector methodCollector = new ASTMethodCollector();

		root.accept(methodCollector);
		//取得專案中所有的method
		List<ASTNode> methodList = methodCollector.getMethodList();

		//目前的Method AST Node
		ASTNode currentMethodNode = null;
		int methodIdx = -1;
		for (ASTNode method : methodList) {
			methodIdx++;

			visitor = new ExceptionAnalyzer(root, method.getStartPosition(), 0);
			method.accept(visitor);
			currentMethodNode = visitor.getCurrentMethodNode();
			currentMethodRLList = visitor.getMethodRLAnnotationList();

			//找尋專案中所有的ignore Exception
			csVisitor = new CodeSmellAnalyzer(root);
			method.accept(csVisitor);
			//取得專案中的ignore Exception
			if(csVisitor.getIgnoreExList() != null)
				ignoreExList.addAll(csVisitor.getIgnoreExList());

			//取得專案中dummy handler
			if(csVisitor.getDummyList() != null)
				dummyList.addAll(csVisitor.getDummyList());

			//取得專案中的Nested Try Block
			if(visitor.getNestedTryList() != null)
				nestedTryList.addAll(visitor.getNestedTryList());

			//尋找該method內的unprotected main program
			mainVisitor = new MainAnalyzer(root);
			method.accept(mainVisitor);
			if(mainVisitor.getUnprotedMainList() != null)
				unprotectedMain.addAll(mainVisitor.getUnprotedMainList());
		}

		//記錄到ReportModel中
//		if (isRecord)
		ClassModel newClassModel = new ClassModel();
		newClassModel.setClassName(icu.getElementName());
		newClassModel.setIgnoreExList(ignoreExList);
		newClassModel.setDummyList(dummyList);
		newClassModel.setUnprotectedMain(unprotectedMain);
		newClassModel.setNestedTryList(nestedTryList);
		newPackageModel.addClassModel(newClassModel);

		model.addIgnoreTotalSize(ignoreExList.size());
		model.addDummyTotalSize(dummyList.size());
		model.addUnMainTotalSize(unprotectedMain.size());
		model.addNestedTotalTrySize(nestedTryList.size());
	}

	/**
	 * 分析專案
	 * @param project
	 */
	private void analysisProject(IProject project) {
		//取得專案的路徑
		IJavaProject javaP = JavaCore.create(project);
		String workPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		model.setProjectPath(workPath + javaP.getPath());

		try {
			List<IPackageFragmentRoot> root = getSourcePaths(javaP);
			for (int i = 0 ; i < root.size() ; i++){
				//取得Root底下的所有Package
				IJavaElement[] packages = root.get(i).getChildren();
				for (IJavaElement ije : packages) {
					if (ije.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
						IPackageFragment pk = (IPackageFragment)ije;

						//判斷是否要記錄
						boolean isRecord = determineRecod(pk);

						//取得Package底下的class
						ICompilationUnit[] compilationUnits = pk.getCompilationUnits();
						PackageModel newPackageModel = null;

						//若要紀錄則新增Package
						if (isRecord) {
							if (compilationUnits.length != 0)
								newPackageModel = model.addSmellList(pk.getElementName());

							//取得Package底下的所有class的smell資訊
							for (int k = 0; k < compilationUnits.length; k++) {
								setSmellInfo(compilationUnits[k], isRecord, newPackageModel);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] EXCEPTION ",e);
		}
	}

	/**
	 * 判斷是否要紀錄這個Package的Smell資訊
	 * @param pk
	 * @return
	 */
	private boolean determineRecod(IPackageFragment pk) {
		//若偵測全部Package 則全部紀錄
		if (isAllPackage) {
			return true;
		} else {
			for (String filterRule : filterRuleList) {
				//判斷是否為"Package.*"的Rule
				if (filterRule.indexOf(".EH_STAR") != -1) {
					if ((filterRule.length() -8) == filterRule.indexOf(".EH_STAR")) {
						String temp = filterRule.substring(0, filterRule.length()-8);
						if (pk.getElementName().length() >= temp.length()) {
							//若Package前半段長度的名稱與filter rule一樣則偵測
							if (pk.getElementName().substring(0,temp.length()).equals(temp))
								return true;
						}
					}
				//若Package與FilterRule一樣則紀錄
				} else if (pk.getElementName().equals(filterRule)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 取得PackageFragmentRoot List (過濾jar)
	 */
	public List<IPackageFragmentRoot> getSourcePaths(IJavaProject project)throws JavaModelException {
        
	    List<IPackageFragmentRoot> sourcePaths =
	            new ArrayList<IPackageFragmentRoot>();

	    IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();

	    for (IPackageFragmentRoot r : roots) {
	    	if (!r.getPath().toString().endsWith(".jar")) {
	    		sourcePaths.add(r);
	    	}
	    }
	    return sourcePaths;
	}
}
