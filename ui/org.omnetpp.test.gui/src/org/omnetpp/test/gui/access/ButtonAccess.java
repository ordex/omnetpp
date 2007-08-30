package org.omnetpp.test.gui.access;

import org.eclipse.swt.widgets.Button;
import org.omnetpp.test.gui.core.InUIThread;

public class ButtonAccess extends ControlAccess
{
	public ButtonAccess(Button control) {
		super(control);
	}
	
	public Button getButton() {
		return (Button)widget;
	}

	@InUIThread
	public void activateWithMouseClick() {
		assertEnabled();
		click();
	}
}
