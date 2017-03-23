package bekrina.whereismobile.util;

public abstract class Constants {
    public static final String GROUP_INFO_PREFERENCES = "groupInfo";
    public static final String GROUP_NAME = "groupName";
    public static final String GROUP_IDENTITY = "groupIdentity";
    public static final String NAME = "name";
    public static final String IDENTITY = "identity";
    private static final String API_URL = "https://rocky-river-45878.herokuapp.com/api";
    public static final String INITIAL_COOKIE_ENDPOINT = API_URL + "/config.json";
    public static final String LOGIN_ENDPOINT = API_URL + "/login";
    public static final String GROUP_ENDPOINT = API_URL + "/group";
    public static final String GET_GROUPS_ACTION = "/getforcurrentuser";
    public static final String INVITE_ACTION = "/invite";
    public static final String JOIN_ACTION = "/join";
    public static final String LEAVE_ACTION = "/leave";
    public static final String SAVE_LOCATION_ACTION = "/savemylocation";
    public static final String GET_LOCATIONS_ACTION = "/getlocations";
}
