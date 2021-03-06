package ntut.csie.csdet.refactor.ui;


import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * this is a  wizard to generate a user page, providing refactoring feature.
 * @author chewei
 */

public class RethrowExWizard extends RefactoringWizard {

	public RethrowExWizard(Refactoring refactoring, int flags) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("Rethrow Unchecked Exception");
		
	}

	@Override
	protected void addUserInputPages() {
		//add rethrow exception page
		addPage(new RethrowExInputPage("Rethrow Unchecked Exception"));
		
		
	}


}
