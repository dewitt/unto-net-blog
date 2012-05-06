(defproject unto-net-blog "1.0.0-SNAPSHOT"
  :description "Google App Engine hosted blog.unto.net."
  :dependencies [[clj-time "0.4.2"]
                 [compojure "1.0.3"]
                 [korma "0.3.0-beta10"]
                 [mysql/mysql-connector-java "5.1.19"]
                 [nu.validator.htmlparser/htmlparser "1.3.1"]  ; see note
                 [org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.1.2"]]
  :dev-dependencies [[appengine-magic "0.5.0"]])

;; Note: the version in the maven repository is only 1.2.1. Instead,
;; download from http://about.validator.nu/htmlparser/ and install
;; using:
;;
;;   $ mvn install:install-file -DgroupId=nu.validator.htmlparser \
;;       -DartifactId=htmlparser -Dversion=1.3.1 -Dpackaging=jar \
;;       -Dfile=htmlparser-1.3.1.jar
;;
;; Be sure to get the path to the local jar file correct.