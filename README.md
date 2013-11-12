# twilio-rest

twilio.rest - a simple wrapper for the Twilio REST API.  
twilio.messages - a higher level messaging interface built on twilio.rest

## Installing

An initial version of this library was released on clojars, so just add the following to your Leiningnen project.clj:

    :dependencies [[com.vitalreactor.twilio-rest "0.9.0"]]

## REST Usage

Here are a few simple uses for the REST API

    (ns my.cool.application
      (:require [twilio.rest :as twilio]))

    (def master {:sid "...." :auth_token "...."}) 

Create or fetch a subaccount

	(def subaccount (twilio/create-resource master twilio/Accounts "Subaccount Name"))
	(def subaccount (twilio/get-subaccount master "Subaccount Name"))

Create or fetch an application

    (def application (twilio/create-resource subaccount twilio/ApplicationList 
                         {:friendly_name "My Application"
                          :sms_url "https://myapp.foo.com/service/twilio/reply"
                          :sms_method "POST"
                          :sms_status_callback "https://myapp.foo.com/service/twilio/status"}))
    (def application (twilio/get-application subaccount "My Application"))

Get available numbers and assign to an account


Send a message

## Messages Usage

The twilio.messages namespace has a higher level interface for SMS
messaging applications that is somewhat opinionated about how to
manage and use multiple accounts, allocate numbers, and assign 
behaviors to specific numbers using applications.

    (ns my.cool.application
	  (:require [twilio.messages :as msg]))

Connect the namespace to your master account

    (msg/configure! "<master sid>" "<master auth_token>")

Register a subaccount and associated application

    (msg/ensure-account "My Staging" 
	   {:country "US"
	    :area-code 415
		:message-url "http://staging.myapp.com/twilio/reply"
		:message-url-method "POST"
		:message-status "http://staging.myapp.com/twilio/status"})

Ensure that we have at least one number
 
    (def number (msg/get-number))

Send a message

    (msg/send-message! "My Staging" 
       {:from number
        :to "+14155551212"
        :body "My message is less than 140 characters..."})

Parse a response from a ring handler

    (defn my-ring-handler 
      "Assumes route extracts app name from URL in :app"
      [request]
  	  (let [params (:params request)
            parsed (msg/parse-ring-callback params)
            message (msg/resolve-ring-callback (:app params) params)]
        message))

Get a new number, allocating if the list of already used numbers exhausts the allocated numbers on the Twilio subaccount.

    (def number2 (msg/get-number [number]))

## Developers

There are a number of good opportunities to improve the library

  - Adding new nouns to the REST interface (should be straight forward).
  - Integrate clj-twilio's implementation support for TwiML 
  - Developing other higher level namespaces for different use cases
  - Improving test coverage

## License

Copyright © 2013 Vital Reactor LLC

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
