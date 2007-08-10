package org.omnetpp.launch.tabs;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import org.omnetpp.launch.IOmnetppLaunchConstants;
import org.omnetpp.launch.LaunchPlugin;

//FIXME default working directory should be the directory of the chosen executable file (not the project root)
/**
 * A little modified version of the JDT-s working block code
 * @author rhornig
 */
public class WorkingDirectoryBlock extends OmnetppLaunchTab {

    // Local directory
    private Button fWorkspaceButton;
    private Button fFileSystemButton;
    private Button fVariablesButton;

    //bug 29565 fix
    private Button fUseDefaultDirButton = null;
    private Button fUseOtherDirButton = null;
    private Text fOtherWorkingText = null;
    private Text fWorkingDirText;

    private String executableLoc;

    public WorkingDirectoryBlock() {
        super();
    }

    public WorkingDirectoryBlock(OmnetppLaunchTab embeddingTab) {
        super(embeddingTab);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Group group = SWTFactory.createGroup(parent, "Working directory", 5, 1, GridData.FILL_HORIZONTAL);
        setControl(group);
    //default choice
        fUseDefaultDirButton = SWTFactory.createRadioButton(group, "Default:");
        fUseDefaultDirButton.addSelectionListener(this);
        fWorkingDirText = SWTFactory.createSingleText(group, 4);
        fWorkingDirText.addModifyListener(this);
        fWorkingDirText.setEnabled(false);
    //user enter choice
        fUseOtherDirButton = SWTFactory.createRadioButton(group, "Other:");
        fUseOtherDirButton.addSelectionListener(this);
        fOtherWorkingText = SWTFactory.createSingleText(group, 1);
        fOtherWorkingText.addModifyListener(this);
    //buttons
        fWorkspaceButton = createPushButton(group, "Workspace...", null);
        fWorkspaceButton.addSelectionListener(this);
        fFileSystemButton = createPushButton(group, "File System...", null);
        fFileSystemButton.addSelectionListener(this);
        fVariablesButton = createPushButton(group, "Variables...", null);
        fVariablesButton.addSelectionListener(this);
    }

