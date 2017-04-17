package bekrina.whereismobile.listeners;

public interface JoinedToGroupListener {
    void onJoined();
    void onJoinFailed(int statusCode);
}
