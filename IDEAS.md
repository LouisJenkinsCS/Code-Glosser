#Ideas

Below are some general ideas and features I'm planning for this project. Comments, disagreements or discussions can be inlined with each pointer by just adding it after the end, preceeded by a | character. Hence

* This should destroy the world!!! | "That would actually be a very bad idea..."

Would show a specific comment there. Unless there is an alternative way to do so, which there probably is.

##Coding

* Abstract over Concrete
    - I STRONGLY recommend we find a way to abstract this to work through an API, as if this is to be open source, we'll probably want other people to create plugins using this.
        + ALSO, by defining all interfaces ahead of time, it comes down to just the implementation detail. As the core design will already have been finished, porting to IntelliJ and Eclipse become simpler as it's just a matter of "Implement this file and this file" rather than "How should I do this... hmmm"
    - Pretty much I mean we should design as if we are not using NetBeans' API, but rather for any generic type.
        + Could take longer initially, but if it's a year-long project, time is no actual issue.
* Java 8
    - I know I said this before, but I thrive on Java 8. Lambdas, Streams, etc. are invaluable for making this work and speeding up development times. It came out in 2014, there's been enough time for people to adapt to it.
        + Although I WILL of course do Java 7 if needed... but I really won't like it.
* Coding Style Guidelines
    - We should create a separate document to define the coding style we should both be using. I've been writing a lot of C in the past few months and my Java has gotten a bit rusty, but from what happened during my project with S.A.K-Overlay, I ended up rewriting it 2 times (I was actually about to rewrite a 3rd before I burnt out and took a break) due to the changes in my overall style. I don't want my C (Pseudo Linux Kernel Style) to leak into Java, so it'd be best if we just set it out at the beginning and make changes as we go along.
        + My initial must-haves definitely are Java 8 Lambdas and Streams, they allow code to be more functional and might I say elegant.
        + We also might want to write up some UML diagrams and other software engineering tools. I've still not even taken the course yet, so I don't know the best way to do this.
* Roles
    - In this project, I'm assuming that since you are very busy as an instructor and all, you will act as the project manager for the project. You get the last say in everything, and you get to determine if the project is getting too big or out of hand and if we should focus on something else.
    - I'm assuming that my role in this project is to be the actual developer, as I definitely have more time than you would for a project like this. I'd update you with all of the things I'm implemented, ask advice when needed, and generally follow the direction you want this project to go into.

##Annotating

* Annotating
    - Highlighted text, when right clicked, add an extra menu option specific for the context of the highlighted text.
        + Quick and easy access rather than having to go into View > Code Gawker from toolbar
        + SHOULD have options to add/modify/remove the comment for the highlighted section
            * Should also leave a mark to signify it contains a comment, rather than being highlighted.
                - If this is not normally possible, think of just keeping track of the area selected (X,Y coordinates, line number, etc.), and creating an overlay which can show the marked areas when toggled.
    - Should allow user to easily know when someone annotated the code, but not in an obnoxious way
        + Having it highlighted can be more than a bit much. Maybe have it underlined in a color similar to a syntax error (red squiggly underline), perhaps green or some configurable color?
        + When hovered over, should show the comment
            * Should also allow the user to respond to comments.
* Persistence
    - Version-Control diff and detection
        + Detect differences in source code to make smarter automated choices
            * Dynamically stretching the comments after the text has changed entirely
                - If the code originally commented on has been expanded on, the comment should also apply to that as well.
                    + Or if the user has deleted portions of the original code, but some still remains, it should also follow suit.
                - Microsoft Word does this for collaborative comments.
        + Helps when serializing and restoring across sessions.
        + Think of devising a plugin for `git diff` or creating a more minimal one.
* Commenting
    - Should allow the commenter to be responded to
        + Should be relatively easy to do...
    - Comments should be persistent in how they are shown
    - Comments should also allow the conversation to be taken from the file to a live-chat kind of thing.
        + Allow the commenter to leave an email address of some sort and allow all responses to be forwarded directly there
            * Difficult part is to update both files, but we'll figure that out later.
*  Concurrent Annotations
    -  Allow edits to annotations to be immediately shown on the other's.
        +  Note that it doesn't need to be the file itself, just the annotations themselves.
        +  This can probably be done by having any clients listen for annotations to change on a socket, and also to automatically push out any changes as well.
            *  The biggest difficulty is setting up the actual server. Servers are relatively cheap, but not free.

##Portability

* Think of a way to make it work on both Eclipse AND NetBeans. In fact, perhaps design an API that can be wrapped on either platform that abstracts the work needed to be done.
* Support multiple languages
    - C, C++, Java, Python, Haskell, etc.
* By supporting Eclipse, NetBeans and IntelliJ, you end up supporting all commonly used IDE's, and any others can use the API devised as well.

##Documentation

* Documentation
    - As I strongly recommended we go abstract first, writing documentation along with the overall interfaces and abstract classes needing to be written would help immensely. In fact, I can do so without even having to have read the NetBeans development guides, as they are, as I said, abstract.
    - I suggest we use Slate for the Wiki and README.md, but this is also up to you.
        + Javadoc can be for more extensive information.
    - All documentation can be hosted on your `github.io` (or `gh-pages`)
