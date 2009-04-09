package org.omnetpp.runtimeenv.views;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.ui.PinnableView;
import org.omnetpp.experimental.simkernel.swig.cClassDescriptor;
import org.omnetpp.experimental.simkernel.swig.cEnum;
import org.omnetpp.experimental.simkernel.swig.cModule;
import org.omnetpp.experimental.simkernel.swig.cObject;
import org.omnetpp.experimental.simkernel.swig.cSimulation;
import org.omnetpp.runtimeenv.Activator;
import org.omnetpp.runtimeenv.ISimulationListener;
import org.omnetpp.runtimeenv.editors.GraphicalModulePart;
import org.omnetpp.runtimeenv.editors.ModelCanvas;
import org.omnetpp.runtimeenv.editors.ModuleIDEditorInput;

/**
 * 
 * @author Andras
 */
//XXX should display full path of root objects
//TODO we should support user-supplied images as well
//FIXME extremely slow!!!! much slower than the graphical canvas or the module log!!!!
//FIXME because of the delayed update (PinnableView), more prone to crashes by showing obsolete objects?
//XXX what's lost, compared to Tcl: bold; field editing; expanding multi-line text
public class ObjectPropertiesView extends PinnableView implements ISimulationListener {
	public static final String ID = "org.omnetpp.runtimeenv.ObjectPropertiesView";

	protected TreeViewer viewer;
    protected MenuManager contextMenuManager = new MenuManager("#PopupMenu");

    class KeyBase {
        Object parent; // otherwise setExpandedElements and preserving selection doesn't work
        long ptr;
        cClassDescriptor desc;

        public KeyBase(Object parent, long ptr, cClassDescriptor desc) {
            this.parent = parent; this.desc = desc; this.ptr = ptr;
        }
        @Override
        public int hashCode() {
            return (int)ptr + 7*(int)(ptr>>32) + 31*desc.hashCode();
        }
    }

