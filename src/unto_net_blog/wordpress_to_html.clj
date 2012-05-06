;; Utilties for reading the original wordpress blog posts into static
;; HTML files under war/.
;;
;; Override the posts entity to match the local tables as necessary.
;;
;; Note, this does some highly unto.net specific logic to the contents
;; of each post.  It might be possible to gain inspiration from the
;; code below, but don't expect to be able to re-use much of it
;; verbatim.
;;
;; Everything is done automatically if run inside a swank repl (i.e.,
;; M-x clojure-jack-in) and (save-published-posts!).

(ns unto-net-blog.wordpress-to-html
  (:require [unto-net-blog.html5 :as html5])
  (:require [unto-net-blog.templates :as templates])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as string])
  (:require [clojure.xml :as xml])  
  (:use [clj-time.coerce :only (from-sql-date)])
  (:use [clj-time.format :only (formatter unparse)])
  (:use [korma.db])
  (:use [korma.core]))

(defdb wordpress (mysql {:db "wordpress" :user "root"}))

(def friendly-date-formatter (formatter "MMMM YYYY"))

(defn fix-dates [ent]
  "Replace the java.sql.Date instance with a ISO8601 date string and friendly string."
  (let [original-date (from-sql-date (:datetime ent))
        iso-date (.toString original-date)
        friendly-date (unparse friendly-date-formatter original-date)]
    (assoc ent
      :datetime iso-date
      :date friendly-date)))

(defn fix-slug [ent]
  "Normalize the slug into 'word-word-word' form."
  (assoc ent :slug (string/replace (:slug ent) "_" "-")))

(defn tag [name]
  (struct-map xml/element :tag name))

(defn tag=? [tag element]
  (= (:tag element) tag))

(defn a-href? [element]
  (and (tag=? :a element) (:href (:attrs element))))

(defn img-src? [element]
  (and (tag=? :img element) (:src (:attrs element))))

(defn edit-when [node pred f & args]
  "Replace node matching (pred node) with the value of (f node args)"
  (if (pred node)
    (apply f node args)
    node))

(defn edit-all-when [node pred f & args]
  "Recursively replace nodes matching (pred node) with the value of (f node args)"
  (let [edited (apply edit-when node pred f args)]
    (if-let [contents (:content edited)]
      (assoc edited :content (map #(apply edit-all-when % pred f args) contents))
      edited)))

(defn replace-tags [element from to]
  (edit-all-when element (partial tag=? from) #(assoc % :tag to)))

(defn replace-i-tags [element]
  (replace-tags element :i :em))

(defn replace-b-tags [element]
  (replace-tags element :b :strong))

(defn replace-center-tags [element]
  (edit-all-when element
                 (partial tag=? :center)
                 #(assoc % :tag :div :attrs {"style" "text-align: center; width: 100%; margin: auto"})))

(defn break-paragraphs [s]
  {:pre [(string? s)]}
  "Replace double newslines with double br tags."
  (interpose (repeat 2 (tag :br)) (vec (.split s "[\r\n]{2,}" -1))))

(defn break-all-paragraphs [parent]
  "Walk through the elements replacing double-newlines with double br tags."
  (if (tag=? :pre parent)
    parent
    (assoc parent
      :content (flatten (map #(if (string? %) (break-paragraphs %) (break-all-paragraphs %)) (:content parent))))))

(defn set-a-href [a href]
  (assoc-in a [:attrs :href] href))

(defn set-img-src [img src]
  (assoc-in img [:attrs :src] src))

(def unto-link-re
  #"https?://(?:blog|www)?.?unto.net/(?:\w+/)*([\w\-\_]+)(?:\.html)?")

(defn extract-slug [url]
  "Extract the slug from a URL shaped like http://blog.unto.net/work/slug.html"
  (second (re-find unto-link-re url)))

(defn replace-link [href]
  "Given a link, give a suitable replacement."
  (if-let [slug (extract-slug href)]
    (str "http://blog.unto.net/" slug ".html")
    href))

(defn replace-a-href-link [a]
  (let [href (:href (:attrs a))]
    (if (.contains href "unto.net")
      (set-a-href a (replace-link href))
      a)))

(defn replace-img-src-link [img]
  (let [src (:src (:attrs img))
        re #"(?:https?://blog.unto.net)?/downloads/(.*)$"]
    (if-let [resource (second (re-find re src))]
      (set-img-src img (str "http://static.unto.net/" resource))
      img)))

(defn replace-a-href-links [parent]
  (edit-all-when parent a-href? replace-a-href-link))

(defn replace-img-src-links [parent]
  (edit-all-when parent img-src? replace-img-src-link))

(defn body [html]
  "Return the inner HTML of a properly serialized HTML5 element."
  (second (re-find #"(?s)<body>(.*?)</body>" html)))

(defn tidy [content]
  "Transform a (potentially invalid) HTML string into a valid HTML5 string."
  (-> (html5/parse-str content)
      (replace-i-tags)
      (replace-b-tags)
      (replace-center-tags)
      (break-all-paragraphs)
      (replace-a-href-links)
      (replace-img-src-links)
      (html5/serialize-doc-to-str)
      (body)))

(defn fix-content [ent]
  (assoc ent :content (tidy (:content ent))))

(def template (slurp "data/template.html"))

(defn to-html [post]
  (templates/interpolate template post))

(defentity posts
  (table :unto_posts)
  (transform fix-dates)
  (transform fix-slug)
  (transform fix-content))

(defn published-posts [& [count]]
  (select posts
    (fields :id
            [:post_date_gmt :datetime]
            [:post_title :title]
            [:post_name :slug]
            [:post_content :content])
    (limit count)
    (where {:post_status [like ["publish"]]})))

(defn blacklisted? [post]
  "Skip these ids when saving posts."
  (contains? #{3 70 71 316} (:id post)))

(defn save-post! [post]
  (let [path (str "war/" (:slug post) ".html")]
    (spit path (to-html post))))

(defn copy-file! [from to]
  (with-open [in (io/input-stream from)
              out (io/output-stream to)]
    (io/copy in out)))

(defn copy-static-files! []
  "Copy a number of precanned files from data/ to war/."
  (doseq [file ["atom.xml" "favicon.ico" "robots.txt" "style.css" "last-post.html"]]
    (copy-file! (str "data/" file) (str "war/" file))))

(defn archive-items-str [posts]
  (apply str (map #(str "<li><a href=\"/" (:slug %) ".html\">" (:title %) "</a></li>\n") (sort-by :datetime posts))))
  
(defn list-of-posts [posts]
  (str "<ul>\n" (archive-items-str posts) "</ul>\n"))

(defn write-archives-file! [posts]
  "Write out a static file 'archives.html' containing links to all of the other posts"
  (let [content (list-of-posts posts)]
    (save-post! {:slug "archives"
                 :content content
                 :title "Archives"
                 :datetime "2012-05-06T07:10:33:000Z"
                 :date "May, 2012"})))
  
(defn save-published-posts! []
  (let [posts (filter (complement blacklisted?) (published-posts))]
    (doseq [post posts] (save-post! post))
    (copy-static-files!)
    (write-archives-file! posts)))

;; Run this to dump and process posts to HTML.
(save-published-posts!)
