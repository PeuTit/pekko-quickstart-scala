# Learn the actor system by doing: Car Simulation project

## Step by Step process

1. Define the problem statement and objectives: Clearly define the problem you are trying to solve and the objectives of the simulation. This will help guide the rest of the development process.
2. Choose the right tools and technologies: Select the appropriate programming languages, frameworks, and libraries for your project. Make sure they are well-suited for the type of simulation you want to create and the resources available to you.
3. Design the system architecture: Create a high-level design of the system, including the embedded device, central server, and any other components. This will help ensure that the system is scalable and can handle large amounts of data.
4. Implement the simulation: Write the code for the embedded device and central server using the chosen programming languages. Make sure to test and debug your code regularly to catch any errors or bugs early on.
5. Test and evaluate the results: Once the system is implemented, test it thoroughly to ensure that it meets the objectives of the simulation. Analyze the data generated by the simulation and make adjustments as needed.
6. Continuously improve the system: Use feedback from users and testing to continuously improve the system. This will help you refine your design and ensure that the system is reliable and scalable in the long term.

## Can you explain to me the actor system like I'm five years old

Actor systems are a way for people, animals, and even things to communicate with each other. They use special words or messages
that they send out into the world. Other people or animals who can hear these words can then understand what they want to say and
do something in response. It's like sending a message through a secret code so that only the right person can decipher it.

Imagine you are playing a game of hide-and-seek with your friends. You all agree on a special word or phrase that you will use to
signal each other when it's time to come out and play. If someone else hears this code, they know that it's time for them to join
in the game too.

In the same way, actor systems use special words or phrases to send messages to other people or animals. They can be very simple,
like "Hello!" or "Good morning," or they can be more complex, like "I need help with my homework." Other people who are able to
understand these codes can then respond by sending their own messages back, like "Hi, how are you?" or "Sure, I'll help you with
your homework."

Actor systems are used in many different ways, from communication between people and animals to control of machines and robots.
They can be very useful for making sure that everyone is on the same page and working together towards a common goal.


## Tutorial

I followed the [Pekko Quick start guide](https://pekko.apache.org/docs/pekko/current/typed/guide/tutorial.html)

This tutorial introduces and illustrates:

. The actor hierarchy and how it influences actor behaviour
. How to choose the right granularity for actors
. How to define protocols as messages
. Typical conversational styles

After finishing the first section, I should be able to comeback here and answer those questions!

Let’s get started by learning more about actors.

Some Keywords and Concepts that I think are important:

- Behavior
- AbstractBehavior
- Signal
- Message
- ActorRef
- PartialFunction
- ActorSystem

### Message delivery

The delivery semantics provided by messaging subsystems typically fall into the following categories:

At-most-once delivery — each message is delivered zero or one time; in more causal terms it means that messages can be lost, but are never duplicated.
At-least-once delivery — potentially multiple attempts are made to deliver each message, until at least one succeeds; again, in more causal terms this means that messages can be duplicated but are never lost.
Exactly-once delivery — each message is delivered exactly once to the recipient; the message can neither be lost nor be duplicated.

