package ntut.csie.csdet.refactor;

import ntut.csie.csdet.refactor.ui.RethrowExWizard;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.builder.RLOrderFix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在Marker上面的Quick Fix中加入Refactoring(Rethrow Unhandled Exceptionb)的功能
 * @author chewei
 */

public class RethrowUncheckExAction implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(RethrowUncheckExAction.class);
	private String label;
	
	public RethrowUncheckExAction(String label){
		this.label = label;
	}	
	
	@Override
	public String getLabel() {	
		return label;
	}

	@Override
	public void run(IMarker marker) {
		//使用者點選ignore ex 或者dummy handler的marker時,會去找尋對應的Refactor方法
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if ((problem != null && problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) ||
					(problem != null && problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER))){
				//建立操作Refactor的物件,並將marker傳進去以利之後取得code smell相關資訊
				RethrowExRefactoring refactoring = new RethrowExRefactoring();				
				refactoring.setMarker(marker);
				//啟動Refactor dialog
				RefactoringWizardOpenOperation operation = 
					new RefactoringWizardOpenOperation(new RethrowExWizard(refactoring,0));
				operation.run(new Shell(), "Rethrow Unhnadle Exception");
				
//				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
//				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
//				RLOrderFix orderFix = new RLOrderFix();
//				//調整RL Annotation順序，順便反白指定行數(-1表示反白RL Annotation)
//				orderFix.run(marker.getResource(), methodIdx, msgIdx, -1);
			}
		
		} catch (InterruptedException e) {
			logger.error("[Refactor][Rethrow Exception] EXCEPTION ",e);
		} catch (CoreException e) {
			logger.error("[Refactor][Rethrow Exception] EXCEPTION ",e);
		}
	}
}
