(in-package "SEQMATCH")

(defun equal-x-constant (x const)
; x is an arbitrary term, const is a quoted constant, e.g., a list of
; the form (QUOTE guts).  We return a term equivalent to (equal x
; const).
  (let ((guts (cadr const)))
    (cond ((symbolp guts)
           (list 'eq x const))
          ((or (acl2-numberp guts)
               (characterp guts))
           (list 'eql x guts))
          ((stringp guts)
           (list 'equal x guts))
          (t (list 'equal x const)))))

(defun match-tests-and-bindings (x pat tests bindings)
; Analyze a single pattern as for case-match
; We return two results.  The first is a list of tests, in reverse
; order, that determine whether x matches the structure pat.
; The tests are accumulated onto tests, which should be nil initially.
; The second result is an alist containing entries of the form (sym expr),
; suitable for use as the bindings in the let we generate if the tests are satisfied.  The
; bindings required by pat are accumulated onto bindings and thus are
; reverse order, although their order is actually irrelevant.
  (cond
   ((symbolp pat)
    (cond
     ((or (eq pat t)
          (eq pat nil))
      (mv (cons (list 'eq x pat) tests) bindings))
     ((and (> (length (symbol-name pat)) 0)
           (eql #\* (char (symbol-name pat) 0)))
      (mv (cons (list 'equal x pat) tests) bindings))
     ((and (> (length (symbol-name pat)) 0)
           (eql #\! (char (symbol-name pat) 0)))
      (mv (cons (list 'equal x
                      (intern (coerce (cdr (coerce (symbol-name pat)
                                                   'list))
                                      'string)
                              "ACL2"))
                tests)
          bindings))
     ((eq pat '&) (mv tests bindings))
     (t (let ((binding (assoc-eq pat bindings)))
          (cond ((null binding)
                 (mv tests (cons (list pat x) bindings)))
                (t (mv (cons (list 'equal x (cadr binding)) tests)
                       bindings)))))))
   ((atom pat)
    (mv (cons (equal-x-constant x (list 'quote pat)) tests)
        bindings))
   ((eq (car pat) 'quote)
    (mv (cons (equal-x-constant x pat) tests)
        bindings))
   (t (mv-let (tests1 bindings1)
        (match-tests-and-bindings (list 'car x) (car pat)
                                  (cons (list 'consp x) tests)
                                  bindings)
        (match-tests-and-bindings (list 'cdr x) (cdr pat)
                                  tests1 bindings1)))))

(defun match-sequential (x pat rest stages)
; decompose a sequential match.
; x is an expression to be matched against pat,
; rest is a list of triples (exp var pat)
; describing further destructuring.
; Result is a list of stages in reverse order,
; where a stage is either a list (let <var> <exp>)
; building a new value, or
; a cons cell (<test> . <bindings>) as returned
; by match-tests-and-bindings
  (mv-let (tests1 bindings1)
          (match-tests-and-bindings x pat () ())
          (let ((stages1 (cons (cons (reverse tests1) (reverse bindings1)) stages)))
            (if (consp rest)
                (let* ((item (car rest))
                       (exp (car item))
                       (x1 (cadr item))
                       (pat1 (caddr item)))
                  (match-sequential x1 pat1 (cdr rest) (cons (list 'let x1 exp) stages1)))
              stages1))))

(defun build-clause (stages test body)
; Build a clause from stages as returned by match-sequential
; accumulates a test and a body as in a cond clause onto the arguments.
  (case-match
   stages
   ((('let var exp) . stages1)
    (build-clause stages1
                  `(let ((,var ,exp)) (declare (ignorable ,var)) ,test)
                  `((let ((,var ,exp)) (declare (ignorable ,var)) ,@body))))
   (((tests . binds) . stages1)
    (build-clause stages1
                  `(and ,@tests
                        (let ,binds (declare (ignorable ,@(strip-cars binds))) ,test))
                  `((let ,binds (declare (ignorable ,@(strip-cars binds))) ,@body))))
   (& (cons test body))))

(defun seqmatch-clause (x pat forms)
  (build-clause (match-sequential x (car pat) (cdr pat) ()) t forms))

(defun seqmatch-clause-list (x clauses)
  (cond ((consp clauses)
         (if (eq (caar clauses) '&)
             ; wildcard clause might as well be last
             (list (seqmatch-clause x (caar clauses) (cdar clauses)))
           (cons (seqmatch-clause x (caar clauses) (cdar clauses))
                 (seqmatch-clause-list x (cdr clauses)))))
        (t '((t nil)))))

(defmacro seq-match (&rest args)
  (declare (xargs :guard (and (consp args)
                              (symbolp (car args))
                              (alistp (cdr args))
                              (null (cdr (member-equal (assoc-eq '& (cdr args))
                                                       (cdr args)))))))
  (cons 'cond (seqmatch-clause-list (car args) (cdr args))))


;(let ((x '((a . (1 . 2)) (b . (3 . 4)))))
;  (seq-match x
;    ((l
;      ((map-lookup 'a l) m ((x . y) . m1))
;      ((map-lookup 'b m1) m2 ((z . w) . ())))
;     (list x y z w)))
;)

;; Example configuration
;; (t
;;  (k (lookup 1))
;;  (heap
;;    (object
;;      (object-id 1)
;;      (val 'A))
;;    (object
;;      (object-id 2)
;;      (val 'B)))))

