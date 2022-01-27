# The Problem

Persistent Data Structures use structural sharing to create an efficient in-memory representation, but these same structures can be a problem to serialize/deserialize. 

**First**, the shared parts are duplicated in the serialised output which can lead to a large and inefficient representation.

**Second**, when the process is reversed,  deserialization doesn't recreate the original, shared, effcient in-memory representation because sharing information was lost in the original serialisation.

## The Solution Provided

This library "de-duplicates" persistent datastructures so they can be more efficiently serialised.

It provides two functions `de-dupe` and `expand`.

`de-dupe` takes a persistent data structure, `pds`, and it returns a hash-map, which maps "tokens" (ids) to duplicated sub-nodes. The item with token `cache-0` represents the root of `pds`.

Having used `de-dupe`, you are expected to then serialise the the hash-map using whatever method makes sense to your usecase - perhaps [transit](https://github.com/cognitect/transit-cljs); or serilization with `edn`.  So `de-dupe` is a pre-processor for use before transit.

Later, `expand` can be used to reverse the process - you give it a hash-map and it reforms the original, sharing and all. 

## Usage

Then add this requirement to your cljs file:
```
(:require [de-dupe.core :refer [de-dupe expand]])
```

The default behaviour is to only recognize duplicates when they compare
with `identical?`

```
(def compressed (de-dupe some-data))
;  if you now compare 
;  (count (prn-str compressed)) and (count (prn-str some-data))
;  you will see the degree of comparision

;  to recover your original data
(def some-data2 (expand compressed))
```

If you want to de-dupe items that are not identical (i.e the same object reference)
```
(def compressed (de-dupe-eq some-data))
;  if you now compare 
;  (count (prn-str compressed)) and (count (prn-str some-data))
;  you will see the degree of comparision it will be greater than de-dupe

;  to recover your original data
(def some-data2 (expand compressed))
```

## State of play

This Library is ok for speed at the moment but can maybe can benefit from more optimisation.

Hash seems to take a big chunk of time but if we use the ECMA 6 (Harmony) (js/Map.)
which tests by identity, the time taken does not reduce. This may be because the 
number of elements in the map increases. As this implementation uses js/Map you will need to run it on a modern browser (Chrome, IE 10, Firefox). The browser will only need to implement the Map.set() and Map.get() methods.

## Limitations

* This implementation can only cache things that can have meta-data attached to them (lists, sets, vectors, maps).
* This implementation caches everthing it can, even if the value is only used once it will be cached, which means that the decompression phase will always take longer than it should.
* This implementation by default will consider two objects as different if (identical? x y) returns false. This is to save time computing the hash of the objects to check for equality. Use de-dupe/create-eq-cache for de-duplication for non-identical structures.

## Implementation details

Its a form of dictionary compression. The shared structures are identified and extracted from the overall data structure, and then added to the result hash-map. 
The result hash-map also contains a represenation of the root note of the `pds`.

This implementation uses a special version of clojure.walk which keeps track of both the original form (or more correctly that returned from the inner function), and the newly modified form from the outer function.

This makes it possible in the prewalk phase (stepping down the tree from root to leaf) to cache all the forms in a js/Map (from now on referred to as the 'values' cache, this is mutable). In addition the key generated for the values cache (itself just a cljs symbol) is added to the metadata of the form. 

If there is a cache 'hit' on the values cache, in this phase the form will be replaced by the cache key that is found and the algorithm will not continue to walk down the structure.

On the way back up the tree the algorithm will begin to construct the 'compressed' cache. This is the cache where the value for each cache key itself contains cache keys. This compressed cache is constructed as the outer function will return the cache key for each value which is found by examining the metadata attached to the object on the way down.

Finally the top level compressed value is returned and assigned to the cache as cache-0.


## License

Copyright © 2015 Michael Thompson

Distributed under The MIT License (MIT) - See LICENSE.txt