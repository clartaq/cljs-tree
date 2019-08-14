(ns cljs-tree.demo-hierarchy
  (:require [reagent.core :as r]))

;; The hierarchical tree of tags is contained in the following. It is possible
;; to change it dynamically and have it re-render correctly.

(defonce test-hierarchy
         (r/atom {:title   "cljs-tree"
                  :tagline "Some experiments with hierarchical data."
                  :tree    [{:topic    "Journal"
                             :expanded true
                             :children [{:topic "2016"}
                                        {:topic    "2017"
                                         :expanded nil
                                         :children [{:topic    "11 - November"
                                                     :expanded true
                                                     :children [{:topic "Christmas Shopping"}
                                                                {:topic "Buy Groceries"}]}
                                                    {:topic    "22 - November"
                                                     :expanded true
                                                     :children [{:topic "Bake Pies"}]}
                                                    {:topic    "25 - November"
                                                     :expanded true
                                                     :children [{:topic "Cook Turkey"}]}]}
                                        {:topic "2018"}]}

                            {:topic    "Books"
                             :expanded true
                             :children [{:topic    "Favorite Authors"
                                         :expanded true
                                         :children [{:topic    "Gum-Lickin' Warburger"
                                                     :expanded true
                                                     :children [{:topic    "Biography"
                                                                 :expanded nil
                                                                 :children [{:topic "He was born a long time ago in some dusty, poor Southern town that is no longer there."}
                                                                            {:topic "His dad was an itinerant tinkerer, specializing in the repair of 15th century tea pots."}
                                                                            {:topic "His mom was a laundress who believed in the purifying power of plain ol' dirt. Her customers were not as fond of it, especially on their laundered clothes."}
                                                                            {:topic "As a result of these career choices, the family remained very poor."}]}
                                                                {:topic "Age"}
                                                                {:topic "DOB"}
                                                                {:topic "Obituary"}]}
                                                    {:topic "Bob Martin"}]}
                                        {:topic    "Genre"
                                         :expanded true
                                         :children [{:topic    "Science"
                                                     :expanded nil
                                                     :children [{:topic    "Astrophysics for People in a Hurry"
                                                                 :expanded true
                                                                 :children [{:topic    "Author"
                                                                             :expanded true
                                                                             :children [{:topic "Neil de Grasse Tyson"}]}
                                                                            {:topic    "ISBN"
                                                                             :expanded true
                                                                             :children [{:topic "978-0-393-60939-4"}]}]}]}
                                                    {:topic    "Science Fiction"
                                                     :expanded nil
                                                     :children [{:topic "Space Opera"}
                                                                {:topic "Military"}]}
                                                    {:topic "Horror"}
                                                    {:topic "Fantasy"}
                                                    {:topic "Biography"}
                                                    {:topic "History"}
                                                    {:topic    "Programming"
                                                     :expanded true
                                                     :children [{:topic "On Lisp"}
                                                                {:topic "Getting Clojure"}
                                                                {:topic    "Clean Code"
                                                                 :expanded nil
                                                                 :children [{:topic    "Author"
                                                                             :expanded true
                                                                             :children [{:topic "Robert Martin"}]}
                                                                            {:topic    "ISBN-10"
                                                                             :expanded true
                                                                             :children [{:topic "0-13-235088-2"}]}
                                                                            {:topic    "ISBN-13"
                                                                             :expanded true
                                                                             :children [{:topic "978-0-13-235088-4"}]}]}]}]}]}

                            {:topic    "Programming"
                             :expanded true
                             :children [{:topic    "Language"
                                         :expanded true
                                         :children [{:topic    "Java"
                                                     :expanded true
                                                     :children [{:topic "Snippets"}
                                                                {:topic "Books"}
                                                                {:topic "Blogs"}
                                                                {:topic "Gui Development"}]}
                                                    {:topic    "Clojure"
                                                     :expanded true
                                                     :children [{:topic "Snippets"}
                                                                {:topic "Books"}
                                                                {:topic "Numerics"}]}
                                                    {:topic    "Lisp"
                                                     :expanded nil
                                                     :children [{:topic "History"}
                                                                {:topic "Weenies"}
                                                                {:topic "The All Powerful"}]}]}]}

                            {:topic    "Animals"
                             :expanded true
                             :children [{:topic "Birds"}
                                        {:topic    "Mammals"
                                         :expanded nil
                                         :children [{:topic "Elephant"}
                                                    {:topic "Mouse"}]}
                                        {:topic "Reptiles"}]}

                            {:topic    "Plants"
                             :expanded true
                             :children [{:topic    "Flowers"
                                         :expanded true
                                         :children [{:topic "Rose"}
                                                    {:topic "Tulip"}]}
                                        {:topic "Trees"}]}]}))
