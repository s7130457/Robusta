package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * cast exception in catch clause to RuntimeException and remove "System.out.println" or "e.printStackTrace" statement from catch clause
 * @author Charles
 *
 */
public class RefineRuntimeExceptionQuickFix implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(RefineRuntimeExceptionQuickFix.class);

	private QuickFixCore quickFixCore;
	
	private String label;

	private String problem;

	private String srcPos;
	
	private SmellSettings smellSettings;
	
	public RefineRuntimeExceptionQuickFix(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		String methodIdx="";
		
		try {
			methodIdx = collectMarkerInfo(marker);
		} catch (CoreException e) {
			throw new RuntimeException("Failed to resolve marker attribute.", e);
		}
		
		quickFixCore.setJavaFileModifiable(marker.getResource());
		CompilationUnit compilationUnit = quickFixCore.getCompilationUnit(); 
		MethodDeclaration methodDeclaration = QuickFixUtils.getMethodDeclaration(compilationUnit, Integer.parseInt(methodIdx));
		if(methodDeclaration == null) {
			return;
		}
		
		CatchClauseFinderVisitor catchClauseFinder = new CatchClauseFinderVisitor(Integer.parseInt(srcPos));
		compilationUnit.accept(catchClauseFinder);
		CatchClause exactlyCatchClause = catchClauseFinder.getFoundCatchClause();
		if(exactlyCatchClause == null) {
			return;
		}

		if(smellSettings.isAddingRobustnessAnnotation()) {
			quickFixCore.generateRobustnessLevelAnnotation(methodDeclaration, 1, RuntimeException.class);
			
			quickFixCore.generateThrowExceptionOnMethodSignature(methodDeclaration, RuntimeException.class);
		}
		quickFixCore.removeNodeInCatchClause(exactlyCatchClause, ".printStackTrace()", "System.out.print", "System.err.print");
		quickFixCore.addThrowRefinedExceptionInCatchClause(exactlyCatchClause, RuntimeException.class);

		quickFixCore.applyChange();
	}
	
	private String collectMarkerInfo(IMarker marker) throws CoreException {
		String methodIdx = "";
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem == null) {
				return methodIdx;
			}
			
			if(!(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) && 
			   !(problem.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK))) {
				return methodIdx;
			}
			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			//store the line number of QuickFix invocation
			srcPos = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();
			return methodIdx;
		} catch (CoreException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
}
