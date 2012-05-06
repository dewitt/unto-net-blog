unto-net-blog
=============

This project contains a set of three services for the migration of
blog.unto.net content from Wordpress to Google App Engine, using
Clojure.  While no effort has been made to make this code generic or
usable by other wordpress blogs, please feel free to repurpose the
code as you see fit, as made available under the Apache License.


Usage
-----

Step 1)  Dumping the existing data from Wordpress.

    ; Dumping the contents of the live wordpress MySQL instance
    $ mysqldump -u root -p wordpress > wordpress.mysql

    ; Loading the remote wordpress instance locally (OS X)
    $ brew install mysql
    $ cd /usr/local
    $ mysql_install_db
    $ mysqld_safe &
    $ mysqladmin -uroot create wordpress
    $ mysql -uroot wordpress < wordpress.mysql

    ; Viewing the local mysql tables
    $ mysql -u root wordpress
    mysql> show tables;

    ; Set up emacs for clojure/slime/swank
    ; See: http://wiki.unto.net/setting-up-clojure-and-slime

    ; Dump static HTML to war/ directory
    $ lein deps
    $ emacs src/unto_net_blog/wordpress_to_html.clj
    (M-x clojure-jack-in)
    (save-published-posts!)


Step 2)  Serving the static version of the blog posts.

    ; Run inside the REPL
    $ emacs src/unto_net_blog/blog_unto_net_webapp.clj
    (M-x clojure-jack-in)
    (ae/serve unto-net-blog-app)

    ; Prepare for appengine
    $ lein appengine-prepare
    $ dev_appserver.sh war
  
    ; Deploy to appengine
    $ lein appengine-prepare
    $ appcfg.sh update war


License
-------

     Copyright 2012 DeWitt Clinton

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