    class StructKey extends KeyBase {
	    public StructKey(Object parent, long ptr, cClassDescriptor desc) {
	        super(parent, ptr, desc);
	    }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GroupKey other = (GroupKey)obj;
            return ptr == other.ptr && desc.equals(other.desc);
        }
	}
	class GroupKey extends KeyBase {
        String groupName;

        public GroupKey(Object parent, long ptr, cClassDescriptor desc, String groupName) {
            super(parent, ptr, desc);
            this.groupName = groupName;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GroupKey other = (GroupKey)obj;
            return ptr == other.ptr && desc.equals(other.desc) && groupName.equals(other.groupName);
        }
        @Override
        public int hashCode() {
            return super.hashCode() + 47*groupName.hashCode();
        }
	}
	class FieldKey extends KeyBase {
	    int fieldID;

	    public FieldKey(Object parent, long ptr, cClassDescriptor desc, int fieldID) {
            super(parent, ptr, desc);
	        this.fieldID = fieldID;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
              if (obj == null)
                return false;
              if (getClass() != obj.getClass())
                return false;
            FieldKey other = (FieldKey)obj;
            return ptr == other.ptr && desc.equals(other.desc) && fieldID == other.fieldID;
        }
        @Override
        public int hashCode() {
            return super.hashCode() + 47*fieldID;
        }
	}
	class ArrayElementKey extends KeyBase {
	    int fieldID;
	    int index;

	    public ArrayElementKey(Object parent, long ptr, cClassDescriptor desc, int fieldID, int index) {
            super(parent, ptr, desc);
	        this.fieldID = fieldID; this.index = index;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
              if (obj == null)
                return false;
              if (getClass() != obj.getClass())
                return false;
            ArrayElementKey other = (ArrayElementKey)obj;
            return ptr == other.ptr && desc.equals(other.desc) && fieldID == other.fieldID && index == other.index;
        }
        @Override
        public int hashCode() {
            return super.hashCode() + 47*fieldID + 113*index;
        }
	}
	
	class ViewContentProvider implements ITreeContentProvider {
	    public Object[] getChildren(Object element) {
	        if (element instanceof Object[]) {
	            return (Object[])element;
	        }
	        else if (element instanceof cObject) {
	            cObject object = (cObject)element;
	            long ptr = cClassDescriptor.getCPtr(object);
                cClassDescriptor desc = cClassDescriptor.getDescriptorFor(object);
                if (desc == null) {
                    return object.getChildObjects();
                } else {
                    //Object[] allFields = getFieldsInGroup(ptr, desc, null); -- use this to present all fields at once (without groups)
                    Object[] ungroupedFields = getFieldsInGroup(element, ptr, desc, "");
                    Object[] groups = getGroupKeys(element, ptr, desc);
                    Object[] childObjects = object.getChildObjects(); //FIXME needed?
                    return ArrayUtils.addAll(ArrayUtils.addAll(ungroupedFields, groups), childObjects);
                }
	        }
            else if (element instanceof StructKey) {
                StructKey key = (StructKey)element;
                Object[] ungroupedFields = getFieldsInGroup(element, key.ptr, key.desc, "");
                Object[] groups = getGroupKeys(element, key.ptr, key.desc);
                return ArrayUtils.addAll(ungroupedFields, groups);
            }
            else if (element instanceof GroupKey) {
                GroupKey key = (GroupKey)element;
                return getFieldsInGroup(element, key.ptr, key.desc, key.groupName);
            }
            else if (element instanceof FieldKey) {
                FieldKey key = (FieldKey)element;
                boolean isArray = key.desc.getFieldIsArray(key.ptr, key.fieldID);
                if (isArray)
                    return getElementsInArray(element, key.ptr, key.desc, key.fieldID);
                boolean isCompound = key.desc.getFieldIsCompound(key.ptr, key.fieldID);
                if (isCompound)
                    return getFieldsOfCompoundField(element, key.ptr, key.desc, key.fieldID, -1);
            }
            else if (element instanceof ArrayElementKey) {
                ArrayElementKey key = (ArrayElementKey)element;
                return getFieldsOfCompoundField(element, key.ptr, key.desc, key.fieldID, key.index);
            }
	        return new Object[0];
	    }

        private Object[] getFieldsOfCompoundField(Object parent, long ptr, cClassDescriptor desc, int fieldID, int index) {
            // return children of this class/struct
            long fieldPtr = desc.getFieldStructPointer(ptr, fieldID, index);
            if (fieldPtr == 0)
                return new Object[0];
            boolean isCObject = desc.getFieldIsCPolymorphic(ptr, fieldID);
            if (isCObject) {
                return getChildren(desc.getFieldAsCObject(ptr, fieldID, index));
            } else {
                String fieldStructName = desc.getFieldStructName(ptr, fieldID);
                cClassDescriptor fieldDesc = cClassDescriptor.getDescriptorFor(fieldStructName);
                if (fieldDesc == null)
                    return new Object[0]; // nothing known about it
                return getChildren(new StructKey(parent, fieldPtr, fieldDesc));
            }
        }

	    protected Object[] getElementsInArray(Object parent, long ptr, cClassDescriptor desc, int fieldID) {
	        int n = desc.getArraySize(ptr, fieldID);
	        Object[] result = new Object[n];
	        for (int i=0; i<n; i++)
	            result[i] = new ArrayElementKey(parent, ptr, desc, fieldID, i);
            return result;
        }

        public Object[] getElements(Object inputElement) {
	        return getChildren(inputElement);
	    }

	    public Object getParent(Object element) {
	        if (element instanceof cObject)
	            return ((cObject)element).getOwner();
            if (element instanceof KeyBase)
                return ((KeyBase)element).parent;
	        return null;
	    }

	    public boolean hasChildren(Object element) {
	        if (element instanceof GroupKey)
	            return true;   
	        else if (element instanceof cObject)
	            return ((cObject)element).hasChildObjects();
	        else
	            return getChildren(element).length!=0; //FIXME make it more efficient (this counts all children!)
	    }

	    protected Object[] getGroupKeys(Object parent, long ptr, cClassDescriptor desc) {
	        // collect unique group names
	        int numFields = desc.getFieldCount(ptr);
	        Object[] groupNames = new Object[0];
	        for (int i=0; i<numFields; i++) {
	            String groupName = desc.getFieldProperty(ptr, i, "group");
	            if (groupName != null && !ArrayUtils.contains(groupNames, groupName))
	                groupNames = ArrayUtils.add(groupNames, groupName);
	        }

	        // convert to GroupKey[] in-place
	        Object[] result = groupNames;
	        for (int i=0; i<result.length; i++)
	            result[i] = new GroupKey(parent, ptr, desc, (String)result[i]);
	        return result;
	        
	    }

	    protected Object[] getFieldsInGroup(Object parent, long ptr, cClassDescriptor desc, String groupName) {
            int numFields = desc.getFieldCount(ptr);
            Object[] fieldKeys = new Object[numFields]; // upper bound
            int numFieldKeys = 0;
            for (int i=0; i<numFields; i++) {
                String fieldGroupName = desc.getFieldProperty(ptr, i, "group");
                if (groupName==null || groupName.equals(fieldGroupName))
                    fieldKeys[numFieldKeys++] = new FieldKey(parent, ptr, desc, i);
            }
            return ArrayUtils.subarray(fieldKeys, 0, numFieldKeys);
	    }
	    
	    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	        // Do nothing
	    }

	    public void dispose() {
            // Do nothing
        }
	}

	class ViewLabelProvider implements IStyledLabelProvider {
        private class ColorStyler extends Styler {
            Color color;
            public ColorStyler(Color color) { this.color = color; }
            @Override public void applyStyles(TextStyle textStyle) { textStyle.foreground = color; }
        };
	    private Styler blueStyle = new ColorStyler(ColorFactory.BLUE3);
	    private Styler greyStyle = new ColorStyler(ColorFactory.GREY60);

        public String getText(Object element) {
            //note: we use "\b...\b" for blue, and "\f" for grey coloring
	        if (element instanceof cObject) {
	            cObject obj = (cObject) element;
	            String typeName = obj.getClassName();  //XXX use opp_getobjectshorttypename
	            return obj.getFullName() + " \f(" + typeName + ")";
	        }
	        else if (element instanceof StructKey) {
	            StructKey key = (StructKey)element;
                return key.desc.getName();
	        }
            else if (element instanceof GroupKey) {
                GroupKey key = (GroupKey)element;
                return key.groupName;
            }
            else if (element instanceof FieldKey) {
                FieldKey key = (FieldKey)element;
                return getFieldText(key.ptr, key.desc, key.fieldID, -1);
            }
            else if (element instanceof ArrayElementKey) {
                ArrayElementKey key = (ArrayElementKey)element;
                return getFieldText(key.ptr, key.desc, key.fieldID, key.index);
            }
	        return element.toString();
	    }
	    
        @Override
		public Image getImage(Object element) {
            if (element instanceof cObject) {
                //FIXME cache image by object's classname!
                cObject object = (cObject)element;
                cClassDescriptor desc = cClassDescriptor.getDescriptorFor(object);
                String icon = desc.getProperty("icon");
                if (!StringUtils.isEmpty(icon)) {
                    return Activator.getCachedImage("icons/obj16/"+icon+".png");
                }
            }
            else if (element instanceof StructKey) {
                return Activator.getCachedImage("icons/obj16/field.png");
            }
            else if (element instanceof GroupKey) {
                return Activator.getCachedImage("icons/obj16/fieldgroup.png");
            }
            else if (element instanceof FieldKey) {
                FieldKey key = (FieldKey)element;
                boolean isObject = key.desc.getFieldIsCObject(key.ptr, key.fieldID);
                if (isObject)
                    return getImage(key.desc.getFieldAsCObject(key.ptr, key.fieldID, -1));
                else
                    return Activator.getCachedImage("icons/obj16/field.png");
            }
            else if (element instanceof ArrayElementKey) {
                ArrayElementKey key = (ArrayElementKey)element;
                boolean isObject = key.desc.getFieldIsCObject(key.ptr, key.fieldID);
                if (isObject)
                    return getImage(key.desc.getFieldAsCObject(key.ptr, key.fieldID, key.index));
                else
                    return Activator.getCachedImage("icons/obj16/field.png");
            }
            
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
		}
        
        public String getFieldText(long object, cClassDescriptor desc, int field, int index) {
            String typeName = desc.getFieldTypeString(object, field);
            boolean isArray = desc.getFieldIsArray(object, field);
            boolean isCompound = desc.getFieldIsCompound(object, field);
            boolean isPoly = desc.getFieldIsCPolymorphic(object, field);
            boolean isObject = desc.getFieldIsCObject(object, field);
            boolean isEditable = desc.getFieldIsEditable(object, field);
        
            // field name can be overridden with @label property
            String name = desc.getFieldProperty(object, field, "label");
            if (StringUtils.isEmpty(name))
                name = desc.getFieldName(object, field);
        
            // if it's an unexpanded array, return "name[size]" immediately
            if (isArray && index == -1) {
                int size = desc.getArraySize(object, field);
                return name + "[" + size + "] \f(" + typeName + ")";
            }
        
            // when showing array elements, omit name and just show "[index]" instead
            if (index != -1)
                name = "[" + index + "]";
        
            // we'll want to print the field type, except for expanded array elements
            // (no need to repeat it, as it's printed in the "name[size]" node already)
            String typeNameText = (index == -1) ? " \f(" + typeName + ")" : "";
        
            // "editable" flag
            if (isEditable)
                typeNameText = " [...] " + typeNameText;
        
            if (isCompound) {
                // if it's an object, try to say something about it...
                if (isPoly) {
                    cObject fieldObj = desc.getFieldAsCObject(object, field, index);
                    if (fieldObj == null)
                        return name + " = NULL" + typeNameText;
                    String fieldObjName;
                    if (!isObject || cClassDescriptor.getCPtr(fieldObj.getOwner()) == object)
                        fieldObjName = fieldObj.getFullName();
                    else
                        fieldObjName = fieldObj.getFullPath();
                    String className = fieldObj.getClassName(); //FIXME use shorttypename!!!
                    String info = fieldObj.info();
                    String infoText = info.equals("") ? "" : ": " + info;
                    return name + " = " + "(" + className + ") " + fieldObjName + infoText + typeNameText;
                } 
                else {
                    // a value was generated via operator<<
                    String value = desc.getFieldAsString(object, field, index);
                    if (value.equals(""))
                        return name + typeNameText;
                    else
                        return name + " = \b" + value + "\b" + typeNameText;
                }
            } else {
                // plain field, return "name = value" text
                String value = desc.getFieldAsString(object, field, index);
                if (typeName.equals("string")) 
                    value = "'" + value + "'";

                String enumName = desc.getFieldProperty(object, field, "enum");
                if (!StringUtils.isEmpty(enumName)) {
                    typeNameText = typeNameText + " - enum " + enumName;
                    cEnum enumDef = cEnum.find(enumName);
                    if (enumDef != null) {
                        try {
                            String symbolicName = enumDef.getStringFor(Integer.parseInt(value));
                            value = StringUtils.defaultIfEmpty(symbolicName, "???") + " (" + value + ")";
                        } 
                        catch (NumberFormatException e) { }
                    }
                }

                if (value.equals(""))
                    return name + typeNameText;
                else
                    return name + " = " + "\b" + value + "\b" + typeNameText;
            }
        }

        @Override
        public StyledString getStyledText(Object element) {
            String text = getText(element);
            int blueStartIndex = text.indexOf('\b', 0);
            int blueLength = blueStartIndex==-1 ? -1 : text.indexOf('\b', blueStartIndex+1)-blueStartIndex-1;
            if (blueLength > 0)
                text = text.replace("\b", "");
            int greyStartIndex = text.indexOf('\f');
            if (greyStartIndex != -1)
                text = text.replace("\f", "");

            StyledString styledString = new StyledString(text);
            if (greyStartIndex >= 0)
                styledString.setStyle(greyStartIndex, text.length()-greyStartIndex, greyStyle);
            if (blueLength > 0)
                styledString.setStyle(blueStartIndex, blueLength, blueStyle);
            return styledString;
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return true;
        }

        @Override
        public void dispose() {
            // nothing
        }

        @Override
        public void addListener(ILabelProviderListener listener) {
            // nothing
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
            // nothing
        }
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
    @Override
    protected Control createViewControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new DecoratingStyledCellLabelProvider(new ViewLabelProvider(), null, null));
		viewer.setInput(cSimulation.getActiveSimulation());
        Activator.getSimulationManager().addChangeListener(this);
        
        // create context menu
        getViewSite().registerContextMenu(contextMenuManager, viewer);
        viewer.getTree().setMenu(contextMenuManager.createContextMenu(viewer.getTree()));
        
        createActions();
        
        // add double-click support
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                Object element = ((IStructuredSelection)event.getSelection()).getFirstElement();
                if (element instanceof cObject)
                    openInspector((cObject)element);
            }
        });
        
        return viewer.getTree();
	}

    protected void openInspector(cObject element) {
        if (cModule.cast(element) != null) {
            cModule module = cModule.cast(element);
            Activator.openEditor(new ModuleIDEditorInput(module.getId()), ModelCanvas.EDITOR_ID);
        }
        //XXX open other types of objects too (use inspector framework)
    }

    protected void createActions() {
        IAction pinAction = getOrCreatePinAction();

        contextMenuManager.add(pinAction); //TODO expand context menu: Copy, etc.
        
        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        toolBarManager.add(pinAction);
    
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
        menuManager.add(pinAction);
    }

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
    public void changed() {
        viewer.refresh();
    }

    @Override
    public void dispose() {
        Activator.getSimulationManager().removeChangeListener(this);
        super.dispose();
    }

    @Override
    protected void rebuildContent() {
        //XXX need something more general than GraphicalModulePart!
        List<Object> input = new ArrayList<Object>();
        ISelection selection = getAssociatedEditorSelection();
        if (selection instanceof IStructuredSelection) {
            Object[] sel = ((IStructuredSelection)selection).toArray();
            for (Object o : sel) {
                if (o instanceof GraphicalModulePart) {
                    GraphicalModulePart part = (GraphicalModulePart)o;
                    cModule module = cSimulation.getActiveSimulation().getModule(part.getModuleID());
                    if (module != null)
                        input.add(module);
                }
            }
        }

        if (input.isEmpty()) {
            cModule systemModule = cSimulation.getActiveSimulation().getSystemModule();
            if (systemModule != null)
                input.add(systemModule);
        }

        Object[] array = input.toArray();
        Object[] expandedElements = viewer.getExpandedElements();
        viewer.setInput(array);
        viewer.setExpandedElements(expandedElements);
    }
}
