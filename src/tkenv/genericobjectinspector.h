//==========================================================================
//  GENERICOBJECTINSPECTOR.H - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_GENERICOBJECTINSPECTOR_H
#define __OMNETPP_GENERICOBJECTINSPECTOR_H

#include "inspector.h"

NAMESPACE_BEGIN
namespace tkenv {


class TKENV_API GenericObjectInspector : public Inspector
{
   public:
      GenericObjectInspector(InspectorFactory *f);
      ~GenericObjectInspector();
      virtual void doSetObject(cObject *obj) override;
      virtual void createWindow(const char *window, const char *geometry) override;
      virtual void useWindow(const char *window) override;
      virtual void refresh() override;
      virtual void commit() override;
      virtual int inspectorCommand(int argc, const char **argv) override;
};

} // namespace tkenv
NAMESPACE_END

#endif



