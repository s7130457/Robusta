package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.visitor.SpareHandlerAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

import agile.exception.RL;
import agile.exception.Robustness;

public class RetryRefactoring extends Refactoring{

	//�ϥΪ̩ҿ�ܪ�Exception Type
	private IType exType;
	//retry���ܼƦW��
	private String retry;
	//attemp���ܼƦW��
	private String attempt;
	//�̤jretry����	
	private String maxNum;
	//�̤jretry���ƪ��ܼƦW��
	private String maxAttempt;		
	// user �Ҷ�g�n��X��Exception,�w�]�ORunTimeException
	private String exceptionType;
	
	private IJavaProject project;
	
	//�s��n�ഫ��ICompilationUnit������
	private IJavaElement element;
	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;
	//�s��Q�ؿ諸����
	private ITextSelection iTSelection;
	
	private TextFileChange textFileChange;
	
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	public RetryRefactoring(IJavaProject project,IJavaElement element,ITextSelection sele){
		this.project = project;
		this.element = element;
		this.iTSelection = sele;
	}	
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {	
		//����check final condition
		RefactoringStatus status = new RefactoringStatus();		
		collectChange(status);
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();		
		if(iTSelection.getOffset() < 0 || iTSelection.getLength() == 0){
			status.addFatalError("Selection Error, please retry again!!!");
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		Change[] changes = new Change[] {textFileChange};
		CompositeChange change = new CompositeChange("Introduce resourceful try clause", changes);
		return change;
	}

	private void collectChange(RefactoringStatus status){
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
				
		parser.setSource((ICompilationUnit) element);
		parser.setResolveBindings(true);
		this.actRoot = (CompilationUnit) parser.createAST(null);
				
		//���o��class�Ҧ���method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		//�Q��ITextSelection��T�Ө��o�ϥΪ̩ҿ�ܭn�ܧ�AST Node
		//�n�ϥγo��method�ݦbxml��import org.eclipse.jdt.astview
		ASTNode selectNode = NodeFinder.perform(actRoot, iTSelection.getOffset(), iTSelection.getLength());
		if(selectNode == null){
			status.addFatalError("Selection Error, please retry again!!!");
		}else if(!(selectNode instanceof TryStatement)){
			status.addFatalError("Selection Error, please retry again!!!");
		}else{			
			List<ASTNode> methodList = methodCollector.getMethodList();
			int methodIdx = -1;
			for(ASTNode method : methodList){
				methodIdx++;
				SpareHandlerAnalyzer visitor = new SpareHandlerAnalyzer(selectNode);
				method.accept(visitor);
				if(visitor.getResult()){
					break;
				}
			}
			
			//���o�ثe�n�Q�ק諸method node
			if(methodIdx != -1){
				//���o�o��method��RL��T
				currentMethodNode = methodList.get(methodIdx);
				ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
				currentMethodNode.accept(exVisitor);
				currentMethodRLList = exVisitor.getMethodRLAnnotationList();
				introduceRetry(selectNode);
			}
		}

	}
	
	/**
	 * �iretry��Refactoring
	 */
	private void introduceRetry(ASTNode selectNode){
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			MethodDeclaration md = (MethodDeclaration)currentMethodNode;
			//���oCode smell����T

			List methodSt = md.getBody().statements();
			//���ƻs�@��method����statement�O�d�U��
			int pos = -1;
			TryStatement original = null;
			for(int i=0;i<methodSt.size();i++){
				if(methodSt.get(i) instanceof TryStatement){
					TryStatement temp = (TryStatement)methodSt.get(i);
					if(temp.getStartPosition() == selectNode.getStartPosition()){
						pos = i;
						original = (TryStatement)methodSt.get(i);
						break;
					}
				}
			}
			
			Block newBlock = ast.newBlock();
			List newStat = newBlock.statements();
			//���]try���e���{������,��L�[�i�s��statement��
			if(pos > 0){
				for(int i=0;i<pos;i++){
					newStat.add(ASTNode.copySubtree(ast, (ASTNode) methodSt.get(i)));
				}
			}
			
			//�s�W�ܼ�
			addNewVariable(ast,newStat);
			//�s�Wdo-while
			DoStatement doWhile = addDoWhile(ast,newStat);
			//�bdo-while�s�Wtry
			TryStatement ts =addTryBlock(ast,doWhile,original);
			//�btry�̭��s�Wcatch
			addCatchBlock(ast, original, ts);
			
			for(int i=pos+1;i<methodSt.size();i++){
				newStat.add(ASTNode.copySubtree(ast, (ASTNode) methodSt.get(i)));
			}
			//�M���쥻�����e
			methodSt.clear();
			//�[�Jrefactoring�᪺���G
			md.setBody(newBlock);
			//�إ�RL Annotation
			addAnnotationRoot(ast);

			//�g�^Edit��
			applyChange();

	}
	
	/**
	 * �إ߭��զ��ƪ������ܼ�
	 * @param ast
	 * @param newStat
	 */
	private void addNewVariable(AST ast,List newStat){
		//�إ�attempt�ܼ�
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(this.attempt));
		VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
		vdf.setInitializer(ast.newNumberLiteral("0"));		
		newStat.add(vds);
		
		//�إ�Max Attempt�ܼ�
		VariableDeclarationFragment maxAttempt = ast.newVariableDeclarationFragment();
		maxAttempt.setName(ast.newSimpleName(this.maxAttempt));
		VariableDeclarationStatement number = ast.newVariableDeclarationStatement(maxAttempt);
		maxAttempt.setInitializer(ast.newNumberLiteral(maxNum));
		newStat.add(number);
		
		//�إ�retry�ܼ�
		VariableDeclarationFragment retry = ast.newVariableDeclarationFragment();
		retry.setName(ast.newSimpleName(this.retry));
		VariableDeclarationStatement retryValue = ast.newVariableDeclarationStatement(retry);
		//�إ�boolean���A
		retryValue.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
//		retryValue.setType(ast.newSimpleType(ast.newSimpleName()));
		retry.setInitializer(ast.newBooleanLiteral(false));
		newStat.add(retryValue);
	}
	
