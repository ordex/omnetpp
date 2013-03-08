/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.cdt.launch;

import org.eclipse.cdt.dsf.debug.service.IRunControl;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.cdt.dsf.service.DsfServiceEventHandler;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.omnetpp.cdt.Activator;
import org.omnetpp.common.IConstants;
import org.omnetpp.common.simulation.AbstractSimulationProcess;
import org.omnetpp.common.simulation.ISimulationProcess;
import org.omnetpp.common.simulation.SimulationEditorInput;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.launch.IOmnetppLaunchConstants;
import org.omnetpp.launch.LaunchPlugin;
import org.omnetpp.launch.tabs.OmnetppLaunchUtils;

/**
 * A proxy launch delegate for debugging simulations.
 */
public class SimulationDebugLaunchDelegate extends GdbLaunchDelegate {
    private static final String PREF_DONT_OFFER_CONFIG_UPGRADE = "org.omnetpp.cdt.launch.DoNotOfferUpgradingConfig";

    public static class DsfSimulationProcess extends AbstractSimulationProcess {
        private boolean isSuspended;
        private boolean isTerminated;

        public DsfSimulationProcess(DsfSession dsfSession) {
            isSuspended = false; // supposedly
            isTerminated = false;
            dsfSession.addServiceEventListener(this, null); // no need for removeListener because DsfSession only lives during the debug session
        }

        @DsfServiceEventHandler
        public void handleEvent(IRunControl.ISuspendedDMEvent event) {
            // Note: apparently these listener methods are invoked in a background thread,
            // and not necessarily in pairs (RESUME is often repeated)
            isSuspended = true;
            fireSimulationProcessSuspended();
        }

        @DsfServiceEventHandler
        public void handleEvent(IRunControl.IResumedDMEvent event) {
            isSuspended = false;
            fireSimulationProcessResumed();
        }

        @DsfServiceEventHandler
        public void handleEvent(IRunControl.IExitedDMEvent event) {
            isTerminated = true;
            fireSimulationProcessTerminated();
        }

        @Override
        public boolean canCancel() {
            return false;
        }

        @Override
        public void cancel() {
            throw new RuntimeException("cancel not supported");
        }

        @Override
        public boolean isSuspended() {
            return isSuspended;
        }

        @Override
        public boolean isTerminated() {
            return isTerminated;
        }
    };

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {

        if (!Platform.getOS().equals(Platform.OS_MACOSX)) {  //OS X contains obsolete gdb without Python support
            ILaunchConfiguration newConfig = offerUpgradingLaunchConfig(configuration);
            if (newConfig != null) {
                OmnetppLaunchUtils.replaceConfigurationInLaunch(launch, newConfig);
                configuration = newConfig;
            }
        }

        // convert configuration to be suitable for CDT
        configuration = OmnetppLaunchUtils.createUpdatedLaunchConfig(configuration, mode);
        OmnetppLaunchUtils.replaceConfigurationInLaunch(launch, configuration);

        // launch the debug session
        super.launch(launch.getLaunchConfiguration(), mode, launch, monitor);

        // open simulation front-end if required
        String envirStr = StringUtils.defaultIfEmpty(configuration.getAttribute(IOmnetppLaunchConstants.OPP_USER_INTERFACE, "").trim(), IOmnetppLaunchConstants.UI_FALLBACKVALUE);
        boolean openSimulationEditor = envirStr.equals(IOmnetppLaunchConstants.UI_IDE);

        if (openSimulationEditor) {
            final String launchConfigurationName = configuration.getName();
            final int portNumber = configuration.getAttribute(IOmnetppLaunchConstants.OPP_HTTP_PORT, -1);
            Assert.isTrue(portNumber != -1); // guaranteed by createUpdatedLaunchConfig()
            final DsfSession dsfSession = (launch instanceof GdbLaunch) ? ((GdbLaunch)launch).getSession() : null; // there must be a better way to obtain the DsfSession, without involving GDB-specific code
            if (dsfSession == null)
                Activator.log(IMarker.SEVERITY_WARNING,
                        "SimulationDebugLaunchDelegate: launch object is not a GdbLaunch (or contains no DsfSession), unable to notify " +
                        "Simulation Front-end about process suspend/resume events during the debug session. Please report this error!");
            final ISimulationProcess simulationProcess = new DsfSimulationProcess(dsfSession);

            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
                    try {
                        // open the editor
                        IEditorInput input = new SimulationEditorInput(launchConfigurationName, "localhost", portNumber, simulationProcess, launchConfigurationName, true);
                        IDE.openEditor(workbenchPage, input, IConstants.SIMULATION_EDITOR_ID);
                    }
                    catch (PartInitException e) {
                        ErrorDialog.openError(workbenchWindow.getShell(), "Error", "Could not open animation window for the running simulation.", e.getStatus());
                        LaunchPlugin.logError(e);
                    }
                }
            });
        }
    }

    /**
     * Convert pre-4.3 launch configs to 4.3+. This code can be removed once we are well past version 4.3.
     */
    protected ILaunchConfiguration offerUpgradingLaunchConfig(final ILaunchConfiguration config) throws CoreException {
        String gdbInitFile = config.getAttribute(IOmnetppLaunchConstants.ATTR_DEBUGGER_GDB_INIT, "");

        if (gdbInitFile.equals(".gdbinit")) {
            // offer user to replace it with the right setting
            final IPreferenceStore preferences = LaunchPlugin.getDefault().getPreferenceStore();
            String pref = preferences.getString(PREF_DONT_OFFER_CONFIG_UPGRADE);

            if (MessageDialogWithToggle.ALWAYS.equals(pref)) {
                return upgradeLaunchConfig(config);
            }
            else if (!MessageDialogWithToggle.NEVER.equals(pref)) {
                final int[] dialogResult = new int[1];
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        dialogResult[0] = MessageDialogWithToggle.openYesNoQuestion(
                                Display.getCurrent().getActiveShell(),
                                "Obsolete Launch Configuration",
                                "Launch configuration '" + config.getName() + "' contains '.gdbinit' as " +
                                "GDB initialization file. To be able to utilize pretty printers, this should be " +
                                "changed to '" + IOmnetppLaunchConstants.OPP_GDB_INIT_FILE + "'.\n\n" +
                                "Upgrade launch configuration?",
                                "Always do that in the future", false,
                                preferences, PREF_DONT_OFFER_CONFIG_UPGRADE).getReturnCode();
                    }
                });

                if (dialogResult[0] == IDialogConstants.YES_ID)
                    return upgradeLaunchConfig(config);
            }
        }
        return null;
    }

    protected ILaunchConfigurationWorkingCopy upgradeLaunchConfig(ILaunchConfiguration config) throws CoreException {
        ILaunchConfigurationWorkingCopy workingCopy = config.getWorkingCopy();
        workingCopy.setAttribute(IOmnetppLaunchConstants.ATTR_DEBUGGER_GDB_INIT, IOmnetppLaunchConstants.OPP_GDB_INIT_FILE);
        workingCopy.doSave();
        return workingCopy;
    }
}
