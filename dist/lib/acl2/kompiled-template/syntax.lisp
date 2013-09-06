(in-package "SYNTAX")
(defun example ()
  '(("exp" :group ("add" "mul" "int"))
    ("add" :element
     ((addend "exp")
      (augend "exp")))
    ("mul" :element
     ((mul1 "exp")
      (mul2 "exp")))
    ("int" :element integerp)))
(defun example-rec ()
  '(("exp" :group ("add" "mul" "int" "stmt"))
    ("add" :element
     ((addend "exp")
      (augend "exp")))
    ("mul" :element
     ((mul1 "exp")
      (mul2 "exp")))
    ("int" :element integerp)
    ("stmt" :group ("exp" "inc-state" "add"))
    ("inc-state" :element
     ((amount "exp")))))

(defun cars (l)
  (if (endp l)
      nil
    (cons (caar l) (cars (cdr l)))))

(defun con-name (pkg name)
  (intern-in-package-of-symbol (string-upcase name) pkg))

(defun mkconstructors (pkg-sym defn)
  (case-match
   defn
   (((name ':element binds) . defns)
    (let ((con (intern-in-package-of-symbol (string-upcase name) pkg-sym)))
      (cons
       (if (listp binds)
           `(defun ,con ,(cars binds) (list ',con ,@(cars binds)))
         `(defun ,con (body) (list ',con body)))
       (mkconstructors pkg-sym defns))))
   (((& ':group &) . defns)
    (mkconstructors pkg-sym defns))))

(defun make-constructors (pkg-sym defn)
  (cons 'progn (mkconstructors pkg-sym defn)))

(include-book "ordinals/lexicographic-ordering" :dir :system)
;; Termination measure for leafs function
(defun unvisited (table visited)
  (if (endp table)
      0
    (if (member-equal (caar table) visited)
        (unvisited (cdr table) visited)
      (1+ (unvisited (cdr table) visited)))))

(defthm unvisited-monotone
  (<= (unvisited table (cons x visited))
      (unvisited table visited)))
(defthm unvisited-monotone-2
  (< (unvisited table (cons x visited))
     (1+ (unvisited table visited))))
(defthm unvisited-decrease
  (implies (and (not (member-equal x visited))
                (assoc x table))
           (< (unvisited table (cons x visited))
              (unvisited table visited))))

(defun leafs (table visited leafs working)
  (declare (xargs :measure (list (if (consp working) 1 0)
                                 (unvisited table visited)
                                 (length working))
                  :well-founded-relation acl2::l<))
  (if (endp working)
      leafs
    (if (member-equal (car working) visited)
        (leafs table visited leafs (cdr working))
      (if (assoc (car working) table)
          (leafs table
                 (cons (car working) visited)
                 leafs
                 (union-equal (cdr working) (cdr (assoc (car working) table))))
        (leafs table
               (cons (car working) visited)
               (union-equal (list (car working)) leafs)
               (cdr working))))))
(defun reachable-leaves (table var)
  (leafs table () () (list var)))

(defun pred-name (pkg name)
  (intern-in-package-of-symbol
   (string-upcase (string-append name "-conformant"))
   pkg))
(defun mktests (pkg l)
  (if (endp l)
      nil
    (cons (list (pred-name pkg (cadar l)) (caar l))
          (mktests pkg (cdr l)))))

(defun subtests (pkg term tests)
  (if (endp tests)
      nil
    (cons (list (pred-name pkg (car tests)) term)
          (subtests pkg term (cdr tests)))))
;; Construct membership predicate definitions
;; for the NonRecursive elements, which are
;; the ones given by a membership predicate
(defun nr-preds (pkg-sym defn)
  (case-match
   defn
   (((name ':element ()) . defns)
    (let ((con (con-name pkg-sym name)))
      (cons
       `(defun ,(pred-name pkg-sym name) (term)
          (case-match
           term
           ((',con) t)))
       (nr-preds pkg-sym defns))))
   (((name ':element binds) . defns)
    (if (symbolp binds)
        (let ((con (con-name pkg-sym name)))
          (cons
           `(defun ,(pred-name pkg-sym name) (term)
              (case-match
               term
               ((',con body) (,binds body))))
           (nr-preds pkg-sym defns)))
      (nr-preds pkg-sym defns)))
   ((& . defns) (nr-preds pkg-sym defns))))

;; accumulate an association list from
;; group names to lists of member names
(defun group-members (table defn)
  (case-match
   defn
   (() table)
   (((& ':element &) . defns)
    (group-members table defns))
   (((name ':group elts) . defns)
    (group-members (cons (cons name elts) table) defns))))

(defun mkpreds (pkg-sym group-table defn)
  (case-match
   defn
   (((name ':element binds) . defns)
    (if (symbolp binds)
        (mkpreds pkg-sym group-table defns)
      (let ((con (con-name pkg-sym name)))
        (cons
         `(defun ,(pred-name pkg-sym name) (term)
            (declare (xargs :measure (list (acl2-count term) 0)))
            (case-match
             term
             ((',con ,@(cars binds))
              (and ,@(mktests pkg-sym binds)))))
         (mkpreds pkg-sym group-table defns)))))
   (((name ':group &) . defns)
    (cons
     `(defun ,(pred-name pkg-sym name) (term)
        (declare (xargs :measure (list (acl2-count term) 1)))
        (or ,@(subtests pkg-sym 'term (reachable-leaves group-table name))))
     (mkpreds pkg-sym group-table defns)))))

(defun make-predicates (pkg-sym defn)
  `(encapsulate
    ()
    (set-well-founded-relation acl2::l<)
    (set-bogus-mutual-recursion-ok t)
    ,@(nr-preds pkg-sym defn)
    (mutual-recursion ,@(mkpreds pkg-sym (group-members () defn) defn))))
