package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.jdom.Attribute;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OverLogging的Code Smell
 * @author Shiau
 *
 */
public class OverLoggingDetector {
	private static Logger logger = LoggerFactory.getLogger(OverLoggingDetector.class);

	//儲存所找到的OverLoging 
	private List<CSMessage> overLoggingList = new ArrayList<CSMessage>();
	//AST Tree的root(檔案名稱)
	private CompilationUnit root;
	//最底層的Method
	private MethodDeclaration startMethod;
	//儲存使用者設定的遇到轉型Exception還偵不偵測設定
	private boolean detectTransExSet = false;
	//儲存偵測"Library的Name"和"是否Library"
	//store使用者要偵測的library名稱，和"是否要偵測此library"
	private TreeMap<String, String> libMap = new TreeMap<String, String>();

	/**
	 * Constructor
	 * @param root
	 * @param method
	 */
	public OverLoggingDetector(CompilationUnit root, ASTNode method) {
		this.root = root;
		startMethod = (MethodDeclaration) method;
	}
	
	/**
	 * 尋查Method
	 */
	public void detect() {
		//將MethodDeclaration(AST)轉換成IMethod
		IMethod method = (IMethod) startMethod.resolveBinding().getJavaElement();

		//將User對於OverLogging的設定存下來
		getOverLoggingSettings();
		
		//解析AST看有沒有發生Logging又throw Exception (空白字串表示最底層Method)
		LoggingThrowAnalyzer visitor = new LoggingThrowAnalyzer(root, libMap);
		startMethod.accept(visitor);
		//是否繼續偵測
		boolean isTrace = visitor.getIsKeepTrace();
		//儲存最底層Exception
		String baseException = visitor.getBaseException();

		if (isTrace) {
			//判斷Catch Throw的Exception是否與Method Throw的Exception相同
			if (isCTEqualMT(startMethod,baseException)) {
				//若使用者設定即使Exception轉型後仍設定，則把判斷Exception設成空白("")
				if (detectTransExSet)
					baseException = "";

				//使用遞迴判斷是否發生OverLogging，若OverLogging則記錄其Message
				if (detectOverLogging(method, baseException)) {
					overLoggingList = visitor.getOverLoggingList();
				}
			}
		}
	}
	
	/**
	 * 判斷是否OverLogging
	 * @param method		被Call的Method
	 * @param baseException	最底層的Exception
	 * @return				是否OverLogging
	 */
	private boolean detectOverLogging(IMethod method,String baseException)
	{
		//有沒有找到Logging動作
		boolean isOverLogging;

		//TODO 曾發生Internal Error
		//往下一層邁進
		MethodWrapper currentMW = new CallHierarchy().getCallerRoot(method);				
		MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());

