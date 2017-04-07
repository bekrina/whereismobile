package bekrina.whereismobile.listeners;


import bekrina.whereismobile.model.Group;

public interface GroupStatusListener {
    void onUserHasGroup(Group group);
    void onUserWithoutGroup();
}
