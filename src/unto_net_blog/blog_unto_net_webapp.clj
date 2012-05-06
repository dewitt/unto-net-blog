;; A Compojure webapp for dispatching requests to blog.unto.net.  Legacy
;; posts are served as static content out of the war directory, and
;; requests to the homepage are redirected to the Google+ profile.

(ns unto-net-blog.blog-unto-net-webapp
  (:use compojure.core)
  (:require [appengine-magic.core :as ae])
  (:require [ring.util.response :as response])
  (:require [clojure.string :as string])
  (:require [compojure.route :as route]))

(defn fix-slug [slug]
  "Normalize the slug into 'word-word-word' form."
  (string/replace slug "_" "-"))

(defn redirect-to-static [slug]
  (response/redirect (str "/" (fix-slug slug) ".html")))

(def not-found-body
  "<!DOCTYPE html><html><head><title>Error 404 (Not Found)</title></head>
    <body><p>The requested URL  was not found on this server.</p></body>")

(def not-found-response
  {:status 404
   :headers {}
   :body not-found-body})

(defroutes unto-net-blog-app-handler
  (GET "/" req (response/redirect "https://plus.google.com/117377434815709898403"))
  (GET "/feed" req (response/redirect "/atom.xml"))
  (GET "/*/feed" [] not-found-response)  ; A common request, always invalid
  (GET "/*/feed/" [] not-found-response)  ; A common request, always invalid
  (GET "/category/*" [] not-found-response)  ; A common request, always invalid
  (GET "/page/*" [] not-found-response)  ; A common request, always invalid
  (GET "/downloads/:whatever" [whatever] (response/redirect (str "http://static.unto.net/" whatever)))
  (GET ["/:category/:slug" :category #"[a-z]+" :slug #"[\w\-]+"] [slug] (redirect-to-static slug))
  (GET ["/:category/:slug/" :category #"[a-z]+" :slug #"[\w\-]+"] [slug] (redirect-to-static slug))
  (GET ["/:slug" :slug #"[\w\-]+"] [slug]  (redirect-to-static slug))
  ;; All other 200s are static files, as found under the 'war' directory.
  (ANY "*" [] not-found-response))

(ae/def-appengine-app unto-net-blog-app #'unto-net-blog-app-handler)

; (ae/serve unto-net-blog-app)