# Code Glosser (Rough Draft)

## What is it?

A tool for instructors to markup student code. Leave feedback by marking up code by leaving messages, which the user can see in HTML.
There is currently minimal support for project-wide markups.

## Features

### NetBeans Integration

Will be available soon on NetBeans Plugin-Portal soon, and hence will be readily available.

### Markup and Evaluate Code

Markup student code by leaving notes, which they will be able to see in HTML. 

### Export Markups to HTML Format

Export marked-up code as HTML. Marked up code appears as being highlighted, and display the message on hover.

![Marked Up as HTML](screenshots/markup_in_html.png)

Powered By: 'highlight.js'

#### Markup Entire Projects

##### Project Files View

![](screenshots/Project-View.png)

![](screenshots/Project-View2.png)

##### Add Markup (Custom)

![](screenshots/create_and_custom_markup.png)

##### Add Markup (Template)

![](screenshots/create_and_apply_template.png)

##### Color-Distinct Markups

![](screenshots/color-specific_markups.png)

## Implementation

### Control Flow

#### Reactive MVC

![Event Control Flow](screenshots/EventControlFlow.PNG)

#### Events

##### Create Markup

![](screenshots/createMarkupEvent.PNG)

##### Show Markup

![](screenshots/showMarkupEvent.PNG)

##### Double and Triple Click

![](screenshots/Double_And_Triple_Click_Event.PNG)

##### Delete Markup

![](screenshots/deleteMarkupEvent.PNG)

##### Modify Markup

![](screenshots/modifyMarkupEvent.PNG)
