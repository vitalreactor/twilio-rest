(defproject com.vitalreactor/twilio-rest "1.0.0"
  :min-lein-version "2.0.0"
  :description "Adapter library for the Twilio web service."
  :url "http://github.com/vitalreactor/twilio-rest"
  :scm "http://github.com/vitalreactor/twilio-rest"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.3.1"]
                 [clj-http "2.0.0"]
                 [midje    "1.8.2"]]
  :plugins [[lein-midje "3.1.3"]])
