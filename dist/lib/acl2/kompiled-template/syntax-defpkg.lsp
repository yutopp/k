(ld "coi/util/def-defpkg.lsp" :dir :system)
(defpkg "SYNTAX"
    (revappend *acl2-exports*
      *common-lisp-symbols-from-main-lisp-package*))
