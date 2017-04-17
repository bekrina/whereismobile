package bekrina.whereismobile.listeners;


public interface InviteStatusListener {
    void onInviteSent();
    void onInviteFailed(int statusCode);
}