package in.space.rs;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ImageController {

    @GetMapping
    public Map<String, String> getmap(){
        Map<String, String> result = new HashMap<>();
        result.put("1", "Jarvis");
        result.put("2", "Friday");
        return result;
    }
}
