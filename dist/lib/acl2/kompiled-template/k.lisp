(in-package "K")

(defun map-lookup (key map)
  (cons (assoc-equal key map) (delete-assoc-equal key map)))

(defthmd map-lookup-test1
  (equal (map-lookup 'x '((x . 1) (y . 2)))
         '((x . 1) (y . 2))))
(defthmd map-lookup-test2
  (equal (map-lookup 'x '((y . 2) (x . 1)))
         '((x . 1) (y . 2))))
(defthmd map-lookup-test3
  (not (alistp (map-lookup 'z '((y . 2) (x . 1))))))

(defun map-lookups (keys map)
  (if (endp keys)
      map
    (cons (assoc-equal (car keys) map)
          (map-lookups (cdr keys) (delete-assoc-equal (car keys) map)))))

(defthmd map-lookups-test1
  (equal (map-lookups '(x y z) '((x . 1) (z . 2) (h . 4) (y . 3) (w . 5)))
         '((x . 1) (y . 3) (z . 2) (h . 4) (w . 5))))
(defthmd map-lookups-test2
  (equal (map-lookups '(x v y) '((z . 2) (x . 1) (h . 4) (y . 3) (w . 5)))
         '((x . 1) nil (y . 3) (z . 2) (h . 4) (w . 5))))

(defthm lookup-alistp-success
  (implies (and (alistp map) (alistp (map-lookup key map)))
           (equal (caar (map-lookup key map)) key)))

(defun map-keys (map)
  (if (endp map)
      ()
    (cons (caar map) (map-keys (cdr map)))))

;; (defun key-prefix (keys alist)
;;   (if (endp keys)
;;       t
;;     (and (equal (car keys) (caar alist))
;;          (key-prefix (cdr keys) (cdr alist)))))

;; (defthm lookups-alistp-success
;;   (implies (and (alistp map) (true-listp keys) (alistp (map-lookups keys map)))
;;            (key-prefix keys (map-lookups keys map))))

(defun freshen-symbols (pkg-sym term)
  (case-match
   term
   (('unfresh t) t)
   ((a . b) (cons (freshen-symbols pkg-sym a)
                  (freshen-symbols pkg-sym b)))
   (& (if (and (symbolp term)
               (equal "K" (symbol-package-name term)))
          (intern-in-package-of-symbol (symbol-name term) pkg-sym)
        term))))

(defun int-op-fn (hook-name function-name)
  `(defun ,hook-name (x y)
     ;; (declare (xargs :guard (and (|#INT-CONFORMANT| x)
     ;;                             (|#INT-CONFORMANT| y))))
     (if (and (|#INT-CONFORMANT| x)
              (|#INT-CONFORMANT| y))
         (|#INT| (,function-name (cadr x)
                                 (cadr y)))
       nil)))
(defun int-rel-fn (hook-name relation-name)
  `(defun ,hook-name (x y)
     ;; (declare (xargs :guard (and (|#INT-CONFORMANT| x)
     ;;                             (|#INT-CONFORMANT| y))))
     (if (and (|#INT-CONFORMANT| x)
              (|#INT-CONFORMANT| y))
         (mkbool (,relation-name (cadr x)
                                 (cadr y)))
       nil)))
(defmacro int-op-defn (hook-name function-name)
  (list 'quote (int-op-fn hook-name function-name)))
(defmacro int-rel-defn (hook-name relation-name)
  (list 'quote (int-rel-fn hook-name relation-name)))

(defun sort-predicates-fn (pkg-sym grammar)
  (if (endp grammar)
      nil
    (append
     (let ((item (car grammar)))
       (and (case-match item
                        ((& ':group . &) t)
                        ((& ':element body) (symbolp body)))
            `((defun ,(intern-in-package-of-symbol
                       (string-upcase (string-append "is-" (caar grammar)))
                       pkg-sym)
                (term)
                (,(intern-in-package-of-symbol "MKBOOL" pkg-sym)
                 (,(intern-in-package-of-symbol
                    (string-upcase (string-append (caar grammar)
                                                  "-conformant"))
                    pkg-sym)
                  term))))))
     (sort-predicates-fn pkg-sym (cdr grammar)))))

(defun define-sort-predicates (pkg-sym grammar)
  (cons 'progn (sort-predicates-fn pkg-sym grammar)))

(defun define-k-builtins (pkg-sym)
  (freshen-symbols
   pkg-sym
   `(progn
      (defun mkbool (x)
        ;; (declare (xargs :guard (booleanp x)))
        (if x (|'TRUE|) (|'FALSE|)))
      (defun bool-sense (b)
        ;; (declare (xargs :guard (or (booleanp b) (|#BOOL-CONFORMANT| b))))
        (equal b '(|'TRUE|)))
      (defun predicate-and-fn (preds)
        (if (endp preds)
            nil
          (cons (list 'bool-sense (car preds))
                (predicate-and-fn (cdr preds)))))
      (defmacro predicate-and (&rest preds)
        (list 'mkbool (cons 'and (predicate-and-fn preds))))
      (defthm mkbool-domain
        (implies (booleanp b)
                 (|#BOOL-CONFORMANT| (mkbool b))))
      ,(int-rel-defn =/=INT /=)
      ,(int-rel-defn <=INT <=)
      ,(int-op-defn +INT +)
      (defun not-bool (b)
        ;; (declare (xargs :guard (|#BOOL-CONFORMANT| b)))
        (mkbool (not (bool-sense b))))
      (defun /INT (x y)
        ;; (declare (xargs :guard (and (|#INT-CONFORMANT| x)
        ;;                             (|#INT-CONFORMANT| y)
        ;;                             (not (equal (cadr y) 0)))))
        (|#INT| (truncate (cadr x) (cadr y))))
      (defun set-member (key set)
        ;; (declare (xargs :guard (true-listp set)))
        (mkbool (consp (member-equal key set))))
      )))
