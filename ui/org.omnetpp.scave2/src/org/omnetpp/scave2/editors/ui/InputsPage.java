package org.omnetpp.scave2.editors.ui;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.ui.CustomSashForm;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engineext.IResultFilesChangeListener;
import org.omnetpp.scave.engineext.ResultFileManagerEx;
import org.omnetpp.scave.model.Analysis;
import org.omnetpp.scave2.actions.AddResultFileAction;
import org.omnetpp.scave2.actions.AddWildcardResultFileAction;
import org.omnetpp.scave2.actions.EditAction;
import org.omnetpp.scave2.actions.RemoveAction;
import org.omnetpp.scave2.actions.SetFilterAction;
import org.omnetpp.scave2.editors.ScaveEditor;
import org.omnetpp.scave2.editors.treeproviders.InputsFileRunViewContentProvider;
import org.omnetpp.scave2.editors.treeproviders.InputsLogicalViewContentProvider;
import org.omnetpp.scave2.editors.treeproviders.InputsLogicalViewLabelProvider;
import org.omnetpp.scave2.editors.treeproviders.InputsPhysicalViewLabelProvider;
import org.omnetpp.scave2.editors.treeproviders.InputsRunFileViewContentProvider;

public class InputsPage extends ScaveEditorPage {

	private FormToolkit formToolkit = null;   //  @jve:decl-index=0:visual-constraint=""
	private Section inputFilesSection = null;
	private Section dataSection = null;
	private SashForm sashform = null;

	public InputsPage(Composite parent, ScaveEditor scaveEditor) {
		super(parent, SWT.V_SCROLL, scaveEditor);
		initialize();
	}
	
	public TreeViewer getInputFilesTreeViewer() {
		InputFilesPanel panel = (InputFilesPanel)inputFilesSection.getClient();
		return panel.getTreeViewer();
	}
	
	public TreeViewer getFileRunTreeViewer() {
		DataPanel panel = (DataPanel)dataSection.getClient();
		return panel.getFileRunTreeViewer();
	}
	
	public TreeViewer getRunFileTreeViewer() {
		DataPanel panel = (DataPanel)dataSection.getClient();
		return panel.getRunFileTreeViewer();
	}
	
	public TreeViewer getLogicalDataTreeViewer() {
		DataPanel panel = (DataPanel)dataSection.getClient();
		return panel.getLogicalTreeViewer();
	}
	
	private void initialize() {
		// set up UI
		setPageTitle("Inputs"); 
		setFormTitle("Inputs");
		setExpandHorizontal(true);
		setExpandVertical(true);
		setBackground(ColorFactory.asColor("white"));
		getBody().setLayout(new GridLayout());
		createSashForm();
		createInputFilesSection();
		createDataSection();

		// configure viewers
        scaveEditor.configureTreeViewer(getInputFilesTreeViewer());
        
        getFileRunTreeViewer().setContentProvider(new InputsFileRunViewContentProvider());
        getFileRunTreeViewer().setLabelProvider(new InputsPhysicalViewLabelProvider());

        getRunFileTreeViewer().setContentProvider(new InputsRunFileViewContentProvider());
        getRunFileTreeViewer().setLabelProvider(new InputsPhysicalViewLabelProvider());
        
        getLogicalDataTreeViewer().setContentProvider(new InputsLogicalViewContentProvider());
        getLogicalDataTreeViewer().setLabelProvider(new InputsLogicalViewLabelProvider());

        scaveEditor.getResultFileManager().addListener(new IResultFilesChangeListener() {
			public void resultFileManagerChanged(ResultFileManager manager) {
				getFileRunTreeViewer().setInput(manager); // force refresh
				getRunFileTreeViewer().setInput(manager);
				getLogicalDataTreeViewer().setInput(manager);
			}
        });
        
        getFileRunTreeViewer().addSelectionChangedListener(scaveEditor.getSelectionChangedListener());
        getRunFileTreeViewer().addSelectionChangedListener(scaveEditor.getSelectionChangedListener());
        getLogicalDataTreeViewer().addSelectionChangedListener(scaveEditor.getSelectionChangedListener());
        
        // set up drag & drop of .sca and .vec files into the viewers
        setupResultFileDropTarget(getInputFilesTreeViewer().getControl());
        setupResultFileDropTarget(getFileRunTreeViewer().getControl());
        setupResultFileDropTarget(getRunFileTreeViewer().getControl());
        setupResultFileDropTarget(getLogicalDataTreeViewer().getControl());

        // set contents
		Analysis analysis = scaveEditor.getAnalysis();
        getInputFilesTreeViewer().setInput(analysis.getInputs());

        ResultFileManagerEx manager = scaveEditor.getResultFileManager();
        getFileRunTreeViewer().setInput(manager);
        getRunFileTreeViewer().setInput(manager);
        getLogicalDataTreeViewer().setInput(manager);
	}

