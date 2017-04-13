package bekrina.whereismobile.util;

public interface Constants {
    String GROUP_INFO_PREFERENCES = "groupInfo";
    String GROUP = "group";
    String GROUP_NAME = "groupName";
    String GROUP_IDENTITY = "groupIdentity";
    String NAME = "name";
    String IDENTITY = "identity";
    String LAT = "latitude";
    String LNG = "longitude";
    String EMAIL = "email";
    String USER = "user";
    String FIRST_NAME = "firstName";
    String LAST_NAME = "lastName";

    double OFFSET = 0.0002;
    int LOCATION_INTERVAL = 10000;
    int LOCATION_FASTEST_INTERVAL = 5000;

    String API_URL = "https://rocky-river-45878.herokuapp.com/api";
    String INITIAL_COOKIE_ENDPOINT = API_URL + "/config.json";
    String LOGIN_ENDPOINT = API_URL + "/login";
    String GROUP_ENDPOINT = API_URL + "/group";
    String GET_GROUPS_ACTION = "/getforcurrentuser";
    String INVITE_ACTION = "/invite";
    String JOIN_ACTION = "/join";
    String LEAVE_ACTION = "/leave";
    String SAVE_LOCATION_ACTION = "/savemylocation";
    String GET_LOCATIONS_ACTION = "/getlocations";

    int PERMISSIONS_REQUEST_FINE_LOCATION = 123;
}
