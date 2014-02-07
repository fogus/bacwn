# bacwn

An implementation of Datalog for Clojure, based on the abandoned contrib-datalog.  This is not meant as a replacement for the contrib-datalog but instead as an extension to target both Clojure and ClojureScript.

![bacwn](https://raw.github.com/fogus/bacwn/master/doc/bacwn-logo.png "bacwn is delicious")

The Bacwn Datalog library is based on the old Clojure-contrib datalog implementation.  The library's syntax will change over time and it will be made to conform to modern Clojure's, but the spirit of the original will remain in tact.

*for a drop-in replacement for the contrib-datalog library see [Martin Trojer's contrib-datalog effort](https://github.com/martintrojer/datalog)*

## Usage

Caveat emptor. Bacwn is a work in progress and should be considered alpha software.  The ClojureScript port does not currently work - patches welcomed.

To use Bacwn in your own libraries, add the following to your dependencies:

### Leiningen

    :dependencies [[fogus/bacwn "0.4.0"] ...]

### Maven

Add the following to your `pom.xml` file:

    <dependency>
      <groupId>fogus</groupId>
      <artifactId>bacwn</artifactId>
      <version>0.4.0</version>
    </dependency>

## License

Copyright © 2009 Jeffrey Straszheim
Copyright © 2012-2014 Fogus

Distributed under the Eclipse Public License, the same as Clojure.