	/**
	 * This method initializes formToolkit	
	 * 	
	 * @return org.eclipse.ui.forms.widgets.FormToolkit	
	 */
	private FormToolkit getFormToolkit() {
		if (formToolkit == null) {
			formToolkit = new FormToolkit(Display.getCurrent());
		}
		return formToolkit;
	}

	private void createSashForm() {
		sashform = new CustomSashForm(getBody(), SWT.VERTICAL | SWT.SMOOTH);
		sashform.setBackground(this.getBackground());
		sashform.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL |
											GridData.GRAB_VERTICAL |
											GridData.FILL_BOTH));
	}
	
	/**
	 * This method initializes inputFilesSection	
	 */
	private void createInputFilesSection() {
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		inputFilesSection = getFormToolkit().createSection(sashform,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		inputFilesSection.setExpanded(true);
		inputFilesSection.setLayoutData(gridData);
		inputFilesSection.setText("Input files");
		inputFilesSection.setDescription("Add or drag & drop result files (*.sca or *.vec) that should be used in this analysis. Wildcards (*,?) can also be used to specify multiple files.");
		InputFilesPanel inputFilesPanel = new InputFilesPanel(inputFilesSection, SWT.NONE);
		inputFilesSection.setClient(inputFilesPanel);

		// buttons
		configureViewerButton(
				inputFilesPanel.getAddFileButton(), 
				inputFilesPanel.getTreeViewer(),
				new AddResultFileAction());
		configureViewerButton(
				inputFilesPanel.getAddWildcardButton(), 
				inputFilesPanel.getTreeViewer(),
				new AddWildcardResultFileAction());
		configureViewerButton(
				inputFilesPanel.getEditButton(),
				inputFilesPanel.getTreeViewer(),
				new EditAction());
		configureViewerButton(
				inputFilesPanel.getRemoveFileButton(), 
				inputFilesPanel.getTreeViewer(),
				new RemoveAction());
		
		// double-clicks
		configureViewerDefaultAction(
				inputFilesPanel.getTreeViewer(),
				new SetFilterAction());
	}

	/**
	 * This method initializes dataSection	
	 *
	 */
	private void createDataSection() {
		dataSection = getFormToolkit().createSection(sashform, 
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		dataSection.setExpanded(true);
		dataSection.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		dataSection.setText("Data");
		dataSection.setDescription("Here you can browse all data (vectors, scalars and histograms) that come from the result files.");
		DataPanel dataPanel = new DataPanel(dataSection, SWT.NONE);
		dataSection.setClient(dataPanel);
		
		// double-clicks
		configureViewerDefaultAction(
				dataPanel.getFileRunTreeViewer(),
				new SetFilterAction());
		configureViewerDefaultAction(
				dataPanel.getRunFileTreeViewer(),
				new SetFilterAction());
		configureViewerDefaultAction(
				dataPanel.getLogicalTreeViewer(),
				new SetFilterAction());
	}
}
