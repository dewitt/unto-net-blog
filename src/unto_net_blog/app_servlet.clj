(ns unto-net-blog.app_servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use unto-net-blog.blog-unto-net-webapp)
  (:use [appengine-magic.servlet :only [make-servlet-service-method]]))

(defn -service [this request response]
  ((make-servlet-service-method unto-net-blog-app) this request response))