    /**
     * Show a dialog that lets the user select a working directory
     */
    private void handleWorkingDirBrowseButtonSelected() {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setMessage("Select a workspace relative working directory:");
        String currentWorkingDir = getWorkingDirectoryText();
        if (!currentWorkingDir.trim().equals("")) { 
            File path = new File(currentWorkingDir);
            if (path.exists()) {
                dialog.setFilterPath(currentWorkingDir);
            }
        }
        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
            fOtherWorkingText.setText(selectedDirectory);
        }
    }

    /**
     * Show a dialog that lets the user select a working directory from
     * the workspace
     */
    private void handleWorkspaceDirBrowseButtonSelected() {
        IContainer currentContainer= getContainer();
        if (currentContainer == null) {
            currentContainer = ResourcesPlugin.getWorkspace().getRoot();
        }
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), currentContainer, false, "Select a workspace relative working directory:");
        dialog.showClosedProjects(false);
        dialog.open();
        Object[] results = dialog.getResult();
        if (results != null && results.length > 0 && results[0] instanceof IPath) {
            IPath path = (IPath)results[0];
            String containerName = path.makeRelative().toString();
            setOtherWorkingDirectoryText("${workspace_loc:" + containerName + "}");  
        }
    }

    /**
     * Returns the selected workspace container,or <code>null</code>
     */
    protected IContainer getContainer() {
        String path = getWorkingDirectoryText();
        if (path.length() > 0) {
            IResource res = null;
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            if (path.startsWith("${workspace_loc:")) { 
                IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
                try {
                    path = manager.performStringSubstitution(path, false);
                    IContainer[] containers = root.findContainersForLocation(new Path(path));
                    if (containers.length > 0) {
                        res = containers[0];
                    }
                }
                catch (CoreException e) {}
            }
            else {
                res = root.findMember(path);
            }
            if (res instanceof IContainer)
                return (IContainer)res;
        }
        return null;
    }

    /**
     * The default working dir radio button has been selected.
     */
    private void handleUseDefaultWorkingDirButtonSelected() {
        fWorkspaceButton.setEnabled(false);
        fOtherWorkingText.setEnabled(false);
        fVariablesButton.setEnabled(false);
        fFileSystemButton.setEnabled(false);
        fUseOtherDirButton.setSelection(false);
    }

    /**
     * The other working dir radio button has been selected
     *
     * @since 3.2
     */
    private void handleUseOtherWorkingDirButtonSelected() {
        fOtherWorkingText.setEnabled(true);
        fWorkspaceButton.setEnabled(true);
        fVariablesButton.setEnabled(true);
        fFileSystemButton.setEnabled(true);
        updateLaunchConfigurationDialog();
    }

    /**
     * The working dir variables button has been selected
     */
    private void handleWorkingDirVariablesButtonSelected() {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variableText = dialog.getVariableExpression();
        if (variableText != null) {
            fOtherWorkingText.insert(variableText);
        }
    }

    /**
     * Sets the default working directory
     */
    protected void setDefaultWorkingDir() {
        if (executableLoc == null)
            setDefaultWorkingDirectoryText("${project_loc}");
        else
            setDefaultWorkingDirectoryText("${container_loc:"+executableLoc+"}");

    }

    /**
     * Sets the executable files workspace relative path so the default working directory can be set correctly
     * @param exeLoc
     */
    public void setExecutableLocation(String exeLoc) {
        executableLoc = exeLoc;
        setDefaultWorkingDir();
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid(ILaunchConfiguration config) {
        setErrorMessage(null);
        setMessage(null);
        // if variables are present, we cannot resolve the directory
        String workingDirPath = getWorkingDirectoryText();
        if (workingDirPath.indexOf("${") >= 0) { 
            IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
            try {
                manager.validateStringVariables(workingDirPath);
            }
            catch (CoreException e) {
                setErrorMessage(e.getMessage());
                return false;
            }
        }
        else if (workingDirPath.length() > 0) {
            IContainer container = getContainer();
            if (container == null) {
                File dir = new File(workingDirPath);
                if (dir.isDirectory())
                    return true;
                setErrorMessage("Not a directory.");
                return false;
            }
        }
        else if (workingDirPath.length() == 0) {
            setErrorMessage("Working directory must be set.");
            return false;
        }
        return true;
    }

    /**
     * Defaults are empty.
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy config) {
        config.setAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, (String)null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        super.initializeFrom(configuration);
        try {
            String wd = configuration.getAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, (String)null);
            setDefaultWorkingDir();
            if (wd != null) {
                setOtherWorkingDirectoryText(wd);
            }
        }
        catch (CoreException e) {
            setErrorMessage("Problem occurred during reading the configuration: " + e.getStatus().getMessage());
            LaunchPlugin.logError(e);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        if (fUseDefaultDirButton.getSelection()) {
            configuration.setAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, (String)null);
        }
        else {
            configuration.setAttribute(IOmnetppLaunchConstants.ATTR_WORKING_DIRECTORY, getWorkingDirectoryText());
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return "Working directory";
    }

    /**
     * gets the path from the text box that is selected
     * @return the working directory the user wishes to use
     * @since 3.2
     */
    protected String getWorkingDirectoryText() {
        if (fUseDefaultDirButton.getSelection())
            return fWorkingDirText.getText().trim();
        return fOtherWorkingText.getText().trim();
    }

    /**
     * sets the default working directory text
     * @param dir the dir to set the widget to
     * @since 3.2
     */
    protected void setDefaultWorkingDirectoryText(String dir) {
        if (dir != null) {
            fWorkingDirText.setText(dir);
            fUseDefaultDirButton.setSelection(true);
            handleUseDefaultWorkingDirButtonSelected();
        }
    }

    /**
     * sets the other dir text
     * @param dir the new text
     * @since 3.2
     */
    protected void setOtherWorkingDirectoryText(String dir) {
        if (dir != null) {
            fOtherWorkingText.setText(dir);
            fUseDefaultDirButton.setSelection(false);
            fUseOtherDirButton.setSelection(true);
            handleUseOtherWorkingDirButtonSelected();
        }
    }

    /**
     * Allows this entire block to be enabled/disabled
     * @param enabled whether to enable it or not
     */
    protected void setEnabled(boolean enabled) {
        fUseDefaultDirButton.setEnabled(enabled);
        fUseOtherDirButton.setEnabled(enabled);
        if (fOtherWorkingText.isEnabled()) {
            fOtherWorkingText.setEnabled(enabled);
            fWorkspaceButton.setEnabled(enabled);
            fVariablesButton.setEnabled(enabled);
            fFileSystemButton.setEnabled(enabled);
        }
        // in the case where the 'other' text is selected and we want to enable
        if (fUseOtherDirButton.getSelection() && enabled == true) {
            fOtherWorkingText.setEnabled(enabled);
        }
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        super.widgetSelected(e);
        Object source= e.getSource();
        if (source == fWorkspaceButton) {
            handleWorkspaceDirBrowseButtonSelected();
        }
        else if (source == fFileSystemButton) {
            handleWorkingDirBrowseButtonSelected();
        }
        else if (source == fVariablesButton) {
            handleWorkingDirVariablesButtonSelected();
        }
        else if (source == fUseDefaultDirButton) {
            //only perform the action if this is the button that was selected
            if (fUseDefaultDirButton.getSelection()) {
                setDefaultWorkingDir();
            }
        }
        else if (source == fUseOtherDirButton) {
            //only perform the action if this is the button that was selected
            if (fUseOtherDirButton.getSelection()) {
                handleUseOtherWorkingDirButtonSelected();
            }
        }
    }

}

