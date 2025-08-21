# **Servant**

**My Smart Home Assistance Prototype**

Let me ask you some questions about your homeworks:
- How do you control your *shopping list* to avoid forgiving anything?
- Wouldn't you like to manage your heating system using your calendar scheduler?
- Wouldn't you like to monitor your indoor and outdoor temperature from anywhere?

**Servant is your home assistant platform to handle domestic and recurrent tasks efficiently.**
     
## **Introduction**

Servant is a different home assistance implementation to provide a software architecture with capabilities to integrate sensors, transformers, and actuators. This customizable implementation supports new integrations as well as new workflows for your personal data. 

See bellow some key features of Servant:

**Modularity**

Servant is a composition of modules. Each module corresponds to a logical subsystem and includes a series of functionalities grouped into actions and events.

**IA Capabilities**

*MCP implementation*. Servant supports the MCP protocol implementing several MCP tools. This toolkit provide integration of Servant in any agentic solution. With these tools one external LLM Agent can interact with the shopping list, query the calendar to review future tasks, and review the temperature. In future versions new tool will be created in order to provide new possibilities.  

LLMs offer a novel way to interact with your data. In addition to generating content from a given input, LLMs can be utilized to transform heterogeneous and irregular content into a structured and more analyzable output.
Then, this transformed content can be used to build a knowledge data model for future activities.  

**Interaction**
Human or machine interactions with Servant are possible thanks to the pre built-in interfaces:

* Servant provides a restful api and integration to its messaging system through websockets. You can use these interfaces and build your own data consumer or data tracker agents in your preferred programming language.

* Web interface that allows the creation of a web application.

* Telegram chatbot as an asynchronous human/system interface. 

* Google Calendar integration in order to schedule and run actions in specific time.


**Extensibility**

Servant provides an extensible framework, implemented in Java and Kotlin, that allows the integration of new elements like sensors and actuators.

Integration of any kind of device into the Internet of Things technology is easy. Thanks to a distributed messaging system and Java, interfacing with any system in order to send or receive data from/to the cloud never will be a hard work. Integration with systems like IFTTT 

Servant provides a MQTT interface to interacts with sensors and external actuators. This solution is already inetgrated with a zigbee2mqtt solution in order to connect to other systems.  


**Smart**
* Implementation based on two basic concepts of events and actions. It is easy to create advanced algorithms interacting devices together (sensors and/or actuators) or with external systems.

* Natural language processing in order to a better human/system interaction.

* Scheduler for automatic actions and events triggering.

* Working into the integration with openIA and a Neo4J knowledge database

**Extra functionalities**

* Include integrations with mongodb and neo4j 

* Easy json <=> java transformation.


**Implementation Details**

* In order to simplify development, a [Conventions over configurations](https://en.wikipedia.org/wiki/Convention_over_configuration) policy has been adopted. This project implement abstract some concepts, providing a mini-framework to simplify development:
       - Providing almost a 99% friendly object-oriented static way to map and use json data.
       - Improved communication system inside the vertx nodes system thanks to java enum objects.
       - Toolkit to build state machines with a java enums language.
       - Interaction between java with kotlin code.

* Asynchronous JVM project (Java & Kotlin) based on Vertx

* Built-in facilities on top of Vertx event bus to handle the interaction with events in a controlled way. 

**Requirements**

* Java 21
* Linux based system
