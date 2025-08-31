# **Servant**

**Smart Home Assistant Platform**

Managing a household involves many small but important tasks: keeping your shopping list up to date, adjusting the heating system, maintaining control over your home calendar so you don't forget to pick up your child when they finish their music lessons, or monitoring some home automation events like temperatures, waterleaking sensors, devices in my wifi network or main door's status. Servant helps you take care of these recurring activities automatically and intelligently—so your home works with you, not against you.

**Servant is your home assistant platform to simplify and automate domestic tasks efficiently.**

## **Table of Contents**

- [Introduction](#introduction)
- [Key Features](#key-features)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Related Projects & Technologies](#related-projects--technologies)
- [License](#license)

     
## **Introduction**

Servant is a flexible and extensible home assistance platform designed to integrate seamlessly with sensors, actuators, and external systems. Unlike traditional solutions, Servant provides a customizable software architecture that adapts to your needs—whether it’s managing daily tasks, automating workflows, or enabling advanced AI-driven interactions.

## **Key Features**

**Modularity**

Servant is a composition of modules. Each module corresponds to a logical subsystem and includes a series of functionalities grouped into actions and events.

**AI Capabilities**

*MCP implementation*. Servant implements the [Java SDK MCP server API](https://github.com/modelcontextprotocol/java-sdk), providing several [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) tools. This toolkit allows the integration of Servant in any GenAI Agentic system. The current implementation allows interaction with the shopping list, querying the calendar to review future tasks, reviewing the temperature, and publishing notifications ([Telegram](https://core.telegram.org/bots/api) messages) to specific contexts. In future versions of Servant, new MCP tools will be provided with additional commands and actions.  

[Large Language Models (LLMs)](https://en.wikipedia.org/wiki/Large_language_model) offer a novel way to interact with your data. In addition to generating content from a given input, LLMs can be utilized to transform heterogeneous and irregular content into structured and more analyzable output.
This transformed content can then be used to build a knowledge data model for future activities.  

**Interaction**
Human or machine interactions with Servant are possible through the pre-built interfaces:

* Servant provides a [RESTful API](https://en.wikipedia.org/wiki/Representational_state_transfer) and integration to its messaging system through [WebSockets](https://en.wikipedia.org/wiki/WebSocket). You can use these interfaces to build your own data consumer or data tracker agents in your preferred programming language.

* Web interface that allows the creation of web applications.

* [Telegram](https://telegram.org/) chatbot as an asynchronous human/system interface. 

* [Google Calendar](https://calendar.google.com/) integration to schedule and run actions at specific times.
  
* [VoIP](https://github.com/ucpdh23/ServantPhone) interface which provides a conversational solution between the user and Servant.


**Extensibility**

Servant provides an extensible framework, implemented in [Java](https://www.oracle.com/java/) and [Kotlin](https://kotlinlang.org/), that allows the integration of new elements like sensors and actuators.

Integration of any kind of device into [Internet of Things (IoT)](https://en.wikipedia.org/wiki/Internet_of_things) technology is straightforward. Thanks to a distributed messaging system and Java, interfacing with any system to send or receive data from/to the cloud is never difficult. Integration with systems like [IFTTT](https://ifttt.com/) is supported.

Servant provides an [MQTT](https://mqtt.org/) interface to interact with sensors and external actuators. This solution is already integrated with a [Zigbee2MQTT](https://www.zigbee2mqtt.io/) solution to connect to other systems.  


**Smart**
* Implementation based on two basic concepts: events and actions. It makes it easy to create advanced algorithms for devices to interact together (sensors and/or actuators) or with external systems.

* Natural language processing for better human/system interaction.

* Scheduler for automatic actions and event triggering.

* Working on integration with [OpenAI](https://openai.com/) and a [Neo4j](https://neo4j.com/) knowledge database.

**Additional Functionalities**

* Includes integrations with [MongoDB](https://www.mongodb.com/) and [Neo4j](https://neo4j.com/)

* Easy JSON ↔ Java transformation.


**Implementation Details**

* To simplify development, a [Convention over Configuration](https://en.wikipedia.org/wiki/Convention_over_configuration) policy has been adopted. This project implements and abstracts some concepts, providing a mini-framework to simplify development:
       - Provides an almost 99% friendly object-oriented static way to map and use JSON data.
       - Improved communication system inside the [Vert.x](https://vertx.io/) nodes system using Java enum objects.
       - Toolkit to build state machines with a Java enums language.
       - Interaction between Java and Kotlin code.

* Asynchronous JVM project (Java & Kotlin) based on [Eclipse Vert.x](https://vertx.io/)

* Built-in facilities on top of [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus) to handle interaction with events in a controlled way. 

**Requirements**

* [Java 21](https://openjdk.org/projects/jdk/21/)
* Linux-based system

## **Getting Started**

1. Clone the repository
2. Ensure Java 21 is installed
3. Build the project using [Maven](https://maven.apache.org/):
   ```bash
   mvn compile
   ```
4. Configure your settings in the config file
5. Run the application

## **Related Projects & Technologies**

This project integrates with and builds upon several open-source technologies:

* **Messaging & Communication**: [Eclipse Vert.x](https://vertx.io/), [MQTT](https://mqtt.org/), [WebSockets](https://en.wikipedia.org/wiki/WebSocket)
* **Databases**: [MongoDB](https://www.mongodb.com/), [Neo4j](https://neo4j.com/), [SQLite](https://www.sqlite.org/)
* **AI & ML**: [OpenAI API](https://openai.com/), [Model Context Protocol](https://modelcontextprotocol.io/)
* **Smart Home**: [Zigbee2MQTT](https://www.zigbee2mqtt.io/), [MQTT](https://mqtt.org/)
* **External Services**: [Telegram Bot API](https://core.telegram.org/bots/api), [Google Calendar API](https://developers.google.com/calendar), [AWS SDK](https://aws.amazon.com/sdk-for-java/)
* **Build Tools**: [Apache Maven](https://maven.apache.org/)

## **License**

See the [LICENSE](LICENSE) file for details.
