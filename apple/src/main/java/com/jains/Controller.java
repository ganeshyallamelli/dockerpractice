package com.jains;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    
    private static Map<String, String> fruits = new HashMap<>();
    
    static {
        fruits.put("1", "apple");
        fruits.put("2", "banana");
        fruits.put("3", "mango");
    }
    
    @GetMapping("/fruits")
    public Map<String, String> getAllFruits(){
        return fruits;
    }
    
    @GetMapping("/fruits/{id}")
    public String getFruit(@PathVariable String id){
        return fruits.getOrDefault(id, "Fruit not found");
    }
    
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "World") String name){
        return "Hello " + name + "!";
    }
    
    @PostMapping("/fruits")
    public Map<String, String> addFruit(@RequestBody Map<String, String> input){
        String id = input.get("id");
        String name = input.get("name");
        if (id != null && name != null) {
            fruits.put(id, name);
        }
        return fruits;
    }
    
    @PutMapping("/fruits/{id}")
    public String updateFruit(@PathVariable String id, @RequestBody Map<String, String> input){
        String name = input.get("name");
        if (name != null) {
            fruits.put(id, name);
            return "Updated fruit " + id;
        }
        return "Name not provided";
    }
    
    @DeleteMapping("/fruits/{id}")
    public String deleteFruit(@PathVariable String id){
        if (fruits.containsKey(id)) {
            fruits.remove(id);
            return "Deleted fruit " + id;
        }
        return "Fruit not found";
    }
    
    // Legacy methods for compatibility
    @GetMapping
    public Map<String, String> get(){
        return fruits;
    }
    
    @PostMapping
    public Map<String, String> post(@RequestBody Map<String, String> input){
        return addFruit(input);
    }
    
}
