#include <omnetpp.h>

USING_NAMESPACE

class Controller : public cSimpleModule
{
  private:
    int k;
  public:
    virtual void initialize();
    virtual void handleMessage(cMessage *msg);
};

Define_Module(Controller);

void Controller::initialize()
{
    k = 0;
    scheduleAt(0, new cMessage());
}

void Controller::handleMessage(cMessage *msg)
{
    // delete some modules
    double p = par("deleteProbability");
    for (cModule::SubmoduleIterator it(getSimulation()->getSystemModule()); !it.end(); /**/) {
        cModule *mod = it++;
        if (strncmp(mod->getName(), "node-", 5)==0 && dblrand() < p)
            mod->deleteModule();
    }

    // and create new ones
    int n = par("numToCreate");
    cModuleType *type = cModuleType::find("Node");
    for (int i = 0; i < n; i++) {
        char name[20];
        sprintf(name, "node-%d", ++k);
        type->createScheduleInit(name, getSimulation()->getSystemModule());
    }
    bubble("created a bunch of modules");

    scheduleAt(simTime()+par("waitTime").doubleValue(), msg);
}

