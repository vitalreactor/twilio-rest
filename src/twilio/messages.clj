(ns twilio.messages
  "Wrap the twilio rest library with higher level interface
   as an example of using the lower level REST API. Intended
   to support a multi-tenant user that uses Applications and
   custom URL routing to route responses to the appropriate
   response handler or pattern-matching route.  The namespace
   captures a single master account for purposes of subaccount
   management.

   This package assumes that you want to manage subaccounts according
   to a convention of account friendly name = application name and
   all numbers get the same/default application."
  (:require [twilio.rest :as rest]))


;; Configure namespace for a master account
;; -----------------------------------------

(defonce master (atom nil))

(defn configure! [sid token]
  (reset! master
          (rest/get (rest/map->Account {:sid sid :auth_token token}))))

;; Local registry
;; --------------------------------

(defonce ^:private registry (atom {}))
;; Schema: {^String name {:keys [account application numbers]}}

(defn- registry-entry [name]
  (@registry name))

(defn config [name]
  (:config (registry-entry name)))

(defn account [name]
  (:account (registry-entry name)))

(defn accounts []
  (map :account (vals registry)))

(defn application [name]
  (:application (registry-entry name)))

(defn numbers [name]
  (:numbers (registry-entry name)))
  

;; Applications
;; ---------------------------------

(defn- configured-application? [name app config]
  (and (= (:friendly_name app) name)
       (= (:sms_url app) (:message-url config))
       (= (:sms_method app) (:message-url-method config))
       (= (:sms_status_callback app) (:message-status-url config))))

(defn- app-config->fields [name config]
  (let [{:keys [message-url message-url-method message-status-url]} config]
    {:friendly_name name
     :sms_url message-url
     :sms_method message-url-method
     :sms_status_callback message-status-url}))

(defn- create-application [acct name config]
  (rest/create-resource acct rest/ApplicationList
                        (app-config->fields name config)))

(defn- configure-application [app name config]
  (rest/put! app (app-config->fields name config)))
             
(defn ensure-application 
  "Ensure application exists and is properly configured"
  [name config]
  (let [acct (account name)
        app (or (application name) (rest/get-application acct name))
        app (cond (nil? app) (create-application acct name config)
                  (configured-application? app name config) app
                  :default (configure-application app name config))]
    (swap! registry update-in [name] merge {:application app})
    app))


;; Accounts
;; --------------------------------

(declare sync-twilio sync-numbers)

(defn ensure-account
  "Create a new subaccount and associated application with
   the following defaults for new numbers:
   :config {
    :country <two digit country code>
    :area-code <by country>
    :message-url <local URL for replies to numbers>
    :message-url-method <:get or :post>
    :message-status-url <local URL>}
  }

  Populates the namespace registry with the current state
  "
  [name config]
  (let [acct (or (account name)
                 (rest/get-subaccount @master name)
                 (rest/create-resource @master rest/Accounts name))]
    (swap! registry update-in [name] merge {:account acct :config config})
    (ensure-application name config)
    (sync-numbers name)
    acct))
    

;; Numbers
;; --------------------------------

(defn allocate-number
  "Add a new number to the account according to configuration"
  [name]
  (let [{:keys [config account application]} (registry-entry name)
        {:keys [area-code country]} config]
    (when-not (and (number? area-code) (string? country) (= (count country) 2))
      (throw (ex-info "Improper account configuration"
                      {:area-code area-code :country country :account-name name})))
    (let [candidate (first (rest/available-numbers account country area-code))]
      (assert (map? candidate))
      (rest/add-number account candidate
                       {:sms_application_sid (:sid application)}))))

(defn allocated-number? [name e164-number]
  (let [numset (set (map rest/as-E164 (numbers name)))]
    (numset e164-number)))

(defn get-number
  "Get a new Twilio number record that doesn't match
   the provided list of strings or records.  Strings
   should be E164 encoded to match."
  ([]
     (get-number nil))
  ([name used]
     {:pre [(sequential? used)]}
     (let [numset (set (map rest/as-E164 used))
           allocated (numbers name)
           available (remove #(numset (:phone_number %)) allocated)]
       (if-not (empty? available)
         (first available)
         (let [new (allocate-number name)]
           (sync-numbers name)
           new)))))


;; Synchronize state
;; --------------------------------

(defn sync-twilio
  "Synchronize the registry with the server"
  ([]
     (map sync-twilio (keys registry)))
  ([name]
     (swap! registry update-in [name]
            (fn [registry-entry]
              (let [acct (rest/get-subaccount @master name)
                    app (rest/get-application acct name)
                    nums (rest/numbers acct)]
                (merge registry-entry
                       {:account acct
                        :application app
                        :numbers nums}))))))

(defn sync-numbers
  "Synchronize only the new number list with the server"
  ([]
     (map sync-numbers (keys registry)))
  ([name]
     (let [nums (rest/numbers (account name))]
       (swap! registry assoc-in [name :numbers] nums))))


;; Messages
;; --------------------------------

(defn send-message!
  [account-name message]
  {:pre [(:body message)
         (rest/E164-formatted? (:from message))
         (allocated-number? account-name (:from message))
         (rest/E164-formatted? (:to message))]}
  (let [acct (account account-name)
        cfg (config account-name)]
    (rest/create-resource acct rest/Messages
                          (assoc message
                            :status_callback (:status-url cfg)))))


;; Callback support
;; --------------------------------

(defn parse-ring-callback
  "Given the application name and post params for a callback,
   return the parameters in a consumable form"
  [params]
  (rest/underscorecase-map params))

(defn resolve-ring-callback
  "Take the resulting parameters and fetch the
   resulting message from Twilio and attach the
   associated account"
  [name params]
  (let [fields (parse-ring-callback params)]
    (assoc (rest/get (rest/map->Message fields))
      :acct (account name))))



;;===========================
;;===========================
;; OLD HANDLERS BELOW
;;===========================
;;===========================
  
;;(defn receive-msg [args]
;;  (log/trace :in :receive-msg-page :args args)
;;  ((map-importer @active-controller map->Message)
;;   (underscorecase-map args)))
;;    (receive-message-handler (get msg))))
    

;;(defn receive-message-status [args]
;;  (when-let [handler (:status-handler @active-controller)]
;    (try
;      (handler args)
;      (catch java.lang.Throwable e
;        (log/error :sms-handler name :args args :error e)))))

;(defn message-status-handler
;  "A ring-style request handler.  To enable Twilio callbacks,
;   register this with the servlet at the same address as
;   configured in the application."
;  [request]
;  (log/trace :in :receive-sms-error-page :args (:params request))
;  (receive-message-status (underscorecase-map (:params request)))
;  {:status 200 :body {}})

