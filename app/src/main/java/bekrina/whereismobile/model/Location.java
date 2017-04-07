package bekrina.whereismobile.model;

import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

public class Location {
    @SerializedName("id")
    private int id;
    @SerializedName("latitude")
    private double latitude;
    @SerializedName("longitude")
    private double longitude;
    @SerializedName("timestamp")
    private Timestamp timestamp;
    @SerializedName("user")
    private User user;

    public Location(double latitude, double longitude, Timestamp timestamp, User user) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.user = user;
    }

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }


    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
