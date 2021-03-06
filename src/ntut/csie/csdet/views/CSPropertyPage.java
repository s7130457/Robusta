package ntut.csie.csdet.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.preference.SmellSettings;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class CSPropertyPage extends org.eclipse.ui.dialogs.PropertyPage {

	// save each tab in page
	private ArrayList<APropertyPage> tabPages;
	private SmellSettings smellSettings;
	private RobustaSettings robustaSettings;
	private ResourceBundle resource = ResourceBundle.getBundle("robusta",
			new Locale("en", "US"));

	public CSPropertyPage() {
		super();
		IProject project = null;
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		IStructuredSelection selection = null;
		if (window != null) {
			selection = (IStructuredSelection) window.getSelectionService().getSelection();
		}
		for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it
				.hasNext();) {
			Object element = it.next();
			if (element instanceof IProject) {
				project = (IProject) element;
			} else if (element instanceof IAdaptable) {
				project = (IProject) ((IAdaptable) element)
						.getAdapter(IProject.class);
			}
		}
		robustaSettings = new RobustaSettings(
				UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(project), project);
		
		//select all bad smell as default detection rule for user who are without the rule configuration
		smellSettings = new SmellSettings(
				UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.activateAllConditionsIfNotConfugured(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		tabPages = new ArrayList<APropertyPage>();
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);

		final TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		addPage(tabFolder);

		return composite;
	}

	private void addPage(TabFolder tabFolder) {
		// add Main Page
		final TabItem mainTabItem = new TabItem(tabFolder, SWT.NONE);
		mainTabItem.setText(resource.getString("setting.page"));
		final Composite mainComposite = new Composite(tabFolder, SWT.NONE);
		APropertyPage mainPage = new SettingPage(mainComposite, this,
				smellSettings);
		mainTabItem.setControl(mainComposite);
		tabPages.add(mainPage);

		// add Dummy Handler Page
		final TabItem dummyHandlerTabItem = new TabItem(tabFolder, SWT.NONE);
		dummyHandlerTabItem.setText(resource.getString("dummy.handler"));
		final Composite dummyHandlerComposite = new Composite(tabFolder,
				SWT.NONE);
		APropertyPage dummyHandlerPage = new DummyHandlerPage(
				dummyHandlerComposite, this, smellSettings);
		dummyHandlerTabItem.setControl(dummyHandlerComposite);
		tabPages.add(dummyHandlerPage);

		// add CarelessCleanup Page
		final TabItem carelessCleanupTabItem = new TabItem(tabFolder, SWT.NONE);
		carelessCleanupTabItem.setText(resource.getString("careless.cleanup"));
		final Composite carelessCleanupPage = new Composite(tabFolder, SWT.NONE);
		APropertyPage cleanupPage = new CarelessCleanupPage(
				carelessCleanupPage, this, smellSettings);
		carelessCleanupTabItem.setControl(carelessCleanupPage);
		tabPages.add(cleanupPage);

		// add SelectedFolder Page
		final TabItem newTabItem = new TabItem(tabFolder, SWT.NONE);
		newTabItem.setText(resource
				.getString("selectedFolderPage.robustaSettings"));
		final Composite newComposite = new Composite(tabFolder, SWT.NONE);
		APropertyPage newPage = new SelectedFolderPage(newComposite, this,
				robustaSettings);
		newTabItem.setControl(newComposite);
		tabPages.add(newPage);
	}

	/**
	 * save configuration in each tab when Ok button is pressed 
	 */
	public boolean performOk() {
		for (int i = 0; i < tabPages.size(); i++) {
			tabPages.get(i).storeSettings();
		}
		return true;
	}
}
