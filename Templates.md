# HTML Templates

Spring Boot supports four template engines that can be used to generate the HTML views.

1. [FreeMarker](https://freemarker.apache.org/docs/)
1. [Thymeleaf](http://www.thymeleaf.org/)
1. [Mustache](https://mustache.github.io/)
1. [Groovy](http://docs.groovy-lang.org/docs/next/html/documentation/template-engines.html#_the_markuptemplateengine)

[JSP](https://www.oracle.com/technetwork/java/index-jsp-138231.html) (JavaServer Pages) can also bu used with Spring Boot, but it is not recommended.

Since AskMe is a Groovy application it makes sense to use Groovy as the template engine.

## Groovy Templates

A Groovy template is just a Groovy script that generates an HTML page. If you are new to Groovy syntax there are a number of syntax details to keep in mind:

1\. Groovy does not require parenthesis around the parameters to a method call.
```groovy
println "Hello world"

int add(int a, int b) { return a+b }
int i = add 1, 2
print i
```
This makes passing a closure as a parameter easier to read"
```groovy
void html(Closure cl) {
    print "<html>"
    // Call the closure
    cl()
    print "</html>"
}
```

2\. Parameters specified as `key:value` are collected into a `Map` before being passed to the method call.
```groovy
void function(Map map) {
    map.each { key,value -> println "$key=$value" }
}
function key1:'value1', key2:'value2'
```
The `org.lappsgrid.askme.web.examples.Templates` class has a simple example with methods corresponding to HTML element names.  The Groovy syntax allows for a HTML markup builder with a fluent API, that is, the code used to generate the HTML closely resembles the HTML itself.
```groovy
html {
    head {
        title "Example HTML Page"
    }
    body {
        div(class:'section', width:'100%') {
            h1 "Example"
            p "This is some text"
        }
        div(class:'footer') {
            p(class:'copyright', "Copyright 2019")
        }
    }
}
```
**Note** When calling a method that takes both Map parameters and a Closure we must either:
1. Remember to specify a comma after the last Map parameter, or
1. Enclose the Map parameters in parenthesis
```groovy
void div(Map attributes, Closure cl) { ... }

div(width:"100%") { println "hello world" }
div width:"100%", { println "hello world" }
```

## MetaProgramming

When a method is called in Java the JVM will look in the object instance for a method that matches the signature. If no matching method is found Java will look in the object's superclass, and so on up the inheritance chain until either a method with a matching signature is found or `java.lang.Object` is reached. [Groovy extends this](http://groovy-lang.org/metaprogramming.html) by adding a `MetaClass` to each object.  When Groovy needs to dispatch a method and the method is not defined in the instance Groovy will look for the method in the `MetaClass` before looking up the inhertiance chain.  The main difference between the `MetaClass` and a super class is the `MetaClass` can be manipulated at runtime.

``` 
String.metaClass.greet = { println "Hello $delegate" }
String s = "world"
s.greet()
```
It is also possible to use Groovy's `ExpandoMetaClass` to provide a custom `MetaClass` with the implementations of additional methods. See the `org.lappsgrid.askme.web.examples.MetaProgramming` class for examples.


## Dynamic Builders

Another powerful feature of Groovy's `MetaClass` is the ability to intercept and handle calls to unknown methods.
```groovy
String.metaClass.methodMissing = { name, args ->
    if (name == 'greet') {
        println "Hello ${delegate}."
    }
    else {
        println "Intercepted missing method $name"
    }
}
String s = "world"
s.greet()
s.foobar()
```
The ability to intercept unknown methods allows us to create a markup builder for arbitrary
## Spring Boot

In a Spring Boot web application a controller method returns a *String* which is the name of a template that will be used to generate the response.

```groovy
@GetMapping(path="/example", produces = "text/html")
String getExample() {
    return "hello_world"
}
```
The original Eager *AskMe* application uses Spring Boot