package org.omnetpp.common.eventlog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.omnetpp.common.ui.EditableList;
import org.omnetpp.common.ui.GenericTreeContentProvider;
import org.omnetpp.common.ui.GenericTreeNode;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.eventlog.engine.IEventLog;
import org.omnetpp.eventlog.engine.ModuleCreatedEntry;
import org.omnetpp.eventlog.engine.ModuleCreatedEntryList;
import org.omnetpp.eventlog.engine.PStringVector;

@SuppressWarnings("unused")
public class FilterEventLogDialog
    extends TitleAreaDialog
{
    private EventLogInput eventLogInput;

	private EventLogFilterParameters filterParameters;
	
	private CheckboxTreeViewer panelCheckboxTree;

    private FilterDialogTreeNode enableCollectionLimits;

    private FilterDialogTreeNode enableEventNumberFilter;

	private FilterDialogTreeNode enableSimulationTimeFilter;

	private FilterDialogTreeNode enableModuleFilter;

    private FilterDialogTreeNode enableModuleExpressionFilter;

    private FilterDialogTreeNode enableModuleClassNameFilter;

    private FilterDialogTreeNode enableModuleNameFilter;

    private FilterDialogTreeNode enableModuleIdFilter;

    private FilterDialogTreeNode enableMessageFilter;
	
	private FilterDialogTreeNode enableMessageExpressionFilter;

    private FilterDialogTreeNode enableMessageClassNameFilter;

    private FilterDialogTreeNode enableMessageNameFilter;

    private FilterDialogTreeNode enableMessageIdFilter;

	private FilterDialogTreeNode enableMessageTreeIdFilter;

	private FilterDialogTreeNode enableMessageEncapsulationIdFilter;

	private FilterDialogTreeNode enableMessageEncapsulationTreeIdFilter;

    private FilterDialogTreeNode enableTraceFilter;

	private Text lowerEventNumberLimit;

	private Text upperEventNumberLimit;
	
	private Text lowerSimulationTimeLimit;
	
	private Text upperSimulationTimeLimit;

	private Text tracedEventNumber;

	private Button traceCauses;

	private Button traceConsequences;

	private Button traceMessageReuses;

	private Button traceSelfMessages;
	
	private Text causeEventNumberDelta;

	private Text consequenceEventNumberDelta;

	private Text causeSimulationTimeDelta;

	private Text consequenceSimulationTimeDelta;

	private Text moduleFilterExpression;

	private CheckboxTableViewer moduleClassNames;

	private ModuleTreeViewer moduleNameIds;

	private CheckboxTableViewer moduleIds;

	private Text messageFilterExpression;

	private CheckboxTableViewer messageClassNames;

	private CheckboxTableViewer messageNames;

	private EditableList messageIds;

	private EditableList messageTreeIds;

	private EditableList messageEncapsulationIds;

	private EditableList messageEncapsulationTreeIds;

    private Button collectMessageReuses;

    private Text maximumNumberOfMessageDependencies;

    private Text maximumDepthOfMessageDependencies;

    private String initialTreeNodeName;

	public FilterEventLogDialog(Shell parentShell, EventLogInput eventLogInput, EventLogFilterParameters filterParameters) {
		super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
		this.eventLogInput = eventLogInput;
		this.filterParameters = filterParameters;
	}

	private void unparseFilterParameters() {
		try {
			Class<EventLogFilterParameters> clazz = EventLogFilterParameters.class;
			
			for (Field parameterField : clazz.getDeclaredFields()) {
				if ((parameterField.getModifiers() & Modifier.PUBLIC) != 0) {
					Class<?> parameterFieldType = parameterField.getType();
					Field guiField = getClass().getDeclaredField(parameterField.getName());
	
					if (parameterFieldType == boolean.class) {
					    Object guiFieldValue = guiField.get(this);
					    boolean value = parameterField.getBoolean(filterParameters);
                        if (guiFieldValue instanceof Button)
                            unparseBoolean((Button)guiFieldValue, value);
                        else if (guiFieldValue instanceof FilterDialogTreeNode)
					        unparseBoolean((FilterDialogTreeNode)guiFieldValue, value);
                        else
                            throw new RuntimeException("Unknown gui field type");
					}
					else if (parameterFieldType == int.class)
						unparseInt((Text)guiField.get(this), parameterField.getInt(filterParameters));
					else if (parameterFieldType == BigDecimal.class)
						unparseBigDecimal((Text)guiField.get(this), (BigDecimal)parameterField.get(filterParameters));
					else if (parameterFieldType == String.class)
						unparseString((Text)guiField.get(this), (String)parameterField.get(filterParameters));
					else if (parameterFieldType == int[].class) {
						Object guiControl = guiField.get(this);
						
						if (guiControl instanceof EditableList)
							unparseIntArray((EditableList)guiControl, (int[])parameterField.get(filterParameters));
						else if (guiControl instanceof CheckboxTableViewer)
							unparseIntArray((CheckboxTableViewer)guiControl, (int[])parameterField.get(filterParameters));
						else if (guiControl instanceof ModuleTreeViewer)
							unparseModuleNameIdArray((ModuleTreeViewer)guiControl, (int[])parameterField.get(filterParameters));
						else
							throw new RuntimeException("Unknown gui field type");
					}
					else if (parameterFieldType == String[].class) {
						Object guiControl = guiField.get(this);
						
						if (guiControl instanceof EditableList)
							unparseStringArray((EditableList)guiField.get(this), (String[])parameterField.get(filterParameters));
						else if (guiControl instanceof CheckboxTableViewer)
							unparseStringArray((CheckboxTableViewer)guiControl, (String[])parameterField.get(filterParameters));
						else
							throw new RuntimeException("Unknown gui field type");
					}
					else
						throw new RuntimeException("Unknown parameter field type");
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void unparseBoolean(Button button, boolean value) {
	    button.setSelection(value);
	}
	
    private void unparseBoolean(FilterDialogTreeNode treeNode, boolean value) {
        panelCheckboxTree.setChecked(treeNode, value);
        treeNode.checkStateChanged(value);
    }
    
	private void unparseInt(Text text, int value) {
		if (value != -1)
			text.setText(String.valueOf(value));
	}
	
	private void unparseBigDecimal(Text text, BigDecimal value) {
		if (value != null)
			text.setText(value.toString());
	}
	
	private void unparseString(Text text, String value) {
		if (value != null)
			text.setText(value);
	}

	private void unparseIntArray(EditableList editableList, int[] values) {
		if (values != null) {
			String[] stringValues = new String[values.length];
			for (int i = 0; i < values.length; i++)
				stringValues[i] = String.valueOf(values[i]);
			editableList.getList().setItems(stringValues);
		}
	}

	private void unparseIntArray(CheckboxTableViewer checkboxTableViewer, int[] values) {
		if (values != null) {
			Integer[] integerValues = new Integer[values.length];

			for (int i = 0; i < values.length; i++)
				integerValues[i] = values[i];

			checkboxTableViewer.setCheckedElements(integerValues);
		}
	}

	private void unparseStringArray(EditableList editableList, String[] values) {
		if (values != null)
			editableList.getList().setItems(values);
	}

	private void unparseModuleNameIdArray(ModuleTreeViewer moduleTreeViewer, int[] values) {
		if (values != null) {
			ArrayList<ModuleTreeItem> moduleTreeItems = new ArrayList<ModuleTreeItem>();
	
			for (int i = 0; i < values.length; i++) {
			    ModuleTreeItem item = eventLogInput.getModuleTreeRoot().findDescendantModule(values[i]);
			    
			    if (item != null)
			        moduleTreeItems.add(item);
			}
	
			moduleTreeViewer.setCheckedElements(moduleTreeItems.toArray());
		}
	}

	private void unparseStringArray(CheckboxTableViewer checkboxTableViewer, String[] values) {
		if (values != null)
			checkboxTableViewer.setCheckedElements(values);
	}

	public void parseFilterParameters() {
		try {
			Class<EventLogFilterParameters> clazz = EventLogFilterParameters.class;
			
			for (Field parameterField : clazz.getDeclaredFields()) {
				if ((parameterField.getModifiers() & Modifier.PUBLIC) != 0) {
					Class<?> parameterFieldType = parameterField.getType();
					Field guiField = getClass().getDeclaredField(parameterField.getName());
	
					if (parameterFieldType == boolean.class) {
					    Object guiFieldValue = guiField.get(this);
					    
					    if (guiFieldValue instanceof Button)
					        parameterField.setBoolean(filterParameters, parseBoolean((Button)guiFieldValue));
					    else if (guiFieldValue instanceof FilterDialogTreeNode)
	                        parameterField.setBoolean(filterParameters, parseBoolean((FilterDialogTreeNode)guiFieldValue));
                        else
                            throw new RuntimeException("Unknown gui field type");
					}
					else if (parameterFieldType == int.class)
						parameterField.setInt(filterParameters, parseInt((Text)guiField.get(this)));
					else if (parameterFieldType == BigDecimal.class)
						parameterField.set(filterParameters, parseBigDecimal((Text)guiField.get(this)));
					else if (parameterFieldType == String.class)
						parameterField.set(filterParameters, parseString((Text)guiField.get(this)));
					else if (parameterFieldType == int[].class) {
						Object guiControl = guiField.get(this);

						if (guiControl instanceof EditableList)
							parameterField.set(filterParameters, parseIntArray((EditableList)guiControl));
						else if (guiControl instanceof ModuleTreeViewer)
							parameterField.set(filterParameters, parseModuleNameIdArray((ModuleTreeViewer)guiField.get(this)));
						else if (guiControl instanceof CheckboxTableViewer)
							parameterField.set(filterParameters, parseIntArray((CheckboxTableViewer)guiField.get(this)));
						else
							throw new RuntimeException("Unknown gui field type");
					}
					else if (parameterFieldType == String[].class) {
						Object guiControl = guiField.get(this);

						if (guiControl instanceof EditableList)
							parameterField.set(filterParameters, parseStringArray((EditableList)guiField.get(this)));
						else if (guiControl instanceof CheckboxTableViewer)
							parameterField.set(filterParameters, parseModuleClassNameArray((CheckboxTableViewer)guiField.get(this)));
						else
							throw new RuntimeException("Unknown gui field type");
					}
					else
						throw new RuntimeException("Unknown parameter field type");
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean parseBoolean(Button button) {
		return button.getSelection();
	}
	
    private boolean parseBoolean(FilterDialogTreeNode treeNode) {
        return panelCheckboxTree.getChecked(treeNode);
    }
    
	private int parseInt(Text text) {
		if (text.getText().length() != 0)
			return Integer.parseInt(text.getText());
		else
			return -1;
	}
	
	private BigDecimal parseBigDecimal(Text text) {
		if (text.getText().length() != 0)
			return new BigDecimal(text.getText());
		else
			return null;
	}
	
	private String parseString(Text text) {
		return text.getText();
	}
	
	private int[] parseIntArray(EditableList editableList) {
		String[] stringValues = editableList.getList().getItems();
		int[] intValues = new int[editableList.getList().getItems().length];

		for (int i = 0; i < stringValues.length; i++)
			intValues[i] = Integer.parseInt(stringValues[i]);

		return intValues;
	}
	
	private int[] parseIntArray(CheckboxTableViewer checkBoxTableViewer) {
		Object[] elements = checkBoxTableViewer.getCheckedElements();
		int[] values = new int[elements.length];
		
		for (int i = 0; i < elements.length; i++)
			values[i] = (Integer)elements[i];
		
		return values;
	}

	private String[] parseStringArray(EditableList editableList) {
		return editableList.getList().getItems();
	}
	
	private int[] parseModuleNameIdArray(ModuleTreeViewer moduleTreeViewer) {
		Object[] treeItems = moduleTreeViewer.getCheckedElements();
		int[] values = new int[treeItems.length];

		for (int i = 0; i < values.length; i++)
			values[i] = ((ModuleTreeItem)treeItems[i]).getModuleId();
		
		return values;
	}

	private String[] parseModuleClassNameArray(CheckboxTableViewer checkBoxTableViewer) {
		Object[] elements = checkBoxTableViewer.getCheckedElements();
		String[] moduleClassNames = new String[elements.length];
		
		for (int i = 0; i < elements.length; i++)
			moduleClassNames[i] = (String)elements[i];
		
		return moduleClassNames;
	}
	
	public int open(String initialTreeNodeName) {
	    this.initialTreeNodeName = initialTreeNodeName;
	    return super.open();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 400;
		container.setLayoutData(gridData);
		container.setLayout(new GridLayout(2, false));

		setHelpAvailable(false);
		setTitle("Select filter criteria");
		setMessage("The event log will be filtered for events that match all criteria");

		// create left hand side tree viewer
		panelCheckboxTree = new CheckboxTreeViewer(container);

		// create right hand side panel container
        final Composite panelContainer = new Composite(container, SWT.NONE);
        panelContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        final StackLayout stackLayout = new StackLayout();
        panelContainer.setLayout(stackLayout);
        final Label defaultPanel = new Label(panelContainer, SWT.None);
        defaultPanel.setText("Please select an option from the tree on the left");
        stackLayout.topControl = defaultPanel;

        // create tree
        GenericTreeNode treeRoot = new GenericTreeNode("root");
        treeRoot.addChild(createCollectionLimitsTreeNode(panelContainer));
        treeRoot.addChild(createGeneralFilterTreeNode(panelContainer));
        treeRoot.addChild(createModuleFilterTreeNode(panelContainer));
        treeRoot.addChild(createMessageFilterTreeNode(panelContainer));
        treeRoot.addChild(createEventTraceFilterTreeNode(panelContainer));

        panelCheckboxTree.setContentProvider(new GenericTreeContentProvider());
        panelCheckboxTree.setInput(treeRoot);
        panelCheckboxTree.expandAll();
        panelCheckboxTree.getTree().setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, true));
        
        panelCheckboxTree.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                Object firstSelection = ((ITreeSelection)event.getSelection()).getFirstElement();
                
                if (firstSelection != null) {
                    FilterDialogTreeNode treeNode = (FilterDialogTreeNode)firstSelection;
                    
                    if (treeNode.getPanel() != null)
                        stackLayout.topControl = treeNode.getPanel();
                    else
                        stackLayout.topControl = defaultPanel;

                    panelContainer.layout();
                }
            }
        });
        
        panelCheckboxTree.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                GenericTreeNode clickedTreeNode = (GenericTreeNode)event.getElement();

                if (event.getChecked()) {
                    GenericTreeNode treeNode = clickedTreeNode; 
                    if (treeNode.getChildCount() != 0)
                        panelCheckboxTree.setChecked(treeNode, false);
                    else {
                        while (treeNode != null) {
                            if (treeNode instanceof FilterDialogTreeNode)
                                ((FilterDialogTreeNode)treeNode).checkStateChanged(true);
                            panelCheckboxTree.setChecked(treeNode, true);
                            treeNode = treeNode.getParent();
                        }
                    }
                }
                else
                    treeNodeDeselected(new GenericTreeNode[] {clickedTreeNode});

                panelCheckboxTree.setSelection(new StructuredSelection(event.getElement()));
                if (clickedTreeNode instanceof FilterDialogTreeNode && ((FilterDialogTreeNode)clickedTreeNode).getPanel() != null) {
                    stackLayout.topControl = ((FilterDialogTreeNode)clickedTreeNode).getPanel();
                    panelContainer.layout();
                }
            }
            
            private void treeNodeDeselected(GenericTreeNode[] treeNodes) {
                for (GenericTreeNode treeNode : treeNodes) {
                    ((FilterDialogTreeNode)treeNode).checkStateChanged(false);
                    panelCheckboxTree.setChecked(treeNode, false);
                    treeNodeDeselected(treeNode.getChildren());
                }
            }
        });

        if (initialTreeNodeName != null) {
            for (GenericTreeNode treeNode : treeRoot.getChildren())
                if (treeNode.getPayload().equals(initialTreeNodeName))
                    panelCheckboxTree.setSelection(new TreeSelection(new TreePath(new Object[] {treeRoot, treeNode})));
        }

		unparseFilterParameters();

		return container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		newShell.setText("Filter event log");
		super.configureShell(newShell);
	}
	
	@Override
	protected void okPressed() {
		parseFilterParameters();
		super.okPressed();
	}

	private GenericTreeNode createCollectionLimitsTreeNode(Composite parent) {
        // depth and number limits
        Composite panel = createPanel(parent, "Collection Limits", "Here you can specify limits when searching for message dependencies.", 2);
        enableCollectionLimits = new FilterDialogTreeNode("Collection limits", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                collectMessageReuses.setEnabled(checked);
                maximumNumberOfMessageDependencies.setEnabled(checked);
                maximumDepthOfMessageDependencies.setEnabled(checked);
            }
        };

        collectMessageReuses = createCheckbox(panel, "Collect message reuse dependencies", "Message reuses will be followed when collecting dependencies between events far away on the consequence chain", 2);

        Label label = createLabel(panel, "Maximum number of message dependencies:", "Collecting message dependencies will stop at this limit for each event", 1);
        maximumNumberOfMessageDependencies = createText(panel, label.getToolTipText(), 1);

        label = createLabel(panel, "Maximum depth of message dependencies:", "Collecting message dependencies will not look deeper into the cause/consequence chain than this limit", 1);
        maximumDepthOfMessageDependencies = createText(panel, label.getToolTipText(), 1);

        return enableCollectionLimits;
	}
	
	private GenericTreeNode createGeneralFilterTreeNode(Composite parent) {
	    // generic filter
	    Composite panel0 = createPanel(parent, "Range", "Choose a subcategory to limit the chart to a range of event numbers or simulation times.", 1);
	    FilterDialogTreeNode generalFilter = new FilterDialogTreeNode("Range", panel0);

		// event number filter
        Composite panel = createPanel(parent, "Event Number Range", "When enabled, only events within the given event number range will be considered.", 2);

        generalFilter.addChild(enableEventNumberFilter = new FilterDialogTreeNode("by event numbers", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                lowerEventNumberLimit.setEnabled(checked);
                upperEventNumberLimit.setEnabled(checked);
            }
        });

		Label label = createLabel(panel, "Lower event number limit:", "Events with event number less than the provided will be filtered out", 1);
		lowerEventNumberLimit = createText(panel, label.getToolTipText(), 1);

		label = createLabel(panel, "Upper event number limit:", "Events with event number greater than the provided will be filtered out", 1);
		upperEventNumberLimit = createText(panel, label.getToolTipText(), 1);

		// simulation time filter
		panel = createPanel(parent, "Simulation Time Range", "When enabled, only events within the given simulation time range will be considered.", 2);
        generalFilter.addChild(enableSimulationTimeFilter = new FilterDialogTreeNode("by simulation time", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                lowerSimulationTimeLimit.setEnabled(checked);
                upperSimulationTimeLimit.setEnabled(checked);
            }
        });
		
		label = createLabel(panel, "Lower simulation time limit in seconds:", "Events occured before this simulation time will be filtered out from the result", 1);
		lowerSimulationTimeLimit = createText(panel, label.getToolTipText(), 1);

		label = createLabel(panel, "Upper simulation time limit in seconds:", "Events occured after this simulation time will be filtered out from the result", 1);
		upperSimulationTimeLimit = createText(panel, label.getToolTipText(), 1);
		
		return generalFilter;
	}

	private FilterDialogTreeNode createModuleFilterTreeNode(Composite parent) {
	    // synchronize tree state first
        eventLogInput.synchronizeModuleTree();

        // module filter 
        Composite panel1 = createPanel(parent, "Module Filter", "Choose a subcategory to filter to events that occurred in selected modules.", 1);
        enableModuleFilter = new FilterDialogTreeNode("Module filter", panel1);

        // expression filter
        Composite panel = createPanel(parent, "Module Filter Expression", "When enabled, only events in modules that match the expression will be considered.", 2);
        enableModuleFilter.addChild(enableModuleExpressionFilter = new FilterDialogTreeNode("by expression", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                moduleFilterExpression.setEnabled(checked);
            }
        });

		Label label = createLabel(panel, "Expression:", null, 1); //FIXME content assist!! tooltip!!!!
		
		moduleFilterExpression = new Text(panel, SWT.BORDER);
		moduleFilterExpression.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		// module class name filter
        IEventLog eventLog = eventLogInput.getEventLog();
        panel = createPanel(parent, "Filter by Module Type", "When enabled, only modules with the selected NED types will be considered.", 2);
        enableModuleFilter.addChild(enableModuleClassNameFilter = new FilterDialogTreeNode("by NED type", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                moduleClassNames.getTable().setEnabled(checked);
            }
        });

        ModuleCreatedEntryList moduleCreatedEntryList = eventLog.getModuleCreatedEntries();
		Set<String> moduleClassNameSet = new HashSet<String>();
		for (int i = 0; i < moduleCreatedEntryList.size(); i++) {
			ModuleCreatedEntry moduleCreatedEntry = moduleCreatedEntryList.get(i);
			if (moduleCreatedEntry != null)
				moduleClassNameSet.add(moduleCreatedEntry.getModuleClassName());
		}

		String[] moduleClassNamesAsStrings = (String[])moduleClassNameSet.toArray(new String[0]);
		Collections.sort(Arrays.asList(moduleClassNamesAsStrings), StringUtils.dictionaryComparator);
		moduleClassNames = CheckboxTableViewer.newCheckList(panel, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		moduleClassNames.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		moduleClassNames.add(moduleClassNamesAsStrings);

		// module name filter
		panel = createPanel(parent, "Filter by Module Name", "When enabled, only modules with the selected names will be considered.", 2);
        enableModuleFilter.addChild(enableModuleNameFilter = new FilterDialogTreeNode("by name", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                moduleNameIds.getTree().setEnabled(checked);
            }
        });

		moduleNameIds = new ModuleTreeViewer(panel, eventLogInput.getModuleTreeRoot());
        moduleNameIds.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        
        // module id filter
        panel = createPanel(parent, "Filter by Module IDs", "When enabled, only modules with the selected IDs will be considered.", 2);
        enableModuleFilter.addChild(enableModuleIdFilter = new FilterDialogTreeNode("by id", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                moduleIds.getTable().setEnabled(checked);
            }
        });

        moduleIds = CheckboxTableViewer.newCheckList(panel, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		moduleIds.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		moduleIds.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				ModuleTreeItem moduleTreeItem = eventLogInput.getModuleTreeRoot().findDescendantModule((Integer)element);
				
				return "(id = " + moduleTreeItem.getModuleId() + ") " + moduleTreeItem.getModuleFullPath();
			}
		});
		ModuleCreatedEntryList moduleCreatedEntries = eventLog.getModuleCreatedEntries();
		for (int i = 0; i < moduleCreatedEntries.size(); i++) {
			ModuleCreatedEntry moduleCreatedEntry = moduleCreatedEntries.get(i);
			
			if (moduleCreatedEntry != null)
				moduleIds.add(moduleCreatedEntry.getModuleId());
		}
		
		return enableModuleFilter;
	}

	private FilterDialogTreeNode createMessageFilterTreeNode(Composite parent) {
        // message filter 
        Composite panel0 = createPanel(parent, "Message Filter", "Choose a subcategory to filter to events processing selected messages.",  1);
        enableMessageFilter = new FilterDialogTreeNode("Message filter", panel0);

		// expression filter
        Composite panel = createPanel(parent, "Message Filter Exression", "When enabled, only messages that match the filter expression will be considered.", 2);
        enableMessageFilter.addChild(enableMessageExpressionFilter = new FilterDialogTreeNode("by expression", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                messageFilterExpression.setEnabled(checked);
            }
        });

        Label label = createLabel(panel, "Expression:", null, 1);  //FIXME tooltip!!!
		messageFilterExpression = createText(panel, null, 1);

		// message class name filter
        IEventLog eventLog = eventLogInput.getEventLog();
        panel = createPanel(parent, "Filter by Message Class", "When enabled, only messages of the selected classes will be considered.", 2);
        enableMessageFilter.addChild(enableMessageClassNameFilter = new FilterDialogTreeNode("by class name", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                messageClassNames.getTable().setEnabled(checked);
            }
        });

		label = createLabel(panel, "Message classes encountered so far:", null, 2);

        PStringVector names = eventLog.getMessageClassNames().keys();
        String[] messageClassNamesAsStrings = new String[(int)names.size()];
        for (int i = 0; i < names.size(); i++)
            messageClassNamesAsStrings[i] = names.get(i);
        Collections.sort(Arrays.asList(messageClassNamesAsStrings), StringUtils.dictionaryComparator);

        messageClassNames = CheckboxTableViewer.newCheckList(panel, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		messageClassNames.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		messageClassNames.add(messageClassNamesAsStrings);

		// message name filter
        panel = createPanel(parent, "Filter by Message Name", "When enabled, only messages with the selected names will be considered.", 2);
        enableMessageFilter.addChild(enableMessageNameFilter = new FilterDialogTreeNode("by name", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                messageNames.getTable().setEnabled(checked);
            }            
        });

        label = createLabel(panel, "Message names encountered so far:", null, 2);
        
		names = eventLog.getMessageNames().keys();
		String[] messageNamesAsStrings = new String[(int)names.size()];
        for (int i = 0; i < names.size(); i++)
            messageNamesAsStrings[i] = names.get(i);
        Collections.sort(Arrays.asList(messageNamesAsStrings), StringUtils.dictionaryComparator);

        messageNames = CheckboxTableViewer.newCheckList(panel, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		messageNames.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		messageNames.add(messageNamesAsStrings);

		// message id filter
//XXX        Composite panel2 = createPanel(parent, "Filter by Message ID", "When enabled, only messages with the selected IDs will be considered.",  1);
//        enableMessageIdFilter = new FilterDialogTreeNode("by id", panel2);

		
		Object[] values = createPanelWithEditableList(parent, "by id");
		enableMessageIdFilter = (FilterDialogTreeNode)values[0];
		messageIds = (EditableList)values[1];
		
		values = createPanelWithEditableList(parent, "by tree id");
		enableMessageTreeIdFilter = (FilterDialogTreeNode)values[0];
		messageTreeIds = (EditableList)values[1];

		values = createPanelWithEditableList(parent, "by encapsulation id");
		enableMessageEncapsulationIdFilter = (FilterDialogTreeNode)values[0];
		messageEncapsulationIds = (EditableList)values[1];
		
		values = createPanelWithEditableList(parent, "by encapsulation tree id");
		enableMessageEncapsulationTreeIdFilter = (FilterDialogTreeNode)values[0];
		messageEncapsulationTreeIds = (EditableList)values[1];
		
		return enableMessageFilter;
	}

	private Object[] createPanelWithEditableList(Composite parent, String label) {
        Composite panel = createPanel(parent, "Filter "+label, "TBD", 2);

        final EditableList editableList = new EditableList(panel, SWT.NONE);
        editableList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        FilterDialogTreeNode treeNode = new FilterDialogTreeNode(label, panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                editableList.setEnabled(checked);
            }
        };
        enableMessageFilter.addChild(treeNode);
        
		return new Object[] {treeNode, editableList};
	}

	private FilterDialogTreeNode createEventTraceFilterTreeNode(Composite parent) {
        // trace filter 
        Composite panel = createPanel(parent, "Cause/Consequence Filter", "When enabled, only the selected event and its causes/consequences will be considered.", 2);
        enableTraceFilter = new FilterDialogTreeNode("Cause/consequence filter", panel) {
            @Override
            public void checkStateChanged(boolean checked) {
                tracedEventNumber.setEnabled(checked);
                traceCauses.setEnabled(checked);
                traceConsequences.setEnabled(checked);
                traceMessageReuses.setEnabled(checked);
                traceSelfMessages.setEnabled(checked);
                causeEventNumberDelta.setEnabled(checked);
                consequenceEventNumberDelta.setEnabled(checked);
                causeSimulationTimeDelta.setEnabled(checked);
                consequenceSimulationTimeDelta.setEnabled(checked);
            }
        };


		Label label = createLabel(panel, "Event number to be traced:", "An event which is neither cause nor consequence of this event will be filtered out from the result", 1);
		tracedEventNumber = createText(panel, label.getToolTipText(), 1);
		traceCauses = createCheckbox(panel, "Include cause events", null, 2);
		traceConsequences = createCheckbox(panel, "Include consequence events", null, 2);
		traceMessageReuses = createCheckbox(panel, "Follow message reuse dependencies", null, 2);
		traceSelfMessages = createCheckbox(panel, "Follow self-message dependencies", null, 2);

		// event number limits
		Group group = new Group(panel, SWT.NONE);
		group.setText("Event number limits");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

		label = createLabel(group, "Cause event number delta limit:", "Cause events with event number delta greater than this will be filtered out from the result", 1);
		causeEventNumberDelta = createText(group, label.getToolTipText(), 1);
        causeEventNumberDelta.setText("1000");

		label = createLabel(group, "Consequence event number delta limit:", "Consequence events with event number delta greater than this will be filtered out from the result", 1);
		consequenceEventNumberDelta = createText(group, label.getToolTipText(), 1);
		consequenceEventNumberDelta.setText("1000");

		// simulation time limits
		group = new Group(panel, SWT.NONE);
		group.setText("Simulation time limits");
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

		label = createLabel(group, "Cause simulation time delta limit in seconds:", "Cause events occured before this simulation time delta will be filtered out from the result", 1);
		causeSimulationTimeDelta = createText(group, label.getToolTipText(), 1);

		label = createLabel(group, "Consequence simulation time delta limit in seconds:", "Consequence events occured after this simulation time delta will be filtered out from the result", 1);
		consequenceSimulationTimeDelta = createText(group, label.getToolTipText(), 1);
		
		return enableTraceFilter;
	}

    protected Composite createPanel(Composite parent, String title, String description, int numColumns) {
        Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        panel.setLayout(new GridLayout(numColumns, false));
        
        Label titleLabel = new Label(panel, SWT.NONE);
        titleLabel.setFont(JFaceResources.getBannerFont());
        titleLabel.setText(title);
        titleLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, numColumns, 1));

        if (description != null) {
            Label descriptionLabel = new Label(panel, SWT.WRAP);
            descriptionLabel.setText(description);
            descriptionLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, numColumns, 1));
        }

        Label separator = new Label(panel, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numColumns, 1));

        return panel;
    }

	protected Label createLabel(Composite parent, String text, String tooltip, int hspan) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setToolTipText(tooltip);
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, hspan, 1));
        return label;
	}
	
	protected Button createCheckbox(Composite parent, String text, String tooltip, int hspan) {
	    Button checkbox = new Button(parent, SWT.CHECK);
	    checkbox.setText(text);
	    checkbox.setToolTipText(tooltip);
	    checkbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, hspan, 1));
	    return checkbox;
	}

	protected Text createText(Composite parent, String tooltip, int hspan) {
	    Text text = new Text(parent, SWT.BORDER);
	    text.setToolTipText(tooltip);
	    text.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, hspan, 1));
	    return text;
	}
	
	private class FilterDialogTreeNode extends GenericTreeNode {
	    private Control panel;

	    public FilterDialogTreeNode(Object payload, Control panel) {
            super(payload);
            this.panel = panel;
        }
	    
	    public Control getPanel() {
            return panel;
        }
	    
	    public void checkStateChanged(boolean checked) {
	    }
	}
}
