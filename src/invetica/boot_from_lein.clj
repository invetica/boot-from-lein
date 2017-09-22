(ns invetica.boot-from-lein
  (:require
   [boot.core :as boot]
   [clojure.pprint :refer [pprint]]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn- expand-profiles
  [project profiles]
  (let [expand-profile (resolve 'leiningen.core.project/expand-profile)
        prefixes (map first profiles)]
    (cond
      (every? #{\+ \-} prefixes)
      (distinct
       (reduce (fn [result profile]
                 (let [pm (first profile), profile (keyword (subs profile 1))
                       profiles (expand-profile project profile)]
                   (if (= \+ pm)
                     (concat result profiles)
                     (remove (set profiles) result))))
               (mapcat #(expand-profile project %)
                       (:active-profiles (meta project)))
               profiles))

      (not-any? #{\+ \-} prefixes)
      (distinct
       (mapcat (comp #(expand-profile project %) keyword)
               profiles))

      :else
      (throw
       (ex-info
        "Profiles in with-profile must either all be qualified, or none qualified"
        {:exit-code 1})))))

(defn- report-from-lein
  [{:keys [profiles project project-file]}]
  (println
   (format "Using %s with %s we get:" project-file profiles))
  (pprint (-> project
              (select-keys [:aot
                            :certificates
                            :dependencies
                            :license
                            :main
                            :repl-options
                            :resource-paths
                            :scm
                            :source-paths
                            :test-paths
                            :url
                            :version])
              (assoc :keys (-> project keys sort)))))

(def ^:private only-existent
  (filter #(.exists (io/file %))))

(defn- paths
  [& xs]
  (into [] only-existent (apply concat xs)))

(defn- from-lein*
  [project-file profile]
  (require 'leiningen.core.project
           'leiningen.core.main)
  (let [lein-default-project (resolve 'leiningen.core.main/default-project)
        lein-read            (resolve 'leiningen.core.project/read)
        lein-set-profiles    (resolve 'leiningen.core.project/set-profiles)

        project (if (.exists project-file)
                  (lein-read (str project-file))
                  (lein-default-project))

        profiles (into [] (expand-profiles
                           project (some-> profile (.split ","))))
        project (lein-set-profiles project profiles)]
    {:profiles profiles
     :project project
     :project-file project-file
     ::resource-paths (paths (:resource-paths project))
     ::source-paths (paths (:source-paths project) (:test-paths project))}))

(boot/deftask from-lein
  "Configure a Boot project using a Leiningen project.clj file."
  [f project-file PATH    file "path to project.clj file"
   p profile      PROFILE str  "profile to use when reading project.clj"
   v verbose              bool "be more verbose"]
  (boot/merge-env! :dependencies '[[leiningen-core "2.7.1"]])
  (boot/with-pass-thru fs
    (let [project-file (or project-file
                           (io/file (System/getProperty "user.dir")
                                    "project.clj"))
          {:keys [project] :as m} (from-lein* project-file profile)]
      (when verbose (report-from-lein m))
      ;; FIXME `repl` is not defined when the code below is uncommented. I'm
      ;; guessing it's because of the use of `with-pass-thru` because it works
      ;; in a `deftask` that ends with `clojure.core/identity.`
      ;;
      ;; (boot/task-options!
      ;;  repl (:repl-options project {}))
      (boot/merge-env!
       :certificates (:certificates project)
       :dependencies (:dependencies project)
       :repositories (:repositories project)
       :resource-paths (::resource-paths m)
       :source-paths (::source-paths m)))))
