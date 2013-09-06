(ld "coi/util/def-defpkg.lsp" :dir :system)

(defpkg "SEQMATCH"
  (union-eq *acl2-exports*
            *common-lisp-symbols-from-main-lisp-package*))
