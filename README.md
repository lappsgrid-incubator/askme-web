# Web

Front end for the AskMe service

## HTML Templates

In a Spring Boot web application a controller method returns a *String* which is the name of a template that will be used to generate the response.

``` 
@GetMapping(path="/example", produces "text/html")
String getExample() {
    return "hello_world"
}
```
The original Eager *AskMe* application uses Spring Boot