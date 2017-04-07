package bekrina.whereismobile.model;

import com.google.gson.annotations.SerializedName;

public class Group {
    @SerializedName("name")
    private String name;
    @SerializedName("identity")
    private String identity;

    public Group(String name, String identity) {
        this.name = name;
        this.identity = identity;
    }

    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
