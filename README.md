# bacwn

An implementation of Datalog for Clojure, based on the abandoned contrib-datalog.

![bacwn](https://raw.github.com/fogus/bacwn/master/doc/bacwn-logo.png "bacwn is delicious")

The Bacwn Datalog library is based on the old Clojure-contrib datalog implementation.  The library's syntax will change over time and it will be made to conform to modern Clojure's, but the spirit of the original will remain in tact.  To use Bacwn Datalog in your onw libraries, add the following to your dependencies:

## Usage

Caveat emptor. Bacwn is a work in progress and should be considered alpha software.  The ClojureScript port does not currently work - patches welcomed.

### Leiningen

    :dependencies [[bacwn "0.2.0-SNAPSHOT"] ...]    

### Maven

Add the following to your `pom.xml` file:

    <dependency>
      <groupId>evalive</groupId>
      <artifactId>bacwn</artifactId>
      <version>0.1.0</version>
    </dependency>

## License

Copyright © 2012 Fogus

Distributed under the Eclipse Public License, the same as Clojure.
