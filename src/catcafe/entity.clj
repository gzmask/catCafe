(ns catcafe.entity)

(defonce registry (atom {}))
(defonce next-id (atom 0))

(defn create-entity
  "Add a new entity with the given components map to the registry and return its id."
  [components]
  (let [id (swap! next-id inc)]
    (swap! registry assoc id components)
    id))

(defn destroy-entity
  "Remove an entity from the registry."
  [id]
  (swap! registry dissoc id))

(defn update-entity
  "Update an entity using fn f and optional args."
  [id f & args]
  (apply swap! registry update id f args))
