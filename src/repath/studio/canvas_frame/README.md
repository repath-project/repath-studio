# Frame Canvas

Our canvas frame is an iframe element that hosts anything that needs to be rendered.
The main svg canvas is rendered inside the iFrame using the [react-frame-component](https://github.com/ryanseddon/react-frame-component).

## Why would you render to an iFrame?
### Style encapsulation
We won't have to worry about style inheritance.
### Layout boundaries 
Canvas elements will not affect the parent window.
## Caveats

This iFrame introduces a different browsing context so we need to simulate some of the events to the parent window.