(ns cljwebserve.util)
(in-ns 'cljwebserve.util)

(defmacro with-release
  "Append a (release-pending-sends) form to body before evaluating."
  [& body]
  `(do
     ~@body
     (release-pending-sends)))

(defmacro agentify
  "Packages body and sends to agent.

agent: the agent to which body will be sent
agent-sym: refers to the previous agent value
agent-modifier: modification function; new agent value given by result of agent-modifier's evaluation."
  [[agent agent-sym agent-modifier] & body]
  (let [inner-function (gensym)]
    `(letfn [(~inner-function
               [~agent-sym]
               ~@body
               ~agent-modifier)]
       (send ~agent ~inner-function))))
            
  