	/**
	 * �إ�do-while
	 * @param ast
	 * @param newStat
	 * @return
	 */
	private DoStatement addDoWhile(AST ast,List newStat){
		DoStatement doWhile = ast.newDoStatement();
		//���إ�attempt <= maxAttempt
		InfixExpression sife = ast.newInfixExpression();
		sife.setLeftOperand(ast.newSimpleName(this.attempt));
		sife.setRightOperand(ast.newSimpleName(this.maxAttempt));
		sife.setOperator(InfixExpression.Operator.LESS_EQUALS);
		InfixExpression bigIfe = ast.newInfixExpression();
		bigIfe.setLeftOperand(sife);
		//�إ�(retry)
		bigIfe.setRightOperand(ast.newSimpleName(this.retry));
		//�إ�(attempt<=maxtAttempt && retry)
		bigIfe.setOperator(InfixExpression.Operator.AND);
		doWhile.setExpression(bigIfe);
		newStat.add(doWhile);
		return doWhile;
	}
	
	/**
	 * �bdo-while���إ�try 
	 * @param ast
	 * @param doWhile
	 * @param original
	 * @return
	 */
	private TryStatement addTryBlock(AST ast,DoStatement doWhile,TryStatement original){
		TryStatement ts = ast.newTryStatement();
		Block block = ts.getBody();
		List tryStatement = block.statements();
		List originalCatch = original.catchClauses();
		
		//�إ�retry = false
		Assignment as = ast.newAssignment();
		as.setLeftHandSide(ast.newSimpleName(this.retry));
		as.setOperator(Assignment.Operator.ASSIGN);
		as.setRightHandSide(ast.newBooleanLiteral(false));
		ExpressionStatement es =ast.newExpressionStatement(as);
		tryStatement.add(es);
		
		//�إ�If statement
		IfStatement ifStat = ast.newIfStatement();
		InfixExpression ife = ast.newInfixExpression();
		
		//�إ�if(....)
		ife.setLeftOperand(ast.newSimpleName(this.attempt));
		ife.setOperator(InfixExpression.Operator.EQUALS);
		ife.setRightOperand(ast.newNumberLiteral("0"));
		ifStat.setExpression(ife);

		//�إ�then statement
		Block thenBlock = ast.newBlock();
		List thenStat = thenBlock.statements();
		List originalStat = original.getBody().statements();
		ifStat.setThenStatement(thenBlock);
		for(int i=0;i<originalStat.size();i++){
			//�N�쥻try�����e�ƻs�i��
			thenStat.add(ASTNode.copySubtree(ast, (ASTNode) originalStat.get(i)));
		}
		
		//�إ�else statement
		Block elseBlock = ast.newBlock();
		List elseStat = elseBlock.statements();
		ifStat.setElseStatement(elseBlock);
		
		//��X�ĤG�htry����m
		TryStatement secTs = null;
		for(int i=0;i<originalCatch.size();i++){
			CatchClause temp = (CatchClause)originalCatch.get(i);
			List tempSt = temp.getBody().statements();
			for(int x=0;x<tempSt.size();x++){
				if(tempSt.get(x) instanceof TryStatement){
					secTs = (TryStatement)tempSt.get(x);
					break;
				}
			}
		}
		
		//�}�l�ƻsseconde try statement�����e��else statement
		List secStat = secTs.getBody().statements();
		for(int i=0;i<secStat.size();i++){
			elseStat.add(ASTNode.copySubtree(ast, (ASTNode) secStat.get(i)));
		}

		//�Nif statement�[�itry����
		tryStatement.add(ifStat);
		//��do while�s�ؤ@��Block,�ñNTry�[�i�h
		Block doBlock = doWhile.getAST().newBlock();
		doWhile.setBody(doBlock);
		List doStat = doBlock.statements();
		doStat.add(ts);		
		return ts;
	}
	