		//若有Caller
		if (calls.length != 0) {
			for (MethodWrapper mw : calls) {
				if (mw.getMember() instanceof IMethod) {
					IMethod callerMethod = (IMethod) mw.getMember();

					//避免Recursive，若Caller Method仍然是自己就不偵測
					if (callerMethod.equals(method))
						continue;

					//IMethod轉成MethodDeclaration ASTNode
					MethodDeclaration methodNode = transMethodNode(callerMethod);

					//methodNode出錯時，就不偵測
					if (methodNode == null)
						continue;

					//解析AST看有沒有OverLogging
					MethodDeclaration md = transMethodNode(method);
					String classInfo = md.resolveBinding().getDeclaringClass().getQualifiedName();

					//解析AST看使用此Method的Catch Block中有沒有發生Logging
					LoggingAnalyzer visitor = new LoggingAnalyzer(baseException, classInfo,
													method.getElementName(), libMap);
					methodNode.accept(visitor);

					//是否有Logging
					boolean isLogging = visitor.getIsLogging();
					//是否繼續Trace
					boolean isTrace = visitor.getIsKeepTrace();

					//若有Logging動作則回傳有Logging不再往下偵測
					if (isLogging)
						return true;

					//是否往上一層追蹤
					if (isTrace) {
						//判斷Method Throws的Exception與最底層的Throw Exception是否相同
						if (!isCTEqualMT(methodNode,baseException))
							continue;

						isOverLogging = detectOverLogging(callerMethod, baseException);
						
						//若上一層結果為OverLoggin就回傳true，否則繼續
						if (isOverLogging)
							return true;
						else
							continue;
					}
				}
			}
		}
		//沒有Caller，或所有Caller跑完仍沒有
		return false;
	}
	
	/**
	 * 判斷Catch Throw的Exception是否與Method Throw的Exception相同
	 * @param method
	 * @param catchThrowEx
	 * @return
	 */
	private boolean isCTEqualMT(MethodDeclaration method, String catchThrowEx) {
		//若使用者設定為轉換Exception後還繼續偵測，則不判斷
		if (detectTransExSet)
			return true;
		
		//取得Method的Throw Exception
		ITypeBinding[] throwExList = method.resolveBinding().getExceptionTypes();
		//TODO 先不考慮複數個
		if (throwExList != null && throwExList.length == 1) {
			//若Throw Exception 與 Method Throw Exception一樣才偵測
			if (throwExList[0].getName().equals(catchThrowEx))
				return true;
		}
		return false;
	}

	/**
	 * 轉換成ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		
		try {
			//Parser Jar檔時，會取不到ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			//產生AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode ast = parserAST.createAST(null);

			//取得AST的Method部份
			ASTNode methodNode = NodeFinder.perform(ast, method.getSourceRange().getOffset(), method.getSourceRange().getLength());

			//若此ASTNode屬於MethodDeclaration，則轉型
			if(methodNode instanceof MethodDeclaration) {
				md = (MethodDeclaration) methodNode;
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}

		return md;
	}

	/**
	 * 將User對於OverLogging的設定存下來
	 * @return
	 */
	private void getOverLoggingSettings(){		
		Element root = JDomUtil.createXMLContent();
		//如果是null表示XML檔是剛建好的,還沒有OverLogging的tag,直接跳出去
		if(root.getChild(JDomUtil.OverLoggingTag) != null){
			//這裡表示之前使用者已經有設定過preference了,去取得相關偵測設定值
			Element overLogging = root.getChild(JDomUtil.OverLoggingTag);
			Element rule = overLogging.getChild("rule");
			String log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			String javaLogger = rule.getAttribute(JDomUtil.java_Logger).getValue();

			/// 把內建偵測加入到名單內 ///
			//把log4j和javaLog加入偵測內
			if (log4jSet.equals("Y"))
				libMap.put("org.apache.log4j",null);
			if (javaLogger.equals("Y"))
				libMap.put("java.util.logging",null);

			Element libRule = overLogging.getChild("librule");
			// 把外部Library和Statement儲存在List內
			List<Attribute> libRuleList = libRule.getAttributes();

			/// 把使用者所設定的Exception轉型偵不偵設定 ///
			Element exrule = overLogging.getChild("exrule");
			String exSet = exrule.getAttribute(JDomUtil.transException).getValue();
			detectTransExSet = exSet.equals("Y");
			
			//把外部的Library加入偵測名單內
			for (int i=0;i<libRuleList.size();i++) {
				if (libRuleList.get(i).getValue().equals("Y")) {
					String temp = libRuleList.get(i).getQualifiedName();					

					//若有.*為只偵測Library，Value設成1
					if (temp.indexOf(".EH_STAR")!=-1){
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0,pos),"1");
					//若有*.為只偵測Method，Value設成2
					}else if (temp.indexOf("EH_STAR.") != -1){
						libMap.put(temp.substring(8),"2");
					//都沒有為都偵測，Key設成Library，Value設成Method
					}else if (temp.lastIndexOf(".")!=-1){
						int pos = temp.lastIndexOf(".");
						//若已經存在，則不加入
						if(!libMap.containsKey(temp.substring(0,pos)))
							libMap.put(temp.substring(0,pos),temp.substring(pos+1));
					//若有其它形況則設成Method
					}else{
						libMap.put(temp,"2");
					}
				}
			}
		}
	}
	
	/**
	 * 取得OverLogging資訊
	 * @return
	 */
	public List<CSMessage> getOverLoggingList() {
		return overLoggingList;
	}
}
