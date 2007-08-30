package org.omnetpp.test.gui.access;

import junit.framework.Assert;

import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.internal.PartSite;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.omnetpp.test.gui.core.InUIThread;

public class WorkbenchPartAccess
	extends Access
{
	protected IWorkbenchPart workbenchPart;

	public WorkbenchPartAccess(IWorkbenchPart workbenchPart) {
		this.workbenchPart = workbenchPart;
	}

	public IWorkbenchPart getWorkbenchPart() {
		return workbenchPart;
	}
	
	public CompositeAccess getComposite() {
		return (CompositeAccess)createAccess(getCompositeInternal());
	}

	protected Composite getCompositeInternal() {
		IWorkbenchPartSite site = workbenchPart.getSite();

		if (site instanceof PartSite)
			return (Composite)((PartSite)site).getPane().getControl();
		else if (site instanceof MultiPageEditorSite)
			return (Composite)((PartSite)((MultiPageEditorSite)site).getMultiPageEditor().getSite()).getPane().getControl();

		Assert.fail("Unknown site " + site);
		return null;
	}

	@InUIThread
	public TreeAccess findTree() {
		return getComposite().findTree();
	}

	@InUIThread
	public void activateWithMouseClick() {
		CTabItem cTabItem = findDescendantCTabItemByLabel(getCompositeInternal().getParent(), workbenchPart.getSite().getRegisteredName());
		new CTabItemAccess(cTabItem).click();
	}
}
