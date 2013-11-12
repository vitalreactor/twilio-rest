(defproject com.vitalreactor.twilio-rest "0.9.0"
  :min-lein-version "2.0.0"
  :description "Adapter library for the Twilio web service."
  :url "http://github.com/vitalreactor/twilio-rest"
  :scm "http://github.com/vitalreactor/twilio-rest"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.2.0"]
                 [clj-http "0.7.7"]
                 [midje    "1.5.0"]]
  :plugins [[lein-midje "3.0.0"]])
