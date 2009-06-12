package ntut.csie.rleht.builder;

import java.util.List;

import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLOrderFix {
	private static Logger logger = LoggerFactory.getLogger(RLOrderFix.class);

	private CompilationUnit actRoot;
	private IOpenable actOpenable;
	//目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;
	//目前的Method AST Node
	private ASTNode currentMethodNode = null;
	//反白的行數
	private int selectLine = -1;

	/**
	 * 進行調整RL Annotation順序
	 * 
	 * @param resource		
	 * @param methodIdx		
	 * @param msgIdx		
	 * @param selectLine	欲反白的行數
	 */
	public void run(IResource resource, String methodIdx, String msgIdx, int selectLine)
	{
		this.selectLine = selectLine;
		
		//取得currentMethodRLList
		findMethod(resource, Integer.parseInt(methodIdx));

		//判斷currentMethodRLList是否需要重置
		boolean isError = checkCurrentMethodRLList();

		//若需要重置，則進行重置作業
		if (isError)
		{			
			//把currentMethodRLList存成Array + 1(加1為排序時可調整的空間)
			RLMessage[] newRLList = new RLMessage[currentMethodRLList.size()+1];
			//currentMethodRLList放入Array中
			for (int i=0;i < currentMethodRLList.size();i++)
				newRLList[i] = currentMethodRLList.get(i);
			//在新增調整空間放入資料，使它不為Null，才不會出錯
			newRLList[currentMethodRLList.size()] = newRLList[0];
			
			//把RL List重新排序，若位置正確把結果存到currentMethodRLList中
			permutation(newRLList,1);
		}

		//更新全部的 RL Annotation List
		updateRLAnnotation(Integer.parseInt(msgIdx));
	}
	
	/**
	 * 取得currentMethodRLList
	 * 
	 * @param resource
	 * @param methodIdx
	 * @return
	 */
	private boolean findMethod(IResource resource, int methodIdx) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {

			try {
				IJavaElement javaElement = JavaCore.create(resource);

				if (javaElement instanceof IOpenable) {
					this.actOpenable = (IOpenable) javaElement;
				}

				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);

				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.actRoot = (CompilationUnit) parser.createAST(null);
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();

				ASTNode method = methodList.get(methodIdx);
				if (method != null) {
					ExceptionAnalyzer visitor = new ExceptionAnalyzer(this.actRoot, method.getStartPosition(), 0);
					method.accept(visitor);
					currentMethodNode = visitor.getCurrentMethodNode();
					currentMethodRLList = visitor.getMethodRLAnnotationList();
					System.out.println("[Size]===>"+currentMethodRLList.size());
				}
			}
			catch (Exception ex) {
				logger.error("[findMethod] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 * 判斷currentMethodRLList是否需要重置
	 * 
	 * @return	是否需要重置
	 */
	private boolean checkCurrentMethodRLList() {
		boolean isError = false;
		int idx = -1;
		for (RLMessage msg : currentMethodRLList) {
			System.out.println(msg.getTypeBinding().getQualifiedName() + "    XXX");
			idx++;
			// 檢查@RL清單內的exception類別階層是否正確
			int idx2 = 0;
			for (RLMessage msg2 : currentMethodRLList) {
				if (idx >= idx2++) {
					continue;
				}
				if (isParentFrontSon(msg2.getTypeBinding(), msg.getTypeBinding().getQualifiedName()))
					isError = true;
			}
		}
		return isError;
	}
	
	/**
	 * 把RL List排列組合排序，每排列一次判斷順序是否正確，若正確把正確結果存到currentMethodRLList中
	 * 
	 * @param newRLList
	 * 				新排序的RL Annotation List	
	 * @param i
	 * 				排列群組位置
	 */
	private void permutation(RLMessage[] newRLList, int i) {
		//排列中
		if(i < newRLList.length - 1) {
    		for(int j = i; j <= newRLList.length - 1; j++) {
    			RLMessage tmp = newRLList[j];
                //旋轉該區段最右邊數字至最左邊
                for(int k = j; k > i; k--)
                	newRLList[k] = newRLList[k-1];
                newRLList[i] = tmp;
                permutation(newRLList, i+1);
                //還原
                for(int k = i; k < j; k++)
                	newRLList[k] = newRLList[k+1];
                newRLList[j] = tmp;
        	}
   		//排列完成
    	} else {
        	//若RL Annotation List順序全部正確，把它記錄到currentMethodRLList裡
        	if (!isRLListCorrect(newRLList)) {
        		currentMethodRLList.clear();
        		for(int j = 1; j <= newRLList.length - 1; j++)
        			currentMethodRLList.add(newRLList[j]);
        	}
        }
    }
	
	/**
	 * 判斷所有的RL List位置是否正確
	 * 
	 * @param newRLList
	 * 		新位置的RL Annotation List
	 * @return
	 * 		RL List順序是否全部正確
	 */
	private boolean isRLListCorrect(RLMessage[] newRLList)
	{
		int msg1 = 0;
		for (int i = 1; i < newRLList.length; i++) {
			msg1++;
			int k = 1;
			for (int j=1; j < newRLList.length; j++) {
				if (msg1 >= k++)
					continue;

				//判斷父類別是否在子類別前
				boolean isErr = isParentFrontSon(newRLList[j].getTypeBinding(),newRLList[i].getTypeBinding().getQualifiedName());
				if (isErr)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * 判斷父類別是否在子類別前
	 * 
	 * @param typeBinding
	 * 			父類別
	 * @param typeName
	 * 			子類別名稱
	 * @return
	 * 			父類別是否在子類別前
	 */
    private boolean isParentFrontSon(ITypeBinding typeBinding,String typeName)
	{
		if (typeBinding == null || typeName == null)
			return false;

		String qname = typeBinding.getQualifiedName();
		if (qname.equals(typeName))
			return true;

		//判斷父類別
		ITypeBinding superClass = typeBinding.getSuperclass();
		if (superClass != null) {
			if (superClass.getQualifiedName().equals(typeName))
				return true;
		}

		//判斷介面
		ITypeBinding[] interfaceType = typeBinding.getInterfaces();
		if (interfaceType != null && interfaceType.length > 0) {
			for (int i = 0, size = interfaceType.length; i < size; i++) {
				if (interfaceType[i].getQualifiedName().equals(typeName))
					return true;
			}
		}
		return false;
	}
    
	/**
	 * 更新RL Annotation
	 * 
	 * @param isAllUpdate
	 * 		是否更新全部的Annotation
	 * @param pos
	 * @param level
	 */
	private void updateRLAnnotation(int pos) {
		try {

			actRoot.recordModifications();

			AST ast = currentMethodNode.getAST();

			NormalAnnotation root = ast.newNormalAnnotation();
			root.setTypeName(ast.newSimpleName("Robustness"));

			MemberValuePair value = ast.newMemberValuePair();
			value.setName(ast.newSimpleName("value"));

			root.values().add(value);

			ArrayInitializer rlary = ast.newArrayInitializer();
			value.setValue(rlary);

			//若全部更新，表示為currentMethodRLList排序，所以加入Annotation Library宣告
			
			int msgIdx = 0;
			for (RLMessage rlmsg : currentMethodRLList) {
				rlary.expressions().add(
					getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
			}

			MethodDeclaration method = (MethodDeclaration) currentMethodNode;

			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//找到舊有的annotation後將它移除
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}

			if (rlary.expressions().size() > 0) {
				//將新建立的annotation root加進去
				method.modifiers().add(0, root);
			}
			
			//將要變更的資料寫回至Document中，並反白
			applyChange();
		}
		catch (Exception ex) {
			logger.error("[updateRLAnnotation] EXCEPTION ",ex);
		}
	}
	
	/**
	 * 產生RL Annotation之RL資料
	 * @param ast
	 *            AST Object
	 * @param levelVal
	 *            強健度等級
	 * @param exClass
	 *            例外類別
	 * @return NormalAnnotation AST Node
	 */
	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal, String exClass) {
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RL"));

		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));

		rl.values().add(level);

		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		exclass.setType(ast.newSimpleType(ast.newName(exClass)));
		exception.setValue(exclass);

		rl.values().add(exception);
		return rl;
	}
	
	/**
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange()
	{
		//寫回Edit中
		try{
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);
			cu.getBuffer().setContents(document.get());

			//反白指定行數
			selectLine(document);
		}
		catch(Exception ex){
			logger.error("[RLQuickFix] EXCEPTION ",ex);
		}
	}
	
	/**
	 * 反白指定行數
	 * 
	 * @param document
	 */
	private void selectLine(Document document) {
		//取得目前的EditPart
		IEditorPart editorPart = EditorUtils.getActiveEditor();
		ITextEditor editor = (ITextEditor) editorPart;

		//若反白行數為
		if (selectLine == -1) {
			//取得Method的起點位置
			int srcPos = currentMethodNode.getStartPosition();

			//用Method起點位置取得Method位於第幾行數(起始行數從0開始，不是1，所以減1)
			selectLine = this.actRoot.getLineNumber(srcPos)-1;
		}

		//欲反白的行數資料
		IRegion lineInfo = null;
		try {
			//取得行數的資料
			lineInfo = document.getLineInformation(selectLine);
		} catch (BadLocationException e) {
			logger.error("[BadLocation] EXCEPTION ",e);
		}

		//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
		editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
	}
}
