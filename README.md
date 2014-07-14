# clojure-cassandra-demo

A basic last.fm clone to demonstrate using Apache Cassandra with Clojure.

## Installation

 * First you need to install CCM
  * https://github.com/pcmanus/ccm
 * You'll also need a last.fm api key
  * http://www.last.fm/api

## Usage

 1. Start a 3 node CCM cluster (see ccm docs)
 1. export LASTFM_API_KEY="your api key"
 1. lein ring server

### Things that could be improved

 * Use prepared statements
 * The counter cfs aren't particularly realistic. Include spark in the demo for periodically generating counts/analytics maybe?
 * UI
  * Add colors or something
  * Use CSS instead of HTML 1 style tags
 * Probably lots more.

## License

Copyright © 2014 Nick Bailey

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
