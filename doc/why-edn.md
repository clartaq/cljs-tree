The data format used to persist the state of the tree is [edn](https://github.com/edn-format/edn). The more traditional way to handle an outline is to use [OPML].

Why use edn instead of OPML?

Well, getting OPML to work must be more complicated that I can understand. There are a number of tools available, but I can't get them to work. Of the things that do work, no two seem to agree on what is valid OPML despite the availability of a spec and a validator.

OPML is an XML-based format. It is supposed to be readable by humans and it mostly is.

The tools to manipulate OPML and the underlying XML seem to be monstrously huge.

edn, on the other hand, I find extremely readable. (Could be because it looks just like Clojure.)

edn represents a very rich set of elements, larger than JSON.

edn is easily extensible.

edn is dead simple to read and write.

There is built-in support for edn in the [transit](https://github.com/cognitect/transit-clj) protocol. I plan to use that to pass data back and forth between the client (where the visual representation of the tree will be edited and viewed) and the server (where database searches will happen and where the tree will be persisted.)



