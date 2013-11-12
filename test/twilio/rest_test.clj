(ns twilio.rest-test
  (:refer-clojure :exclude [get])
  (:use midje.sweet
        [midje.util :only [expose-testables]])
  (:require [twilio.rest :as rest]))
  

(expose-testables twilio.rest)

(facts "convert to/from camelcase"
  (fact "clojure"
    (dashed->camelcase "this-is-a-test")
    => "thisIsATest")
  (fact "underscore"
    (dashed->camelcase "this_is_a_test")
    => "thisIsATest")
  (fact "camel"
    (camelcase->dashed "thisIsATest")
    => "this-is-a-test"))

(facts "pascalcase"
  (fact "is uppercase camelcase"
    (dashed->pascalcase "this-is-a-test")
    => "ThisIsATest"))

(def master {:sid "..." :auth_token "..."})

(future-facts "master account"
  (let [macct (rest/get (rest/map->Account master))]
    (fact "our account is the master"
      macct => rest/master-account?)
    (let [old (:friendly_name macct)]
      (rest/set-account-name macct "FooBar")
      (fact "can change account name"
        (:friendly_name (rest/get macct))
        => "FooBar")
      (rest/set-account-name macct old))))



      

