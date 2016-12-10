## Implementation

### Control Flow

### Code Snippet (TODO: Rename Category)

Events are made up like such...

```java
class Event {
    public final int tag;
    public final Object data;
}
```

An example of event propagation would be the MarkupController, which handles all of the heavy-lifting of the application.
In the below snippets, you can see how responding to `MarkupProperty` can be handled in an elegant manner.

```java
observer.onEvent()
    .map(e -> {
        Function<Object, Optional<Event>> dispatchHandler;
        switch (e.tag) {
            case Event.CREATE_MARKUP:
                dispatchHandler = this::createMarkup;
                break;
            case Event.MARKUP_MUTATION:
                dispatchHandler = this::markupMutation;
                break;
            case Event.MARKUP_SELECTION:
                dispatchHandler = this::markupSelection;
                break;
            case Event.FILE_CHANGE:
                dispatchHandler = this::fileChange;
                break;
            default:
                dispatchHandler = ignored -> Optional::empty;
        }
        return dispatchHandler.apply(e.data);
    })
    .filter(Optional::isPresent)
    .map(Optional::get)
    .subscribe(onEvent::onNext);
```

For those unfamiliar with Java 8, the syntax may look unfamiliar, so I will give an overall summary of what is going on here.

`observer` would be an rxJava `Observable`, which allows push-based notifications, when an event is emitted from the source `Observable`.
By calling `subscribe`, we become an `Observer`, and the callback provided, `onEvent::onNext` which is a method reference to another `Observable`
that we are pushing events (and therefore propagating), is called on each event emitted.

Operators, like `map`, and `filter`, provide a new `Observable` that, put simply, will perform those actions before passing onwards. So, `map` will take
the `Event` object, map it to a new `Optional<Event>` object, and `filter` will only emit it's object if and only if it succeeds it's predicate: that
`Optional::isPresent` (which is a method to determine if the element the `Optional` holds is null or not).

The `dispatchHandler` is more of clarity, to help the reader understand what is going on: `Function` is a Java 8 construct that represents a function that
allows application. Application meaning "applying an argument" later. A Method Reference can be safely be stored in a `Function`. A `Function` of type
`Function<T,R>` is a function that takes `T`, and returns `R`. So, `Function<Object, Optional<Event>>` is a function that takes `Object` and returns `Optional<Event>`.
In the `default` statement, a lambda takes the form `(n0, n1, ... nk) -> { }`, but a single argument lambda does not need parenthesis surrounding it. As well a 
single line lambda does not need to be wrapped in a block. A lambda expression can also fit within a `Function`.

While this may seem daunting at first, once understood, it makes code a lot more clean and easy to read.


#### Reactive MVC

![Event Control Flow](screenshots/EventControlFlow.PNG)

#### Events

##### Create Markup

![](screenshots/CreateMarkupDiagram.PNG)

##### Select Markup

![](screenshots/SelectMarkupDiagram.PNG)

##### Mutate Markup

![](screenshots/MutateMarkupDiagram.PNG)


##### File Change

![](screenshots/FileChangeDiagram.PNG)

##### Apply Templates

![](screenshots/ApplyTemplatesDiagram.PNG)