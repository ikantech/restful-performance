package im.jxl.bm;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kay on 19-3-27.
 */
@RestController
public class HelloController {

    @RequestMapping("/plaintext")
    public String plaintext() {
        return "Hello World";
    }

    @RequestMapping("/json")
    public Map json() {
        Map map = new HashMap();
        map.put("id", 1);
        map.put("name", "Hello World");
        return map;
    }
}
