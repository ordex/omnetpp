package org.omnetpp.simulation.inspectors;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.omnetpp.simulation.model.cObject;


/**
 * Represents an inspector. An inspector presents a simulation object in the
 * runtime GUI. The visual representation of an inspector is a Draw2D figure
 * that can be placed on a canvas.
 *
 * The inspector is also a selection provider. When this inspector is selected
 * (see isSelected()), the selection should include this inspector (or its
 * inspected object) as well.
 */
public interface IInspectorPart {
    /**
     * Returns container for the inspector. The container handles selection,
     * closing / maximizing / changing Z-order of inspectors, and other tasks.
     */
    IInspectorContainer getContainer();

    /**
     * Returns the inspected object.
     */
    cObject getObject();

    /**
     * Refresh the inspector's contents. Should only be called when the
     * inspector is already installed (the figure is on a draw2d canvas,
     * and setContainer() was already called). The client is responsible
     * for calling this method frequently enough to keep the inspector's
     * content up to date.
     */
    void refresh();

    /**
     * Must be called when the inspector is no longer needed
     */
    void dispose();

    /**
     * TODO
     */
    boolean isDisposed();

    /**
     * Brings this inspector to the front of all inspectors in Z-order.
     */
    void raiseToTop();

    /**
     * Returns the corresponding figure. Clients are responsible
     * for inserting the figure into a draw2d canvas.
     */
    IInspectorFigure getFigure();

    /**
     * Returns whether this inspector can be maximized to fill the canvas.
     */
    boolean isMaximizable();

    /**
     * Returns true if this inspector is selected.
     */
    boolean isSelected();

    /**
     * Called when the canvas selection changes. The inspector part should
     * look at the objects in the selection, and highlight the relevant ones
     * (the ones it displays) on the canvas
     */
    void selectionChanged(IStructuredSelection selection);

    /**
     * TODO
     */
    void populateContextMenu(MenuManager manager, Point p);

    /**
     * TODO
     */
    void populateFloatingToolbar(ToolBarManager manager);

}
