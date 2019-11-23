# MuWire architecture

### Core-UI separation

The MuWire application is split conceptually into a `core` component and two `ui` components - one graphical component which is build using Swing and one text-only component built using the "lanterna" library.

The core is written in mixture of Java and Groovy and is designed to be easy to embed into any application or language running on a JVM.  To achieve this, all communicatioon between the core and the outside world happens over an event bus using event objects.

### Event bus and events

At the heart of the core is the event bus.  It allows the different components that comprise the core to be decoupled, and allows the external components like UIs to communicate in asynchronous fashion with the core.

The Core object has a single instance of the `com.muwire.core.EventBus` class.  It is responsible for dispatching events to any registered listeners.  Events themselves extend the `com.muwire.core.Event` class and carry arbitrary information relevant to the event.  See below or an example how to build a custom event:

1.  Define the event in a class that extends `com.muwire.core.Event`:
```
package mypackage
import com.muwire.core.Event

class MyEvent extends Event {
    // add relevant fields here
}
```

2. Define one or more classes that will be notified of your events:
```
package mypackage

class MyEventListener {
    // ... add other logic here

    void onMyEvent(MyEvent e) {
        // logic to handle your type of event
    }
}
```

3. Register your event listener with the event bus:
```
MyEventListener myListener = new MyEventListener()
eventBus.register(MyEvent.class,myListener)
```
You can register more than one listener for the same type of event; they will be notified in the order you register them.

4. Publish events to the event bus
```
MyEvent myEvent = new MyEvent()
// ... set relevant fields of the event ...
eventBus.publish(myEvent)
```

Threading: the event bus creates a dedicated thread and all events are dispatched on that thread, regardless which thread publishes them.

### Sharing files
The UI publishes an event of type `com.muwire.core.files.FileSharedEvent` which contains a `java.io.File` reference to the file the user has chosen to share.  A component in the core called `HasherService` listens for these events, and when it receives notification that a FileSharedEvent has been posted it pereforms some sanity checks, then offloads the actual hashing to a dedicated thread.

Before the hashing begins, another event of type `com.muwire.core.files.FileHashingEvent` is published that contains the name of the file.  At the moment that event serves only to update the UI with the current file being hashed.

When the hashing completes, a `com.muwire.core.files.FileHashedEvent` is published by the HasherService.  The UI listens to this event and updates its list of shared files.  Another core component called `FileManager` also listens for such events and updates the interenal search index from the file name.
