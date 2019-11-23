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

2. Define one or more classes thatt will be notified of your events:
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


