# **Servant**

**My Smart Home System**

Let me ask you some home management questions:
- How do you manage your Shopping list to avoid forgiving anything?
- Don't you like to manage your heating system from your google calendar?
- Don't you like to monitor the home and outdoor temperature from everywhere?
- Would you like a DIY security system for your home?
- What about tracking your utilities bills?


**Servant is a home assistant platform to handle various domestic and recurrent activities efficiently.**
     

## **Introduction**

Servant aims to provide a software architecture with capacity to integrate various sensors, transformers, and actuators into a single platform. 

See bellow some key features of Servant:

**IA Capabilities**

LLMs offer a novel way to interact with your data. In addition to generating content from a given input, LLMs can be utilized to transform heterogeneous and irregular content into a structured and more analyzable output.
Then, this transformed content can be used to build a knowledge data model for future activities.  


**Interaction**
Human or machine interactions with Servant are possible thanks to the pre built-in interfaces:

* Servant provides a restful api and integration to its messaging system through websockets. You can use these interfaces and build your own data consumer or data tracker agents in your preferred programming language.

* Web interface that allows the creation of a web application.

* Includes a Telegram chatbot as a human/system interface. 

* Google Calendar integration in order to schedule and run operations.


**Extensibility**

Servant provides an extensible framework, implemented in Java and Kotlin, that allows the integration of new elements like sensors and actuators.

Integration of any kind of device into the Internet of Things technology is easy. Thanks to a distributed messaging system and Java, interfacing with any system in order to send or receive data from/to the cloud never will be a hard work. Integration with systems like IFTTT 



**Smart**
* Based on a events and actions systems. It is easy to create advanced algorithms interacting devices together (sensors and/or actuators) or with external systems.

* Natural language processing in order to a better human/system interaction.

* Scheduler for automatic actions and events triggering.

* Working into the integration with openIA and a Neo4J knowledge database

**Extra functionalities**

* Include integrations with mongodb and neo4j 

* Easy json <=> java transformation.


**Implementation Details**

* In order to simplify development, a [Conventions over configurations](https://en.wikipedia.org/wiki/Convention_over_configuration) policy has been adopted. This project implement abstract some concepts, providing a mini-framework to simplify development:
       - Providing almost a 99% friendly object-oriented static way to map and use json data.
       - improved communication system inside the vertx nodes system thanks to java enum objects.
       - Toolkit to build state machines with a java enums language.
       - Interaction between java with kotlin code.

* Asynchronous JVM project (Java & Kotlin) based on Vertx

* Built-in facilities on top of Vertx event bus to handle the interaction with events in a controlled way. 

* SMTP bridge to interact with external systems.

**Requirements**

* Java 21