	/**
	 * �btry���إ�catch block
	 * @param ast
	 * @param original
	 * @param ts
	 */
	private void addCatchBlock(AST ast,TryStatement original,TryStatement ts){
		Block block = ts.getBody();
		List catchStatement = ts.catchClauses();
		List originalCatch = original.catchClauses();

		//�إ߷s��catch
		for(int i=0;i<originalCatch.size();i++){
			CatchClause temp = (CatchClause)originalCatch.get(i);
			SingleVariableDeclaration sv = (SingleVariableDeclaration) ASTNode.copySubtree(ast, temp.getException());
			CatchClause cc = ast.newCatchClause();		
			cc.setException(sv);
			catchStatement.add(cc);
		}
		
		//�H�U�}�l�إ�catch Statment�������e
	
		for(int x=0;x<catchStatement.size();x++){
			//���إ�attempt++
			PostfixExpression pfe = ast.newPostfixExpression();
			pfe.setOperand(ast.newSimpleName(this.attempt));
			pfe.setOperator(PostfixExpression.Operator.INCREMENT);
			ExpressionStatement epfe= ast.newExpressionStatement(pfe);
				
			//�إ�retry = true
			Assignment as = ast.newAssignment();
			as.setLeftHandSide(ast.newSimpleName(this.retry));
			as.setOperator(Assignment.Operator.ASSIGN);
			as.setRightHandSide(ast.newBooleanLiteral(true));
			ExpressionStatement es =ast.newExpressionStatement(as);
				
			//�إ�if statement,��throw exception
			IfStatement ifStat = ast.newIfStatement();
			InfixExpression ife = ast.newInfixExpression();
			
			//�إ�if(....)
			ife.setLeftOperand(ast.newSimpleName(this.attempt));
			ife.setOperator(InfixExpression.Operator.GREATER);
			ife.setRightOperand(ast.newSimpleName(this.maxAttempt));
			ifStat.setExpression(ife);
				
			CatchClause cc =(CatchClause)catchStatement.get(x);
			CatchClause temp = (CatchClause)originalCatch.get(x);
	
			//�إ�then statement
			Block thenBlock = ast.newBlock();
			List thenStat = thenBlock.statements();
			ifStat.setThenStatement(thenBlock);
			//�ۦ�إߤ@��throw statement�[�J
			ThrowStatement tst = ast.newThrowStatement();
			//�Nthrow��variable�ǤJ
			ClassInstanceCreation cic = ast.newClassInstanceCreation();
			//throw new RuntimeException()<--�w�]��
			cic.setType(ast.newSimpleType(ast.newSimpleName(this.exceptionType)));
				
			SingleVariableDeclaration svd = (SingleVariableDeclaration) temp
			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			//�Nthrow new RuntimeException(ex)�A�����[�J�Ѽ� 
			cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
			tst.setExpression(cic);
			thenStat.add(tst);
			
			//�N�ѤU��statement�[�i�hcatch����
			List ccStat = cc.getBody().statements();
			ccStat.add(epfe);
			ccStat.add(es);
			ccStat.add(ifStat);

		}
		
		//�[�J��import��Library(�J��RuntimeException�N���Υ[Library)
		if(!exceptionType.equals("RuntimeException")){
			addImportDeclaration();
			//���pmethod�e���S��throw�F�誺��,�N�[�W�h
			MethodDeclaration md = (MethodDeclaration)currentMethodNode;
			List thStat = md.thrownExceptions();
			boolean isExist = false;
			for(int i=0;i<thStat.size();i++){
				if(thStat.get(i) instanceof SimpleName){
					SimpleName sn = (SimpleName)thStat.get(i);
					if(sn.getIdentifier().equals(exceptionType)){
						isExist = true;
						break;
					}
				}
			}
			if(!isExist)
				thStat.add(ast.newSimpleName(this.exceptionType));
		}
	}
	
	
	/**
	 * �P�_�O�_�����[�J��Library,��throw RuntimeException�����p�n�ư�
	 * �]��throw RuntimeException����import Library
	 */
	private void addImportDeclaration(){
		//�P�_�O�_��import library
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = this.actRoot.imports();
		for(ImportDeclaration id : importList){
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())){
				isImportLibrary = true;
			}
		}
		
		//���p�S��import�N�[�J��AST��
		AST rootAst = this.actRoot.getAST(); 
		if(!isImportLibrary){
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(exType.getFullyQualifiedName()));
			this.actRoot.imports().add(imp);
		}		
	}
	
	
	@SuppressWarnings("unchecked")
	private void addAnnotationRoot(AST ast){
		//�n�إ�@Robustness(value={@RL(level=3, exception=java.lang.RuntimeException.class)})�o�˪�Annotation
		//�إ�Annotation root
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		MethodDeclaration method = (MethodDeclaration) currentMethodNode;		
		if(currentMethodRLList.size() == 0){		
			//Retry��RL = 3
			rlary.expressions().add(getRLAnnotation(ast,3,exceptionType));
		}else{	
			//���p���ӴN��annotation�����ª��[�i�h
			for (RLMessage rlmsg : currentMethodRLList) {
				rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));
			}
			//�ª��[������[�s��RL = 3 annotation�i��
			rlary.expressions().add(getRLAnnotation(ast,3,exceptionType));
			
			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//����¦���annotation��N������
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		//�NRL��library�[�i��
		addImportRLDeclaration();
	}
	
	/**
	 * ����RL Annotation��RL��� 
	 * @param ast :AST Object
	 * @param levelVal :�j���׵���
	 * @param exClass : �ҥ~���O
	 * @return NormalAnnotation AST Node
	 */

	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//�n�إ�@Robustness(value={@RL(level=1, exception=java.lang.RuntimeException.class)})�o�˪�Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RL"));

		// level = 3
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		//Retry �w�]level = 3
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		//exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		//�w�]��RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}
	
	@SuppressWarnings("unchecked")
	private void addImportRLDeclaration() {
		// �P�_�O�_�w�gImport Robustness��RL���ŧi
		List<ImportDeclaration> importList = this.actRoot.imports();
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;
		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName())) {
				isImportRobustnessClass = true;
			}
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName())) {
				isImportRLClass = true;
			}
		}

		AST rootAst = this.actRoot.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			this.actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RL.class.getName()));
			this.actRoot.imports().add(imp);
		}
	}
	
	/**
	 * �N�n�ܧ󪺸�Ƽg�^��Document��
	 */
	private void applyChange(){
		//�g�^Edit��
		try {
			ICompilationUnit cu = (ICompilationUnit) element;
			Document document = new Document(cu.getBuffer().getContents());	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public String getName() {	
		return "Introduce resourceful try clause";
	}
	
	/**
	 * ���oJavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}

	/**
	 * �x�s�nThrow��Exception��m(�nimport�ϥ�)
	 * @param type
	 */
	public void setExType(IType type){		
		this.exType = type;
	}
	
	/**
	 * set attempt�ܼƦW��
	 * @param attempt
	 */
	public RefactoringStatus setAttemptVariable(String attempt){		
		if(attempt.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.attempt = attempt;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set maxAttempt�ܼƦW��
	 * @param attempt
	 */
	public RefactoringStatus setMaxAttemptVariable(String attempt){
		if(attempt.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.maxAttempt = attempt;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set �̤j���զ���
	 * @param num
	 */
	public RefactoringStatus setMaxAttemptNum(String num){
		if(num.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.maxNum = num;
			return new RefactoringStatus();
		}		
	}
	
	/**
	 * set Retry�ܼƦW��
	 * @param attempt
	 */
	public RefactoringStatus setRetryVariable(String retry){
		if(retry.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.retry = retry;
			return new RefactoringStatus();
		}	
	}
	
	/**
	 * ����user�ҭnthrow��exception type
	 * @param name : exception type
	 */
	public RefactoringStatus setExceptionName(String name){
		//���p�ϥΪ̨S����g����F��,��RefactoringStatus�]��Error
		if(name.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			//���p���g�N��L�s�U��
			this.exceptionType = name;
			return new RefactoringStatus();
		}		
	}
}