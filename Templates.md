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
This makes passing a closure as a parameter easier to read.
```groovy
void html(Closure cl) {
    print "<html>"
    // Call the closure
    cl()
    print "</html>"
}

void p(String body) {
    print "<p>$body</p>"
}

html {
    p "Hello world"
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

Groovy templates can be very powerful since the full expressiveness of the Groovy language can be using including flow of control and function calls.  It is recommended to not get carried away with programming in a template and try to perform most of the logic in the controller rather than the template.

```groovy
html {
    head {
        title "Table example"        
    }
    body {
        // Assume the controller injected a Map named 'values' for us.
        h1 "The Values"
        table {
            th {  // Table header row
                td 'Key'
                td 'Value'
            }
            // Iterate over the table and print the key/value pairs
            values.sort().each { key,value ->
                tr {
                    td key
                    td value
                }                    
            }
        }      
    }
}
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
The ability to intercept unknown methods allows us to create a markup builder that can handle any  markup without any previous knowledge of the syntax.

## Spring Boot

In a Spring Boot web application a controller method returns a *String* which is the name of a template that will be used to generate the response.

```groovy
@GetMapping(path="/example", produces = "text/html")
String getExample() {
    return "index"
}
```
Here Spring Boot will look for a template named *index.tpl* from the `src/main/resources/template` folder and use it to generate the web page. The example *index.tpl* contains the following:
```groovy
layout 'layouts/main.gsp',
title: 'Example Template',
content: {
    form(action:'formAction', method:'POST') {
        input(type:'text', name: 'question', id:'question', placeholder:'Ask me a question.', required:'true', '')
        input(type:'submit', value:'Ask', '')
    }
}
```
Recalling that this is an executable Groovy script we see that it simply calls the function `layout` and passes two parameters; the string *layouts/main.gsp* and a Map with two entries: *title* containing a String and *content* that contains a closure.

Spring Boot knows that the first String parameter *layouts/main.gsp* refers to another Groovy template that provides the layout for the page and Spring will make the Map parameters available as variables in the layout template.

In *layouts/main.gsp* we see the code to generate the HTML markup with the *title* parameter being used to set the page title.  On line 12 we see that the *content* closure is invoked to generate the main page content, which generates a form with two input fields.  We can pass as many parameters as we need as Map parameters to the layout template.

When the user clicks the *submit* button on the index page a **POST** message will be sent to the */formAction* endpoint (as specified in the form's *action* parameter) which is handled by the `handlePost` method in the `TemplateController` since it has been annotated with
```groovy
@PostMapping(path="/formAction", produces="text/html")
String handlePost() { }
```
The `handlePost` method sets an attribute in the `Model` object which are made available as variables to the *question* template.  If we look at *question.tpl* we see that it simply displays the text entered as the "question" inside a paragraph (&lt;p>) element.