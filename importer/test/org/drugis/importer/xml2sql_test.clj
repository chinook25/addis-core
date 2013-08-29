(ns org.drugis.importer.xml2sql_test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [org.drugis.importer.xml2sql :refer :all]
            [riveted.core :as vtd]  
            ))

(deftest test-xpath-parent
  (testing "xpath can go back up the tree"
    (let [xml "<root attr=\"x\"><foobar/></root>"
          node ($x? "/root/foobar" xml)]
          (is (= "root" (vtd/tag ($x? ".." node)))))))

(deftest test-xpath-attr
  (testing "xpath can select attributes"
    (let [xml "<root attr=\"x\"/>"
          node ($x? "/root/@attr" xml)]
      (is (= "attr" (vtd/tag node)))
      (is (= "x" (vtd/attr node (vtd/tag node))))
      )))

(deftest test-get-column-value
  (testing "get-column-value should apply xpath and transform"
    (is (= {:foo "bar"}
           (apply-context (get-column-value
             "<root><foobar>bar</foobar></root>"
             :foo
             ["/root/foobar" vtd/text]) nil nil)))
    (is (= {:foo "baz"}
           (apply-context (get-column-value
             "<root><foobar foo=\"baz\">bar</foobar></root>"
             :foo
             ["/root/foobar" #(vtd/attr % :foo)]) nil nil))))
  (testing "get-column-value generates context closure where sql-id required"
    (let [rval (:foo (get-column-value
                       "<root><foobar>bar</foobar></root>"
                       :foo
                       ["/root/foobar" vtd/text :sibling :pitty]))
          context {:pitty {"bar" [8 {}] "baz" [10 {}]}}]
      (is (= 8 (rval nil context)))))
  )

(deftest test-get-column-values
  (testing "get-column-values maps all columns"
    (is (= {:foo "bar" :fu "baz"}
           (apply-context (get-column-values
             "<root><foobar foo=\"baz\">bar</foobar></root>"
             {:foo["/root/foobar" vtd/text]
              :fu ["/root/foobar" #(vtd/attr % :foo)]}) nil)))))

(deftest test-get-xml-value
  (testing "get-xml-value works"
    (is (= "bar" (get-xml-value "<foobar foo=\"baz\">bar</foobar>" ["/foobar" vtd/text])))))

(deftest test-get-table
  (let [foobar-def {:xml-id ["." vtd/text]
                    :sql-id :foo
                    :each "./foobar"
                    :table :foobar
                    :columns {:foo ["." vtd/text]
                              :fu ["." #(vtd/attr % :foo)]}
                    :dependent-tables []}
        ctx-map-row (fn [row-tpl parent context] (assoc row-tpl :columns (apply-context (:columns row-tpl) parent context)))
        nil-map #(ctx-map-row % nil nil)
        ctx-map-rows (fn [rows-tpl parent context] (into {} (map (fn [[k v]] {k (ctx-map-row v parent context)}) rows-tpl)))
        nil-map-rows #(ctx-map-rows % nil nil)
        ctx-map-table (fn [table-tpl parent context] (assoc table-tpl :rows (ctx-map-rows (:rows table-tpl) parent context)))
        nil-map-table #(ctx-map-table % nil nil)
        ctx-map-tables (fn [tables-tpl parent context] (into [] (map #(ctx-map-table % parent context) tables-tpl)))
        nil-map-tables #(ctx-map-tables % nil nil)]
    (testing "get-table-row returns xml-id and columns"
      (let [table-row (nil-map-rows (get-table-row
                                     ($x? "/foobar" "<foobar foo=\"baz\">bar</foobar>")
                                     foobar-def))]
        (is (= {"bar" {:columns {:foo "bar" :fu "baz"} :dependent-tables []}} table-row))))
    (testing "get-table-row generates random xml-id when missing"
      (let [table-def (dissoc foobar-def :xml-id)
            table-row-fn (fn [] (nil-map-rows (get-table-row
                                     ($x? "/foobar" "<foobar foo=\"baz\">bar</foobar>")
                                     table-def)))
            table-row1 (table-row-fn)
            table-row2 (table-row-fn)
            xml-id1 (first (keys table-row1))
            xml-id2 (first (keys table-row2))]
        (is (not (nil? xml-id1)))
        (is (not (= xml-id1 xml-id2)))
        (is (= {:columns {:foo "bar" :fu "baz"} :dependent-tables []} (get table-row1 xml-id1)))))
    (testing "get-table returns xml-id and columns"
      (let [table (nil-map-table (get-table
                                   ($x? "/root" "<root><foobar foo=\"baz\">bar</foobar><foobar foo=\"qux\">qox</foobar></root>")
                                   foobar-def))]
        (is (= {"bar" {:columns {:foo "bar" :fu "baz"} :dependent-tables []}
                "qox" {:columns {:foo "qox" :fu "qux"} :dependent-tables []}} (:rows table)))))
    (testing "get-table-row recurses for dependent-tables"
      (let [table-def {:xml-id ["." #(vtd/attr % :id)]
                       :sql-id :id
                       :each "/root/container"
                       :table :container
                       :columns {:id ["." #(vtd/attr % :id)]}
                       :dependent-tables [foobar-def]}
            table-tpl (get-table-row
                        ($x? "/root/container" "<root><container id=\"3\"><foobar foo=\"baz\">bar</foobar></container></root>")
                        table-def)
            table (assoc-in (nil-map-rows table-tpl) ["3" :dependent-tables] (nil-map-tables (get-in table-tpl ["3" :dependent-tables]))) ]
        (is (= {"3" {:columns {:id "3"}
                     :dependent-tables [{:table :foobar
                                         :sql-id :foo
                                         :rows {"bar" {:columns {:foo "bar" :fu "baz"}
                                                       :dependent-tables []}}}]}}
               table))))
    (testing "get-table-row recurses with parent keys"
      (let [nested-def {:xml-id ["." #(vtd/attr % :name)]
                        :sql-id :id
                        :each "./nested"
                        :table :nested
                        :columns {:parent [".." #(vtd/attr % :name) :parent]
                                  :name ["." #(vtd/attr % :name)]}
                        :dependent-tables []}
            container-def {:xml-id ["." #(vtd/attr % :name)]
                           :sql-id :id
                           :each "/root/container"
                           :table :container
                           :columns {:name ["." #(vtd/attr % :name)]}
                           :dependent-tables [nested-def]}
            table-tpl (get-table-row
                        ($x? "/root/container" "<root><container name=\"foo\"><nested name=\"bar\" /></container></root>")
                        container-def)
            table (assoc-in (nil-map-rows table-tpl)
                            ["foo" :dependent-tables]
                            (ctx-map-tables (get-in table-tpl ["foo" :dependent-tables]) 12 {})) ]
        (is (= {"foo" {:columns {:name "foo"}
                       :dependent-tables [{:table :nested
                                           :sql-id :id
                                           :rows {"bar" {:columns {:parent 12 :name "bar"}
                                                         :dependent-tables []}}}]}}
               table))))
    (testing "get-table-row does not allow :collapse with :dependent-tables"
      (is (thrown? IllegalArgumentException (get-table-row "<root />" 
                                                           {:xml-id ["." vtd/tag]
                                                            :sql-id :id
                                                            :each "/root"
                                                            :table :root
                                                            :columns {}
                                                            :collapse [{}]
                                                            :dependent-tables [{}]}))))
    (testing "get-table-row returns multiple rows for :collapse"
      (let [table-def {:xml-id ["." vtd/tag]
                       :sql-id :id
                       :each "/root/*"
                       :table :root
                       :columns {:tag ["." vtd/tag]}
                       :collapse [{:xml-id ["." vtd/tag]
                                   :each "@*"
                                   :columns {:attr ["." vtd/tag]
                                             :value ["." vtd-value]}}]}
            node ($x? "/root/*" "<root><foobar foo=\"baz\" bar=\"qux\"/></root>")
            table (nil-map-rows (get-table-row node table-def))]
        (is (= {["foobar" "foo"] {:columns {:tag "foobar" :attr "foo" :value "baz"}
                                  :dependent-tables []}
                ["foobar" "bar"] {:columns {:tag "foobar" :attr "bar" :value "qux"}
                                  :dependent-tables []}}
               table))))
    (testing "get-table-row concatenates multiple :collapse definitions"
      (let [table-def {:xml-id ["." vtd/tag]
                       :sql-id :id
                       :each "/root/*"
                       :table :root
                       :columns {:tag ["." vtd/tag]}
                       :collapse [{:xml-id ["." vtd/tag]
                                   :each "@*"
                                   :columns {:attr ["." vtd/tag]
                                             :value ["." vtd-value]}}
                                  {:xml-id ["." vtd/tag]
                                   :each "./*"
                                   :columns {:attr ["." vtd/tag]
                                             :value ["." vtd/text]}}]}
            node ($x? "/root/*" "<root><foobar foo=\"baz\" bar=\"qux\"><x>3</x></foobar></root>")
            table (nil-map-rows (get-table-row node table-def))]
        (is (= {["foobar" "foo"] {:columns {:tag "foobar" :attr "foo" :value "baz"}
                                  :dependent-tables []}
                ["foobar" "bar"] {:columns {:tag "foobar" :attr "bar" :value "qux"}
                                  :dependent-tables []} 
                ["foobar" "x"] {:columns {:tag "foobar" :attr "x" :value "3"}
                                  :dependent-tables []}}
               table))))
    (testing "get-table-row returns multiple rows for :collapse"
      (let [table-def {:xml-id ["." vtd/tag]
                       :sql-id :id
                       :each "/root/*"
                       :table :root
                       :columns {:tag ["." vtd/tag]}
                       :collapse [{:xml-id ["." vtd/tag]
                                   :each "@*"
                                   :columns {:attr ["." vtd/tag]
                                             :value ["." vtd-value]}}]
                       }
            table (nil-map-rows (get-table-row
                        ($x? "/root/*" "<root><foobar foo=\"baz\" bar=\"qux\"/></root>")
                        table-def))]
        (is (= {["foobar" "foo"] {:columns {:tag "foobar" :attr "foo" :value "baz"}
                                  :dependent-tables []}
                ["foobar" "bar"] {:columns {:tag "foobar" :attr "bar" :value "qux"}
                                  :dependent-tables []}}
               table))))

    ))

(deftest test-insert-table
  (let [inserter-fn (fn [expected]
                      (let [remaining (atom expected)]
                        (fn [table-name columns]
                          (if (nil? table-name)
                            (is (empty? @remaining))
                            (let [rval (get @remaining [table-name columns])]
                              (is (not (nil? rval)))
                              (swap! remaining dissoc [table-name columns])
                              rval)))))]
    (testing "insert-row returns xml->sql id map"
      (let [columns {:name "foo"}
            expected {[:foobar {:name "foo"}] {:id 1}}
            inserter (inserter-fn expected)]
        (is (= (insert-row inserter :foobar :id "foo" columns) ["foo" 1]))))
    (testing "A simple insert-table returns xml->sql id map"
      (let [table {:table :foobar
                   :sql-id :id
                   :rows {"foo" {:columns {:name (fn [_ _] "foo")} :dependent-tables []}
                          "bar" {:columns {:name (fn [_ _] "bar")} :dependent-tables []}}}
            expected {[:foobar {:name "foo"}] {:id 1}
                      [:foobar {:name "bar"}] {:id 2}}
            inserter (inserter-fn expected)]
        (is (= (insert-table inserter table) {:foobar {"foo" [1 {}] "bar" [2 {}]}}))))
    (testing "insert-table passes parent id to dependent-tables"
      (let [nested-foo {:table :baz
                        :sql-id :id
                        :rows {"baz" {:columns {:parent (fn [p _] p) :name (fn [_ _] "baz")}}}}
            nested-bar {:table :baz
                        :sql-id :id
                        :rows {"baz" {:columns {:parent (fn [p _] p) :name (fn [_ _] "baz")}}
                               "qux" {:columns {:parent (fn [p _] p) :name (fn [_ _] "qux")}}}}
            table {:table :foobar
                   :sql-id :id
                   :rows {"foo" {:columns {:name (fn [_ _] "foo")} :dependent-tables [nested-foo]}
                          "bar" {:columns {:name (fn [_ _] "bar")} :dependent-tables [nested-bar]}}}
            expected {[:foobar {:name "foo"}] {:id 1}
                      [:foobar {:name "bar"}] {:id 2}
                      [:baz {:parent 1 :name "baz"}] {:id 3}
                      [:baz {:parent 2 :name "baz"}] {:id 4}
                      [:baz {:parent 2 :name "qux"}] {:id 5}}
            inserter (inserter-fn expected)]
        (is (= (insert-table inserter table)
               {:foobar {"foo" [1 {:baz {"baz" [3 {}]}}]
                         "bar" [2 {:baz {"baz" [4 {}]
                                         "qux" [5 {}]}}]}}))
        (inserter nil nil)
        ))
    (testing "insert-table passes sibling ids to dependent-tables"
      (let [nested-foo {:table :foo
                        :sql-id :id
                        :rows {"foo" {:columns {:parent (fn [p _] p) :name (fn [_ _] "foo")}}}}
            nested-bar {:table :bar
                        :sql-id :id
                        :rows {"bar" {:columns {:parent (fn [p _] p)
                                                :foo (fn [_ ctx] (first (get-in ctx [:foo "foo"])))
                                                :name (fn [_ _] "bar")}}
                               "qux" {:columns {:parent (fn [p _] p)
                                                :foo (fn [_ ctx] (first (get-in ctx [:foo "foo"])))
                                                :name (fn [_ _] "qux")}}}}
            table {:table :foobar
                   :sql-id :id
                   :rows {"foobar" {:columns {:name (fn [_ _] "foobar")} :dependent-tables [nested-foo nested-bar]}}}
            expected {[:foobar {:name "foobar"}] {:id 1}
                      [:foo {:parent 1 :name "foo"}] {:id 2}
                      [:bar {:parent 1 :foo 2 :name "bar" }] {:id 3}
                      [:bar {:parent 1 :foo 2 :name "qux" }] {:id 4}}
            inserter (inserter-fn expected)]
        (is (= (insert-table inserter table)
               {:foobar {"foobar" [1 {:foo {"foo" [2 {}]}
                                      :bar {"bar" [3 {}]
                                            "qux" [4 {}]}}]}}))
        (inserter nil nil)
        ))))
