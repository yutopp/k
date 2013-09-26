(ld "coi/util/def-defpkg.lsp" :dir :system)

(defpkg "K"
  (union-eq *acl2-exports*
            *common-lisp-symbols-from-main-lisp-package*))
(defconst *k-exports*
   '(k::|'TRUE|
     k::|'FALSE|
     k::|'TRUE-CONFORMANT|
     k::|'FALSE-CONFORMANT|
     k::|#BOOL-CONFORMANT|
     k::mkbool
     k::not-bool
     k::bool-sense
     k::predicate-and
     k::|#INT|
     k::|#INT-CONFORMANT|
     k::=/=INT
     k::<=INT
     k::+INT
     k::/INT
     k::set-member
     k::|#ID|
     k::|#ID-CONFORMANT|
     k::|#STRING|
     k::|#STRING-CONFORMANT|
     ))
