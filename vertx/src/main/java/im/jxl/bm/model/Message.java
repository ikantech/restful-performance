package im.jxl.bm.model;

import io.vertx.core.json.JsonObject;

public class Message extends JsonObject {
    public String getMessage() {
        return getString("message");
    }

    public void setMessage(String message) {
        put("message", message);
    }
}
